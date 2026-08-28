package com.example.wammy

import android.app.Application
import coil.ImageLoader
import coil.Coil
import eu.kanade.tachiyomi.network.NetworkHelper

class WammyApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        // Clean up any leftover reader cache from previous sessions that were forcefully killed
        kotlinx.coroutines.GlobalScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val cacheDir = java.io.File(cacheDir, "reader_cache")
                if (cacheDir.exists()) {
                    cacheDir.deleteRecursively()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }


        try {
            java.security.Security.insertProviderAt(org.conscrypt.Conscrypt.newProvider(), 1)
        } catch (e: Exception) {
            android.util.Log.e("Wammy", "Failed to insert Conscrypt", e)
        }

        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, exception ->
            if (thread.name.startsWith("RxIoScheduler") || 
                thread.name.startsWith("RxComputation") ||
                thread.name.startsWith("RxNewThread")) {
                android.util.Log.e("RxJava", "Caught RxJava scheduler crash (suppressed)", exception)
                return@setDefaultUncaughtExceptionHandler
            }
            try {
                val trace = android.util.Log.getStackTraceString(exception)
                val file = java.io.File(filesDir, "crash_log.txt")
                file.writeText(trace)
            } catch (e: Exception) {
            }
            defaultHandler?.uncaughtException(thread, exception)
        }

        try {
            rx.plugins.RxJavaHooks.setOnError { e ->
                android.util.Log.e("RxJava", "Undelivered RxJava error (suppressed): ${e?.message}")
            }
        } catch (e: Throwable) {
            try {
                rx.plugins.RxJavaPlugins.getInstance().registerErrorHandler(object : rx.plugins.RxJavaErrorHandler() {
                    override fun handleError(e: Throwable?) {
                        android.util.Log.e("RxJava", "Caught unhandled RxJava error", e)
                    }
                })
            } catch (_: Throwable) {}
        }

        NetworkHelper.init(this)
        
        val coilClient = NetworkHelper.client.newBuilder()
            .addInterceptor { chain ->
                val request = chain.request()
                val url = request.url
                val newRequest = request.newBuilder()
                    .header("Referer", "${url.scheme}://${url.host}/")
                    .build()
                chain.proceed(newRequest)
            }
            .build()
            
        Coil.setImageLoader(ImageLoader.Builder(this).okHttpClient { coilClient }.build())
        
        AppContainer.init(this)
    }

    override fun getPackageName(): String {
        try {
            val stackTrace = Thread.currentThread().stackTrace
            val isChromiumCall = stackTrace.any { trace ->
                trace.className.lowercase() in setOf("org.chromium.base.buildinfo", "org.chromium.base.apkinfo") &&
                    trace.methodName.lowercase() in setOf("getall", "getpackagename", "<init>")
            }

            if (isChromiumCall) {
                return runCatching { packageManager.getPackageInfo("com.android.chrome", 0) }
                    .recoverCatching { packageManager.getPackageInfo("com.android.settings", 0) }
                    .recoverCatching { packageManager.getPackageInfo("com.google.android.youtube.tv", 0) }
                    .fold(
                        onSuccess = { it.packageName },
                        onFailure = { packageManager.getInstalledPackages(0).random().packageName }
                    )
            }
        } catch (_: Exception) {
        }
        return super.getPackageName()
    }
}
