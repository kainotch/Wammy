import re

with open('app/src/main/java/com/example/wammy/ui/screens/DetailsScreen.kt', 'r') as f:
    text = f.read()

old_chapter_ui = """                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = chapter.name,
                            color = if (chapter.read) Color.Gray else Color.White,
                            style = MaterialTheme.typography.bodyLarge,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Chapter ${chapter.chapterNumber}",
                            color = Color.Gray,
                            fontSize = 12.sp
                        )
                    }
                    
                    if (chapter.read) {
                        Icon(Icons.Default.Check, contentDescription = "Read", tint = Color.Gray, modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                    }"""

new_chapter_ui = """                    Column(modifier = Modifier.weight(1f)) {
                        val titleText = if (chapter.read) chapter.name else "• ${chapter.name}"
                        Text(
                            text = titleText,
                            color = if (chapter.read) Color.Gray else Color(0xFFB388FF), // Primary purple for unread
                            style = MaterialTheme.typography.bodyLarge,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        
                        // Format date
                        val dateText = if (chapter.dateUpload > 0L) {
                            java.text.SimpleDateFormat("M/d/yy", java.util.Locale.getDefault()).format(java.util.Date(chapter.dateUpload))
                        } else {
                            "Unknown date"
                        }
                        
                        val subtitleText = if (!chapter.scanlator.isNullOrEmpty()) {
                            "$dateText • ${chapter.scanlator}"
                        } else {
                            dateText
                        }
                        
                        Text(
                            text = subtitleText,
                            color = Color.Gray,
                            fontSize = 12.sp
                        )
                    }"""

text = text.replace(old_chapter_ui, new_chapter_ui)

with open('app/src/main/java/com/example/wammy/ui/screens/DetailsScreen.kt', 'w') as f:
    f.write(text)

