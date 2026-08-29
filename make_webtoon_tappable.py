import re

with open('app/src/main/java/com/example/wammy/ui/screens/ReaderScreen.kt', 'r') as f:
    text = f.read()

old_webtoon_error = """                                    if (imageUrl.state == com.example.wammy.ui.PageState.ERROR) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text("Error Loading Page", color = Color.Red, style = MaterialTheme.typography.titleMedium)
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Text("Retrying in background...", color = Color.Gray, style = MaterialTheme.typography.bodySmall)
                                        }"""

new_webtoon_error = """                                    if (imageUrl.state == com.example.wammy.ui.PageState.ERROR) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable { viewModel.retryPage(imageUrl.index) }.padding(16.dp)) {
                                            Text("Error Loading Page", color = Color.Red, style = MaterialTheme.typography.titleMedium)
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Text("Tap to Retry", color = Color.White, style = MaterialTheme.typography.bodySmall, modifier = Modifier.background(Color.DarkGray, shape = androidx.compose.foundation.shape.RoundedCornerShape(4.dp)).padding(8.dp))
                                        }"""

text = text.replace(old_webtoon_error, new_webtoon_error)

with open('app/src/main/java/com/example/wammy/ui/screens/ReaderScreen.kt', 'w') as f:
    f.write(text)

