package com.example.wammy.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.wammy.ui.StatisticsViewModel
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatisticsScreen(
    viewModel: StatisticsViewModel = viewModel(),
    onBack: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val inLibrary by viewModel.inLibraryCount.collectAsState()
    val completed by viewModel.completedCount.collectAsState()
    val started by viewModel.startedCount.collectAsState()
    val totalChapters by viewModel.totalChaptersCount.collectAsState()
    val readChapters by viewModel.readChaptersCount.collectAsState()
    val trackedEntries by viewModel.trackedEntriesCount.collectAsState()
    val meanScore by viewModel.meanScore.collectAsState()
    val usedTrackers by viewModel.usedTrackersCount.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Statistics", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            item { Spacer(modifier = Modifier.height(8.dp)) }

            item {
                SectionTitle("Overview")
                StatCard {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        StatItem(value = inLibrary.toString(), label = "In library", icon = Icons.Default.LibraryBooks)
                        val durationMillis = com.example.wammy.util.ReadDurationTracker.getTotalDuration(context)
                        val durationStr = if (durationMillis == 0L) "N/A" else com.example.wammy.util.ReadDurationTracker.formatDuration(durationMillis)
                        StatItem(value = durationStr, label = "Read duration", icon = Icons.Default.AccessTime)
                        StatItem(value = completed.toString(), label = "Completed entries", icon = Icons.Default.MenuBook)
                    }
                }
            }

            item {
                SectionTitle("Entries")
                StatCard {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        StatItem(value = "N/A", label = "In global update")
                        StatItem(value = started.toString(), label = "Started")
                        StatItem(value = "N/A", label = "Local")
                    }
                }
            }

            item {
                SectionTitle("Chapters")
                StatCard {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        StatItem(value = totalChapters.toString(), label = "Total")
                        StatItem(value = readChapters.toString(), label = "Read")
                        StatItem(value = "N/A", label = "Downloaded")
                    }
                }
            }

            item {
                SectionTitle("Trackers")
                StatCard {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        StatItem(value = trackedEntries.toString(), label = "Tracked entries")
                        StatItem(value = if (meanScore == null || meanScore == 0f) "N/A" else String.format(Locale.US, "%.1f", meanScore), label = "Mean score")
                        StatItem(value = usedTrackers.toString(), label = "Used")
                    }
                }
            }
            
            item { Spacer(modifier = Modifier.height(32.dp)) }
        }
    }
}

@Composable
fun SectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Medium),
        color = MaterialTheme.colorScheme.onBackground,
        modifier = Modifier.padding(bottom = 8.dp, start = 8.dp)
    )
}

@Composable
fun StatCard(content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .padding(vertical = 24.dp, horizontal = 16.dp)
    ) {
        content()
    }
}

@Composable
fun StatItem(value: String, label: String, icon: ImageVector? = null) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(100.dp)
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
            textAlign = TextAlign.Center
        )
        if (icon != null) {
            Spacer(modifier = Modifier.height(16.dp))
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}
