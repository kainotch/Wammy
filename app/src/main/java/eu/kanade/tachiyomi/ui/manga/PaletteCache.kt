package eu.kanade.tachiyomi.ui.manga

import android.content.Context
import coil3.toBitmap
import coil3.imageLoader
import coil3.request.ImageRequest
import coil3.request.allowHardware
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

object PaletteCache {
    val vibrantCoverColorMap = java.util.concurrent.ConcurrentHashMap<Long, Int>()

    fun precalculate(context: Context, mangaId: Long, coverUrl: String?) {
        if (coverUrl.isNullOrEmpty()) return
        if (vibrantCoverColorMap.containsKey(mangaId)) return

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val request = ImageRequest.Builder(context)
                    .data(coverUrl)
                    .allowHardware(false)
                    .build()
                val result = context.imageLoader.execute(request)
                val bitmap = result.image?.toBitmap() ?: return@launch
                val scaled = android.graphics.Bitmap.createScaledBitmap(bitmap, 24, 24, true)
                var rTotal = 0L; var gTotal = 0L; var bTotal = 0L
                val w = scaled.width; val h = scaled.height; val count = w * h
                for (x in 0 until w) {
                    for (y in 0 until h) {
                        val pixel = scaled.getPixel(x, y)
                        rTotal += android.graphics.Color.red(pixel)
                        gTotal += android.graphics.Color.green(pixel)
                        bTotal += android.graphics.Color.blue(pixel)
                    }
                }
                val avgColor = android.graphics.Color.rgb(
                    (rTotal / count).toInt(),
                    (gTotal / count).toInt(),
                    (bTotal / count).toInt(),
                )
                vibrantCoverColorMap[mangaId] = avgColor
            } catch (_: Exception) {}
        }
    }
}