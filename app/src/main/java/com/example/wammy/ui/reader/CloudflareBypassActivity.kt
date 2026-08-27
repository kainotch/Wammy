package com.example.wammy.ui.reader

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.foundation.isSystemInDarkTheme
import com.example.wammy.AppContainer
import com.example.wammy.theme.ThemeMode
import com.example.wammy.theme.AppTheme

import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.example.wammy.theme.WammyTheme

class CloudflareBypassActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3Api::class)
    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val url = intent.getStringExtra("URL") ?: ""
        
        setContent {
            val themeMode by com.example.wammy.AppContainer.themePreferences.themeMode.collectAsState()
            val appTheme by com.example.wammy.AppContainer.themePreferences.appTheme.collectAsState()
            val amoled by com.example.wammy.AppContainer.themePreferences.amoled.collectAsState()
            val darkTheme = when (themeMode) {
                com.example.wammy.theme.ThemeMode.LIGHT -> false
                com.example.wammy.theme.ThemeMode.DARK -> true
                com.example.wammy.theme.ThemeMode.SYSTEM -> androidx.compose.foundation.isSystemInDarkTheme()
            }
            WammyTheme(appTheme = appTheme, amoled = amoled, darkTheme = darkTheme) {
                Scaffold(
                    topBar = {
                        TopAppBar(
                            title = { Text("Cloudflare Bypass") },
                            navigationIcon = {
                                IconButton(onClick = { finish() }) {
                                    Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                                }
                            }
                        )
                    }
                ) { padding ->
                    AndroidView(
                        modifier = Modifier.fillMaxSize().padding(padding),
                        factory = { context ->
                            WebView(context).apply {
                                settings.javaScriptEnabled = true
                                settings.domStorageEnabled = true
                                settings.userAgentString = "Mozilla/5.0 (Linux; Android 13; Pixel 7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/116.0.0.0 Mobile Safari/537.36"
                                
                                webViewClient = object : WebViewClient() {
                                    override fun onPageFinished(view: WebView?, url: String?) {
                                        super.onPageFinished(view, url)
                                        CookieManager.getInstance().flush()
                                    }
                                }
                                webChromeClient = WebChromeClient()
                                loadUrl(url)
                            }
                        }
                    )
                }
            }
        }
    }
}
