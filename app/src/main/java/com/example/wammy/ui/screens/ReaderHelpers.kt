// Created by Notch
package com.example.wammy.ui.screens

import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import com.example.wammy.ui.ColorFilterMode

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
}

enum class TapZone { LEFT, CENTER, RIGHT }
