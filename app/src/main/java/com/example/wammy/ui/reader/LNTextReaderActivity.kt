package com.example.wammy.ui.reader

import android.annotation.SuppressLint
import android.app.Activity
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.lifecycleScope
import com.example.wammy.AppContainer
import com.example.wammy.theme.ThemeMode
import com.example.wammy.theme.WammyTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

class LNTextReaderActivity : ComponentActivity(), TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = null
    private var isTtsReady = false
    private var currentWebView: WebView? = null
    private var chapterPath: String? = null
    private var parsedSentences = listOf<String>()
    private var isLoadingNextChapter = false
    private var sessionStartTime = 0L

    // Read from session store
    private val chapterPaths get() = NovelSessionStore.chapterPaths
    private val chapterNames get() = NovelSessionStore.chapterNames
    private val activeChapterIndexState = androidx.compose.runtime.mutableIntStateOf(NovelSessionStore.currentIndex)
    private var currentChapterIndex get() = NovelSessionStore.currentIndex
        set(v) { 
            NovelSessionStore.currentIndex = v 
            activeChapterIndexState.intValue = v
        }
    private val apkFileName get() = NovelSessionStore.apkFile
    private val pkgName get() = NovelSessionStore.pkgName

    override fun onPause() {
        if (sessionStartTime > 0) {
            val duration = System.currentTimeMillis() - sessionStartTime
            com.example.wammy.util.ReadDurationTracker.addDuration(this, duration)
        }
        saveHistory()
        super.onPause()
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts?.language = Locale.US
            tts?.setOnUtteranceProgressListener(object : android.speech.tts.UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {
                    utteranceId?.toIntOrNull()?.let { id ->
                        runOnUiThread {
                            currentWebView?.evaluateJavascript("highlightTts($id)", null)
                        }
                    }
                }
                override fun onDone(utteranceId: String?) {}
                @Deprecated("Deprecated in Java")
                override fun onError(utteranceId: String?) {}
            })
            isTtsReady = true
        }
    }

    override fun onDestroy() {
        tts?.stop()
        tts?.shutdown()
        super.onDestroy()
    }


    private fun saveHistory() {
        val novelUrl = NovelSessionStore.novelUrl
        if (novelUrl.isEmpty()) return
        lifecycleScope.launch(Dispatchers.IO) {
            val manga = AppContainer.database.mangaDao().getMangaByUrl(novelUrl)
            if (manga != null) {
                val existingHistory = AppContainer.database.historyDao().getHistoryByManga(novelUrl)
                val chapterUrl = chapterPath ?: ""
                val chapterName = chapterNames.getOrNull(currentChapterIndex) ?: ""
                
                val history = com.example.wammy.data.local.HistoryEntity(
                    id = existingHistory?.id ?: 0L,
                    mangaSourceUrl = manga.sourceUrl,
                    mangaTitle = manga.titleRomaji,
                    mangaCoverUrl = manga.coverImageUrl ?: "",
                    chapterName = chapterName,
                    chapterSourceUrl = chapterUrl,
                    lastPageRead = currentChapterIndex, // For novels, page means chapter index in history
                    totalPages = chapterPaths.size,
                    lastReadTimestamp = System.currentTimeMillis()
                )
                AppContainer.database.historyDao().insertHistory(history)
            }
        }
    }

    private fun loadNextChapter() {
        if (isLoadingNextChapter) return
        val nextIndex = currentChapterIndex + 1
        if (nextIndex >= chapterPaths.size) return
        isLoadingNextChapter = true
        val nextPath = chapterPaths[nextIndex]
        val nextName = chapterNames.getOrNull(nextIndex) ?: "Chapter ${nextIndex + 1}"

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val loader = com.example.wammy.lnreader.loader.LNReaderPluginLoader(this@LNTextReaderActivity)
                val pluginFile = java.io.File(filesDir, "ir_extensions/$apkFileName")
                var nextHtml = ""
                val plugin = com.example.wammy.lnreader.plugin.PluginRegistry.find(pkgName)
                if (plugin != null) {
                    nextHtml = plugin.parseChapter(nextPath)
                } else if (apkFileName.isNotEmpty()) {
                    nextHtml = loader.parseChapter(pluginFile, nextPath)
                }
                if (nextHtml.isNotEmpty()) {
                    val parsedNext = NovelHtmlParser.injectTtsSpans(nextHtml)
                    parsedSentences = parsedSentences + parsedNext.second
                    val divider = "<hr style='border:none;border-top:1px solid #444;margin:40px 0 20px;'><p style='text-align:center;opacity:0.5;font-size:13px;'>— $nextName —</p>"
                    val safeHtml = (divider + parsedNext.first)
                        .replace("\\", "\\\\").replace("`", "\\`")
                    val appendJs = "document.body.innerHTML += `$safeHtml`;"
                    withContext(Dispatchers.Main) {
                        currentWebView?.evaluateJavascript(appendJs, null)
                        currentChapterIndex = nextIndex
                        chapterPath = nextPath
                        isLoadingNextChapter = false
                        saveHistory()
                    }
                } else {
                    isLoadingNextChapter = false
                }
            } catch (e: Exception) {
                e.printStackTrace()
                isLoadingNextChapter = false
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @SuppressLint("SetJavaScriptEnabled")
    
    override fun onResume() {
        super.onResume()
        sessionStartTime = System.currentTimeMillis()
    }



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        activeChapterIndexState.intValue = NovelSessionStore.currentIndex
        tts = TextToSpeech(this, this)

        val htmlCachePath = intent.getStringExtra("HTML_CACHE_PATH")
        var rawHtml = if (htmlCachePath != null) {
            try { java.io.File(htmlCachePath).readText() } catch (e: Exception) { "<p>Failed to load.</p>" }
        } else {
            intent.getStringExtra("HTML_CONTENT") ?: "<p>Failed to load.</p>"
        }
        chapterPath = intent.getStringExtra("CHAPTER_PATH")

        // Clean up common noise
        rawHtml = rawHtml.replace(Regex("<p>.*?(Translator's Note|TL Note).*?</p>", RegexOption.IGNORE_CASE), "")

        // Inject TTS spans
        val parsed = NovelHtmlParser.injectTtsSpans(rawHtml)
        rawHtml = parsed.first
        parsedSentences = parsed.second

        setContent {
            val themeMode by AppContainer.themePreferences.themeMode.collectAsState()
            val appTheme by AppContainer.themePreferences.appTheme.collectAsState()
            val amoled by AppContainer.themePreferences.amoled.collectAsState()
            val isDarkTheme = when (themeMode) {
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
                ThemeMode.SYSTEM -> androidx.compose.foundation.isSystemInDarkTheme()
            }
            WammyTheme(appTheme = appTheme, amoled = amoled, darkTheme = isDarkTheme) {
                NovelReaderScreen(
                    rawHtml = rawHtml,
                    chapterName = chapterNames.getOrNull(activeChapterIndexState.intValue) ?: "Chapter",
                    onBack = { finish() },
                    onTtsToggle = { isPlaying ->
                        if (isPlaying) {
                            tts?.stop()
                        } else if (isTtsReady) {
                            parsedSentences.forEachIndexed { index, text ->
                                val mode = if (index == 0) TextToSpeech.QUEUE_FLUSH else TextToSpeech.QUEUE_ADD
                                tts?.speak(text, mode, null, index.toString())
                            }
                        }
                    },
                    onWebViewReady = { wv -> currentWebView = wv },
                    onNearBottom = { loadNextChapter() },
                    chapterPath = chapterPath,
                    onActiveChapterChanged = { idx -> currentChapterIndex = idx }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun NovelReaderScreen(
    rawHtml: String,
    chapterName: String,
    onBack: () -> Unit,
    onTtsToggle: (Boolean) -> Unit,
    onWebViewReady: (WebView) -> Unit,
    onNearBottom: () -> Unit,
    chapterPath: String?,
    onActiveChapterChanged: (Int) -> Unit = {}
) {
    val context = LocalContext.current
    var showOverlay by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }
    var isPlaying by remember { mutableStateOf(false) }
    var fontSize by remember { mutableStateOf(18) }
    var lineSpacing by remember { mutableStateOf(1.75f) }
    var isWarmLightEnabled by remember { mutableStateOf(false) }

    val colorScheme = MaterialTheme.colorScheme

    DisposableEffect(showOverlay) {
        val window = (context as? Activity)?.window
        if (window != null) {
            val controller = WindowCompat.getInsetsController(window, window.decorView)
            controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            if (showOverlay) controller.show(WindowInsetsCompat.Type.systemBars())
            else controller.hide(WindowInsetsCompat.Type.systemBars())
        }
        onDispose {
            val window = (context as? Activity)?.window
            if (window != null) WindowCompat.getInsetsController(window, window.decorView)
                .show(WindowInsetsCompat.Type.systemBars())
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(colorScheme.background)) {

        // ── WebView ──────────────────────────────────────────────────────────
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                WebView(ctx).apply {
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    settings.builtInZoomControls = false
                    settings.displayZoomControls = false
                    setBackgroundColor(android.graphics.Color.TRANSPARENT)
                    addJavascriptInterface(object {
                        @JavascriptInterface fun toggleOverlay() {
                            (ctx as? Activity)?.runOnUiThread { showOverlay = !showOverlay }
                        }
                        @JavascriptInterface fun updateScrollProgress(pct: Float) {
                            chapterPath?.let { path ->
                                ctx.getSharedPreferences("ln_progress", android.content.Context.MODE_PRIVATE)
                                    .edit().putFloat(path, pct).apply()
                            }
                        }
                        @JavascriptInterface fun onNearBottom() {
                            (ctx as? Activity)?.runOnUiThread { onNearBottom() }
                        }
                        @JavascriptInterface fun updateActiveChapter(idx: Int) {
                            (ctx as? Activity)?.runOnUiThread { onActiveChapterChanged(idx) }
                        }
                    }, "AndroidInterface")

                    webViewClient = object : WebViewClient() {
                        override fun onPageFinished(view: WebView?, url: String?) {
                            super.onPageFinished(view, url)
                            onWebViewReady(view ?: return)

                            // Restore scroll
                            chapterPath?.let { path ->
                                val pct = ctx.getSharedPreferences("ln_progress", android.content.Context.MODE_PRIVATE)
                                    .getFloat(path, 0f)
                                if (pct > 0f) view.evaluateJavascript(
                                    "setTimeout(() => { window.scrollTo(0, document.body.scrollHeight * $pct); }, 200);", null
                                )
                            }

                            view?.evaluateJavascript("""
                                document.body.addEventListener('click', function() {
                                    if (window.getSelection().toString().length === 0) AndroidInterface.toggleOverlay();
                                });
                                let _st; window.addEventListener('scroll', function() {
                                    clearTimeout(_st); _st = setTimeout(function() {
                                        var pct = window.scrollY / (document.body.scrollHeight - window.innerHeight);
                                        if (pct > 0) AndroidInterface.updateScrollProgress(pct);
                                        if (pct > 0.9) AndroidInterface.onNearBottom();
                                        
                                        var markers = document.querySelectorAll('.chapter-marker');
                                        var visibleIndex = -1;
                                        for (var i = markers.length - 1; i >= 0; i--) {
                                            var rect = markers[i].getBoundingClientRect();
                                            if (rect.top <= window.innerHeight / 3) {
                                                visibleIndex = parseInt(markers[i].getAttribute('data-index'));
                                                break;
                                            }
                                        }
                                        if (visibleIndex === -1 && markers.length > 0) {
                                            visibleIndex = parseInt(markers[0].getAttribute('data-index'));
                                        }
                                        if (visibleIndex >= 0) {
                                            AndroidInterface.updateActiveChapter(visibleIndex);
                                        }
                                    }, 400);
                                });
                            """.trimIndent(), null)
                        }
                    }
                }
            },
            update = { webView ->
                val html = NovelWebViewStyler.generateHtml(
                    rawContent = rawHtml,
                    backgroundColor = colorScheme.background,
                    textColor = colorScheme.onBackground,
                    primaryColor = colorScheme.primary,
                    fontSize = fontSize,
                    lineSpacing = lineSpacing
                )
                webView.loadDataWithBaseURL(null, html, "text/html", "UTF-8", null)
            }
        )

        // ── Warm Light Overlay ───────────────────────────────────────────────
        if (isWarmLightEnabled) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFFFF9900).copy(alpha = 0.15f))
            )
        }

        // ── Top Bar ──────────────────────────────────────────────────────────
        AnimatedVisibility(
            visible = showOverlay,
            enter = fadeIn(), exit = fadeOut(),
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(colorScheme.surface.copy(alpha = 0.95f))
                    .statusBarsPadding()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().height(56.dp).padding(horizontal = 4.dp)
                ) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = colorScheme.onSurface)
                    }
                    Text(
                        text = chapterName,
                        style = MaterialTheme.typography.titleMedium,
                        color = colorScheme.onSurface,
                        maxLines = 1,
                        modifier = Modifier.weight(1f).padding(horizontal = 8.dp)
                    )
                    // Warm Light toggle
                    IconButton(onClick = { isWarmLightEnabled = !isWarmLightEnabled }) {
                        Icon(
                            if (isWarmLightEnabled) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                            "Warm Light",
                            tint = if (isWarmLightEnabled) Color(0xFFFF9900) else colorScheme.onSurface
                        )
                    }
                    // TTS toggle
                    IconButton(onClick = {
                        onTtsToggle(isPlaying)
                        isPlaying = !isPlaying
                    }) {
                        Icon(
                            if (isPlaying) Icons.Default.VolumeOff else Icons.Default.VolumeUp,
                            "TTS",
                            tint = if (isPlaying) colorScheme.primary else colorScheme.onSurface
                        )
                    }
                    // Settings
                    IconButton(onClick = { showSettings = true }) {
                        Icon(Icons.Default.Settings, "Settings", tint = colorScheme.onSurface)
                    }
                }
            }
        }

        // ── Bottom Bar ───────────────────────────────────────────────────────
        AnimatedVisibility(
            visible = showOverlay,
            enter = fadeIn(), exit = fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(colorScheme.surface.copy(alpha = 0.95f))
                    .navigationBarsPadding()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    modifier = Modifier.fillMaxWidth().height(56.dp)
                ) {
                    // Font size control
                    IconButton(onClick = { if (fontSize > 12) fontSize-- }) {
                        Text("A", fontSize = 14.sp, color = colorScheme.onSurface)
                    }
                    Text("$fontSize px", color = colorScheme.onSurface, fontSize = 13.sp)
                    IconButton(onClick = { if (fontSize < 32) fontSize++ }) {
                        Text("A", fontSize = 20.sp, color = colorScheme.onSurface, fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.width(24.dp))
                    // Line spacing control
                    IconButton(onClick = { if (lineSpacing > 1.2f) lineSpacing = (lineSpacing - 0.1f).coerceAtLeast(1.2f) }) {
                        Icon(Icons.Default.UnfoldLess, "Decrease spacing", tint = colorScheme.onSurface, modifier = Modifier.size(20.dp))
                    }
                    Text("≡", color = colorScheme.onSurface, fontSize = 16.sp)
                    IconButton(onClick = { if (lineSpacing < 3f) lineSpacing = (lineSpacing + 0.1f).coerceAtMost(3f) }) {
                        Icon(Icons.Default.UnfoldMore, "Increase spacing", tint = colorScheme.onSurface, modifier = Modifier.size(20.dp))
                    }
                }
            }
        }
    }
}
