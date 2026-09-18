package eu.kanade.presentation.more

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import eu.kanade.presentation.manga.components.MarkdownRender
import eu.kanade.presentation.theme.TachiyomiPreviewTheme
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.ui.more.NewUpdateScreenModel
import org.intellij.markdown.flavours.gfm.GFMFlavourDescriptor

@Composable
fun NewUpdateScreen(
    versionName: String,
    changelogInfo: String,
    stage: NewUpdateScreenModel.Stage,
    downloadProgress: () -> Int,
    onOpenInBrowser: () -> Unit,
    onAcceptUpdate: () -> Unit,
    onRejectUpdate: () -> Unit,
) {
    val backgroundColor = Color(0xFF0C1017)
    val cardColor = Color(0xFF161E2D)
    val primaryBlue = Color(0xFF2B78FF)
    val textMuted = Color(0xFFA0AAB6)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor)
    ) {
        // Top Gradient background
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(350.dp)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color(0xFF1B2A47), backgroundColor)
                    )
                )
        )

        // Scrollable content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(100.dp))

            // App Icon
            Image(
                painter = painterResource(id = R.drawable.wammy_logo),
                contentDescription = null,
                modifier = Modifier
                    .size(100.dp)
                    .clip(RoundedCornerShape(24.dp)),
                contentScale = ContentScale.Crop
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Wammy",
                color = Color.White,
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = "New update available",
                color = textMuted,
                fontSize = 18.sp,
                modifier = Modifier.padding(top = 4.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Better reading. Smoother experience.\nSame Wammy you love.",
                color = Color(0xFF788596),
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                lineHeight = 20.sp
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Changelog Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = cardColor)
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Text(
                        text = "Version $versionName",
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Spacer(modifier = Modifier.height(24.dp))

                    Text(
                        text = "What's New",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Markdown rendering for actual dynamic changelog
                    CompositionLocalProvider(LocalContentColor provides Color(0xFFC0C9D4)) {
                        MarkdownRender(
                            content = changelogInfo,
                            flavour = remember { GFMFlavourDescriptor() }
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    TextButton(
                        onClick = onOpenInBrowser,
                        modifier = Modifier.padding(top = 8.dp)
                    ) {
                        Text(text = "Open in Browser", color = primaryBlue)
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.OpenInNew,
                            contentDescription = null,
                            tint = primaryBlue,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = textMuted,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Thanks for being with Wammy!",
                            color = textMuted,
                            fontSize = 12.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(180.dp)) // padding for bottom bar
        }

        // Close button (Placed after column so it sits on top)
        IconButton(
            onClick = onRejectUpdate,
            modifier = Modifier
                .padding(top = 48.dp, start = 16.dp)
        ) {
            Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = Color.White)
        }

        // Bottom Bar (Fixed)
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, backgroundColor),
                        startY = 0f
                    )
                )
                .padding(24.dp)
                .padding(bottom = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val buttonText = when (stage) {
                NewUpdateScreenModel.Stage.Available -> "Update Now"
                NewUpdateScreenModel.Stage.Downloading -> "Downloading... ${downloadProgress()}%"
                NewUpdateScreenModel.Stage.Downloaded -> "Install Update"
                NewUpdateScreenModel.Stage.Failed -> "Retry Update"
            }
            val isEnabled = stage != NewUpdateScreenModel.Stage.Downloading

            Button(
                onClick = onAcceptUpdate,
                enabled = isEnabled,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = primaryBlue,
                    disabledContainerColor = Color(0xFF1E3A6B)
                ),
                shape = RoundedCornerShape(28.dp)
            ) {
                if (stage == NewUpdateScreenModel.Stage.Available) {
                    Icon(imageVector = Icons.Default.Download, contentDescription = null, tint = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text(text = buttonText, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(16.dp))

            TextButton(onClick = onRejectUpdate) {
                Text(text = "Later", color = textMuted, fontSize = 16.sp)
            }
        }
    }
}

@PreviewLightDark
@Composable
private fun NewUpdateScreenPreview() {
    TachiyomiPreviewTheme {
        NewUpdateScreen(
            versionName = "v0.99.9",
            changelogInfo = """
                ## Yay
                Foobar

                ### More info
                - Hello
                - World
            """.trimIndent(),
            stage = NewUpdateScreenModel.Stage.Available,
            downloadProgress = { 0 },
            onOpenInBrowser = {},
            onAcceptUpdate = {},
            onRejectUpdate = {},
        )
    }
}
