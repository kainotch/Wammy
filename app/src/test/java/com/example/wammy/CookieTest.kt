package com.example.wammy
import org.junit.Test
import okhttp3.Cookie
import okhttp3.HttpUrl.Companion.toHttpUrl

class CookieTest {
    @Test
    fun testParse() {
        val url = "https://novelfull.com".toHttpUrl()
        val c1 = Cookie.parse(url, "cf_clearance=123")
        val c2 = Cookie.parse(url, " cf_clearance=123")
        println("c1: $c1")
        println("c2: $c2")
    }
}
