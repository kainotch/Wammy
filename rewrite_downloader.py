import re

with open('app/src/main/java/com/example/wammy/ui/ReaderViewModel.kt', 'r') as f:
    text = f.read()

# We will completely rip out the manual fallback and replace it with Mihon's exact logic
old_block = """                            if (targetPage!!.sourcePage != null && targetPage!!.sourcePage!!.imageUrl != null) {
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
                                
                                if (response.code == 404 || response.code == 403) {
                                    // Node Expired! Refresh URLs safely for both built-in and extensions!
                                    claimMutex.withLock {
                                        val now = System.currentTimeMillis()
                                        if (now - lastMdRefreshTime > 10000) { // Only refresh once per 10 seconds
                                            lastMdRefreshTime = now
                                            try {
                                                android.util.Log.d("WammyReader", "Refreshing Chapter Page URLs due to 404...")
                                                if (sourceId == -1L) {
                                                    val newUrls = AppContainer.mangaDexSource.fetchPageList(chapter.sourceUrl)
                                                    _pages.update { list ->
                                                        list.map { page ->
                                                            if (page.index < newUrls.size && !page.url.startsWith("file://")) {
                                                                page.copy(url = newUrls[page.index], state = if (page.state == PageState.ERROR) PageState.QUEUE else page.state)
                                                            } else page
                                                        }
                                                    }
                                                } else if (source != null) {
                                                    val sChapter = eu.kanade.tachiyomi.source.model.SChapter.create().apply { url = chapter.sourceUrl }
                                                    val newSourcePages = source.getPageList(sChapter)
                                                    _pages.update { list ->
                                                        list.map { page ->
                                                            if (page.index < newSourcePages.size && !page.url.startsWith("file://")) {
                                                                val newSourcePage = newSourcePages[page.index]
                                                                page.copy(url = newSourcePage.url, sourcePage = newSourcePage, state = if (page.state == PageState.ERROR) PageState.QUEUE else page.state)
                                                            } else page
                                                        }
                                                    }
                                                }
                                            } catch (e: Exception) {
                                                android.util.Log.e("WammyReader", "Failed to refresh Chapter URLs", e)
                                            }
                                        }
                                    }
                                } else if (targetPage!!.sourcePage != null) {
                                    targetPage!!.sourcePage!!.imageUrl = null
                                }
                                
                                updatePageState(targetPage!!.index, PageState.ERROR)
                                kotlinx.coroutines.delay(2000) // wait before retry
                            }"""

new_block = """                            var response: okhttp3.Response? = null
                            try {
                                if (source != null && targetPage!!.sourcePage != null) {
                                    // 1. Mihon strictly relies on source.getImage() and relies on its interceptors to handle URLs and errors.
                                    android.util.Log.d("WammyReader", "Attempting to download page ${targetPage!!.index} via extension source.getImage")
                                    response = source.getImage(targetPage!!.sourcePage!!)
                                } else {
                                    // 2. Built-in source fallback
                                    val finalImageUrlToDownload = targetPage!!.url
                                    val reqUrl = if (finalImageUrlToDownload.startsWith("http")) finalImageUrlToDownload else (AppContainer.mangaDexSource.baseUrl) + if (finalImageUrlToDownload.startsWith("/")) finalImageUrlToDownload else "/$finalImageUrlToDownload"
                                    android.util.Log.d("WammyReader", "Attempting to download page ${targetPage!!.index} via built-in request: $reqUrl")
                                    val builder = okhttp3.Request.Builder().url(reqUrl)
                                    targetPage!!.headers?.forEach { (k, v) -> builder.addHeader(k, v) }
                                    response = client.newCall(builder.build()).execute()
                                }
                                
                                if (response != null && response.isSuccessful) {
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
                                    // If response is not successful, we handle it as a network error
                                    throw Exception("HTTP error ${response?.code}")
                                }
                            } finally {
                                response?.close()
                            }"""

text = text.replace(old_block, new_block)

