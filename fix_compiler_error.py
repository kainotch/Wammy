import re

with open('app/src/main/java/com/example/wammy/ui/ReaderViewModel.kt', 'r') as f:
    text = f.read()

# Fix the getPageList call for the extension
old_code = "val newSourcePages = source.getPageList(chapter).awaitFirst()"
new_code = """val sChapter = eu.kanade.tachiyomi.source.model.SChapter.create().apply { url = chapter.sourceUrl }
                                                    val newSourcePages = source.getPageList(sChapter)"""

text = text.replace(old_code, new_code)

with open('app/src/main/java/com/example/wammy/ui/ReaderViewModel.kt', 'w') as f:
    f.write(text)

