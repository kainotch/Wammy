package com.example.wammy.data.prefs

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class ReadingMode { DEFAULT, LTR, RTL, VERTICAL, WEBTOON, CONTINUOUS_VERTICAL }
enum class OrientationType { DEFAULT, FREE, PORTRAIT, LANDSCAPE, LOCKED_PORTRAIT, LOCKED_LANDSCAPE, REVERSE_PORTRAIT }
enum class TapInvertMode { NONE, HORIZONTAL, VERTICAL, BOTH }
enum class ScaleType { FIT_SCREEN, STRETCH, FIT_WIDTH, FIT_HEIGHT, ORIGINAL, SMART_FIT }
enum class ZoomStart { AUTOMATIC, LEFT, RIGHT, CENTER }
enum class DualPageView { NEVER, ALWAYS, WIDE }
enum class ReaderTheme { BLACK, GRAY, WHITE, AUTOMATIC }
enum class FlashColor { BLACK, WHITE, WHITE_THEN_BLACK }
enum class WebGPUTransition { DEFAULT, FLIP_LEFT, FLIP_RIGHT, STACK_LEFT, STACK_RIGHT, STACK_UP, STACK_DOWN, SPHERE, CUBE_INSIDE, CUBE_OUTSIDE, FADE, FADE_WHITE, NONE }
enum class WebGPUCutout { IGNORE, AVOID, SHIFT }
enum class HideThreshold { HIGHEST, HIGH, LOW, LOWEST }
enum class BlendModeType { DEFAULT, MULTIPLY, SCREEN, OVERLAY, LIGHTEN, DARKEN }

