// Created by Notch
package com.example.wammy.util

object ChapterRecognition {
    private val numberRegex = Regex("""(\d+\.?\d*)""")
    
    // Looks for explicit chapter markers like "Ch. X", "Chapter X", "Ep X" etc.
    private val chapterRegex = Regex("""(?i)(?:ch\.|chapter|ep\.|episode|part)\s*(\d+\.?\d*)""")

    /**
     * Attempts to parse a Float chapter number from a chapter name.
     * Mimics Mihon's behavior of stripping the manga title first.
     */
    fun parseChapterNumber(mangaTitle: String, chapterName: String): Float {
        // 1. Remove manga title to avoid extracting numbers from the title itself
        val nameWithoutTitle = if (mangaTitle.isNotBlank()) {
            chapterName.replace(mangaTitle, "", ignoreCase = true).trim()
        } else {
            chapterName.trim()
        }
        
        // 2. Try to find explicit "Chapter X" or "Ch. X"
        val chMatch = chapterRegex.find(nameWithoutTitle)
        if (chMatch != null) {
            return chMatch.groupValues[1].toFloatOrNull() ?: -1f
        }

        // 3. Try to find standalone numbers
        val allNumbers = numberRegex.findAll(nameWithoutTitle).mapNotNull { it.groupValues[1].toFloatOrNull() }.toList()
        if (allNumbers.isNotEmpty()) {
            return allNumbers.last()
        }

        // 4. Fallbacks using original string
        val fallbackMatch = chapterRegex.find(chapterName)
        if (fallbackMatch != null) {
            return fallbackMatch.groupValues[1].toFloatOrNull() ?: -1f
        }

        val fallbackNumbers = numberRegex.findAll(chapterName).mapNotNull { it.groupValues[1].toFloatOrNull() }.toList()
        if (fallbackNumbers.isNotEmpty()) {
            return fallbackNumbers.last()
        }

        // 5. Default
        return -1f
    }
}
