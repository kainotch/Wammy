import re

with open('app/src/main/java/com/example/wammy/ui/screens/ReaderScreen.kt', 'r') as f:
    text = f.read()

# Remove the Color Filter section in the settings sheet since it references the deleted ColorFilterMode
color_filter_ui = re.search(r'Text\("Color Filter".*?Row\(.*?\).*?\}', text, re.DOTALL)
if color_filter_ui:
    text = text.replace(color_filter_ui.group(0), "")

with open('app/src/main/java/com/example/wammy/ui/screens/ReaderScreen.kt', 'w') as f:
    f.write(text)

