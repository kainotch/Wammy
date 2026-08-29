import re

with open('app/src/main/java/com/example/wammy/ui/screens/DetailsScreen.kt', 'r') as f:
    text = f.read()

old_btn = """                    // Download Button
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .background(Color(0xFF22222E), RoundedCornerShape(12.dp))
                            .clickable { 
                                val toDownload = chapters.filter { !downloadedChapters.contains(it.sourceUrl) }
                                if (toDownload.isNotEmpty() && !isCurrentlyDownloading) {
                                    viewModel.downloadAllChapters(context, toDownload)
                                }
                            }
                            .padding(vertical = 12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {"""

new_btn = """                    // Download Button
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .background(Color(0xFF22222E), RoundedCornerShape(12.dp))
                            .clickable { 
                                if (isCurrentlyDownloading) {
                                    com.example.wammy.util.DownloadManager.cancelDownload(safeManga.sourceUrl)
                                } else {
                                    val toDownload = chapters.filter { !downloadedChapters.contains(it.sourceUrl) }
                                    if (toDownload.isNotEmpty()) {
                                        viewModel.downloadAllChapters(context, toDownload)
                                    }
                                }
                            }
                            .padding(vertical = 12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {"""

text = text.replace(old_btn, new_btn)

# Ensure the UI text reflects that tapping it again cancels it
old_text = """Text(if (isDownloaded) "Cached" else if (isCurrentlyDownloading) "Caching..." else "Not Cached", color = Color.LightGray, fontSize = 12.sp)"""
new_text = """Text(if (isDownloaded) "Cached" else if (isCurrentlyDownloading) "Cancel" else "Not Cached", color = if (isCurrentlyDownloading) Color(0xFFFF6B6B) else Color.LightGray, fontSize = 12.sp)"""

text = text.replace(old_text, new_text)

with open('app/src/main/java/com/example/wammy/ui/screens/DetailsScreen.kt', 'w') as f:
    f.write(text)

