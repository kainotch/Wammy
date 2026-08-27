// Created by Notch
package com.example.wammy.theme

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class ThemeMode {
    SYSTEM, LIGHT, DARK
}

enum class AppTheme {
    DEFAULT, MONET, GREEN_APPLE, LAVENDER, MIDNIGHT_DUSK, NORD, STRAWBERRY, TAKO, TEALTURQOISE, TIDAL_WAVE, YINYANG, YOTSUBA, MONOCHROME, TOKYO_NIGHT, CATPPUCCIN
}

class ThemePreferences(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("theme_prefs", Context.MODE_PRIVATE)

    private val _themeMode = MutableStateFlow(getSavedThemeMode())
    val themeMode: StateFlow<ThemeMode> = _themeMode.asStateFlow()

    private val _appTheme = MutableStateFlow(getSavedAppTheme())
    val appTheme: StateFlow<AppTheme> = _appTheme.asStateFlow()
    
    private val _amoled = MutableStateFlow(prefs.getBoolean("amoled", false))
    val amoled: StateFlow<Boolean> = _amoled.asStateFlow()

    private fun getSavedAppTheme(): AppTheme {
        val savedName = prefs.getString("app_theme", AppTheme.DEFAULT.name) ?: AppTheme.DEFAULT.name
        return try {
            AppTheme.valueOf(savedName)
        } catch (e: Exception) {
            AppTheme.DEFAULT
        }
    }

    fun setAppTheme(theme: AppTheme) {
        prefs.edit().putString("app_theme", theme.name).apply()
        _appTheme.value = theme
    }
    
    fun setAmoled(isAmoled: Boolean) {
        prefs.edit().putBoolean("amoled", isAmoled).apply()
        _amoled.value = isAmoled
    }

    private fun getSavedThemeMode(): ThemeMode {
        val savedName = prefs.getString("theme_mode", ThemeMode.SYSTEM.name) ?: ThemeMode.SYSTEM.name
        return try {
            ThemeMode.valueOf(savedName)
        } catch (e: Exception) {
            ThemeMode.SYSTEM
        }
    }

    fun setThemeMode(mode: ThemeMode) {
        prefs.edit().putString("theme_mode", mode.name).apply()
        _themeMode.value = mode
    }
}
