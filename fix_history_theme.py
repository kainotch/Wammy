import re

with open('app/src/main/java/com/example/wammy/ui/screens/HistoryScreen.kt', 'r') as f:
    text = f.read()

# Fix TopAppBar Icons
text = text.replace('tint = Color.LightGray', 'tint = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant')

# Fix Date Header color
text = text.replace('color = Color.LightGray,\n                            style = MaterialTheme.typography.titleMedium', 'color = androidx.compose.material3.MaterialTheme.colorScheme.onBackground,\n                            style = MaterialTheme.typography.titleMedium')

# Fix HistoryItemRow
old_row = """    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp)
            .padding(vertical = 4.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF16161A))
            .clickable(onClick = onClick)
    ) {
        // Blurred Background
        AsyncImage(
            model = item.mangaCoverUrl,
            contentDescription = null,
            modifier = Modifier.fillMaxSize().alpha(0.6f),
            contentScale = ContentScale.Crop
        )
        // Gradient Overlay blending into black
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(androidx.compose.ui.graphics.Brush.horizontalGradient(
                    colorStops = arrayOf(
                        0.0f to Color.Black.copy(alpha = 0.1f),
                        0.3f to Color.Black.copy(alpha = 0.6f),
                        0.6f to Color.Black,
                        1.0f to Color.Black
                    )
                ))
        )
        
        Row(
            modifier = Modifier.fillMaxSize().padding(8.dp),
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
                    color = Color.White,
                    fontWeight = FontWeight.Bold, fontSize = 15.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                val progressText = if (item.totalPages > 0) " (Page ${item.lastPageRead}/${item.totalPages})" else ""
                Text(
                    text = "${item.chapterName}$progressText - $timeStr",
                    color = Color(0xFF90CAF9),
                    fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis
                )
            }
            
            IconButton(onClick = { /* Add to library */ }) {
                Icon(Icons.Default.FavoriteBorder, contentDescription = "Favorite", tint = Color.LightGray)
            }
            
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", tint = Color.LightGray)
            }
        }
    }"""

new_row = """    val surfaceColor = androidx.compose.material3.MaterialTheme.colorScheme.surfaceVariant
    val onSurfaceColor = androidx.compose.material3.MaterialTheme.colorScheme.onSurface
    val onSurfaceVariantColor = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp)
            .padding(vertical = 4.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(surfaceColor)
            .clickable(onClick = onClick)
    ) {
        // Blurred Background
        AsyncImage(
            model = item.mangaCoverUrl,
            contentDescription = null,
            modifier = Modifier.fillMaxSize().alpha(0.3f),
            contentScale = ContentScale.Crop
        )
        // Gradient Overlay blending into surface color
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(androidx.compose.ui.graphics.Brush.horizontalGradient(
                    colorStops = arrayOf(
                        0.0f to surfaceColor.copy(alpha = 0.1f),
                        0.3f to surfaceColor.copy(alpha = 0.6f),
                        0.6f to surfaceColor,
                        1.0f to surfaceColor
                    )
                ))
        )
        
        Row(
            modifier = Modifier.fillMaxSize().padding(8.dp),
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
                    color = androidx.compose.material3.MaterialTheme.colorScheme.primary,
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

text = text.replace(old_row, new_row)

with open('app/src/main/java/com/example/wammy/ui/screens/HistoryScreen.kt', 'w') as f:
    f.write(text)

