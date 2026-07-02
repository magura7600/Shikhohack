package com.example

import org.junit.Test
import java.net.URL
import java.net.HttpURLConnection
import java.net.URLEncoder

class TiooTest {

    private fun resolveRedirect(urlStr: String): String {
        val trimmed = urlStr.trim()
        try {
            var currentUrl = trimmed
            var redirects = 0
            while (redirects < 5) {
                val url = URL(currentUrl)
                val connection = url.openConnection() as HttpURLConnection
                connection.instanceFollowRedirects = false
                connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                connection.connectTimeout = 5000
                connection.readTimeout = 5000
                
                val responseCode = connection.responseCode
                if (responseCode in 300..399) {
                    val nextUrl = connection.getHeaderField("Location")
                    if (nextUrl != null) {
                        redirects++
                        connection.disconnect()
                        currentUrl = nextUrl.replace("&amp;", "&")
                    } else {
                        connection.disconnect()
                        break
                    }
                } else {
                    connection.disconnect()
                    break
                }
            }
            return currentUrl.replace("&amp;", "&")
        } catch (e: Exception) {
            return trimmed.replace("&amp;", "&")
        }
    }

    @Test
    fun testTioo() {
        val urlStr = "https://www.facebook.com/share/r/18ukpBcQyU/"
        val resolved = resolveRedirect(urlStr)
        System.err.println("Resolved: $resolved")
        
        try {
            val url = URL(resolved)
            val connection = url.openConnection() as HttpURLConnection
            connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
            connection.setRequestProperty("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8")
            connection.setRequestProperty("Accept-Language", "en-US,en;q=0.5")
            
            if (connection.responseCode in 200..299) {
                val html = connection.inputStream.bufferedReader().readText()
                val sdVideoRegex = """"browser_native_sd_url"\s*:\s*"([^"]+)"""".toRegex()
                val hdVideoRegex = """"browser_native_hd_url"\s*:\s*"([^"]+)"""".toRegex()
                val playableUrlRegex = """"playable_url"\s*:\s*"([^"]+)"""".toRegex()
                val playableUrlHdRegex = """"playable_url_quality_hd"\s*:\s*"([^"]+)"""".toRegex()
                
                System.err.println("SD: ${sdVideoRegex.find(html)?.groups?.get(1)?.value}")
                System.err.println("HD: ${hdVideoRegex.find(html)?.groups?.get(1)?.value}")
                System.err.println("Playable: ${playableUrlRegex.find(html)?.groups?.get(1)?.value}")
                System.err.println("PlayableHD: ${playableUrlHdRegex.find(html)?.groups?.get(1)?.value}")
            }
        } catch(e:Exception){}
    }
}
