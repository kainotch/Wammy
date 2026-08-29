import re

with open('app/src/main/java/com/example/wammy/data/local/Entities.kt', 'r') as f:
    text = f.read()

old_manga = """    val novelPkgName: String? = null,
    val novelApkFile: String? = null,
    val readingMode: Int = 0 // 0=RTL, 1=LTR, 2=WEBTOON
)"""

new_manga = """    val novelPkgName: String? = null,
    val novelApkFile: String? = null,
    val readingMode: Int = 0, // 0=DEFAULT, 1=LTR, 2=RTL, 3=VERTICAL, 4=WEBTOON, 5=CONTINUOUS_VERTICAL
    val orientation: Int = 0 // 0=DEFAULT, 1=FREE, 2=PORTRAIT, 3=LANDSCAPE, 4=LOCKED_PORTRAIT, 5=LOCKED_LANDSCAPE, 6=REVERSE_PORTRAIT
)"""

text = text.replace(old_manga, new_manga)

with open('app/src/main/java/com/example/wammy/data/local/Entities.kt', 'w') as f:
    f.write(text)

