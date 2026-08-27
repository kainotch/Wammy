// Created by Notch
package com.example.wammy.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(onBack: () -> Unit) {
    val context = LocalContext.current

    val notchInstaImg = "https://instagram.fdel25-1.fna.fbcdn.net/v/t51.82787-19/767819389_17902075119503352_4539452439841738117_n.jpg?stp=dst-jpg_s150x150_tt6&_nc_cat=106&ccb=7-5&_nc_sid=f7ccc5&efg=eyJ2ZW5jb2RlX3RhZyI6InByb2ZpbGVfcGljLnd3dy4xMDgwLkMzIn0%3D&_nc_ohc=IetDvBLlerwQ7kNvwEn_k4c&_nc_oc=Adpgsl5pt3PZVP2UPaUEiMfYhXoQmBSE2NvjOkpPtIIrRjiOUxuagROUkFShw7gd2dhh5wocEaAVcGSSp1WuI71x&_nc_zt=24&_nc_ht=instagram.fdel25-1.fna&_nc_gid=qIzLfNV141d0iZCFhYtlZQ&_nc_ss=7b6a8&oh=00_AQFkaNm-8xYfpIg1rcby49OTxudhJbALPyaB-bLr6PTeEw&oe=6A8E1CF3"
    val partnerInstaImg = "https://instagram.fdel25-4.fna.fbcdn.net/v/t51.82787-19/775154221_18019485719879287_5363667965202398240_n.jpg?stp=dst-jpg_s150x150_tt6&efg=eyJ2ZW5jb2RlX3RhZyI6InByb2ZpbGVfcGljLmRqYW5nby4xMDgwLmMyIn0&_nc_ht=instagram.fdel25-4.fna.fbcdn.net&_nc_cat=107&_nc_oc=Q6cZ2gGrdQLIXZD92zH61N9lYY9bbqfCsjAIZs7PmVHPvmU2jkur8PKFd7ZUeuOFOj81Ezke-n0q53XAUwDm-0YC3xf4&_nc_ohc=0rgEhzavcl8Q7kNvwGIoC2w&_nc_gid=hqyR0HKzSS_3fMluU4chgA&edm=AP4sbd4BAAAA&ccb=7-5&oh=00_AQFYBNdrrtoqIUCzYvruZ4uPZHGNFBngOg8Ec5kDalCHDA&oe=6A8E1E19&_nc_sid=7a9f4b"

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("About", color = androidx.compose.material3.MaterialTheme.colorScheme.onBackground) },
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Text(
                text = "Developer",
                color = androidx.compose.material3.MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
            
            ProfileRow(
                imageUrl = "https://github.com/kainotch.png",
                name = "Notch",
                subtitle = "GitHub",
                onClick = { 
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/kainotch")))
                }
            )
            
            DynamicInstagramProfileRow(
                username = "kainotch",
                name = "Notch",
                fallbackUrl = notchInstaImg,
                onClick = { 
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.instagram.com/kainotch")))
                }
            )

            HorizontalDivider(color = Color.DarkGray, modifier = Modifier.padding(vertical = 12.dp))

            Text(
                text = "Partner",
                color = androidx.compose.material3.MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )

            DynamicInstagramProfileRow(
                username = "xo._kiwikaffine",
                name = "1unun",
                fallbackUrl = partnerInstaImg,
                onClick = { 
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.instagram.com/xo._kiwikaffine/")))
                }
            )

            HorizontalDivider(color = Color.DarkGray, modifier = Modifier.padding(vertical = 12.dp))

            Text(
                text = "About Wammy",
                color = androidx.compose.material3.MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
            
            Text(
                text = "Wammy is a modern, sleek manga and comic reader built natively for Android using Jetpack Compose. It allows you to seamlessly discover, organize, and read manga by integrating directly with thousands of community-built Keiyoushi (Tachiyomi) extensions. With features like full library management, cross-device tracking (AniList/MyAnimeList), and dynamic light/dark modes, it provides a highly customizable and premium reading experience.",
                color = androidx.compose.material3.MaterialTheme.colorScheme.onBackground,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }
    }
}

@Composable
fun DynamicInstagramProfileRow(username: String, name: String, fallbackUrl: String, onClick: () -> Unit) {
    val imageUrl = androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(fallbackUrl) }
    
    androidx.compose.runtime.LaunchedEffect(username) {
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val doc = org.jsoup.Jsoup.connect("https://www.instagram.com/$username/").get()
                val url = doc.select("meta[property=og:image]").attr("content")
                if (url.isNotEmpty()) {
                    imageUrl.value = url.replace("&amp;", "&")
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
    
    ProfileRow(
        imageUrl = imageUrl.value,
        name = name,
        subtitle = "Instagram",
        onClick = onClick
    )
}

@Composable
fun ProfileRow(imageUrl: String, name: String, subtitle: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = imageUrl,
            contentDescription = "$name Avatar",
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(Color.DarkGray)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(name, color = androidx.compose.material3.MaterialTheme.colorScheme.onBackground, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(subtitle, color = Color.Gray, style = MaterialTheme.typography.bodyMedium)
        }
    }
}
