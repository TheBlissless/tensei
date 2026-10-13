package tachiyomi.core.common.util.lang

import kotlinx.coroutines.suspendCancellableCoroutine
import rx.Observable
import rx.Subscriber
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Awaits the single item emitted by an RxJava 1 [Observable]. Mirrors
 * `tachiyomi.core.common.util.lang.awaitSingle` so extension ABI matches.
 */
suspend fun <T> Observable<T>.awaitSingle(): T = suspendCancellableCoroutine { cont ->
    subscribe(
        object : Subscriber<T>() {
            override fun onNext(t: T) {
                if (cont.isActive) cont.resume(t)
            }

            override fun onCompleted() {
                if (cont.isActive) {
                    cont.resumeWithException(
                        NoSuchElementException("Observable completed without emitting a value"),
                    )
                }
            }

            override fun onError(e: Throwable) {
                if (cont.isActive) cont.resumeWithException(e)
            }
        },
    )
}
