import org.jsoup.Jsoup
import java.io.File

fun main() {
    try {
        val doc = Jsoup.connect("https://www.instagram.com/kainotch/")
            .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
            .get()
        val url = doc.select("meta[property=og:image]").attr("content")
        println("URL: $url")
    } catch (e: Exception) {
        e.printStackTrace()
    }
}
