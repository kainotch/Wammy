import re

with open('app/src/main/java/com/example/wammy/ui/ReaderViewModel.kt', 'r') as f:
    text = f.read()

# Remove color filter from VM completely because we read from ReaderPreferences directly in ReaderScreen!
text = re.sub(r'private val _colorFilter = MutableStateFlow.*?asStateFlow\(\)', '', text, flags=re.DOTALL)
text = re.sub(r'fun setColorFilter\(mode: ColorFilterMode.*?\}', '', text, flags=re.DOTALL)

with open('app/src/main/java/com/example/wammy/ui/ReaderViewModel.kt', 'w') as f:
    f.write(text)

