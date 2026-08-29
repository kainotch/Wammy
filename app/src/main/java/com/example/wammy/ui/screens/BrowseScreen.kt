// Created by Notch
package com.example.wammy.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import kotlinx.coroutines.launch
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.wammy.AppContainer
import com.example.wammy.data.remote.extensions.Extension
import com.example.wammy.ui.BrowseSourceItem
import com.example.wammy.ui.ExtensionsViewModel
import com.example.wammy.ui.HomeViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BrowseScreen(
    extViewModel: ExtensionsViewModel = viewModel(),
    homeViewModel: HomeViewModel,
    initialTab: Int = 0,
    onSourceClick: (Long) -> Unit
) {
    val context = LocalContext.current
    var selectedTabIndex by remember(initialTab) { mutableIntStateOf(initialTab) }
    val tabs = listOf("Sources", "Extensions")
    
    val availableSources by homeViewModel.availableSources.collectAsState()
    val pinnedManga by homeViewModel.pinnedMangaSources.collectAsState()
    val pinnedNovel by homeViewModel.pinnedNovelSources.collectAsState()
    
    val extensions by extViewModel.extensions.collectAsState()
    val installedPackages by extViewModel.installedPackageNames.collectAsState()
    val isLoading by extViewModel.isLoading.collectAsState()
    val searchQuery by extViewModel.searchQuery.collectAsState()
    val installing by extViewModel.installing.collectAsState()

    // Refresh sources when switching to Sources tab or when installed packages change
    LaunchedEffect(selectedTabIndex, installedPackages) {
        if (selectedTabIndex == 0) {
            homeViewModel.refreshAvailableSources()
        }
    }

        var isLightNovelMode by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = { Text(if (isLightNovelMode) "Browse Novels" else "Browse Manga", color = androidx.compose.material3.MaterialTheme.colorScheme.onBackground, fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                    windowInsets = WindowInsets(0.dp)
            )
        },
        floatingActionButton = {
            androidx.compose.material3.ExtendedFloatingActionButton(
                onClick = { isLightNovelMode = !isLightNovelMode },
                containerColor = androidx.compose.material3.MaterialTheme.colorScheme.primaryContainer,
                contentColor = androidx.compose.material3.MaterialTheme.colorScheme.onPrimaryContainer,
                icon = { 
                    Icon(
                        imageVector = if (isLightNovelMode) androidx.compose.material.icons.Icons.Default.Search else androidx.compose.material.icons.Icons.Default.SwapHoriz, 
                        contentDescription = null 
                    ) 
                },
                text = { Text(if (isLightNovelMode) "Manga" else "Novels", fontWeight = FontWeight.Bold) }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Transparent)
                .padding(paddingValues)
        ) {
        TabRow(
            selectedTabIndex = selectedTabIndex,
            containerColor = Color.Transparent,
            contentColor = androidx.compose.material3.MaterialTheme.colorScheme.onBackground,
            indicator = { tabPositions ->
                TabRowDefaults.Indicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]),
                    color = androidx.compose.material3.MaterialTheme.colorScheme.primary
                )
            }
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTabIndex == index,
                    onClick = { selectedTabIndex = index },
                    text = { 
                        Text(
                            text = title, 
                            color = if (selectedTabIndex == index) androidx.compose.material3.MaterialTheme.colorScheme.primary else Color.Gray,
                            fontWeight = if (selectedTabIndex == index) FontWeight.Bold else FontWeight.Normal
                        ) 
                    }
                )
            }
        }

        if (selectedTabIndex == 0) {
            if (isLightNovelMode) {
                // ─── Light Novel Sources Tab ───
                LNSourcesTab(context = context, pinnedNovel = pinnedNovel, onTogglePin = { homeViewModel.toggleNovelPin(it) })
            } else {
                // ─── Sources Tab (Mihon style: grouped by language) ───
                SourcesTab(
                    sources = availableSources,
                    pinnedManga = pinnedManga,
                    extensions = extensions,
                    onTogglePin = { homeViewModel.toggleMangaPin(it) },
                    onUninstall = { extViewModel.uninstallExtension(it) },
                    onSourceClick = onSourceClick
                )
            }
        } else {
            if (isLightNovelMode) {
                // ─── Light Novel Extensions Tab ───
                LNExtensionsTab(context = context)
            } else {
                // ─── Extensions Tab ───
                ExtensionsTab(
                    extViewModel = extViewModel,
                    searchQuery = searchQuery,
                    isLoading = isLoading,
                    installedPackages = installedPackages,
                    installing = installing,
                    context = context
                )
            }
        }
    }
}
}

