package com.example.wammy.lnreader.plugin.plugins

import com.example.wammy.lnreader.model.ChapterItem
import com.example.wammy.lnreader.model.LNNovel
import com.example.wammy.lnreader.model.SourceNovel
import com.example.wammy.lnreader.plugin.LNPlugin
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class NovelFirePlugin : LNPlugin() {
    override val id = "novelfire"
    override val name = "Novel Fire"
    override val site = "https://novelfire.net"
    override val version = "1.0.0"
    override val lang = "en"

    override suspend fun popularNovels(page: Int, showLatest: Boolean): List<LNNovel> = withContext(Dispatchers.IO) {
        val url = if (showLatest) "\$site/novels/new?page=\$page" else "\$site/novels/hot?page=\$page"
        val html = fetchText(url)
        val doc = parseHTML(html)

        doc.select(".novel-list .novel-item").map { el ->
            LNNovel(
                title = el.select(".novel-title").text().trim(),
                coverUrl = el.select("img").attr("abs:src"),
                url = el.select("a").first()?.attr("href")?.removePrefix(site) ?: ""
            )
        }
    }

    override suspend fun parseNovel(path: String): SourceNovel = withContext(Dispatchers.IO) {
        val html = fetchText("\$site\$path")
        val doc = parseHTML(html)

        val name = doc.select("h1.novel-title").text()
        val cover = doc.select(".novel-cover img").attr("abs:src")
        val author = doc.select(".author span").text()
        val genres = doc.select(".categories li").joinToString(", ") { it.text() }
        val summary = doc.select(".summary .content").text()
        val status = doc.select(".header-stats span:contains(Status)").next().text()

        val chapters = doc.select(".chapter-list li").mapIndexed { index, el ->
            val link = el.select("a")
            ChapterItem(
                name = link.text().trim(),
                path = link.attr("href").removePrefix(site),
                releaseTime = el.select(".chapter-update").text(),
                chapterNumber = index.toDouble() + 1
            )
        }

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

        val content = doc.select("#chapter-container").html()
        if (content.isEmpty()) doc.select(".chapter-content").html() else content
    }

    override suspend fun searchNovels(term: String, page: Int): List<LNNovel> = withContext(Dispatchers.IO) {
        val url = "\$site/search?q=\${term.replace(' ', '+')}&page=\$page"
        val html = fetchText(url)
        val doc = parseHTML(html)

        doc.select(".novel-list .novel-item").map { el ->
            LNNovel(
                title = el.select(".novel-title").text().trim(),
                coverUrl = el.select("img").attr("abs:src"),
                url = el.select("a").first()?.attr("href")?.removePrefix(site) ?: ""
            )
        }
    }
}
