package eu.kanade.tachiyomi.ui.discover

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import eu.kanade.tachiyomi.data.auth.AuthManager
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import kotlin.math.absoluteValue
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import cafe.adriel.voyager.navigator.tab.LocalTabNavigator
import coil3.compose.AsyncImage
import androidx.lifecycle.viewmodel.compose.viewModel
import eu.kanade.presentation.library.components.MangaCompactGridItem
import eu.kanade.presentation.util.Tab
import eu.kanade.tachiyomi.source.CatalogueSource
import eu.kanade.tachiyomi.ui.browse.BrowseTab
import eu.kanade.tachiyomi.ui.manga.MangaScreen
import kotlinx.coroutines.launch
import tachiyomi.domain.manga.model.Manga
import tachiyomi.domain.manga.model.MangaCover
import tachiyomi.domain.manga.model.asMangaCover
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.i18n.MR
import eu.kanade.tachiyomi.ui.webview.TrackerWebViewLoginActivity

object DiscoverTab : Tab {

    override val options: cafe.adriel.voyager.navigator.tab.TabOptions
        @Composable
        get() {
            val title = "Home"
            val icon = rememberVectorPainter(Icons.Outlined.Explore)
            return remember {
                cafe.adriel.voyager.navigator.tab.TabOptions(
                    index = 0u,
                    title = title,
                    icon = icon,
                )
            }
        }

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val viewModel = viewModel<DiscoverViewModel>()
        val state by viewModel.state.collectAsState()
        val scope = rememberCoroutineScope()
        val tabNavigator = LocalTabNavigator.current
        
        val authManager: AuthManager = Injekt.get()
        val user by authManager.currentUser.collectAsState()

        Scaffold(
            topBar = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.background)
                        .statusBarsPadding()
                        .padding(horizontal = 16.dp)
                        .padding(top = 16.dp, bottom = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (state.isNovel) "Novels" else "Manga",
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        IconButton(onClick = { navigator.push(eu.kanade.tachiyomi.ui.profile.ProfileScreen()) }) {
                            if (user?.photoUrl != null) {
                                coil3.compose.AsyncImage(
                                    model = user?.photoUrl,
                                    contentDescription = "Profile",
                                    modifier = Modifier.size(28.dp).clip(CircleShape),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Outlined.AccountCircle,
                                    contentDescription = "Profile",
                                    modifier = Modifier.size(28.dp),
                                    tint = MaterialTheme.colorScheme.onBackground
                                )
                            }
                        }
                    }

