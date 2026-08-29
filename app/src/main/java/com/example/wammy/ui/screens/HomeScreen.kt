package com.example.wammy.ui.screens
import android.widget.Toast
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.runtime.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.wammy.R
import com.example.wammy.data.local.MangaEntity
import com.example.wammy.ui.BrowseSourceItem
import com.example.wammy.ui.HomeViewModel
import eu.kanade.tachiyomi.source.Source
// Created by Notch


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(viewModel: HomeViewModel, onMangaClick: (String) -> Unit, onSourceClick: (Long) -> Unit = {}) {
    val keyboardController = LocalSoftwareKeyboardController.current
    val latestManga by viewModel.latestManga.collectAsState()
    val bigThree by viewModel.bigThreeManga.collectAsState()
    val seinen by viewModel.seinenManga.collectAsState()
    val searchManga by viewModel.searchManga.collectAsState()
    val searchNovel by viewModel.searchNovel.collectAsState()
    val searchResults by viewModel.searchResults.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val isSearching by viewModel.isSearching.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val availableSources by viewModel.availableSources.collectAsState()
    val isLoadingMore by viewModel.isLoadingMore.collectAsState()
    val hasMoreExtensions by viewModel.hasMoreExtensions.collectAsState()

    androidx.compose.material3.pulltorefresh.PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = { viewModel.refreshHome() },
        modifier = Modifier.fillMaxSize()
    ) {
        LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item(span = { GridItemSpan(2) }) {
            Column {
                Spacer(modifier = Modifier.height(16.dp))
                if (isSearching) {
                    TextField(
                        value = searchQuery,
                        onValueChange = { viewModel.onSearchQueryChanged(it) },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Search manga or novels...", color = Color.Gray) },
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = androidx.compose.material3.MaterialTheme.colorScheme.surfaceVariant,
                            unfocusedContainerColor = androidx.compose.material3.MaterialTheme.colorScheme.surfaceVariant,
                            cursorColor = androidx.compose.material3.MaterialTheme.colorScheme.primary,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            focusedTextColor = androidx.compose.material3.MaterialTheme.colorScheme.onBackground,
                            unfocusedTextColor = androidx.compose.material3.MaterialTheme.colorScheme.onBackground
                        ),
                        shape = RoundedCornerShape(8.dp),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = { 
                            keyboardController?.hide()
                            viewModel.forceSearch()
                        }),
                        trailingIcon = {
                            IconButton(onClick = { viewModel.toggleSearch() }) {
                                Icon(Icons.Default.Close, contentDescription = "Close Search", tint = androidx.compose.material3.MaterialTheme.colorScheme.onBackground)
                            }
                        }
                    )
                    Row(modifier = Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = searchManga,
                            onClick = { viewModel.toggleSearchType(!searchManga, searchNovel) },
                            label = { Text("Manga") }
                        )
                        FilterChip(
                            selected = searchNovel,
                            onClick = { viewModel.toggleSearchType(searchManga, !searchNovel) },
                            label = { Text("Novel") }
                        )
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "WAMMY",
                            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                            color = androidx.compose.material3.MaterialTheme.colorScheme.onBackground
                        )
                        IconButton(onClick = { viewModel.toggleSearch() }) {
                            Icon(Icons.Default.Search, contentDescription = "Search", tint = androidx.compose.material3.MaterialTheme.colorScheme.onBackground)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }

        if (isSearching) {
            if (isLoading && searchQuery.length > 2) {
                item(span = { GridItemSpan(2) }) {
                    Text("SEARCHING...", color = androidx.compose.material3.MaterialTheme.colorScheme.onBackground, fontWeight = FontWeight.Bold)
                }
                items(6) {
                    Box(modifier = Modifier.fillMaxWidth().aspectRatio(0.66f).clip(RoundedCornerShape(12.dp)).shimmerEffect())
                }
            } else if (searchQuery.length > 2) {
                if (searchManga && searchNovel) {
                    item(span = { GridItemSpan(2) }) {
                        Text("MANGA", color = androidx.compose.material3.MaterialTheme.colorScheme.onBackground, fontWeight = FontWeight.Bold)
                    }
                    item(span = { GridItemSpan(2) }) {
                        androidx.compose.foundation.lazy.LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            items(searchResults.filter { !it.isNovel }) { manga ->
                                Box(modifier = Modifier.width(120.dp)) { MangaGridItem(manga, onMangaClick) }
                            }
                        }
                    }
                    item(span = { GridItemSpan(2) }) {
                        Text("NOVELS", color = androidx.compose.material3.MaterialTheme.colorScheme.onBackground, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 16.dp))
                    }
                    item(span = { GridItemSpan(2) }) {
                        androidx.compose.foundation.lazy.LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            items(searchResults.filter { it.isNovel }) { manga ->
                                Box(modifier = Modifier.width(120.dp)) { MangaGridItem(manga, onMangaClick) }
                            }
                        }
                    }
                } else {
                    item(span = { GridItemSpan(2) }) {
                        Text("SEARCH RESULTS", color = androidx.compose.material3.MaterialTheme.colorScheme.onBackground, fontWeight = FontWeight.Bold)
                    }
                    items(searchResults) { manga ->
                        MangaGridItem(manga, onMangaClick)
                    }
                }
            } else {
                item(span = { GridItemSpan(2) }) {
                    Text("TYPE TO SEARCH", color = androidx.compose.material3.MaterialTheme.colorScheme.onBackground, fontWeight = FontWeight.Bold)
                }
            }
        } else {
            item(span = { GridItemSpan(2) }) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.Transparent)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.aizen),
                        contentDescription = "Featured Image",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
            }
            
            item(span = { GridItemSpan(2) }) { Spacer(modifier = Modifier.height(8.dp)) }

            item(span = { GridItemSpan(2) }) {
                Column {
                    Text("THE BIG THREE", color = androidx.compose.material3.MaterialTheme.colorScheme.onBackground, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    if (bigThree.isEmpty()) {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            items(4) {
                                Box(
                                    modifier = Modifier
                                        .size(140.dp, 200.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .shimmerEffect()
                                )
                            }
                        }
                    } else {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            items(bigThree.size) { index ->
                                val manga = bigThree[index]
                                Card(
                                    modifier = Modifier
                                        .size(140.dp, 200.dp)
                                        .clickable { onMangaClick(manga.sourceUrl) },
                                    colors = CardDefaults.cardColors(containerColor = androidx.compose.material3.MaterialTheme.colorScheme.surfaceVariant)
                                ) {
                                    AsyncImage(
                                        model = manga.coverImageUrl,
                                        contentDescription = manga.titleRomaji,
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                }
                            }
                        }
                    }
                }
            }
            
            item(span = { GridItemSpan(2) }) { Spacer(modifier = Modifier.height(16.dp)) }

            item(span = { GridItemSpan(2) }) {
                Column {
                    Text("SEINEN", color = androidx.compose.material3.MaterialTheme.colorScheme.onBackground, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    if (seinen.isEmpty()) {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            items(4) {
                                Box(
                                    modifier = Modifier
                                        .size(140.dp, 200.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .shimmerEffect()
                                )
                            }
                        }
                    } else {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            items(seinen.size) { index ->
                                val manga = seinen[index]
                                Card(
                                    modifier = Modifier
                                        .size(140.dp, 200.dp)
                                        .clickable { onMangaClick(manga.sourceUrl) },
                                    colors = CardDefaults.cardColors(containerColor = androidx.compose.material3.MaterialTheme.colorScheme.surfaceVariant)
                                ) {
                                    AsyncImage(
                                        model = manga.coverImageUrl,
                                        contentDescription = manga.titleRomaji,
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                }
                            }
                        }
                    }
                }
            }
            
            item(span = { GridItemSpan(2) }) { Spacer(modifier = Modifier.height(16.dp)) }
            
            item(span = { GridItemSpan(2) }) {
                Text("ALL BOOKS", color = androidx.compose.material3.MaterialTheme.colorScheme.onBackground, fontWeight = FontWeight.Bold)
            }

            if (latestManga.isEmpty()) {
                items(6) {
                    androidx.compose.foundation.layout.Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(0.66f)
                            .clip(androidx.compose.foundation.shape.RoundedCornerShape(12.dp))
                            .shimmerEffect()
                    )
                }
            } else {
                itemsIndexed(latestManga) { index, manga ->
                    if (index >= latestManga.lastIndex - 3) {
                        LaunchedEffect(latestManga.size) {
                            viewModel.loadNextExtension()
                        }
                    }
                    MangaGridItem(manga, onMangaClick)
                }
                
                // Loading footer when more extensions are being fetched
                if (isLoadingMore || hasMoreExtensions) {
                    item(span = { GridItemSpan(2) }) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    strokeWidth = 2.dp,
                                    color = androidx.compose.material3.MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    "Loading more sources...",
                                    color = Color.Gray,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }
                }
            }
        }
        
        item(span = { GridItemSpan(2) }) { Spacer(modifier = Modifier.height(32.dp)) }
    }
    }
}

