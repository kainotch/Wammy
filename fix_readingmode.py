import re

with open('app/src/main/java/com/example/wammy/ui/ReaderViewModel.kt', 'r') as f:
    text = f.read()

# 1. Update initReader
init_old = """    fun initReader(chaptersList: List<ChapterEntity>, startIndex: Int, source: Long, manga: MangaEntity) {
        chapters = chaptersList
        currentChapterIndex = startIndex
        sourceId = source
        activeManga = manga
        loadCurrentChapter()
    }"""

init_new = """    fun initReader(chaptersList: List<ChapterEntity>, startIndex: Int, source: Long, manga: MangaEntity) {
        chapters = chaptersList
        currentChapterIndex = startIndex
        sourceId = source
        activeManga = manga
        _readingMode.value = ReadingMode.values()[manga.readingMode.coerceIn(0, ReadingMode.values().size - 1)]
        loadCurrentChapter()
    }"""

text = text.replace(init_old, init_new)

# 2. Update setReadingMode
set_old = """    fun setReadingMode(mode: ReadingMode) {
        _readingMode.value = mode
    }"""

set_new = """    fun setReadingMode(mode: ReadingMode) {
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

text = text.replace(set_old, set_new)

with open('app/src/main/java/com/example/wammy/ui/ReaderViewModel.kt', 'w') as f:
    f.write(text)

