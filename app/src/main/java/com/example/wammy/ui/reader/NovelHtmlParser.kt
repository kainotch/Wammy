package com.example.wammy.ui.reader

import org.jsoup.Jsoup

object NovelHtmlParser {
    
    fun injectTtsSpans(rawHtml: String): Pair<String, List<String>> {
        val document = Jsoup.parseBodyFragment(rawHtml)
        var sentenceId = 0
        val sentencesList = mutableListOf<String>()

        val textNodes = document.body().select("*").flatMap { it.textNodes() }.toList()

        for (textNode in textNodes) {
            val text = textNode.text()
            if (text.isBlank()) continue

            // Split by sentence ending punctuation
            val sentences = text.split(Regex("(?<=[.!?])\\s+"))
            
            var newHtml = ""
            for (sentence in sentences) {
                if (sentence.isNotBlank()) {
                    newHtml += "<span id='tts-$sentenceId'>$sentence</span> "
                    sentencesList.add(sentence.trim())
                    sentenceId++
                }
            }

            if (newHtml.isNotBlank() && textNode.parent() != null) {
                val parsedNodes = org.jsoup.parser.Parser.parseFragment(newHtml, textNode.parent() as org.jsoup.nodes.Element, document.baseUri())
                var prevNode: org.jsoup.nodes.Node = textNode
                for (node in parsedNodes) {
                    prevNode.after(node)
                    prevNode = node
                }
                textNode.remove()
            }
        }
        
        return Pair(document.body().html(), sentencesList)
    }
}
