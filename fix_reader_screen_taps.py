import re

with open('app/src/main/java/com/example/wammy/ui/screens/ReaderScreen.kt', 'r') as f:
    text = f.read()

# Add imports for NavigationSystem
imports = """import androidx.compose.ui.geometry.Offset
import com.example.wammy.ui.reader.navigation.NavigationLayouts
import com.example.wammy.ui.reader.navigation.TapAction
import com.example.wammy.ui.reader.navigation.TapRegion"""

if "import com.example.wammy.ui.reader.navigation.TapAction" not in text:
    text = text.replace("import androidx.compose.ui.Alignment", imports + "\nimport androidx.compose.ui.Alignment")

# Find the HorizontalPager in the LTR/RTL branch and the Fallback branch
# We will use regex to find onTap = { zone -> ... } and replace it

def replace_ontap(match):
    return """onTap = { offset ->
                                        val layoutIndex = AppContainer.readerPreferences.navigationModePager.get()
                                        val invertMode = AppContainer.readerPreferences.pagerNavInverted.get()
                                        val isVert = readingMode == com.example.wammy.data.prefs.ReadingMode.VERTICAL
                                        val layout = NavigationLayouts.getLayout(layoutIndex, false, isVert)
                                        
                                        // We need the screen dimensions. Since BoxWithConstraints is not here, we can use LocalConfiguration
                                        // Or we can just use the PagerState to infer screen size roughly, but it's better to get the exact width
                                        // Wait, the easiest way to get viewport width inside a Composable is LocalConfiguration
                                        val config = LocalConfiguration.current
                                        val width = config.screenWidthDp // This is in DP, but we need pixels if we use offset.x which is in pixels
                                        val density = LocalDensity.current.density
                                        val pxWidth = (width * density).toInt()
                                        val pxHeight = (config.screenHeightDp * density).toInt()
                                        
                                        val action = NavigationLayouts.resolveTap(offset, pxWidth, pxHeight, layout, invertMode)
                                        
                                        // Translate LEFT/RIGHT to PREV/NEXT based on reading direction
                                        val finalAction = when (action) {
                                            TapAction.LEFT -> if (isRtl) TapAction.NEXT else TapAction.PREV
                                            TapAction.RIGHT -> if (isRtl) TapAction.PREV else TapAction.NEXT
                                            else -> action
                                        }
                                        
                                        if (finalAction == TapAction.MENU) {
                                            showOverlay = !showOverlay
                                        } else if (finalAction == TapAction.PREV) {
                                            coroutineScope.launch { pagerState.animateScrollToPage(maxOf(0, pagerState.currentPage - 1)) }
                                        } else if (finalAction == TapAction.NEXT) {
                                            coroutineScope.launch { pagerState.animateScrollToPage(minOf(pages.size, pagerState.currentPage + 1)) }
                                        }
                                    }"""

# LTR/RTL branch 
text = re.sub(r'onTap = \{ zone ->\s+if \(zone == TapZone\.CENTER\)[^{}]*\}\s+else if \(zone == TapZone\.LEFT\)[^{}]*\}\s+else[^{}]*\}\s+\}', replace_ontap, text, count=0)
text = re.sub(r'onTap = \{ zone ->\s+if \(zone == TapZone\.CENTER\)[^{}]*showOverlay = !showOverlay\s+else if \(zone == TapZone\.LEFT\)[^{}]*pagerState\.animateScrollToPage.*?\}\s+else[^{}]*pagerState\.animateScrollToPage.*?\}\s+\}', replace_ontap, text, count=0)

with open('app/src/main/java/com/example/wammy/ui/screens/ReaderScreen.kt', 'w') as f:
    f.write(text)

