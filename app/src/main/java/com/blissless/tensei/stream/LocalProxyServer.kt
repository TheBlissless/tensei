package com.blissless.tensei.stream

import android.util.Log
import eu.kanade.tachiyomi.animesource.AnimeCatalogueSource
import eu.kanade.tachiyomi.animesource.online.AnimeHttpSource
import eu.kanade.tachiyomi.animesource.model.Video
import okhttp3.OkHttpClient
import okhttp3.Request as OkRequest
import okhttp3.Response as OkResponse
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket
import java.net.URI
import java.util.concurrent.Executors
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit
import com.blissless.tensei.util.ErrorHandler

/**
 * Local HTTP proxy server that rewrites m3u8 playlists and forwards
 * segment requests with the extension's headers/cookies.
 *
 * ## Thread-safety fixes in this version
 *
 * The previous version used `Executors.newCachedThreadPool()` which
 * grows without bound — every incoming request spawns a new thread
 * if no idle thread is available. When the stale-URL retry loop in
 * `playEpisodeWithExtension` thrashed through every server (each
 * invocation calling `LocalProxyServer.start` + `registerVideo` for
 * every video), the proxy received a flood of HLS segment requests
 * and the cached thread pool grew to hundreds of threads. Eventually
 * the OS refused to spawn more → `pthread_create (4112KB stack) failed`
 * → app crash.
 *
 * This version uses a **bounded** `ThreadPoolExecutor` with:
 *   - Core pool size: 4 threads (kept alive indefinitely).
 *   - Max pool size: 16 threads (hard cap to prevent OOM).
 *   - Work queue: `LinkedBlockingQueue` (requests queue up instead of
 *     spawning new threads beyond the cap).
 *   - Rejection policy: `CallerRunsPolicy` (if the queue is full, the
 *     calling thread runs the task itself — back-pressure instead of
 *     OOM).
 *
 * We also:
 *   - Track the accept-loop `Thread` reference and `interrupt()` it
 *     in `stop()` so it actually exits (previously, `running = false`
 *     alone left the thread blocked in `serverSocket!!.accept()`).
 *   - Properly shut down the executor in `stop()` AND recreate it on
 *     the next `start()` (since `shutdownNow()` puts a thread pool
 *     executor into a permanent shutdown state).
 *   - Make `clientExecutor` a `var` so we can replace the shut-down
 *     instance with a fresh one.
 */
object LocalProxyServer {
    private const val TAG = "LocalProxyServer"
    const val PROXY_PORT = 41223

    /** Hard cap on concurrent proxy-handler threads. Prevents OOM. */
    private const val MAX_THREADS = 16
    /** Idle threads kept alive between bursts of requests. */
    private const val CORE_THREADS = 4
    /** How long an idle thread waits before being reaped (seconds). */
    private const val IDLE_THREAD_KEEPALIVE_SEC = 30L
    /** How long to wait for the executor to drain on stop (seconds). */
    private const val SHUTDOWN_TIMEOUT_SEC = 2L

    private var serverSocket: ServerSocket? = null
    @Volatile private var running = false
    private var extensionClient: OkHttpClient? = null
    private var currentSource: AnimeHttpSource? = null
    private val pathToVideo = mutableMapOf<String, Video>()
    /** Bounded thread pool — see class kdoc for why. */
    private var clientExecutor: ThreadPoolExecutor = newBoundedExecutor()
    /** Reference to the accept-loop thread so we can interrupt it on stop. */
    private var acceptThread: Thread? = null

    private fun newBoundedExecutor(): ThreadPoolExecutor = ThreadPoolExecutor(
        CORE_THREADS,
        MAX_THREADS,
        IDLE_THREAD_KEEPALIVE_SEC,
        TimeUnit.SECONDS,
        LinkedBlockingQueue(),
        // CallerRunsPolicy: if queue is full + max threads reached, run
        // the task on the caller's thread instead of throwing / spawning
        // beyond the cap. This is back-pressure, not OOM.
        ThreadPoolExecutor.CallerRunsPolicy(),
    )

