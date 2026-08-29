import re

with open('app/src/main/java/com/example/wammy/ui/screens/ReaderScreen.kt', 'r') as f:
    text = f.read()

old_top = """    val lifecycleOwner = LocalLifecycleOwner.current"""

new_top = """    val lifecycleOwner = LocalLifecycleOwner.current
    val config = androidx.compose.ui.platform.LocalConfiguration.current
    val density = androidx.compose.ui.platform.LocalDensity.current.density
    val pxWidth = (config.screenWidthDp * density).toInt()
    val pxHeight = (config.screenHeightDp * density).toInt()"""

text = text.replace(old_top, new_top)

with open('app/src/main/java/com/example/wammy/ui/screens/ReaderScreen.kt', 'w') as f:
    f.write(text)