class ReaderPreferences(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("reader_prefs", Context.MODE_PRIVATE)

    // Reading
    val defaultReadingMode = PreferenceFlow(prefs, "defaultReadingMode", ReadingMode.RTL.name) { ReadingMode.valueOf(it) }
    val defaultOrientationType = PreferenceFlow(prefs, "defaultOrientationType", OrientationType.FREE.name) { OrientationType.valueOf(it) }

    // Navigation - Pager
    val navigationModePager = PreferenceFlow(prefs, "navigationModePager", 0) { it.toInt() }
    val pagerNavInverted = PreferenceFlow(prefs, "pagerNavInverted", TapInvertMode.NONE.name) { TapInvertMode.valueOf(it) }
    val navigateToPan = PreferenceFlow(prefs, "navigateToPan", true) { it.toBoolean() }
    
    // Navigation - Webtoon
    val navigationModeWebtoon = PreferenceFlow(prefs, "navigationModeWebtoon", 0) { it.toInt() }
    val webtoonNavInverted = PreferenceFlow(prefs, "webtoonNavInverted", TapInvertMode.NONE.name) { TapInvertMode.valueOf(it) }
    val webtoonSidePadding = PreferenceFlow(prefs, "webtoonSidePadding", 0) { it.toInt() }
    val readerHideThreshold = PreferenceFlow(prefs, "readerHideThreshold", HideThreshold.LOW.name) { HideThreshold.valueOf(it) }
    val webtoonDoubleTapZoomEnabled = PreferenceFlow(prefs, "webtoonDoubleTapZoomEnabled", true) { it.toBoolean() }
    val webtoonDisableZoomOut = PreferenceFlow(prefs, "webtoonDisableZoomOut", false) { it.toBoolean() }

    // Navigation - Other
    val readWithVolumeKeys = PreferenceFlow(prefs, "readWithVolumeKeys", false) { it.toBoolean() }
    val readWithVolumeKeysInverted = PreferenceFlow(prefs, "readWithVolumeKeysInverted", false) { it.toBoolean() }
    val readWithLongTap = PreferenceFlow(prefs, "readWithLongTap", true) { it.toBoolean() }
    val showNavigationOverlayOnStart = PreferenceFlow(prefs, "showNavigationOverlayOnStart", false) { it.toBoolean() }
    val showNavigationOverlayNewUser = PreferenceFlow(prefs, "showNavigationOverlayNewUser", true) { it.toBoolean() }

    // Zoom/scale (pager)
    val imageScaleType = PreferenceFlow(prefs, "imageScaleType", ScaleType.FIT_SCREEN.name) { ScaleType.valueOf(it) }
    val zoomStart = PreferenceFlow(prefs, "zoomStart", ZoomStart.AUTOMATIC.name) { ZoomStart.valueOf(it) }
    val landscapeZoom = PreferenceFlow(prefs, "landscapeZoom", true) { it.toBoolean() }
    val cropBorders = PreferenceFlow(prefs, "cropBorders", false) { it.toBoolean() }
    val cropBordersWebtoon = PreferenceFlow(prefs, "cropBordersWebtoon", false) { it.toBoolean() }

    // Dual page
    val dualPageSplitPaged = PreferenceFlow(prefs, "dualPageSplitPaged", false) { it.toBoolean() }
    val dualPageSplitWebtoon = PreferenceFlow(prefs, "dualPageSplitWebtoon", false) { it.toBoolean() }
    val dualPageInvertPaged = PreferenceFlow(prefs, "dualPageInvertPaged", false) { it.toBoolean() }
    val dualPageInvertWebtoon = PreferenceFlow(prefs, "dualPageInvertWebtoon", false) { it.toBoolean() }
    val dualPageRotateToFit = PreferenceFlow(prefs, "dualPageRotateToFit", false) { it.toBoolean() }
    val dualPageRotateToFitWebtoon = PreferenceFlow(prefs, "dualPageRotateToFitWebtoon", false) { it.toBoolean() }
    val dualPageRotateToFitInvert = PreferenceFlow(prefs, "dualPageRotateToFitInvert", false) { it.toBoolean() }
    val dualPageRotateToFitInvertWebtoon = PreferenceFlow(prefs, "dualPageRotateToFitInvertWebtoon", false) { it.toBoolean() }
    val dualPageView = PreferenceFlow(prefs, "dualPageView", DualPageView.NEVER.name) { DualPageView.valueOf(it) }

    // Vertical navigator
    val verticalNavigatorModes = PreferenceFlow(prefs, "verticalNavigatorModes", "") { it } // Store as comma-separated string
    val verticalNavigatorOnLeft = PreferenceFlow(prefs, "verticalNavigatorOnLeft", false) { it.toBoolean() }
    val verticalNavigatorHeight = PreferenceFlow(prefs, "verticalNavigatorHeight", 65) { it.toInt() }

    // Display
    val readerTheme = PreferenceFlow(prefs, "readerTheme", ReaderTheme.BLACK.name) { ReaderTheme.valueOf(it) }
    val fullscreen = PreferenceFlow(prefs, "fullscreen", true) { it.toBoolean() }
    val drawUnderCutout = PreferenceFlow(prefs, "drawUnderCutout", true) { it.toBoolean() }
    val keepScreenOn = PreferenceFlow(prefs, "keepScreenOn", false) { it.toBoolean() }
    val showPageNumber = PreferenceFlow(prefs, "showPageNumber", true) { it.toBoolean() }
    val showReadingMode = PreferenceFlow(prefs, "showReadingMode", true) { it.toBoolean() }
    val pageTransitions = PreferenceFlow(prefs, "pageTransitions", true) { it.toBoolean() }
    val doubleTapAnimSpeed = PreferenceFlow(prefs, "doubleTapAnimSpeed", 500) { it.toInt() }
    val alwaysShowChapterTransition = PreferenceFlow(prefs, "alwaysShowChapterTransition", true) { it.toBoolean() }

    // Color filter
    val customBrightness = PreferenceFlow(prefs, "customBrightness", false) { it.toBoolean() }
    val customBrightnessValue = PreferenceFlow(prefs, "customBrightnessValue", 0) { it.toInt() }
    val colorFilter = PreferenceFlow(prefs, "colorFilter", false) { it.toBoolean() }
    val colorFilterValueR = PreferenceFlow(prefs, "colorFilterValueR", 0) { it.toInt() }
    val colorFilterValueG = PreferenceFlow(prefs, "colorFilterValueG", 0) { it.toInt() }
    val colorFilterValueB = PreferenceFlow(prefs, "colorFilterValueB", 0) { it.toInt() }
    val colorFilterValueA = PreferenceFlow(prefs, "colorFilterValueA", 0) { it.toInt() }
    val colorFilterMode = PreferenceFlow(prefs, "colorFilterMode", BlendModeType.DEFAULT.name) { BlendModeType.valueOf(it) }
    val grayscale = PreferenceFlow(prefs, "grayscale", false) { it.toBoolean() }
    val invertedColors = PreferenceFlow(prefs, "invertedColors", false) { it.toBoolean() }

    // E-Ink
    val flashOnPageChange = PreferenceFlow(prefs, "flashOnPageChange", false) { it.toBoolean() }
    val flashDurationMillis = PreferenceFlow(prefs, "flashDurationMillis", 100) { it.toInt() }
    val flashPageInterval = PreferenceFlow(prefs, "flashPageInterval", 1) { it.toInt() }
    val flashColor = PreferenceFlow(prefs, "flashColor", FlashColor.BLACK.name) { FlashColor.valueOf(it) }

    // Chapter list behavior
    val skipRead = PreferenceFlow(prefs, "skipRead", false) { it.toBoolean() }
    val skipFiltered = PreferenceFlow(prefs, "skipFiltered", true) { it.toBoolean() }
    val skipDuplicate = PreferenceFlow(prefs, "skipDuplicate", false) { it.toBoolean() }

    // Experimental WebGPU
    val transitionAnimation = PreferenceFlow(prefs, "transitionAnimation", WebGPUTransition.DEFAULT.name) { WebGPUTransition.valueOf(it) }
    val cutoutMode = PreferenceFlow(prefs, "cutoutMode", WebGPUCutout.AVOID.name) { WebGPUCutout.valueOf(it) }

    // Actions
    val folderPerManga = PreferenceFlow(prefs, "folderPerManga", false) { it.toBoolean() }

    inner class PreferenceFlow<T>(
        private val prefs: SharedPreferences,
        val key: String,
        private val defaultValue: Any,
        private val mapper: (String) -> T
    ) {
        private val _state = MutableStateFlow(get())
        val state: StateFlow<T> = _state.asStateFlow()

        fun get(): T {
            val str = prefs.getString(key, defaultValue.toString()) ?: defaultValue.toString()
            return mapper(str)
        }

        fun set(value: T) {
            prefs.edit().putString(key, value.toString()).apply()
            _state.value = value
        }
    }
}