// ═══════════════════════════════════════════════════════════════
// Sources Tab — Mihon style: grouped by language, with icons
// ═══════════════════════════════════════════════════════════════
@Composable
fun SourcesTab(
    sources: List<BrowseSourceItem>,
    pinnedManga: Set<String>,
    extensions: List<com.example.wammy.data.remote.extensions.Extension>,
    onTogglePin: (Long) -> Unit,
    onUninstall: (String) -> Unit,
    onSourceClick: (Long) -> Unit
) {
    if (sources.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("No sources installed", color = Color.Gray, fontSize = 16.sp)
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Go to Extensions tab to install sources",
                    color = Color.Gray.copy(alpha = 0.6f),
                    fontSize = 13.sp
                )
            }
        }
    } else {
        // Split pinned and unpinned
        val pinnedList = sources.filter { pinnedManga.contains(it.id.toString()) }
        val unpinnedList = sources.filter { !pinnedManga.contains(it.id.toString()) }

        val grouped = unpinnedList.groupBy { it.lang }
        val sortedLangs = grouped.keys.sortedWith(compareBy { 
            if (it == "all" || it.isEmpty()) "0" else it 
        })

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 0.dp, vertical = 0.dp)
        ) {
            if (pinnedList.isNotEmpty()) {
                item {
                    Text("Pinned", color = Color.Gray, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 4.dp))
                }
                items(pinnedList) { source ->
                    SourceRow(
                        source = source,
                        isPinned = true,
                        onPin = { onTogglePin(source.id) },
                        onDelete = {
                            val pkg = extensions.find { it.name == source.name }?.packageName
                            if (pkg != null) onUninstall(pkg)
                        },
                        onClick = { onSourceClick(source.id) }
                    )
                }
            }
            
            sortedLangs.forEach { lang ->
                val langSources = grouped[lang] ?: return@forEach

                item {
                    Text(text = getLanguageDisplayName(lang), color = Color.Gray, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 4.dp))
                }

                items(langSources) { source ->
                    SourceRow(
                        source = source,
                        isPinned = false,
                        onPin = { onTogglePin(source.id) },
                        onDelete = {
                            val pkg = extensions.find { it.name == source.name }?.packageName
                            if (pkg != null) onUninstall(pkg)
                        },
                        onClick = { onSourceClick(source.id) }
                    )
                }
            }

            item { Spacer(modifier = Modifier.height(80.dp)) }
        }
    }
}

