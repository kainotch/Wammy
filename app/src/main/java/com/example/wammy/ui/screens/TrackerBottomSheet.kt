// Created by Notch
package com.example.wammy.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrackerBottomSheet(
    mangaId: Long,
    title: String,
    viewModel: com.example.wammy.ui.DetailsViewModel,
    onDismissRequest: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val tracks by viewModel.tracks.collectAsState()
    val services = com.example.wammy.AppContainer.trackManager.services

    var searchResults by remember { mutableStateOf<List<com.example.wammy.track.TrackSearchItem>>(emptyList()) }
    var isSearching by remember { mutableStateOf(false) }
    var currentSearchServiceId by remember { mutableStateOf(-1) }
    val coroutineScope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState()

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        containerColor = androidx.compose.material3.MaterialTheme.colorScheme.surfaceVariant
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Tracking", color = androidx.compose.material3.MaterialTheme.colorScheme.onBackground, style = MaterialTheme.typography.titleLarge)
            Spacer(modifier = Modifier.height(16.dp))

            if (isSearching) {
                Text("Search Results for $title", color = Color.Gray)
                Spacer(modifier = Modifier.height(8.dp))
                LazyColumn {
                    items(searchResults) { item ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.bindTracker(item.toTrackEntity(mangaId, currentSearchServiceId)) // Hardcoded 1 for AniList for now
                                    isSearching = false
                                }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(item.title, color = androidx.compose.material3.MaterialTheme.colorScheme.onBackground)
                        }
                    }
                }
            } else {
                services.forEach { service ->
                    val track = tracks.find { it.syncId == service.id }
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(service.name, color = androidx.compose.material3.MaterialTheme.colorScheme.onBackground, style = MaterialTheme.typography.bodyLarge)

                        if (!service.isLogged()) {
                            TextButton(onClick = {
                                val authUrl = when(service) {
                                    is com.example.wammy.track.anilist.AniListTracker -> service.getAuthUrl()
                                    is com.example.wammy.track.mal.MalTracker -> service.getAuthUrl()
                                    is com.example.wammy.track.kitsu.KitsuTracker -> service.getAuthUrl()
                                    else -> ""
                                }
                                if (authUrl.isNotEmpty()) {
                                    val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(authUrl))
                                    context.startActivity(intent)
                                }
                            }) {
                                Text("Login", color = Color(0xFF4CAF50))
                            }
                        } else if (track == null) {
                            TextButton(onClick = {
                                isSearching = true
                                currentSearchServiceId = service.id
                                coroutineScope.launch {
                                    try {
                                        searchResults = service.search(title)
                                    } catch(e: Exception) { e.printStackTrace() }
                                }
                            }) {
                                Text("Bind", color = androidx.compose.material3.MaterialTheme.colorScheme.primary)
                            }
                        } else {
                            Column(horizontalAlignment = Alignment.End) {
                                Text(track.title, color = Color.LightGray, style = MaterialTheme.typography.bodySmall)
                                Text("Ch: ${track.lastChapterRead.toInt()}", color = Color.Gray, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
