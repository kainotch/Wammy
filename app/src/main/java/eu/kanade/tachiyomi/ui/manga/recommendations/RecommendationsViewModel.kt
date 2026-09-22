package eu.kanade.tachiyomi.ui.manga.recommendations

import androidx.compose.runtime.Immutable
import androidx.lifecycle.viewModelScope
import eu.kanade.tachiyomi.data.track.TrackerManager
import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.network.POST
import eu.kanade.tachiyomi.network.NetworkHelper
import eu.kanade.tachiyomi.network.awaitSuccess
import eu.kanade.tachiyomi.network.parseAs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.longOrNull
import logcat.LogPriority

import tachiyomi.core.common.util.system.logcat
import mihon.core.viewmodel.StateViewModel
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.toRequestBody
import tachiyomi.core.common.util.lang.launchIO
import tachiyomi.core.common.util.lang.withIOContext
import tachiyomi.domain.manga.interactor.GetManga
import tachiyomi.domain.manga.interactor.NetworkToLocalManga
import tachiyomi.domain.manga.model.Manga
import tachiyomi.domain.track.interactor.GetTracks
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class RecommendationsViewModel(
    private val mangaId: Long,
    private val sourceId: Long,
) : StateViewModel<RecommendationsViewModel.State>(State()) {

    private val getManga: GetManga = Injekt.get()
    private val getTracks: GetTracks = Injekt.get()
    private val networkToLocalManga: NetworkToLocalManga = Injekt.get()
    private val trackerManager: TrackerManager = Injekt.get()
    private val networkHelper: NetworkHelper = Injekt.get()
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    private val client: OkHttpClient get() = networkHelper.client

    companion object {
        const val RECOMMENDS_SOURCE_ID = -1000L
    }

    init {
        viewModelScope.launchIO {
            val manga = getManga.await(mangaId) ?: return@launchIO
            mutableState.update { it.copy(title = manga.title) }

            val tracks = getTracks.await(mangaId)

            // Launch all fetches in parallel
            launch(Dispatchers.IO) { fetchMangaUpdates(manga.title, tracks.find { it.trackerId == 7L }?.remoteId?.toLong()) }
            launch(Dispatchers.IO) { fetchAniList(manga.title, tracks.find { it.trackerId == 2L }?.remoteId?.toLong()) }
            launch(Dispatchers.IO) { fetchMyAnimeList(manga.title, tracks.find { it.trackerId == 1L }?.remoteId?.toLong()) }
        }
    }

    private suspend fun fetchMangaUpdates(title: String, trackedId: Long?) {
        updateSection("MangaUpdates", "Similar titles", SectionResult.Loading)
        updateSection("MangaUpdates ", "Community recommendations", SectionResult.Loading)
        try {
            val seriesId = trackedId ?: run {
                val searchBody = "{\"search\":\"${title.replace("\"", "\\\"")}\",\"stype\":\"title\"}"
                    .toRequestBody("application/json".toMediaType())
                val searchResponse = client.newCall(
                    POST("https://api.mangaupdates.com/v1/series/search", body = searchBody)
                ).awaitSuccess()
                val searchJson = json.parseToJsonElement(searchResponse.body.string()).jsonObject
                val results = searchJson["results"]?.jsonArray
                results?.firstOrNull()?.jsonObject?.get("record")?.jsonObject?.get("series_id")?.jsonPrimitive?.longOrNull
            }

            if (seriesId == null) {
                updateSection("MangaUpdates", "Similar titles", SectionResult.Success(emptyList()))
                updateSection("MangaUpdates ", "Community recommendations", SectionResult.Success(emptyList()))
                return
            }

            val response = client.newCall(
                GET("https://api.mangaupdates.com/v1/series/$seriesId")
            ).awaitSuccess()
            val seriesJson = json.parseToJsonElement(response.body.string()).jsonObject

            // Similar titles
            val categoryRecs = seriesJson["category_recommendations"]?.jsonArray ?: JsonArray(emptyList())
            val similarMangas = categoryRecs.mapNotNull { rec ->
                val obj = rec.jsonObject
                val seriesName = obj["series_name"]?.jsonPrimitive?.contentOrNull ?: return@mapNotNull null
                val seriesUrl = obj["series_url"]?.jsonPrimitive?.contentOrNull ?: ""
                val seriesImage = obj["series_image"]?.jsonObject?.get("url")?.jsonObject?.get("original")?.jsonPrimitive?.contentOrNull ?: ""
                createManga(seriesName, seriesUrl, seriesImage)
            }
            val savedSimilar = if (similarMangas.isNotEmpty()) networkToLocalManga(similarMangas) else emptyList()
            updateSection("MangaUpdates", "Similar titles", SectionResult.Success(savedSimilar))

            // Community recommendations
            val communityRecs = seriesJson["recommendations"]?.jsonArray ?: JsonArray(emptyList())
            val communityMangas = communityRecs.mapNotNull { rec ->
                val obj = rec.jsonObject
                val seriesName = obj["series_name"]?.jsonPrimitive?.contentOrNull ?: return@mapNotNull null
                val seriesUrl = obj["series_url"]?.jsonPrimitive?.contentOrNull ?: ""
                val seriesImage = obj["series_image"]?.jsonObject?.get("url")?.jsonObject?.get("original")?.jsonPrimitive?.contentOrNull ?: ""
                createManga(seriesName, seriesUrl, seriesImage)
            }
            val savedCommunity = if (communityMangas.isNotEmpty()) networkToLocalManga(communityMangas) else emptyList()
            updateSection("MangaUpdates ", "Community recommendations", SectionResult.Success(savedCommunity))
        } catch (e: Exception) {
            logcat(LogPriority.ERROR, e) { "Failed to fetch MangaUpdates recommendations" }
            updateSection("MangaUpdates", "Similar titles", SectionResult.Error(e.message ?: "Unknown error"))
            updateSection("MangaUpdates ", "Community recommendations", SectionResult.Error(e.message ?: "Unknown error"))
        }
    }

    private suspend fun fetchAniList(title: String, trackedId: Long?) {
        updateSection("AniList", "Community recommendations", SectionResult.Loading)
        try {
            val mediaId = trackedId?.toInt() ?: run {
                val searchQuery = """
                    {"query":"query(${'$'}search:String){Page{media(search:${'$'}search,type:MANGA){id title{romaji english native}}}}","variables":{"search":"${title.replace("\"", "\\\"")}"} }
                """.trim()
                val searchResponse = client.newCall(
                    POST("https://graphql.anilist.co", body = searchQuery.toRequestBody("application/json".toMediaType()))
                ).awaitSuccess()
                val searchJson = json.parseToJsonElement(searchResponse.body.string()).jsonObject
                val media = searchJson["data"]?.jsonObject?.get("Page")?.jsonObject?.get("media")?.jsonArray
                media?.firstOrNull()?.jsonObject?.get("id")?.jsonPrimitive?.intOrNull
            }

            if (mediaId == null) {
                updateSection("AniList", "Community recommendations", SectionResult.Success(emptyList()))
                return
            }

            val recsQuery = """
                {"query":"query(${'$'}id:Int){Media(id:${'$'}id,type:MANGA){recommendations(sort:RATING_DESC){edges{node{mediaRecommendation{id title{romaji english native}coverImage{large}siteUrl}}}}}}","variables":{"id":$mediaId}}
            """.trim()
            val recsResponse = client.newCall(
                POST("https://graphql.anilist.co", body = recsQuery.toRequestBody("application/json".toMediaType()))
            ).awaitSuccess()
            val recsJson = json.parseToJsonElement(recsResponse.body.string()).jsonObject
            val edges = recsJson["data"]?.jsonObject
                ?.get("Media")?.jsonObject
                ?.get("recommendations")?.jsonObject
                ?.get("edges")?.jsonArray ?: JsonArray(emptyList())

            val mangas = edges.mapNotNull { edge ->
                val node = edge.jsonObject["node"]?.jsonObject ?: return@mapNotNull null
                val rec = node["mediaRecommendation"]?.jsonObject ?: return@mapNotNull null
                val titleObj = rec["title"]?.jsonObject
                val name = titleObj?.get("english")?.jsonPrimitive?.contentOrNull
                    ?: titleObj?.get("romaji")?.jsonPrimitive?.contentOrNull
                    ?: return@mapNotNull null
                val cover = rec["coverImage"]?.jsonObject?.get("large")?.jsonPrimitive?.contentOrNull ?: ""
                val url = rec["siteUrl"]?.jsonPrimitive?.contentOrNull ?: ""
                createManga(name, url, cover)
            }
            val saved = if (mangas.isNotEmpty()) networkToLocalManga(mangas) else emptyList()
            updateSection("AniList", "Community recommendations", SectionResult.Success(saved))
        } catch (e: Exception) {
            logcat(LogPriority.ERROR, e) { "Failed to fetch AniList recommendations" }
            updateSection("AniList", "Community recommendations", SectionResult.Error(e.message ?: "Unknown error"))
        }
    }

    private suspend fun fetchMyAnimeList(title: String, trackedId: Long?) {
        updateSection("MyAnimeList", "Community recommendations", SectionResult.Loading)
        try {
            val malId = trackedId?.toInt() ?: run {
                val encodedTitle = java.net.URLEncoder.encode(title, "UTF-8")
                val searchResponse = client.newCall(
                    GET("https://api.jikan.moe/v4/manga?q=$encodedTitle&limit=1")
                ).awaitSuccess()
                val searchJson = json.parseToJsonElement(searchResponse.body.string()).jsonObject
                searchJson["data"]?.jsonArray?.firstOrNull()?.jsonObject?.get("mal_id")?.jsonPrimitive?.intOrNull
            }

            if (malId == null) {
                updateSection("MyAnimeList", "Community recommendations", SectionResult.Success(emptyList()))
                return
            }

            val recsResponse = client.newCall(
                GET("https://api.jikan.moe/v4/manga/$malId/recommendations")
            ).awaitSuccess()
            val recsJson = json.parseToJsonElement(recsResponse.body.string()).jsonObject
            val data = recsJson["data"]?.jsonArray ?: JsonArray(emptyList())

            val mangas = data.mapNotNull { item ->
                val entry = item.jsonObject["entry"]?.jsonObject ?: return@mapNotNull null
                val name = entry["title"]?.jsonPrimitive?.contentOrNull ?: return@mapNotNull null
                val url = entry["url"]?.jsonPrimitive?.contentOrNull ?: ""
                val images = entry["images"]?.jsonObject
                val cover = images?.get("webp")?.jsonObject?.get("image_url")?.jsonPrimitive?.contentOrNull
                    ?: images?.get("jpg")?.jsonObject?.get("image_url")?.jsonPrimitive?.contentOrNull
                    ?: ""
                createManga(name, url, cover)
            }
            val saved = if (mangas.isNotEmpty()) networkToLocalManga(mangas) else emptyList()
            updateSection("MyAnimeList", "Community recommendations", SectionResult.Success(saved))
        } catch (e: Exception) {
            logcat(LogPriority.ERROR, e) { "Failed to fetch MyAnimeList recommendations" }
            updateSection("MyAnimeList", "Community recommendations", SectionResult.Error(e.message ?: "Unknown error"))
        }
    }

    private fun createManga(title: String, url: String, thumbnailUrl: String): Manga {
        return Manga.create().copy(
            title = title,
            url = url,
            thumbnailUrl = thumbnailUrl,
            source = RECOMMENDS_SOURCE_ID,
        )
    }

    private fun updateSection(name: String, subtitle: String, result: SectionResult) {
        mutableState.update { state ->
            val key = "$name|$subtitle"
            state.copy(sections = state.sections + (key to result))
        }
    }

    @Immutable
    data class State(
        val title: String = "",
        val sections: Map<String, SectionResult> = emptyMap(),
    ) {
        val progress: Int get() = sections.count { it.value !is SectionResult.Loading }
        val total: Int get() = sections.size
    }

    sealed interface SectionResult {
        data object Loading : SectionResult
        data class Success(val mangas: List<Manga>) : SectionResult {
            val isEmpty: Boolean get() = mangas.isEmpty()
        }
        data class Error(val message: String) : SectionResult
    }
}