@Composable
fun SourceRow(source: BrowseSourceItem, isPinned: Boolean, onPin: () -> Unit, onDelete: () -> Unit, onClick: () -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Source icon
        if (source.iconUrl != null) {
            val iconModel: Any = if (source.iconUrl.startsWith("/")) {
                java.io.File(source.iconUrl)
            } else {
                source.iconUrl
            }
            AsyncImage(
                model = iconModel,
                contentDescription = source.name,
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(androidx.compose.material3.MaterialTheme.colorScheme.surfaceVariant)
            )
        } else {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(androidx.compose.material3.MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = source.name.firstOrNull()?.uppercase() ?: "?",
                    color = androidx.compose.material3.MaterialTheme.colorScheme.onBackground,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            }
        }
        
        Spacer(modifier = Modifier.width(14.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = source.name,
                color = androidx.compose.material3.MaterialTheme.colorScheme.onBackground,
                fontWeight = FontWeight.Medium,
                fontSize = 15.sp
            )
            if (source.lang.isNotEmpty()) {
                Text(
                    text = getLanguageDisplayName(source.lang),
                    color = Color.Gray,
                    fontSize = 12.sp
                )
            }
        }
        
        Box {
            IconButton(onClick = { expanded = true }) {
                Icon(androidx.compose.material.icons.Icons.Default.MoreVert, contentDescription = "More", tint = Color.Gray)
            }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                DropdownMenuItem(
                    text = { Text(if (isPinned) "Unpin" else "Pin") },
                    onClick = { expanded = false; onPin() }
                )
                if (source.id != 1L) { // Don't allow uninstalling built-in MangaDex
                    DropdownMenuItem(
                        text = { Text("Delete", color = Color.Red) },
                        onClick = { expanded = false; onDelete() }
                    )
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════
// Extensions Tab — Mihon style: search + installed/available sections
// ═══════════════════════════════════════════════════════════════
@Composable
fun ExtensionsTab(
    extViewModel: ExtensionsViewModel,
    searchQuery: String,
    isLoading: Boolean,
    installedPackages: Set<String>,
    installing: Set<String>,
    context: android.content.Context
) {
    Column(modifier = Modifier.fillMaxSize()) {
        // Search bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { extViewModel.onSearchQueryChanged(it) },
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            placeholder = { Text("Search extensions...", color = Color.Gray) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search", tint = Color.Gray) },
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = androidx.compose.material3.MaterialTheme.colorScheme.surfaceVariant,
                unfocusedContainerColor = androidx.compose.material3.MaterialTheme.colorScheme.surfaceVariant,
                focusedBorderColor = androidx.compose.material3.MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = Color.Transparent,
                focusedTextColor = androidx.compose.material3.MaterialTheme.colorScheme.onBackground,
                unfocusedTextColor = androidx.compose.material3.MaterialTheme.colorScheme.onBackground
            ),
            shape = RoundedCornerShape(12.dp)
        )

        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = androidx.compose.material3.MaterialTheme.colorScheme.primary)
            }
        } else {
            val installedExtensions = extViewModel.getFilteredExtensions(showInstalled = true)
            val availableExtensions = extViewModel.getFilteredExtensions(showInstalled = false)

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 0.dp)
            ) {
                // ─── Installed section ───
                if (installedExtensions.isNotEmpty()) {
                    item {
                        Text(
                            text = "Installed (${installedExtensions.size})",
                            color = Color.Gray,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(start = 16.dp, top = 8.dp, bottom = 4.dp)
                        )
                    }
                    items(installedExtensions) { extension ->
                        ExtensionRow(
                            extension = extension,
                            isInstalled = true,
                            isInstalling = installing.contains(extension.packageName),
                            onInstall = {},
                            onUninstall = {
                                extViewModel.uninstallExtension(extension.packageName)
                                Toast.makeText(context, "Removed ${extension.name}", Toast.LENGTH_SHORT).show()
                            }
                        )
                    }
                }

                // ─── Available section ───
                if (availableExtensions.isNotEmpty()) {
                    item {
                        Text(
                            text = "Available (${availableExtensions.size})",
                            color = Color.Gray,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 4.dp)
                        )
                    }
                    items(availableExtensions) { extension ->
                        ExtensionRow(
                            extension = extension,
                            isInstalled = false,
                            isInstalling = installing.contains(extension.packageName),
                            onInstall = {
                                extViewModel.installExtension(extension) { success ->
                                    if (success) {
                                        Toast.makeText(context, "✓ Installed ${extension.name}", Toast.LENGTH_SHORT).show()
                                    } else {
                                        Toast.makeText(context, "✗ Failed to install ${extension.name}", Toast.LENGTH_LONG).show()
                                    }
                                }
                            },
                            onUninstall = {}
                        )
                    }
                }

                item { Spacer(modifier = Modifier.height(80.dp)) }
            }
        }
    }
}

