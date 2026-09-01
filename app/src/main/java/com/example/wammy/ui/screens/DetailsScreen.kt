// Created by Notch
package com.example.wammy.ui.screens


import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.border
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.HourglassBottom
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.animation.animateContentSize
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
  import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Public
import androidx.compose.ui.graphics.vector.ImageVector

import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.content.Intent

import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import coil.compose.AsyncImage
import com.example.wammy.ui.DetailsViewModel
import com.example.wammy.data.local.MangaEntity
import com.example.wammy.data.local.ChapterEntity
import com.example.wammy.data.local.FolderEntity


@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun DetailsScreen(viewModel: DetailsViewModel, onBack: () -> Unit, showDownloadedOnly: Boolean = false, onChapterClick: (com.example.wammy.data.local.ChapterEntity, Long, com.example.wammy.data.local.MangaEntity) -> Unit = { _, _, _ -> }) {
    val context = androidx.compose.ui.platform.LocalContext.current
    
    val chapters by viewModel.chapters.collectAsState()
    val coroutineScope = rememberCoroutineScope()
    val storageUri by com.example.wammy.AppContainer.storagePreferences.storageUri.collectAsState(initial = null)
    
    var pendingDownloadChapters by remember { mutableStateOf<List<com.example.wammy.data.local.ChapterEntity>?>(null) }
    
    val documentTreeLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        if (uri != null) {
            val takeFlags: Int = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            context.contentResolver.takePersistableUriPermission(uri, takeFlags)
            coroutineScope.launch {
                com.example.wammy.AppContainer.storagePreferences.setStorageUri(uri.toString())
                pendingDownloadChapters?.let {
                    viewModel.downloadAllChapters(context, it)
                    pendingDownloadChapters = null
                }
            }
        }
    }

    LaunchedEffect(chapters) {
        viewModel.checkDownloadedChapters(context)
    }
    
    val listState = androidx.compose.foundation.lazy.rememberLazyListState()
    var showTrackerSheet by remember { mutableStateOf(false) }

    val isLoadingChapters by viewModel.isLoadingChapters.collectAsState()
    val isFavorite by viewModel.isFavorite.collectAsState()
    val isDownloaded by viewModel.isDownloaded.collectAsState()
    val manga by viewModel.manga.collectAsState()
    

    val activeDownloads by com.example.wammy.util.DownloadManager.downloads.collectAsState()
    val currentMangaUrl = manga?.sourceUrl
    val currentDownload = activeDownloads.find { it.manga.sourceUrl == currentMangaUrl }
    val isCurrentlyDownloading = currentDownload != null
    val downloadingChaptersCount = if (isCurrentlyDownloading) currentDownload!!.totalChapters - currentDownload.downloadedChapters else 0
    val downloadedChapters by viewModel.downloadedChapters.collectAsState()
    

    val chapterLoadError by viewModel.chapterLoadError.collectAsState()

    val allFolders by viewModel.allFolders.collectAsState()
    val mangaFolders by viewModel.mangaFolders.collectAsState()
    var showFolderSheet by remember { mutableStateOf(false) }
    var showCreateFolderDialog by remember { mutableStateOf(false) }
    
    var createSelectedImageUri by remember { mutableStateOf<android.net.Uri?>(null) }
    val createPhotoLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia(),
        onResult = { uri -> if (uri != null) createSelectedImageUri = uri }
    )




    val safeManga = manga ?: return
    val handleReadClick: (com.example.wammy.data.local.ChapterEntity) -> Unit = { chapter ->
        val isInstalled = safeManga.sourceId == 1L || com.example.wammy.AppContainer.extensionManager.activeSources.any { it.id == safeManga.sourceId }
        if (!isInstalled) {
            android.widget.Toast.makeText(context, "Source not downloaded: ${safeManga.sourceName}", android.widget.Toast.LENGTH_SHORT).show()
        } else {
            onChapterClick(chapter, safeManga.sourceId, safeManga)
        }
    }


    val bgColor = androidx.compose.material3.MaterialTheme.colorScheme.background
                        Box(modifier = Modifier.fillMaxSize().background(bgColor)) {
        // Dynamic Blurred Cover Background behind everything (including TopBar)
        Box(modifier = Modifier.fillMaxSize()) {
            AsyncImage(
                model = safeManga.coverImageUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .blur(20.dp, edgeTreatment = BlurredEdgeTreatment.Unbounded)
            )
            // Dark overlay to ensure text is readable while keeping the poster theme
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(
                        androidx.compose.ui.graphics.Brush.verticalGradient(
                            colorStops = arrayOf(
                                0.0f to bgColor.copy(alpha = 0.2f),
                                0.2f to bgColor.copy(alpha = 0.6f),
                                0.4f to bgColor,
                                1.0f to bgColor
                            )
                        )
                    )
            )
        }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            var showDeleteDialog by remember { mutableStateOf(false) }
            Row(
                modifier = Modifier.fillMaxWidth().statusBarsPadding().padding(top = 8.dp, bottom = 8.dp, start = 8.dp, end = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = androidx.compose.material3.MaterialTheme.colorScheme.onBackground)
                }
                Text("Detail", color = androidx.compose.material3.MaterialTheme.colorScheme.onBackground, fontSize = 20.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
                
                IconButton(onClick = { viewModel.fetchMangaDetails(context, safeManga) }) {
                    Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            
            if (showDeleteDialog) {
                AlertDialog(
                    onDismissRequest = { showDeleteDialog = false },
                    containerColor = androidx.compose.material3.MaterialTheme.colorScheme.surfaceVariant,
                    title = { Text("Delete", color = androidx.compose.material3.MaterialTheme.colorScheme.onBackground, fontWeight = FontWeight.Bold) },
                    text = {
                        Column {
                            Text("What would you like to delete?", color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
                            if (isDownloaded || downloadedChapters.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("• Delete Downloads", color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("• Remove from Library", color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                        }
                    },
                    confirmButton = {
                        Column {
                            if (isDownloaded || downloadedChapters.isNotEmpty()) {
                                TextButton(onClick = {
                                    showDeleteDialog = false
                                    viewModel.deleteDownloads()
                                }) {
                                    Text("Delete Downloads", color = Color(0xFFFF6B6B))
                                }
                            }
                            TextButton(onClick = {
                                showDeleteDialog = false
                                viewModel.deleteFromLibrary { onBack() }
                            }) {
                                Text("Remove from Library", color = Color(0xFFFF6B6B))
                            }
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showDeleteDialog = false }) {
                            Text("Cancel", color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                )
            }
        }
    ) { paddingValues ->
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
        ) {
            item {
                // Header section with gradient background
                Box(modifier = Modifier.fillMaxWidth()) {

                    
                    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp, horizontal = 12.dp)) {
                    // Cover Image
                    Box(
                        modifier = Modifier
                            .width(110.dp)
                            .height(150.dp)
                            .background(Color.DarkGray, RoundedCornerShape(8.dp))
                    ) {
                        AsyncImage(
                            model = safeManga.coverImageUrl,
                            contentDescription = "Cover",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(8.dp))
                        )
                    }
                    
                    Spacer(modifier = Modifier.width(16.dp))
                    
                    // Info Column
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = safeManga.titleRomaji,
                            color = androidx.compose.material3.MaterialTheme.colorScheme.onBackground,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            lineHeight = 26.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Text(
                            text = safeManga.author ?: "Unknown Author",
                            color = Color(0xFFB388FF), // Purple author text
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.HourglassBottom, contentDescription = null, tint = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Status: ${safeManga.status ?: "Unknown"}", color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Description, contentDescription = null, tint = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("${chapters.size} Chapters • ${safeManga.sourceName}", color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        
                        val readCount = chapters.count { it.read }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("$readCount / ${chapters.size} chapters read", color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                        }
                    }
                }
                } // Close Background Box
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // Genres
                val genresList = safeManga.genre?.split(",")?.map { it.trim() }?.filter { it.isNotEmpty() } ?: emptyList()
                if (genresList.isNotEmpty()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        genresList.forEach { genre ->
                            Box(
                                modifier = Modifier
                                    .border(1.dp, Color(0xFF303040), RoundedCornerShape(16.dp))
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text(genre, color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }
                
                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Favorite Button
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .background(Color(0xFF22222E), RoundedCornerShape(12.dp))
                            .clickable { showFolderSheet = true }
                            .padding(vertical = 12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = if (isFavorite) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                            contentDescription = "Favorite",
                            tint = if (isFavorite) Color(0xFFB388FF) else Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(if (isFavorite) "Favorite" else "Add Favorite", color = Color.LightGray, fontSize = 12.sp)
                    }
                    
                    // Download Button
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .background(Color(0xFF22222E), RoundedCornerShape(12.dp))
                            .clickable { 
                                if (isCurrentlyDownloading) {
                                    com.example.wammy.util.DownloadManager.cancelAllDownloads()
                                } else {
                                    val toDownload = chapters.filter { !downloadedChapters.contains(it.sourceUrl) }
                                    if (toDownload.isNotEmpty()) {
                                        viewModel.downloadAllChapters(context, toDownload)
                                    }
                                }
                            }
                            .padding(vertical = 12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.CloudDownload,
                            contentDescription = "Download",
                            tint = if (isDownloaded) Color(0xFFB388FF) else Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(if (isDownloaded) "Cached" else if (isCurrentlyDownloading) "Cancel all" else "Not Cached", color = if (isCurrentlyDownloading) Color(0xFFFF6B6B) else Color.LightGray, fontSize = 12.sp)
                    }
                    
                    // Info / WebView Button
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .background(Color(0xFF22222E), RoundedCornerShape(12.dp))
                            .clickable {
                                val url = viewModel.getMangaUrl(safeManga)
                                if (url != null) {
                                    val intent = android.content.Intent(android.content.Intent.ACTION_VIEW)
                                    intent.data = android.net.Uri.parse(url)
                                    context.startActivity(intent)
                                }
                            }
                            .padding(vertical = 12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Info",
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("Info", color = Color.LightGray, fontSize = 12.sp)
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Introduction
                Text("Introduction", color = androidx.compose.material3.MaterialTheme.colorScheme.onBackground, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(12.dp))
                
                var isDescriptionExpanded by remember { mutableStateOf(false) }
                val descText = (safeManga.description ?: "").ifEmpty { "No description available." }
                
                Column(modifier = Modifier.fillMaxWidth().animateContentSize()) {
                    Text(
                        text = descText,
                        color = Color(0xFFAAAAAA),
                        fontSize = 14.sp,
                        lineHeight = 22.sp,
                        maxLines = if (isDescriptionExpanded) Int.MAX_VALUE else 4,
                        overflow = TextOverflow.Ellipsis
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable { isDescriptionExpanded = !isDescriptionExpanded }.padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(if (isDescriptionExpanded) "Collapse" else "Expand", color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = if (isDescriptionExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                            contentDescription = "Expand",
                            tint = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                Divider(color = Color(0xFF22222E), thickness = 1.dp)
                Spacer(modifier = Modifier.height(16.dp))
                
                // Contents Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) { Text("Contents", color = androidx.compose.material3.MaterialTheme.colorScheme.onBackground, fontSize = 18.sp, fontWeight = FontWeight.Bold); if (isLoadingChapters && chapters.isNotEmpty()) { Spacer(modifier = Modifier.width(12.dp)); androidx.compose.material3.CircularProgressIndicator(modifier = Modifier.size(16.dp), color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant, strokeWidth = 2.dp) } }
                    Spacer(modifier = Modifier.width(16.dp))
                    
                    // Hide Read Button
                    Box(
                        modifier = Modifier
                            .border(1.dp, Color(0xFF303040), RoundedCornerShape(16.dp))
                            .clickable { /* Toggle hide read */ }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text("Hide Read", color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                    }
                    
                    Spacer(modifier = Modifier.weight(1f))
                    
                    // Start Reading Button
                    val sortedForReading = chapters.sortedBy { it.chapterNumber }
                    val nextChapter = sortedForReading.firstOrNull { !it.read } ?: sortedForReading.lastOrNull()
                    val buttonText = if (nextChapter != null && nextChapter.read) "Read Again"
                                     else if (nextChapter != null && nextChapter.lastPageRead > 0) "Resume"
                                     else "Start Reading"
                                     
                    Box(
                        modifier = Modifier
                            .background(Color(0xFF5E35B1), RoundedCornerShape(12.dp))
                            .clickable {
                                if (nextChapter != null) {
                                    coroutineScope.launch {
                                        handleReadClick(nextChapter)
                                    }
                                }
                            }
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.MenuBook, contentDescription = "Read", tint = Color.White, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(buttonText, color = androidx.compose.material3.MaterialTheme.colorScheme.onBackground, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                if (isLoadingChapters && chapters.isEmpty()) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator(color = Color(0xFF5E35B1))
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Loading chapters...", color = Color.Gray)
                    }
                } else if (chapterLoadError != null && chapters.isEmpty()) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(chapterLoadError ?: "Load failed", color = Color.Red)
                        Spacer(modifier = Modifier.height(16.dp))
                        IconButton(onClick = { viewModel.retryLoadChapters() }) {
                            Icon(Icons.Default.Refresh, contentDescription = "Retry", tint = Color.White, modifier = Modifier.size(32.dp))
                        }
                    }
                }
            }

val displayChapters = if (showDownloadedOnly) {
                chapters.filter { downloadedChapters.contains(it.sourceUrl) }
            } else chapters
            
            itemsIndexed(displayChapters) { index, chapter ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { handleReadClick(chapter) }
                        .padding(vertical = 12.dp, horizontal = 16.dp), // Added horizontal padding
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        // Format chapter number like Mihon (Ch. 1, Ch. 1.5, etc.)
                        val chNum = chapter.chapterNumber
                        val chNumStr = if (chNum == chNum.toLong().toFloat()) "Ch. ${chNum.toLong()}" else "Ch. $chNum"
                        val displayName = if (chapter.name.isNotBlank() && !chapter.name.startsWith("Chapter", ignoreCase = true) && !chapter.name.startsWith("Ch.", ignoreCase = true)) {
                            "$chNumStr - ${chapter.name}"
                        } else if (chapter.name.isBlank()) {
                            chNumStr
                        } else {
                            chapter.name
                        }
                        val titleText = if (chapter.read) displayName else "• $displayName"
                        Text(
                            text = titleText,
                            color = if (chapter.read) Color.Gray else Color(0xFFB388FF),
                            style = MaterialTheme.typography.bodyLarge,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        
                        // Format date
                        val dateText = if (chapter.dateUpload > 0L) {
                            java.text.SimpleDateFormat("M/d/yy", java.util.Locale.getDefault()).format(java.util.Date(chapter.dateUpload))
                        } else {
                            "Unknown date"
                        }
                        
                        val subtitleText = if (!chapter.scanlator.isNullOrEmpty()) {
                            "$dateText • ${chapter.scanlator}"
                        } else {
                            dateText
                        }
                        
                        Text(
                            text = subtitleText,
                            color = Color.Gray,
                            fontSize = 12.sp
                        )
                    }
                    val isThisChapterDownloading = currentDownload?.queuedChapterUrls?.contains(chapter.sourceUrl) == true
                    if (downloadedChapters.contains(chapter.sourceUrl)) {
                        Icon(Icons.Default.CheckCircle, contentDescription = "Downloaded", tint = Color(0xFF1ED760))
                    } else if (isThisChapterDownloading) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp).padding(4.dp), color = Color(0xFF1ED760), strokeWidth = 2.dp)
                    } else {
                        IconButton(onClick = { viewModel.downloadChapter(context, chapter) }) {
                            Icon(Icons.Default.Download, contentDescription = "Download", tint = Color.Gray)
                        }
                    }
                }
                Divider(color = androidx.compose.material3.MaterialTheme.colorScheme.surfaceVariant)
            }
            
            item {
                Spacer(modifier = Modifier.height(100.dp)) // Space for FAB
            }
    }

        if (showFolderSheet) {
            ModalBottomSheet(
                onDismissRequest = { showFolderSheet = false },
                containerColor = androidx.compose.material3.MaterialTheme.colorScheme.surfaceVariant
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text("Save to...", color = androidx.compose.material3.MaterialTheme.colorScheme.onBackground, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().clickable { viewModel.toggleFavorite() }) {
                        Checkbox(
                            checked = isFavorite,
                            onCheckedChange = { viewModel.toggleFavorite() },
                            colors = CheckboxDefaults.colors(checkedColor = androidx.compose.material3.MaterialTheme.colorScheme.primary)
                        )
                        Text("Default Saved List", color = androidx.compose.material3.MaterialTheme.colorScheme.onBackground, fontSize = 16.sp)
                    }
                    
                    allFolders.forEach { folder ->
                        val isSelected = mangaFolders.contains(folder.id)
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().clickable { viewModel.toggleFolderForManga(folder.id) }) {
                            Checkbox(
                                checked = isSelected,
                                onCheckedChange = { viewModel.toggleFolderForManga(folder.id) },
                                colors = CheckboxDefaults.colors(checkedColor = androidx.compose.material3.MaterialTheme.colorScheme.primary)
                            )
                            Text(folder.name, color = androidx.compose.material3.MaterialTheme.colorScheme.onBackground, fontSize = 16.sp)
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Button(
                        onClick = { showCreateFolderDialog = true },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = androidx.compose.material3.MaterialTheme.colorScheme.primary, contentColor = androidx.compose.material3.MaterialTheme.colorScheme.onPrimary)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Add")
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("New BookPlace")
                    }
                    
                    Spacer(modifier = Modifier.height(32.dp))
                }
            }
        
        if (showCreateFolderDialog) {
            var folderName by remember { mutableStateOf("") }
            var isPinned by remember { mutableStateOf(false) }
            
            AlertDialog(
                onDismissRequest = { showCreateFolderDialog = false },
                containerColor = androidx.compose.material3.MaterialTheme.colorScheme.surfaceVariant,
                title = { Text("Create New BookPlace", color = androidx.compose.material3.MaterialTheme.colorScheme.onBackground) },
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
                                            androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia.ImageOnly
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
                        Text("Cancel", color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            )

    }
}


}

        if (showTrackerSheet && safeManga != null) {
            TrackerBottomSheet(
                mangaId = safeManga.id,
                title = safeManga.titleRomaji,
                viewModel = viewModel,
                onDismissRequest = { showTrackerSheet = false }
            )
        }
}
} @Composable
fun ActionButton(icon: ImageVector, label: String, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable(onClick = onClick).padding(8.dp)
    ) {
        Icon(icon, contentDescription = label, tint = androidx.compose.material3.MaterialTheme.colorScheme.onBackground, modifier = Modifier.size(24.dp))
        Spacer(modifier = Modifier.height(8.dp))
        Text(label, color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
    }
}

