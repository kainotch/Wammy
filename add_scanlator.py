import re

with open('app/src/main/java/com/example/wammy/data/local/Entities.kt', 'r') as f:
    text = f.read()

old_chapter = """data class ChapterEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val mangaId: Long,
    val sourceUrl: String,
    val name: String,
    val chapterNumber: Float,
    val dateUpload: Long,
    val read: Boolean = false,
    val lastPageRead: Int = 0
)"""

new_chapter = """data class ChapterEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val mangaId: Long,
    val sourceUrl: String,
    val name: String,
    val chapterNumber: Float,
    val dateUpload: Long,
    val read: Boolean = false,
    val lastPageRead: Int = 0,
    val scanlator: String? = null
)"""

text = text.replace(old_chapter, new_chapter)

with open('app/src/main/java/com/example/wammy/data/local/Entities.kt', 'w') as f:
    f.write(text)

