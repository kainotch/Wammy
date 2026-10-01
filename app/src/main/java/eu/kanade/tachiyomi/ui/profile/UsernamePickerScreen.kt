package eu.kanade.tachiyomi.ui.profile

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.google.firebase.auth.FirebaseAuth
import eu.kanade.tachiyomi.data.sync.CloudUser
import eu.kanade.tachiyomi.data.sync.FirestoreUserRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * The Gatekeeper Screen.
 * Shown after first-time Google sign-in when no Firestore profile exists.
 * Forces the user to choose a unique username before they can proceed.
 */
class UsernamePickerScreen : Screen {

    @Composable
    override fun Content() {
        val context = LocalContext.current
        val navigator = LocalNavigator.currentOrThrow
        val scope = rememberCoroutineScope()
        val repo = remember { FirestoreUserRepository() }
        val currentUser = FirebaseAuth.getInstance().currentUser

        var username by remember { mutableStateOf("") }
        var validationError by remember { mutableStateOf<String?>(null) }
        var isAvailable by remember { mutableStateOf<Boolean?>(null) }
        var isChecking by remember { mutableStateOf(false) }
        var isCreating by remember { mutableStateOf(false) }
        var checkJob by remember { mutableStateOf<Job?>(null) }

        // Debounced availability check
        fun checkAvailability(name: String) {
            checkJob?.cancel()
            isAvailable = null
            isChecking = false

            // First validate locally
            val error = CloudUser.validateUsername(name)
            validationError = error
            if (error != null) return

            isChecking = true
            checkJob = scope.launch {
                delay(300) // Debounce 300ms
                val available = repo.isUsernameAvailable(name)
                isAvailable = available
                isChecking = false
                if (!available) {
                    validationError = "Username is already taken"
                }
            }
        }

        Scaffold { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "Choose your username",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "This is how other users will find you.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(32.dp))

                OutlinedTextField(
                    value = username,
                    onValueChange = { newValue ->
                        // Force lowercase, strip invalid chars as they type
                        val cleaned = newValue.lowercase().filter { it.isLetterOrDigit() || it == '_' }
                        username = cleaned
                        if (cleaned.length >= 3) {
                            checkAvailability(cleaned)
                        } else {
                            isAvailable = null
                            validationError = if (cleaned.isNotEmpty()) "Username must be at least 3 characters" else null
                        }
                    },
                    label = { Text("Username") },
                    placeholder = { Text("e.g. manga_fan") },
                    singleLine = true,
                    isError = validationError != null,
                    trailingIcon = {
                        when {
                            isChecking -> CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp
                            )
                            isAvailable == true -> Icon(
                                Icons.Default.Check,
                                contentDescription = "Available",
                                tint = Color(0xFF4CAF50)
                            )
                            validationError != null -> Icon(
                                Icons.Default.Close,
                                contentDescription = "Error",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    },
                    supportingText = {
                        when {
                            validationError != null -> Text(
                                validationError!!,
                                color = MaterialTheme.colorScheme.error
                            )
                            isAvailable == true -> Text(
                                "Username is available!",
                                color = Color(0xFF4CAF50)
                            )
                            else -> Text("Lowercase letters, numbers, and underscores only")
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = {
                        if (currentUser == null) return@Button
                        isCreating = true
                        scope.launch {
                            val result = repo.createUser(currentUser.uid, username)
                            isCreating = false
                            result.fold(
                                onSuccess = {
                                    Toast.makeText(context, "Welcome, @$username!", Toast.LENGTH_SHORT).show()
                                    navigator.pop()
                                },
                                onFailure = { e ->
                                    validationError = e.message ?: "Failed to create profile"
                                    Toast.makeText(context, validationError, Toast.LENGTH_LONG).show()
                                }
                            )
                        }
                    },
                    enabled = isAvailable == true && !isCreating && validationError == null,
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    if (isCreating) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Creating...")
                    } else {
                        Text("Confirm Username", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
