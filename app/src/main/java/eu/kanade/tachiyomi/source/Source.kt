// Created by Notch
package eu.kanade.tachiyomi.source
import eu.kanade.tachiyomi.source.model.*
import okhttp3.Response
import rx.Observable
import com.example.wammy.extension.awaitFirst
import kotlinx.coroutines.runBlocking

interface Source {
    val id: Long
    val name: String
    val lang: String
    
    @Suppress("DEPRECATION")
    fun fetchPopularManga(page: Int): Observable<MangasPage> = Observable.fromCallable {
        runBlocking { getPopularManga(page) }
    }
    
    @Suppress("DEPRECATION")
    fun fetchSearchManga(page: Int, query: String, filters: FilterList): Observable<MangasPage> = Observable.fromCallable {
        runBlocking { getSearchManga(page, query, filters) }
    }
    
    @Suppress("DEPRECATION")
    fun fetchLatestUpdates(page: Int): Observable<MangasPage> = Observable.fromCallable {
        runBlocking { getLatestUpdates(page) }
    }
    
    @Suppress("DEPRECATION")
    fun fetchMangaDetails(manga: SManga): Observable<SManga> = Observable.fromCallable {
        runBlocking { getMangaDetails(manga) }
    }
    
    @Suppress("DEPRECATION")
    fun fetchChapterList(manga: SManga): Observable<List<SChapter>> = Observable.fromCallable {
        runBlocking { getChapterList(manga) }
    }
    
    @Suppress("DEPRECATION")
    fun fetchPageList(chapter: SChapter): Observable<List<Page>> = Observable.fromCallable {
        runBlocking { getPageList(chapter) }
    }

    @Suppress("DEPRECATION")
    fun fetchImageUrl(page: Page): Observable<String> = Observable.fromCallable {
        runBlocking { getImageUrl(page) }
    }

    @Suppress("DEPRECATION")
    fun fetchImage(page: Page): Observable<Response> = Observable.fromCallable {
        runBlocking { getImage(page) }
    }

    suspend fun getPopularManga(page: Int): MangasPage = fetchPopularManga(page).awaitFirst()
    suspend fun getSearchManga(page: Int, query: String, filters: FilterList): MangasPage = fetchSearchManga(page, query, filters).awaitFirst()
    suspend fun getLatestUpdates(page: Int): MangasPage = fetchLatestUpdates(page).awaitFirst()
    suspend fun getMangaDetails(manga: SManga): SManga = fetchMangaDetails(manga).awaitFirst()
    suspend fun getChapterList(manga: SManga): List<SChapter> = fetchChapterList(manga).awaitFirst()
    suspend fun getPageList(chapter: SChapter): List<Page> = fetchPageList(chapter).awaitFirst()
    suspend fun getImageUrl(page: Page): String = fetchImageUrl(page).awaitFirst()
    suspend fun getImage(page: Page): Response = fetchImage(page).awaitFirst()
}