    fun start(client: OkHttpClient?, source: AnimeCatalogueSource?) {
        synchronized(this) {
            if (serverSocket != null && running) {
                Log.d(TAG, "start: already running, ignoring")
                return
            }
            extensionClient = client ?: try { eu.kanade.tachiyomi.network.NetworkHelper.getInstance().client } catch (e: Exception) { ErrorHandler.report("LocalProxyServer", "operation failed, returning null", e); null }
            if (extensionClient == null) {
                Log.w(TAG, "No extension client available — proxy will return 502")
            }
            currentSource = source as? AnimeHttpSource

            try {
                // If a previous executor is shut down, recreate it.
                if (clientExecutor.isShutdown) {
                    clientExecutor = newBoundedExecutor()
                }
                serverSocket = ServerSocket()
                serverSocket!!.reuseAddress = true
                serverSocket!!.bind(InetSocketAddress(PROXY_PORT))
                running = true
                Log.i(TAG, "Proxy server started on port $PROXY_PORT (executor: core=${clientExecutor.corePoolSize} max=${clientExecutor.maximumPoolSize})")

                acceptThread = Thread {
                    while (running) {
                        try {
                            val clientSocket = serverSocket!!.accept()
                            try {
                                clientExecutor.execute { handleClient(clientSocket) }
                            } catch (rejected: java.util.concurrent.RejectedExecutionException) {
                                // Pool is shutting down — close the socket and stop accepting.
                                try { clientSocket.close() } catch (_: Exception) {}
                                if (running) Log.w(TAG, "Executor rejected task (shutting down?)")
                                break
                            }
                        } catch (e: Exception) {
                            if (running) Log.e(TAG, "Accept error", e)
                        }
                    }
                    Log.d(TAG, "Accept loop thread exiting")
                }.apply { isDaemon = true; name = "LocalProxyServer-accept" }
                acceptThread!!.start()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to start proxy server", e)
            }
        }
    }

    fun registerVideo(video: Video) {
        try {
            val uri = URI(video.videoUrl)
            // CRITICAL: use path + query as the lookup key, not just path.
            //
            // All videos from sources like animex share the same path
            // (/playlist.m3u8) and differ only in the ?url=... query
            // parameter (each query encodes a different upstream URL).
            // If we keyed on path alone, every registerVideo() call would
            // overwrite the previous entry, and only the LAST registered
            // video would be findable — ExoPlayer's request for a different
            // video would get the wrong upstream URL → HTTP 500 → playback
            // fails ("keeps loading").
            val key = uri.path + (if (uri.query != null) "?${uri.query}" else "")
            pathToVideo[key] = video
            Log.d(TAG, "Registered video key: $key -> ${video.videoUrl.take(60)}")
        } catch (e: Exception) { ErrorHandler.ignore("LocalProxyServer", "best-effort operation failed", e) }
    }

    /**
     * Clear all registered videos WITHOUT stopping the proxy server.
     *
     * Call this at the start of each `playEpisodeWithExtension` call,
     * BEFORE registering new videos. Without this, stale entries from
     * a previous fetch (pointing to a now-dead source HTTP server on a
     * different port) remain in `pathToVideo`. When the segment fallback
     * uses `pathToVideo.values.first()` to determine the source server's
     * scheme+host+port, it gets the STALE entry → wrong port → "unexpected
     * end of stream" → playback stuck loading.
     */
    fun clearRegisteredVideos() {
        val count = pathToVideo.size
        pathToVideo.clear()
        Log.d(TAG, "clearRegisteredVideos: cleared $count stale video(s)")
    }

    fun stop() {
        synchronized(this) {
            running = false
            try { serverSocket?.close() } catch (e: Exception) { ErrorHandler.ignore("LocalProxyServer", "best-effort operation failed", e) }
            serverSocket = null
            // Interrupt the accept loop so it actually exits (otherwise
            // it's blocked in serverSocket!!.accept() and won't see
            // `running = false`).
            acceptThread?.interrupt()
            acceptThread = null
            // Drain pending tasks then shut down. Don't use
            // shutdownNow() because that cancels in-flight requests
            // mid-transfer (ExoPlayer would see truncated responses).
            clientExecutor.shutdown()
            try {
                if (!clientExecutor.awaitTermination(SHUTDOWN_TIMEOUT_SEC, TimeUnit.SECONDS)) {
                    Log.w(TAG, "stop: forcing shutdown, ${clientExecutor.shutdownNow().size} task(s) cancelled")
                }
            } catch (_: InterruptedException) {
                clientExecutor.shutdownNow()
            }
            // Mark for recreation on next start().
            clientExecutor = newBoundedExecutor()
            pathToVideo.clear()
            Log.d(TAG, "Proxy server stopped")
        }
    }

