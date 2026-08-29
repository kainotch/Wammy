import re

with open('app/src/main/java/com/example/wammy/ui/screens/ReaderScreen.kt', 'r') as f:
    text = f.read()

# 1. Background Theme Support
old_bg = """    Scaffold(
        containerColor = Color.Black,"""

new_bg = """    val readerThemeStr = com.example.wammy.AppContainer.readerPreferences.readerTheme.get()
    val bgColor = when (readerThemeStr) {
        com.example.wammy.data.prefs.ReaderTheme.BLACK -> Color.Black
        com.example.wammy.data.prefs.ReaderTheme.GRAY -> Color.DarkGray
        com.example.wammy.data.prefs.ReaderTheme.WHITE -> Color.White
        com.example.wammy.data.prefs.ReaderTheme.AUTOMATIC -> androidx.compose.material3.MaterialTheme.colorScheme.background
        else -> Color.Black
    }
    
    Scaffold(
        containerColor = bgColor,"""

text = text.replace(old_bg, new_bg)

# 2. Window Flags (Fullscreen, Keep Screen On)
old_effect = """    DisposableEffect(lifecycleOwner) {
        var startTime = System.currentTimeMillis()"""

new_effect = """    val context = androidx.compose.ui.platform.LocalContext.current
    val activity = context as? android.app.Activity
    val isFullscreen = com.example.wammy.AppContainer.readerPreferences.fullscreen.get()
    val keepScreenOn = com.example.wammy.AppContainer.readerPreferences.keepScreenOn.get()
    
    DisposableEffect(lifecycleOwner, isFullscreen, keepScreenOn) {
        if (activity != null) {
            val window = activity.window
            if (keepScreenOn) {
                window.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            } else {
                window.clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            }
            
            val insetsController = androidx.core.view.WindowCompat.getInsetsController(window, window.decorView)
            if (isFullscreen) {
                insetsController.hide(androidx.core.view.WindowInsetsCompat.Type.systemBars())
                insetsController.systemBarsBehavior = androidx.core.view.WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            } else {
                insetsController.show(androidx.core.view.WindowInsetsCompat.Type.systemBars())
            }
        }
        
        var startTime = System.currentTimeMillis()"""

text = text.replace(old_effect, new_effect)

# Clean up window flags on dispose
old_dispose = """        onDispose {
            if (startTime > 0) {
                val duration = System.currentTimeMillis() - startTime
                com.example.wammy.util.ReadDurationTracker.addDuration(currentContext, duration)
            }
        }"""

new_dispose = """        onDispose {
            if (activity != null) {
                val window = activity.window
                window.clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                val insetsController = androidx.core.view.WindowCompat.getInsetsController(window, window.decorView)
                insetsController.show(androidx.core.view.WindowInsetsCompat.Type.systemBars())
            }
            if (startTime > 0) {
                val duration = System.currentTimeMillis() - startTime
                com.example.wammy.util.ReadDurationTracker.addDuration(currentContext, duration)
            }
        }"""

text = text.replace(old_dispose, new_dispose)

with open('app/src/main/java/com/example/wammy/ui/screens/ReaderScreen.kt', 'w') as f:
    f.write(text)

