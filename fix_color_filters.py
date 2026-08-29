import re
import os

with open('app/src/main/java/com/example/wammy/ui/screens/ReaderHelpers.kt', 'r') as f:
    text = f.read()

old_helper = """import com.example.wammy.ui.ColorFilterMode

fun getColorFilterForMode(mode: ColorFilterMode): ColorFilter? {
    return when (mode) {
        ColorFilterMode.NONE -> null
        ColorFilterMode.INVERT -> {
            ColorFilter.colorMatrix(ColorMatrix(floatArrayOf(
                -1f, 0f, 0f, 0f, 255f,
                0f, -1f, 0f, 0f, 255f,
                0f, 0f, -1f, 0f, 255f,
                0f, 0f, 0f, 1f, 0f
            )))
        }
        ColorFilterMode.GREYSCALE -> {
            val matrix = ColorMatrix()
            matrix.setToSaturation(0f)
            ColorFilter.colorMatrix(matrix)
        }
        ColorFilterMode.SEPIA -> {
            ColorFilter.colorMatrix(ColorMatrix(floatArrayOf(
                0.393f, 0.769f, 0.189f, 0f, 0f,
                0.349f, 0.686f, 0.168f, 0f, 0f,
                0.272f, 0.534f, 0.131f, 0f, 0f,
                0f, 0f, 0f, 1f, 0f
            )))
        }
    }
}"""

new_helper = """fun getCustomColorFilter(grayscale: Boolean, invert: Boolean): ColorFilter? {
    if (!grayscale && !invert) return null
    
    val matrix = ColorMatrix()
    
    if (grayscale) {
        matrix.setToSaturation(0f)
    }
    
    if (invert) {
        // Multiply by invert matrix
        val invertMatrix = ColorMatrix(floatArrayOf(
            -1f, 0f, 0f, 0f, 255f,
            0f, -1f, 0f, 0f, 255f,
            0f, 0f, -1f, 0f, 255f,
            0f, 0f, 0f, 1f, 0f
        ))
        
        if (grayscale) {
            // Apply both: grayscale then invert. 
            // In Compose, there is no built-in matrix concat. 
            // We just construct it manually or use a simple hack.
            // A combined grayscale + invert is basically:
            // R = 255 - gray, G = 255 - gray, B = 255 - gray
            return ColorFilter.colorMatrix(ColorMatrix(floatArrayOf(
                -0.2126f, -0.7152f, -0.0722f, 0f, 255f,
                -0.2126f, -0.7152f, -0.0722f, 0f, 255f,
                -0.2126f, -0.7152f, -0.0722f, 0f, 255f,
                0f, 0f, 0f, 1f, 0f
            )))
        } else {
            return ColorFilter.colorMatrix(invertMatrix)
        }
    }
    
    return ColorFilter.colorMatrix(matrix)
}"""

text = text.replace(old_helper, new_helper)

with open('app/src/main/java/com/example/wammy/ui/screens/ReaderHelpers.kt', 'w') as f:
    f.write(text)

os.remove('app/src/main/java/com/example/wammy/ui/ColorFilterMode.kt')

