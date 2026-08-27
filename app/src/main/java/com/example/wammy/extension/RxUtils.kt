// Created by Notch
package com.example.wammy.extension

import kotlinx.coroutines.suspendCancellableCoroutine
import rx.Observable
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

suspend fun <T> Observable<T>.awaitFirst(): T = suspendCancellableCoroutine { cont ->
    val sub = this.first().subscribe(
        { cont.resume(it) },
        { cont.resumeWithException(it) }
    )
    cont.invokeOnCancellation { sub.unsubscribe() }
}
