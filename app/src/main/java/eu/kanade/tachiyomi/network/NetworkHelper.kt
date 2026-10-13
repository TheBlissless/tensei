package eu.kanade.tachiyomi.network

import android.content.Context
import eu.kanade.tachiyomi.network.interceptor.CloudflareInterceptor
import eu.kanade.tachiyomi.network.interceptor.UncaughtExceptionInterceptor
import eu.kanade.tachiyomi.network.interceptor.UserAgentInterceptor
import okhttp3.Cache
import okhttp3.OkHttpClient
import okhttp3.Protocol
import java.io.File
import java.security.KeyStore
import java.security.SecureRandom
import java.security.cert.X509Certificate
import java.util.concurrent.TimeUnit
import javax.net.ssl.HttpsURLConnection
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.TrustManagerFactory
import javax.net.ssl.X509TrustManager

class NetworkHelper private constructor(context: Context) {
    private val appContext = context.applicationContext

    val cookieJar = AndroidCookieJar()

    val client: OkHttpClient
    val cloudflareClient: OkHttpClient
    val trustAllClient: OkHttpClient

    init {
        client = OkHttpClient.Builder()
            .addInterceptor(UncaughtExceptionInterceptor())
            .addInterceptor(UserAgentInterceptor(::defaultUserAgentProvider))
            .addInterceptor(CloudflareInterceptor(appContext, cookieJar, ::defaultUserAgentProvider))
            .addInterceptor { chain ->
                val request = chain.request()
                val host = request.url.host
                // Ensure Referer is always set for aniwave sites (required by their API)
                val newRequest = if (!request.headers.names().any { it.equals("Referer", ignoreCase = true) }
                    && (host.contains("animewave") || host.contains("aniwave"))) {
                    request.newBuilder()
                        .header("Referer", "https://$host/")
                        .build()
                } else {
                    request
                }
                val response = chain.proceed(newRequest)
                if (response.code == 500) {
                    // HTTP 500 logged silently
                }
                response
            }
            .cookieJar(cookieJar)
            .cache(Cache(File(appContext.cacheDir, "http_cache"), 150L * 1024 * 1024))
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
        cloudflareClient = client

        val defaultTmFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm()).apply {
            init(null as KeyStore?)
        }
        val defaultTrustManager = defaultTmFactory.trustManagers.filterIsInstance<X509TrustManager>().first()
        val trustAllManager = object : X509TrustManager {
            override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
            override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
            override fun getAcceptedIssuers(): Array<X509Certificate> = defaultTrustManager.acceptedIssuers
        }
        val sslContext = SSLContext.getInstance("TLS").apply {
            init(null, arrayOf<TrustManager>(trustAllManager), SecureRandom())
        }
        SSLContext.setDefault(sslContext)
        HttpsURLConnection.setDefaultSSLSocketFactory(sslContext.socketFactory)
        HttpsURLConnection.setDefaultHostnameVerifier { _, _ -> true }
        trustAllClient = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .callTimeout(120, TimeUnit.SECONDS)
            .sslSocketFactory(sslContext.socketFactory, trustAllManager)
            .hostnameVerifier { _, _ -> true }
            .cookieJar(cookieJar)
            .protocols(listOf(Protocol.HTTP_1_1))
            .build()
    }

    fun defaultUserAgentProvider(): String {
        return "Mozilla/5.0 (Android 14; Mobile; rv:120.0) Gecko/120.0 Firefox/120.0"
    }

    companion object {
        @Volatile
        private var instance: NetworkHelper? = null

        @Volatile
        private var appContext: Context? = null

        fun getInstance(context: Context): NetworkHelper {
            appContext = context.applicationContext
            return getInstance()
        }

        fun getInstance(): NetworkHelper {
            val ctx = appContext ?: throw IllegalStateException(
                "NetworkHelper not initialized; call getInstance(context) first",
            )
            return instance ?: synchronized(this) {
                instance ?: NetworkHelper(ctx).also { instance = it }
            }
        }
    }
}