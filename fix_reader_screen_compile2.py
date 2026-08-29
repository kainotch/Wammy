import re

with open('app/src/main/java/com/example/wammy/ui/screens/ReaderScreen.kt', 'r') as f:
    text = f.read()

# Add config and density at the top of ReaderScreen
old_top = """fun ReaderScreen(
    viewModel: com.example.wammy.ui.ReaderViewModel = androidx.lifecycle.viewmodel.compose.viewModel(),
    onNavigateBack: () -> Unit
) {
    val pages by viewModel.pages.collectAsState()
    val readingMode by viewModel.readingMode.collectAsState()"""

new_top = """fun ReaderScreen(
    viewModel: com.example.wammy.ui.ReaderViewModel = androidx.lifecycle.viewmodel.compose.viewModel(),
    onNavigateBack: () -> Unit
) {
    val config = androidx.compose.ui.platform.LocalConfiguration.current
    val density = androidx.compose.ui.platform.LocalDensity.current.density
    val pxWidth = (config.screenWidthDp * density).toInt()
    val pxHeight = (config.screenHeightDp * density).toInt()

    val pages by viewModel.pages.collectAsState()
    val readingMode by viewModel.readingMode.collectAsState()"""

text = text.replace(old_top, new_top)

# Remove the inner declarations
bad_decl = """                                        val config = LocalConfiguration.current
                                        val density = LocalDensity.current.density
                                        val pxWidth = (config.screenWidthDp * density).toInt()
                                        val pxHeight = (config.screenHeightDp * density).toInt()"""

text = text.replace(bad_decl, "")

with open('app/src/main/java/com/example/wammy/ui/screens/ReaderScreen.kt', 'w') as f:
    f.write(text)

