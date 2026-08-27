// Created by Notch
package eu.kanade.tachiyomi.util

import okhttp3.Response
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element

fun Response.asJsoup(html: String = body!!.string()): Document {
    return Jsoup.parse(html, request.url.toString())
}

fun Element.selectText(cssQuery: String): String {
    return this.select(cssQuery).text()
}
