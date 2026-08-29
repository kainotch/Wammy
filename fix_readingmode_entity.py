import re

with open('app/src/main/java/com/example/wammy/data/local/Entities.kt', 'r') as f:
    text = f.read()

text = text.replace("val novelApkFile: String? = null", "val novelApkFile: String? = null,\n    val readingMode: Int = 0 // 0=RTL, 1=LTR, 2=WEBTOON")

with open('app/src/main/java/com/example/wammy/data/local/Entities.kt', 'w') as f:
    f.write(text)

