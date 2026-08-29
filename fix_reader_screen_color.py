import re

with open('app/src/main/java/com/example/wammy/ui/screens/ReaderScreen.kt', 'r') as f:
    text = f.read()

# 1. Collect preferences
old_collect = """    val pages by viewModel.pages.collectAsState()
    val readingMode by viewModel.readingMode.collectAsState()"""

new_collect = """    val pages by viewModel.pages.collectAsState()
    val readingMode by viewModel.readingMode.collectAsState()
    
    val prefs = com.example.wammy.AppContainer.readerPreferences
    val grayscale by prefs.grayscale.state.collectAsState()
    val invertedColors by prefs.invertedColors.state.collectAsState()
    val isCustomBrightness by prefs.customBrightness.state.collectAsState()
    val brightnessValue by prefs.customBrightnessValue.state.collectAsState()
    val isColorFilter by prefs.colorFilter.state.collectAsState()
    val colorR by prefs.colorFilterValueR.state.collectAsState()
    val colorG by prefs.colorFilterValueG.state.collectAsState()
    val colorB by prefs.colorFilterValueB.state.collectAsState()
    val colorA by prefs.colorFilterValueA.state.collectAsState()
    
    val scaleTypeStr by prefs.imageScaleType.state.collectAsState()
    val scaleType = when(scaleTypeStr) {
        com.example.wammy.data.prefs.ScaleType.FIT_SCREEN -> ContentScale.Fit
        com.example.wammy.data.prefs.ScaleType.STRETCH -> ContentScale.FillBounds
        com.example.wammy.data.prefs.ScaleType.FIT_WIDTH -> ContentScale.FillWidth
        com.example.wammy.data.prefs.ScaleType.FIT_HEIGHT -> ContentScale.FillHeight
        else -> ContentScale.Fit
    }"""

text = text.replace(old_collect, new_collect)

# 2. Update ZoomableImage Signature and usages
# Find all occurrences of ZoomableImage(
text = text.replace("filterMode: com.example.wammy.ui.ColorFilterMode,", 
                    "grayscale: Boolean,\n    invertedColors: Boolean,\n    isCustomBrightness: Boolean,\n    brightnessValue: Int,\n    isColorFilter: Boolean,\n    colorR: Int,\n    colorG: Int,\n    colorB: Int,\n    colorA: Int,")

# Update getColorFilterForMode call
text = text.replace("val colorFilter = remember(filterMode) { com.example.wammy.ui.screens.getColorFilterForMode(filterMode) }",
                    "val colorFilter = remember(grayscale, invertedColors) { com.example.wammy.ui.screens.getCustomColorFilter(grayscale, invertedColors) }")

# Update ZoomableImage content: add the overlay boxes
old_zoom_content = """                onClick = { offset ->
                    val x = offset.x
                    onTap(offset)
                }
            )"""

new_zoom_content = """                onClick = { offset ->
                    val x = offset.x
                    onTap(offset)
                }
            )
            
            // Custom Color Filter Overlay
            if (isColorFilter) {
                Box(modifier = Modifier.matchParentSize().background(Color(colorR, colorG, colorB, colorA)))
            }
            
            // Custom Brightness Overlay (when negative, draw black overlay)
            if (isCustomBrightness && brightnessValue < 0) {
                val alpha = (kotlin.math.abs(brightnessValue) / 100f).coerceIn(0f, 1f)
                Box(modifier = Modifier.matchParentSize().background(Color.Black.copy(alpha = alpha)))
            }"""

text = text.replace(old_zoom_content, new_zoom_content)

with open('app/src/main/java/com/example/wammy/ui/screens/ReaderScreen.kt', 'w') as f:
    f.write(text)

