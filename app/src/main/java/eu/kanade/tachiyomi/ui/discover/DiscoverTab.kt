package eu.kanade.tachiyomi.ui.discover

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.navigator.currentOrThrow
import cafe.adriel.voyager.navigator.tab.LocalTabNavigator
import cafe.adriel.voyager.navigator.tab.TabOptions
import eu.kanade.presentation.components.AppBar
import eu.kanade.presentation.components.AppBarTitle
import eu.kanade.presentation.library.components.MangaCompactGridItem
import eu.kanade.presentation.util.Screen
import eu.kanade.presentation.util.Tab
import eu.kanade.tachiyomi.ui.browse.source.browse.BrowseSourceScreen
import eu.kanade.tachiyomi.ui.manga.MangaScreen
import tachiyomi.domain.manga.model.Manga
import tachiyomi.domain.manga.model.MangaCover
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.material.Scaffold
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import coil3.compose.AsyncImage
import kotlinx.coroutines.delay
import tachiyomi.presentation.core.i18n.stringResource
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Tab as ComposeTab
import kotlinx.coroutines.launch

import androidx.compose.animation.core.tween
import androidx.compose.ui.graphics.graphicsLayer
import kotlin.math.absoluteValue

import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.RepeatMode
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.ui.draw.clip
import androidx.compose.material3.Icon
import androidx.compose.material.icons.filled.Extension
import eu.kanade.tachiyomi.ui.browse.BrowseTab

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
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(5) {
            Column(modifier = Modifier.width(120.dp)) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(2f / 3f)
                        .clip(MaterialTheme.shapes.small)
                        .background(MaterialTheme.colorScheme.onSurface.copy(alpha = alpha))
                )
                Box(
                    modifier = Modifier
                        .padding(top = 4.dp)
                        .fillMaxWidth(0.8f)
                        .height(14.dp)
                        .background(MaterialTheme.colorScheme.onSurface.copy(alpha = alpha))
                )
                Box(
                    modifier = Modifier
                        .padding(top = 2.dp)
                        .fillMaxWidth(0.5f)
                        .height(14.dp)
                        .background(MaterialTheme.colorScheme.onSurface.copy(alpha = alpha))
                )
            }
        }
    }
}

@Composable
fun EmptyDiscoverScreen(
    isNovel: Boolean,
    onBrowseExtensions: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Nothing here yet!",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Download some ${if (isNovel) "novel" else "manga"} extensions to get started.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onBrowseExtensions) {
            Icon(Icons.Filled.Extension, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Get Extensions")
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
                .height(350.dp)
                .background(MaterialTheme.colorScheme.onSurface.copy(alpha = alpha))
        )
        return
    }
    
    val pagerState = rememberPagerState(pageCount = { featuredManga.size })
    
    // Auto-scroll logic
    LaunchedEffect(pagerState) {
        while (true) {
            delay(5000)
            if (featuredManga.size > 1) {
                val nextPage = (pagerState.currentPage + 1) % featuredManga.size
                pagerState.animateScrollToPage(
                    page = nextPage,
                    animationSpec = tween(1000)
                )
            }
        }
    }

    HorizontalPager(
        state = pagerState,
        modifier = Modifier.fillMaxWidth().height(350.dp)
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
        ) {
            // Background Image
            AsyncImage(
                model = tachiyomi.domain.manga.model.MangaCover(
                    mangaId = manga.id,
                    sourceId = manga.source,
                    isMangaFavorite = manga.favorite,
                    url = manga.thumbnailUrl,
                    lastModified = manga.coverLastModified
                ),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            // Gradient Overlay
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.9f)),
                            startY = 300f
                        )
                    )
            )
            
            // Content
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(16.dp)
            ) {
                Text(
                    text = manga.title,
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold, color = Color.White),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth().padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { onClick(manga) },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Start Reading")
                    }
                    OutlinedButton(
                        onClick = { onClick(manga) },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                        border = BorderStroke(1.dp, Color.White),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("+ Subscribe")
                    }
                }
            }
        }
    }
}

object DiscoverTab : Tab {

    override val options: TabOptions
        @Composable
        get() {
            val isSelected = LocalTabNavigator.current.current.key == key
            val title = "Home"
            val icon = rememberVectorPainter(Icons.Outlined.Explore)
            return TabOptions(
                index = 0u,
                title = title,
                icon = icon,
            )
        }

    override suspend fun onReselect(navigator: Navigator) {
        // Handle reselect (e.g. scroll to top)
    }

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val viewModel = viewModel<DiscoverViewModel>()
        val state by viewModel.state.collectAsState()
        val scope = androidx.compose.runtime.rememberCoroutineScope()
        val user by viewModel.authManager.currentUser.collectAsState()

