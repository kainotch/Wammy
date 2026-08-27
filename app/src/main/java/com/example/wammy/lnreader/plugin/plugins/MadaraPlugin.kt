package com.example.wammy.lnreader.plugin.plugins

import com.example.wammy.lnreader.model.ChapterItem
import com.example.wammy.lnreader.model.LNNovel
import com.example.wammy.lnreader.model.SourceNovel
import com.example.wammy.lnreader.plugin.LNPlugin
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.Request

class FirstKissNovelPlugin : LNPlugin() {
    override val id = "1stkissnovel"
    override val name = "FirstKissNovel"
    override val site = "https://1stkissnovel.org"
    override val version = "1.0.0"
    override val lang = "en"

    override suspend fun popularNovels(page: Int, showLatest: Boolean): List<LNNovel> = withContext(Dispatchers.IO) {
        val url = if (showLatest) "\$site/manga-m-orderby=latest" else "\$site/manga-m-orderby=trending"
        // Wait, Madara uses /page/X/?s=&post_type=wp-manga&m_orderby=trending
        val order = if (showLatest) "latest" else "trending"
        val actualUrl = "$site/page/$page/?s=&post_type=wp-manga&m_orderby=$order"
        val html = fetchText(actualUrl)
        val doc = parseHTML(html)

        doc.select(".c-tabs-item__content").map { el ->
            LNNovel(
                title = el.select(".post-title h3 a, .post-title h4 a").text().trim(),
                coverUrl = el.select("img").let { img -> img.attr("data-src").ifEmpty { img.attr("src") } },
                url = el.select(".post-title h3 a, .post-title h4 a").attr("href").removePrefix(site)
            )
        }
    }

    override suspend fun parseNovel(path: String): SourceNovel = withContext(Dispatchers.IO) {
        val html = fetchText("\$site\$path")
        val doc = parseHTML(html)

        val name = doc.select(".post-title h1").text().trim()
        val cover = doc.select(".summary_image img").let { img -> img.attr("data-src").ifEmpty { img.attr("src") } }
        val author = doc.select(".author-content a").joinToString(", ") { it.text() }
        val genres = doc.select(".genres-content a").joinToString(", ") { it.text() }
        val summary = doc.select(".summary__content").text().trim()
        val status = doc.select(".post-status .summary-content").text().trim()

        val mangaId = doc.select(".rating-post-id").attr("value")
        
        val chaptersHtml = if (mangaId.isNotEmpty()) {
            val request = Request.Builder()
                .url("\$site/wp-admin/admin-ajax.php")
                .post(FormBody.Builder().add("action", "manga_get_chapters").add("manga", mangaId).build())
                .build()
            client.newCall(request).execute().body?.string() ?: ""
        } else {
            ""
        }
        
        val chapDoc = parseHTML(chaptersHtml)
        
        val chapters = chapDoc.select(".wp-manga-chapter").mapIndexed { index, el ->
            val link = el.select("a")
            ChapterItem(
                name = link.text().trim(),
                path = link.attr("href").removePrefix(site),
                releaseTime = el.select(".chapter-release-date").text().trim(),
                chapterNumber = index.toDouble() + 1
            )
        }.reversed() // Madara returns newest first, so we reverse it

        SourceNovel(
            path = path,
            name = name,
            cover = cover,
            author = author,
            genres = genres,
            summary = summary,
            status = status,
            chapters = chapters
        )
    }

    override suspend fun parseChapter(path: String): String = withContext(Dispatchers.IO) {
        val html = fetchText("\$site\$path")
        val doc = parseHTML(html)

        val content = doc.select(".text-left, .text-right, .entry-content, .c-blog-post > div > div:nth-child(2)").html()
        content
    }

    override suspend fun searchNovels(term: String, page: Int): List<LNNovel> = withContext(Dispatchers.IO) {
        val url = "\$site/page/\$page/?s=\${term.replace(' ', '+')}&post_type=wp-manga"
        val html = fetchText(url)
        val doc = parseHTML(html)

        doc.select(".c-tabs-item__content").map { el ->
            LNNovel(
                title = el.select(".post-title h3 a, .post-title h4 a").text().trim(),
                coverUrl = el.select("img").let { img -> img.attr("data-src").ifEmpty { img.attr("src") } },
                url = el.select(".post-title h3 a, .post-title h4 a").attr("href").removePrefix(site)
            )
        }
    }
}
