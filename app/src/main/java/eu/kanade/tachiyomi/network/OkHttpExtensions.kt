package eu.kanade.tachiyomi.network

import okhttp3.Call
import okhttp3.Callback
import okhttp3.Response
import rx.Observable
import java.io.IOException

fun Call.asObservableSuccess(): Observable<Response> {
    return Observable.create { subscriber ->
        subscriber.add(rx.subscriptions.Subscriptions.create { cancel() })

        enqueue(object : Callback {
            override fun onResponse(call: Call, response: Response) {
                if (subscriber.isUnsubscribed) {
                    response.close()
                    return
                }
                
                if (!response.isSuccessful) {
                    response.close()
                    subscriber.onError(Exception("HTTP error ${response.code}"))
                    return
                }

                try {
                    subscriber.onNext(response)
                    subscriber.onCompleted()
                } catch (e: Exception) {
                    subscriber.onError(e)
                }
            }

            override fun onFailure(call: Call, e: IOException) {
                if (!subscriber.isUnsubscribed) {
                    subscriber.onError(e)
                }
            }
        })
    }
}
