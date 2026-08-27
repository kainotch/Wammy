// Created by Notch
package com.example.wammy.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.material.icons.filled.Close
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.wammy.ui.HistoryViewModel
import com.example.wammy.data.local.HistoryEntity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    viewModel: HistoryViewModel = viewModel(),
    onMangaClick: (String) -> Unit,
    listState: LazyListState = rememberLazyListState()
) {
    val history by viewModel.history.collectAsState()

    Scaffold(
        containerColor = androidx.compose.ui.graphics.Color.Transparent,
        topBar = {
            var isSearching by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
            var searchQuery by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf("") }
            
            if (isSearching) {
                TopAppBar(
                    title = {
                        TextField(
                            value = searchQuery,
                            onValueChange = { 
                                searchQuery = it
                                viewModel.setSearchQuery(it)
                            },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("Search history...") },
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent
                            ),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                            keyboardActions = KeyboardActions(onSearch = { /* hide keyboard */ })
                        )
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = androidx.compose.ui.graphics.Color.Transparent),
                    windowInsets = WindowInsets(0.dp),
                    actions = {
                        IconButton(onClick = {
                            isSearching = false
                            searchQuery = ""
                            viewModel.setSearchQuery("")
                        }) {
                            Icon(Icons.Default.Close, contentDescription = "Close Search", tint = Color.LightGray)
                        }
                    }
                )
            } else {
                TopAppBar(
                    title = { Text("History", color = androidx.compose.material3.MaterialTheme.colorScheme.onBackground, fontWeight = FontWeight.Bold) },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = androidx.compose.ui.graphics.Color.Transparent),
                    windowInsets = WindowInsets(0.dp),
                    actions = {
                        IconButton(onClick = { isSearching = true }) {
                            Icon(Icons.Default.Search, contentDescription = "Search", tint = Color.LightGray)
                        }
                        IconButton(onClick = { viewModel.clearAllHistory() }) {
                            Icon(Icons.Default.DeleteOutline, contentDescription = "Clear All", tint = Color.LightGray)
                        }
                    }
                )
            }
        }
    ) { padding ->
        if (history.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("No history yet.", color = Color.Gray)
            }
        } else {
            val groupedHistory = history.groupBy { viewModel.formatRelativeDate(it.lastReadTimestamp) }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                groupedHistory.forEach { (dateHeader, items) ->
                    item {
                        Text(
                            text = dateHeader,
                            color = Color.LightGray,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold, fontSize = 15.sp,
                            modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
                        )
                    }
                    items(items) { item ->
                        HistoryItemRow(
                            item = item,
                            timeStr = viewModel.formatTime(item.lastReadTimestamp),
                            onClick = { onMangaClick(item.mangaSourceUrl) },
                            onDelete = { viewModel.deleteHistory(item.id) }
                        )
                    }
                }
                item { Spacer(modifier = Modifier.height(32.dp)) }
            }
        }
    }
}

@Composable
fun HistoryItemRow(
    item: HistoryEntity,
    timeStr: String,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp)
            .padding(vertical = 4.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF16161A))
            .clickable(onClick = onClick)
    ) {
        // Blurred Background
        AsyncImage(
            model = item.mangaCoverUrl,
            contentDescription = null,
            modifier = Modifier.fillMaxSize().alpha(0.6f),
            contentScale = ContentScale.Crop
        )
        // Gradient Overlay blending into black
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(androidx.compose.ui.graphics.Brush.horizontalGradient(
                    colorStops = arrayOf(
                        0.0f to Color.Black.copy(alpha = 0.1f),
                        0.3f to Color.Black.copy(alpha = 0.6f),
                        0.6f to Color.Black,
                        1.0f to Color.Black
                    )
                ))
        )
        
        Row(
            modifier = Modifier.fillMaxSize().padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Poster
            AsyncImage(
                model = item.mangaCoverUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(72.dp, 102.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color.DarkGray)
            )
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = item.mangaTitle,
                    color = Color.White,
                    fontWeight = FontWeight.Bold, fontSize = 15.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                val progressText = if (item.totalPages > 0) " (Page ${item.lastPageRead}/${item.totalPages})" else ""
                Text(
                    text = "${item.chapterName}$progressText - $timeStr",
                    color = Color(0xFF90CAF9),
                    fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis
                )
            }
            
            IconButton(onClick = { /* Add to library */ }) {
                Icon(Icons.Default.FavoriteBorder, contentDescription = "Favorite", tint = Color.LightGray)
            }
            
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", tint = Color.LightGray)
            }
        }
    }
}
