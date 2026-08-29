import re

with open('app/src/main/java/com/example/wammy/ui/screens/LibraryScreen.kt', 'r') as f:
    text = f.read()

# 1. Fix the Stats bar background and text color
stats_bar_old = """.background(Color(0xFF16161A))
                            .padding(vertical = 16.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable { selectedFilter = "Entries" }) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.List, contentDescription = null, tint = if (selectedFilter == "Entries") Color(0xFF2E65F3) else Color.Gray, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("${stats.entries}", color = if (selectedFilter == "Entries") Color(0xFF2E65F3) else Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                            }
                            Text("Entries", color = Color.Gray, fontSize = 11.sp)
                        }
                        Box(modifier = Modifier.width(1.dp).height(30.dp).background(Color.DarkGray))
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable { selectedFilter = "Favorites" }) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Star, contentDescription = null, tint = if (selectedFilter == "Favorites") Color(0xFFFFB703) else Color.Gray, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("${stats.favorites}", color = if (selectedFilter == "Favorites") Color(0xFFFFB703) else Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            }
                            Text("Favorites", color = Color.Gray, fontSize = 11.sp)
                        }
                        Box(modifier = Modifier.width(1.dp).height(30.dp).background(Color.DarkGray))
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable { selectedFilter = "Completed" }) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = if (selectedFilter == "Completed") Color(0xFF4CAF50) else Color.Gray, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("${stats.completed}", color = if (selectedFilter == "Completed") Color(0xFF4CAF50) else Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            }
                            Text("Completed", color = Color.Gray, fontSize = 11.sp)
                        }"""

stats_bar_new = """.background(androidx.compose.material3.MaterialTheme.colorScheme.surfaceVariant)
                            .padding(vertical = 16.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val onSurface = androidx.compose.material3.MaterialTheme.colorScheme.onSurface
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable { selectedFilter = "Entries" }) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.List, contentDescription = null, tint = if (selectedFilter == "Entries") Color(0xFF2E65F3) else Color.Gray, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("${stats.entries}", color = if (selectedFilter == "Entries") Color(0xFF2E65F3) else onSurface, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                            }
                            Text("Entries", color = Color.Gray, fontSize = 11.sp)
                        }
                        Box(modifier = Modifier.width(1.dp).height(30.dp).background(Color.DarkGray))
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable { selectedFilter = "Favorites" }) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Star, contentDescription = null, tint = if (selectedFilter == "Favorites") Color(0xFFFFB703) else Color.Gray, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("${stats.favorites}", color = if (selectedFilter == "Favorites") Color(0xFFFFB703) else onSurface, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            }
                            Text("Favorites", color = Color.Gray, fontSize = 11.sp)
                        }
                        Box(modifier = Modifier.width(1.dp).height(30.dp).background(Color.DarkGray))
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable { selectedFilter = "Completed" }) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = if (selectedFilter == "Completed") Color(0xFF4CAF50) else Color.Gray, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("${stats.completed}", color = if (selectedFilter == "Completed") Color(0xFF4CAF50) else onSurface, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            }
                            Text("Completed", color = Color.Gray, fontSize = 11.sp)
                        }"""

text = text.replace(stats_bar_old, stats_bar_new)

# 2. Fix Continue Reading Box (Remove background, blur, and gradient)
continue_reading_old = """                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp)
                                .padding(vertical = 4.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFF16161A))
                                .clickable { 
                                    if (selectedFilter == "Novels") {
                                        val intent = android.content.Intent(context, com.example.wammy.ui.reader.LNNovelDetailsActivity::class.java).apply {
                                            putExtra("APK_FILE", m?.novelApkFile)
                                            putExtra("PKG_NAME", m?.novelPkgName)
                                            putExtra("NOVEL_URL", history.mangaSourceUrl)
                                            putExtra("NOVEL_TITLE", history.mangaTitle)
                                        }
                                        context.startActivity(intent)
                                    } else {
                                        onMangaClick(history.chapterSourceUrl, false)
                                    }
                                }
                        ) {
                            // Blurred Background
                            AsyncImage(
                                model = history.mangaCoverUrl,
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                            // Gradient Overlay
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(androidx.compose.ui.graphics.Brush.horizontalGradient(
                                        colors = listOf(Color.Black.copy(alpha = 0.8f), Color.Black.copy(alpha = 0.5f), Color.Black.copy(alpha = 0.8f))
                                    ))
                            )
                            
                            Row(
                                modifier = Modifier.fillMaxSize().padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Poster
                                AsyncImage(
                                    model = history.mangaCoverUrl,
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .size(72.dp, 102.dp)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(Color.DarkGray)
                                )
                                
                                Spacer(modifier = Modifier.width(16.dp))
                                
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(history.mangaTitle, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(history.chapterName, color = Color(0xFF90CAF9), fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                            }
                        }"""

continue_reading_new = """                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp)
                                .clickable { 
                                    if (selectedFilter == "Novels") {
                                        val intent = android.content.Intent(context, com.example.wammy.ui.reader.LNNovelDetailsActivity::class.java).apply {
                                            putExtra("APK_FILE", m?.novelApkFile)
                                            putExtra("PKG_NAME", m?.novelPkgName)
                                            putExtra("NOVEL_URL", history.mangaSourceUrl)
                                            putExtra("NOVEL_TITLE", history.mangaTitle)
                                        }
                                        context.startActivity(intent)
                                    } else {
                                        onMangaClick(history.chapterSourceUrl, false)
                                    }
                                },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val onSurfaceColor = androidx.compose.material3.MaterialTheme.colorScheme.onSurface
                            val onSurfaceVariantColor = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant
                            
                            // Poster
                            AsyncImage(
                                model = history.mangaCoverUrl,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(72.dp, 102.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color.DarkGray)
                            )
                            
                            Spacer(modifier = Modifier.width(16.dp))
                            
                            Column(modifier = Modifier.weight(1f)) {
                                Text(history.mangaTitle, color = onSurfaceColor, fontWeight = FontWeight.Bold, fontSize = 15.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(history.chapterName, color = onSurfaceVariantColor, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }"""

text = text.replace(continue_reading_old, continue_reading_new)

# 3. Fix LibraryItem (List mode)
library_item_old_start = "@Composable\nfun LibraryItem(manga: MangaEntity, modifier: Modifier = Modifier, onClick: () -> Unit, onLongClick: (() -> Unit)? = null) {"
library_item_old_end = "    }\n}"

start_idx = text.find(library_item_old_start)
end_idx = text.find(library_item_old_end, start_idx) + len(library_item_old_end)

library_item_new = """@Composable
fun LibraryItem(manga: MangaEntity, modifier: Modifier = Modifier, onClick: () -> Unit, onLongClick: (() -> Unit)? = null) {
    val onSurfaceColor = androidx.compose.material3.MaterialTheme.colorScheme.onSurface
    val onSurfaceVariantColor = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick ?: {}),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Poster
        AsyncImage(
            model = manga.coverImageUrl,
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
                text = manga.titleRomaji,
                color = onSurfaceColor,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = manga.sourceName,
                color = onSurfaceVariantColor,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}"""

text = text[:start_idx] + library_item_new + text[end_idx:]

with open('app/src/main/java/com/example/wammy/ui/screens/LibraryScreen.kt', 'w') as f:
    f.write(text)

