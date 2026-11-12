package com.blissless.tensei.network

import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.Channel.Factory.UNLIMITED
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.net.URL
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import javax.net.ssl.HttpsURLConnection
import kotlin.time.Duration.Companion.milliseconds

/**
 * Result wrapper for GraphQL requests with metadata
 */
data class GraphQLResult<T>(
    val data: T?,
    val error: GraphQLError?,
    val fromCache: Boolean,
    val retryCount: Int
)

data class GraphQLError(
    val message: String,
    val code: Int? = null,
    val isRateLimit: Boolean = false,
    val retryAfter: Long? = null
)

/**
 * Configuration for the GraphQL client
 */
data class GraphQLConfig(
    val maxConcurrentRequests: Int = 5,
    val minRequestIntervalMs: Long = 100L,
    val defaultTimeout: Int = 30000,
    /**
     * Hard wall-clock budget PER REQUEST, rate-limit wait and retries included. Once it
     * expires the request resolves with a timeout error instead of leaving the calling
     * screen spinning forever. Generous enough to cover a 429 back-off plus one retry.
     */
    val requestDeadlineMs: Long = 60_000L,
    val maxRetries: Int = 3,
    val baseRetryDelayMs: Long = 1000L,
    val maxRetryDelayMs: Long = 30000L,
    val cacheDurationMs: Long = 5 * 60 * 1000L, // 5 minutes for public data
    val userDataCacheDurationMs: Long = 60 * 60 * 1000L, // 1 hour for user/authenticated data
    val maxCacheSize: Int = 200
)

/**
 * High-performance GraphQL client with:
 * - Request queue with limited concurrency
 * - Automatic rate limit handling (HTTP 429)
 * - Request deduplication
 * - In-memory caching
 * - Exponential backoff retries
 * - Client ID rotation
 */
