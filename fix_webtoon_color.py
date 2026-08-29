import re

with open('app/src/main/java/com/example/wammy/ui/screens/ReaderScreen.kt', 'r') as f:
    text = f.read()

old_webtoon_img = """                                val cFilter = remember(colorFilter) { getColorFilterForMode(colorFilter) }
                                
                                AsyncImage(
                                    model = imageRequest,
                                    contentDescription = null,
                                    contentScale = ContentScale.FillWidth,
                                    alignment = Alignment.TopCenter,
                                    modifier = Modifier
                                        .fillMaxWidth(if (sidePadding > 0) 1f - (sidePadding * 2f / 100f) else 1f)
                                        .graphicsLayer { this.colorFilter = cFilter }
                                )"""

new_webtoon_img = """                                val cFilter = remember(grayscale, invertedColors) { getCustomColorFilter(grayscale, invertedColors) }
                                
                                Box(modifier = Modifier.fillMaxWidth(if (sidePadding > 0) 1f - (sidePadding * 2f / 100f) else 1f)) {
                                    AsyncImage(
                                        model = imageRequest,
                                        contentDescription = null,
                                        contentScale = scaleType,
                                        alignment = Alignment.TopCenter,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .graphicsLayer { this.colorFilter = cFilter }
                                    )
                                    if (isColorFilter) {
                                        Box(modifier = Modifier.matchParentSize().background(Color(colorR, colorG, colorB, colorA)))
                                    }
                                    if (isCustomBrightness && brightnessValue < 0) {
                                        val alpha = (kotlin.math.abs(brightnessValue) / 100f).coerceIn(0f, 1f)
                                        Box(modifier = Modifier.matchParentSize().background(Color.Black.copy(alpha = alpha)))
                                    }
                                }"""

text = text.replace(old_webtoon_img, new_webtoon_img)

# Ensure getColorFilterForMode is not used anywhere else
text = text.replace("getColorFilterForMode", "getCustomColorFilter")

with open('app/src/main/java/com/example/wammy/ui/screens/ReaderScreen.kt', 'w') as f:
    f.write(text)

