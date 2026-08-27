// Created by Notch
package com.example.wammy.ui.screens

import androidx.compose.ui.unit.sp
import androidx.compose.foundation.background
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.clickable
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.*
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.Icons
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.filled.Search
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.filled.Close
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.sp
import androidx.compose.material3.*
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.getValue
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.sp
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.sp
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.unit.sp
import com.example.wammy.ui.BrowseSourceViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BrowseSourceScreen(
    sourceId: Long,
    viewModel: BrowseSourceViewModel = viewModel(),
    onBack: () -> Unit,
    onMangaClick: (String) -> Unit
) {
    LaunchedEffect(Unit) {
        viewModel.initSource(sourceId)
    }

    val mangaList by viewModel.mangaList.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val sourceName by viewModel.sourceName.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    
    var isSearching by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = { 
                    if (isSearching) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            modifier = Modifier.fillMaxWidth().height(50.dp),
                            placeholder = { Text("Search ${sourceName}...", color = Color.Gray, fontSize = 14.sp) },
                            textStyle = androidx.compose.ui.text.TextStyle(color = androidx.compose.material3.MaterialTheme.colorScheme.onBackground, fontSize = 14.sp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = androidx.compose.material3.MaterialTheme.colorScheme.surfaceVariant,
                                unfocusedContainerColor = androidx.compose.material3.MaterialTheme.colorScheme.surfaceVariant,
                                focusedBorderColor = Color.Transparent,
                                unfocusedBorderColor = Color.Transparent,
                            ),
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(25.dp),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                            keyboardActions = KeyboardActions(onSearch = { viewModel.searchManga(searchQuery) })
                        )
                    } else {
                        Text(if (sourceName.isNotEmpty()) "Browse $sourceName" else "Browse Source", color = androidx.compose.material3.MaterialTheme.colorScheme.onBackground, fontWeight = FontWeight.Bold) 
                    }
                },
                navigationIcon = {
                    IconButton(onClick = {
                        if (isSearching) {
                            isSearching = false
                            searchQuery = ""
                            viewModel.searchManga("")
                        } else {
                            onBack()
                        }
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = androidx.compose.material3.MaterialTheme.colorScheme.onBackground)
                    }
                },
                actions = {
                    if (!isSearching) {
                        IconButton(onClick = { isSearching = true }) {
                            Icon(Icons.Default.Search, contentDescription = "Search", tint = androidx.compose.material3.MaterialTheme.colorScheme.onBackground)
                        }
                    } else if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { 
                            searchQuery = "" 
                            viewModel.searchManga("")
                        }) {
                            Icon(Icons.Default.Close, contentDescription = "Clear", tint = androidx.compose.material3.MaterialTheme.colorScheme.onBackground)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = androidx.compose.material3.MaterialTheme.colorScheme.primary
                )
            } else if (mangaList.isEmpty()) {
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = errorMessage ?: "No manga found in this source.",
                        color = Color.Gray,
                        modifier = Modifier.padding(horizontal = 32.dp),
                        fontSize = 14.sp
                    )
                    if (errorMessage != null) {
                        Spacer(modifier = Modifier.height(16.dp))
                        androidx.compose.material3.TextButton(onClick = { viewModel.retry() }) {
                            Text("Retry", color = androidx.compose.material3.MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(mangaList) { manga ->
                        MangaGridItem(manga = manga, onMangaClick = onMangaClick)
                    }
                    item { Spacer(modifier = Modifier.height(32.dp)) }
                }
            }
        }
    }
}