                    // Tabs removed as part of FAB transition
                    /*
                    Spacer(modifier = Modifier.height(16.dp))

                    Row(modifier = Modifier.fillMaxWidth()) {
                        CustomTab(
                            text = "Manga",
                            isSelected = !state.isNovel,
                            onClick = { viewModel.toggleNovel(false) }
                        )
                        Spacer(modifier = Modifier.width(24.dp))
                        CustomTab(
                            text = "Novels",
                            isSelected = state.isNovel,
                            onClick = { viewModel.toggleNovel(true) }
                        )
                    }
                    */
                }
            },
            floatingActionButton = {
                SwipeUpFab(onSlideUpTriggered = { viewModel.toggleNovel(!state.isNovel) })
            }
        ) { contentPadding ->
            androidx.compose.animation.Crossfade(targetState = state, animationSpec = tween(400), modifier = Modifier.padding(contentPadding).fillMaxSize(), label = "discover") { animatedState ->
                if (animatedState.isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                val recentlyRead by viewModel.recentlyRead.collectAsState()
                val featuredManga = remember(viewModel.popularCache.toMap(), animatedState.isNovel) {
                    val result = mutableListOf<Manga>()
                    var index = 0
                    while (result.size < 5) {
                        var added = false
                        for (source in animatedState.sources) {
                            val list = viewModel.popularCache[source.id]
                            if (list != null && index < list.size) {
                                if (result.none { it.title == list[index].title }) {
                                    result.add(list[index])
                                    added = true
                                }
                                if (result.size == 5) break
                            }
                        }
                        if (!added) break
                        index++
                    }
                    result
                }

                val allCachesLoaded = animatedState.sources.all { viewModel.popularCache.containsKey(it.id) }
                val hasNoMangaAtAll = allCachesLoaded && viewModel.popularCache.values.all { it.isEmpty() }

                if (animatedState.sources.isEmpty() || hasNoMangaAtAll) {
                    EmptyDiscoverScreen(
                        isNovel = animatedState.isNovel,
                        onBrowseExtensions = { tabNavigator.current = BrowseTab }
                    )
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(0.dp),
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        item {
                            Spacer(modifier = Modifier.height(8.dp))
                            HeroCarousel(
                                featuredManga = featuredManga,
                                onClick = { manga ->
                                    scope.launch {
                                        val localManga = viewModel.getNetworkToLocalManga(manga)
                                        navigator.push(MangaScreen(localManga.id))
                                    }
                                }
                            )
                            Spacer(modifier = Modifier.height(24.dp))
                        }

                        item {
                            Column(modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp)) {
                                SectionHeader(title = "Recently Read", onSeeAll = {})
                                if (recentlyRead.isEmpty()) {
                                    Box(
                                        modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "No recently read manga",
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                    }
                                } else {
                                    LazyRow(
                                        contentPadding = PaddingValues(horizontal = 16.dp),
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        items(recentlyRead) { history ->
                                            Box(modifier = Modifier.width(110.dp)) {
                                                MangaCompactGridItem(
                                                    isSelected = false,
                                                    title = history.title,
                                                    coverData = history.coverData,
                                                    coverBadgeStart = {},
                                                    coverBadgeEnd = {},
                                                    onLongClick = {},
                                                    onClick = {
                                                        scope.launch {
                                                            navigator.push(MangaScreen(history.mangaId))
                                                        }
                                                    },
                                                    onClickContinueReading = null
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        if (animatedState.sources.isNotEmpty()) {
                            val firstSource = animatedState.sources.first()
                            
                            item {
                                val cached = viewModel.popularCache[firstSource.id]
                                if (cached == null) {
                                    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                                        val titleText = if (firstSource.lang == "all" || firstSource.lang.isEmpty()) firstSource.name else "${firstSource.name} (${firstSource.lang.uppercase()})"
                                        SectionHeader(title = titleText, onSeeAll = {})
                                        SkeletonCarousel()
                                    }
                                    LaunchedEffect(firstSource.id) {
                                        val result = viewModel.loadSourcePopular(firstSource)
                                        viewModel.popularCache[firstSource.id] = result
                                    }
                                } else if (cached.isNotEmpty()) {
                                    Column(modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp)) {
                                        val titleText = if (firstSource.lang == "all" || firstSource.lang.isEmpty()) firstSource.name else "${firstSource.name} (${firstSource.lang.uppercase()})"
                                        SectionHeader(title = titleText, onSeeAll = {})
                                        LazyRow(
                                            contentPadding = PaddingValues(horizontal = 16.dp),
                                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                                        ) {
                                            items(cached) { manga ->
                                                Box(modifier = Modifier.width(110.dp)) {
                                                    MangaCompactGridItem(
                                                        isSelected = false,
                                                        title = manga.title,
                                                        coverData = manga.asMangaCover(),
                                                        coverBadgeStart = {},
                                                        coverBadgeEnd = {},
                                                        onLongClick = {},
                                                        onClick = {
                                                            scope.launch {
                                                                val localManga = viewModel.getNetworkToLocalManga(manga)
                                                                navigator.push(MangaScreen(localManga.id))
                                                            }
                                                        },
                                                        onClickContinueReading = null
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            item {
                                SectionHeader(title = "Recently Updated", onSeeAll = {})
                                
                                val latest = viewModel.latestCache[firstSource.id]

                                if (latest == null) {
                                    Box(
                                        modifier = Modifier.fillMaxWidth().height(100.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        CircularProgressIndicator()
                                    }
                                    LaunchedEffect(firstSource.id) {
                                        viewModel.latestCache[firstSource.id] = viewModel.loadSourceLatest(firstSource)
                                    }
                                } else if (latest.isEmpty()) {
                                    Box(
                                        modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "No recent updates",
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                    }
                                } else {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 16.dp)
                                            .padding(bottom = 24.dp)
                                    ) {
                                        latest.take(5).forEach { manga ->
                                            RecentlyUpdatedItem(
                                                manga = manga,
                                                onClick = {
                                                    scope.launch {
                                                        val localManga = viewModel.getNetworkToLocalManga(manga)
                                                        navigator.push(MangaScreen(localManga.id))
                                                    }
                                                }
                                            )
                                            Spacer(modifier = Modifier.height(12.dp))
                                        }
                                    }
                                }
                            }

                            items(animatedState.sources.drop(1), key = { "popular_${it.id}" }) { source ->
                                val cached = viewModel.popularCache[source.id]
                                if (cached == null) {
                                    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                                        val titleText = if (source.lang == "all" || source.lang.isEmpty()) source.name else "${source.name} (${source.lang.uppercase()})"
                                        SectionHeader(title = titleText, onSeeAll = {})
                                        SkeletonCarousel()
                                    }
                                    LaunchedEffect(source.id) {
                                        val result = viewModel.loadSourcePopular(source)
                                        viewModel.popularCache[source.id] = result
                                    }
                                } else if (cached.isNotEmpty()) {
                                    Column(modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp)) {
                                        val titleText = if (source.lang == "all" || source.lang.isEmpty()) source.name else "${source.name} (${source.lang.uppercase()})"
                                        SectionHeader(title = titleText, onSeeAll = {})
                                        LazyRow(
                                            contentPadding = PaddingValues(horizontal = 16.dp),
                                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                                        ) {
                                            items(cached) { manga ->
                                                Box(modifier = Modifier.width(110.dp)) {
                                                    MangaCompactGridItem(
                                                        isSelected = false,
                                                        title = manga.title,
                                                        coverData = manga.asMangaCover(),
                                                        coverBadgeStart = {},
                                                        coverBadgeEnd = {},
                                                        onLongClick = {},
                                                        onClick = {
                                                            scope.launch {
                                                                val localManga = viewModel.getNetworkToLocalManga(manga)
                                                                navigator.push(MangaScreen(localManga.id))
                                                            }
                                                        },
                                                        onClickContinueReading = null
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

            }    }
}

@Composable
fun CustomTab(text: String, isSelected: Boolean, onClick: () -> Unit) {
    Column(
        modifier = Modifier.clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = text,
            fontSize = 18.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
        )
        if (isSelected) {
            Spacer(modifier = Modifier.height(4.dp))
            Box(
                modifier = Modifier
                    .height(3.dp)
                    .width(40.dp)
                    .background(color = MaterialTheme.colorScheme.primary, shape = RoundedCornerShape(1.5.dp))
            )
        } else {
            Spacer(modifier = Modifier.height(7.dp))
        }
    }
}

@Composable
fun SectionHeader(title: String, onSeeAll: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
    }
}

@Composable
fun RecentlyUpdatedItem(manga: Manga, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .background(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), shape = RoundedCornerShape(12.dp))
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = manga.asMangaCover(),
            contentDescription = null,
            modifier = Modifier
                .size(64.dp)
                .clip(RoundedCornerShape(8.dp)),
            contentScale = ContentScale.Crop
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = manga.title,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Updated recently",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
            )
        }
        Box(
            modifier = Modifier
                .background(
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(8.dp)
                )
                .padding(horizontal = 10.dp, vertical = 4.dp)
        ) {
            Text(
                text = "New",
                color = MaterialTheme.colorScheme.primary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun HeroCarousel(
    featuredManga: List<Manga>,
    onClick: (Manga) -> Unit
) {
    if (featuredManga.isEmpty()) {
        val infiniteTransition = rememberInfiniteTransition()
        val alpha by infiniteTransition.animateFloat(
            initialValue = 0.1f,
            targetValue = 0.3f,
            animationSpec = infiniteRepeatable(
                animation = tween(1000),
                repeatMode = RepeatMode.Reverse
            )
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(240.dp)
                .padding(horizontal = 16.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.onSurface.copy(alpha = alpha))
        )
        return
    }

    val pagerState = androidx.compose.foundation.pager.rememberPagerState(pageCount = { featuredManga.size })
    
    // Auto-scroll logic
    LaunchedEffect(pagerState) {
        while (true) {
            kotlinx.coroutines.delay(5000)
            if (featuredManga.size > 1) {
                val nextPage = (pagerState.currentPage + 1) % featuredManga.size
                pagerState.animateScrollToPage(
                    page = nextPage,
                    animationSpec = tween(1000)
                )
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(240.dp)
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(16.dp))
    ) {
        androidx.compose.foundation.pager.HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            val pageOffset = (
                (pagerState.currentPage - page) + pagerState.currentPageOffsetFraction
            ).absoluteValue
            
            val manga = featuredManga[page]
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        alpha = 1f - pageOffset.coerceIn(0f, 1f)
                    }
                    .clickable { onClick(manga) }
            ) {
                AsyncImage(
                    model = manga.asMangaCover(),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )

                // Gradients
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.8f)),
                                startY = 100f
                            )
                        )
                )

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(Color.Black.copy(alpha = 0.9f), Color.Transparent),
                                endX = 600f
                            )
                        )
                )

                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(16.dp)
                ) {
                    Text(
                        text = manga.title,
                        color = Color.White,
                        fontSize = 22.sp,
                        lineHeight = 28.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    
                    val subtitle = manga.author ?: "Trending Now"
                    Text(
                        text = subtitle,
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 14.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
                    )

                    Button(
                        onClick = { onClick(manga) },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        shape = RoundedCornerShape(50),
                        modifier = Modifier.height(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.PlayArrow,
                            contentDescription = "Start Reading",
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Start Reading",
                            color = MaterialTheme.colorScheme.onPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
        
        // Pager Indicators
        Row(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            repeat(featuredManga.size) { iteration ->
                val color = if (pagerState.currentPage == iteration) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.5f)
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(androidx.compose.foundation.shape.CircleShape)
                        .background(color)
                )
            }
        }
    }
}

@Composable
fun SkeletonCarousel() {
    val infiniteTransition = rememberInfiniteTransition()
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.1f,
        targetValue = 0.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000),
            repeatMode = RepeatMode.Reverse
        )
    )
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(5) {
            Box(
                modifier = Modifier
                    .width(110.dp)
                    .height(160.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.onSurface.copy(alpha = alpha))
            )
        }
    }
}

@Composable
fun EmptyDiscoverScreen(isNovel: Boolean, onBrowseExtensions: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("No sources found. Please install an extension.")
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onBrowseExtensions) {
            Text("Browse Extensions")
        }
    }
}

