package eu.kanade.tachiyomi.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import coil3.compose.AsyncImage
import eu.kanade.tachiyomi.data.sync.CloudUser
import eu.kanade.tachiyomi.data.sync.FavoriteManga
import eu.kanade.tachiyomi.data.sync.FirestoreUserRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import tachiyomi.domain.source.service.SourceManager
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class PublicProfileScreen(
    private val uid: String,
    private val user: CloudUser
) : Screen {

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val scope = rememberCoroutineScope()
        val repo = remember { FirestoreUserRepository() }
        val sourceManager: SourceManager = remember { Injekt.get() }

        val highResPhotoUrl = if (user.avatarUrl.isNotEmpty()) user.avatarUrl else null

        // Load favorites
        var topManga by remember { mutableStateOf<List<FavoriteManga>>(emptyList()) }
        var topNovels by remember { mutableStateOf<List<FavoriteManga>>(emptyList()) }

        LaunchedEffect(uid) {
            topManga = repo.getTopManga(uid)
            topNovels = repo.getTopNovels(uid)
        }

        Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
            AsyncImage(
                model = highResPhotoUrl,
                contentDescription = "Profile Background",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            0.0f to Color.Transparent,
                            0.15f to Color.Black.copy(alpha = 0.6f),
                            0.35f to Color.Black,
                            1.0f to Color.Black
                        )
                    )
            ) {}

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 48.dp, start = 16.dp, end = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(
                    onClick = { navigator.pop() },
                    modifier = Modifier.background(Color.Black.copy(alpha = 0.5f), CircleShape)
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                }
            }
            
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 100.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.Start
            ) {
                Spacer(modifier = Modifier.height(32.dp))
                Column(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                    horizontalAlignment = Alignment.Start
                ) {
                    if (highResPhotoUrl != null) {
                        AsyncImage(
                            model = highResPhotoUrl,
                            contentDescription = "Profile Picture",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.size(80.dp).clip(CircleShape)
                        )
                    } else {
                        Surface(
                            modifier = Modifier.size(80.dp),
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    user.displayName.take(1).uppercase(),
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    style = MaterialTheme.typography.headlineLarge
                                )
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column {
                            Text(
                                text = user.displayName,
                                style = MaterialTheme.typography.titleLarge,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                            if (user.username.isNotEmpty()) {
                                Text(
                                    text = "@${user.username}",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = Color.White.copy(alpha = 0.7f)
                                )
                            }
                        }
                    }
                }
                
                if (user.bio.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        text = user.bio,
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.White,
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp)
                    )
                }
                
                Spacer(modifier = Modifier.height(32.dp))

                // ── Top 5 Favorites Section ──
                var selectedFavTab by remember { mutableIntStateOf(0) }
                val favTabs = listOf("Manga", "Novels")
                val currentFavList = if (selectedFavTab == 0) topManga else topNovels

                Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp)) {
                    Text(
                        text = "Top 5 Favorites",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        favTabs.forEachIndexed { index, title ->
                            FilterChip(
                                selected = selectedFavTab == index,
                                onClick = { selectedFavTab = index },
                                label = { Text(title) },
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    if (currentFavList.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp)
                                .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(16.dp)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = "No favorites set yet.",
                                color = Color.White.copy(alpha = 0.5f),
                                textAlign = TextAlign.Center,
                            )
                        }
                    } else {
                        var selectedFav by remember { mutableStateOf<FavoriteManga?>(null) }

                        androidx.compose.foundation.lazy.LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            items(items = currentFavList, key = { it.title }) { fav ->
                                val index = currentFavList.indexOf(fav)
                                Box(
                                    modifier = Modifier
                                        .width(110.dp)
                                        .height(160.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .clickable { selectedFav = fav },
                                ) {
                                    AsyncImage(
                                        model = tachiyomi.domain.manga.model.MangaCover(mangaId = fav.mangaUrl.hashCode().toLong(), sourceId = fav.sourceId, isMangaFavorite = false, url = fav.thumbnailUrl, lastModified = 0L),
                                        contentDescription = fav.title,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize(),
                                    )
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .align(Alignment.BottomCenter)
                                            .background(
                                                brush = Brush.verticalGradient(
                                                    colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f)),
                                                ),
                                            )
                                            .padding(6.dp),
                                    ) {
                                        Text(
                                            text = fav.title,
                                            color = Color.White,
                                            style = MaterialTheme.typography.labelSmall,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                    }
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.TopStart)
                                            .padding(4.dp)
                                            .size(22.dp)
                                            .background(MaterialTheme.colorScheme.primary, CircleShape),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Text(
                                            text = "${index + 1}",
                                            color = MaterialTheme.colorScheme.onPrimary,
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                        )
                                    }
                                }
                            }
                        }

                        // Info Dialog
                        if (selectedFav != null) {
                            val fav = selectedFav!!
                            val hasExtension = sourceManager.get(fav.sourceId) != null

                            androidx.compose.ui.window.Dialog(onDismissRequest = { selectedFav = null }) {
                                Surface(
                                    shape = RoundedCornerShape(16.dp),
                                    color = Color.Black,
                                    modifier = Modifier.fillMaxWidth().wrapContentHeight()
                                ) {
                                    Box {
                                        // Background Image
                                        AsyncImage(
                                            model = tachiyomi.domain.manga.model.MangaCover(mangaId = fav.mangaUrl.hashCode().toLong(), sourceId = fav.sourceId, isMangaFavorite = false, url = fav.thumbnailUrl, lastModified = 0L),
                                            contentDescription = null,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxWidth().height(250.dp)
                                        )
                                        // Gradient Overlay
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(250.dp)
                                                .background(
                                                    Brush.verticalGradient(
                                                        0.0f to Color.Transparent,
                                                        0.5f to Color.Black.copy(alpha = 0.5f),
                                                        1.0f to Color.Black
                                                    )
                                                )
                                        )
                                        
                                        // Content
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(24.dp)
                                        ) {
                                            Spacer(modifier = Modifier.height(100.dp)) // push content down a bit to show the background
                                            
                                            Text(
                                                text = fav.title,
                                                style = MaterialTheme.typography.titleLarge,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White
                                            )
                                            Spacer(modifier = Modifier.height(8.dp))
                                            if (fav.author.isNotEmpty()) {
                                                Text("by ${fav.author}", style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.8f))
                                            }
                                            Text("Source: ${fav.sourceName}", style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.6f))
                                            if (fav.genres.isNotEmpty()) {
                                                Spacer(modifier = Modifier.height(8.dp))
                                                Text(
                                                    fav.genres.take(3).joinToString(" • "),
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.primary,
                                                )
                                            }
                                            if (fav.description.isNotEmpty()) {
                                                Spacer(modifier = Modifier.height(16.dp))
                                                Text(
                                                    fav.description.take(200) + if (fav.description.length > 200) "..." else "",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = Color.White.copy(alpha = 0.8f)
                                                )
                                            }
                                            Spacer(modifier = Modifier.height(24.dp))
                                            
                                            // Buttons
                                            if (hasExtension) {
                                                Button(
                                                    onClick = {
                                                        selectedFav = null
                                                        // Navigate to MangaScreen by first finding the local manga
                                                        scope.launch {
                                                            val mangaRepo: tachiyomi.domain.manga.repository.MangaRepository = Injekt.get()
                                                            val localManga = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                                                                mangaRepo.getMangaByUrlAndSourceId(fav.mangaUrl, fav.sourceId)
                                                            }
                                                            if (localManga != null) {
                                                                navigator.push(eu.kanade.tachiyomi.ui.manga.MangaScreen(localManga.id, fromSource = true))
                                                            } else {
                                                                // Manga not in local DB => browse to it from source
                                                                navigator.push(eu.kanade.tachiyomi.ui.browse.source.browse.BrowseSourceScreen(fav.sourceId, fav.mangaUrl))
                                                            }
                                                        }
                                                    },
                                                    modifier = Modifier.fillMaxWidth()
                                                ) {
                                                    Text("📖 Read")
                                                }
                                            } else {
                                                Text(
                                                    "Extension '${fav.sourceName}' is not installed.",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.error,
                                                )
                                                Spacer(modifier = Modifier.height(8.dp))
                                                OutlinedButton(
                                                    onClick = {
                                                        selectedFav = null
                                                        navigator.push(eu.kanade.tachiyomi.ui.browse.BrowseTab)
                                                    },
                                                    modifier = Modifier.fillMaxWidth()
                                                ) {
                                                    Text("Get Extension")
                                                }
                                            }
                                            Spacer(modifier = Modifier.height(8.dp))
                                            TextButton(
                                                onClick = { selectedFav = null },
                                                modifier = Modifier.align(Alignment.End)
                                            ) {
                                                Text("Close", color = Color.White)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(48.dp))
            }
        }
    }
}
