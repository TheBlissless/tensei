@file:JvmName("RequestsKt")

package eu.kanade.tachiyomi.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.CacheControl
import okhttp3.Headers
import okhttp3.HttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response

fun GET(url: String, headers: Headers = Headers.Builder().build(), cache: CacheControl? = null): Request {
    return Request.Builder()
        .url(url)
        .headers(headers)
        .apply { if (cache != null) cacheControl(cache) }
        .get()
        .build()
}

fun GET(url: HttpUrl, headers: Headers = Headers.Builder().build(), cache: CacheControl? = null): Request {
    return Request.Builder()
        .url(url)
        .headers(headers)
        .apply { if (cache != null) cacheControl(cache) }
        .get()
        .build()
}

fun POST(
    url: String,
    headers: Headers = Headers.Builder().build(),
    body: RequestBody? = null,
    cache: CacheControl? = null,
): Request {
    val requestBody = body ?: "".toRequestBody("application/x-www-form-urlencoded".toMediaType())
    return Request.Builder()
        .url(url)
        .headers(headers)
        .apply { if (cache != null) cacheControl(cache) }
        .post(requestBody)
        .build()
}

suspend fun OkHttpClient.newCachelessCallWithProgress(
    request: Request,
    listener: ProgressListener,
): Response {
    return withContext(Dispatchers.IO) {
        newCall(request).awaitSuccess()
    }
}

@Suppress("UNUSED_PARAMETER")
suspend fun OkHttpClient.get(
    url: String,
    headers: Headers = Headers.Builder().build(),
    cacheControl: CacheControl? = null,
): Response {
    return withContext(Dispatchers.IO) { newCall(GET(url, headers = headers, cache = cacheControl)).await() }
}

@Suppress("UNUSED_PARAMETER")
suspend fun OkHttpClient.get(
    url: HttpUrl,
    headers: Headers = Headers.Builder().build(),
    cacheControl: CacheControl? = null,
): Response {
    return withContext(Dispatchers.IO) { newCall(GET(url, headers = headers, cache = cacheControl)).await() }
}

interface ProgressListener {
    fun update(bytesRead: Long, contentLength: Long, done: Boolean)
}
