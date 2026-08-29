import re

with open('app/src/main/java/com/example/wammy/AppContainer.kt', 'r') as f:
    text = f.read()

# Add ReaderPreferences initialization
if "lateinit var readerPreferences: com.example.wammy.data.prefs.ReaderPreferences" not in text:
    text = text.replace("lateinit var database: WammyDatabase", 
                        "lateinit var database: WammyDatabase\n    lateinit var readerPreferences: com.example.wammy.data.prefs.ReaderPreferences")

if "readerPreferences = com.example.wammy.data.prefs.ReaderPreferences(appContext)" not in text:
    text = text.replace("extensionManager = com.example.wammy.extension.ExtensionManager(appContext)",
                        "extensionManager = com.example.wammy.extension.ExtensionManager(appContext)\n        readerPreferences = com.example.wammy.data.prefs.ReaderPreferences(appContext)")

with open('app/src/main/java/com/example/wammy/AppContainer.kt', 'w') as f:
    f.write(text)

