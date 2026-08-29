// Created by Notch
package com.example.wammy.ui

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.collectAsState
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.wammy.ui.components.Screen
import com.example.wammy.ui.components.WammyBottomNav
import com.example.wammy.ui.screens.DetailsScreen
import com.example.wammy.ui.screens.HistoryScreen
import com.example.wammy.ui.screens.HomeScreen
import com.example.wammy.ui.screens.LibraryScreen
import android.content.Context
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext


import com.example.wammy.util.NetworkUtils

import com.example.wammy.data.local.ChapterEntity
import com.example.wammy.ui.screens.ReaderScreen
import com.example.wammy.ui.screens.OnboardingScreen

import com.example.wammy.ui.screens.BrowseSourceScreen
import com.example.wammy.ui.screens.BrowseScreen
import com.example.wammy.ui.ExtensionsViewModel
import com.example.wammy.ui.screens.SettingsScreen

@Composable
fun WammyApp() {
    val context = LocalContext.current
    var currentScreen by remember { mutableStateOf(if (NetworkUtils.isOnline(context)) Screen.Home else Screen.Library) }
    
    // Hoisted scroll states to survive DetailsScreen overlay recompositions
    val historyListState = rememberLazyListState()
    val libraryListState = rememberLazyListState()
    val libraryGridState = rememberLazyGridState()
    val sharedPrefs = remember { context.getSharedPreferences("wammy_prefs", Context.MODE_PRIVATE) }
    var hasCompletedOnboarding by remember { mutableStateOf(sharedPrefs.getBoolean("has_completed_onboarding", false)) }
    
    
    var showDataStorage by remember { mutableStateOf(false) }
    var showAppearance by remember { mutableStateOf(false) }
    var showAbout by remember { mutableStateOf(false) }
    var showStatistics by remember { mutableStateOf(false) }
    var selectedBrowseSourceId by remember { mutableStateOf<Long?>(null) }
    var selectedMangaId by remember { mutableStateOf<String?>(null) }
    var selectedFolderId by remember { mutableStateOf<Long?>(null) }
    var openedFromDownloads by remember { mutableStateOf(false) }
    var selectedChapter by remember { mutableStateOf<ChapterEntity?>(null) }
    var selectedMangaForReader by remember { mutableStateOf<com.example.wammy.data.local.MangaEntity?>(null) }
    var selectedSourceId by remember { mutableStateOf(1L) }
    var selectedBrowseTab by remember { androidx.compose.runtime.mutableIntStateOf(0) }

val homeViewModel: HomeViewModel = viewModel()
    val detailsViewModel: DetailsViewModel = viewModel()
    val readerViewModel: ReaderViewModel = viewModel()
    val historyViewModel: HistoryViewModel = viewModel()
    val libraryViewModel: LibraryViewModel = viewModel()
    val browseSourceViewModel: com.example.wammy.ui.BrowseSourceViewModel = viewModel()
    val extViewModel: ExtensionsViewModel = viewModel()
    
    val isSearching by homeViewModel.isSearching.collectAsState()

    BackHandler(enabled = selectedChapter != null || selectedMangaId != null || selectedBrowseSourceId != null || selectedFolderId != null  || showDataStorage || showAbout || showStatistics || (currentScreen == Screen.Home && isSearching) || currentScreen != Screen.Home) {
        if (selectedChapter != null) {
            selectedChapter = null
        } else if (selectedMangaId != null) {
            selectedMangaId = null
        } else if (selectedBrowseSourceId != null) {
            selectedBrowseSourceId = null
        } else if (selectedFolderId != null) {
            selectedFolderId = null
        } else if (showAbout) {
            showAbout = false
        } else if (showStatistics) {
            showStatistics = false
        } else if (showAppearance) {
            showAppearance = false
        } else if (showDataStorage) {
            showDataStorage = false
        } else if (currentScreen == Screen.Home && isSearching) {
            homeViewModel.toggleSearch()
        } else if (currentScreen != Screen.Home) {
            currentScreen = Screen.Home
        }
    }

    if (!hasCompletedOnboarding) {
        OnboardingScreen(
            onFinish = {
                sharedPrefs.edit().putBoolean("has_completed_onboarding", true).apply()
                hasCompletedOnboarding = true
                currentScreen = Screen.Browse
            }
        )
    } else if (selectedChapter != null) {
        LaunchedEffect(selectedChapter) {
            val chapters = detailsViewModel.chapters.value
            val startIndex = chapters.indexOfFirst { it.sourceUrl == selectedChapter?.sourceUrl }.takeIf { it >= 0 } ?: 0
            readerViewModel.initReader(chapters, startIndex, selectedSourceId, selectedMangaForReader!!)
        }
        ReaderScreen(
            viewModel = readerViewModel,
            onBack = { selectedChapter = null }
        )
    } else if (selectedMangaId != null) {
        detailsViewModel.loadManga(selectedMangaId!!, homeViewModel.latestManga.value + homeViewModel.searchResults.value + homeViewModel.bigThreeManga.value + homeViewModel.seinenManga.value + browseSourceViewModel.mangaList.value)
        DetailsScreen(
            viewModel = detailsViewModel,
            onBack = { selectedMangaId = null },
            showDownloadedOnly = openedFromDownloads,
            onChapterClick = { chapter, sourceId, manga -> 
                selectedSourceId = sourceId
                selectedMangaForReader = manga
                selectedChapter = chapter
            }
        )
    } else if (showAbout) {
        com.example.wammy.ui.screens.AboutScreen(onBack = { showAbout = false })
    } else if (showStatistics) {
        com.example.wammy.ui.screens.StatisticsScreen(onBack = { showStatistics = false })
    } else if (showAppearance) {
        com.example.wammy.ui.screens.AppearanceScreen(onBack = { showAppearance = false })
    } else if (showDataStorage) {
        com.example.wammy.ui.screens.DataStorageScreen(onBack = { showDataStorage = false })
    } else if (selectedBrowseSourceId != null) {
        BrowseSourceScreen(
            sourceId = selectedBrowseSourceId!!,
            viewModel = browseSourceViewModel,
            onBack = { selectedBrowseSourceId = null },
            onMangaClick = { mangaId ->
                selectedMangaId = mangaId
                openedFromDownloads = false
            }
        )
    } else if (selectedFolderId != null) {
            com.example.wammy.ui.screens.FolderScreen(
                folderId = selectedFolderId!!,
                onBack = { selectedFolderId = null },
                onMangaClick = { mangaId, fromDownloads ->
                    selectedMangaId = mangaId
                    openedFromDownloads = fromDownloads
                }
            )
        } else {
        com.example.wammy.ui.components.WammyBackground {
        Scaffold(
            containerColor = androidx.compose.ui.graphics.Color.Transparent,
            bottomBar = {
                WammyBottomNav(
                    currentScreen = currentScreen,
                    onScreenSelected = { 
                        if (it == Screen.Browse) selectedBrowseTab = 0
                        currentScreen = it 
                    }
                )
            }
        ) { innerPadding ->
            Crossfade(targetState = currentScreen, modifier = Modifier.padding(innerPadding)) { screen ->
                when (screen) {
Screen.Home -> HomeScreen(
                        viewModel = homeViewModel,
                        onMangaClick = { mangaId -> 
                            selectedMangaId = mangaId
                            openedFromDownloads = false
                        }
                    )
                    Screen.History -> HistoryScreen(
                        viewModel = historyViewModel,
                        onMangaClick = { mangaId -> 
                            selectedMangaId = mangaId
                            openedFromDownloads = false
                        },
                        listState = historyListState
                    )
                    Screen.Library -> LibraryScreen(
                        viewModel = libraryViewModel,
                        homeViewModel = homeViewModel,
                        onFolderClick = { folderId ->
                            selectedFolderId = folderId
                        },
                        onMangaClick = { mangaId, fromDownloads -> 
                            selectedMangaId = mangaId
                            openedFromDownloads = fromDownloads 
                        },
                        initialFilter = if (NetworkUtils.isOnline(context)) "Entries" else "Downloaded",
                        listState = libraryListState,
                        gridState = libraryGridState
                    )
                    Screen.Settings -> SettingsScreen(
                        onExtensionsClick = { 
                            selectedBrowseTab = 1
                            currentScreen = Screen.Browse 
                        },
                        onDataStorageClick = { showDataStorage = true },
                        onAboutClick = { showAbout = true },
                        onStatisticsClick = { showStatistics = true },
                        onAppearanceClick = { showAppearance = true }
                    )
                    Screen.Browse -> BrowseScreen(
                        extViewModel = extViewModel,
                        homeViewModel = homeViewModel,
                        initialTab = selectedBrowseTab,
                        onSourceClick = { sourceId ->
                            selectedBrowseSourceId = sourceId
                        }
                    )
                }
            }
        }
    }
}
}
