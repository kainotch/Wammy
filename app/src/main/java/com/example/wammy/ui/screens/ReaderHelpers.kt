// Created by Notch
package com.example.wammy.ui.screens

import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
fun getCustomColorFilter(grayscale: Boolean, invert: Boolean): ColorFilter? {
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
}


