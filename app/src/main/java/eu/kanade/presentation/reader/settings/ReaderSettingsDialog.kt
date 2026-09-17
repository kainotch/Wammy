package eu.kanade.presentation.reader.settings

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.RecordVoiceOver
import androidx.compose.material.icons.outlined.Swipe
import androidx.compose.material.icons.outlined.TextFields
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.window.DialogWindowProvider
import eu.kanade.presentation.components.TabTitle
import eu.kanade.presentation.components.TabbedDialog
import eu.kanade.presentation.components.TabbedDialogPaddings
import eu.kanade.presentation.components.toTabTitles
import eu.kanade.tachiyomi.ui.reader.setting.ReaderSettingsViewModel
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.util.collectAsState

@Composable
fun ReaderSettingsDialog(
    onDismissRequest: () -> Unit,
    onShowMenus: () -> Unit,
    onHideMenus: () -> Unit,
    viewModel: ReaderSettingsViewModel,
    isNovelMode: Boolean = false,
) {
    if (isNovelMode) {
        NovelReaderSettingsDialog(
            onDismissRequest = onDismissRequest,
            onShowMenus = onShowMenus,
            viewModel = viewModel,
        )
    } else {
        MangaReaderSettingsDialog(
            onDismissRequest = onDismissRequest,
            onShowMenus = onShowMenus,
            onHideMenus = onHideMenus,
            viewModel = viewModel,
        )
    }
}

@Composable
private fun MangaReaderSettingsDialog(
    onDismissRequest: () -> Unit,
    onShowMenus: () -> Unit,
    onHideMenus: () -> Unit,
    viewModel: ReaderSettingsViewModel,
) {
    val tabTitles = listOf(
        stringResource(MR.strings.pref_category_reading_mode),
        stringResource(MR.strings.pref_category_general),
        stringResource(MR.strings.custom_filter),
    ).toTabTitles()
    val pagerState = rememberPagerState { tabTitles.size }

    BoxWithConstraints {
        TabbedDialog(
            modifier = Modifier.heightIn(max = maxHeight * 0.75f),
            onDismissRequest = {
                onDismissRequest()
                onShowMenus()
            },
            tabTitles = tabTitles,
            pagerState = pagerState,
        ) { page ->
            val window = (LocalView.current.parent as? DialogWindowProvider)?.window

            LaunchedEffect(pagerState.currentPage) {
                if (pagerState.currentPage == 2) {
                    window?.setDimAmount(0f)
                    onHideMenus()
                } else {
                    window?.setDimAmount(0.5f)
                    onShowMenus()
                }
            }

            Column(
                modifier = Modifier
                    .padding(vertical = TabbedDialogPaddings.Vertical)
                    .verticalScroll(rememberScrollState()),
            ) {
                when (page) {
                    0 -> ReadingModePage(viewModel)
                    1 -> GeneralPage(viewModel)
                    2 -> ColorFilterPage(viewModel)
                }
            }
        }
    }
}

@Composable
private fun NovelReaderSettingsDialog(
    onDismissRequest: () -> Unit,
    onShowMenus: () -> Unit,
    viewModel: ReaderSettingsViewModel,
) {
    val renderingMode by viewModel.preferences.novelRenderingMode.collectAsState()
    val tabTitles = listOf(
        TabTitle.Icon(imageVector = Icons.Outlined.TextFields), // Reading
        TabTitle.Icon(imageVector = Icons.Outlined.Palette), // Appearance
        TabTitle.Icon(imageVector = Icons.Outlined.Swipe), // Controls
        TabTitle.Icon(imageVector = Icons.Outlined.RecordVoiceOver), // TTS
        TabTitle.Icon(imageVector = Icons.Outlined.Code), // Advanced
    )
    val pagerState = rememberPagerState { tabTitles.size }

    BoxWithConstraints {
        TabbedDialog(
            modifier = Modifier.heightIn(max = maxHeight * 0.75f),
            onDismissRequest = {
                onDismissRequest()
                onShowMenus()
            },
            tabTitles = tabTitles,
            pagerState = pagerState,
        ) { page ->
            Column(
                modifier = Modifier
                    .padding(vertical = TabbedDialogPaddings.Vertical)
                    .verticalScroll(rememberScrollState()),
            ) {
                when (page) {
                    0 -> NovelReadingTab(viewModel, renderingMode)
                    1 -> NovelAppearanceTab(viewModel, renderingMode)
                    2 -> NovelControlsTab(viewModel, renderingMode)
                    3 -> NovelTtsTab(viewModel)
                    4 -> NovelAdvancedTab(viewModel, renderingMode)
                }
            }
        }
    }
}
