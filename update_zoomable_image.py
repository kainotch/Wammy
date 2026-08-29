import re

with open('app/src/main/java/com/example/wammy/ui/screens/ReaderScreen.kt', 'r') as f:
    text = f.read()

# Update ZoomableImage parameter
old_param = "onTap: (com.example.wammy.ui.screens.TapZone) -> Unit"
new_param = "onTap: (androidx.compose.ui.geometry.Offset) -> Unit"
text = text.replace(old_param, new_param)

# Remove old TapZone enum
old_enum = "enum class TapZone { LEFT, CENTER, RIGHT }"
new_enum = ""
text = text.replace(old_enum, new_enum)
with open('app/src/main/java/com/example/wammy/ui/screens/ReaderHelpers.kt', 'r') as f:
    helper_text = f.read()
    helper_text = helper_text.replace("enum class TapZone { LEFT, CENTER, RIGHT }", "")
with open('app/src/main/java/com/example/wammy/ui/screens/ReaderHelpers.kt', 'w') as f:
    f.write(helper_text)

# Update tap detector
old_tap = """                    when {
                        x < width * 0.33f -> onTap(com.example.wammy.ui.screens.TapZone.LEFT)
                        x > width * 0.66f -> onTap(com.example.wammy.ui.screens.TapZone.RIGHT)
                        else -> onTap(com.example.wammy.ui.screens.TapZone.CENTER)
                    }"""
new_tap = """                    onTap(offset)"""
text = text.replace(old_tap, new_tap)

with open('app/src/main/java/com/example/wammy/ui/screens/ReaderScreen.kt', 'w') as f:
    f.write(text)

