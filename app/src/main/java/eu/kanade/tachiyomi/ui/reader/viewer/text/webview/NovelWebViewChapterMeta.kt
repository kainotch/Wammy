package eu.kanade.tachiyomi.ui.reader.viewer.text.webview

import eu.kanade.tachiyomi.ui.reader.model.ReaderChapter
import eu.kanade.tachiyomi.ui.reader.viewer.text.shared.HtmlUtils
import java.net.URI

internal object NovelWebViewChapterMeta {

    const val CHAPTER_TAG_NAME = "wammy-chapter"
    const val CHAPTER_ID_ATTR = "data-chapter-id"
    const val CHAPTER_TITLE_ATTR = "data-chapter-title"
    const val CHAPTER_NUMBER_ATTR = "data-chapter-number"
    const val CHAPTER_PATH_ATTR = "data-chapter-path"
    const val CHAPTER_URL_ATTR = "data-chapter-url"
    const val WAMMY_CHAPTER_ATTR = "data-wammy-chapter"
    const val CHAPTER_DIVIDER_CLASS = "wammy-chapter-divider"
    const val WAMMY_CHAPTERS_CONTAINER_ID = "wammy-chapters-container"

    // Paged mode: class toggled on <html> by paged-reader.js, and the CSS multicol target.
    const val PAGED_BODY_CLASS = "wammy-paged"

    const val WAMMY_OBJECT_NAME = "Wammy"
    const val WAMMY_NOVEL_URL_KEY = "novelUrl"
    const val WAMMY_CURRENT_CHAPTER_KEY = "currentChapter"
    const val WAMMY_CHAPTERS_KEY = "chapters"
    const val WAMMY_IS_EDIT_MODE_KEY = "isEditMode"
    const val WAMMY_IS_INF_SCROLL_KEY = "isInfScroll"
    const val WAMMY_TEXT_SELECTION_BLOCKED_KEY = "textSelectionBlocked"
    const val WAMMY_FORCED_LOWERCASE_KEY = "forcedLowercase"
    const val WAMMY_MENU_VISIBLE_KEY = "menuVisible"
    const val WAMMY_IMMERSIVE_KEY = "immersive"
    const val WAMMY_TTS_STATE_KEY = "ttsState"
    const val WAMMY_LOADING_CHAPTER_KEY = "loadingChapter"
    const val WAMMY_PAGING_ENABLED_KEY = "pagingEnabled"

    // Event names dispatched on `window` so plugins/snippets can subscribe with addEventListener.
    const val EVENT_MENU_VISIBILITY = "wammy:menuvisibilitychange"
    const val EVENT_CHAPTER_NAVIGATE = "wammy:chapternavigate"
    const val EVENT_CHAPTER_LOADING = "wammy:chapterloading"
    const val EVENT_TTS_STATE = "wammy:ttsstatechange"

    // Fired page-side (paged-reader.js) whenever the current page or page count changes: a page
    // turn, a repagination after reflow/rotation/append, or an explicit goToPage.
    const val EVENT_PAGE_CHANGE = "wammy:pagechange"

    // Fired page-side (scroll-tracking.js, or paged-reader.js when paged mode is active) as reading
    // progress advances, throttled with the slider bridge. Dispatched from JS, not Kotlin, so there
    // is no per-frame bridge hop. Same detail shape `{progress, chapterProgress, chapterId, isLast}`
    // in both modes - a snippet can bind one listener regardless of which mode is active.
    const val EVENT_PROGRESS = "wammy:progresschange"

    // Safe-area CSS custom properties: the reader menu bar heights the page must clear (0 while the
    // menu is hidden). Single source of truth for both the injector and the CSS that reads them.
    const val CSS_VAR_SAFE_TOP = "--wammy-safe-top"
    const val CSS_VAR_SAFE_BOTTOM = "--wammy-safe-bottom"

    fun String.jsEscape(): String =
        this.replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("</script>", "<\\/script>")
            .replace("</Script>", "<\\/Script>")
            .replace("</SCRIPT>", "<\\/SCRIPT>")

    fun String.htmlAttributeEscape(): String =
        this.replace("&", "&amp;")
            .replace("\"", "&quot;")
            .replace("'", "&#39;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")

