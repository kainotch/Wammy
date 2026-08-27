package com.example.wammy.lnreader.plugin

import com.example.wammy.lnreader.plugin.plugins.*

object PluginRegistry {
    private val plugins: List<LNPlugin> = listOf(
        NovelFirePlugin(),
        FirstKissNovelPlugin()
    )

    fun all(): List<LNPlugin> = plugins
    
    fun find(idOrPkg: String): LNPlugin? {
        val lowerIdOrPkg = idOrPkg.lowercase()
        return plugins.find { 
            lowerIdOrPkg.contains(it.id.lowercase()) || lowerIdOrPkg.contains(it.name.lowercase().replace(" ", ""))
        }
    }
    
    fun findByName(name: String): LNPlugin? = plugins.find { it.name.equals(name, ignoreCase = true) }
}
