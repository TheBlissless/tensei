package eu.kanade.tachiyomi.network.interceptor

import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException

/**
 * Catches any uncaught exceptions from later in the chain and rethrows as a non-fatal
 * IOException to avoid catastrophic failure.
 *
 * This should be the first interceptor in the client. Keiyoushi extension runtimes require
 * an interceptor of this type to be present in the host's default client.
 */
class UncaughtExceptionInterceptor : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        return try {
            chain.proceed(chain.request())
        } catch (e: Exception) {
            if (e is IOException) {
                throw e
            } else {
                throw IOException(e)
            }
        }
    }
}
