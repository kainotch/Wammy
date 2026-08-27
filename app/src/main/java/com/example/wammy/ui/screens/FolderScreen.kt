// Created by Notch
package com.example.wammy.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.filled.Edit
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.clickable
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.filled.Close

import com.example.wammy.ui.FolderViewModel
import androidx.lifecycle.viewmodel.compose.viewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FolderScreen(
    folderId: Long,
    viewModel: FolderViewModel = viewModel(factory = FolderViewModel.Factory(folderId)),
    onBack: () -> Unit,
    onMangaClick: (String, Boolean) -> Unit
) {
    
    val folder by viewModel.folder.collectAsState()
    val mangas by viewModel.mangas.collectAsState()
    var showEditDialog by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
    
    var editSelectedImageUri by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf<android.net.Uri?>(null) }
    val editPhotoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri -> if (uri != null) editSelectedImageUri = uri }
    )
    
    // Update when folder changes
    LaunchedEffect(folder) {
        if (folder != null) {
            editSelectedImageUri = folder?.coverImageUri?.let { android.net.Uri.parse(it) }
        }
    }

    val context = androidx.compose.ui.platform.LocalContext.current


    Column(modifier = Modifier.fillMaxSize().background(androidx.compose.ui.graphics.Color.Transparent)) {

    if (showEditDialog && folder != null) {
        var folderName by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(folder!!.name) }
        var isPinned by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(folder!!.isPinned) }
        
        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            containerColor = androidx.compose.material3.MaterialTheme.colorScheme.surfaceVariant,
            title = { Text("Edit BookPlace", color = androidx.compose.material3.MaterialTheme.colorScheme.onBackground) },
            text = {
                Column {
                    OutlinedTextField(
                        value = folderName,
                        onValueChange = { folderName = it },
                        label = { Text("BookPlace Name", color = Color.Gray) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Pin to Top", color = androidx.compose.material3.MaterialTheme.colorScheme.onBackground, modifier = Modifier.weight(1f))
                        Switch(checked = isPinned, onCheckedChange = { isPinned = it })
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Text("Cover Image (Optional)", color = androidx.compose.material3.MaterialTheme.colorScheme.onBackground)
                    Spacer(modifier = Modifier.height(8.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp)
                            .background(Color(0xFF2E2E2E), RoundedCornerShape(8.dp))
                            .clickable {
                                editPhotoLauncher.launch(
                                    androidx.activity.result.PickVisualMediaRequest(
                                        ActivityResultContracts.PickVisualMedia.ImageOnly
                                    )
                                )
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        if (editSelectedImageUri != null) {
                            AsyncImage(
                                model = editSelectedImageUri,
                                contentDescription = "Selected Cover",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Text("Tap to Pick Image", color = Color.Gray)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (folderName.isNotBlank()) {
                            viewModel.updateFolder(context, folderName, editSelectedImageUri?.toString(), isPinned)
                            showEditDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = androidx.compose.material3.MaterialTheme.colorScheme.primary, contentColor = androidx.compose.material3.MaterialTheme.colorScheme.onPrimary)
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditDialog = false }) {
                    Text("Cancel", color = Color.Gray)
                }
            }
        )
    }

        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp).statusBarsPadding(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = androidx.compose.material3.MaterialTheme.colorScheme.onBackground)
            }
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = folder?.name ?: "Loading...",
                color = androidx.compose.material3.MaterialTheme.colorScheme.onBackground,
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.weight(1f)
            )
            if (folder != null) {
                IconButton(onClick = { showEditDialog = true }) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit BookPlace", tint = androidx.compose.material3.MaterialTheme.colorScheme.onBackground)
                }
            }
        }

        var mangaToRemove by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf<com.example.wammy.data.local.MangaEntity?>(null) }

        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp),
            contentPadding = PaddingValues(bottom = 100.dp)
        ) {
            items(mangas) { manga ->
                LibraryItem(
                    manga = manga,
                    onClick = { onMangaClick(manga.sourceUrl, false) },
                    onLongClick = { mangaToRemove = manga }
                )
            }
        }

        if (mangaToRemove != null) {
            val m = mangaToRemove!!
            ModalBottomSheet(
                onDismissRequest = { mangaToRemove = null },
                containerColor = androidx.compose.material3.MaterialTheme.colorScheme.surfaceVariant
            ) {
                Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                    Text(
                        m.titleRomaji,
                        color = androidx.compose.material3.MaterialTheme.colorScheme.onBackground,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                viewModel.removeMangaFromFolder(m)
                                mangaToRemove = null
                            }
                            .padding(vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            androidx.compose.material.icons.Icons.Default.Close,
                            contentDescription = null,
                            tint = Color(0xFFFF6B6B),
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Remove from Folder", color = Color(0xFFFF6B6B), fontSize = MaterialTheme.typography.bodyLarge.fontSize)
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}
