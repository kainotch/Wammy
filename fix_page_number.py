import re

with open('app/src/main/java/com/example/wammy/ui/screens/ReaderScreen.kt', 'r') as f:
    text = f.read()

# Collect the preference
collect_prefs = """    val scaleTypeStr by prefs.imageScaleType.state.collectAsState()"""
new_collect = """    val showPageNumber by prefs.showPageNumber.state.collectAsState()
    val scaleTypeStr by prefs.imageScaleType.state.collectAsState()"""
text = text.replace(collect_prefs, new_collect)

# Hide immersive counter if disabled
old_immersive = """            if (!showOverlay) {
                // Immersive Mode Page Counter
                Box("""
new_immersive = """            if (!showOverlay && showPageNumber) {
                // Immersive Mode Page Counter
                Box("""
text = text.replace(old_immersive, new_immersive)

# Hide top-bar counter if disabled
old_topbar = """                        Text(
                            text = "${currentPageNum} / ${maxOf(1, pages.size)}",
                            color = Color.White,
                            style = MaterialTheme.typography.bodyMedium
                        )"""
new_topbar = """                        if (showPageNumber) {
                            Text(
                                text = "${currentPageNum} / ${maxOf(1, pages.size)}",
                                color = Color.White,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }"""
text = text.replace(old_topbar, new_topbar)

with open('app/src/main/java/com/example/wammy/ui/screens/ReaderScreen.kt', 'w') as f:
    f.write(text)

