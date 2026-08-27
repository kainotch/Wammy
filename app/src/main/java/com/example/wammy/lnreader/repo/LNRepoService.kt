package com.example.wammy.lnreader.repo

import android.content.Context
import com.example.wammy.lnreader.model.IReaderExtensionMeta
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream

class IReaderRepoService(private val context: Context) {
    private val client = OkHttpClient()
    private val json = Json { ignoreUnknownKeys = true }

    companion object {
        const val INDEX_URL = "https://raw.githubusercontent.com/lnreader/lnreader-plugins/plugins/v3.0.0/.dist/plugins.min.json"
    }

    suspend fun fetchIndex(): List<IReaderExtensionMeta> = withContext(Dispatchers.IO) {
        val request = Request.Builder().url(INDEX_URL).build()
        val response = client.newCall(request).execute()
        val body = response.body?.string() ?: return@withContext emptyList()
        // LNReader plugin JSON format: [{"id":"anf.net","name":"AllNovelFull","site":"...","lang":"English","version":"1.0.0","url":"...","iconUrl":"..."}]
        val plugins = json.decodeFromString<List<Map<String, String>>>(body)
        plugins.map { p ->
            IReaderExtensionMeta(
                pkg = p["id"] ?: "",
                apk = (p["id"] ?: "") + ".js", // store as .js locally
                name = p["name"] ?: "",
                id = (p["id"]?.hashCode()?.toLong() ?: 0L),
                lang = p["lang"] ?: "en",
                version = p["version"] ?: "1.0",
                sourceDir = p["url"] ?: "", // Store the remote URL in sourceDir
                iconUrl = p["iconUrl"] ?: ""
            )
        }.filter { it.lang.contains("English", ignoreCase = true) }
    }

    fun getInstalledExtensions(): List<IReaderExtensionMeta> {
        val dir = File(context.filesDir, "ir_extensions")
        if (!dir.exists()) return emptyList()
        val metaFile = File(dir, "installed.json")
        if (!metaFile.exists()) return emptyList()
        return try {
            json.decodeFromString<List<IReaderExtensionMeta>>(metaFile.readText())
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun downloadExtension(meta: IReaderExtensionMeta, onProgress: (Float) -> Unit) = withContext(Dispatchers.IO) {
        val request = Request.Builder().url(meta.sourceDir).build()
        val response = client.newCall(request).execute()
        val dir = File(context.filesDir, "ir_extensions")
        if (!dir.exists()) dir.mkdirs()
        
        val file = File(dir, meta.apk)
        if (file.exists()) {
            file.setWritable(true)
            file.delete()
        }
        
        response.body?.byteStream()?.use { input ->
            FileOutputStream(file).use { output ->
                input.copyTo(output)
            }
        }
        file.setReadOnly()

        val installed = getInstalledExtensions().toMutableList()
        installed.removeAll { it.pkg == meta.pkg }
        installed.add(meta)
        File(dir, "installed.json").writeText(
            json.encodeToString(installed)
        )
    }

    fun deleteExtension(meta: IReaderExtensionMeta) {
        val dir = File(context.filesDir, "ir_extensions")
        File(dir, meta.apk).delete()
        val installed = getInstalledExtensions().toMutableList()
        installed.removeAll { it.pkg == meta.pkg }
        File(dir, "installed.json").writeText(
            json.encodeToString(installed)
        )
    }

    fun isInstalled(meta: IReaderExtensionMeta): Boolean {
        return File(context.filesDir, "ir_extensions/${meta.apk}").exists()
    }
}
