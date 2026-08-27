import re

with open('app/src/main/java/com/example/wammy/ui/ReaderViewModel.kt', 'r') as f:
    text = f.read()

# Add onCleared method before the closing brace of the class
on_cleared = """
    override fun onCleared() {
        super.onCleared()
        // Clean up the temporary reader cache to prevent 120MB+ storage bloat
        kotlinx.coroutines.GlobalScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val cacheDir = java.io.File(AppContainer.appContext.cacheDir, "reader_cache")
                if (cacheDir.exists()) {
                    cacheDir.deleteRecursively()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}"""

# Find the last closing brace and replace it
text = text.rsplit('}', 1)[0] + on_cleared

with open('app/src/main/java/com/example/wammy/ui/ReaderViewModel.kt', 'w') as f:
    f.write(text)
