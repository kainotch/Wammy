// Created by Notch
package eu.kanade.tachiyomi.source.online

import eu.kanade.tachiyomi.source.Source
import eu.kanade.tachiyomi.source.model.*
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import rx.Observable
import eu.kanade.tachiyomi.network.asObservableSuccess
import eu.kanade.tachiyomi.network.NetworkHelper
import okhttp3.Headers
import okhttp3.HttpUrl.Companion.toHttpUrl

abstract class HttpSource : Source {
    open val network: NetworkHelper = NetworkHelper
    open val headers: Headers by lazy { headersBuilder().build() }
    
    open val client: OkHttpClient = network.client
    abstract val baseUrl: String
    open val supportsLatest: Boolean = true
    open fun headersBuilder() = Headers.Builder()
    
    // ═══════════════════════════════════════════════════════════
    // URL helpers that extensions call (critical Tachiyomi API)
    // ═══════════════════════════════════════════════════════════
    
    /**
     * Sets the url of the manga without the domain. Used by extensions in parse methods.
     */
    open fun setUrlWithoutDomain(manga: SManga, url: String) {
        manga.url = getUrlWithoutDomain(url)
    }

    open fun setUrlWithoutDomain(chapter: SChapter, url: String) {
        chapter.url = getUrlWithoutDomain(url)
    }

    /**
     * Returns the url of the given string without the domain.
     * e.g. "https://example.com/manga/123" -> "/manga/123"
     */
    protected open fun getUrlWithoutDomain(url: String): String {
        return try {
            val parsed = url.toHttpUrl()
            val path = parsed.encodedPath
            val query = parsed.encodedQuery
            if (query != null) "$path?$query" else path
        } catch (e: Exception) {
            url
        }
    }

    /**
     * Returns the absolute url of the manga.
     */
    open fun getMangaUrl(manga: SManga): String {
        val mangaUrl = manga.url
        return if (mangaUrl.startsWith("http")) mangaUrl else "$baseUrl$mangaUrl"
    }

    open fun getChapterUrl(chapter: SChapter): String {
        val chapterUrl = chapter.url
        return if (chapterUrl.startsWith("http")) chapterUrl else "$baseUrl$chapterUrl"
    }

    // ═══════════════════════════════════════════════════════════
    // Fetch methods with proper RxJava error handling
    // ═══════════════════════════════════════════════════════════

    override fun fetchPopularManga(page: Int): Observable<MangasPage> {
        return client.newCall(popularMangaRequest(page))
            .asObservableSuccess()
            .map { response ->
                popularMangaParse(response)
            }
    }
    
    override fun fetchSearchManga(page: Int, query: String, filters: FilterList): Observable<MangasPage> {
        return client.newCall(searchMangaRequest(page, query, filters))
            .asObservableSuccess()
            .map { response ->
                searchMangaParse(response)
            }
    }
    
    override fun fetchLatestUpdates(page: Int): Observable<MangasPage> {
        return client.newCall(latestUpdatesRequest(page))
            .asObservableSuccess()
            .map { response ->
                latestUpdatesParse(response)
            }
    }
    
    override fun fetchMangaDetails(manga: SManga): Observable<SManga> {
        return client.newCall(mangaDetailsRequest(manga))
            .asObservableSuccess()
            .map { response ->
                mangaDetailsParse(response).apply { initialized = true }
            }
    }
    
    override fun fetchChapterList(manga: SManga): Observable<List<SChapter>> {
        return client.newCall(chapterListRequest(manga))
            .asObservableSuccess()
            .map { response ->
                chapterListParse(response)
            }
    }
    
    override fun fetchPageList(chapter: SChapter): Observable<List<Page>> {
        return client.newCall(pageListRequest(chapter))
            .asObservableSuccess()
            .map { response ->
                pageListParse(response)
            }
    }
    
    // ═══════════════════════════════════════════════════════════
    // Abstract methods that extensions implement
    // ═══════════════════════════════════════════════════════════
    
    abstract fun popularMangaRequest(page: Int): Request
    abstract fun popularMangaParse(response: Response): MangasPage
    
    abstract fun searchMangaRequest(page: Int, query: String, filters: FilterList): Request
    abstract fun searchMangaParse(response: Response): MangasPage
    
    open fun latestUpdatesRequest(page: Int): Request = popularMangaRequest(page)
    open fun latestUpdatesParse(response: Response): MangasPage = popularMangaParse(response)
    
    open fun mangaDetailsRequest(manga: SManga): Request {
        return Request.Builder().url(getMangaUrl(manga)).headers(headers).build()
    }
    abstract fun mangaDetailsParse(response: Response): SManga
    
    open fun chapterListRequest(manga: SManga): Request {
        return Request.Builder().url(getMangaUrl(manga)).headers(headers).build()
    }
    abstract fun chapterListParse(response: Response): List<SChapter>
    
    open fun pageListRequest(chapter: SChapter): Request {
        return Request.Builder().url(getChapterUrl(chapter)).headers(headers).build()
    }
    abstract fun pageListParse(response: Response): List<Page>
    
    open fun imageRequest(page: Page): Request = Request.Builder().url(page.imageUrl!!).headers(headers).build()
    
    override fun fetchImage(page: Page): Observable<Response> {
        return client.newCall(imageRequest(page))
            .asObservableSuccess()
    }
    
    open fun imageUrlRequest(page: Page): Request = Request.Builder().url(page.url).headers(headers).build()
    
    override fun fetchImageUrl(page: Page): Observable<String> {
        return client.newCall(imageUrlRequest(page))
            .asObservableSuccess()
            .map { response ->
                imageUrlParse(response)
            }
    }

    open fun imageUrlParse(response: Response): String = ""
    
    // ═══════════════════════════════════════════════════════════
    // Filter helpers
    // ═══════════════════════════════════════════════════════════
    
    open fun getFilterList(): FilterList = FilterList()
}
