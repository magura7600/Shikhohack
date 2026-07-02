package com.example

import org.junit.Test
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

class ExampleUnitTest {
    
    private fun shouldResolveRedirect(url: String): Boolean {
        val lower = url.lowercase()
        return lower.contains("fb.watch") || 
               lower.contains("facebook.com/share") || 
               lower.contains("fb.me") || 
               (lower.contains("fb.com") && !lower.contains("facebook.com/reel") && !lower.contains("facebook.com/watch") && !lower.contains("facebook.com/videos"))
    }

    private fun resolveRedirect(urlStr: String): String {
        val trimmed = urlStr.trim()
        if (!shouldResolveRedirect(trimmed)) {
            return trimmed
        }
        try {
            var currentUrl = trimmed
            for (i in 0..4) {
                val connection = java.net.URL(currentUrl).openConnection() as java.net.HttpURLConnection
                connection.instanceFollowRedirects = false
                connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                connection.connectTimeout = 6000
                connection.readTimeout = 6000
                
                val responseCode = connection.responseCode
                if (responseCode in 300..399) {
                    val loc = connection.getHeaderField("Location")
                    if (!loc.isNullOrBlank()) {
                        val nextUrl = if (loc.startsWith("/")) {
                            val originalUrl = java.net.URL(currentUrl)
                            "${originalUrl.protocol}://${originalUrl.host}$loc"
                        } else {
                            loc
                        }
                        
                        if (nextUrl.contains("login") || nextUrl.contains("checkpoint") || nextUrl.contains("cookie")) {
                            connection.disconnect()
                            break
                        }
                        currentUrl = nextUrl
                    } else {
                        connection.disconnect()
                        break
                    }
                } else {
                    connection.disconnect()
                    break
                }
                connection.disconnect()
            }
            return currentUrl
        } catch (e: Exception) {
            return trimmed
        }
    }

    @Test
    fun fetchFbHtml() {
        try {
            val shareUrl = "https://www.facebook.com/share/v/19AExLG3zX/"
            var resolved = resolveRedirect(shareUrl)
            resolved = resolved.replace("&amp;", "&")
            println("INITIAL SHARE URL: $shareUrl")
            println("RESOLVED REDIRECT: $resolved")
            
            // Try Direct Meta Extraction
            val url = java.net.URL(resolved)
            val connection = url.openConnection() as java.net.HttpURLConnection
            connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
            connection.setRequestProperty("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8")
            connection.setRequestProperty("Accept-Language", "en-US,en;q=0.5")
            connection.connectTimeout = 10000
            connection.readTimeout = 10000
            
            println("Direct FB Response Code: ${connection.responseCode}")
            if (connection.responseCode in 200..299) {
                val html = connection.inputStream.bufferedReader().readText()
                java.io.File("fb_direct.html").writeText(html)
                
                val ogVideoRegex = """<meta\s+property="og:video:secure_url"\s+content="([^"]+)"""".toRegex()
                val ogVideoRegex2 = """<meta\s+property="og:video"\s+content="([^"]+)"""".toRegex()
                val sdVideoRegex = """"browser_native_sd_url"\s*:\s*"([^"]+)"""".toRegex()
                val hdVideoRegex = """"browser_native_hd_url"\s*:\s*"([^"]+)"""".toRegex()
                
                var videoUrl = ogVideoRegex.find(html)?.groups?.get(1)?.value
                    ?: ogVideoRegex2.find(html)?.groups?.get(1)?.value
                
                var sdUrl = sdVideoRegex.find(html)?.groups?.get(1)?.value
                var hdUrl = hdVideoRegex.find(html)?.groups?.get(1)?.value
                
                videoUrl = videoUrl?.replace("&amp;", "&")?.replace("\\/", "/")
                sdUrl = sdUrl?.replace("&amp;", "&")?.replace("\\/", "/")
                hdUrl = hdUrl?.replace("&amp;", "&")?.replace("\\/", "/")
                
                println("og:video Extracted URL: $videoUrl")
                println("browser_native_sd_url Extracted URL: $sdUrl")
                println("browser_native_hd_url Extracted URL: $hdUrl")
                
                // Let's also search for any other video strings
                val regex3 = """"playable_url"\s*:\s*"([^"]+)"""".toRegex()
                val regex4 = """"playable_url_quality_hd"\s*:\s*"([^"]+)"""".toRegex()
                val playable = regex3.find(html)?.groups?.get(1)?.value?.replace("\\/", "/")
                val playableHd = regex4.find(html)?.groups?.get(1)?.value?.replace("\\/", "/")
                println("playable_url: $playable")
                println("playable_url_quality_hd: $playableHd")
            } else {
                val err = connection.errorStream?.bufferedReader()?.readText()
                println("Direct FB Error: $err")
            }
            
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
