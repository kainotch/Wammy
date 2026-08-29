import re

with open('app/src/main/java/com/example/wammy/ui/screens/ReaderScreen.kt', 'r') as f:
    text = f.read()

# Fix Conflicting Import Offset
lines = text.split('\n')
unique_lines = []
for line in lines:
    if line.startswith('import androidx.compose.ui.geometry.Offset'):
        if line not in unique_lines:
            unique_lines.append(line)
    else:
        unique_lines.append(line)
text = '\n'.join(unique_lines)

# Fix Unresolved reference 'ReadingMode' at line 60
# It's probably in the imports. I'll just fully qualify it or add import com.example.wammy.data.prefs.ReadingMode
if "import com.example.wammy.data.prefs.ReadingMode" not in text:
    text = text.replace("package com.example.wammy.ui.screens", "package com.example.wammy.ui.screens\nimport com.example.wammy.data.prefs.ReadingMode")

# Fix Conflicting Context declarations
# We will just remove any duplicate 'val context = androidx.compose.ui.platform.LocalContext.current'
count = text.count('val context = androidx.compose.ui.platform.LocalContext.current')
if count > 1:
    # Remove the FIRST occurrence, which is likely the one I added in fix_reader_screen_compile3.py
    pattern = re.compile(r'\s*val context = androidx\.compose\.ui\.platform\.LocalContext\.current')
    text = pattern.sub("", text, count=count-1)

with open('app/src/main/java/com/example/wammy/ui/screens/ReaderScreen.kt', 'w') as f:
    f.write(text)