    fun quoteForJson(value: String): String {
        val sb = StringBuilder(value.length + 2)
        sb.append('"')
        for (c in value) {
            when (c) {
                '"' -> sb.append("\\\"")
                '\\' -> sb.append("\\\\")
                '/' -> sb.append("\\/")
                '\n' -> sb.append("\\n")
                '\r' -> sb.append("\\r")
                '\t' -> sb.append("\\t")
                '\b' -> sb.append("\\b")
                '\u000C' -> sb.append("\\f")
                else -> if (c.code < 0x20) {
                    sb.append("\\u").append(String.format("%04x", c.code))
                } else {
                    sb.append(c)
                }
            }
        }
        sb.append('"')
        return sb.toString()
    }

    /**
     * Inverse of [quoteForJson]: decodes a JSON string literal (as handed back by
     * `evaluateJavascript`, quotes included) into the raw text.
     *
     * Walks the string once, consuming each backslash escape together with the character
     * after it. Sequential global replaces (e.g. `.replace("\\n", "\n")` after `.replace("\\\\",
     * "\\")`) can't tell a real `\n` escape apart from an escaped literal backslash followed by
     * the letter n - both collapse to the same two characters after the backslash-unescape pass,
     * so a page using "\" as text obfuscation would have it misread as a newline.
     */
    fun unescapeJsResult(result: String): String {
        if (!(result.startsWith("\"") && result.endsWith("\""))) return result
        val inner = result.substring(1, result.length - 1)
        val sb = StringBuilder(inner.length)
        var i = 0
        while (i < inner.length) {
            val c = inner[i]
            if (c == '\\' && i + 1 < inner.length) {
                when (inner[i + 1]) {
                    '\\' -> { sb.append('\\'); i += 2 }
                    'n' -> { sb.append('\n'); i += 2 }
                    't' -> { sb.append('\t'); i += 2 }
                    'r' -> { sb.append('\r'); i += 2 }
                    '"' -> { sb.append('"'); i += 2 }
                    '/' -> { sb.append('/'); i += 2 }
                    'b' -> { sb.append('\b'); i += 2 }
                    'f' -> { sb.append('\u000C'); i += 2 }
                    'u' -> {
                        val hex = inner.substring(i + 2, minOf(i + 6, inner.length))
                        val code = hex.takeIf { it.length == 4 }?.toIntOrNull(16)
                        if (code != null) {
                            sb.append(code.toChar())
                            i += 6
                        } else {
                            sb.append(c)
                            i += 1
                        }
                    }
                    else -> { sb.append(c); i += 1 }
                }
            } else {
                sb.append(c)
                i += 1
            }
        }
        return sb.toString()
    }

    fun toAbsoluteChapterUrl(chapterPath: String?, novelUrl: String?): String {
        val normalized = HtmlUtils.normalizeUrl(chapterPath).orEmpty().trim()
        if (normalized.isBlank()) return ""
        if (normalized.startsWith("http://") || normalized.startsWith("https://")) return normalized

        val novel = HtmlUtils.normalizeUrl(novelUrl).orEmpty().trim()
        if (!(novel.startsWith("http://") || novel.startsWith("https://"))) return normalized

        return try {
            URI(novel).resolve(normalized).toString()
        } catch (_: Exception) {
            normalized
        }
    }

    fun resolveWebViewBaseUrl(chapterUrl: String?, novelUrl: String?, sourceBaseUrl: String? = null): String? {
        val repaired = HtmlUtils.normalizeUrl(chapterUrl)
        val absoluteChapterUrl = repaired?.trim().takeUnless { it.isNullOrBlank() }
            ?.takeIf { it.startsWith("http://") || it.startsWith("https://") }
        if (absoluteChapterUrl != null) return absoluteChapterUrl
        val novel = HtmlUtils.normalizeUrl(novelUrl)?.trim().takeUnless { it.isNullOrBlank() }
            ?.takeIf { it.startsWith("http://") || it.startsWith("https://") }
        if (novel != null) return novel

        // Neither url is absolute (common: sources store relative paths). Anchor the WebView base
        // on the source's site so the browser resolves relative asset urls (e.g. /uploads/x.webp)
        // itself, exactly like a normal page load. Prefer the chapter path, then the novel path.
        val base = sourceBaseUrl?.trim().takeUnless { it.isNullOrBlank() }
            ?.takeIf { it.startsWith("http://") || it.startsWith("https://") }
            ?: return null
        val relative = repaired?.trim().takeUnless { it.isNullOrBlank() }
            ?: HtmlUtils.normalizeUrl(novelUrl)?.trim().takeUnless { it.isNullOrBlank() }
            ?: return base
        return try {
            URI(base.trimEnd('/') + "/").resolve(relative.trimStart('/')).toString()
        } catch (_: Exception) {
            base
        }
    }

