import re

with open('app/src/main/java/com/example/wammy/ui/screens/ReaderScreen.kt', 'r') as f:
    text = f.read()

# Fix import
text = text.replace("import com.example.wammy.ui.ReadingMode\n", "import com.example.wammy.data.prefs.ReadingMode\n")
text = text.replace("import com.example.wammy.ui.ReadingMode", "import com.example.wammy.data.prefs.ReadingMode")

# Fix the when block by replacing the old LTR and RTL branches and adding an else branch
old_ltr_rtl = """                com.example.wammy.data.prefs.ReadingMode.LTR -> {
                    PagerViewer(viewModel, pages, currentChapterIndex, currentChapterName, initialPage, isRtl = false) { showOverlay = !showOverlay }
                }
                com.example.wammy.data.prefs.ReadingMode.RTL -> {
                    PagerViewer(viewModel, pages, currentChapterIndex, currentChapterName, initialPage, isRtl = true) { showOverlay = !showOverlay }
                }"""

new_ltr_rtl = """                com.example.wammy.data.prefs.ReadingMode.LTR -> {
                    PagerViewer(viewModel, pages, currentChapterIndex, currentChapterName, initialPage, isRtl = false) { showOverlay = !showOverlay }
                }
                com.example.wammy.data.prefs.ReadingMode.RTL -> {
                    PagerViewer(viewModel, pages, currentChapterIndex, currentChapterName, initialPage, isRtl = true) { showOverlay = !showOverlay }
                }
                else -> {
                    // Fallback to RTL pager for now for unsupported modes
                    PagerViewer(viewModel, pages, currentChapterIndex, currentChapterName, initialPage, isRtl = true) { showOverlay = !showOverlay }
                }"""

text = text.replace(old_ltr_rtl, new_ltr_rtl)

# Also fix the when statement to group WEBTOON and CONTINUOUS_VERTICAL
text = text.replace("com.example.wammy.data.prefs.ReadingMode.WEBTOON -> {", "com.example.wammy.data.prefs.ReadingMode.WEBTOON, com.example.wammy.data.prefs.ReadingMode.CONTINUOUS_VERTICAL -> {")

with open('app/src/main/java/com/example/wammy/ui/screens/ReaderScreen.kt', 'w') as f:
    f.write(text)

