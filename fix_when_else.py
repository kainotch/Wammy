import re

with open('app/src/main/java/com/example/wammy/ui/screens/ReaderScreen.kt', 'r') as f:
    text = f.read()

# Replace the closing of LTR/RTL branch with an else branch appended
old_closing = """                            }
                        }
                    }
                }
            }
        }

        // Settings Modal Bottom Sheet"""

new_closing = """                            }
                        }
                    }
                }
                else -> {
                    // Fallback for VERTICAL, etc
                    val isRtl = true
                    val layoutDirection = LayoutDirection.Rtl
                    
CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
                        val pagerState = rememberPagerState(pageCount = { pages.size + 1 })
                        
                        LaunchedEffect(pagerState.isScrollInProgress) {
                            if (pagerState.isScrollInProgress && showOverlay) showOverlay = false
                        }
                        
                        LaunchedEffect(pagerState.currentPage) {
                            if (pages.isNotEmpty() && pagerState.currentPage < pages.size) {
                                currentProgress = pagerState.currentPage.toFloat() / maxOf(1, pages.size - 1).toFloat()
                                viewModel.updateProgress(pagerState.currentPage)
                            }
                        }
                        
                        LaunchedEffect(initialPage) {
                            if (initialPage > 0 && initialPage < pages.size) {
                                pagerState.scrollToPage(initialPage)
                            }
                        }

                        HorizontalPager(
                            state = pagerState,
                            modifier = Modifier.fillMaxSize(),
                            key = { it },
                            reverseLayout = false
                        ) { page ->
                            if (page < pages.size) {
                                PagerPage(
                                    page = pages[page],
                                    onRetry = { viewModel.retryPage(pages[page].index) },
                                    onTap = { zone ->
                                        if (zone == TapZone.CENTER) showOverlay = !showOverlay
                                        else if (zone == TapZone.LEFT) coroutineScope.launch { pagerState.animateScrollToPage(maxOf(0, pagerState.currentPage - 1)) }
                                        else coroutineScope.launch { pagerState.animateScrollToPage(minOf(pages.size, pagerState.currentPage + 1)) }
                                    }
                                )
                            } else {
                                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    ChapterTransition(viewModel)
                                }
                            }
                        }
                    }
                }
            }
        }

        // Settings Modal Bottom Sheet"""

text = text.replace(old_closing, new_closing)

with open('app/src/main/java/com/example/wammy/ui/screens/ReaderScreen.kt', 'w') as f:
    f.write(text)

