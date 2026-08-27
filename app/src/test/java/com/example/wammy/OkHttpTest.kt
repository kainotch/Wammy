package com.example.wammy

import org.junit.Test
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.ConnectionSpec
import java.util.concurrent.TimeUnit

class OkHttpTest {
    @Test
    fun testCloudflare() {
        try {
            val client = OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .connectionSpecs(listOf(ConnectionSpec.MODERN_TLS, ConnectionSpec.COMPATIBLE_TLS, ConnectionSpec.CLEARTEXT))
                .build()

            val request = Request.Builder()
                .url("https://1stkissnovel.org/")
                .header("User-Agent", "Mozilla/5.0 (Linux; Android 13; Pixel 7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/116.0.0.0 Mobile Safari/537.36")
                .header("Accept", "*/*")
                .header("Accept-Language", "*")
                .header("Sec-Fetch-Mode", "cors")
                .header("Connection", "keep-alive")
                .build()

            println("Sending request to ${request.url}")
            val response = client.newCall(request).execute()
            println("Response code: ${response.code}")
            println("Response msg: ${response.message}")
            println("Response headers: ${response.headers}")
        } catch (e: Throwable) {
            e.printStackTrace()
        }
    }
}
