package com.example.wammy.lnreader.loader

import android.content.Context
import com.example.wammy.lnreader.model.LNNovel
import com.example.wammy.lnreader.tsundoku.jsplugin.runtime.PluginRuntime
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

class LNReaderPluginLoader(private val context: Context) {
    suspend fun loadAndFetchPopular(pluginFile: java.io.File, page: Int = 1): List<com.example.wammy.lnreader.model.LNNovel> = withContext(Dispatchers.IO) {
        val runtime = PluginRuntime("wammy_plugin", context, Dispatchers.IO)
        val instance = runtime.executePlugin(pluginFile.readText())

        try {
            val resultJsonString = instance.execute("JSON.stringify(await plugin.popularNovels(1, { showLatestNovels: false, filters: plugin.filters }))") as? String
                ?: run { android.util.Log.e("LNReaderPluginLoader", "Returning empty list!"); return@withContext emptyList() }
                
            val result = org.json.JSONTokener(resultJsonString).nextValue()
            val novelsArray = if (result is JSONArray) {
                result
            } else if (result is JSONObject && result.has("novels")) {
                result.getJSONArray("novels")
            } else {
                JSONArray()
            }

            val novels = mutableListOf<com.example.wammy.lnreader.model.LNNovel>()
            for (i in 0 until novelsArray.length()) {
                val item = novelsArray.getJSONObject(i)
                novels.add(
                    com.example.wammy.lnreader.model.LNNovel(
                        title = item.optString("name", "Unknown"),
                        coverUrl = item.optString("cover", ""),
                        url = item.optString("path", "")
                    )
                )
            }
            novels
        } finally {
            instance.close()
        }
    }

    suspend fun getSite(pluginFile: java.io.File): String = withContext(Dispatchers.IO) {
        val runtime = PluginRuntime("wammy_plugin", context, Dispatchers.IO)
        val instance = runtime.executePlugin(pluginFile.readText())
        try {
            instance.getSite() ?: ""
        } finally {
            instance.close()
        }
    }

    suspend fun parseNovel(pluginFile: java.io.File, novelUrl: String): com.example.wammy.lnreader.model.SourceNovel = withContext(Dispatchers.IO) {
        val runtime = PluginRuntime("wammy_plugin", context, Dispatchers.IO)
        val instance = runtime.executePlugin(pluginFile.readText())
        try {
            val js = "JSON.stringify(await plugin.parseNovel('$novelUrl'))"
            val resultJsonString = instance.execute(js) as? String ?: return@withContext com.example.wammy.lnreader.model.SourceNovel(path="")
            val obj = org.json.JSONObject(resultJsonString)
            
            val chapters = mutableListOf<com.example.wammy.lnreader.model.ChapterItem>()
            if (obj.has("chapters")) {
                val arr = obj.getJSONArray("chapters")
                for (i in 0 until arr.length()) {
                    val c = arr.getJSONObject(i)
                    chapters.add(com.example.wammy.lnreader.model.ChapterItem(
                        name = c.optString("name", "Chapter"),
                        path = c.optString("path", "")
                    ))
                }
            }
            
            com.example.wammy.lnreader.model.SourceNovel(
                path = novelUrl,
                name = obj.optString("name", "Unknown"),
                cover = obj.optString("cover", ""),
                summary = obj.optString("summary", ""),
                chapters = chapters
            )
        } finally {
            instance.close()
        }
    }

    suspend fun parseChapter(pluginFile: java.io.File, chapterUrl: String): String = withContext(Dispatchers.IO) {
        val runtime = PluginRuntime("wammy_plugin", context, Dispatchers.IO)
        val instance = runtime.executePlugin(pluginFile.readText())
        try {
            val js = "await plugin.parseChapter('$chapterUrl')"
            instance.execute(js) as? String ?: ""
        } finally {
            instance.close()
        }
    }

    suspend fun searchNovels(pluginFile: java.io.File, searchTerm: String, page: Int = 1): List<com.example.wammy.lnreader.model.LNNovel> = withContext(Dispatchers.IO) {
        val runtime = PluginRuntime("wammy_plugin", context, Dispatchers.IO)
        val instance = runtime.executePlugin(pluginFile.readText())

        try {
            val escapedTerm = org.json.JSONObject.quote(searchTerm)
            val resultJsonString = instance.execute("JSON.stringify(await plugin.searchNovels($escapedTerm, 1))") as? String
                ?: run { android.util.Log.e("LNReaderPluginLoader", "Returning empty list for search!"); return@withContext emptyList() }
                
            val result = org.json.JSONTokener(resultJsonString).nextValue()
            val novelsArray = if (result is JSONArray) {
                result
            } else if (result is JSONObject && result.has("novels")) {
                result.getJSONArray("novels")
            } else {
                JSONArray()
            }

            val novels = mutableListOf<com.example.wammy.lnreader.model.LNNovel>()
            for (i in 0 until novelsArray.length()) {
                val item = novelsArray.getJSONObject(i)
                novels.add(
                    com.example.wammy.lnreader.model.LNNovel(
                        title = item.optString("name", "Unknown"),
                        coverUrl = item.optString("cover", ""),
                        url = item.optString("path", "")
                    )
                )
            }
            novels
        } catch (e: Exception) {
            android.util.Log.e("LNReaderPluginLoader", "Search error", e)
            throw Exception("Search Error: " + (e.message ?: "Unknown JS error"))
        } finally {
            instance.close()
        }
    }
}
