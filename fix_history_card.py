import re

with open('app/src/main/java/com/example/wammy/ui/screens/HistoryScreen.kt', 'r') as f:
    text = f.read()

# Replace HistoryItemRow
old_row_start = "@Composable\nfun HistoryItemRow("
old_row_end = "    }\n}"

start_idx = text.find(old_row_start)
end_idx = text.find(old_row_end, start_idx) + len("    }\n}")

new_row = """@Composable
fun HistoryItemRow(
    item: HistoryEntity,
    timeStr: String,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val onSurfaceColor = androidx.compose.material3.MaterialTheme.colorScheme.onSurface
    val onSurfaceVariantColor = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Poster
        AsyncImage(
            model = item.mangaCoverUrl,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(72.dp, 102.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(Color.DarkGray)
        )
        
        Spacer(modifier = Modifier.width(16.dp))
        
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = item.mangaTitle,
                color = onSurfaceColor,
                fontWeight = FontWeight.Bold, fontSize = 15.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(4.dp))
            val progressText = if (item.totalPages > 0) " (Page ${item.lastPageRead}/${item.totalPages})" else ""
            Text(
                text = "${item.chapterName}$progressText - $timeStr",
                color = onSurfaceVariantColor,
                fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis
            )
        }
        
        IconButton(onClick = { /* Add to library */ }) {
            Icon(Icons.Default.FavoriteBorder, contentDescription = "Favorite", tint = onSurfaceVariantColor)
        }
        
        IconButton(onClick = onDelete) {
            Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", tint = onSurfaceVariantColor)
        }
    }
}"""

text = text[:start_idx] + new_row + text[end_idx:]

with open('app/src/main/java/com/example/wammy/ui/screens/HistoryScreen.kt', 'w') as f:
    f.write(text)

