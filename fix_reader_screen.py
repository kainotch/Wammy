import re

with open('app/src/main/java/com/example/wammy/ui/screens/ReaderScreen.kt', 'r') as f:
    text = f.read()

# Fix setReadingMode calls
text = text.replace("viewModel.setReadingMode", "viewModel.setReadingModeOverride")
text = text.replace("ReadingMode.WEBTOON", "com.example.wammy.data.prefs.ReadingMode.WEBTOON")
text = text.replace("ReadingMode.LTR", "com.example.wammy.data.prefs.ReadingMode.LTR")
text = text.replace("ReadingMode.RTL", "com.example.wammy.data.prefs.ReadingMode.RTL")
text = text.replace("ReadingMode.VERTICAL", "com.example.wammy.data.prefs.ReadingMode.VERTICAL")
text = text.replace("ReadingMode.CONTINUOUS_VERTICAL", "com.example.wammy.data.prefs.ReadingMode.CONTINUOUS_VERTICAL")

# Add the imports if missing, or just rely on fully qualified names.
# Actually, the best way to handle the viewer engines is:
# Pager -> LTR, RTL, VERTICAL
# Webtoon -> WEBTOON, CONTINUOUS_VERTICAL
# Let's fix the When clause that picks the engine!

old_when = """    when (readingMode) {
        com.example.wammy.data.prefs.ReadingMode.WEBTOON -> {
            WebtoonViewer(viewModel, pages, currentChapterIndex, currentChapterName, initialPage, listState) { onToggleMenu() }
        }
        com.example.wammy.data.prefs.ReadingMode.LTR -> {
            PagerViewer(viewModel, pages, currentChapterIndex, currentChapterName, initialPage, isRtl = false) { onToggleMenu() }
        }
        com.example.wammy.data.prefs.ReadingMode.RTL -> {
            PagerViewer(viewModel, pages, currentChapterIndex, currentChapterName, initialPage, isRtl = true) { onToggleMenu() }
        }
    }"""

new_when = """    when (readingMode) {
        com.example.wammy.data.prefs.ReadingMode.WEBTOON,
        com.example.wammy.data.prefs.ReadingMode.CONTINUOUS_VERTICAL -> {
            WebtoonViewer(viewModel, pages, currentChapterIndex, currentChapterName, initialPage, listState) { onToggleMenu() }
        }
        com.example.wammy.data.prefs.ReadingMode.LTR -> {
            PagerViewer(viewModel, pages, currentChapterIndex, currentChapterName, initialPage, isRtl = false) { onToggleMenu() }
        }
        com.example.wammy.data.prefs.ReadingMode.RTL -> {
            PagerViewer(viewModel, pages, currentChapterIndex, currentChapterName, initialPage, isRtl = true) { onToggleMenu() }
        }
        com.example.wammy.data.prefs.ReadingMode.VERTICAL -> {
            PagerViewer(viewModel, pages, currentChapterIndex, currentChapterName, initialPage, isRtl = false) { onToggleMenu() } // We will need a true Vertical Pager later
        }
        else -> {
            PagerViewer(viewModel, pages, currentChapterIndex, currentChapterName, initialPage, isRtl = true) { onToggleMenu() }
        }
    }"""

text = text.replace(old_when, new_when)

with open('app/src/main/java/com/example/wammy/ui/screens/ReaderScreen.kt', 'w') as f:
    f.write(text)