        val popularCache = remember { mutableStateMapOf<Long, List<Manga>>() }

        Scaffold(
            topBar = {
                Column(modifier = Modifier.background(androidx.compose.material3.MaterialTheme.colorScheme.background)) {
                    AppBar(
                        title = "Home",
                        actions = {
                            androidx.compose.material3.IconButton(onClick = { navigator.push(eu.kanade.tachiyomi.ui.profile.ProfileScreen()) }) {
                                if (user != null) {
                                    val photoUrl = user?.photoUrl?.toString()?.replace("s96-c", "s192-c")
                                    if (photoUrl != null) {
                                        coil3.compose.AsyncImage(
                                            model = photoUrl,
                                            contentDescription = "Profile",
                                            contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                                            modifier = Modifier.size(24.dp).clip(androidx.compose.foundation.shape.CircleShape)
                                        )
                                    } else {
                                        androidx.compose.material3.Icon(
                                            imageVector = androidx.compose.material.icons.Icons.Outlined.AccountCircle,
                                            contentDescription = "Profile",
                                            tint = androidx.compose.material3.MaterialTheme.colorScheme.primary
                                        )
                                    }
                                } else {
                                    androidx.compose.material3.Icon(
                                        imageVector = androidx.compose.material.icons.Icons.Outlined.AccountCircle,
                                        contentDescription = "Login"
                                    )
                                }
                            }
                        }
                    )
                    PrimaryTabRow(
                        selectedTabIndex = if (state.isNovel) 1 else 0,
                        modifier = Modifier
                            .width(200.dp)
                            .align(Alignment.CenterHorizontally)
                            .offset(y = (-12).dp),
                        containerColor = androidx.compose.ui.graphics.Color.Transparent,
                        divider = {}
                    ) {
                        ComposeTab(
                            selected = !state.isNovel,
                            onClick = { viewModel.toggleNovel(false) },
                            text = { Text("Manga") }
                        )
                        ComposeTab(
                            selected = state.isNovel,
                            onClick = { viewModel.toggleNovel(true) },
                            text = { Text("Novels") }
                        )
                    }
                }
            }
        ) { contentPadding ->
            if (state.isLoading) {
                Box(modifier = Modifier.fillMaxSize().padding(contentPadding), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                val featuredManga = remember(popularCache.values.sumOf { it.size }, state.isNovel) {
                    val result = mutableListOf<Manga>()
                    var index = 0
                    while (result.size < 5) {
                        var added = false
                        for (source in state.sources) {
                            val list = popularCache[source.id]
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
                
                val allCachesLoaded = state.sources.all { popularCache.containsKey(it.id) }
                val hasNoMangaAtAll = allCachesLoaded && popularCache.values.all { it.isEmpty() }

                val tabNavigator = LocalTabNavigator.current

                if (state.sources.isEmpty() || hasNoMangaAtAll) {
                    EmptyDiscoverScreen(
                        isNovel = state.isNovel,
                        onBrowseExtensions = { tabNavigator.current = BrowseTab }
                    )
                } else {
                    LazyColumn(
                        contentPadding = contentPadding,
                        modifier = Modifier.fillMaxSize(),
                    ) {
                    item {
                        HeroCarousel(
                            featuredManga = featuredManga,
                            onClick = { manga -> 
                                scope.launch {
                                    val localManga = viewModel.getNetworkToLocalManga(manga)
                                    navigator.push(MangaScreen(localManga.id))
                                }
                            }
                        )
                    }

                    items(state.sources, key = { it.id }) { source ->
                        val cached = popularCache[source.id]
                        
                        if (cached == null) {
                            Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                                Text(
                                    text = source.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                                )
                                SkeletonCarousel()
                            }
                            LaunchedEffect(source.id) {
                                val result = viewModel.loadSourcePopular(source)
                                popularCache[source.id] = result
                            }
                        } else if (cached.isNotEmpty()) {
                            Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                                Text(
                                    text = source.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                                )
                                LazyRow(
                                    contentPadding = PaddingValues(horizontal = 16.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    items(cached) { manga ->
                                        Box(modifier = Modifier.width(120.dp)) {
                                            MangaCompactGridItem(
                                                isSelected = false,
                                                title = manga.title,
                                                coverData = MangaCover(
                                                    mangaId = manga.id,
                                                    sourceId = manga.source,
                                                    isMangaFavorite = false,
                                                    url = manga.thumbnailUrl,
                                                    lastModified = manga.coverLastModified
                                                ),
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
}