@Composable
fun ExtensionRow(
    extension: Extension,
    isInstalled: Boolean,
    isInstalling: Boolean,
    onInstall: () -> Unit,
    onUninstall: () -> Unit
) {
    val isNsfw = extension.contentWarning.contains("NSFW")

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Extension icon from CDN
        AsyncImage(
            model = extension.resources.iconUrl,
            contentDescription = "${extension.name} icon",
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(androidx.compose.material3.MaterialTheme.colorScheme.surfaceVariant)
        )
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(extension.name, color = androidx.compose.material3.MaterialTheme.colorScheme.onBackground, fontWeight = FontWeight.Medium, fontSize = 15.sp)
                if (isNsfw) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Surface(color = Color.Red.copy(alpha = 0.2f), shape = RoundedCornerShape(4.dp)) {
                        Text("18+", color = Color.Red, fontSize = 10.sp, modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp))
                    }
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = getLanguageDisplayName(extension.sources.firstOrNull()?.language ?: "en"),
                    color = Color.Gray,
                    fontSize = 12.sp
                )
                Text(
                    text = " • v${extension.versionName}",
                    color = Color.Gray,
                    fontSize = 12.sp
                )
            }
        }
        if (isInstalling) {
            CircularProgressIndicator(modifier = Modifier.size(24.dp), color = androidx.compose.material3.MaterialTheme.colorScheme.primary, strokeWidth = 2.dp)
        } else if (isInstalled) {
            IconButton(onClick = onUninstall) {
                Icon(Icons.Default.Delete, contentDescription = "Uninstall", tint = Color.Red.copy(alpha = 0.7f))
            }
        } else {
            IconButton(onClick = onInstall) {
                Icon(Icons.Default.Download, contentDescription = "Install", tint = androidx.compose.material3.MaterialTheme.colorScheme.primary)
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════
// Language display name helper (like Mihon)
// ═══════════════════════════════════════════════════════════════
fun getLanguageDisplayName(langCode: String): String {
    return when (langCode.lowercase()) {
        "", "all" -> "All"
        "en" -> "English"
        "ja" -> "Japanese"
        "ko" -> "Korean"
        "zh" -> "Chinese"
        "zh-hans", "zh-cn" -> "Chinese (Simplified)"
        "zh-hant", "zh-tw" -> "Chinese (Traditional)"
        "es" -> "Spanish"
        "es-419" -> "Spanish (LATAM)"
        "pt", "pt-br" -> "Portuguese (Brazil)"
        "pt-pt" -> "Portuguese"
        "fr" -> "French"
        "de" -> "German"
        "it" -> "Italian"
        "ru" -> "Russian"
        "ar" -> "Arabic"
        "th" -> "Thai"
        "vi" -> "Vietnamese"
        "id" -> "Indonesian"
        "pl" -> "Polish"
        "tr" -> "Turkish"
        "hi" -> "Hindi"
        "uk" -> "Ukrainian"
        "fil" -> "Filipino"
        "my" -> "Burmese"
        "ms" -> "Malay"
        "bn" -> "Bengali"
        "hu" -> "Hungarian"
        "nl" -> "Dutch"
        "ro" -> "Romanian"
        "el" -> "Greek"
        "cs" -> "Czech"
        "bg" -> "Bulgarian"
        "he" -> "Hebrew"
        "sv" -> "Swedish"
        "da" -> "Danish"
        "fi" -> "Finnish"
        "no" -> "Norwegian"
        "fa" -> "Persian"
        "lt" -> "Lithuanian"
        "ca" -> "Catalan"
        "eu" -> "Basque"
        "multi" -> "Multi"
        "other" -> "Other"
        else -> langCode.uppercase()
    }
}

@Composable
fun LNSourcesTab(context: android.content.Context, pinnedNovel: Set<String>, onTogglePin: (String) -> Unit) {
    val repoService = remember { com.example.wammy.lnreader.repo.IReaderRepoService(context) }
    var installedExts by remember { mutableStateOf<List<com.example.wammy.lnreader.model.IReaderExtensionMeta>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    fun loadInstalled() {
        installedExts = repoService.getInstalledExtensions()
        isLoading = false
    }

    LaunchedEffect(Unit) { loadInstalled() }

    if (isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = androidx.compose.material3.MaterialTheme.colorScheme.primary)
        }
    } else if (installedExts.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("No Novel sources installed", color = Color.Gray, fontSize = 16.sp)
                Spacer(modifier = Modifier.height(8.dp))
                Text("Download them from the Extensions tab", color = Color.Gray.copy(alpha = 0.6f), fontSize = 13.sp)
            }
        }
    } else {
        val pinnedNovelsList = installedExts.filter { pinnedNovel.contains(it.pkg) }
        val unpinnedNovelsList = installedExts.filter { !pinnedNovel.contains(it.pkg) }

        LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(vertical = 8.dp)) {
            if (pinnedNovelsList.isNotEmpty()) {
                item {
                    Text("Pinned", color = Color.Gray, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(start = 16.dp, top = 8.dp, bottom = 4.dp))
                }
                items(pinnedNovelsList) { ext ->
                    NovelSourceRow(ext, context, repoService, { loadInstalled() }, true, { onTogglePin(ext.pkg) })
                }
            }
            if (unpinnedNovelsList.isNotEmpty()) {
                item {
                    Text("Sources", color = Color.Gray, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(start = 16.dp, top = 8.dp, bottom = 4.dp))
                }
                items(unpinnedNovelsList) { ext ->
                    NovelSourceRow(ext, context, repoService, { loadInstalled() }, false, { onTogglePin(ext.pkg) })
                }
            }
        }
    }
}

