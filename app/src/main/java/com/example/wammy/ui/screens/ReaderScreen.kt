package com.example.wammy.ui.screens
import com.example.wammy.data.prefs.ReadingMode
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowCompat
import android.app.Activity
// Created by Notch

import androidx.compose.foundation.background
import kotlinx.coroutines.launch
import androidx.compose.foundation.clickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
  import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.FormatLineSpacing
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import me.saket.telephoto.zoomable.zoomable
import me.saket.telephoto.zoomable.rememberZoomableState
import com.example.wammy.ui.reader.navigation.NavigationLayouts
import com.example.wammy.ui.reader.navigation.TapAction
import com.example.wammy.ui.reader.navigation.TapRegion
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import com.example.wammy.ui.ReaderViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.runtime.DisposableEffect

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun ReaderScreen(
    viewModel: ReaderViewModel = viewModel(),
    onBack: () -> Unit
) {
    val currentContext = androidx.compose.ui.platform.LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val config = androidx.compose.ui.platform.LocalConfiguration.current
    val density = androidx.compose.ui.platform.LocalDensity.current.density
    val pxWidth = (config.screenWidthDp * density).toInt()
    val pxHeight = (config.screenHeightDp * density).toInt()
    val context = androidx.compose.ui.platform.LocalContext.current
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
        
        var startTime = System.currentTimeMillis()
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                startTime = System.currentTimeMillis()
            } else if (event == Lifecycle.Event.ON_PAUSE) {
                if (startTime > 0) {
                    val duration = System.currentTimeMillis() - startTime
                    com.example.wammy.util.ReadDurationTracker.addDuration(currentContext, duration)
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val pages by viewModel.pages.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val chapterName by viewModel.chapterName.collectAsState()
    val readingMode by viewModel.readingMode.collectAsState()


    val prefs = com.example.wammy.AppContainer.readerPreferences
    val readerTheme by prefs.readerTheme.state.collectAsState()
    val bgColor = when(readerTheme) {
        com.example.wammy.data.prefs.ReaderTheme.BLACK -> Color.Black
        com.example.wammy.data.prefs.ReaderTheme.GRAY -> Color(0xFF22222E)
        com.example.wammy.data.prefs.ReaderTheme.WHITE -> Color.White
        com.example.wammy.data.prefs.ReaderTheme.AUTOMATIC -> androidx.compose.material3.MaterialTheme.colorScheme.background
    }
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

    
    // Tsundoku feature: Tie Slider to scroll states
    var currentProgress by remember { mutableStateOf(0f) }
    var requestScrollTo by remember { mutableStateOf<Int?>(null) }
    
    val initialPage by viewModel.initialPage.collectAsState()
    var hasScrolledToInitial by remember(chapterName) { mutableStateOf(false) }

    LaunchedEffect(pages.size) {
        if (pages.isNotEmpty() && !hasScrolledToInitial) {
            hasScrolledToInitial = true
            requestScrollTo = initialPage
        }
    }

    val coroutineScope = rememberCoroutineScope()

    var showOverlay by remember { mutableStateOf(false) }
    DisposableEffect(showOverlay) {
        val window = (context as? Activity)?.window
        if (window != null) {
            val insetsController = WindowCompat.getInsetsController(window, window.decorView)
            insetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            if (showOverlay) {
                insetsController.show(WindowInsetsCompat.Type.systemBars())
            } else {
                insetsController.hide(WindowInsetsCompat.Type.systemBars())
            }
        }
        onDispose {
            val window = (context as? Activity)?.window
            if (window != null) {
                WindowCompat.getInsetsController(window, window.decorView).show(WindowInsetsCompat.Type.systemBars())
            }
        }
    }


    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bgColor)
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                color = androidx.compose.material3.MaterialTheme.colorScheme.primary,
                modifier = Modifier.align(Alignment.Center)
            )
        } else if (pages.isEmpty()) {
            Text(
                text = "No pages found.",
                color = androidx.compose.material3.MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.align(Alignment.Center)
            )
        } else {
            // Reader Content
            when (readingMode) {
com.example.wammy.data.prefs.ReadingMode.WEBTOON -> {
                    val listState = rememberLazyListState()
                    
                    // Tsundoku feature: Hide overlay on scroll
                    val isScrolling = listState.isScrollInProgress
                    LaunchedEffect(isScrolling) {
                        if (isScrolling && showOverlay) {
                            showOverlay = false
                        }
                    }
                    
                    LaunchedEffect(listState.firstVisibleItemIndex) {
                        if (pages.isNotEmpty()) {
                            currentProgress = listState.firstVisibleItemIndex.toFloat() / maxOf(1, pages.size - 1).toFloat()
                            viewModel.updateProgress(listState.firstVisibleItemIndex)
                        }
                    }
                    LaunchedEffect(requestScrollTo) {
                        requestScrollTo?.let {
                            listState.scrollToItem(it)
                            requestScrollTo = null
                        }
                    }
                    val zoomableState = me.saket.telephoto.zoomable.rememberZoomableState()
                                    val sidePadding by com.example.wammy.AppContainer.readerPreferences.webtoonSidePadding.state.collectAsState()
                    
                    LazyColumn(
                        state = listState, horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .fillMaxSize()
                            .zoomable(zoomableState)
                            .pointerInput(Unit) {
                                detectTapGestures(
                                    onTap = { offset ->
                                        val layoutIndex = com.example.wammy.AppContainer.readerPreferences.navigationModeWebtoon.get()
                                        val invertMode = com.example.wammy.AppContainer.readerPreferences.webtoonNavInverted.get()
                                        val layout = NavigationLayouts.getLayout(layoutIndex, true, true)
                                        

                                        
                                        val action = NavigationLayouts.resolveTap(offset, pxWidth, pxHeight, layout, invertMode)
                                        
                                        val finalAction = when (action) {
                                            TapAction.LEFT -> TapAction.PREV // Webtoon scrolls down, so "left" usually implies back/up
                                            TapAction.RIGHT -> TapAction.NEXT
                                            else -> action
                                        }
                                        
                                        if (finalAction == TapAction.MENU) {
                                            showOverlay = !showOverlay
                                        } else if (finalAction == TapAction.PREV) {
                                            coroutineScope.launch { listState.animateScrollToItem(maxOf(0, listState.firstVisibleItemIndex - 1)) }
                                        } else if (finalAction == TapAction.NEXT) {
                                            coroutineScope.launch { listState.animateScrollToItem(minOf(pages.size, listState.firstVisibleItemIndex + 1)) }
                                        }
                                    }
                                )
                            }
                    ) {
                        items(pages) { imageUrl ->
                            if (imageUrl.state == com.example.wammy.ui.PageState.READY) {
                                val imageRequest = remember(imageUrl) {
                                    ImageRequest.Builder(context)
                                        .data(imageUrl.url)
                                        .apply {
                                            imageUrl.headers?.forEach { (key, value) ->
                                                addHeader(key, value)
                                            }
                                        }
                                        .crossfade(true)
                                        .build()
                                }
                                val cFilter = remember(grayscale, invertedColors) { getCustomColorFilter(grayscale, invertedColors) }
                                
                                Box(modifier = Modifier.fillMaxWidth(if (sidePadding > 0) 1f - (sidePadding * 2f / 100f) else 1f)) {
                                    AsyncImage(
                                        model = imageRequest,
                                        contentDescription = null,
                                        contentScale = scaleType,
                                        alignment = Alignment.TopCenter,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .graphicsLayer { this.colorFilter = cFilter }
                                    )
                                    if (isColorFilter) {
                                        Box(modifier = Modifier.matchParentSize().background(Color(colorR, colorG, colorB, colorA)))
                                    }
                                    if (isCustomBrightness && brightnessValue < 0) {
                                        val alpha = (kotlin.math.abs(brightnessValue) / 100f).coerceIn(0f, 1f)
                                        Box(modifier = Modifier.matchParentSize().background(Color.Black.copy(alpha = alpha)))
                                    }
                                }
                            } else {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(min = 400.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (imageUrl.state == com.example.wammy.ui.PageState.ERROR) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable { viewModel.retryPage(imageUrl.index) }.padding(16.dp)) {
                                            Text("Error Loading Page", color = Color.Red, style = MaterialTheme.typography.titleMedium)
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Text("Tap to Retry", color = Color.White, style = MaterialTheme.typography.bodySmall, modifier = Modifier.background(Color.DarkGray, shape = androidx.compose.foundation.shape.RoundedCornerShape(4.dp)).padding(8.dp))
                                        }
                                    } else {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            CircularProgressIndicator(color = androidx.compose.material3.MaterialTheme.colorScheme.primary)
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Text("Loading...", color = Color.Gray)
                                        }
                                    }
                                }
                            }
                        }
                        item {
                            ChapterTransition(viewModel, isWebtoon = true)
                        }
                    }
                }
                com.example.wammy.data.prefs.ReadingMode.LTR, com.example.wammy.data.prefs.ReadingMode.RTL -> {
                    val isRtl = readingMode == com.example.wammy.data.prefs.ReadingMode.RTL
                    val layoutDirection = if (isRtl) LayoutDirection.Rtl else LayoutDirection.Ltr
                    
CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
                        val pagerState = rememberPagerState(pageCount = { pages.size + 1 }) // +1 for the next chapter screen
                        
                        // Tsundoku feature: Hide overlay on page turn
                        val isScrolling = pagerState.isScrollInProgress
                        LaunchedEffect(isScrolling) {
                            if (isScrolling && showOverlay) {
                                showOverlay = false
                            }
                        }
                        
                        LaunchedEffect(pagerState.currentPage) {
                            if (pages.isNotEmpty() && pagerState.currentPage < pages.size) {
                                currentProgress = pagerState.currentPage.toFloat() / maxOf(1, pages.size - 1).toFloat()
                                viewModel.updateProgress(pagerState.currentPage)
                            }
                        }
                        LaunchedEffect(requestScrollTo) {
                            requestScrollTo?.let {
                                pagerState.scrollToPage(it)
                                requestScrollTo = null
                            }
                        }
                        
                        HorizontalPager(
                            state = pagerState,
                            modifier = Modifier.fillMaxSize()
                        ) { page ->
                            if (page < pages.size) {
                                ZoomableImage(
                                    model = pages[page],
                                    contentScale = scaleType,
                                    modifier = Modifier.fillMaxSize(),
                                    grayscale = grayscale,
                                    invertedColors = invertedColors,
                                    isCustomBrightness = isCustomBrightness,
                                    brightnessValue = brightnessValue,
                                    isColorFilter = isColorFilter,
                                    colorR = colorR,
                                    colorG = colorG,
                                    colorB = colorB,
                                    colorA = colorA,
                                    onRetry = { viewModel.retryPage(pages[page].index) },
                                    onTap = { offset ->
                                        val layoutIndex = com.example.wammy.AppContainer.readerPreferences.navigationModePager.get()
                                        val invertMode = com.example.wammy.AppContainer.readerPreferences.pagerNavInverted.get()
                                        val isVert = readingMode == com.example.wammy.data.prefs.ReadingMode.VERTICAL
                                        val layout = NavigationLayouts.getLayout(layoutIndex, false, isVert)
                                        

                                        
                                        val action = NavigationLayouts.resolveTap(offset, pxWidth, pxHeight, layout, invertMode)
                                        
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
                                    }
                                )
                            } else {
                                // Reached the end
                                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    ChapterTransition(viewModel)
                                }
                            }
                        }
                    }
                }
                else -> {
                    // Fallback for VERTICAL, etc
                    val isRtl = true
                    val layoutDirection = LayoutDirection.Rtl
                    
CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
                        val pagerState = rememberPagerState(pageCount = { pages.size + 1 })
                        
                        LaunchedEffect(pagerState.isScrollInProgress) {
                            if (pagerState.isScrollInProgress && showOverlay) showOverlay = false
                        }
                        
                        LaunchedEffect(pagerState.currentPage) {
                            if (pages.isNotEmpty() && pagerState.currentPage < pages.size) {
                                currentProgress = pagerState.currentPage.toFloat() / maxOf(1, pages.size - 1).toFloat()
                                viewModel.updateProgress(pagerState.currentPage)
                            }
                        }
                        
                        LaunchedEffect(initialPage) {
                            if (initialPage > 0 && initialPage < pages.size) {
                                pagerState.scrollToPage(initialPage)
                            }
                        }

                        HorizontalPager(
                            state = pagerState,
                            modifier = Modifier.fillMaxSize()
                        ) { page ->
                            if (page < pages.size) {
                                ZoomableImage(
                                    model = pages[page],
                                    contentScale = scaleType,
                                    modifier = Modifier.fillMaxSize(),
                                    grayscale = grayscale,
                                    invertedColors = invertedColors,
                                    isCustomBrightness = isCustomBrightness,
                                    brightnessValue = brightnessValue,
                                    isColorFilter = isColorFilter,
                                    colorR = colorR,
                                    colorG = colorG,
                                    colorB = colorB,
                                    colorA = colorA,
                                    onRetry = { viewModel.retryPage(pages[page].index) },
                                    onTap = { offset ->
                                        val layoutIndex = com.example.wammy.AppContainer.readerPreferences.navigationModePager.get()
                                        val invertMode = com.example.wammy.AppContainer.readerPreferences.pagerNavInverted.get()
                                        val isVert = readingMode == com.example.wammy.data.prefs.ReadingMode.VERTICAL
                                        val layout = NavigationLayouts.getLayout(layoutIndex, false, isVert)
                                        

                                        
                                        val action = NavigationLayouts.resolveTap(offset, pxWidth, pxHeight, layout, invertMode)
                                        
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
                                    }
                                )
                            } else {
                                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    ChapterTransition(viewModel)
                                }
                            }
                        }
                    }
                }
            }
        }

        // Settings Modal Bottom Sheet
        var showSettingsSheet by remember { mutableStateOf(false) }
        
        if (showSettingsSheet) {
            ReaderSettingsSheet(
                viewModel = viewModel,
                onDismissRequest = { showSettingsSheet = false }
            )
        }

        // Always-on Page Counter in immersive mode
        if (pages.isNotEmpty() && !showOverlay) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(bottom = 16.dp)
                    .background(Color.Black.copy(alpha = 0.5f), shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp))
                    .padding(horizontal = 12.dp, vertical = 4.dp)
            ) {
                val currentPageNum = (currentProgress * maxOf(1, pages.size - 1)).toInt().coerceIn(0, pages.size - 1) + 1
                Text(
                    text = "$currentPageNum / ${pages.size}",
                    color = Color.White,
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }

        if (showOverlay) {
            // Top Bar
            Surface(
                color = Color.Black.copy(alpha = 0.85f),
                modifier = Modifier
                    
                    .align(Alignment.TopCenter)
            ) {
                Row(
                    modifier = Modifier
                        
                        .statusBarsPadding()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            text = chapterName,
                            color = Color.White,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = "Tsundoku Reader",
                            color = Color.LightGray,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
            
            // Bottom Bar (Tsundoku Style)
            Surface(
                color = Color.Black.copy(alpha = 0.85f),
                modifier = Modifier
                    
                    .align(Alignment.BottomCenter)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    val currentPageNum = (currentProgress * maxOf(1, pages.size - 1)).toInt().coerceIn(0, pages.size - 1) + 1
                    Text(
                        text = "$currentPageNum / ${maxOf(1, pages.size)}",
                        color = Color.White,
                        style = MaterialTheme.typography.labelMedium
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    // Page / Chapter Scrubber (Dummy Slider for visual parity)
                    Row(
                        modifier = Modifier,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { /* Previous Chapter */ }) {
                            Icon(Icons.Default.SkipPrevious, contentDescription = "Previous", tint = Color.White)
                        }
                        Slider(
                            value = if (pages.isEmpty()) 0f else currentProgress,
                            onValueChange = { newValue ->
                                currentProgress = newValue
                                if (pages.isNotEmpty()) {
                                    requestScrollTo = (newValue * maxOf(1, pages.size - 1)).toInt().coerceIn(0, pages.size - 1)
                                }
                            },
                            steps = if (pages.size > 2) pages.size - 2 else 0,
                            modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
                            colors = SliderDefaults.colors(
                                thumbColor = Color(0xFFB388FF),
                                activeTrackColor = Color(0xFFB388FF),
                                activeTickColor = Color.White.copy(alpha = 0.5f),
                                inactiveTickColor = Color.White.copy(alpha = 0.3f)
                            )
                        )
                        IconButton(onClick = { /* Next Chapter */ }) {
                            Icon(Icons.Default.SkipNext, contentDescription = "Next", tint = Color.White)
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    // Quick Action Buttons
                    Row(
                        modifier = Modifier,
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        IconButton(onClick = { showSettingsSheet = true }) {
                            Icon(Icons.Default.Settings, contentDescription = "Settings", tint = Color.White)
                        }
                        IconButton(onClick = { /* Rotation Lock */ }) {
                            Icon(Icons.Default.ScreenRotation, contentDescription = "Rotation", tint = Color.White)
                        }
                        IconButton(onClick = { /* Crop Borders */ }) {
                            Icon(Icons.Default.Crop, contentDescription = "Crop", tint = Color.White)
                        }
                    }
                }
            }
        }
    }
}


@Composable
fun ChapterTransition(viewModel: ReaderViewModel, isWebtoon: Boolean = false) {
    val hasNext = viewModel.hasNextChapter
    
    if (!isWebtoon) {
        // Auto-trigger next chapter load when this transition screen is rendered
        LaunchedEffect(hasNext) {
            if (hasNext) {
                kotlinx.coroutines.delay(600) // Brief delay so user sees the transition
                viewModel.loadNextChapter()
            }
        }
    }

    Box(
        modifier = Modifier.fillMaxWidth().heightIn(min = 200.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (hasNext) {
                if (isWebtoon) {
                    androidx.compose.material3.OutlinedButton(onClick = { viewModel.loadNextChapter() }) {
                        Text("Load Next Chapter")
                    }
                } else {
                    CircularProgressIndicator(color = androidx.compose.material3.MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        text = "Loading next chapter...", 
                        color = androidx.compose.material3.MaterialTheme.colorScheme.onBackground,
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            } else {
                Text(
                    text = "No more chapters.", 
                    color = Color.Gray,
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }
    }
}

@Composable
fun ZoomableImage(
    model: com.example.wammy.ui.ReaderPage, 
    contentScale: ContentScale, 
    modifier: Modifier, 
    grayscale: Boolean,
    invertedColors: Boolean,
    isCustomBrightness: Boolean,
    brightnessValue: Int,
    isColorFilter: Boolean,
    colorR: Int,
    colorG: Int,
    colorB: Int,
    colorA: Int,
    onTap: (androidx.compose.ui.geometry.Offset) -> Unit,
    onRetry: () -> Unit = {}
) {
    androidx.compose.foundation.layout.BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val width = constraints.maxWidth
        
        val tapModifier = Modifier.fillMaxSize().pointerInput(Unit) {
            detectTapGestures(
                onTap = { offset ->
                    val x = offset.x
                    onTap(offset)
                }
            )
        }

        if (model.state == com.example.wammy.ui.PageState.READY) {
            val context = androidx.compose.ui.platform.LocalContext.current
            val imageRequest = remember(model) {
                ImageRequest.Builder(context)
                    .data(model.url)
                    .apply {
                        model.headers?.forEach { (key, value) ->
                            addHeader(key, value)
                        }
                    }
                    .crossfade(true)
                    .build()
            }
            
            val colorFilter = remember(grayscale, invertedColors) { com.example.wammy.ui.screens.getCustomColorFilter(grayscale, invertedColors) }
            
            me.saket.telephoto.zoomable.coil.ZoomableAsyncImage(
                model = imageRequest,
                contentDescription = "Manga Page",
                modifier = Modifier.fillMaxSize().graphicsLayer { this.colorFilter = colorFilter },
                contentScale = contentScale,
                onClick = { offset ->
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
            }
                } else {
            // Background Loading / Queued / Error State
            Box(
                modifier = tapModifier.then(Modifier.heightIn(min = 400.dp)), 
                contentAlignment = Alignment.Center
            ) {
                if (model.state == com.example.wammy.ui.PageState.ERROR) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable { onRetry() }.padding(16.dp)) {
                        Text("Error Loading Page", color = Color.Red, style = MaterialTheme.typography.titleMedium)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Tap to Retry", color = Color.White, style = MaterialTheme.typography.bodySmall, modifier = Modifier.background(Color.DarkGray, shape = androidx.compose.foundation.shape.RoundedCornerShape(4.dp)).padding(8.dp))
                    }
                } else {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = androidx.compose.material3.MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = if (model.state == com.example.wammy.ui.PageState.DOWNLOAD_IMAGE) "Downloading image..." else "Queued...", 
                            color = androidx.compose.material3.MaterialTheme.colorScheme.onBackground
                        )
                    }
                }
            }
        }
    }
}