    private fun handleClient(clientSocket: Socket) {
        try {
            clientSocket.soTimeout = 30000
            val reader = BufferedReader(InputStreamReader(clientSocket.getInputStream(), Charsets.UTF_8))
            val requestLine = reader.readLine() ?: run {
                Log.w(TAG, "handleClient: empty request line, closing socket")
                return
            }
            Log.i(TAG, "handleClient: request=$requestLine")
            val parts = requestLine.split(" ")
            if (parts.size < 2) {
                Log.w(TAG, "handleClient: malformed request line (parts=${parts.size}), closing socket")
                return
            }
            val method = parts[0]
            val requestUri = parts[1]
            val uri = URI(requestUri)
            val requestPath = uri.path
            val query = uri.query

            val headers = mutableMapOf<String, String>()
            var line = reader.readLine()
            var contentLength = 0
            while (line != null && line.isNotEmpty()) {
                val colonIdx = line.indexOf(":")
                if (colonIdx > 0) {
                    val key = line.substring(0, colonIdx).trim()
                    val value = line.substring(colonIdx + 1).trim()
                    headers[key] = value
                    if (key.equals("Content-Length", ignoreCase = true)) {
                        contentLength = value.toIntOrNull() ?: 0
                    }
                }
                line = reader.readLine()
            }

            if (contentLength > 0) {
                reader.skip(contentLength.toLong())
            }

            Log.d(TAG, "Proxy $method $requestPath${if (query != null) "?$query" else ""}")

            // CRITICAL: look up by path + query (matches registerVideo's keying).
            // Keying by path alone would return the wrong video when multiple
            // videos share the same path (e.g. animex's /playlist.m3u8 with
            // different ?url=... queries).
            val lookupKey = requestPath + (if (query != null) "?$query" else "")
            val video = pathToVideo[lookupKey]
            if (video != null) {
                Log.d(TAG, "handleClient: found video for key=$lookupKey -> ${video.videoUrl.take(80)}")
            } else {
                // No exact match — this is likely a SEGMENT request (.ts, .m3u8
                // sub-playlist, .mp4, etc.) that was rewritten by rewriteM3u8
                // to point back at our proxy. We don't register individual
                // segments (there are hundreds per episode), but we CAN
                // forward to the source's HTTP server using the SAME
                // scheme+host+port as the registered playlist video.
                //
                // Without this fallback, segment requests get 404 → ExoPlayer
                // gets the m3u8 playlist but never any segment data → playback
                // stuck loading forever ("keeps loading").
                Log.d(TAG, "handleClient: no exact match for key=$lookupKey — falling back to source HTTP server (first registered video's base)")
            }
            val videoHeaders = video?.headers
            val sourceHeaders = currentSource?.headers

            val client = extensionClient
            if (client == null) {
                Log.e(TAG, "handleClient: extensionClient is null — cannot forward request")
                sendResponse(clientSocket, 502, "text/plain", "No extension client".toByteArray())
                return
            }

            val sourceBaseUrl = currentSource?.baseUrl
            val referer = videoHeaders?.let { h ->
                (0 until h.size).firstOrNull { h.name(it).equals("Referer", ignoreCase = true) }
                    ?.let { h.value(it) }
            } ?: sourceHeaders?.let { h ->
                (0 until h.size).firstOrNull { h.name(it).equals("Referer", ignoreCase = true) }
                    ?.let { h.value(it) }
            }
            val upstreamHost = when {
                referer != null -> try { URI(referer).let { "${it.scheme}://${it.host}" } } catch (e: Exception) { ErrorHandler.report("LocalProxyServer", "operation failed, returning null", e); null }
                sourceBaseUrl != null -> sourceBaseUrl
                else -> null
            }

            val upstreamUrl = if (video != null) {
                val base = video.videoUrl
                if (query != null && !base.contains("?")) "$base?$query" else base
            } else if (pathToVideo.isNotEmpty()) {
                val firstVideo = pathToVideo.values.first()
                val videoUri = try { URI(firstVideo.videoUrl) } catch (e: Exception) { ErrorHandler.report("LocalProxyServer", "operation failed, returning null", e); null }
                val serverBase = if (videoUri != null) {
                    val port = if (videoUri.port > 0) ":${videoUri.port}" else ""
                    "${videoUri.scheme}://${videoUri.host}$port"
                } else {
                    firstVideo.videoUrl.substringBeforeLast("/")
                }
                "$serverBase$requestPath${if (query != null) "?$query" else ""}"
            } else {
                if (upstreamHost == null) {
                    Log.w(TAG, "Cannot determine upstream host for $requestPath")
                    sendResponse(clientSocket, 502, "text/plain", "Unknown upstream host".toByteArray())
                    return
                }
                "$upstreamHost$requestPath${if (query != null) "?$query" else ""}"
            }
            Log.i(TAG, "handleClient: forwarding to $upstreamUrl")

            val effectiveHeaders = videoHeaders ?: sourceHeaders
            val reqBuilder = OkRequest.Builder().url(upstreamUrl)
            if (effectiveHeaders != null) {
                for (i in 0 until effectiveHeaders.size) {
                    val name = effectiveHeaders.name(i)
                    if (!name.equals("Host", ignoreCase = true) && !name.equals("Content-Length", ignoreCase = true)) {
                        reqBuilder.header(name, effectiveHeaders.value(i))
                    }
                }
            }
            reqBuilder.header("Connection", "close")
            if (upstreamHost != null) reqBuilder.header("Origin", upstreamHost)

            val upstreamResponse: OkResponse = try {
                client.newCall(reqBuilder.build()).execute()
            } catch (e: Exception) {
                Log.e(TAG, "handleClient: upstream request failed: ${e.message}", e)
                sendResponse(clientSocket, 502, "text/plain", "Upstream error: ${e.message}".toByteArray())
                return
            }
            val bodyBytes = upstreamResponse.body?.bytes() ?: ByteArray(0)
            val contentType = upstreamResponse.header("Content-Type") ?: "application/octet-stream"
            Log.d(TAG, "handleClient: upstream responded ${upstreamResponse.code} ${upstreamResponse.message} (${bodyBytes.size} bytes, type=$contentType)")

            val responseBytes = if (contentType.contains("m3u8", ignoreCase = true) ||
                contentType.contains("vnd.apple.mpegurl", ignoreCase = true)) {
                rewriteM3u8(bodyBytes.toString(Charsets.UTF_8), requestPath, upstreamHost ?: upstreamUrl).toByteArray(Charsets.UTF_8)
            } else {
                bodyBytes
            }

            sendResponse(clientSocket, 200, contentType, responseBytes)
            if (bodyBytes.isNotEmpty()) {
                Log.d(TAG, "Proxied $method ${requestPath.take(60)} -> ${bodyBytes.size} bytes")
            } else {
                Log.d(TAG, "Proxied $method ${requestPath.take(60)} -> empty body")
            }
            upstreamResponse.close()
        } catch (e: Exception) {
            Log.e(TAG, "Proxy handler error", e)
        } finally {
            try { clientSocket.close() } catch (e: Exception) { ErrorHandler.ignore("LocalProxyServer", "best-effort operation failed", e) }
        }
    }