@Composable
fun MangaGridItem(manga: com.example.wammy.data.local.MangaEntity, onMangaClick: (String) -> Unit = {}) {
    val context = LocalContext.current
    androidx.compose.material3.Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(220.dp)
            .clickable { onMangaClick(manga.sourceUrl) },
        colors = androidx.compose.material3.CardDefaults.cardColors(containerColor = androidx.compose.material3.MaterialTheme.colorScheme.surfaceVariant),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            AsyncImage(
                model = manga.coverImageUrl,
                contentDescription = manga.titleRomaji,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            // Source Name Badge
            androidx.compose.material3.Surface(
                modifier = Modifier.padding(4.dp).align(Alignment.TopStart),
                color = Color.Black.copy(alpha = 0.7f),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(4.dp)
            ) {
                Text(
                    text = manga.sourceName,
                    color = Color.White,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .background(
                        androidx.compose.ui.graphics.Brush.verticalGradient(
                            listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f))
                        )
                    )
                    .padding(8.dp)
            ) {
                Text(
                    text = manga.titleRomaji,
                    color = Color.White,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun SourceListItem(source: BrowseSourceItem, onClick: () -> Unit) {
    androidx.compose.material3.Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = androidx.compose.material3.CardDefaults.cardColors(containerColor = androidx.compose.material3.MaterialTheme.colorScheme.surfaceVariant),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (source.iconUrl != null) {
                AsyncImage(
                    model = source.iconUrl,
                    contentDescription = source.name,
                    modifier = Modifier.size(32.dp).clip(androidx.compose.foundation.shape.RoundedCornerShape(4.dp))
                )
                Spacer(modifier = Modifier.width(12.dp))
            } else {
                Box(modifier = Modifier.size(32.dp).background(Color.DarkGray, androidx.compose.foundation.shape.RoundedCornerShape(4.dp)))
                Spacer(modifier = Modifier.width(12.dp))
            }
            Text(
                text = source.name,
                color = androidx.compose.material3.MaterialTheme.colorScheme.onBackground,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}


fun Modifier.shimmerEffect(): Modifier = composed {
    var size by remember { mutableStateOf(IntSize.Zero) }
    val transition = rememberInfiniteTransition()
    val startOffsetX by transition.animateFloat(
        initialValue = -2 * size.width.toFloat(),
        targetValue = 2 * size.width.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(1000)
        )
    )

    background(
        brush = Brush.linearGradient(
            colors = listOf(
                androidx.compose.material3.MaterialTheme.colorScheme.surfaceVariant,
                androidx.compose.material3.MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                androidx.compose.material3.MaterialTheme.colorScheme.surfaceVariant,
            ),
            start = Offset(startOffsetX, 0f),
            end = Offset(startOffsetX + size.width.toFloat(), size.height.toFloat())
        )
    )
    .onGloballyPositioned {
        size = it.size
    }
}
