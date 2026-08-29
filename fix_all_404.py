import re

with open('app/src/main/java/com/example/wammy/ui/ReaderViewModel.kt', 'r') as f:
    text = f.read()

# 1. Fix the ERROR page priority logic so it doesn't get stuck on Page 1
old_priority = """                            targetPage = currentList
                                .filter { it.state == PageState.QUEUE }
                                .minByOrNull { kotlin.math.abs(it.index - currentPageIndex) }
                                ?: if (currentList.all { it.state == PageState.READY || it.state == PageState.ERROR }) currentList.firstOrNull { it.state == PageState.ERROR } else null"""

new_priority = """                            targetPage = currentList
                                .filter { it.state == PageState.QUEUE }
                                .minByOrNull { kotlin.math.abs(it.index - currentPageIndex) }
                                ?: if (currentList.all { it.state == PageState.READY || it.state == PageState.ERROR }) {
                                    currentList
                                        .filter { it.state == PageState.ERROR }
                                        .minByOrNull { kotlin.math.abs(it.index - currentPageIndex) }
                                } else null"""

text = text.replace(old_priority, new_priority)

# 2. Fix the 404 handling to refresh URLs for BOTH built-in and extensions!
old_404 = """                                if (targetPage!!.sourcePage != null) {
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
                                }"""

new_404 = """                                if (response.code == 404 || response.code == 403) {
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
                                                    val newSourcePages = source.getPageList(chapter).awaitFirst()
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
                                }"""

text = text.replace(old_404, new_404)

with open('app/src/main/java/com/example/wammy/ui/ReaderViewModel.kt', 'w') as f:
    f.write(text)

