package com.example.wammy.lnreader.plugin

import com.example.wammy.lnreader.model.LNNovel
import com.example.wammy.lnreader.model.SourceNovel
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jsoup.Jsoup
import java.util.concurrent.TimeUnit

abstract class LNPlugin {
    abstract val id: String
    abstract val name: String
    abstract val site: String
    abstract val version: String
    abstract val lang: String

    protected val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .cookieJar(com.example.wammy.lnreader.WebViewCookieJar())
        
        .build()

    protected fun fetchText(url: String, headers: Map<String, String> = emptyMap()): String {
        val requestBuilder = Request.Builder().url(url)
        headers.forEach { (k, v) -> requestBuilder.addHeader(k, v) }
        
        if (!headers.keys.any { it.equals("User-Agent", ignoreCase = true) }) {
            requestBuilder.addHeader("User-Agent", System.getProperty("http.agent") ?: "Mozilla/5.0 (Linux; Android 13; Pixel 7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/116.0.0.0 Mobile Safari/537.36")
        }

        return client.newCall(requestBuilder.build()).execute().use {
            if (!it.isSuccessful) {
                if (it.code == 503) error("Cloudflare protection (503) detected. Try opening in WebView.")
                else error("HTTP Error \${it.code}")
            }
            it.body?.string() ?: error("Empty response from \$url")
        }
    }

    protected fun parseHTML(html: String, baseUri: String = site): org.jsoup.nodes.Document {
        return Jsoup.parse(html, baseUri)
    }

    abstract suspend fun popularNovels(page: Int, showLatest: Boolean = false): List<LNNovel>
    abstract suspend fun parseNovel(path: String): SourceNovel
    abstract suspend fun parseChapter(path: String): String
    abstract suspend fun searchNovels(term: String, page: Int = 1): List<LNNovel>
}
