import re

with open('app/src/main/java/com/example/wammy/ui/screens/ReaderScreen.kt', 'r') as f:
    text = f.read()

old_counter = """                val currentPageNum = (currentProgress * maxOf(1, pages.size - 1)).toInt().coerceIn(0, pages.size - 1) + 1
                Text(
                    text = "$currentPageNum / ${pages.size}",
                    color = Color.White,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                )"""

new_counter = """                val currentPageNum = (currentProgress * maxOf(1, pages.size - 1)).toInt().coerceIn(0, pages.size - 1) + 1
                Text(
                    text = "${if (chapterName.isNotBlank()) chapterName + " • " else ""}$currentPageNum / ${pages.size}",
                    color = Color.White,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Bold),
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )"""

text = text.replace(old_counter, new_counter)

with open('app/src/main/java/com/example/wammy/ui/screens/ReaderScreen.kt', 'w') as f:
    f.write(text)

