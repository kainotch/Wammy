import re

with open('app/src/main/java/com/example/wammy/ui/screens/ReaderScreen.kt', 'r') as f:
    text = f.read()

old = """        if (model.state == com.example.wammy.ui.PageState.READY) {
                    val imageRequest = remember(model) {"""
new = """        if (model.state == com.example.wammy.ui.PageState.READY) {
            val context = androidx.compose.ui.platform.LocalContext.current
            val imageRequest = remember(model) {"""

text = text.replace(old, new)

with open('app/src/main/java/com/example/wammy/ui/screens/ReaderScreen.kt', 'w') as f:
    f.write(text)