@Composable
fun LNExtensionsTab(context: android.content.Context) {
    val repoService = remember { com.example.wammy.lnreader.repo.IReaderRepoService(context) }
    var allExts by remember { mutableStateOf<List<com.example.wammy.lnreader.model.IReaderExtensionMeta>>(emptyList()) }
    var installedPkgs by remember { mutableStateOf<Set<String>>(emptySet()) }
    var isLoading by remember { mutableStateOf(true) }
    var downloadingPkgs by remember { mutableStateOf<Set<String>>(emptySet()) }
    val scope = rememberCoroutineScope()

    fun refresh() {
        installedPkgs = repoService.getInstalledExtensions().map { it.pkg }.toSet()
    }

    LaunchedEffect(Unit) {
        try {
            refresh()
            allExts = repoService.fetchIndex()
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            isLoading = false
        }
    }

    if (isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = androidx.compose.material3.MaterialTheme.colorScheme.primary)
        }
    } else {
        val installedExts = allExts.filter { installedPkgs.contains(it.pkg) }
        val availableExts = allExts.filter { !installedPkgs.contains(it.pkg) }

        LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(horizontal = 0.dp)) {
            if (installedExts.isNotEmpty()) {
                item {
                    Text("Installed (${installedExts.size})", color = Color.Gray, fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(start = 16.dp, top = 8.dp, bottom = 4.dp))
                }
                items(installedExts) { ext ->
                    IReaderExtensionRow(ext = ext, isInstalled = true, isDownloading = false,
                        onAction = {
                            repoService.deleteExtension(ext)
                            Toast.makeText(context, "Removed ${ext.name}", Toast.LENGTH_SHORT).show()
                            refresh()
                        }
                    )
                }
            }

            if (availableExts.isNotEmpty()) {
                item {
                    Text("Available (${availableExts.size})", color = Color.Gray, fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 4.dp))
                }
                items(availableExts) { ext ->
                    val isDownloading = downloadingPkgs.contains(ext.pkg)
                    IReaderExtensionRow(ext = ext, isInstalled = false, isDownloading = isDownloading,
                        onAction = {
                            scope.launch {
                                downloadingPkgs = downloadingPkgs + ext.pkg
                                try {
                                    repoService.downloadExtension(ext) { /* ignore progress */ }
                                    Toast.makeText(context, "Installed ${ext.name}!", Toast.LENGTH_SHORT).show()
                                    refresh()
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Failed: ${e.message}", Toast.LENGTH_LONG).show()
                                } finally {
                                    downloadingPkgs = downloadingPkgs - ext.pkg
                                }
                            }
                        }
                    )
                }
            }
            item { Spacer(modifier = Modifier.height(80.dp)) }
        }
    }
}