# We also need to fix the outer catch block to handle the 404/expired node refreshing correctly!
outer_catch_old = """                        } catch (e: Exception) {
                            android.util.Log.e("WammyReader", "Exception in pageLoaderJob for index ${targetPage!!.index}", e)
                            if (targetPage!!.sourcePage != null) targetPage!!.sourcePage!!.imageUrl = null
                            updatePageState(targetPage!!.index, PageState.ERROR)
                            kotlinx.coroutines.delay(2000) // wait before retry
                        }"""

outer_catch_new = """                        } catch (e: Exception) {
                            android.util.Log.e("WammyReader", "Exception in pageLoaderJob for index ${targetPage!!.index}", e)
                            
                            val isExpiredNodeError = e.message?.contains("HTTP error 404") == true || e.message?.contains("HTTP error 403") == true
                            if (isExpiredNodeError) {
                                // Node Expired! Refresh URLs safely for both built-in and extensions!
                                claimMutex.withLock {
                                    val now = System.currentTimeMillis()
                                    if (now - lastMdRefreshTime > 10000) { // Only refresh once per 10 seconds
                                        lastMdRefreshTime = now
                                        try {
                                            android.util.Log.d("WammyReader", "Refreshing Chapter Page URLs due to 404...")
                                            if (sourceId == -1L) {
                                                val newUrls = AppContainer.mangaDexSource.fetchPageList(chapter.sourceUrl)
                                                _pages.update { list ->
                                                    list.map { page ->
                                                        if (page.index < newUrls.size && !page.url.startsWith("file://")) {
                                                            page.copy(url = newUrls[page.index], state = if (page.state == PageState.ERROR) PageState.QUEUE else page.state)
                                                        } else page
                                                    }
                                                }
                                            } else if (source != null) {
                                                val sChapter = eu.kanade.tachiyomi.source.model.SChapter.create().apply { url = chapter.sourceUrl }
                                                val newSourcePages = source.getPageList(sChapter)
                                                _pages.update { list ->
                                                    list.map { page ->
                                                        if (page.index < newSourcePages.size && !page.url.startsWith("file://")) {
                                                            val newSourcePage = newSourcePages[page.index]
                                                            page.copy(url = newSourcePage.url, sourcePage = newSourcePage, state = if (page.state == PageState.ERROR) PageState.QUEUE else page.state)
                                                        } else page
                                                    }
                                                }
                                            }
                                        } catch (e2: Exception) {
                                            android.util.Log.e("WammyReader", "Failed to refresh Chapter URLs", e2)
                                        }
                                    }
                                }
                            } else {
                                // Mihon behavior: clear the cached imageUrl so it forces a re-fetch on the next retry
                                if (targetPage!!.sourcePage != null) {
                                    targetPage!!.sourcePage!!.imageUrl = null
                                }
                            }
                            
                            updatePageState(targetPage!!.index, PageState.ERROR)
                            kotlinx.coroutines.delay(2000) // wait before retry
                        }"""

text = text.replace(outer_catch_old, outer_catch_new)

# One more fix: we need to throw an exception if getImageUrl fails, instead of silently ignoring it and leaving it null.
get_image_url_old = """                            if (targetPage!!.sourcePage != null && targetPage!!.sourcePage!!.imageUrl == null) {
                                try {
                                    var parsed = source?.getImageUrl(targetPage!!.sourcePage!!)
                                    if (parsed != null && !parsed.startsWith("http")) {
                                        parsed = (source?.baseUrl ?: "") + if (parsed.startsWith("/")) parsed else "/$parsed"
                                    }
                                    targetPage!!.sourcePage!!.imageUrl = parsed
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                            }"""

get_image_url_new = """                            if (targetPage!!.sourcePage != null && targetPage!!.sourcePage!!.imageUrl == null) {
                                var parsed = source?.getImageUrl(targetPage!!.sourcePage!!)
                                if (parsed != null && !parsed.startsWith("http")) {
                                    parsed = (source?.baseUrl ?: "") + if (parsed.startsWith("/")) parsed else "/$parsed"
                                }
                                targetPage!!.sourcePage!!.imageUrl = parsed
                            }"""

text = text.replace(get_image_url_old, get_image_url_new)

with open('app/src/main/java/com/example/wammy/ui/ReaderViewModel.kt', 'w') as f:
    f.write(text)

