import re

with open('app/src/main/java/com/example/wammy/ui/ReaderViewModel.kt', 'r') as f:
    text = f.read()

# Add currentPageIndex variable
if "var currentPageIndex = 0" not in text:
    text = text.replace("var currentChapterIndex = initialChapterIndex", "var currentChapterIndex = initialChapterIndex\n    var currentPageIndex = 0")

# Update currentPageIndex in updateProgress
text = text.replace("fun updateProgress(pageIndex: Int) {\n        saveHistory(pageIndex)", "fun updateProgress(pageIndex: Int) {\n        currentPageIndex = pageIndex\n        saveHistory(pageIndex)")

# Update the targetPage selector to prioritize pages closest to the user's current scroll position (Mihon behavior)
old_selector = """                            val currentList = _pages.value
                            targetPage = currentList.firstOrNull { it.state == PageState.QUEUE } ?: currentList.firstOrNull { it.state == PageState.ERROR }"""

new_selector = """                            val currentList = _pages.value
                            // Mihon behavior: Prioritize pages closest to the user's current view (currentPageIndex)
                            // We sort QUEUE pages by their absolute distance to currentPageIndex, then fall back to ERROR pages.
                            targetPage = currentList
                                .filter { it.state == PageState.QUEUE }
                                .minByOrNull { kotlin.math.abs(it.index - currentPageIndex) }
                                ?: currentList.firstOrNull { it.state == PageState.ERROR }"""

text = text.replace(old_selector, new_selector)

# Fix the infinite retry loop for ERROR pages. Only retry ERROR pages if ALL other pages are READY!
old_error_selector = """?: currentList.firstOrNull { it.state == PageState.ERROR }"""
new_error_selector = """?: if (currentList.all { it.state == PageState.READY || it.state == PageState.ERROR }) currentList.firstOrNull { it.state == PageState.ERROR } else null"""

text = text.replace(old_error_selector, new_error_selector)

with open('app/src/main/java/com/example/wammy/ui/ReaderViewModel.kt', 'w') as f:
    f.write(text)
