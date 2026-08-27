package com.example.wammy

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import eu.kanade.tachiyomi.network.NetworkHelper
import okhttp3.Request
import org.junit.Test
import org.junit.runner.RunWith
import android.util.Log

@RunWith(AndroidJUnit4::class)
class CloudflareTest {
    @Test
    fun testNovelfull() {
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        NetworkHelper.init(appContext)
        
        val request = Request.Builder()
            .url("https://novelfull.com/most-popular?page=1")
            .build()
            
        Log.d("WammyTest", "Executing request to novelfull.com...")
        val response = NetworkHelper.client.newCall(request).execute()
        Log.d("WammyTest", "Response code: ${response.code}")
        
        val cookies = NetworkHelper.client.cookieJar.loadForRequest(request.url)
        Log.d("WammyTest", "Cookies: $cookies")
    }
}
