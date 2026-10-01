package eu.kanade.tachiyomi.ui.profile

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.IntentSenderRequest
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import eu.kanade.tachiyomi.data.sync.AuthorizationResult
import eu.kanade.tachiyomi.data.sync.DriveApiHelper
import eu.kanade.tachiyomi.data.sync.DriveSyncManager
import eu.kanade.tachiyomi.data.sync.SyncResult
import eu.kanade.tachiyomi.data.sync.DriveSyncWorker
import kotlinx.coroutines.launch
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class CloudSyncScreen : Screen {
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val context = LocalContext.current
        val navigator = LocalNavigator.currentOrThrow
        val scope = rememberCoroutineScope()
        val driveSyncManager: DriveSyncManager = Injekt.get()
        val driveApiHelper: DriveApiHelper = Injekt.get()

        var isSyncing by remember { mutableStateOf(false) }
        var lastSyncStatus by remember { mutableStateOf<String?>(null) }
        var pendingAction by remember { mutableStateOf<String?>(null) }

        val consentLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.StartIntentSenderForResult()
        ) { result ->
            if (result.resultCode == android.app.Activity.RESULT_OK) {
                scope.launch {
                    isSyncing = true
                    lastSyncStatus = null
                    val syncResult = if (pendingAction == "backup") {
                        driveSyncManager.backup()
                    } else {
                        driveSyncManager.restore()
                    }
                    when (syncResult) {
                        is SyncResult.Success -> {
                            lastSyncStatus = "✓ ${syncResult.message}"
                            Toast.makeText(context, syncResult.message, Toast.LENGTH_SHORT).show()
                        }
                        is SyncResult.Error -> {
                            lastSyncStatus = "✗ ${syncResult.message}"
                            Toast.makeText(context, syncResult.message, Toast.LENGTH_LONG).show()
                        }
                    }
                    isSyncing = false
                    pendingAction = null
                }
            }
        }

        fun launchDriveAction(action: String) {
            scope.launch {
                isSyncing = true
                lastSyncStatus = null
                pendingAction = action

                when (val authResult = driveApiHelper.requestDriveAuthorization()) {
                    is AuthorizationResult.Success -> {
                        val syncResult = if (action == "backup") {
                            driveSyncManager.backup()
                        } else {
                            driveSyncManager.restore()
                        }
                        when (syncResult) {
                            is SyncResult.Success -> {
                                lastSyncStatus = "✓ ${syncResult.message}"
                                Toast.makeText(context, syncResult.message, Toast.LENGTH_SHORT).show()
                            }
                            is SyncResult.Error -> {
                                lastSyncStatus = "✗ ${syncResult.message}"
                                Toast.makeText(context, syncResult.message, Toast.LENGTH_LONG).show()
                            }
                        }
                        isSyncing = false
                    }
                    is AuthorizationResult.NeedsConsent -> {
                        isSyncing = false
                        try {
                            val intentSenderRequest = IntentSenderRequest.Builder(authResult.pendingIntent).build()
                            consentLauncher.launch(intentSenderRequest)
                        } catch (e: Exception) {
                            lastSyncStatus = "✗ Could not open consent screen"
                        }
                    }
                    is AuthorizationResult.Error -> {
                        lastSyncStatus = "✗ ${authResult.message}"
                        isSyncing = false
                    }
                }
            }
        }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Cloud Sync") },
                    navigationIcon = {
                        IconButton(onClick = { navigator.pop() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    }
                )
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 24.dp, vertical = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Backup & restore your library via Google Drive",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(32.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = { launchDriveAction("backup") },
                        enabled = !isSyncing,
                        modifier = Modifier.weight(1f).height(56.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF4285F4),
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        if (isSyncing && pendingAction == "backup") {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text("Backup", fontWeight = FontWeight.Bold)
                        }
                    }

                    Button(
                        onClick = { launchDriveAction("restore") },
                        enabled = !isSyncing,
                        modifier = Modifier.weight(1f).height(56.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF34A853),
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        if (isSyncing && pendingAction == "restore") {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text("Restore", fontWeight = FontWeight.Bold)
                        }
                    }
                }

                if (lastSyncStatus != null) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = lastSyncStatus!!,
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (lastSyncStatus!!.startsWith("✓")) Color(0xFF4CAF50) else Color(0xFFE53935),
                        textAlign = TextAlign.Center
                    )
                }

                // Last synced timestamp
                val syncPrefs = context.getSharedPreferences(
                    DriveSyncWorker.PREFS_NAME,
                    android.content.Context.MODE_PRIVATE
                )
                val lastSyncTime = syncPrefs.getLong(DriveSyncWorker.KEY_LAST_SYNC_TIME, 0L)
                if (lastSyncTime > 0L) {
                    Spacer(modifier = Modifier.height(16.dp))
                    val elapsed = System.currentTimeMillis() - lastSyncTime
                    val timeAgo = when {
                        elapsed < 60_000 -> "just now"
                        elapsed < 3_600_000 -> "${elapsed / 60_000} min ago"
                        elapsed < 86_400_000 -> "${elapsed / 3_600_000}h ago"
                        else -> "${elapsed / 86_400_000}d ago"
                    }
                    Text(
                        text = "Last synced: $timeAgo",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        textAlign = TextAlign.Center
                    )
                }

                // Auto-sync toggle
                Spacer(modifier = Modifier.height(32.dp))
                var autoSyncEnabled by remember {
                    mutableStateOf(
                        syncPrefs.getBoolean(DriveSyncWorker.KEY_AUTO_SYNC_ENABLED, true)
                    )
                }
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Auto-sync",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Switch(
                            checked = autoSyncEnabled,
                            onCheckedChange = { enabled ->
                                autoSyncEnabled = enabled
                                syncPrefs.edit().putBoolean(
                                    DriveSyncWorker.KEY_AUTO_SYNC_ENABLED,
                                    enabled
                                ).apply()
                            }
                        )
                    }
                }
            }
        }
    }
}
