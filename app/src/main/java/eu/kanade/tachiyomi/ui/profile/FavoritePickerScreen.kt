package eu.kanade.tachiyomi.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import coil3.compose.AsyncImage
import com.google.firebase.auth.FirebaseAuth
import eu.kanade.tachiyomi.data.sync.FavoriteManga
import eu.kanade.tachiyomi.data.sync.FirestoreUserRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import tachiyomi.domain.manga.model.Manga
import tachiyomi.domain.manga.model.asMangaCover
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Close
import tachiyomi.domain.manga.repository.MangaRepository
import tachiyomi.domain.source.service.SourceManager
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

/**
 * Screen that lets the user pick up to 5 manga/novels from their library
 * to set as their Top 5 Favorites on their profile.
 *
 * @param isNovel true to pick novels, false to pick manga
 * @param existing the currently saved favorites (pre-selected)
 */
class FavoritePickerScreen(
    private val isNovel: Boolean,
    private val existing: List<FavoriteManga> = emptyList(),
) : Screen {

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val scope = rememberCoroutineScope()
        val repo = remember { FirestoreUserRepository() }
        val mangaRepository: MangaRepository = remember { Injekt.get() }
        val sourceManager: SourceManager = remember { Injekt.get() }

        var libraryManga by remember { mutableStateOf<List<Manga>>(emptyList()) }
        var isLoading by remember { mutableStateOf(true) }
        var isSaving by remember { mutableStateOf(false) }
        var searchQuery by remember { mutableStateOf("") }
        var isSearchExpanded by remember { mutableStateOf(false) }

        // Track selected manga IDs in order
        val selectedIds = remember { mutableStateListOf<Long>() }

        // Pre-populate with existing selections by matching url+sourceId
        var prePopulated by remember { mutableStateOf(false) }

        // Load library manga
        LaunchedEffect(Unit) {
            withContext(Dispatchers.IO) {
                val allFavorites = mangaRepository.getFavorites()
                libraryManga = allFavorites
                    .filter { it.isNovel == isNovel }
                    .sortedBy { it.title }
            }
            isLoading = false
        }

        // Pre-populate selections after library is loaded
        LaunchedEffect(libraryManga) {
            if (!prePopulated && libraryManga.isNotEmpty() && existing.isNotEmpty()) {
                existing.sortedBy { it.order }.forEach { fav ->
                    val match = libraryManga.find { m ->
                        m.url == fav.mangaUrl && m.source == fav.sourceId
                    }
                    if (match != null && !selectedIds.contains(match.id)) {
                        selectedIds.add(match.id)
                    }
                }
                prePopulated = true
            }
        }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        if (isSearchExpanded) {
                            OutlinedTextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                placeholder = { Text("Search library...", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(end = 8.dp)
                                    .height(50.dp),
                                textStyle = LocalTextStyle.current.copy(color = MaterialTheme.colorScheme.onSurface),
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = Color.Transparent,
                                    unfocusedContainerColor = Color.Transparent,
                                    focusedIndicatorColor = Color.Transparent,
                                    unfocusedIndicatorColor = Color.Transparent
                                ),
                                trailingIcon = {
                                    if (searchQuery.isNotEmpty()) {
                                        IconButton(onClick = { searchQuery = "" }) {
                                            Icon(Icons.Default.Close, contentDescription = "Clear")
                                        }
                                    }
                                }
                            )
                        } else {
                            Text(
                                "Pick Top 5 ${if (isNovel) "Novels" else "Manga"}",
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = { 
                            if (isSearchExpanded) {
                                isSearchExpanded = false
                                searchQuery = ""
                            } else {
                                navigator.pop() 
                            }
                        }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    },
                    actions = {
                        if (!isSearchExpanded) {
                            IconButton(onClick = { isSearchExpanded = true }) {
                                Icon(Icons.Default.Search, contentDescription = "Search")
                            }
                        }
                        TextButton(
                            onClick = {
                                if (isSaving) return@TextButton
                                isSaving = true
                                scope.launch {
                                    val uid = FirebaseAuth.getInstance().currentUser?.uid
                                    if (uid != null) {
                                        val favorites = selectedIds.mapIndexedNotNull { index, id ->
                                            val manga = libraryManga.find { it.id == id } ?: return@mapIndexedNotNull null
                                            val source = sourceManager.get(manga.source)
                                            FavoriteManga(
                                                title = manga.title,
                                                author = manga.author ?: "",
                                                description = manga.description ?: "",
                                                thumbnailUrl = manga.thumbnailUrl ?: "",
                                                genres = manga.genre ?: emptyList(),
                                                sourceName = source?.name ?: "Unknown",
                                                sourceId = manga.source,
                                                mangaUrl = manga.url,
                                                status = manga.status,
                                                order = index,
                                            )
                                        }
                                        if (isNovel) {
                                            repo.saveTopNovels(uid, favorites)
                                        } else {
                                            repo.saveTopManga(uid, favorites)
                                        }
                                    }
                                    isSaving = false
                                    withContext(Dispatchers.Main) {
                                        navigator.pop()
                                    }
                                }
                            },
                            enabled = !isSaving,
                        ) {
                            if (isSaving) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp,
                                )
                            } else {
                                Text(
                                    "Save (${selectedIds.size}/5)",
                                    fontWeight = FontWeight.Bold,
                                    color = if (selectedIds.isNotEmpty()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    },
                )
            },
        ) { paddingValues ->
            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
            } else if (libraryManga.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        "No ${if (isNovel) "novels" else "manga"} in your library yet.\nAdd some first!",
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentPadding = PaddingValues(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    val filteredManga = if (searchQuery.isEmpty()) libraryManga else libraryManga.filter { it.title.contains(searchQuery, ignoreCase = true) }
                    items(filteredManga, key = { it.id }) { manga ->
                        val isSelected = selectedIds.contains(manga.id)
                        val selectionIndex = selectedIds.indexOf(manga.id)

                        MangaGridItem(
                            manga = manga,
                            isSelected = isSelected,
                            selectionNumber = if (isSelected) selectionIndex + 1 else null,
                            onClick = {
                                if (isSelected) {
                                    selectedIds.remove(manga.id)
                                } else if (selectedIds.size < 5) {
                                    selectedIds.add(manga.id)
                                }
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MangaGridItem(
    manga: Manga,
    isSelected: Boolean,
    selectionNumber: Int?,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(0.66f)
            .clip(RoundedCornerShape(8.dp))
            .then(
                if (isSelected) {
                    Modifier.border(3.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(8.dp))
                } else {
                    Modifier
                },
            )
            .clickable(onClick = onClick),
    ) {
        // Cover image
        AsyncImage(
            model = manga.asMangaCover(),
            contentDescription = manga.title,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )

        // Dark gradient at bottom for title
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .background(
                    brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.8f)),
                    ),
                )
                .padding(horizontal = 6.dp, vertical = 4.dp),
        ) {
            Text(
                text = manga.title,
                color = Color.White,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }

        // Selection badge
        if (isSelected && selectionNumber != null) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(4.dp)
                    .size(24.dp)
                    .background(MaterialTheme.colorScheme.primary, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = selectionNumber.toString(),
                    color = MaterialTheme.colorScheme.onPrimary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}
