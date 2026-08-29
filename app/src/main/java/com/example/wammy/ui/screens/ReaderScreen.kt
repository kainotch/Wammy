package com.example.wammy.ui.screens
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
import com.example.wammy.ui.ReaderViewModel
import com.example.wammy.ui.ReadingMode
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
    DisposableEffect(lifecycleOwner) {
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
    val colorFilter by viewModel.colorFilter.collectAsState()
    
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
    val context = LocalContext.current
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
            .background(androidx.compose.material3.MaterialTheme.colorScheme.background)
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
ReadingMode.WEBTOON -> {
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
                    val context = LocalContext.current
                    val sidePadding by viewModel.webtoonSidePadding.collectAsState()
                    
                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .fillMaxSize()
                            .zoomable(zoomableState)
                            .pointerInput(Unit) {
                                detectTapGestures(
                                    onTap = { offset ->
                                        val x = offset.x
                                        val width = size.width
                                        when {
                                            x < width * 0.33f -> {
                                                coroutineScope.launch { listState.animateScrollToItem(maxOf(0, listState.firstVisibleItemIndex - 1)) }
                                            }
                                            x > width * 0.66f -> {
                                                coroutineScope.launch { listState.animateScrollToItem(minOf(pages.size, listState.firstVisibleItemIndex + 1)) }
                                            }
                                            else -> showOverlay = !showOverlay
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
                                val cFilter = remember(colorFilter) { getColorFilterForMode(colorFilter) }
                                
                                AsyncImage(
                                    model = imageRequest,
                                    contentDescription = null,
                                    contentScale = ContentScale.FillWidth,
                                    alignment = Alignment.TopCenter,
                                    modifier = Modifier
                                        .fillMaxWidth(if (sidePadding > 0) 1f - (sidePadding * 2f / 100f) else 1f)
                                        .graphicsLayer { this.colorFilter = cFilter }
                                )
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
                ReadingMode.LTR, ReadingMode.RTL -> {
                    val isRtl = readingMode == ReadingMode.RTL
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
                                    contentScale = ContentScale.Fit,
                                    modifier = Modifier.fillMaxSize(),
                                    filterMode = colorFilter,
                                    onRetry = { viewModel.retryPage(pages[page].index) },
                                    onTap = { zone ->
                                        if (zone == TapZone.CENTER) {
                                            showOverlay = !showOverlay
                                        } else if (zone == TapZone.LEFT) {
                                            coroutineScope.launch { pagerState.animateScrollToPage(maxOf(0, pagerState.currentPage - 1)) }
                                        } else {
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
            }
        }

        // Settings Modal Bottom Sheet
        var showSettingsSheet by remember { mutableStateOf(false) }
        
        if (showSettingsSheet) {
            ModalBottomSheet(
                onDismissRequest = { showSettingsSheet = false },
                containerColor = Color(0xFF1E1E28), // Tsundoku dark theme style
                dragHandle = { BottomSheetDefaults.DragHandle() }
            ) {
                Column(
                    modifier = Modifier
                        
                        .padding(horizontal = 24.dp, vertical = 16.dp)
                ) {
                    Text("Reading Mode", color = Color.White, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = 16.dp))
                    Row(
                        modifier = Modifier,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Button(
                            onClick = { viewModel.setReadingMode(ReadingMode.WEBTOON); showSettingsSheet = false },
                            colors = ButtonDefaults.buttonColors(containerColor = if (readingMode == ReadingMode.WEBTOON) Color(0xFFB388FF) else Color(0xFF2E2E3A), contentColor = if (readingMode == ReadingMode.WEBTOON) Color(0xFF0D0D1A) else Color.White)
                        ) { Text("Webtoon") }
                        Button(
                            onClick = { viewModel.setReadingMode(ReadingMode.LTR); showSettingsSheet = false },
                            colors = ButtonDefaults.buttonColors(containerColor = if (readingMode == ReadingMode.LTR) Color(0xFFB388FF) else Color(0xFF2E2E3A), contentColor = if (readingMode == ReadingMode.LTR) Color(0xFF0D0D1A) else Color.White)
                        ) { Text("LTR") }
                        Button(
                            onClick = { viewModel.setReadingMode(ReadingMode.RTL); showSettingsSheet = false },
                            colors = ButtonDefaults.buttonColors(containerColor = if (readingMode == ReadingMode.RTL) Color(0xFFB388FF) else Color(0xFF2E2E3A), contentColor = if (readingMode == ReadingMode.RTL) Color(0xFF0D0D1A) else Color.White)
                        ) { Text("RTL") }
                    }
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    Text("Color Filter", color = Color.White, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = 16.dp))
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
                    }
                    Spacer(modifier = Modifier.height(32.dp))
                }
            }
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
    filterMode: com.example.wammy.ui.ColorFilterMode,
    onTap: (com.example.wammy.ui.screens.TapZone) -> Unit,
    onRetry: () -> Unit = {}
) {
    androidx.compose.foundation.layout.BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val width = constraints.maxWidth
        
        val tapModifier = Modifier.fillMaxSize().pointerInput(Unit) {
            detectTapGestures(
                onTap = { offset ->
                    val x = offset.x
                    when {
                        x < width * 0.33f -> onTap(com.example.wammy.ui.screens.TapZone.LEFT)
                        x > width * 0.66f -> onTap(com.example.wammy.ui.screens.TapZone.RIGHT)
                        else -> onTap(com.example.wammy.ui.screens.TapZone.CENTER)
                    }
                }
            )
        }

        if (model.state == com.example.wammy.ui.PageState.READY) {
            val context = LocalContext.current
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
            
            val colorFilter = remember(filterMode) { com.example.wammy.ui.screens.getColorFilterForMode(filterMode) }
            
            me.saket.telephoto.zoomable.coil.ZoomableAsyncImage(
                model = imageRequest,
                contentDescription = "Manga Page",
                modifier = Modifier.fillMaxSize().graphicsLayer { this.colorFilter = colorFilter },
                contentScale = contentScale,
                onClick = { offset ->
                    val x = offset.x
                    when {
                        x < width * 0.33f -> onTap(com.example.wammy.ui.screens.TapZone.LEFT)
                        x > width * 0.66f -> onTap(com.example.wammy.ui.screens.TapZone.RIGHT)
                        else -> onTap(com.example.wammy.ui.screens.TapZone.CENTER)
                    }
                }
            )
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
