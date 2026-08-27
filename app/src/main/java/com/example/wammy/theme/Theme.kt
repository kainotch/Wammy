// Created by Notch
package com.example.wammy.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.example.wammy.theme.schemes.*

@Composable
fun WammyTheme(
    appTheme: AppTheme = AppTheme.DEFAULT,
    amoled: Boolean = false,
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    
    val baseScheme = when (appTheme) {
        AppTheme.MONET -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
            } else {
                if (darkTheme) TachiyomiColorScheme.darkScheme else TachiyomiColorScheme.lightScheme
            }
        }
        AppTheme.GREEN_APPLE -> if (darkTheme) GreenAppleColorScheme.darkScheme else GreenAppleColorScheme.lightScheme
        AppTheme.LAVENDER -> if (darkTheme) LavenderColorScheme.darkScheme else LavenderColorScheme.lightScheme
        AppTheme.MIDNIGHT_DUSK -> if (darkTheme) MidnightDuskColorScheme.darkScheme else MidnightDuskColorScheme.lightScheme
        AppTheme.NORD -> if (darkTheme) NordColorScheme.darkScheme else NordColorScheme.lightScheme
        AppTheme.STRAWBERRY -> if (darkTheme) StrawberryColorScheme.darkScheme else StrawberryColorScheme.lightScheme
        AppTheme.TAKO -> if (darkTheme) TakoColorScheme.darkScheme else TakoColorScheme.lightScheme
        AppTheme.TEALTURQOISE -> if (darkTheme) TealTurqoiseColorScheme.darkScheme else TealTurqoiseColorScheme.lightScheme
        AppTheme.TIDAL_WAVE -> if (darkTheme) TidalWaveColorScheme.darkScheme else TidalWaveColorScheme.lightScheme
        AppTheme.YINYANG -> if (darkTheme) YinYangColorScheme.darkScheme else YinYangColorScheme.lightScheme
        AppTheme.YOTSUBA -> if (darkTheme) YotsubaColorScheme.darkScheme else YotsubaColorScheme.lightScheme
        AppTheme.MONOCHROME -> if (darkTheme) MonochromeColorScheme.darkScheme else MonochromeColorScheme.lightScheme
        AppTheme.TOKYO_NIGHT -> if (darkTheme) TokyoNightColorScheme.darkScheme else TokyoNightColorScheme.lightScheme
        AppTheme.CATPPUCCIN -> if (darkTheme) CatppuccinColorScheme.darkScheme else CatppuccinColorScheme.lightScheme
        AppTheme.DEFAULT -> if (darkTheme) TachiyomiColorScheme.darkScheme else TachiyomiColorScheme.lightScheme
    }

    val finalScheme = if (darkTheme && amoled) {
        baseScheme.copy(
            background = Color.Black,
            surface = Color.Black,
            surfaceVariant = Color.Black,
            surfaceContainer = Color.Black,
            surfaceContainerLow = Color.Black,
            surfaceContainerLowest = Color.Black
        )
    } else {
        baseScheme
    }

    MaterialTheme(colorScheme = finalScheme, typography = Typography, content = content)
}
