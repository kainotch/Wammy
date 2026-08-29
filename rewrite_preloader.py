import re

with open('app/src/main/java/com/example/wammy/ui/ReaderViewModel.kt', 'r') as f:
    text = f.read()

old_func_start = "    private fun startPagePreloader() {"
old_func_end = "    fun updateProgress(pageIndex: Int) {"

# Extract the old function
start_idx = text.find(old_func_start)
end_idx = text.find(old_func_end)

if start_idx == -1 or end_idx == -1:
    print("Could not find function bounds!")
    exit(1)

old_func = text[start_idx:end_idx]

new_func = """    private fun startPagePreloader() {
        pageLoaderJob?.cancel()
        pageLoaderJob = viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            val source = AppContainer.extensionManager.activeSources.find { it.id == sourceId } as? eu.kanade.tachiyomi.source.online.HttpSource
            val client = source?.client ?: defaultClient
            
            val chapter = chapters[currentChapterIndex]
            val manga = activeManga ?: return@launch
            
            val cacheDir = java.io.File(AppContainer.appContext.cacheDir, "reader_cache/${manga.sourceUrl.hashCode()}_${chapter.sourceUrl.hashCode()}")
            if (!cacheDir.exists()) cacheDir.mkdirs()

            val claimMutex = kotlinx.coroutines.sync.Mutex()

            // Launch a worker pool of 3 concurrent downloaders to mimic Mihon/Tachiyomi
            val workers = List(3) {
                kotlinx.coroutines.launch {
                    while (true) {
                        var targetPage: com.example.wammy.ui.ReaderPage? = null
                        
                        claimMutex.withLock {
                            val currentList = _pages.value
                            targetPage = currentList.firstOrNull { it.state == PageState.QUEUE } ?: currentList.firstOrNull { it.state == PageState.ERROR }
                            if (targetPage != null) {
                                _pages.update { list ->
                                    list.map { if (it.index == targetPage!!.index) it.copy(state = PageState.DOWNLOAD_IMAGE) else it }
                                }
                            }
                        }

                        if (targetPage == null) {
                            kotlinx.coroutines.delay(500)
                            // check if all pages are READY
                            if (_pages.value.all { it.state == PageState.READY }) break
                            continue
                        }

                        try {
                            val finalFile = java.io.File(cacheDir, "page_${targetPage!!.index}.jpg")
                            
                            if (finalFile.exists() && finalFile.length() > 0) {
                                _pages.update { list ->
                                    list.map { if (it.index == targetPage!!.index) it.copy(state = PageState.READY, url = "file://${finalFile.absolutePath}") else it }
                                }
                                continue
                            }

                            if (targetPage!!.sourcePage != null && targetPage!!.sourcePage!!.imageUrl == null) {
                                try {
                                    var parsed = source?.getImageUrl(targetPage!!.sourcePage!!)
                                    if (parsed != null && !parsed.startsWith("http")) {
                                        parsed = (source?.baseUrl ?: "") + if (parsed.startsWith("/")) parsed else "/$parsed"
                                    }
                                    targetPage!!.sourcePage!!.imageUrl = parsed
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                            }

                            if (targetPage!!.sourcePage != null && targetPage!!.sourcePage!!.imageUrl != null) {
                                var imgUrl = targetPage!!.sourcePage!!.imageUrl!!
                                if (!imgUrl.startsWith("http")) {
                                    imgUrl = (source?.baseUrl ?: "") + if (imgUrl.startsWith("/")) imgUrl else "/$imgUrl"
                                    targetPage!!.sourcePage!!.imageUrl = imgUrl
                                }
                            }

                            val finalImageUrlToDownload = targetPage!!.sourcePage?.imageUrl ?: targetPage!!.url

                            android.util.Log.d("WammyReader", "Attempting to download page ${targetPage!!.index} with URL: ${targetPage!!.sourcePage?.imageUrl ?: finalImageUrlToDownload}")
                            val response = if (source != null && targetPage!!.sourcePage != null && targetPage!!.sourcePage!!.imageUrl != null) {
                                try {
                                    source.getImage(targetPage!!.sourcePage!!)
                                } catch (e: Exception) {
                                    val reqUrl = if (finalImageUrlToDownload.startsWith("http")) finalImageUrlToDownload else (source.baseUrl) + if (finalImageUrlToDownload.startsWith("/")) finalImageUrlToDownload else "/$finalImageUrlToDownload"
                                    val builder = okhttp3.Request.Builder().url(reqUrl)
                                    targetPage!!.headers?.forEach { (k, v) -> builder.addHeader(k, v) }
                                    client.newCall(builder.build()).execute()
                                }
                            } else {
                                val reqUrl = if (finalImageUrlToDownload.startsWith("http")) finalImageUrlToDownload else (source?.baseUrl ?: "") + if (finalImageUrlToDownload.startsWith("/")) finalImageUrlToDownload else "/${finalImageUrlToDownload}"
                                val builder = okhttp3.Request.Builder().url(reqUrl)
                                targetPage!!.headers?.forEach { (k, v) -> builder.addHeader(k, v) }
                                client.newCall(builder.build()).execute()
                            }
                            
                            if (response.isSuccessful) {
                                val tmpFile = java.io.File(cacheDir, "page_${targetPage!!.index}.tmp")
                                response.body?.byteStream()?.use { input ->
                                    tmpFile.outputStream().use { output ->
                                        input.copyTo(output)
                                    }
                                }
                                tmpFile.renameTo(finalFile)
                                _pages.update { list ->
                                    list.map { if (it.index == targetPage!!.index) it.copy(state = PageState.READY, url = "file://${finalFile.absolutePath}") else it }
                                }
                            } else {
                                android.util.Log.e("WammyReader", "Network response was not successful: ${response.code} for URL: ${response.request.url}")
                                response.close()
                                if (targetPage!!.sourcePage != null) targetPage!!.sourcePage!!.imageUrl = null
                                updatePageState(targetPage!!.index, PageState.ERROR)
                                kotlinx.coroutines.delay(2000) // wait before retry
                            }
                        } catch (e: Exception) {
                            android.util.Log.e("WammyReader", "Exception in pageLoaderJob for index ${targetPage!!.index}", e)
                            if (targetPage!!.sourcePage != null) targetPage!!.sourcePage!!.imageUrl = null
                            updatePageState(targetPage!!.index, PageState.ERROR)
                            kotlinx.coroutines.delay(2000) // wait before retry
                        }
                    }
                }
            }
            
            workers.forEach { it.join() }
        }
    }

"""

new_text = text[:start_idx] + new_func + text[end_idx:]

with open('app/src/main/java/com/example/wammy/ui/ReaderViewModel.kt', 'w') as f:
    f.write(new_text)

print("Preloader rewritten to use concurrent workers!")
