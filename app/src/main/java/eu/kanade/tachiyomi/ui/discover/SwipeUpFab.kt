package eu.kanade.tachiyomi.ui.discover

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import tachiyomi.domain.library.service.LibraryPreferences

@Composable
fun SwipeUpFab(
    onSlideUpTriggered: () -> Unit
) {
    val libraryPreferences = remember { Injekt.get<LibraryPreferences>() }
    val initialHasShown = remember { libraryPreferences.hasShownSwipeUpFabTutorial.get() }
    var hasShownTutorial by remember { mutableStateOf(initialHasShown) }
    var showTutorialOverlay by remember { mutableStateOf(false) }
    var dragOffset by remember { mutableStateOf(0f) }
    val maxDragPx = with(LocalDensity.current) { -72.dp.toPx() }
    val haptic = LocalHapticFeedback.current
    val animatedOffset by animateFloatAsState(targetValue = dragOffset)

    if (showTutorialOverlay) {
        Dialog(
            onDismissRequest = {
                showTutorialOverlay = false
                hasShownTutorial = true
                libraryPreferences.hasShownSwipeUpFabTutorial.set(true)
            },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            val infiniteTransition = rememberInfiniteTransition()
            val fingerY by infiniteTransition.animateFloat(
                initialValue = 0f,
                targetValue = -120f,
                animationSpec = infiniteRepeatable(
                    animation = tween(1500, delayMillis = 500, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Restart
                )
            )
            val fingerAlpha by infiniteTransition.animateFloat(
                initialValue = 1f,
                targetValue = 0f,
                animationSpec = infiniteRepeatable(
                    animation = tween(1500, delayMillis = 500, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Restart
                )
            )

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.7f))
                    .clickable {
                        showTutorialOverlay = false
                        hasShownTutorial = true
                        libraryPreferences.hasShownSwipeUpFabTutorial.set(true)
                    }
            ) {
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Press and hold, then slide up",
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(onClick = {
                        showTutorialOverlay = false
                        hasShownTutorial = true
                        libraryPreferences.hasShownSwipeUpFabTutorial.set(true)
                    }) {
                        Text("Got it")
                    }
                }

                // Spotlight Fake FAB in bottom right
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(16.dp)
                        .padding(bottom = 80.dp) // Approximate padding above bottom nav
                ) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer)
                    ) {
                        Icon(
                            Icons.Filled.KeyboardArrowUp,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }

                    Icon(
                        Icons.Filled.TouchApp,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = fingerAlpha),
                        modifier = Modifier
                            .size(48.dp)
                            .offset { IntOffset(0, fingerY.toInt()) }
                            .align(Alignment.Center)
                    )
                }
            }
        }
    }

    Box(
        modifier = Modifier
            .offset { IntOffset(0, animatedOffset.toInt()) }
            .pointerInput(hasShownTutorial) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    
                    if (!hasShownTutorial) {
                        do {
                            val event = awaitPointerEvent()
                        } while (event.changes.any { it.pressed })
                        
                        showTutorialOverlay = true
                        return@awaitEachGesture
                    }
                    
                    var triggered = false
                    
                    do {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull()
                        if (change != null && change.pressed) {
                            val dy = change.position.y - down.position.y
                            if (dy < 0) {
                                dragOffset = dy
                                if (dragOffset <= maxDragPx && !triggered) {
                                    triggered = true
                                    dragOffset = 0f
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    onSlideUpTriggered()
                                }
                            }
                        }
                    } while (event.changes.any { it.pressed })
                    
                    if (!triggered) {
                        dragOffset = 0f
                    }
                }
            }
    ) {
        FloatingActionButton(
            onClick = {
                if (!hasShownTutorial) {
                    showTutorialOverlay = true
                }
            },
            modifier = Modifier.size(56.dp)
        ) {
            Icon(Icons.Filled.KeyboardArrowUp, contentDescription = "Swipe up")
        }
    }
}
