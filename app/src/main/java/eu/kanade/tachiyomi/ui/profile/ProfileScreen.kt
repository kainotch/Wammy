package eu.kanade.tachiyomi.ui.profile

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.PickVisualMediaRequest
import java.io.File
import java.io.FileOutputStream
import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.CustomCredential
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import coil3.compose.AsyncImage
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import eu.kanade.tachiyomi.data.auth.AuthManager
import kotlinx.coroutines.launch
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class ProfileScreen : Screen {

    @Composable
    override fun Content() {
        val context = LocalContext.current
        val navigator = LocalNavigator.currentOrThrow
        val scope = rememberCoroutineScope()
        val authManager: AuthManager = Injekt.get()
        val user by authManager.currentUser.collectAsState()

        val webClientId = "997612260567-i7gkfnks53c0tlh9kvfslmml0bnn0lle.apps.googleusercontent.com"

        if (user != null) {
            val highResPhotoUrl = user?.photoUrl?.toString()?.replace("s96-c", "s800-c")

            Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
                AsyncImage(
                    model = highResPhotoUrl,
                    contentDescription = "Profile Background",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                0.0f to Color.Transparent,
                                0.15f to Color.Black.copy(alpha = 0.6f),
                                0.35f to Color.Black,
                                1.0f to Color.Black
                            )
                        )
                ) {}

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 48.dp, start = 16.dp, end = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    IconButton(
                        onClick = { navigator.pop() },
                        modifier = Modifier.background(Color.Black.copy(alpha = 0.5f), CircleShape)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                    }
                    IconButton(
                        onClick = { },
                        modifier = Modifier.background(Color.Black.copy(alpha = 0.5f), CircleShape)
                    ) {
                        Icon(Icons.Default.MoreHoriz, contentDescription = "Options", tint = Color.White)
                    }
                }
                
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = 100.dp)
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Spacer(modifier = Modifier.height(32.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        AsyncImage(
                            model = user?.photoUrl?.toString()?.replace("s96-c", "s192-c"),
                            contentDescription = "Profile Picture",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.size(64.dp).clip(CircleShape)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = user?.displayName ?: "Unknown User",
                                style = MaterialTheme.typography.displaySmall,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Button(
                            onClick = { authManager.signOut(); navigator.pop() },
                            modifier = Modifier.weight(1f).height(50.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black),
                            shape = RoundedCornerShape(25.dp)
                        ) {
                            Text("Sign Out", fontWeight = FontWeight.Bold)
                        }
                        
                        var showEditDialog by remember { mutableStateOf(false) }
                        IconButton(
                            onClick = { showEditDialog = true },
                            modifier = Modifier.size(50.dp).background(Color.White.copy(alpha = 0.2f), CircleShape)
                        ) {
                            Icon(Icons.Default.Settings, contentDescription = "Settings", tint = Color.White)
                        }

                        if (showEditDialog) {
                            var newName by remember { mutableStateOf(user?.displayName ?: "") }
                            var newPhotoUrl by remember { mutableStateOf(user?.photoUrl?.toString() ?: "") }
                            var isUpdating by remember { mutableStateOf(false) }
                            
                            val photoPickerLauncher = rememberLauncherForActivityResult(
                                contract = ActivityResultContracts.PickVisualMedia()
                            ) { uri ->
                                if (uri != null) {
                                    try {
                                        val inputStream = context.contentResolver.openInputStream(uri)
                                        val file = File(context.filesDir, "profile_pic_${System.currentTimeMillis()}.jpg")
                                        val outputStream = FileOutputStream(file)
                                        inputStream?.copyTo(outputStream)
                                        inputStream?.close()
                                        outputStream.close()
                                        newPhotoUrl = "file://${file.absolutePath}"
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "Failed to load image", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                            
                            AlertDialog(
                                onDismissRequest = { if (!isUpdating) showEditDialog = false },
                                title = { Text("Edit Profile") },
                                text = {
                                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                        OutlinedTextField(
                                            value = newName,
                                            onValueChange = { newName = it },
                                            label = { Text("Name") },
                                            singleLine = true
                                        )
                                        
                                        Column {
                                            Text("Profile Picture", style = MaterialTheme.typography.labelMedium)
                                            Spacer(modifier = Modifier.height(8.dp))
                                            OutlinedButton(
                                                onClick = {
                                                    photoPickerLauncher.launch(
                                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                                    )
                                                },
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Text("Choose from Gallery")
                                            }
                                        }
                                    }
                                },
                                confirmButton = {
                                    TextButton(
                                        onClick = {
                                            isUpdating = true
                                            if (newPhotoUrl.startsWith("file://")) {
                                                val fileUri = android.net.Uri.parse(newPhotoUrl)
                                                // Always use "avatar.jpg" so we overwrite their old picture and save storage space
                                                val storageRef = com.google.firebase.storage.FirebaseStorage.getInstance().reference.child("profile_pics/${user?.uid}/avatar.jpg")
                                                
                                                storageRef.putFile(fileUri)
                                                    .addOnSuccessListener {
                                                        storageRef.downloadUrl.addOnSuccessListener { downloadUri ->
                                                            val profileUpdates = com.google.firebase.auth.userProfileChangeRequest {
                                                                displayName = newName
                                                                photoUri = downloadUri
                                                            }
                                                            user?.updateProfile(profileUpdates)?.addOnCompleteListener { task ->
                                                                isUpdating = false
                                                                showEditDialog = false
                                                                if (!task.isSuccessful) {
                                                                    Toast.makeText(context, "Failed to update profile", Toast.LENGTH_SHORT).show()
                                                                }
                                                            }
                                                        }
                                                    }
                                                    .addOnFailureListener {
                                                        isUpdating = false
                                                        Toast.makeText(context, "Failed to upload image", Toast.LENGTH_SHORT).show()
                                                    }
                                            } else {
                                                val profileUpdates = com.google.firebase.auth.userProfileChangeRequest {
                                                    displayName = newName
                                                    if (newPhotoUrl.isNotEmpty()) {
                                                        photoUri = android.net.Uri.parse(newPhotoUrl)
                                                    }
                                                }
                                                user?.updateProfile(profileUpdates)?.addOnCompleteListener { task ->
                                                    isUpdating = false
                                                    showEditDialog = false
                                                    if (!task.isSuccessful) {
                                                        Toast.makeText(context, "Failed to update profile", Toast.LENGTH_SHORT).show()
                                                    }
                                                }
                                            }
                                        },
                                        enabled = !isUpdating
                                    ) {
                                        Text("Save")
                                    }
                                },
                                dismissButton = {
                                    TextButton(
                                        onClick = { showEditDialog = false },
                                        enabled = !isUpdating
                                    ) {
                                        Text("Cancel")
                                    }
                                }
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(48.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp)
                            .height(180.dp)
                            .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(16.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Top 5 Fav Manga\n(Coming Soon)",
                            color = Color.White.copy(alpha = 0.5f),
                            textAlign = TextAlign.Center
                        )
                    }
                    Spacer(modifier = Modifier.height(48.dp))
                }
            }
        } else {
            Scaffold(
                topBar = {
                    @OptIn(ExperimentalMaterial3Api::class)
                    TopAppBar(
                        title = { Text("Profile") },
                        navigationIcon = {
                            IconButton(onClick = { navigator.pop() }) {
                                Icon(Icons.Default.Close, contentDescription = "Back")
                            }
                        }
                    )
                }
            ) { paddingValues ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text("You are not logged in.", style = MaterialTheme.typography.titleLarge)
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = {
                        scope.launch {
                            try {
                                val credentialManager = CredentialManager.create(context)
                                val googleIdOption: GetGoogleIdOption = GetGoogleIdOption.Builder()
                                    .setFilterByAuthorizedAccounts(false)
                                    .setServerClientId(webClientId)
                                    .build()
                                val request: GetCredentialRequest = GetCredentialRequest.Builder()
                                    .addCredentialOption(googleIdOption)
                                    .build()
                                val result = credentialManager.getCredential(context, request)
                                val credential = result.credential
                                if (credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                                    val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                                    val firebaseCredential = GoogleAuthProvider.getCredential(googleIdTokenCredential.idToken, null)
                                    FirebaseAuth.getInstance().signInWithCredential(firebaseCredential).addOnCompleteListener { task ->
                                        if (!task.isSuccessful) {
                                            android.util.Log.e("ProfileScreen", "Auth Failed", task.exception)
                                            Toast.makeText(context, "Firebase Auth Failed: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                                        }
                                    }
                                }
                            } catch (e: Exception) {
                                android.util.Log.e("ProfileScreen", "Sign in failed", e)
                                Toast.makeText(context, "Sign in failed: ${e.message}", Toast.LENGTH_LONG).show()
                            }
                        }
                    }) {
                        Text("Sign in with Google")
                    }
                }
            }
        }
    }

    @Composable
    private fun StatItem(value: String, label: String) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = value, style = MaterialTheme.typography.titleLarge, color = Color.White, fontWeight = FontWeight.Bold)
            Text(text = label, style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.7f))
        }
    }
}