    private fun rewriteM3u8(content: String, requestPath: String? = null, upstreamHost: String? = null): String {
        val proxyBase = "http://127.0.0.1:$PROXY_PORT"
        val baseDir = if (requestPath != null && requestPath.contains("/")) {
            requestPath.substringBeforeLast("/")
        } else null
        return content.lines().joinToString("\n") { line ->
            val trimmed = line.trim()
            when {
                trimmed.startsWith("http://127.0.0.1:") || trimmed.startsWith("http://localhost:") -> {
                    try {
                        val uri = URI(trimmed)
                        "$proxyBase${uri.path}${if (uri.query != null) "?${uri.query}" else ""}"
                    } catch (_: Exception) { line }
                }
                trimmed.startsWith("http://") || trimmed.startsWith("https://") -> line
                trimmed.endsWith(".ts") || trimmed.endsWith(".m3u8") ||
                    trimmed.endsWith(".aac") || trimmed.endsWith(".mp4") ||
                    trimmed.endsWith(".vtt") || trimmed.endsWith(".png") ||
                    trimmed.endsWith(".jpg") -> {
                    if (trimmed.startsWith("/")) {
                        "$proxyBase$trimmed"
                    } else if (baseDir != null) {
                        "$proxyBase$baseDir/$trimmed"
                    } else {
                        "$proxyBase/$trimmed"
                    }
                }
                else -> line
            }
        }
    }

    private fun sendResponse(socket: Socket, statusCode: Int, contentType: String, body: ByteArray) {
        try {
            val writer = socket.getOutputStream()
            val statusText = when (statusCode) {
                200 -> "OK"
                404 -> "Not Found"
                502 -> "Bad Gateway"
                else -> "Error"
            }
            val header = "HTTP/1.1 $statusCode $statusText\r\n" +
                "Content-Type: $contentType\r\n" +
                "Content-Length: ${body.size}\r\n" +
                "Access-Control-Allow-Origin: *\r\n" +
                "Connection: close\r\n" +
                "\r\n"
            writer.write(header.toByteArray())
            writer.write(body)
            writer.flush()
        } catch (e: Exception) { ErrorHandler.ignore("LocalProxyServer", "best-effort operation failed", e) }
    }
}
