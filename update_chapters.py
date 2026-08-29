import re

with open('app/src/main/java/com/example/wammy/ui/DetailsViewModel.kt', 'r') as f:
    text = f.read()

old_insert = """                        val chaptersToInsert = chaptersList.map { it.copy(mangaId = finalMangaId) }
                        AppContainer.database.chapterDao().insertChapters(chaptersToInsert)"""

new_insert = """                        val chaptersToInsert = chaptersList.map { it.copy(mangaId = finalMangaId) }
                        // Merge with existing chapters to preserve read state
                        val existingChapters = AppContainer.database.chapterDao().getChaptersForManga(finalMangaId).firstOrNull() ?: emptyList()
                        val existingMap = existingChapters.associateBy { it.sourceUrl }
                        
                        val mergedChapters = chaptersToInsert.map { newCh ->
                            val existing = existingMap[newCh.sourceUrl]
                            if (existing != null) {
                                // Keep the read state, lastPageRead, and ID from the local database, but update scanlator and date
                                newCh.copy(id = existing.id, read = existing.read, lastPageRead = existing.lastPageRead)
                            } else {
                                newCh
                            }
                        }
                        
                        // Because insertChapters is IGNORE, it won't update existing. We must use an update loop or REPLACE.
                        // Actually, if we use REPLACE, it would wipe the primary key ID if we didn't copy it. Since we copied the ID, we can just use an upsert loop.
                        mergedChapters.forEach { ch ->
                            if (ch.id > 0L) {
                                AppContainer.database.chapterDao().updateChapter(ch)
                            } else {
                                AppContainer.database.chapterDao().insertChapters(listOf(ch))
                            }
                        }"""

text = text.replace(old_insert, new_insert)

with open('app/src/main/java/com/example/wammy/ui/DetailsViewModel.kt', 'w') as f:
    f.write(text)

