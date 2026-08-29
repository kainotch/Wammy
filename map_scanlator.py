import re

with open('app/src/main/java/com/example/wammy/ui/DetailsViewModel.kt', 'r') as f:
    text = f.read()

old_mapping = """                                        name = sCh.name ?: ("Chapter " + (index + 1)),
                                        sourceUrl = sCh.url,
                                        chapterNumber = if (parsedNumber >= 0f) parsedNumber else (sChapters.size - index).toFloat(),
                                        dateUpload = sCh.date_upload
                                    )"""

new_mapping = """                                        name = sCh.name ?: ("Chapter " + (index + 1)),
                                        sourceUrl = sCh.url,
                                        chapterNumber = if (parsedNumber >= 0f) parsedNumber else (sChapters.size - index).toFloat(),
                                        dateUpload = sCh.date_upload,
                                        scanlator = sCh.scanlator
                                    )"""

text = text.replace(old_mapping, new_mapping)

with open('app/src/main/java/com/example/wammy/ui/DetailsViewModel.kt', 'w') as f:
    f.write(text)

