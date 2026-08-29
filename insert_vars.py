import re

with open('app/src/main/java/com/example/wammy/ui/screens/ReaderScreen.kt', 'r') as f:
    text = f.read()

# Remove the old colorFilter collectAsState completely
text = re.sub(r'\s*val colorFilter by viewModel\.colorFilter\.collectAsState\(\)', '', text)

# Insert new variables
new_vars = """
    val prefs = com.example.wammy.AppContainer.readerPreferences
    val showPageNumber by prefs.showPageNumber.state.collectAsState()
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
        com.example.wammy.data.prefs.ScaleType.FIT_SCREEN -> androidx.compose.ui.layout.ContentScale.Fit
        com.example.wammy.data.prefs.ScaleType.STRETCH -> androidx.compose.ui.layout.ContentScale.FillBounds
        com.example.wammy.data.prefs.ScaleType.FIT_WIDTH -> androidx.compose.ui.layout.ContentScale.FillWidth
        com.example.wammy.data.prefs.ScaleType.FIT_HEIGHT -> androidx.compose.ui.layout.ContentScale.FillHeight
        else -> androidx.compose.ui.layout.ContentScale.Fit
    }
"""

# Find where to insert (after readingMode)
text = re.sub(r'(val readingMode by viewModel\.readingMode\.collectAsState\(\))', r'\1' + new_vars, text)

with open('app/src/main/java/com/example/wammy/ui/screens/ReaderScreen.kt', 'w') as f:
    f.write(text)

