package com.example.wammy.ui.reader

import android.os.Bundle
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.foundation.isSystemInDarkTheme
import com.example.wammy.AppContainer
import com.example.wammy.theme.ThemeMode
import com.example.wammy.theme.AppTheme
import com.example.wammy.theme.WammyTheme

import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.wammy.lnreader.model.LNNovel

class BrowseNovelActivity : ComponentActivity() {

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        androidx.core.view.WindowCompat.setDecorFitsSystemWindows(window, false)
        val apkFileName = intent.getStringExtra("APK_FILE") ?: ""
        val sourceName  = intent.getStringExtra("SOURCE_NAME") ?: "Novel Source"

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
                var novels by remember { mutableStateOf<List<LNNovel>>(emptyList()) }
                var errorMsg by remember { mutableStateOf<String?>(null) }
                var siteUrl by remember { mutableStateOf("") }
                val focusManager = LocalFocusManager.current
                var isSearching by remember { mutableStateOf(false) }
                var searchQuery by remember { mutableStateOf("") }
                var performSearchTrigger by remember { mutableStateOf(0) }
                var refreshTrigger by remember { mutableStateOf(0) }

                LaunchedEffect(refreshTrigger, performSearchTrigger) {
                    isLoading = true
                    errorMsg = null
                    novels = emptyList()
                    try {
                        if (apkFileName.isBlank()) {
                            errorMsg = "No extension specified"
                            return@LaunchedEffect
                        }
                        val pkg = intent.getStringExtra("PKG") ?: ""
                        val lang = intent.getStringExtra("LANG") ?: "en"
                        val sourceDir = intent.getStringExtra("SOURCE_DIR") ?: "main"
                        
                        val pluginFile = java.io.File(filesDir, "ir_extensions/${apkFileName}")
                        val plugin = com.example.wammy.lnreader.plugin.PluginRegistry.find(pkg)
                        
                        if (siteUrl.isEmpty()) {
                            if (plugin != null) {
                                siteUrl = plugin.site
                            } else {
                                val loaderForSite = com.example.wammy.lnreader.loader.LNReaderPluginLoader(this@BrowseNovelActivity)
                                siteUrl = loaderForSite.getSite(pluginFile)
                            }
                        }

                        if (plugin != null) {
                            if (searchQuery.isNotBlank()) {
                                novels = plugin.searchNovels(searchQuery, 1)
                            } else {
                                novels = plugin.popularNovels(1)
                            }
                        } else {
                            val loader = com.example.wammy.lnreader.loader.LNReaderPluginLoader(this@BrowseNovelActivity)
                            if (searchQuery.isNotBlank()) {
                                novels = loader.searchNovels(pluginFile, searchQuery, 1)
                            } else {
                                novels = loader.loadAndFetchPopular(pluginFile, 1)
                            }
                        }
                        if (novels.isEmpty()) errorMsg = "No books found for this query."
                    } catch (e: Exception) {
                        e.printStackTrace()
                        errorMsg = "Error: ${e.message}"
                    } finally {
                        isLoading = false
                    }
                }

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Scaffold(
                        containerColor = Color.Transparent,
                        topBar = {
                            if (isSearching) {
                                TopAppBar(
                                    title = {
                                        TextField(
                                            value = searchQuery,
                                            onValueChange = { searchQuery = it },
                                            modifier = Modifier.fillMaxWidth(),
                                            placeholder = { Text("Search novels...") },
                                            colors = TextFieldDefaults.colors(
                                                focusedContainerColor = Color.Transparent,
                                                unfocusedContainerColor = Color.Transparent,
                                                focusedIndicatorColor = Color.Transparent,
                                                unfocusedIndicatorColor = Color.Transparent
                                            ),
                                            singleLine = true,
                                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                                            keyboardActions = KeyboardActions(onSearch = { 
                                                performSearchTrigger++ 
                                                focusManager.clearFocus()
                                            })
                                        )
                                    },
                                    navigationIcon = {
                                        IconButton(onClick = {
                                            isSearching = false
                                            searchQuery = ""
                                            performSearchTrigger++
                                        }) {
                                            Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.onBackground)
                                        }
                                    },
                                    actions = {
                                        IconButton(onClick = {
                                            searchQuery = ""
                                        }) {
                                            Icon(Icons.Default.Close, contentDescription = "Clear", tint = MaterialTheme.colorScheme.onBackground)
                                        }
                                    },
                                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
                                )
                            } else {
                                TopAppBar(
                                    title = {
                                        Text(sourceName,
                                            color = MaterialTheme.colorScheme.onBackground,
                                            fontWeight = FontWeight.Bold)
                                    },
                                    navigationIcon = {
                                        IconButton(onClick = { finish() }) {
                                            Icon(Icons.Default.ArrowBack,
                                                contentDescription = "Back",
                                                tint = MaterialTheme.colorScheme.onBackground)
                                        }
                                    },
                                    actions = {
                                        IconButton(onClick = { isSearching = true }) {
                                            Icon(Icons.Default.Search, contentDescription = "Search", tint = MaterialTheme.colorScheme.onBackground)
                                        }
                                    },
                                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
                                )
                            }
                        }
                    ) { padding ->
                        when {
                            isLoading -> Box(
                                Modifier.fillMaxSize().padding(padding),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                                    Spacer(Modifier.height(12.dp))
                                    Text("Loading novels...", color = Color.Gray, fontSize = 14.sp)
                                }
                            }

                            errorMsg != null -> Box(
                                Modifier.fillMaxSize().padding(padding),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    errorMsg ?: "Unknown error",
                                    color = Color.Red, modifier = Modifier.padding(16.dp)
                                )
                            }
                            novels.isEmpty() -> Box(
                                Modifier.fillMaxSize().padding(padding),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "No books found.",
                                    color = Color.Gray, modifier = Modifier.padding(16.dp)
                                )
                            }

                            else -> LazyVerticalGrid(
                                columns = GridCells.Fixed(3),
                                modifier = Modifier.fillMaxSize().padding(padding),
                                contentPadding = PaddingValues(8.dp)
                            ) {
                                items(novels) { novel ->
                                    NovelItem(novel) {
                                        val intent = android.content.Intent(this@BrowseNovelActivity, LNNovelDetailsActivity::class.java).apply {
                                            putExtra("APK_FILE", apkFileName)
                                            putExtra("PKG_NAME", intent.getStringExtra("PKG"))
                                            putExtra("NOVEL_URL", novel.url)
                                            putExtra("NOVEL_TITLE", novel.title)
                                        }
                                        startActivity(intent)
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

@Composable
fun NovelItem(novel: LNNovel, onClick: () -> Unit) {
    Column(
        modifier = Modifier.padding(4.dp).clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        AsyncImage(
            model = novel.coverUrl,
            contentDescription = novel.title,
            modifier = Modifier
                .aspectRatio(0.7f)
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(Color.DarkGray),
            contentScale = ContentScale.Crop
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = novel.title,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onBackground,
            maxLines = 2,
            lineHeight = 14.sp
        )
    }
}
