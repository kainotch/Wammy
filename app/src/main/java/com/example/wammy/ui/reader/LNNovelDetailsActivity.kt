package com.example.wammy.ui.reader
import androidx.compose.material.icons.filled.*
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.border
import androidx.compose.animation.animateContentSize
import androidx.compose.ui.text.style.TextOverflow


import androidx.compose.foundation.background
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.graphics.Brush
import coil.compose.AsyncImage

import android.content.Intent
import android.os.Bundle
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.foundation.isSystemInDarkTheme
import com.example.wammy.AppContainer
import com.example.wammy.theme.ThemeMode
import com.example.wammy.theme.AppTheme

import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material.icons.filled.HourglassBottom
import androidx.compose.material.icons.filled.Refresh
import com.example.wammy.theme.WammyTheme
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.wammy.lnreader.loader.LNReaderPluginLoader
import com.example.wammy.lnreader.model.SourceNovel
import kotlinx.coroutines.launch
import java.io.File

class LNNovelDetailsActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        androidx.core.view.WindowCompat.setDecorFitsSystemWindows(window, false)
        val apkFileName = intent.getStringExtra("APK_FILE") ?: ""
        val pkgName = intent.getStringExtra("PKG_NAME") ?: ""
        val novelUrl = intent.getStringExtra("NOVEL_URL") ?: ""
        val novelTitle = intent.getStringExtra("NOVEL_TITLE") ?: "Novel Details"

        setContent {
            
            val themeMode by com.example.wammy.AppContainer.themePreferences.themeMode.collectAsState(
                initial = ThemeMode.SYSTEM
            )
            val isDarkTheme = when (themeMode) {
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
                else -> androidx.compose.foundation.isSystemInDarkTheme()
            }
            WammyTheme(darkTheme = isDarkTheme) {

                var isLoading by remember { mutableStateOf(true) }
                var novel by remember { mutableStateOf<SourceNovel?>(null) }
                var isFetchingChapter by remember { mutableStateOf(false) }
                var isFavorite by remember { mutableStateOf(false) }
                var dbManga by remember { mutableStateOf<com.example.wammy.data.local.MangaEntity?>(null) }
                val scope = rememberCoroutineScope()
                
                LaunchedEffect(novelUrl) {
                    val existing = com.example.wammy.AppContainer.database.mangaDao().getMangaByUrl(novelUrl)
                    if (existing != null) {
                        dbManga = existing
                        isFavorite = existing.favorite
                    }
                }
                val loader = remember { LNReaderPluginLoader(this@LNNovelDetailsActivity) }
                val pluginFile = remember { File(filesDir, "ir_extensions/${apkFileName}") }

                LaunchedEffect(Unit) {
                    if (novelUrl.isNotEmpty()) {
                        val plugin = com.example.wammy.lnreader.plugin.PluginRegistry.find(pkgName)
                        if (plugin != null) {
                            novel = plugin.parseNovel(novelUrl)
                        } else if (apkFileName.isNotEmpty()) {
                            novel = loader.parseNovel(pluginFile, novelUrl)
                        }

                        // Auto-insert into DB to ensure history can be saved
                        val n = novel
                        if (n != null) {
                            val existing = com.example.wammy.AppContainer.database.mangaDao().getMangaByUrl(novelUrl)
                            val mangaToSave = if (existing != null) {
                                existing.copy(
                                    titleRomaji = n.name ?: "Unknown Novel",
                                    coverImageUrl = n.cover,
                                    description = n.summary,
                                    author = n.author,
                                    status = n.status,
                                    genre = n.genres
                                )
                            } else {
                                com.example.wammy.data.local.MangaEntity(
                                    aniListId = null,
                                    titleRomaji = n.name ?: "Unknown Novel",
                                    coverImageUrl = n.cover,
                                    description = n.summary,
                                    sourceId = 9999L,
                                    sourceUrl = novelUrl,
                                    author = n.author,
                                    status = n.status,
                                    sourceName = pkgName,
                                    genre = n.genres,
                                    favorite = false,
                                    isNovel = true,
                                    novelPkgName = pkgName,
                                    novelApkFile = apkFileName
                                )
                            }
                            com.example.wammy.AppContainer.database.mangaDao().insertManga(mangaToSave)
                            dbManga = com.example.wammy.AppContainer.database.mangaDao().getMangaByUrl(novelUrl)
                        }
                    }
                    isLoading = false
                }

                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    val bgColor = androidx.compose.material3.MaterialTheme.colorScheme.background
                        Box(modifier = Modifier.fillMaxSize().background(bgColor)) {
                        // Dynamic Blurred Cover Background behind everything (including TopBar)
                        Box(modifier = Modifier.fillMaxSize()) {
                            AsyncImage(
                                model = novel?.cover ?: "",
                                contentDescription = null,
                                contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .blur(20.dp, edgeTreatment = androidx.compose.ui.draw.BlurredEdgeTreatment.Unbounded)
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

                    Scaffold(containerColor = Color.Transparent,
                        topBar = {
                            Row(
                                modifier = Modifier.fillMaxWidth().statusBarsPadding().padding(top = 8.dp, bottom = 8.dp, start = 8.dp, end = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(onClick = { finish() }) {
                                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                                }
                                Text("Detail", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
                                IconButton(onClick = { /* Refresh */ }) {
                                    Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = Color.LightGray)
                                }
                            }
                        }
                    ) { padding ->
                        Box(Modifier.fillMaxSize().padding(padding)) {
                            if (isLoading) {
                                CircularProgressIndicator(Modifier.align(Alignment.Center))
                            } else if (novel == null) {
                                Text("Failed to load novel details.", Modifier.align(Alignment.Center))
                            } else {
                                LazyColumn(contentPadding = PaddingValues(16.dp)) {
                                    item {
                                        Box(modifier = Modifier.fillMaxWidth()) {

                                            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp, horizontal = 12.dp)) {
                                                Box(modifier = Modifier.width(110.dp).height(150.dp).background(Color.DarkGray, androidx.compose.foundation.shape.RoundedCornerShape(8.dp))) {
                                                    coil.compose.AsyncImage(
                                                        model = novel?.cover ?: "",
                                                        contentDescription = "Cover",
                                                        contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                                                        modifier = Modifier.fillMaxSize().clip(androidx.compose.foundation.shape.RoundedCornerShape(8.dp))
                                                    )
                                                }
                                                Spacer(modifier = Modifier.width(16.dp))
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(text = novel?.name ?: novelTitle, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold, lineHeight = 26.sp)
                                                    Spacer(modifier = Modifier.height(8.dp))
                                                    Text(text = novel?.author ?: "Unknown Author", color = Color(0xFFB388FF), fontSize = 15.sp, fontWeight = FontWeight.Medium)
                                                    Spacer(modifier = Modifier.height(8.dp))
                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                        Icon(Icons.Default.HourglassBottom, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(14.dp))
                                                        Spacer(modifier = Modifier.width(6.dp))
                                                        Text("Updated: ${novel?.status ?: "Unknown"}", color = Color.LightGray, fontSize = 13.sp)
                                                    }
                                                }
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(12.dp))
                                        
                                        val genresList = novel?.genres?.split(",")?.map { it.trim() }?.filter { it.isNotEmpty() } ?: emptyList()
                                        if (genresList.isNotEmpty()) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .horizontalScroll(androidx.compose.foundation.rememberScrollState()),
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                genresList.forEach { genre ->
                                                    Box(
                                                        modifier = Modifier
                                                            .border(1.dp, Color(0xFF303040), androidx.compose.foundation.shape.RoundedCornerShape(16.dp))
                                                            .padding(horizontal = 12.dp, vertical = 6.dp)
                                                    ) {
                                                        Text(genre, color = Color.LightGray, fontSize = 12.sp)
                                                    }
                                                }
                                            }
                                            Spacer(modifier = Modifier.height(16.dp))
                                        }
                                        
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                                        ) {
                                            Column(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .background(Color(0xFF22222E), androidx.compose.foundation.shape.RoundedCornerShape(12.dp))
                                                    .clickable { 
                                                        scope.launch(kotlinx.coroutines.Dispatchers.IO) {
                                                            val existing = com.example.wammy.AppContainer.database.mangaDao().getMangaByUrl(novelUrl)
                                                                if (existing != null) {
                                                                    val updated = existing.copy(favorite = !isFavorite)
                                                                    com.example.wammy.AppContainer.database.mangaDao().insertManga(updated)
                                                                    dbManga = com.example.wammy.AppContainer.database.mangaDao().getMangaByUrl(novelUrl)
                                                                    isFavorite = !isFavorite
                                                                }
                                                        }
                                                    }
                                                    .padding(vertical = 12.dp),
                                                horizontalAlignment = Alignment.CenterHorizontally
                                            ) {
                                                Icon(
                                                    imageVector = if (isFavorite) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                                                    contentDescription = "Favorite",
                                                    tint = if (isFavorite) Color(0xFF1ED760) else Color.White,
                                                    modifier = Modifier.size(22.dp)
                                                )
                                                Spacer(modifier = Modifier.height(6.dp))
                                                Text(if (isFavorite) "Saved" else "Add Favorite", color = Color.LightGray, fontSize = 12.sp)
                                            }
                                            
                                            Column(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .background(Color(0xFF22222E), androidx.compose.foundation.shape.RoundedCornerShape(12.dp))
                                                    .clickable { Toast.makeText(this@LNNovelDetailsActivity, "Not supported yet", Toast.LENGTH_SHORT).show() }
                                                    .padding(vertical = 12.dp),
                                                horizontalAlignment = Alignment.CenterHorizontally
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.CloudDownload,
                                                    contentDescription = "Download",
                                                    tint = Color.White,
                                                    modifier = Modifier.size(22.dp)
                                                )
                                                Spacer(modifier = Modifier.height(6.dp))
                                                Text("Not Cached", color = Color.LightGray, fontSize = 12.sp)
                                            }
                                            
                                            Column(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .background(Color(0xFF22222E), androidx.compose.foundation.shape.RoundedCornerShape(12.dp))
                                                    .clickable { Toast.makeText(this@LNNovelDetailsActivity, "Not supported yet", Toast.LENGTH_SHORT).show() }
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
                                        
                                        Text("Introduction", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                                        Spacer(modifier = Modifier.height(12.dp))
                                        
                                        var isDescriptionExpanded by remember { mutableStateOf(false) }
                                        val descText = (novel?.summary ?: "").ifEmpty { "No description available." }
                                        
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
                                                Text(if (isDescriptionExpanded) "Collapse" else "Expand", color = Color.LightGray, fontSize = 13.sp)
                                                Icon(
                                                    imageVector = if (isDescriptionExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                                    contentDescription = null,
                                                    tint = Color.LightGray,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        }
                                        
                                        Spacer(modifier = Modifier.height(16.dp))
                                        
                                        // Contents Header
                                        Row(
                                            modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text("Contents", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                                            Spacer(modifier = Modifier.width(16.dp))
                                            Box(
                                                modifier = Modifier
                                                    .border(1.dp, Color(0xFF303040), androidx.compose.foundation.shape.RoundedCornerShape(16.dp))
                                                    .clickable { Toast.makeText(this@LNNovelDetailsActivity, "Not supported yet", Toast.LENGTH_SHORT).show() }
                                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                                            ) {
                                                Text("Hide Read", color = Color.LightGray, fontSize = 12.sp)
                                            }
                                            Spacer(modifier = Modifier.weight(1f))
                                            Row(
                                                modifier = Modifier
                                                    .background(Color(0xFF5E35B1), androidx.compose.foundation.shape.RoundedCornerShape(8.dp))
                                                    .clickable { 
                                                        val chapters = novel?.chapters ?: emptyList()
                                                        if (chapters.isNotEmpty()) {
                                                            val ch = chapters.first()
                                                            if (isFetchingChapter) return@clickable
                                                            isFetchingChapter = true
                                                            Toast.makeText(this@LNNovelDetailsActivity, "Fetching chapter...", Toast.LENGTH_SHORT).show()
                                                            scope.launch {
                                                                val html = loader.parseChapter(pluginFile, ch.path)
                                                                isFetchingChapter = false
                                                                if (html.isNotEmpty()) {
                                                                    val allChapters2 = chapters
                                                                    val currentIndex = allChapters2.indexOf(ch)
                                                                    // Store session data in singleton to avoid TransactionTooLargeException
                                                                    NovelSessionStore.chapterPaths = allChapters2.map { it.path }
                                                                    NovelSessionStore.chapterNames = allChapters2.map { it.name }
                                                                    NovelSessionStore.currentIndex = currentIndex
                                                                    NovelSessionStore.apkFile = apkFileName
                                                                    NovelSessionStore.pkgName = pkgName
                                                                    NovelSessionStore.novelUrl = novelUrl
                                                                    val tempFile = java.io.File(cacheDir, "ln_chapter_temp.html")
                                                                    tempFile.writeText(html)
                                                                    val intent = Intent(this@LNNovelDetailsActivity, LNTextReaderActivity::class.java).apply {
                                                                        putExtra("HTML_CACHE_PATH", tempFile.absolutePath)
                                                                        putExtra("CHAPTER_PATH", ch.path)
                                                                    }
                                                                    startActivity(intent)
                                                                } else {
                                                                    Toast.makeText(this@LNNovelDetailsActivity, "Failed to fetch chapter.", Toast.LENGTH_SHORT).show()
                                                                }
                                                            }
                                                        }
                                                     }
                                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(Icons.Default.MenuBook, contentDescription = "Read", tint = Color.White, modifier = Modifier.size(16.dp))
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text("Start Reading", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                                            }
                                        }
                                    }
                                    
                                    val chapters = novel?.chapters ?: emptyList()
                                    items(chapters) { ch ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable {
                                                    if (isFetchingChapter) return@clickable
                                                    isFetchingChapter = true
                                                    Toast.makeText(this@LNNovelDetailsActivity, "Fetching chapter...", Toast.LENGTH_SHORT).show()
                                                    scope.launch {
                                                        val html = loader.parseChapter(pluginFile, ch.path)
                                                        isFetchingChapter = false
                                                        if (html.isNotEmpty()) {
                                                            val allChapters = novel?.chapters ?: emptyList()
                                                            val currentIndex = allChapters.indexOf(ch)
                                                            // Store session data in singleton to avoid TransactionTooLargeException
                                                            NovelSessionStore.chapterPaths = allChapters.map { it.path }
                                                            NovelSessionStore.chapterNames = allChapters.map { it.name }
                                                            NovelSessionStore.currentIndex = currentIndex
                                                            NovelSessionStore.apkFile = apkFileName
                                                            NovelSessionStore.pkgName = pkgName
                                                            NovelSessionStore.novelUrl = novelUrl
                                                            val tempFile = java.io.File(cacheDir, "ln_chapter_temp.html")
                                                            tempFile.writeText(html)
                                                            val intent = Intent(this@LNNovelDetailsActivity, LNTextReaderActivity::class.java).apply {
                                                                putExtra("HTML_CACHE_PATH", tempFile.absolutePath)
                                                                putExtra("CHAPTER_PATH", ch.path)
                                                            }
                                                            startActivity(intent)
                                                        } else {
                                                            Toast.makeText(this@LNNovelDetailsActivity, "Failed to fetch chapter.", Toast.LENGTH_SHORT).show()
                                                        }
                                                    }
                                                }
                                                .padding(vertical = 12.dp)
                                        ) {
                                            Text(ch.name, fontSize = 16.sp)
                                        }
                                    }
                                }
                            }
                            if (isFetchingChapter) {
                                Box(
                                    modifier = Modifier.fillMaxSize().padding(16.dp),
                                    contentAlignment = Alignment.BottomCenter
                                ) {
                                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
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
