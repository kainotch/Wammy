// Created by Notch
package com.example.wammy.ui.screens
import androidx.compose.foundation.horizontalScroll

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material3.*
import androidx.activity.compose.BackHandler
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.runtime.*
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Add
  import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Delete
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.wammy.R
import com.example.wammy.ui.LibraryViewModel
import com.example.wammy.data.local.MangaEntity

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun LibraryScreen(
    viewModel: LibraryViewModel = viewModel(),
    homeViewModel: com.example.wammy.ui.HomeViewModel,
    onMangaClick: (String, Boolean) -> Unit,
    onFolderClick: (Long) -> Unit = {},
    initialFilter: String = "Entries",
    listState: LazyListState = rememberLazyListState(),
    gridState: LazyGridState = rememberLazyGridState()
) {
    var isSearchMode by remember { mutableStateOf(false) }
    val currentQuery by viewModel.currentQuery.collectAsState()
    val allLibraryItems by viewModel.allLibraryItems.collectAsState()
    val allManga by viewModel.allManga.collectAsState()
    
    val isLightNovelMode by homeViewModel.libraryNovelMode.collectAsState()
    val baseLibraryManga by viewModel.libraryManga.collectAsState()
    val baseLibraryNovels by viewModel.libraryNovels.collectAsState()
    val libraryManga = if (isSearchMode) allLibraryItems else baseLibraryManga
    


    val folders by viewModel.folders.collectAsState()
    val folderCounts by viewModel.folderCounts.collectAsState()
    var selectedFilter by remember { mutableStateOf(initialFilter) }

    BackHandler(enabled = isSearchMode || selectedFilter != "Entries") {
        if (isSearchMode) {
            isSearchMode = false
            viewModel.setSearchQuery("")
        } else {
            selectedFilter = "Entries"
        }
    }
    val filters = listOf("Entries", "Favorites", "Completed", "Downloaded")


    var showCreateFolderDialog by remember { mutableStateOf(false) }
    var folderToEdit by remember { mutableStateOf<com.example.wammy.data.local.FolderEntity?>(null) }
    var folderContextMenu by remember { mutableStateOf<com.example.wammy.data.local.FolderEntity?>(null) }
    var mangaContextMenu by remember { mutableStateOf<com.example.wammy.data.local.MangaEntity?>(null) }
    var showDeleteMangaConfirm by remember { mutableStateOf(false) }
    var showDeleteFolderConfirm by remember { mutableStateOf(false) }
    
    var createSelectedImageUri by remember { mutableStateOf<android.net.Uri?>(null) }
    val createPhotoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri -> if (uri != null) createSelectedImageUri = uri }
    )
    
    var editSelectedImageUri by remember { mutableStateOf<android.net.Uri?>(null) }
    val editPhotoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri -> if (uri != null) editSelectedImageUri = uri }
    )
    
    // Update edit image URI whenever a new folder is selected for editing
    LaunchedEffect(folderToEdit) {
        if (folderToEdit != null) {
            editSelectedImageUri = folderToEdit?.coverImageUri?.let { android.net.Uri.parse(it) }
        }
    }

    
    val context = androidx.compose.ui.platform.LocalContext.current




    val continueReading by viewModel.continueReading.collectAsState()
    val stats by viewModel.stats.collectAsState()

    val mangaHistory: List<com.example.wammy.data.local.HistoryEntity> = continueReading.filter { history ->
        val m = allManga.find { it.sourceUrl == history.mangaSourceUrl }
        m?.isNovel != true
    }
    val novelHistory: List<com.example.wammy.data.local.HistoryEntity> = continueReading.filter { history ->
        val m = allManga.find { it.sourceUrl == history.mangaSourceUrl }
        m?.isNovel == true
    }

    val filteredManga = when (selectedFilter) {
        "Entries" -> libraryManga.filter { !it.favorite }
        "Favorites" -> libraryManga.filter { it.favorite }
        "Completed" -> libraryManga.filter { it.readCompleted }
        "Downloaded" -> libraryManga.filter { it.downloaded }
        "Continue Reading" -> mangaHistory.mapNotNull { h -> allManga.find { it.sourceUrl == h.mangaSourceUrl } }
        "Novels" -> novelHistory.mapNotNull { h -> allManga.find { it.sourceUrl == h.mangaSourceUrl } }
        else -> libraryManga
    }

    

    Scaffold(
        containerColor = androidx.compose.ui.graphics.Color.Transparent
    ) { padding ->
        if (isSearchMode) {
            Column(modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { 
                        isSearchMode = false 
                        viewModel.setSearchQuery("") 
                    }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = androidx.compose.material3.MaterialTheme.colorScheme.onBackground)
                    }
                    OutlinedTextField(
                        value = currentQuery,
                        onValueChange = { viewModel.setSearchQuery(it) },
                        modifier = Modifier.fillMaxWidth().padding(start = 8.dp),
                        placeholder = { Text("Search library...", color = Color.Gray) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = androidx.compose.material3.MaterialTheme.colorScheme.surfaceVariant,
                            unfocusedContainerColor = androidx.compose.material3.MaterialTheme.colorScheme.surfaceVariant,
                            focusedBorderColor = androidx.compose.material3.MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = Color.Transparent,
                            focusedTextColor = androidx.compose.material3.MaterialTheme.colorScheme.onBackground,
                            unfocusedTextColor = androidx.compose.material3.MaterialTheme.colorScheme.onBackground
                        )
                    )
                }
                LazyVerticalGrid(
                    state = gridState,
                    columns = GridCells.Fixed(3),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(filteredManga, span = { GridItemSpan(3) }) { manga ->
                        LibraryItem(
                            manga = manga,
                            onClick = { 
                                if (manga.isNovel) {
                                    val intent = android.content.Intent(context, com.example.wammy.ui.reader.LNNovelDetailsActivity::class.java).apply {
                                        putExtra("APK_FILE", manga.novelApkFile)
                                        putExtra("PKG_NAME", manga.novelPkgName)
                                        putExtra("NOVEL_URL", manga.sourceUrl)
                                        putExtra("NOVEL_TITLE", manga.titleRomaji)
                                    }
                                    context.startActivity(intent)
                                } else {
                                    onMangaClick(manga.sourceUrl, selectedFilter == "Downloaded") 
                                }
                            },
                            onLongClick = { mangaContextMenu = manga }
                        )
                    }
                }
            }
        } else {
            LazyVerticalGrid(
                state = gridState,
                columns = GridCells.Fixed(3),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp)
            ) {
                // 1. Header Area
                item(span = { GridItemSpan(3) }) {
                    if (selectedFilter == "Continue Reading" || selectedFilter == "Novels") {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(onClick = { selectedFilter = "Entries" }) {
                                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = androidx.compose.material3.MaterialTheme.colorScheme.onBackground)
                            }
                            Text(if (selectedFilter == "Novels") "Novel" else "Continue Reading", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = androidx.compose.material3.MaterialTheme.colorScheme.onBackground)
                        }
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(top = 0.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("My Library", fontWeight = FontWeight.Bold, fontSize = 28.sp, color = androidx.compose.material3.MaterialTheme.colorScheme.onBackground)
                                Text("Your stories, your journey.", color = Color.Gray, fontSize = 14.sp)
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Box(modifier = Modifier.size(40.dp).clip(CircleShape).border(1.dp, Color.DarkGray, CircleShape).clickable { isSearchMode = true }, contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.Search, contentDescription = "Search", tint = Color.LightGray, modifier = Modifier.size(20.dp))
                                }
                                Box(modifier = Modifier.size(40.dp).clip(CircleShape).border(1.dp, Color.DarkGray, CircleShape).clickable { 
                                    val request = androidx.work.OneTimeWorkRequestBuilder<com.example.wammy.work.LibraryUpdateWorker>().build()
                                    androidx.work.WorkManager.getInstance(context).enqueue(request)
                                    android.widget.Toast.makeText(context, "Updating library in background...", android.widget.Toast.LENGTH_SHORT).show()
                                }, contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = Color.LightGray, modifier = Modifier.size(20.dp))
                                }
                                Box(modifier = Modifier.size(40.dp).clip(CircleShape).border(1.dp, Color.DarkGray, CircleShape).clickable { showCreateFolderDialog = true }, contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.Add, contentDescription = "Add", tint = Color.LightGray, modifier = Modifier.size(20.dp))
                                }
                            }
                        }
                    }
                }

                // 3. Stats Container
                item(span = { GridItemSpan(3) }) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(androidx.compose.material3.MaterialTheme.colorScheme.surfaceVariant)
                            .padding(vertical = 16.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val onSurface = androidx.compose.material3.MaterialTheme.colorScheme.onSurface
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable { selectedFilter = "Entries" }) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.List, contentDescription = null, tint = if (selectedFilter == "Entries") Color(0xFF2E65F3) else Color.Gray, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("${stats.entries}", color = if (selectedFilter == "Entries") Color(0xFF2E65F3) else onSurface, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                            }
                            Text("Entries", color = Color.Gray, fontSize = 11.sp)
                        }
                        Box(modifier = Modifier.width(1.dp).height(30.dp).background(Color.DarkGray))
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable { selectedFilter = "Favorites" }) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Star, contentDescription = null, tint = if (selectedFilter == "Favorites") Color(0xFFFFB703) else Color.Gray, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("${stats.favorites}", color = if (selectedFilter == "Favorites") Color(0xFFFFB703) else onSurface, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            }
                            Text("Favorites", color = Color.Gray, fontSize = 11.sp)
                        }
                        Box(modifier = Modifier.width(1.dp).height(30.dp).background(Color.DarkGray))
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable { selectedFilter = "Completed" }) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = if (selectedFilter == "Completed") Color(0xFF4CAF50) else Color.Gray, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("${stats.completed}", color = if (selectedFilter == "Completed") Color(0xFF4CAF50) else onSurface, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            }
                            Text("Completed", color = Color.Gray, fontSize = 11.sp)
                        }
                        Box(modifier = Modifier.width(1.dp).height(30.dp).background(Color.DarkGray))
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable { selectedFilter = "Downloaded" }) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Download, contentDescription = null, tint = if (selectedFilter == "Downloaded") Color(0xFF4CAF50) else Color.Gray, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("${stats.downloaded}", color = if (selectedFilter == "Downloaded") Color(0xFF4CAF50) else Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            }
                            Text("Downloaded", color = Color.Gray, fontSize = 11.sp)
                        }
                    }
                }

                // 4. Continue Reading
                if (selectedFilter == "Entries") {
                    item(span = { GridItemSpan(3) }) {
                        Row(modifier = Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
                            Text("Continue Reading", color = androidx.compose.material3.MaterialTheme.colorScheme.onBackground, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                            Text("View all ->", color = Color(0xFF2E65F3), fontSize = 13.sp, fontWeight = FontWeight.Medium, modifier = Modifier.clickable { selectedFilter = "Continue Reading" })
                        }
                    }
                    item(span = { GridItemSpan(3) }) {
                        if (mangaHistory.isEmpty()) {
                            Text("No manga reading history yet.", color = Color.Gray, fontSize = 14.sp, modifier = Modifier.padding(vertical = 16.dp))
                        } else {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            items(mangaHistory.take(10)) { history ->
                                Box(
                                    modifier = Modifier
                                        .width(120.dp)
                                        .aspectRatio(2f / 3f)
                                        .clip(RoundedCornerShape(10.dp))
                                        .clickable { onMangaClick(history.mangaSourceUrl, false) }
                                ) {
                                    AsyncImage(
                                        model = history.mangaCoverUrl,
                                        contentDescription = null,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(androidx.compose.ui.graphics.Brush.verticalGradient(colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.9f)), startY = 150f))
                                    )
                                    Column(
                                        modifier = Modifier.align(Alignment.BottomStart).padding(8.dp)
                                    ) {
                                        Text(history.mangaTitle, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                        Spacer(modifier = Modifier.height(4.dp))
                                        val progress = if (history.totalPages > 0) (history.lastPageRead.toFloat() / history.totalPages) else 0f
                                        val pctStr = "${(progress * 100).toInt()}%"
                                        Text(history.chapterName, color = Color.LightGray, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            LinearProgressIndicator(
                                                progress = { progress },
                                                modifier = Modifier.weight(1f).height(4.dp).clip(RoundedCornerShape(50)),
                                                color = Color(0xFF2E65F3),
                                                trackColor = Color.DarkGray
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(pctStr, color = Color.Gray, fontSize = 9.sp)
                                        }
                                    }
                                }
                            }
                        }
                        }
                    }
                }

                // 5. Novel Section
                if (selectedFilter == "Entries") {
                    item(span = { GridItemSpan(3) }) {
                        Row(modifier = Modifier.fillMaxWidth().padding(top = 0.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                            Row(verticalAlignment = Alignment.Top) {
                                Icon(Icons.Default.MenuBook, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(20.dp).padding(top = 2.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text("Novel", color = androidx.compose.material3.MaterialTheme.colorScheme.onBackground, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                                    Text("Explore epic stories in prose.", color = Color.Gray, fontSize = 12.sp)
                                }
                            }
                            Text("View all ->", color = Color(0xFF2E65F3), fontSize = 13.sp, fontWeight = FontWeight.Medium, modifier = Modifier.padding(top = 4.dp).clickable { selectedFilter = "Novels" })
                        }
                    }
                    item(span = { GridItemSpan(3) }) {
                        if (novelHistory.isEmpty()) {
                            Text("No novel reading history yet.", color = Color.Gray, fontSize = 14.sp, modifier = Modifier.padding(vertical = 16.dp))
                        } else {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            items(novelHistory.take(10)) { history ->
                                val m = allManga.find { it.sourceUrl == history.mangaSourceUrl }
                                val progress = if (history.totalPages > 0) history.lastPageRead.toFloat() / history.totalPages else 0f
                                Box(
                                    modifier = Modifier
                                        .width(120.dp)
                                        .aspectRatio(2f / 3f)
                                        .clip(RoundedCornerShape(10.dp))
                                        .clickable { 
                                            val intent = android.content.Intent(context, com.example.wammy.ui.reader.LNNovelDetailsActivity::class.java).apply {
                                                putExtra("APK_FILE", m?.novelApkFile)
                                                putExtra("PKG_NAME", m?.novelPkgName)
                                                putExtra("NOVEL_URL", history.mangaSourceUrl)
                                                putExtra("NOVEL_TITLE", history.mangaTitle)
                                            }
                                            context.startActivity(intent)
                                        }
                                ) {
                                    AsyncImage(
                                        model = history.mangaCoverUrl,
                                        contentDescription = null,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                    Box(
                                        modifier = Modifier.fillMaxSize().background(androidx.compose.ui.graphics.Brush.verticalGradient(colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.9f)), startY = 150f))
                                    )
                                    Column(
                                        modifier = Modifier.align(Alignment.BottomStart).padding(8.dp)
                                    ) {
                                        Text(history.mangaTitle, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(history.chapterName, color = Color.LightGray, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                                            Box(modifier = Modifier.weight(1f).height(4.dp).clip(RoundedCornerShape(2.dp)).background(Color.DarkGray)) {
                                                Box(modifier = Modifier.fillMaxWidth(progress.coerceIn(0f, 1f)).height(4.dp).background(Color(0xFF2E65F3)))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        }
                    }
                }
                
                // 6. BookPlaces
                if (selectedFilter == "Entries" && folders.isNotEmpty()) {
                    item(span = { GridItemSpan(3) }) {
                        Text("BookPlaces", color = androidx.compose.material3.MaterialTheme.colorScheme.onBackground, fontWeight = FontWeight.Bold, fontSize = 18.sp, modifier = Modifier.padding(top = 16.dp, bottom = 8.dp))
                    }
                    items(folders, span = { GridItemSpan(3) }) { folder ->
                        val count = folderCounts[folder.id] ?: 0
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(90.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color(0xFF22222E))
                                .combinedClickable(onClick = { onFolderClick(folder.id) }, onLongClick = { folderContextMenu = folder })
                        ) {
                            // Blurred Background
                            if (folder.coverImageUri != null) {
                                AsyncImage(
                                    model = folder.coverImageUri,
                                    contentDescription = null,
                                    modifier = Modifier.fillMaxSize().blur(radius = 16.dp),
                                    contentScale = ContentScale.Crop
                                )
                            }
                            // Gradient Overlay to ensure text readability and match aesthetic
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(androidx.compose.ui.graphics.Brush.horizontalGradient(
                                        colors = listOf(Color.Black.copy(alpha = 0.7f), Color.Transparent, Color.Black.copy(alpha = 0.7f))
                                    ))
                            )
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color.Black.copy(alpha = 0.3f))
                            )
                            
                            Row(
                                modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Thumbnail
                                Box(
                                    modifier = Modifier
                                        .size(70.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Color(0xFF303040))
                                ) {
                                    if (folder.coverImageUri != null) {
                                        AsyncImage(
                                            model = folder.coverImageUri,
                                            contentDescription = "Folder Cover",
                                            modifier = Modifier.fillMaxSize(),
                                            contentScale = ContentScale.Crop
                                        )
                                    } else {
                                        Icon(Icons.Default.Folder, contentDescription = "Folder", tint = Color.Gray, modifier = Modifier.align(Alignment.Center).size(32.dp))
                                    }
                                }
                                
                                Spacer(modifier = Modifier.width(16.dp))
                                
                                // Text details
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(folder.name, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        if (folder.isPinned) {
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Icon(Icons.Default.PushPin, contentDescription = "Pinned", tint = androidx.compose.material3.MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    val itemText = if (count == 1) "1 item" else "$count items"
                                    Text(itemText, color = Color(0xFF90CAF9), fontSize = 14.sp)
                                }
                                
                                // Menu Icon
                                IconButton(onClick = { folderContextMenu = folder }) {
                                    Icon(Icons.Default.MoreVert, contentDescription = "More", tint = Color.White)
                                }
                            }
                        }
                    }
                }

                // 7. Grid of items (based on selected filter)
                if (filteredManga.isNotEmpty()) {
                    if (selectedFilter != "Continue Reading" && selectedFilter != "Novels" && selectedFilter != "Entries") {
                        item(span = { GridItemSpan(3) }) {
                            Text(selectedFilter, color = androidx.compose.material3.MaterialTheme.colorScheme.onBackground, fontWeight = FontWeight.Bold, fontSize = 18.sp, modifier = Modifier.padding(top = 16.dp, bottom = 8.dp))
                        }
                    }
                    items(filteredManga, span = { GridItemSpan(3) }) { manga ->
                        LibraryItem(
                            manga = manga,
                            onClick = { 
                                if (manga.isNovel) {
                                    val intent = android.content.Intent(context, com.example.wammy.ui.reader.LNNovelDetailsActivity::class.java).apply {
                                        putExtra("APK_FILE", manga.novelApkFile)
                                        putExtra("PKG_NAME", manga.novelPkgName)
                                        putExtra("NOVEL_URL", manga.sourceUrl)
                                        putExtra("NOVEL_TITLE", manga.titleRomaji)
                                    }
                                    context.startActivity(intent)
                                } else {
                                    onMangaClick(manga.sourceUrl, selectedFilter == "Downloaded") 
                                }
                            },
                            onLongClick = { mangaContextMenu = manga }
                        )
                    }
                }
                
                
                                if (selectedFilter == "Continue Reading" || selectedFilter == "Novels") {
                    items(if (selectedFilter == "Novels") novelHistory else mangaHistory, span = { GridItemSpan(3) }) { history ->
                        val m = allManga.find { it.sourceUrl == history.mangaSourceUrl }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp)
                                .clickable { 
                                    if (selectedFilter == "Novels") {
                                        val intent = android.content.Intent(context, com.example.wammy.ui.reader.LNNovelDetailsActivity::class.java).apply {
                                            putExtra("APK_FILE", m?.novelApkFile)
                                            putExtra("PKG_NAME", m?.novelPkgName)
                                            putExtra("NOVEL_URL", history.mangaSourceUrl)
                                            putExtra("NOVEL_TITLE", history.mangaTitle)
                                        }
                                        context.startActivity(intent)
                                    } else {
                                        onMangaClick(history.chapterSourceUrl, false)
                                    }
                                },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val onSurfaceColor = androidx.compose.material3.MaterialTheme.colorScheme.onSurface
                            val onSurfaceVariantColor = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant
                            
                            // Poster
                            AsyncImage(
                                model = history.mangaCoverUrl,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(72.dp, 102.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color.DarkGray)
                            )
                            
                            Spacer(modifier = Modifier.width(16.dp))
                            
                            Column(modifier = Modifier.weight(1f)) {
                                Text(history.mangaTitle, color = onSurfaceColor, fontWeight = FontWeight.Bold, fontSize = 15.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(history.chapterName, color = onSurfaceVariantColor, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }
                }
                item(span = { GridItemSpan(3) }) { Spacer(modifier = Modifier.height(32.dp)) }
            }
        }
        if (mangaContextMenu != null) {
                        val m = mangaContextMenu!!
                        ModalBottomSheet(
                            onDismissRequest = { mangaContextMenu = null },
                            containerColor = androidx.compose.material3.MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                                Text(m.titleRomaji, color = androidx.compose.material3.MaterialTheme.colorScheme.onBackground, fontWeight = FontWeight.Bold, fontSize = 16.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Spacer(modifier = Modifier.height(16.dp))
                                Row(modifier = Modifier.fillMaxWidth().clickable {
                                    viewModel.toggleMangaPin(m)
                                    mangaContextMenu = null
                                }.padding(vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.PushPin, contentDescription = null, tint = androidx.compose.material3.MaterialTheme.colorScheme.onBackground, modifier = Modifier.size(22.dp))
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(if (m.favorite) "Unpin (remove from Saved)" else "Pin (add to Saved)", color = androidx.compose.material3.MaterialTheme.colorScheme.onBackground, fontSize = 15.sp)
                                }
                                Row(modifier = Modifier.fillMaxWidth().clickable {
                                    showDeleteMangaConfirm = true
                                }.padding(vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Delete, contentDescription = null, tint = Color(0xFFFF6B6B), modifier = Modifier.size(22.dp))
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text("Delete from Library", color = Color(0xFFFF6B6B), fontSize = 15.sp)
                                }
                                Spacer(modifier = Modifier.height(16.dp))
                            }
                        }
                    }
                    if (showDeleteMangaConfirm && mangaContextMenu != null) {
                        AlertDialog(
                            onDismissRequest = { showDeleteMangaConfirm = false },
                            containerColor = androidx.compose.material3.MaterialTheme.colorScheme.surfaceVariant,
                            title = { Text("Delete Book?", color = androidx.compose.material3.MaterialTheme.colorScheme.onBackground) },
                            text = { Text("This will delete all downloaded files and remove \"${mangaContextMenu!!.titleRomaji}\" from your library.", color = Color.LightGray, fontSize = 14.sp) },
                            confirmButton = {
                                TextButton(onClick = {
                                    viewModel.deleteMangaFromLibrary(mangaContextMenu!!)
                                    showDeleteMangaConfirm = false
                                    mangaContextMenu = null
                                }) { Text("Delete", color = Color(0xFFFF6B6B)) }
                            },
                            dismissButton = {
                                TextButton(onClick = { showDeleteMangaConfirm = false }) { Text("Cancel", color = Color.Gray) }
                            }
                        )
                    }

    if (folderToEdit != null) {
        val folder = folderToEdit!!
        var folderName by remember { mutableStateOf(folder.name) }
        var isPinned by remember { mutableStateOf(folder.isPinned) }
        
        AlertDialog(
            onDismissRequest = { folderToEdit = null },
            containerColor = androidx.compose.material3.MaterialTheme.colorScheme.surfaceVariant,
            title = { Text("Edit BookPlace", color = androidx.compose.material3.MaterialTheme.colorScheme.onBackground) },
            text = {
                Column {
                    OutlinedTextField(
                        value = folderName,
                        onValueChange = { folderName = it },
                        label = { Text("BookPlace Name", color = Color.Gray) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Pin to Top", color = androidx.compose.material3.MaterialTheme.colorScheme.onBackground, modifier = Modifier.weight(1f))
                        Switch(checked = isPinned, onCheckedChange = { isPinned = it })
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Text("Cover Image (Optional)", color = androidx.compose.material3.MaterialTheme.colorScheme.onBackground)
                    Spacer(modifier = Modifier.height(8.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp)
                            .background(Color(0xFF2E2E2E), RoundedCornerShape(8.dp))
                            .clickable {
                                editPhotoLauncher.launch(
                                    androidx.activity.result.PickVisualMediaRequest(
                                        ActivityResultContracts.PickVisualMedia.ImageOnly
                                    )
                                )
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        if (editSelectedImageUri != null) {
                            AsyncImage(
                                model = editSelectedImageUri,
                                contentDescription = "Selected Cover",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Text("Tap to Pick Image", color = Color.Gray)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (folderName.isNotBlank()) {
                            viewModel.updateFolder(context, folder.id, folderName, editSelectedImageUri?.toString(), isPinned)
                            folderToEdit = null
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = androidx.compose.material3.MaterialTheme.colorScheme.primary, contentColor = androidx.compose.material3.MaterialTheme.colorScheme.onPrimary)
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { folderToEdit = null }) {
                    Text("Cancel", color = Color.Gray)
                }
            }
        )
    }

    // ---- Folder long-press context bottom sheet ----
    if (folderContextMenu != null) {
        val f = folderContextMenu!!
        androidx.compose.material3.ModalBottomSheet(
            onDismissRequest = { folderContextMenu = null },
            containerColor = androidx.compose.material3.MaterialTheme.colorScheme.surfaceVariant
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                Text(f.name, color = androidx.compose.material3.MaterialTheme.colorScheme.onBackground, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Spacer(modifier = Modifier.height(16.dp))
                // Edit
                Row(
                    modifier = Modifier.fillMaxWidth().clickable {
                        folderToEdit = f
                        folderContextMenu = null
                    }.padding(vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Edit, null, tint = androidx.compose.material3.MaterialTheme.colorScheme.onBackground, modifier = Modifier.size(22.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("Edit Folder", color = androidx.compose.material3.MaterialTheme.colorScheme.onBackground, fontSize = 15.sp)
                }
                // Pin / Unpin
                Row(
                    modifier = Modifier.fillMaxWidth().clickable {
                        viewModel.toggleFolderPin(f.id)
                        folderContextMenu = null
                    }.padding(vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.PushPin, null, tint = if (f.isPinned) androidx.compose.material3.MaterialTheme.colorScheme.primary else androidx.compose.material3.MaterialTheme.colorScheme.onBackground, modifier = Modifier.size(22.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(if (f.isPinned) "Unpin Folder" else "Pin Folder", color = androidx.compose.material3.MaterialTheme.colorScheme.onBackground, fontSize = 15.sp)
                }
                // Delete
                Row(
                    modifier = Modifier.fillMaxWidth().clickable {
                        showDeleteFolderConfirm = true
                    }.padding(vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Delete, null, tint = Color(0xFFFF6B6B), modifier = Modifier.size(22.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("Delete Folder", color = Color(0xFFFF6B6B), fontSize = 15.sp)
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    if (showDeleteFolderConfirm && folderContextMenu != null) {
        AlertDialog(
            onDismissRequest = { showDeleteFolderConfirm = false },
            containerColor = androidx.compose.material3.MaterialTheme.colorScheme.surfaceVariant,
            title = { Text("Delete Folder?", color = androidx.compose.material3.MaterialTheme.colorScheme.onBackground) },
            text = { Text("Delete \"${folderContextMenu!!.name}\"? Books inside will not be deleted.", color = Color.LightGray, fontSize = 14.sp) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteFolder(folderContextMenu!!.id)
                    showDeleteFolderConfirm = false
                    folderContextMenu = null
                }) { Text("Delete", color = Color(0xFFFF6B6B)) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteFolderConfirm = false }) { Text("Cancel", color = Color.Gray) }
            }
        )
    }

    if (showCreateFolderDialog) {
        var folderName by remember { mutableStateOf("") }
        var isPinned by remember { mutableStateOf(false) }
        
        AlertDialog(
            onDismissRequest = { showCreateFolderDialog = false },
            containerColor = androidx.compose.material3.MaterialTheme.colorScheme.surfaceVariant,
            title = { Text("Create BookPlace", color = androidx.compose.material3.MaterialTheme.colorScheme.onBackground) },
            text = {
                Column {
                    OutlinedTextField(
                        value = folderName,
                        onValueChange = { folderName = it },
                        label = { Text("BookPlace Name", color = Color.Gray) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Pin to Top", color = androidx.compose.material3.MaterialTheme.colorScheme.onBackground, modifier = Modifier.weight(1f))
                        Switch(checked = isPinned, onCheckedChange = { isPinned = it })
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Text("Cover Image (Optional)", color = androidx.compose.material3.MaterialTheme.colorScheme.onBackground)
                    Spacer(modifier = Modifier.height(8.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp)
                            .background(Color(0xFF2E2E2E), RoundedCornerShape(8.dp))
                            .clickable {
                                createPhotoLauncher.launch(
                                    androidx.activity.result.PickVisualMediaRequest(
                                        ActivityResultContracts.PickVisualMedia.ImageOnly
                                    )
                                )
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        if (createSelectedImageUri != null) {
                            AsyncImage(
                                model = createSelectedImageUri,
                                contentDescription = "Selected Cover",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Icon(Icons.Default.Add, contentDescription = "Add Photo", tint = Color.Gray)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (folderName.isNotBlank()) {
                            viewModel.createFolder(context, folderName, createSelectedImageUri?.toString(), isPinned)
                            showCreateFolderDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = androidx.compose.material3.MaterialTheme.colorScheme.primary, contentColor = androidx.compose.material3.MaterialTheme.colorScheme.onPrimary)
                ) {
                    Text("Create")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateFolderDialog = false }) {
                    Text("Cancel", color = Color.Gray)
                }
            }
        )
    }

    }
}

@Composable
fun LibraryItem(manga: MangaEntity, modifier: Modifier = Modifier, onClick: () -> Unit, onLongClick: (() -> Unit)? = null) {
    val onSurfaceColor = androidx.compose.material3.MaterialTheme.colorScheme.onSurface
    val onSurfaceVariantColor = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick ?: {}),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Poster
        AsyncImage(
            model = manga.coverImageUrl,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(72.dp, 102.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(Color.DarkGray)
        )
        
        Spacer(modifier = Modifier.width(16.dp))
        
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = manga.titleRomaji,
                color = onSurfaceColor,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = manga.sourceName,
                color = onSurfaceVariantColor,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
