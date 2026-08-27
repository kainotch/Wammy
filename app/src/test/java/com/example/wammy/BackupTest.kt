package com.example.wammy

import com.example.wammy.data.backup.models.Backup
import kotlinx.serialization.protobuf.ProtoBuf
import kotlinx.serialization.decodeFromByteArray
import org.junit.Test
import java.io.File
import java.util.zip.GZIPInputStream
import okio.buffer
import okio.source
import kotlinx.serialization.ExperimentalSerializationApi

@OptIn(ExperimentalSerializationApi::class)
class BackupTest {
    @Test
    fun testBackup() {
        val file = File("/home/notch/Downloads/eu.kanade.tachiyomi_2026-07-09_10-25.tachibk")
        if (!file.exists()) return
        try {
            val byteArray = GZIPInputStream(file.inputStream()).use { gzipStream ->
                gzipStream.source().buffer().use { source ->
                    source.readByteArray()
                }
            }
            val protoBuf = ProtoBuf
            val backup = protoBuf.decodeFromByteArray<Backup>(byteArray)
            println("PARSED_CATEGORIES_COUNT=" + backup.backupCategories.size)
            var mangaWithCategories = 0
            for (manga in backup.backupManga) {
                if (manga.categories.isNotEmpty()) mangaWithCategories++
            }
            println("MANGA_WITH_CATEGORIES=" + mangaWithCategories)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
