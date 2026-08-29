import re

with open('app/src/main/java/com/example/wammy/ui/screens/DetailsScreen.kt', 'r') as f:
    text = f.read()

# Define old block exactly as it appears
old_block = """                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { handleReadClick(chapter) }
                        .padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // The explicitly requested 1, 2, 3 indexing (now descending to match Mihon)
                    Text(
                        text = "${displayChapters.size - index}",
                        color = Color.DarkGray,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.width(32.dp)
                    )
                    
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = chapter.name,
                            color = if (chapter.read) Color.Gray else androidx.compose.material3.MaterialTheme.colorScheme.onBackground,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium
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

new_block = """                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { handleReadClick(chapter) }
                        .padding(vertical = 12.dp, horizontal = 16.dp), // Added horizontal padding
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
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

text = text.replace(old_block, new_block)

with open('app/src/main/java/com/example/wammy/ui/screens/DetailsScreen.kt', 'w') as f:
    f.write(text)

