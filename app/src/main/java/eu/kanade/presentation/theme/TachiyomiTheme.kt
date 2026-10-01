package eu.kanade.presentation.theme

import android.content.Context
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.ui.graphics.Color
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import eu.kanade.domain.ui.UiPreferences
import eu.kanade.domain.ui.model.AppTheme
import tachiyomi.presentation.core.util.collectAsState
import androidx.compose.runtime.getValue
import eu.kanade.presentation.theme.colorscheme.BaseColorScheme
import eu.kanade.presentation.theme.colorscheme.CatppuccinColorScheme
import eu.kanade.presentation.theme.colorscheme.GreenAppleColorScheme
import eu.kanade.presentation.theme.colorscheme.LavenderColorScheme
import eu.kanade.presentation.theme.colorscheme.MidnightDuskColorScheme
import eu.kanade.presentation.theme.colorscheme.MonetColorScheme
import eu.kanade.presentation.theme.colorscheme.MonochromeColorScheme
import eu.kanade.presentation.theme.colorscheme.NordColorScheme
import eu.kanade.presentation.theme.colorscheme.StrawberryColorScheme
import eu.kanade.presentation.theme.colorscheme.TachiyomiColorScheme
import eu.kanade.presentation.theme.colorscheme.TakoColorScheme
import eu.kanade.presentation.theme.colorscheme.TealTurqoiseColorScheme
import eu.kanade.presentation.theme.colorscheme.TidalWaveColorScheme
import eu.kanade.presentation.theme.colorscheme.TokyoNightColorScheme
import eu.kanade.presentation.theme.colorscheme.YinYangColorScheme
import eu.kanade.presentation.theme.colorscheme.YotsubaColorScheme
import eu.kanade.presentation.theme.colorscheme.MonetCompatColorScheme
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

@androidx.annotation.Keep
@Composable
fun TachiyomiTheme(
    appTheme: AppTheme? = null,
    amoled: Boolean? = null,
    seedColor: Int? = null,
    content: @Composable () -> Unit,
) {
    val uiPreferences = Injekt.get<UiPreferences>()
    val prefAppTheme by uiPreferences.appTheme.collectAsState()
    val prefIsAmoled by uiPreferences.themeDarkAmoled.collectAsState()
    val themeMode by uiPreferences.themeMode.collectAsState()
    BaseTachiyomiTheme(
        appTheme = appTheme ?: prefAppTheme,
        isAmoled = amoled ?: prefIsAmoled,
        themeMode = themeMode,
        seedColor = seedColor,
        content = content,
    )
}

@Composable
fun TachiyomiPreviewTheme(
    appTheme: AppTheme = AppTheme.DEFAULT,
    isAmoled: Boolean = false,
    content: @Composable () -> Unit,
) = BaseTachiyomiTheme(appTheme, isAmoled, eu.kanade.domain.ui.model.ThemeMode.SYSTEM, content = content)

@Composable
private fun BaseTachiyomiTheme(
    appTheme: AppTheme,
    isAmoled: Boolean,
    themeMode: eu.kanade.domain.ui.model.ThemeMode,
    seedColor: Int? = null,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val isSystemDark = isSystemInDarkTheme()
    val isDark = when (themeMode) {
        eu.kanade.domain.ui.model.ThemeMode.LIGHT -> false
        eu.kanade.domain.ui.model.ThemeMode.DARK -> true
        eu.kanade.domain.ui.model.ThemeMode.SYSTEM -> isSystemDark
    }
    MaterialExpressiveTheme(
        colorScheme = remember(appTheme, isDark, isAmoled, seedColor) {
            if (seedColor != null) {
                val scheme = MonetCompatColorScheme(Color(seedColor))
                scheme.getColorScheme(isDark, isAmoled, overrideDarkSurfaceContainers = false)
            } else {
                getThemeColorScheme(
                    context = context,
                    appTheme = appTheme,
                    isDark = isDark,
                    isAmoled = isAmoled,
                )
            }
        },
        content = content,
    )
}

private fun getThemeColorScheme(
    context: Context,
    appTheme: AppTheme,
    isDark: Boolean,
    isAmoled: Boolean,
): ColorScheme {
    val colorScheme = if (appTheme == AppTheme.MONET) {
        MonetColorScheme(context)
    } else {
        colorSchemes.getOrDefault(appTheme, TachiyomiColorScheme)
    }
    return colorScheme.getColorScheme(
        isDark = isDark,
        isAmoled = isAmoled,
        overrideDarkSurfaceContainers = appTheme != AppTheme.MONET,
    )
}

private val colorSchemes: Map<AppTheme, BaseColorScheme> = mapOf(
    AppTheme.DEFAULT to TachiyomiColorScheme,
    AppTheme.CATPPUCCIN to CatppuccinColorScheme,
    AppTheme.TOKYONIGHT to TokyoNightColorScheme,
    AppTheme.GREEN_APPLE to GreenAppleColorScheme,
    AppTheme.LAVENDER to LavenderColorScheme,
    AppTheme.MIDNIGHT_DUSK to MidnightDuskColorScheme,
    AppTheme.MONOCHROME to MonochromeColorScheme,
    AppTheme.NORD to NordColorScheme,
    AppTheme.STRAWBERRY_DAIQUIRI to StrawberryColorScheme,
    AppTheme.TAKO to TakoColorScheme,
    AppTheme.TEALTURQUOISE to TealTurqoiseColorScheme,
    AppTheme.TIDAL_WAVE to TidalWaveColorScheme,
    AppTheme.YINYANG to YinYangColorScheme,
    AppTheme.YOTSUBA to YotsubaColorScheme,
)


