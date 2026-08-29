package eu.kanade.tachiyomi.network

import java.security.cert.X509Certificate
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager


import android.content.Context
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit
import okhttp3.Protocol
import java.util.Collections

class UserAgentInterceptor : okhttp3.Interceptor {
    override fun intercept(chain: okhttp3.Interceptor.Chain): okhttp3.Response {
        val originalRequest = chain.request()
        val newRequest = originalRequest.newBuilder()
            .header("User-Agent", "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/116.0.0.0 Mobile Safari/537.36")
            .build()
        return chain.proceed(newRequest)
    }
}



fun getUnsafeOkHttpClientBuilder(): OkHttpClient.Builder {
    try {
        val trustAllCerts = arrayOf<TrustManager>(object : X509TrustManager {
            override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
            override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
            override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
        })

        val sslContext = SSLContext.getInstance("SSL")
        sslContext.init(null, trustAllCerts, java.security.SecureRandom())
        val sslSocketFactory = sslContext.socketFactory

        val builder = OkHttpClient.Builder()
        builder.sslSocketFactory(sslSocketFactory, trustAllCerts[0] as X509TrustManager)
        builder.hostnameVerifier { _, _ -> true }
        return builder
    } catch (e: Exception) {
        throw RuntimeException(e)
    }
}

object NetworkHelper {
    private var _client: OkHttpClient? = null
    val cookieJar = AndroidCookieJar()

    fun init(context: Context) {
        if (_client == null) {
            _client = getUnsafeOkHttpClientBuilder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .callTimeout(2, TimeUnit.MINUTES)
                .cookieJar(cookieJar)
                .addInterceptor(eu.kanade.tachiyomi.network.interceptor.UncaughtExceptionInterceptor())
                .addInterceptor(UserAgentInterceptor())
                .addInterceptor(okhttp3.brotli.BrotliInterceptor)
                .addInterceptor(CloudflareInterceptor(context, cookieJar, { "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/116.0.0.0 Mobile Safari/537.36" }))
                .build()
        }
    }

    val client: OkHttpClient 
        get() = _client ?: getUnsafeOkHttpClientBuilder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .callTimeout(2, TimeUnit.MINUTES)
            .cookieJar(cookieJar)
            .addInterceptor(eu.kanade.tachiyomi.network.interceptor.UncaughtExceptionInterceptor())
            .addInterceptor(UserAgentInterceptor())
            .addInterceptor(okhttp3.brotli.BrotliInterceptor)
            .build()
}
