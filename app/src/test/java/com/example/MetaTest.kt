package com.example

import org.junit.Test
import java.net.URL
import java.net.HttpURLConnection

class MetaTest {
    @Test
    fun testMeta() {
        val url = URL("https://www.facebook.com/shikho.bangladesh/videos/1556415209387103/")
        val connection = url.openConnection() as HttpURLConnection
        connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
        connection.setRequestProperty("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8")
        connection.setRequestProperty("Accept-Language", "en-US,en;q=0.5")
        
        if (connection.responseCode in 200..299) {
            val html = connection.inputStream.bufferedReader().readText()
            println("HTML Length: ${html.length}")
            val sdVideoRegex = """"browser_native_sd_url"\s*:\s*"([^"]+)"""".toRegex()
            val hdVideoRegex = """"browser_native_hd_url"\s*:\s*"([^"]+)"""".toRegex()
            val playableUrlRegex = """"playable_url"\s*:\s*"([^"]+)"""".toRegex()
            val playableUrlHdRegex = """"playable_url_quality_hd"\s*:\s*"([^"]+)"""".toRegex()
            
            println("SD: ${sdVideoRegex.find(html)?.groups?.get(1)?.value}")
            println("HD: ${hdVideoRegex.find(html)?.groups?.get(1)?.value}")
            println("Playable: ${playableUrlRegex.find(html)?.groups?.get(1)?.value}")
            println("PlayableHD: ${playableUrlHdRegex.find(html)?.groups?.get(1)?.value}")
        }
    }
}
