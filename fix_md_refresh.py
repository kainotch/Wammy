import re

with open('app/src/main/java/com/example/wammy/ui/ReaderViewModel.kt', 'r') as f:
    text = f.read()

# We need to add a lastRefreshTime variable outside the worker loop to prevent spamming
worker_pool_start = "            val workers = List(3) {"
worker_pool_new = """            var lastMdRefreshTime = 0L
            val workers = List(3) {"""

text = text.replace(worker_pool_start, worker_pool_new)

# Now, handle the 404 response
error_handling_old = """                            } else {
                                android.util.Log.e("WammyReader", "Network response was not successful: ${response.code} for URL: ${response.request.url}")
                                response.close()
                                if (targetPage!!.sourcePage != null) targetPage!!.sourcePage!!.imageUrl = null
                                updatePageState(targetPage!!.index, PageState.ERROR)
                                kotlinx.coroutines.delay(2000) // wait before retry
                            }"""

error_handling_new = """                            } else {
                                android.util.Log.e("WammyReader", "Network response was not successful: ${response.code} for URL: ${response.request.url}")
                                response.close()
                                
                                if (targetPage!!.sourcePage != null) {
                                    targetPage!!.sourcePage!!.imageUrl = null
                                } else if (sourceId == -1L && (response.code == 404 || response.code == 403)) {
                                    // Built-in MangaDexSource Node Expired! Refresh URLs safely!
                                    claimMutex.withLock {
                                        val now = System.currentTimeMillis()
                                        if (now - lastMdRefreshTime > 10000) { // Only refresh once per 10 seconds
                                            lastMdRefreshTime = now
                                            try {
                                                android.util.Log.d("WammyReader", "Refreshing MangaDex@Home node URLs due to 404...")
                                                val newUrls = AppContainer.mangaDexSource.fetchPageList(chapter.sourceUrl)
                                                _pages.update { list ->
                                                    list.map { page ->
                                                        if (page.index < newUrls.size && !page.url.startsWith("file://")) {
                                                            page.copy(url = newUrls[page.index], state = if (page.state == PageState.ERROR) PageState.QUEUE else page.state)
                                                        } else page
                                                    }
                                                }
                                            } catch (e: Exception) {
                                                android.util.Log.e("WammyReader", "Failed to refresh MD@Home URLs", e)
                                            }
                                        }
                                    }
                                }
                                
                                updatePageState(targetPage!!.index, PageState.ERROR)
                                kotlinx.coroutines.delay(2000) // wait before retry
                            }"""

text = text.replace(error_handling_old, error_handling_new)

with open('app/src/main/java/com/example/wammy/ui/ReaderViewModel.kt', 'w') as f:
    f.write(text)

