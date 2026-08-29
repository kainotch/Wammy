import re

with open('app/src/main/java/com/example/wammy/ui/screens/ReaderScreen.kt', 'r') as f:
    text = f.read()

text = text.replace("import com.example.wammy.ui.ReadingMode\n", "")
text = text.replace("    val context = LocalContext.current\n", "")

with open('app/src/main/java/com/example/wammy/ui/screens/ReaderScreen.kt', 'w') as f:
    f.write(text)