    fun buildChapterJson(chapter: ReaderChapter?, novelUrl: String?): String {
        val chapterModel = chapter?.chapter
        val chapterId = chapterModel?.id ?: -1L
        val chapterTitle = quoteForJson(chapterModel?.name.orEmpty())
        val chapterNumber = chapterModel?.chapter_number ?: -1f
        val chapterPath = quoteForJson(chapterModel?.url.orEmpty())
        val chapterUrl = quoteForJson(toAbsoluteChapterUrl(chapterModel?.url, novelUrl))
        return buildString {
            append('{')
            append("\"id\": ").append(chapterId).append(',')
            append("\"title\": ").append(chapterTitle).append(',')
            append("\"number\": ").append(chapterNumber).append(',')
            append("\"path\": ").append(chapterPath).append(',')
            append("\"url\": ").append(chapterUrl)
            append('}')
        }
    }

    fun buildChaptersJson(chapters: List<ReaderChapter>, novelUrl: String?): String =
        chapters.joinToString(prefix = "[", postfix = "]") { buildChapterJson(it, novelUrl) }

    data class WammyScriptContext(
        val novelUrl: String?,
        val currentChapter: ReaderChapter?,
        val chaptersInOrder: List<ReaderChapter>,
        val isEditingMode: Boolean,
        val isInfiniteScroll: Boolean,
        val isPagedMode: Boolean,
        val textSelectionBlocked: Boolean,
        val forcedLowercase: Boolean,
        val menuVisible: Boolean = false,
        val immersive: Boolean = false,
        val ttsState: String = "stopped",
        val loadingChapter: Boolean = false,
    )

    fun buildWammyScript(context: WammyScriptContext): String {
        val novelUrl = quoteForJson(HtmlUtils.normalizeUrl(context.novelUrl).orEmpty())
        val currentChapterJson = buildChapterJson(context.currentChapter, context.novelUrl)
        val chaptersJson = buildChaptersJson(context.chaptersInOrder, context.novelUrl)
        return """
            window.$WAMMY_OBJECT_NAME = window.$WAMMY_OBJECT_NAME || {};
            window.$WAMMY_OBJECT_NAME.$WAMMY_NOVEL_URL_KEY = $novelUrl;
            window.$WAMMY_OBJECT_NAME.$WAMMY_CURRENT_CHAPTER_KEY = $currentChapterJson;
            window.$WAMMY_OBJECT_NAME.$WAMMY_CHAPTERS_KEY = $chaptersJson;
            window.$WAMMY_OBJECT_NAME.runtime = window.$WAMMY_OBJECT_NAME.runtime || {};
            window.$WAMMY_OBJECT_NAME.runtime.$WAMMY_IS_EDIT_MODE_KEY = ${context.isEditingMode};
            window.$WAMMY_OBJECT_NAME.runtime.$WAMMY_IS_INF_SCROLL_KEY = ${context.isInfiniteScroll};
            window.$WAMMY_OBJECT_NAME.runtime.$WAMMY_PAGING_ENABLED_KEY = ${context.isPagedMode};
            window.$WAMMY_OBJECT_NAME.runtime.$WAMMY_TEXT_SELECTION_BLOCKED_KEY = ${context.textSelectionBlocked};
            window.$WAMMY_OBJECT_NAME.runtime.$WAMMY_FORCED_LOWERCASE_KEY = ${context.forcedLowercase};
            window.$WAMMY_OBJECT_NAME.runtime.$WAMMY_MENU_VISIBLE_KEY = ${context.menuVisible};
            window.$WAMMY_OBJECT_NAME.runtime.$WAMMY_IMMERSIVE_KEY = ${context.immersive};
            window.$WAMMY_OBJECT_NAME.runtime.$WAMMY_TTS_STATE_KEY = ${quoteForJson(context.ttsState)};
            window.$WAMMY_OBJECT_NAME.runtime.$WAMMY_LOADING_CHAPTER_KEY = ${context.loadingChapter};
            window.$WAMMY_OBJECT_NAME.actions = window.$WAMMY_OBJECT_NAME.actions || {
                nextChapter: function() { Android.requestNextChapter(); },
                prevChapter: function() { Android.requestPrevChapter(); },
                startTts: function() { Android.requestStartTts(); },
                pauseTts: function() { Android.requestPauseTts(); },
                resumeTts: function() { Android.requestResumeTts(); },
                stopTts: function() { Android.requestStopTts(); },
                setProgress: function(percent) { Android.requestSetProgress(percent); },
            };
        """.trimIndent()
    }
}