class GraphQLClient(
    private val endpoint: String = Endpoints.AniList.GRAPHQL,
    private val config: GraphQLConfig = GraphQLConfig()
) {
    companion object {
        /**
         * Process-wide shared rate-limit state. Multiple GraphQLClient instances (anime + manga)
         * share this so that when one gets a 429, the other also backs off. AniList rate-limits
         * per-IP, not per-client-id, so this is the correct behavior.
         */
        @Volatile
        private var sharedRateLimitState: RateLimitState? = null

        internal fun getSharedRateLimitState(): RateLimitState {
            return sharedRateLimitState ?: synchronized(this) {
                sharedRateLimitState ?: RateLimitState().also { sharedRateLimitState = it }
            }
        }
    };

    // Request queue channel (unlimited buffer)
    private val requestChannel = Channel<QueuedRequest<*>>(UNLIMITED)

    // Concurrency limiter
    private val concurrencySemaphore = Semaphore(config.maxConcurrentRequests)

    // Rate limit state — shared across all GraphQLClient instances via the companion object
    // so that when anime gets a 429, manga also backs off (AniList rate-limits per-IP).
    private val rateLimitState: RateLimitState get() = getSharedRateLimitState()

    // Response cache
    private val responseCache = ConcurrentHashMap<String, CacheEntry>()

    // Pending requests for deduplication
    private val pendingRequests = ConcurrentHashMap<String, Deferred<String?>>()

    // Client ID rotation
    private val clientIdCounter = AtomicInteger(0)

    // Minimum interval tracking
    @Volatile
    private var lastRequestTime = 0L
    private val intervalLock = Any()

    // Queue processing job
    private var queueProcessorJob: Job? = null

    // Scope for queue processing
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    init {
        startQueueProcessor()
    }

    fun getConfig() = config

    /**
     * Start the request queue processor.
     *
     * Each queued request is launched into its own coroutine; the concurrency semaphore
     * bounds how many actually run at once. The previous implementation consumed the
     * channel SERIALLY (one `for` loop calling `processRequest` in place), so every
     * AniList call in the app waited for all earlier ones — a single slow or rate-limited
     * request stalled every screen behind it ("loads forever" bug).
     */
    private fun startQueueProcessor() {
        queueProcessorJob = scope.launch {
            for (request in requestChannel) {
                scope.launch { processRequest(request) }
            }
        }
    }

    /**
     * Process a single request from the queue under the concurrency semaphore, with a hard
     * deadline so no request can spin a screen forever.
     */
    private suspend fun <T> processRequest(request: QueuedRequest<T>) {
        concurrencySemaphore.withPermit {
            var timedOut = false
            val result: GraphQLResult<T> = try {
                withTimeoutOrNull(config.requestDeadlineMs) {
                    // The rate-limit wait and retry back-offs live INSIDE the deadline so the
                    // total wait is bounded no matter what AniList does.
                    waitForRateLimit()
                    applyMinInterval()
                    executeRequestWithRetry(request)
                } ?: run {
                    timedOut = true
                    GraphQLResult(
                        data = null,
                        error = GraphQLError("Request timed out after ${config.requestDeadlineMs / 1000}s"),
                        fromCache = false,
                        retryCount = config.maxRetries
                    )
                }
            } catch (e: Exception) {
                request.onRawResponse(null)
                GraphQLResult(
                    data = null,
                    error = GraphQLError(e.message ?: "Unknown error"),
                    fromCache = false,
                    retryCount = 0
                )
            }
            if (timedOut) request.onRawResponse(null)
            request.continuation.resumeSafe(result)
        }
    }

    /**
     * Execute request with retry logic.
     *
     * Retries are reserved for TRANSIENT failures — rate limits (429), network errors
     * (no HTTP code) and 5xx server errors. Client errors (other 4xx) fail immediately:
     * the same request would just fail again.
     */
    private suspend fun <T> executeRequestWithRetry(request: QueuedRequest<T>): GraphQLResult<T> {
        var lastError: GraphQLError? = null
        var retryCount = 0

        repeat(config.maxRetries + 1) { attempt ->
            if (attempt > 0) {
                val delay = calculateBackoffDelay(attempt - 1, lastError?.retryAfter)
                kotlinx.coroutines.delay(delay.milliseconds)
            }

            val response = executeHttpRequest(request)

            when {
                response.isRateLimited -> {
                    lastError = response.error
                    handleRateLimit(response.retryAfter)
                    // Continue to retry
                }
                response.isSuccess -> {
                    // Hand the raw body to duplicate waiters before parsing (the parser also
                    // caches it for future calls).
                    request.onRawResponse(response.body)
                    val parsed = request.parser(response.body!!)
                    return GraphQLResult(
                        data = parsed,
                        error = null,
                        fromCache = false,
                        retryCount = attempt
                    )
                }
                else -> {
                    val code = response.error?.code
                    if (code != null && code in 400..499) {
                        // Non-retriable client error
                        request.onRawResponse(null)
                        return GraphQLResult(
                            data = null,
                            error = response.error,
                            fromCache = false,
                            retryCount = attempt
                        )
                    }
                    // Transient failure (network error or 5xx) — retry with back-off.
                    lastError = response.error
                }
            }
            retryCount = attempt + 1
        }

        request.onRawResponse(null)
        return GraphQLResult(
            data = null,
            error = lastError ?: GraphQLError("Max retries exceeded"),
            fromCache = false,
            retryCount = retryCount
        )
    }

    /**
     * Execute the actual HTTP request
     */
    private suspend fun <T> executeHttpRequest(request: QueuedRequest<T>): HttpResponse {
        return withContext(Dispatchers.IO) {
            val url = URL(endpoint)
            val connection = url.openConnection() as HttpsURLConnection

            try {
                connection.requestMethod = "POST"
                connection.setRequestProperty("Content-Type", "application/json")
                connection.setRequestProperty("Accept", "application/json")

                // Add auth if required
                if (request.requiresAuth && request.authToken != null) {
                    connection.setRequestProperty("Authorization", "Bearer ${request.authToken}")
                }

                // Rotate client ID
                val clientIdIndex = request.clientIdIndex.getAndIncrement() % request.clientIds.size
                connection.setRequestProperty("X-Client-Id", request.clientIds[clientIdIndex])

                connection.connectTimeout = 10_000 // Fail fast on unreachable hosts; retries cover the rest.
                connection.readTimeout = config.defaultTimeout
                connection.doOutput = true

                // Build request body
                val body = buildRequestBody(request.query, request.variables)
                connection.outputStream.use { it.write(body.toByteArray()) }

                when (val responseCode = connection.responseCode) {
                    429 -> {
                        val retryAfter = connection.getHeaderField("Retry-After")?.toLongOrNull()
                        HttpResponse(
                            isRateLimited = true,
                            retryAfter = retryAfter,
                            error = GraphQLError(
                                message = "Rate limited",
                                code = 429,
                                isRateLimit = true,
                                retryAfter = retryAfter
                            )
                        )
                    }
                    200 -> {
                        val responseBody = connection.inputStream.bufferedReader().readText()
                        HttpResponse(
                            isSuccess = true,
                            body = responseBody
                        )
                    }
                    else -> {
                        val errorBody = connection.errorStream?.bufferedReader()?.readText()
                        HttpResponse(
                            error = GraphQLError(
                                message = "HTTP $responseCode: ${errorBody?.take(200)}",
                                code = responseCode
                            )
                        )
                    }
                }
            } catch (e: Exception) {
                HttpResponse(
                    error = GraphQLError(message = e.message ?: "Network error")
                )
            } finally {
                connection.disconnect()
            }
        }
    }

    /**
     * Handle rate limit by updating state
     */
    private fun handleRateLimit(retryAfter: Long?) {
        synchronized(rateLimitState) {
            rateLimitState.isLimited = true
            rateLimitState.lastLimitedTime = System.currentTimeMillis()
            rateLimitState.retryAfterMs = (retryAfter ?: 60) * 1000 // Default to 60s if not specified
        }
    }

    /**
     * Wait if we're rate limited
     */
    private suspend fun waitForRateLimit() {
        val delayNeeded = synchronized(rateLimitState) {
            if (rateLimitState.isLimited) {
                val elapsed = System.currentTimeMillis() - rateLimitState.lastLimitedTime
                val remaining = rateLimitState.retryAfterMs - elapsed
                if (remaining > 0) {
                    remaining
                } else {
                    rateLimitState.isLimited = false
                    0L
                }
            } else {
                0L
            }
        }

        if (delayNeeded > 0) {
            kotlinx.coroutines.delay(delayNeeded.milliseconds)
            synchronized(rateLimitState) {
                rateLimitState.isLimited = false
            }
        }
    }

    /**
     * Apply minimum interval between requests
     */
    private suspend fun applyMinInterval() {
        val delayNeeded = synchronized(intervalLock) {
            val now = System.currentTimeMillis()
            val elapsed = now - lastRequestTime
            if (elapsed < config.minRequestIntervalMs) {
                config.minRequestIntervalMs - elapsed
            } else {
                0L
            }
        }

        if (delayNeeded > 0) {
            kotlinx.coroutines.delay(delayNeeded.milliseconds)
        }

        synchronized(intervalLock) {
            lastRequestTime = System.currentTimeMillis()
        }
    }

    /**
     * Calculate backoff delay with exponential increase
     */
    private fun calculateBackoffDelay(attempt: Int, retryAfter: Long?): Long {
        // Use Retry-After header if available
        if (retryAfter != null && retryAfter > 0) {
            return retryAfter * 1000
        }

        // Exponential backoff: baseDelay * 2^attempt with jitter
        val baseDelay = config.baseRetryDelayMs
        val exponentialDelay = baseDelay * (1 shl attempt)
        val jitter = (0..500).random().toLong()
        return minOf(exponentialDelay + jitter, config.maxRetryDelayMs)
    }

    /**
     * Build JSON request body
     */
    private fun buildRequestBody(query: String, variables: Map<String, Any?>): String {
        val variablesJson = if (variables.isEmpty()) "{}" else {
            variables.entries.joinToString(",", "{", "}") { (key, value) ->
                "\"$key\":${serializeValue(value)}"
            }
        }

        val escapedQuery = query
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t")

        return "{\"query\":\"$escapedQuery\",\"variables\":$variablesJson}"
    }

    private fun serializeValue(value: Any?): String = when (value) {
        is String -> "\"$value\""
        is Number -> value.toString()
        is Boolean -> value.toString()
        null -> "null"
        is List<*> -> value.joinToString(",", "[", "]") { serializeValue(it) }
        else -> "\"$value\""
    }

    /**
     * Generate cache key for request
     */
    private fun generateCacheKey(query: String, variables: Map<String, Any?>): String {
        val varsStr = variables.entries
            .sortedBy { it.key }
            .joinToString(",") { "${it.key}=${it.value}" }
        return "${query.hashCode()}_$varsStr"
    }

    /**
     * Get cached response if valid
     */
    private fun getCachedResponse(cacheKey: String, cacheDurationMs: Long = config.cacheDurationMs): String? {
        val entry = responseCache[cacheKey] ?: return null
        val now = System.currentTimeMillis()
        if (now - entry.timestamp < cacheDurationMs) {
            return entry.response
        }
        responseCache.remove(cacheKey)
        return null
    }

    /**
     * Cache response
     */
    private fun cacheResponse(cacheKey: String, response: String) {
        responseCache[cacheKey] = CacheEntry(
            response = response,
            timestamp = System.currentTimeMillis()
        )

        // Prune cache if too large
        if (responseCache.size > config.maxCacheSize) {
            val sortedKeys = responseCache.entries
                .sortedBy { it.value.timestamp }
                .take(responseCache.size - config.maxCacheSize / 2)
                .map { it.key }
            sortedKeys.forEach { responseCache.remove(it) }
        }
    }

    /**
     * Execute a GraphQL query with full optimization
     */
    suspend fun <T> execute(
        query: String,
        variables: Map<String, Any?> = emptyMap(),
        requiresAuth: Boolean = false,
        authToken: String? = null,
        clientIds: List<String>,
        useCache: Boolean = true,
        cacheDurationMs: Long? = null, // Override cache duration
        parser: (String) -> T?
    ): GraphQLResult<T> = suspendCancellableCoroutine { continuation ->
        val cacheKey = generateCacheKey(query, variables)
        val effectiveCacheDuration = cacheDurationMs ?: if (requiresAuth) config.userDataCacheDurationMs else config.cacheDurationMs

        // Check cache first if enabled
        if (useCache) {
            getCachedResponse(cacheKey, effectiveCacheDuration)?.let { cached ->
                val parsed = parser(cached)
                if (parsed != null) {
                    continuation.resumeSafe(
                        GraphQLResult(
                            data = parsed,
                            error = null,
                            fromCache = true,
                            retryCount = 0
                        )
                    )
                    return@suspendCancellableCoroutine
                }
            }
        }

        // Check for pending duplicate request
        val existingPending = pendingRequests[cacheKey]
        if (existingPending != null) {
            scope.launch {
                try {
                    val response = existingPending.await()
                    if (response != null) {
                        val parsed = parser(response)
                        continuation.resumeSafe(
                            GraphQLResult(
                                data = parsed,
                                error = null,
                                fromCache = false,
                                retryCount = 0
                            )
                        )
                    } else {
                        // Fall through to queue new request
                        queueRequest(query, variables, requiresAuth, authToken, clientIds, parser, cacheKey, continuation)
                    }
                } catch (_: Exception) {
                    queueRequest(query, variables, requiresAuth, authToken, clientIds, parser, cacheKey, continuation)
                }
            }
            return@suspendCancellableCoroutine
        }

        queueRequest(query, variables, requiresAuth, authToken, clientIds, parser, cacheKey, continuation)
    }

    private fun <T> queueRequest(
        query: String,
        variables: Map<String, Any?>,
        requiresAuth: Boolean,
        authToken: String?,
        clientIds: List<String>,
        parser: (String) -> T?,
        cacheKey: String,
        continuation: CancellableContinuation<GraphQLResult<T>>
    ) {
        // Register the in-flight request so duplicate calls (same query + variables) wait
        // for THIS attempt instead of each issuing their own HTTP request. Previously the
        // deferred was never added, so this dedup path was dead code.
        val deferred: CompletableDeferred<String?> = CompletableDeferred()
        pendingRequests[cacheKey] = deferred

        val request = QueuedRequest(
            query = query,
            variables = variables,
            requiresAuth = requiresAuth,
            authToken = authToken,
            clientIds = clientIds,
            clientIdIndex = clientIdCounter,
            parser = { response ->
                // Cache successful response
                cacheResponse(cacheKey, response)
                parser(response)
            },
            onRawResponse = { raw ->
                // Complete duplicate waiters with the raw body (null = failed attempt),
                // then clear the pending slot so later calls re-queue normally.
                deferred.complete(raw)
                pendingRequests.remove(cacheKey)
            },
            continuation = continuation
        )

        // Queue the request
        requestChannel.trySend(request).isSuccess.let { sent ->
            if (!sent) {
                pendingRequests.remove(cacheKey)
                deferred.complete(null)
                continuation.resumeSafe(
                    GraphQLResult(
                        data = null,
                        error = GraphQLError("Request queue full"),
                        fromCache = false,
                        retryCount = 0
                    )
                )
            }
        }
    }

    /**
     * Clear all caches
     */
    fun clearCache() {
        responseCache.clear()
        pendingRequests.clear()
    }

    /**
     * Resume a continuation with [result], tolerating a continuation that was already
     * resumed or cancelled (caller navigated away / deadline raced a late in-flight
     * result). A raw `resume` would throw IllegalStateException and kill the queue's
     * worker coroutine.
     */
    private fun <T> CancellableContinuation<GraphQLResult<T>>.resumeSafe(result: GraphQLResult<T>) {
        try {
            resume(result) { _, _, _ -> }
        } catch (_: IllegalStateException) {
            // Already resumed/cancelled — nothing to do.
        }
    }

}

// ============================================
// Internal data classes
// ============================================

internal data class RateLimitState(
    var isLimited: Boolean = false,
    var retryAfterMs: Long = 0,
    var lastLimitedTime: Long = 0
)

private data class QueuedRequest<T>(
    val query: String,
    val variables: Map<String, Any?>,
    val requiresAuth: Boolean,
    val authToken: String?,
    val clientIds: List<String>,
    val clientIdIndex: AtomicInteger,
    val parser: (String) -> T?,
    val onRawResponse: (String?) -> Unit = {},
    val continuation: CancellableContinuation<GraphQLResult<T>>
)

private data class CacheEntry(
    val response: String,
    val timestamp: Long
)

private data class HttpResponse(
    val isSuccess: Boolean = false,
    val isRateLimited: Boolean = false,
    val body: String? = null,
    val error: GraphQLError? = null,
    val retryAfter: Long? = null
)


