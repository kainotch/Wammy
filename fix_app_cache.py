import re

with open('app/src/main/java/com/example/wammy/WammyApplication.kt', 'r') as f:
    text = f.read()

cleanup_code = """        super.onCreate()

        // Clean up any leftover reader cache from previous sessions that were forcefully killed
        kotlinx.coroutines.GlobalScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val cacheDir = java.io.File(cacheDir, "reader_cache")
                if (cacheDir.exists()) {
                    cacheDir.deleteRecursively()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
"""
text = text.replace('        super.onCreate()', cleanup_code)

with open('app/src/main/java/com/example/wammy/WammyApplication.kt', 'w') as f:
    f.write(text)
