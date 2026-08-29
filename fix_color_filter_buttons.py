import re

with open('app/src/main/java/com/example/wammy/ui/screens/ReaderScreen.kt', 'r') as f:
    text = f.read()

old_block = """                    Text("Color Filter", color = Color.White, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = 16.dp))
                    Row(
                        modifier = Modifier,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Button(
                            onClick = { viewModel.setColorFilter(com.example.wammy.ui.ColorFilterMode.NONE); showSettingsSheet = false },
                            colors = ButtonDefaults.buttonColors(containerColor = if (colorFilter == com.example.wammy.ui.ColorFilterMode.NONE) Color(0xFFB388FF) else Color(0xFF2E2E3A), contentColor = if (colorFilter == com.example.wammy.ui.ColorFilterMode.NONE) Color(0xFF0D0D1A) else Color.White)
                        ) { Text("None") }
                        Button(
                            onClick = { viewModel.setColorFilter(com.example.wammy.ui.ColorFilterMode.INVERT); showSettingsSheet = false },
                            colors = ButtonDefaults.buttonColors(containerColor = if (colorFilter == com.example.wammy.ui.ColorFilterMode.INVERT) Color(0xFFB388FF) else Color(0xFF2E2E3A), contentColor = if (colorFilter == com.example.wammy.ui.ColorFilterMode.INVERT) Color(0xFF0D0D1A) else Color.White)
                        ) { Text("Invert") }
                        Button(
                            onClick = { viewModel.setColorFilter(com.example.wammy.ui.ColorFilterMode.SEPIA); showSettingsSheet = false },
                            colors = ButtonDefaults.buttonColors(containerColor = if (colorFilter == com.example.wammy.ui.ColorFilterMode.SEPIA) Color(0xFFB388FF) else Color(0xFF2E2E3A), contentColor = if (colorFilter == com.example.wammy.ui.ColorFilterMode.SEPIA) Color(0xFF0D0D1A) else Color.White)
                        ) { Text("Sepia") }
                    }"""

new_block = """                    // Color filter buttons removed because filters are now managed via ReaderPreferences"""

text = text.replace(old_block, new_block)

with open('app/src/main/java/com/example/wammy/ui/screens/ReaderScreen.kt', 'w') as f:
    f.write(text)