@Composable
fun IReaderExtensionRow(
    ext: com.example.wammy.lnreader.model.IReaderExtensionMeta,
    isInstalled: Boolean,
    isDownloading: Boolean,
    onAction: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        coil.compose.AsyncImage(
            model = ext.iconUrl.takeIf { it.isNotEmpty() } ?: ("https://raw.githubusercontent.com/IReaderorg/IReader-extensions/repo/icon/" + ext.apk.replace(".apk", ".png")),
            contentDescription = null,
            modifier = Modifier.size(40.dp).clip(RoundedCornerShape(8.dp))
                .background(androidx.compose.material3.MaterialTheme.colorScheme.surfaceVariant),
            contentScale = androidx.compose.ui.layout.ContentScale.Crop
        )
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(ext.name, color = androidx.compose.material3.MaterialTheme.colorScheme.onBackground,
                    fontWeight = FontWeight.Medium, fontSize = 15.sp)
                if (ext.nsfw) {
                    Spacer(Modifier.width(6.dp))
                    Text("18+", color = Color.Red, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }
            Text("${ext.lang.uppercase()} • v${ext.version}", color = Color.Gray, fontSize = 12.sp)
        }
        if (isDownloading) {
            CircularProgressIndicator(modifier = Modifier.size(24.dp), color = androidx.compose.material3.MaterialTheme.colorScheme.primary, strokeWidth = 2.dp)
        } else if (isInstalled) {
            IconButton(onClick = onAction) {
                Icon(androidx.compose.material.icons.Icons.Default.Delete,
                    contentDescription = "Uninstall", tint = Color.Red.copy(alpha = 0.7f))
            }
        } else {
            IconButton(onClick = onAction) {
                Icon(androidx.compose.material.icons.Icons.Default.Download,
                    contentDescription = "Install", tint = androidx.compose.material3.MaterialTheme.colorScheme.primary)
            }
        }
    }
}


@Composable
fun NovelSourceRow(ext: com.example.wammy.lnreader.model.IReaderExtensionMeta, context: android.content.Context, repoService: com.example.wammy.lnreader.repo.IReaderRepoService, onRefresh: () -> Unit, isPinned: Boolean, onPin: () -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                val intent = android.content.Intent(context, com.example.wammy.ui.reader.BrowseNovelActivity::class.java)
                intent.putExtra("APK_FILE", ext.apk)
                intent.putExtra("SOURCE_NAME", ext.name)
                intent.putExtra("PKG", ext.pkg)
                intent.putExtra("LANG", ext.lang)
                intent.putExtra("SOURCE_DIR", ext.sourceDir)
                context.startActivity(intent)
            }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        coil.compose.AsyncImage(
            model = ext.iconUrl.takeIf { it.isNotEmpty() } ?: ("https://raw.githubusercontent.com/IReaderorg/IReader-extensions/repo/icon/" + ext.apk.replace(".apk", ".png")),
            contentDescription = null,
            modifier = Modifier.size(40.dp).clip(RoundedCornerShape(8.dp))
                .background(androidx.compose.material3.MaterialTheme.colorScheme.surfaceVariant),
            contentScale = androidx.compose.ui.layout.ContentScale.Crop
        )
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(ext.name, color = androidx.compose.material3.MaterialTheme.colorScheme.onBackground,
                fontWeight = FontWeight.Medium, fontSize = 16.sp)
            Text("${ext.lang} • v${ext.version}", color = Color.Gray, fontSize = 12.sp)
        }
        Box {
            IconButton(onClick = { expanded = true }) {
                Icon(androidx.compose.material.icons.Icons.Default.MoreVert, contentDescription = "More", tint = Color.Gray)
            }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                DropdownMenuItem(
                    text = { Text(if (isPinned) "Unpin" else "Pin") },
                    onClick = { expanded = false; onPin() }
                )
                DropdownMenuItem(
                    text = { Text("Delete", color = Color.Red) },
                    onClick = { 
                        expanded = false
                        repoService.deleteExtension(ext)
                        Toast.makeText(context, "Removed ${ext.name}", Toast.LENGTH_SHORT).show()
                        onRefresh()
                    }
                )
            }
        }
    }
}
