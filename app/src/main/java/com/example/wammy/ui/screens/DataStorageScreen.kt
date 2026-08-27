// Created by Notch
package com.example.wammy.ui.screens

import android.content.Intent
import androidx.compose.ui.unit.sp
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.wammy.AppContainer
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DataStorageScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var isLoading by remember { mutableStateOf(false) }

    var selectedBackup by remember { mutableStateOf<com.example.wammy.data.backup.models.Backup?>(null) }
    var selectedBackupUri by remember { mutableStateOf<android.net.Uri?>(null) }
    var selectedBackupName by remember { mutableStateOf("") }
    
    var restoreLibrary by remember { mutableStateOf(true) }
    var restoreCategories by remember { mutableStateOf(true) }

    val createBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/octet-stream")
    ) { uri ->
        if (uri != null) {
            isLoading = true
            coroutineScope.launch {
                val success = AppContainer.backupManager.createBackup(uri)
                isLoading = false
                if (success) {
                    Toast.makeText(context, "Backup created successfully!", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "Failed to create backup.", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    val restoreBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            isLoading = true
            coroutineScope.launch {
                val backup = AppContainer.restoreManager.parseBackup(uri)
                isLoading = false
                if (backup != null) {
                    selectedBackup = backup
                    selectedBackupUri = uri
                    // Extract filename from URI
                    val cursor = context.contentResolver.query(uri, null, null, null, null)
                    selectedBackupName = cursor?.use {
                        if (it.moveToFirst()) {
                            val index = it.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                            if (index != -1) it.getString(index) else "Unknown file"
                        } else "Unknown file"
                    } ?: "Unknown file"
                } else {
                    Toast.makeText(context, "Failed to read backup file.", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    if (selectedBackup != null) {
        AlertDialog(
            onDismissRequest = { selectedBackup = null; selectedBackupUri = null },
            title = { Text("Restore: $selectedBackupName", fontSize = 16.sp) },
            text = {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable { restoreLibrary = !restoreLibrary },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(checked = restoreLibrary, onCheckedChange = { restoreLibrary = it })
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Library (${selectedBackup!!.backupManga.size})", color = androidx.compose.material3.MaterialTheme.colorScheme.onSurface)
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable { restoreCategories = !restoreCategories },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(checked = restoreCategories, onCheckedChange = { restoreCategories = it })
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("BookPlaces (${selectedBackup!!.backupCategories.size})", color = androidx.compose.material3.MaterialTheme.colorScheme.onSurface)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val backup = selectedBackup!!
                    val uri = selectedBackupUri!!
                    selectedBackup = null
                    selectedBackupUri = null
                    isLoading = true
                    coroutineScope.launch {
                        val options = com.example.wammy.data.backup.RestoreOptions(
                            library = restoreLibrary,
                            categories = restoreCategories
                        )
                        val success = AppContainer.restoreManager.restoreBackup(backup, options)
                        isLoading = false
                        if (success) {
                            Toast.makeText(context, "Backup restored successfully!", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "Failed to restore backup.", Toast.LENGTH_SHORT).show()
                        }
                    }
                }) {
                    Text("Restore", color = androidx.compose.material3.MaterialTheme.colorScheme.primary)
                }
            },
            dismissButton = {
                TextButton(onClick = { selectedBackup = null; selectedBackupUri = null }) {
                    Text("Cancel", color = Color.Gray)
                }
            },
            containerColor = androidx.compose.material3.MaterialTheme.colorScheme.surfaceVariant
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Data and Storage", color = androidx.compose.material3.MaterialTheme.colorScheme.onBackground) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = androidx.compose.material3.MaterialTheme.colorScheme.onBackground)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = androidx.compose.ui.graphics.Color.Transparent)
            )
        },
        containerColor = androidx.compose.ui.graphics.Color.Transparent
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues).fillMaxSize()) {
            Column(modifier = Modifier.fillMaxSize()) {
                Text(
                    text = "Backup",
                    color = androidx.compose.material3.MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )

                StorageItemRow(
                    icon = Icons.Default.Save,
                    title = "Create backup",
                    subtitle = "Can be used to restore current library",
                    onClick = { createBackupLauncher.launch("wammy_backup_${System.currentTimeMillis()}.tachibk") }
                )

                StorageItemRow(
                    icon = Icons.Default.Restore,
                    title = "Restore backup",
                    subtitle = "Restore library from backup file",
                    onClick = { restoreBackupLauncher.launch(arrayOf("application/octet-stream", "application/json", "*/*")) }
                )

                HorizontalDivider(color = Color.DarkGray, modifier = Modifier.padding(vertical = 8.dp))

                Text(
                    text = "Storage",
                    color = androidx.compose.material3.MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )

                StorageItemRow(
                    icon = Icons.Default.Folder,
                    title = "Storage location",
                    subtitle = "Not implemented yet (coming soon)",
                    onClick = { Toast.makeText(context, "Coming soon!", Toast.LENGTH_SHORT).show() }
                )

                StorageItemRow(
                    icon = Icons.Default.Delete,
                    title = "Clear chapter cache",
                    subtitle = "Delete temporary downloaded pages",
                    onClick = { 
                        coroutineScope.launch {
                            AppContainer.cacheManager.clearChapterCache()
                            Toast.makeText(context, "Chapter cache cleared!", Toast.LENGTH_SHORT).show()
                        }
                    }
                )
            }

            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.6f)),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = androidx.compose.material3.MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}

@Composable
fun StorageItemRow(icon: ImageVector, title: String, subtitle: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(24.dp))
        Spacer(modifier = Modifier.width(20.dp))
        Column {
            Text(title, color = androidx.compose.material3.MaterialTheme.colorScheme.onBackground, style = MaterialTheme.typography.bodyLarge)
            Text(subtitle, color = Color.Gray, style = MaterialTheme.typography.bodyMedium)
        }
    }
}
