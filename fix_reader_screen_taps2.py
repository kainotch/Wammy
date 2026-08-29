import re

with open('app/src/main/java/com/example/wammy/ui/screens/ReaderScreen.kt', 'r') as f:
    text = f.read()

old_pager_tap = """                                    onTap = { zone ->
                                        if (zone == TapZone.CENTER) {
                                            showOverlay = !showOverlay
                                        } else if (zone == TapZone.LEFT) {
                                            coroutineScope.launch { pagerState.animateScrollToPage(maxOf(0, pagerState.currentPage - 1)) }
                                        } else {
                                            coroutineScope.launch { pagerState.animateScrollToPage(minOf(pages.size, pagerState.currentPage + 1)) }
                                        }
                                    }"""

new_pager_tap = """                                    onTap = { offset ->
                                        val layoutIndex = com.example.wammy.AppContainer.readerPreferences.navigationModePager.get()
                                        val invertMode = com.example.wammy.AppContainer.readerPreferences.pagerNavInverted.get()
                                        val isVert = readingMode == com.example.wammy.data.prefs.ReadingMode.VERTICAL
                                        val layout = NavigationLayouts.getLayout(layoutIndex, false, isVert)
                                        
                                        val config = LocalConfiguration.current
                                        val density = LocalDensity.current.density
                                        val pxWidth = (config.screenWidthDp * density).toInt()
                                        val pxHeight = (config.screenHeightDp * density).toInt()
                                        
                                        val action = NavigationLayouts.resolveTap(offset, pxWidth, pxHeight, layout, invertMode)
                                        
                                        val finalAction = when (action) {
                                            TapAction.LEFT -> if (isRtl) TapAction.NEXT else TapAction.PREV
                                            TapAction.RIGHT -> if (isRtl) TapAction.PREV else TapAction.NEXT
                                            else -> action
                                        }
                                        
                                        if (finalAction == TapAction.MENU) {
                                            showOverlay = !showOverlay
                                        } else if (finalAction == TapAction.PREV) {
                                            coroutineScope.launch { pagerState.animateScrollToPage(maxOf(0, pagerState.currentPage - 1)) }
                                        } else if (finalAction == TapAction.NEXT) {
                                            coroutineScope.launch { pagerState.animateScrollToPage(minOf(pages.size, pagerState.currentPage + 1)) }
                                        }
                                    }"""

text = text.replace(old_pager_tap, new_pager_tap)

old_webtoon_tap = """                                    onTap = { offset ->
                                        val x = offset.x
                                        val width = size.width
                                        when {
                                            x < width * 0.33f -> {
                                                coroutineScope.launch { listState.animateScrollToItem(maxOf(0, listState.firstVisibleItemIndex - 1)) }
                                            }
                                            x > width * 0.66f -> {
                                                coroutineScope.launch { listState.animateScrollToItem(minOf(pages.size, listState.firstVisibleItemIndex + 1)) }
                                            }
                                            else -> showOverlay = !showOverlay
                                        }
                                    }"""

new_webtoon_tap = """                                    onTap = { offset ->
                                        val layoutIndex = com.example.wammy.AppContainer.readerPreferences.navigationModeWebtoon.get()
                                        val invertMode = com.example.wammy.AppContainer.readerPreferences.webtoonNavInverted.get()
                                        val layout = NavigationLayouts.getLayout(layoutIndex, true, true)
                                        
                                        val config = LocalConfiguration.current
                                        val density = LocalDensity.current.density
                                        val pxWidth = (config.screenWidthDp * density).toInt()
                                        val pxHeight = (config.screenHeightDp * density).toInt()
                                        
                                        val action = NavigationLayouts.resolveTap(offset, pxWidth, pxHeight, layout, invertMode)
                                        
                                        val finalAction = when (action) {
                                            TapAction.LEFT -> TapAction.PREV // Webtoon scrolls down, so "left" usually implies back/up
                                            TapAction.RIGHT -> TapAction.NEXT
                                            else -> action
                                        }
                                        
                                        if (finalAction == TapAction.MENU) {
                                            showOverlay = !showOverlay
                                        } else if (finalAction == TapAction.PREV) {
                                            coroutineScope.launch { listState.animateScrollToItem(maxOf(0, listState.firstVisibleItemIndex - 1)) }
                                        } else if (finalAction == TapAction.NEXT) {
                                            coroutineScope.launch { listState.animateScrollToItem(minOf(pages.size, listState.firstVisibleItemIndex + 1)) }
                                        }
                                    }"""

text = text.replace(old_webtoon_tap, new_webtoon_tap)

if "import androidx.compose.ui.platform.LocalConfiguration" not in text:
    text = text.replace("import androidx.compose.ui.platform.LocalContext", "import androidx.compose.ui.platform.LocalContext\nimport androidx.compose.ui.platform.LocalConfiguration\nimport androidx.compose.ui.platform.LocalDensity")

with open('app/src/main/java/com/example/wammy/ui/screens/ReaderScreen.kt', 'w') as f:
    f.write(text)

