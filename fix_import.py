import re

with open('app/src/main/java/com/example/wammy/WammyApplication.kt', 'r') as f:
    text = f.read()

text = text.replace('import coil.Coil', 'import coil.Coil\nimport kotlinx.coroutines.launch')

with open('app/src/main/java/com/example/wammy/WammyApplication.kt', 'w') as f:
    f.write(text)
