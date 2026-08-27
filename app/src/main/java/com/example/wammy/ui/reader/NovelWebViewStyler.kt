package com.example.wammy.ui.reader

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb

object NovelWebViewStyler {

    fun generateHtml(
        rawContent: String,
        backgroundColor: Color,
        textColor: Color,
        primaryColor: Color,
        fontSize: Int,
        lineSpacing: Float,
        fontFamily: String = "sans-serif"
    ): String {
        val bgHex = String.format("#%06X", 0xFFFFFF and backgroundColor.toArgb())
        val textHex = String.format("#%06X", 0xFFFFFF and textColor.toArgb())
        val primaryHex = String.format("#%06X", 0xFFFFFF and primaryColor.toArgb())

        // We use Google Fonts for custom typography (Lora, Merriweather, etc.)
        val googleFonts = if (fontFamily != "sans-serif") {
            "<link href=\"https://fonts.googleapis.com/css2?family=${fontFamily.replace(" ", "+")}:wght@400;700&display=swap\" rel=\"stylesheet\">"
        } else ""

        val actualFont = if (fontFamily != "sans-serif") "'$fontFamily', serif" else "sans-serif"

        return """
            <!DOCTYPE html>
            <html>
            <head>
                <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
                $googleFonts
                <style>
                    :root {
                        --theme-primary: $primaryHex;
                        --theme-background: $bgHex;
                        --theme-on-background: $textHex;
                    }
                    html {
                        background-color: var(--theme-background);
                    }
                    body {
                        font-family: $actualFont;
                        font-size: ${fontSize}px;
                        line-height: $lineSpacing;
                        padding: 24px;
                        padding-bottom: 120px;
                        padding-top: 40px;
                        overflow-x: hidden;
                        word-wrap: break-word;
                        color: var(--theme-on-background);
                        background-color: transparent;
                        margin: 0;
                        transition: color 0.3s ease, background-color 0.3s ease;
                    }
                    img { 
                        max-width: 100%; 
                        height: auto; 
                        display: block; 
                        margin: 16px auto; 
                        border-radius: 8px; 
                    }
                    p { 
                        margin-bottom: 1.2em; 
                    }
                    a {
                        color: var(--theme-primary);
                        text-decoration: none;
                    }
                    /* TTS Highlight Class */
                    .tts-highlight {
                        background-color: var(--theme-primary);
                        color: var(--theme-background);
                        border-radius: 4px;
                        padding: 2px 4px;
                        transition: background-color 0.2s ease;
                    }
                    /* Selection colors */
                    ::selection {
                        background: var(--theme-primary);
                        color: var(--theme-background);
                    }
                </style>
            
                <script>
                    function highlightTts(id) {
                        // Remove previous highlight
                        var prev = document.querySelector('.tts-highlight');
                        if (prev) {
                            prev.classList.remove('tts-highlight');
                        }
                        // Add new highlight
                        var curr = document.getElementById('tts-' + id);
                        if (curr) {
                            curr.classList.add('tts-highlight');
                            curr.scrollIntoView({ behavior: 'smooth', block: 'center' });
                        }
                    }
                </script>
            </head>
            <body>
                $rawContent
            </body>
            </html>
        """.trimIndent()
    }
}
