import re

with open('app/src/main/java/com/example/wammy/WammyApplication.kt', 'r') as f:
    text = f.read()

# Replace the simple Coil image loader with a strictly memory-managed one
old_coil = "Coil.setImageLoader(ImageLoader.Builder(this).okHttpClient { coilClient }.build())"
new_coil = """Coil.setImageLoader(
            ImageLoader.Builder(this)
                .okHttpClient { coilClient }
                .diskCache {
                    coil.disk.DiskCache.Builder()
                        .directory(cacheDir.resolve("image_cache"))
                        .maxSizeBytes(64L * 1024 * 1024) // Strictly limit disk cache to 64 MB
                        .build()
                }
                .memoryCache {
                    coil.memory.MemoryCache.Builder(this)
                        .maxSizePercent(0.15) // Limit memory cache to 15% of available RAM (down from default 25%)
                        .build()
                }
                .build()
        )"""

text = text.replace(old_coil, new_coil)

with open('app/src/main/java/com/example/wammy/WammyApplication.kt', 'w') as f:
    f.write(text)
