package eu.kanade.tachiyomi.ui.browse.source.feed

import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.outlined.FlipToBack
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Checklist
import androidx.compose.ui.platform.LocalContext
import eu.kanade.tachiyomi.util.system.toast

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.FilterList
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.compose.viewModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import eu.kanade.core.util.ifSourcesLoaded
import eu.kanade.presentation.browse.components.GlobalSearchCardRow
import eu.kanade.presentation.browse.components.GlobalSearchLoadingResultItem
import eu.kanade.presentation.browse.components.GlobalSearchResultItem
import eu.kanade.presentation.util.Screen
import eu.kanade.tachiyomi.source.online.HttpSource
import eu.kanade.tachiyomi.ui.browse.source.browse.BrowseSourceScreen
import eu.kanade.tachiyomi.ui.manga.MangaScreen
import eu.kanade.tachiyomi.ui.webview.WebViewScreen
import tachiyomi.core.common.util.lang.launchIO
import tachiyomi.domain.source.interactor.GetRemoteManga
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.material.Scaffold
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.screens.LoadingScreen

data class SourceFeedScreen(
    val sourceId: Long,
) : Screen() {

    @Composable
    override fun Content() {
        if (!ifSourcesLoaded()) {
            LoadingScreen()
            return
        }

        val viewModel = viewModel<SourceFeedViewModel>(
            factory = SourceFeedViewModel.Factory,
            extras = CreationExtras {
                set(SourceFeedViewModel.SOURCE_ID_KEY, sourceId)
            },
        )
        val state by viewModel.state.collectAsState()
        val navigator = LocalNavigator.currentOrThrow
        val scope = rememberCoroutineScope()
        val haptic = LocalHapticFeedback.current
        val context = LocalContext.current

        val source = viewModel.source

        Scaffold(
            topBar = {
                if (state.selectionMode) {
                    eu.kanade.presentation.components.AppBar(
                        titleContent = { Text(text = "${state.selection.size} selected") },
                        navigateUp = { viewModel.clearSelection() },
                        actions = {
                            IconButton(onClick = { viewModel.selectAll() }) {
                                Icon(
                                    imageVector = Icons.Filled.SelectAll,
                                    contentDescription = stringResource(MR.strings.action_select_all),
                                )
                            }
                            IconButton(onClick = { viewModel.invertSelection() }) {
                                Icon(
                                    imageVector = Icons.Outlined.FlipToBack,
                                    contentDescription = stringResource(MR.strings.action_select_inverse),
                                )
                            }
                            if (state.selection.isNotEmpty()) {
                                IconButton(onClick = {
                                    val size = state.selection.size
                                    state.selection.forEach { manga ->
                                        if (!manga.favorite) {
                                            viewModel.addFavorite(manga)
                                        }
                                    }
                                    viewModel.clearSelection()
                                    context.toast("Added $size to library")
                                }) {
                                    Icon(
                                        imageVector = Icons.Outlined.FavoriteBorder,
                                        contentDescription = stringResource(MR.strings.add_to_library),
                                    )
                                }
                            }
                        },
                    )
                } else {
                    TopAppBar(
                        title = {
                            Text(
                                text = source.name,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        },
                        navigationIcon = {
                            IconButton(onClick = { navigator.pop() }) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                                    contentDescription = null,
                                )
                            }
                        },
                        actions = {
                            IconButton(onClick = { viewModel.toggleSelectionMode() }) {
                                Icon(
                                    imageVector = Icons.Outlined.Checklist,
                                    contentDescription = null,
                                )
                            }
                            IconButton(onClick = {
                                navigator.push(BrowseSourceScreen(sourceId, null))
                            }) {
                                Icon(
                                    imageVector = Icons.Outlined.Search,
                                    contentDescription = stringResource(MR.strings.action_search),
                                )
                            }
                            if (viewModel.getFilters().isNotEmpty()) {
                                IconButton(onClick = {
                                    navigator.push(BrowseSourceScreen(sourceId, null))
                                }) {
                                    Icon(
                                        imageVector = Icons.Outlined.FilterList,
                                        contentDescription = stringResource(MR.strings.action_filter),
                                    )
                                }
                            }
                            if (source is HttpSource) {
                                IconButton(onClick = {
                                    navigator.push(
                                        WebViewScreen(
                                            url = source.baseUrl,
                                            initialTitle = source.name,
                                            sourceId = source.id,
                                        ),
                                    )
                                }) {
                                    Icon(
                                        imageVector = Icons.Outlined.Public,
                                        contentDescription = stringResource(MR.strings.action_web_view),
                                    )
                                }
                            }
                        },
                    )
                }
            },
            floatingActionButton = {
                if (viewModel.getFilters().isNotEmpty()) {
                    ExtendedFloatingActionButton(
                        text = { Text(text = stringResource(MR.strings.action_filter)) },
                        icon = {
                            Icon(
                                imageVector = Icons.Outlined.FilterList,
                                contentDescription = null,
                            )
                        },
                        onClick = {
                            navigator.push(BrowseSourceScreen(sourceId, null))
                        },
                    )
                }
            },
        ) { paddingValues ->
            LazyColumn(
                contentPadding = paddingValues,
            ) {
                // Latest section
                if (source.supportsLatest) {
                    item(key = "latest") {
                        GlobalSearchResultItem(
                            title = stringResource(MR.strings.latest),
                            subtitle = source.name,
                            onClick = {
                                navigator.push(
                                    BrowseSourceScreen(sourceId, GetRemoteManga.QUERY_LATEST),
                                )
                            },
                        ) {
                            val latestItems = state.latestItems
                            if (latestItems == null) {
                                GlobalSearchLoadingResultItem()
                            } else {
                                GlobalSearchCardRow(
                                    titles = latestItems,
                                    getManga = { viewModel.getManga(it) },
                                    selection = state.selection,
                                    onClick = { manga ->
                                        if (state.selectionMode) {
                                            viewModel.toggleSelection(manga)
                                        } else {
                                            navigator.push(MangaScreen(manga.id, true))
                                        }
                                    },
                                    onLongClick = { manga ->
                                        if (state.selectionMode) {
                                            viewModel.toggleSelection(manga)
                                        } else {
                                            scope.launchIO {
                                                val duplicates = viewModel.getDuplicateLibraryManga(manga)
                                                when {
                                                    manga.favorite -> viewModel.setDialog(
                                                        SourceFeedViewModel.Dialog.RemoveManga(manga),
                                                    )
                                                    duplicates.isNotEmpty() -> viewModel.setDialog(
                                                        SourceFeedViewModel.Dialog.AddDuplicateManga(
                                                            manga,
                                                            duplicates,
                                                        ),
                                                    )
                                                    else -> viewModel.addFavorite(manga)
                                                }
                                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            }
                                        }
                                    },
                                )
                            }
                        }
                    }
                }

                // Browse (Popular) section
                item(key = "browse") {
                    GlobalSearchResultItem(
                        title = stringResource(MR.strings.popular),
                        subtitle = source.name,
                        onClick = {
                            navigator.push(
                                BrowseSourceScreen(sourceId, GetRemoteManga.QUERY_POPULAR),
                            )
                        },
                    ) {
                        val browseItems = state.browseItems
                        if (browseItems == null) {
                            GlobalSearchLoadingResultItem()
                        } else {
                            GlobalSearchCardRow(
                                titles = browseItems,
                                getManga = { viewModel.getManga(it) },
                                selection = state.selection,
                                onClick = { manga ->
                                    if (state.selectionMode) {
                                        viewModel.toggleSelection(manga)
                                    } else {
                                        navigator.push(MangaScreen(manga.id, true))
                                    }
                                },
                                onLongClick = { manga ->
                                    if (state.selectionMode) {
                                        viewModel.toggleSelection(manga)
                                    } else {
                                        scope.launchIO {
                                            val duplicates = viewModel.getDuplicateLibraryManga(manga)
                                            when {
                                                manga.favorite -> viewModel.setDialog(
                                                    SourceFeedViewModel.Dialog.RemoveManga(manga),
                                                )
                                                duplicates.isNotEmpty() -> viewModel.setDialog(
                                                    SourceFeedViewModel.Dialog.AddDuplicateManga(
                                                        manga,
                                                        duplicates,
                                                    ),
                                                )
                                                else -> viewModel.addFavorite(manga)
                                            }
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        }
                                    }
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}
