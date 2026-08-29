package com.example.wammy.ui.reader.navigation

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect

enum class TapAction { MENU, NEXT, PREV, LEFT, RIGHT }

data class TapRegion(val rect: Rect, val action: TapAction)

object NavigationLayouts {
    val lShaped = listOf(
        TapRegion(Rect(0f, 0f, 1f, 0.33f), TapAction.PREV),
        TapRegion(Rect(0f, 0.33f, 0.33f, 0.66f), TapAction.PREV),
        TapRegion(Rect(0.33f, 0.33f, 0.66f, 0.66f), TapAction.MENU),
        TapRegion(Rect(0.66f, 0.33f, 1f, 0.66f), TapAction.NEXT),
        TapRegion(Rect(0f, 0.66f, 1f, 1f), TapAction.NEXT)
    )
    
    val kindleIsh = listOf(
        TapRegion(Rect(0f, 0f, 1f, 0.33f), TapAction.MENU),
        TapRegion(Rect(0f, 0.33f, 0.33f, 1f), TapAction.PREV),
        TapRegion(Rect(0.33f, 0.33f, 1f, 1f), TapAction.NEXT)
    )
    
    val edge = listOf(
        TapRegion(Rect(0f, 0f, 0.33f, 1f), TapAction.NEXT),
        TapRegion(Rect(0.66f, 0f, 1f, 1f), TapAction.NEXT),
        TapRegion(Rect(0.33f, 0f, 0.66f, 0.66f), TapAction.MENU),
        TapRegion(Rect(0.33f, 0.66f, 0.66f, 1f), TapAction.PREV)
    )
    
    val rightAndLeft = listOf(
        TapRegion(Rect(0f, 0f, 0.33f, 1f), TapAction.LEFT),
        TapRegion(Rect(0.33f, 0f, 0.66f, 1f), TapAction.MENU),
        TapRegion(Rect(0.66f, 0f, 1f, 1f), TapAction.RIGHT)
    )
    
    val disabled = emptyList<TapRegion>()

    fun getLayout(index: Int, isWebtoon: Boolean, isVertical: Boolean): List<TapRegion> {
        return when (index) {
            0 -> if (isWebtoon || isVertical) lShaped else rightAndLeft // Default
            1 -> lShaped
            2 -> kindleIsh
            3 -> edge
            4 -> rightAndLeft
            5 -> disabled
            else -> rightAndLeft
        }
    }

    fun resolveTap(
        offset: Offset, 
        viewportWidth: Int, 
        viewportHeight: Int, 
        layout: List<TapRegion>, 
        invertMode: com.example.wammy.data.prefs.TapInvertMode
    ): TapAction {
        if (layout.isEmpty()) return TapAction.MENU
        
        var nx = offset.x / viewportWidth.toFloat()
        var ny = offset.y / viewportHeight.toFloat()

        when (invertMode) {
            com.example.wammy.data.prefs.TapInvertMode.HORIZONTAL -> nx = 1f - nx
            com.example.wammy.data.prefs.TapInvertMode.VERTICAL -> ny = 1f - ny
            com.example.wammy.data.prefs.TapInvertMode.BOTH -> {
                nx = 1f - nx
                ny = 1f - ny
            }
            else -> {}
        }
        
        for (region in layout) {
            if (region.rect.contains(Offset(nx, ny))) {
                return region.action
            }
        }
        return TapAction.MENU // Fallback if somehow outside regions
    }
}
