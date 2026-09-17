package eu.kanade.tachiyomi.jsplugin.model

import kotlinx.serialization.Serializable

/**
 * Represents a JS plugin from LNReader-compatible repositories.
 * Maps directly to the plugin index JSON format.
 */
@Serializable
data class JsPlugin(
    val id: String,
    val name: String,
    val site: String,
    val lang: String,
    val version: String,
    val url: String,
    val iconUrl: String,
    val customCSS: String? = null,
    val customJS: String? = null,
    var repositoryUrl: String? = null,
) {
    companion object {
        /** Package name prefix for novel JS plugins - unique to wammy fork */
        const val PKG_PREFIX = "com.notch.wammy.jsplugin."
    }

    /**
     * Unique identifier combining plugin ID and repository URL for disambiguation
     */
    fun uniqueId(): String = "js:$id"

    /**
     * Unique package name for this plugin - prevents conflicts with other forks
     */
    fun pkgName(): String = "${PKG_PREFIX}$id"

    /**
     * Generate a stable Long ID for Source compatibility
     */
    fun sourceId(): Long {
        // Use same hashing approach as HttpSource for consistency
        val key = "${name.lowercase()}/$lang/js"
        return key.hashCode().toLong() and Long.MAX_VALUE
    }

    /**
     * Normalized language code for grouping (e.g., "English" -> "en")
     */
    fun langCode(): String = when {
        lang.contains("English", ignoreCase = true) -> "en"
        lang.contains("ä¸­æ–‡") || lang.contains("Chinese", ignoreCase = true) -> "zh"
        lang.contains("æ—¥æœ¬") || lang.contains("Japanese", ignoreCase = true) -> "ja"
        lang.contains("í•œêµ­") || lang.contains("Korean", ignoreCase = true) -> "ko"
        lang.contains("FranÃ§ais", ignoreCase = true) -> "fr"
        lang.contains("EspaÃ±ol", ignoreCase = true) -> "es"
        lang.contains("PortuguÃªs", ignoreCase = true) -> "pt"
        lang.contains("Ð ÑƒÑÑÐºÐ¸Ð¹", ignoreCase = true) -> "ru"
        lang.contains("Indonesia", ignoreCase = true) -> "id"
        lang.contains("TÃ¼rkÃ§e", ignoreCase = true) -> "tr"
        lang.contains("Ø§Ù„Ø¹Ø±Ø¨ÙŠØ©") -> "ar"
        lang.contains("à¹„à¸—à¸¢") -> "th"
        lang.contains("Viá»‡t", ignoreCase = true) -> "vi"
        lang.contains("Polski", ignoreCase = true) -> "pl"
        lang.contains("Ð£ÐºÑ€Ð°Ñ—Ð½ÑÑŒÐºÐ°", ignoreCase = true) -> "uk"
        lang.contains("Multi", ignoreCase = true) -> "all"
        else -> "other"
    }
}

/**
 * Represents a JS plugin repository
 */
@Serializable
data class JsPluginRepository(
    val name: String,
    val url: String,
    val enabled: Boolean = true,
)

/**
 * Installed JS plugin with cached code
 */
data class InstalledJsPlugin(
    val plugin: JsPlugin,
    val code: String,
    val installedVersion: String,
    val repositoryUrl: String,
)
