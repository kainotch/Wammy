package com.example.wammy.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.wammy.AppContainer
import com.example.wammy.data.prefs.*
import com.example.wammy.ui.ReaderViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReaderSettingsSheet(
    viewModel: ReaderViewModel,
    onDismissRequest: () -> Unit
) {
    val prefs = AppContainer.readerPreferences
    val readingMode by viewModel.readingMode.collectAsState()
    
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        containerColor = Color(0xFF1E1E28),
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        var selectedTab by remember { mutableStateOf(0) }
        val tabs = listOf("Reading", "General", "Color Filter")

        Column(modifier = Modifier.fillMaxWidth().heightIn(min = 400.dp, max = 600.dp)) {
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color.Transparent,
                contentColor = Color.White,
                indicator = { tabPositions ->
                    if (selectedTab < tabPositions.size) {
                        TabRowDefaults.SecondaryIndicator(
                            Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                            color = Color(0xFFB388FF)
                        )
                    }
                }
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(title) }
                    )
                }
            }

            Box(modifier = Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState())) {
                when (selectedTab) {
                    0 -> ReadingTab(viewModel, readingMode)
                    1 -> GeneralTab(prefs)
                    2 -> ColorFilterTab(prefs)
                }
            }
        }
    }
}

@Composable
private fun ReadingTab(viewModel: ReaderViewModel, currentMode: ReadingMode) {
    Column {
        Text("Reading Mode", color = Color.White, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        
        val modes = listOf(
            ReadingMode.DEFAULT to "Default",
            ReadingMode.LTR to "Left to Right",
            ReadingMode.RTL to "Right to Left",
            ReadingMode.VERTICAL to "Vertical",
            ReadingMode.WEBTOON to "Webtoon",
            ReadingMode.CONTINUOUS_VERTICAL to "Cont. Vertical"
        )
        
        modes.forEach { (mode, label) ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { viewModel.setReadingModeOverride(mode) }
                    .padding(vertical = 12.dp)
            ) {
                RadioButton(
                    selected = currentMode == mode,
                    onClick = null,
                    colors = RadioButtonDefaults.colors(selectedColor = Color(0xFFB388FF), unselectedColor = Color.Gray)
                )
                Spacer(Modifier.width(16.dp))
                Text(label, color = Color.White)
            }
        }
        
        Spacer(Modifier.height(24.dp))
        Text("Orientation", color = Color.White, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        
        val orientations = listOf(
            OrientationType.DEFAULT to "Default",
            OrientationType.FREE to "Free",
            OrientationType.PORTRAIT to "Portrait",
            OrientationType.LANDSCAPE to "Landscape"
        )
        val currentOrientation by viewModel.orientation.collectAsState()
        
        orientations.forEach { (orientation, label) ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { viewModel.setOrientationOverride(orientation) }
                    .padding(vertical = 12.dp)
            ) {
                RadioButton(
                    selected = currentOrientation == orientation,
                    onClick = null,
                    colors = RadioButtonDefaults.colors(selectedColor = Color(0xFFB388FF), unselectedColor = Color.Gray)
                )
                Spacer(Modifier.width(16.dp))
                Text(label, color = Color.White)
            }
        }
    }
}

@Composable
private fun GeneralTab(prefs: ReaderPreferences) {
    val bgTheme by prefs.readerTheme.state.collectAsState()
    val fullscreen by prefs.fullscreen.state.collectAsState()
    val keepScreenOn by prefs.keepScreenOn.state.collectAsState()
    val showPageNumber by prefs.showPageNumber.state.collectAsState()
    
    Column {
        Text("Background Color", color = Color.White, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        
        val themes = listOf(
            ReaderTheme.BLACK to "Black",
            ReaderTheme.GRAY to "Gray",
            ReaderTheme.WHITE to "White",
            ReaderTheme.AUTOMATIC to "Automatic"
        )
        
        themes.forEach { (theme, label) ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { prefs.readerTheme.set(theme) }
                    .padding(vertical = 12.dp)
            ) {
                RadioButton(
                    selected = bgTheme == theme,
                    onClick = null,
                    colors = RadioButtonDefaults.colors(selectedColor = Color(0xFFB388FF), unselectedColor = Color.Gray)
                )
                Spacer(Modifier.width(16.dp))
                Text(label, color = Color.White)
            }
        }
        
        Spacer(Modifier.height(24.dp))
        Text("Display", color = Color.White, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        
        SwitchSetting("Fullscreen", fullscreen) { prefs.fullscreen.set(it) }
        SwitchSetting("Keep screen on", keepScreenOn) { prefs.keepScreenOn.set(it) }
        SwitchSetting("Show page number", showPageNumber) { prefs.showPageNumber.set(it) }
    }
}

@Composable
private fun ColorFilterTab(prefs: ReaderPreferences) {
    val grayscale by prefs.grayscale.state.collectAsState()
    val inverted by prefs.invertedColors.state.collectAsState()
    val customBrightness by prefs.customBrightness.state.collectAsState()
    val brightnessValue by prefs.customBrightnessValue.state.collectAsState()
    val colorFilter by prefs.colorFilter.state.collectAsState()
    
    Column {
        Text("Filters", color = Color.White, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        
        SwitchSetting("Grayscale", grayscale) { prefs.grayscale.set(it) }
        SwitchSetting("Inverted Colors", inverted) { prefs.invertedColors.set(it) }
        
        Spacer(Modifier.height(24.dp))
        
        SwitchSetting("Custom Brightness", customBrightness) { prefs.customBrightness.set(it) }
        if (customBrightness) {
            Slider(
                value = brightnessValue.toFloat(),
                onValueChange = { prefs.customBrightnessValue.set(it.toInt()) },
                valueRange = -100f..100f,
                colors = SliderDefaults.colors(thumbColor = Color(0xFFB388FF), activeTrackColor = Color(0xFFB388FF))
            )
        }
        
        Spacer(Modifier.height(24.dp))
        
        SwitchSetting("Custom Color Filter", colorFilter) { prefs.colorFilter.set(it) }
        // Simple placeholder for color sliders (since we don't have a full color picker UI)
        if (colorFilter) {
            Text("Edit RGBA values globally in App Settings", color = Color.Gray, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun SwitchSetting(title: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 12.dp)
    ) {
        Text(title, color = Color.White)
        Switch(
            checked = checked,
            onCheckedChange = null,
            colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = Color(0xFFB388FF))
        )
    }
}
