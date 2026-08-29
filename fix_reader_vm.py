import re

with open('app/src/main/java/com/example/wammy/ui/ReaderViewModel.kt', 'r') as f:
    text = f.read()

# Replace the old ReadingMode enum
old_enum = """enum class ReadingMode {
    WEBTOON, LTR, RTL
}"""

new_enum = """// ReadingMode is now com.example.wammy.data.prefs.ReadingMode"""

text = text.replace(old_enum, new_enum)

# Replace the _readingMode StateFlow initialization
old_rm_flow = """private val _readingMode = MutableStateFlow(ReadingMode.RTL)
    val readingMode: StateFlow<ReadingMode> = _readingMode.asStateFlow()"""

new_rm_flow = """private val _readingMode = MutableStateFlow(com.example.wammy.data.prefs.ReadingMode.RTL)
    val readingMode: StateFlow<com.example.wammy.data.prefs.ReadingMode> = _readingMode.asStateFlow()
    
    private val _orientation = MutableStateFlow(com.example.wammy.data.prefs.OrientationType.FREE)
    val orientation: StateFlow<com.example.wammy.data.prefs.OrientationType> = _orientation.asStateFlow()"""

text = text.replace(old_rm_flow, new_rm_flow)

# Update loadChapters readingMode extraction
old_load = """_readingMode.value = ReadingMode.values()[manga.readingMode.coerceIn(0, ReadingMode.values().size - 1)]"""

new_load = """val titleMode = com.example.wammy.data.prefs.ReadingMode.values().getOrElse(manga.readingMode) { com.example.wammy.data.prefs.ReadingMode.DEFAULT }
        if (titleMode == com.example.wammy.data.prefs.ReadingMode.DEFAULT) {
            _readingMode.value = AppContainer.readerPreferences.defaultReadingMode.get()
        } else {
            _readingMode.value = titleMode
        }
        
        val titleOrientation = com.example.wammy.data.prefs.OrientationType.values().getOrElse(manga.orientation) { com.example.wammy.data.prefs.OrientationType.DEFAULT }
        if (titleOrientation == com.example.wammy.data.prefs.OrientationType.DEFAULT) {
            _orientation.value = AppContainer.readerPreferences.defaultOrientationType.get()
        } else {
            _orientation.value = titleOrientation
        }"""

text = text.replace(old_load, new_load)

# Update setReadingMode
old_set_rm = """fun setReadingMode(mode: ReadingMode) {
        _readingMode.value = mode
        val manga = activeManga ?: return
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            val existing = AppContainer.database.mangaDao().getMangaByUrl(manga.sourceUrl)
            if (existing != null) {
                AppContainer.database.mangaDao().insertManga(existing.copy(readingMode = mode.ordinal))
                activeManga = existing.copy(readingMode = mode.ordinal)
            } else {
                val newManga = manga.copy(readingMode = mode.ordinal, id = 0)
                AppContainer.database.mangaDao().insertManga(newManga)
                activeManga = newManga
            }
        }
    }"""

new_set_rm = """fun setReadingModeOverride(mode: com.example.wammy.data.prefs.ReadingMode) {
        val newEffectiveMode = if (mode == com.example.wammy.data.prefs.ReadingMode.DEFAULT) {
            AppContainer.readerPreferences.defaultReadingMode.get()
        } else {
            mode
        }
        _readingMode.value = newEffectiveMode
        
        val manga = activeManga ?: return
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            val existing = AppContainer.database.mangaDao().getMangaByUrl(manga.sourceUrl)
            if (existing != null) {
                AppContainer.database.mangaDao().insertManga(existing.copy(readingMode = mode.ordinal))
                activeManga = existing.copy(readingMode = mode.ordinal)
            } else {
                val newManga = manga.copy(readingMode = mode.ordinal, id = 0)
                AppContainer.database.mangaDao().insertManga(newManga)
                activeManga = newManga
            }
        }
    }
    
    fun setOrientationOverride(orientation: com.example.wammy.data.prefs.OrientationType) {
        val newEffectiveOrientation = if (orientation == com.example.wammy.data.prefs.OrientationType.DEFAULT) {
            AppContainer.readerPreferences.defaultOrientationType.get()
        } else {
            orientation
        }
        _orientation.value = newEffectiveOrientation
        
        val manga = activeManga ?: return
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            val existing = AppContainer.database.mangaDao().getMangaByUrl(manga.sourceUrl)
            if (existing != null) {
                AppContainer.database.mangaDao().insertManga(existing.copy(orientation = orientation.ordinal))
                activeManga = existing.copy(orientation = orientation.ordinal)
            } else {
                val newManga = manga.copy(orientation = orientation.ordinal, id = 0)
                AppContainer.database.mangaDao().insertManga(newManga)
                activeManga = newManga
            }
        }
    }"""

text = text.replace(old_set_rm, new_set_rm)

with open('app/src/main/java/com/example/wammy/ui/ReaderViewModel.kt', 'w') as f:
    f.write(text)

