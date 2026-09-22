package eu.kanade.tachiyomi.ui.manga.recommendations

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier

import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import eu.kanade.presentation.browse.components.GlobalSearchCardRow
import eu.kanade.presentation.browse.components.GlobalSearchLoadingResultItem
import eu.kanade.presentation.browse.components.GlobalSearchResultItem
import eu.kanade.presentation.util.Screen
import eu.kanade.tachiyomi.ui.manga.MangaScreen
import tachiyomi.presentation.core.components.material.Scaffold
import tachiyomi.presentation.core.screens.LoadingScreen

class RecommendationsScreen(
    private val mangaId: Long,
    private val sourceId: Long,
) : Screen() {

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow

        val viewModel = viewModel<RecommendationsViewModel>(
            factory = viewModelFactory {
                initializer {
                    RecommendationsViewModel(
                        mangaId = mangaId,
                        sourceId = sourceId,
                    )
                }
            },
        )

        val state by viewModel.state.collectAsState()

        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = if (state.title.isNotEmpty()) "Similar to ${state.title}" else "Recommendations",
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
                )
            },
        ) { paddingValues ->
            if (state.sections.isEmpty()) {
                LoadingScreen()
                return@Scaffold
            }

            LazyColumn(modifier = Modifier.padding(paddingValues)) {
                state.sections.forEach { (key, result) ->
                    val parts = key.split("|")
                    val sourceName = parts.getOrElse(0) { key }.trim()
                    val subtitle = parts.getOrElse(1) { "" }.trim()

                    item(key = key) {
                        GlobalSearchResultItem(
                            title = sourceName,
                            subtitle = subtitle,
                            onClick = {},
                        ) {
                            when (result) {
                                is RecommendationsViewModel.SectionResult.Loading -> {
                                    GlobalSearchLoadingResultItem()
                                }
                                is RecommendationsViewModel.SectionResult.Success -> {
                                    if (result.isEmpty) {
                                        GlobalSearchLoadingResultItem() // Shows empty state
                                    } else {
                                        GlobalSearchCardRow(
                                            titles = result.mangas,
                                            getManga = { manga ->
                                                androidx.compose.runtime.produceState(initialValue = manga) {
                                                    value = manga
                                                }
                                            },
                                            onClick = { manga ->
                                                navigator.push(MangaScreen(manga.id, true))
                                            },
                                            onLongClick = { },
                                        )
                                    }
                                }
                                is RecommendationsViewModel.SectionResult.Error -> {
                                    GlobalSearchErrorItem(result.message)
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
private fun GlobalSearchErrorItem(message: String) {
    androidx.compose.material3.Text(
        text = message,
        modifier = Modifier
            .padding(
                horizontal = 16.dp,
                vertical = 8.dp,
            ),
        style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
    )
}
