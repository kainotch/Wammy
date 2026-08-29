import re

with open('app/src/main/java/com/example/wammy/ui/screens/ReaderScreen.kt', 'r') as f:
    text = f.read()

# Replace the "Retrying in background..." text and make the Box clickable if it's an error
old_error_ui = """                if (model.state == com.example.wammy.ui.PageState.ERROR) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Error Loading Page", color = Color.Red, style = MaterialTheme.typography.titleMedium)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Retrying in background...", color = Color.Gray, style = MaterialTheme.typography.bodySmall)
                    }
                } else {"""

new_error_ui = """                if (model.state == com.example.wammy.ui.PageState.ERROR) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable { 
                        // Tap to retry!
                        onTap(com.example.wammy.ui.screens.TapZone.CENTER) // We will handle retry in the ViewModel, but for now we need a callback. Wait, we can't easily pass the retry callback here without changing the signature. Let's just change the signature!
                    }) {
                        Text("Error Loading Page", color = Color.Red, style = MaterialTheme.typography.titleMedium)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Tap to Retry", color = Color.White, style = MaterialTheme.typography.bodySmall, modifier = Modifier.background(Color.DarkGray, shape = androidx.compose.foundation.shape.RoundedCornerShape(4.dp)).padding(8.dp))
                    }
                } else {"""

# We need to add a retry callback to ZoomableImage
old_sig = """fun ZoomableImage(
    model: com.example.wammy.ui.ReaderPage, 
    contentScale: ContentScale, 
    modifier: Modifier, 
    filterMode: com.example.wammy.ui.ColorFilterMode,
    onTap: (com.example.wammy.ui.screens.TapZone) -> Unit
) {"""

new_sig = """fun ZoomableImage(
    model: com.example.wammy.ui.ReaderPage, 
    contentScale: ContentScale, 
    modifier: Modifier, 
    filterMode: com.example.wammy.ui.ColorFilterMode,
    onTap: (com.example.wammy.ui.screens.TapZone) -> Unit,
    onRetry: () -> Unit = {}
) {"""

# And update the click handler
new_error_ui_real = """                if (model.state == com.example.wammy.ui.PageState.ERROR) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable { onRetry() }.padding(16.dp)) {
                        Text("Error Loading Page", color = Color.Red, style = MaterialTheme.typography.titleMedium)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Tap to Retry", color = Color.White, style = MaterialTheme.typography.bodySmall, modifier = Modifier.background(Color.DarkGray, shape = androidx.compose.foundation.shape.RoundedCornerShape(4.dp)).padding(8.dp))
                    }
                } else {"""

text = text.replace(old_error_ui, new_error_ui_real)
text = text.replace(old_sig, new_sig)

# Now update where ZoomableImage is called in ReaderScreen.kt
old_call = """                                ZoomableImage(
                                    model = pages[page],
                                    contentScale = ContentScale.Fit,
                                    modifier = Modifier.fillMaxSize(),
                                    filterMode = colorFilter,
                                    onTap = { zone ->"""

new_call = """                                ZoomableImage(
                                    model = pages[page],
                                    contentScale = ContentScale.Fit,
                                    modifier = Modifier.fillMaxSize(),
                                    filterMode = colorFilter,
                                    onRetry = { viewModel.retryPage(pages[page].index) },
                                    onTap = { zone ->"""

text = text.replace(old_call, new_call)

with open('app/src/main/java/com/example/wammy/ui/screens/ReaderScreen.kt', 'w') as f:
    f.write(text)

