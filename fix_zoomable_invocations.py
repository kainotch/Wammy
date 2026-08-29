import re

with open('app/src/main/java/com/example/wammy/ui/screens/ReaderScreen.kt', 'r') as f:
    text = f.read()

old_invocation = """                                    contentScale = ContentScale.Fit,
                                    modifier = Modifier.fillMaxSize(),
                                    filterMode = colorFilter,"""

new_invocation = """                                    contentScale = scaleType,
                                    modifier = Modifier.fillMaxSize(),
                                    grayscale = grayscale,
                                    invertedColors = invertedColors,
                                    isCustomBrightness = isCustomBrightness,
                                    brightnessValue = brightnessValue,
                                    isColorFilter = isColorFilter,
                                    colorR = colorR,
                                    colorG = colorG,
                                    colorB = colorB,
                                    colorA = colorA,"""

text = text.replace(old_invocation, new_invocation)

with open('app/src/main/java/com/example/wammy/ui/screens/ReaderScreen.kt', 'w') as f:
    f.write(text)

