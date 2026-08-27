// Created by Notch
package com.example.wammy.ui.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import com.example.wammy.R
import coil.compose.AsyncImage

@Composable
fun SettingsScreen(onExtensionsClick: () -> Unit = {}, onDataStorageClick: () -> Unit = {}, onAboutClick: () -> Unit = {}, onAppearanceClick: () -> Unit = {}, onStatisticsClick: () -> Unit = {}) {
    val context = LocalContext.current
    var showThemeDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
    ) {
        Text(
            text = "Settings",
            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(start = 8.dp, top = 8.dp, end = 8.dp, bottom = 24.dp)
        )
        
        SettingsRow(
            icon = Icons.Default.Extension,
            title = "Extensions",
            onClick = onExtensionsClick
        )
        SettingsRow(
            icon = Icons.Default.ColorLens,
            title = "Appearance",
            onClick = onAppearanceClick
        )
        SettingsRow(
            icon = Icons.Default.Storage,
            title = "Data & Storage",
            onClick = onDataStorageClick
        )
        SettingsRow(
            icon = Icons.Default.ShowChart,
            title = "Statistics",
            onClick = onStatisticsClick
        )
        SettingsRow(
            icon = Icons.Default.Info,
            title = "About",
            onClick = onAboutClick
        )
        
        if (showThemeDialog) {
            ThemeDialog(onDismiss = { showThemeDialog = false })
        }
    }
}

@Composable
fun SettingsRow(icon: ImageVector, title: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(androidx.compose.foundation.shape.RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 16.dp, horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = title,
            tint = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground
        )
    }
}

@Composable
fun ThemeDialog(onDismiss: () -> Unit) {
    val currentTheme by com.example.wammy.AppContainer.themePreferences.themeMode.collectAsState()
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Choose Theme") },
        text = {
            Column {
                ThemeOptionRow(
                    text = "System Default",
                    selected = currentTheme == com.example.wammy.theme.ThemeMode.SYSTEM,
                    onClick = {
                        com.example.wammy.AppContainer.themePreferences.setThemeMode(com.example.wammy.theme.ThemeMode.SYSTEM)
                        onDismiss()
                    }
                )
                ThemeOptionRow(
                    text = "Light Mode",
                    selected = currentTheme == com.example.wammy.theme.ThemeMode.LIGHT,
                    onClick = {
                        com.example.wammy.AppContainer.themePreferences.setThemeMode(com.example.wammy.theme.ThemeMode.LIGHT)
                        onDismiss()
                    }
                )
                ThemeOptionRow(
                    text = "Dark Mode",
                    selected = currentTheme == com.example.wammy.theme.ThemeMode.DARK,
                    onClick = {
                        com.example.wammy.AppContainer.themePreferences.setThemeMode(com.example.wammy.theme.ThemeMode.DARK)
                        onDismiss()
                    }
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun ThemeOptionRow(text: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp, horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = selected,
            onClick = onClick
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(text = text, style = MaterialTheme.typography.bodyLarge)
    }
}
