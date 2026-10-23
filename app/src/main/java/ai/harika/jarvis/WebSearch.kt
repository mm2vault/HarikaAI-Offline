package ai.harika.jarvis

import android.text.Html
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

data class WebSearchResult(
    val title: String,
    val url: String,
    val snippet: String
)

/**
 * Small API-key-free DuckDuckGo HTML search adapter.
 * Network failures are surfaced to the caller; no fabricated fallback results.
 */
object WebSearch {
    fun search(query: String, limit: Int = 5): List<WebSearchResult> {
        val cleanQuery = query.trim().replace(Regex("\\s+"), " ")
        require(cleanQuery.isNotEmpty()) { "Arama sorgusu boş olamaz." }
        require(limit in 1..10) { "Sonuç sayısı 1-10 arasında olmalı." }

        val encoded = URLEncoder.encode(cleanQuery, "UTF-8")
        val connection = (URL("https://html.duckduckgo.com/html/?q=$encoded").openConnection() as HttpURLConnection)
        connection.connectTimeout = 8000
        connection.readTimeout = 10000
        connection.instanceFollowRedirects = true
        connection.setRequestProperty("User-Agent", "JarvisAI-Android/0.1")
        connection.setRequestProperty("Accept", "text/html")
        try {
            val code = connection.responseCode
            if (code !in 200..299) throw IllegalStateException("Arama sağlayıcısı HTTP $code döndürdü.")
            val html = connection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText().take(2_000_000) }
            val results = mutableListOf<WebSearchResult>()
            val blocks = Regex(
                """(?is)<div[^>]*class=["'][^"']*result[^"']*["'][^>]*>(.*?)(?=<div[^>]*class=["'][^"']*result[^"']*["']|\\z)"""
            )
            val linkRegex = Regex("""(?is)<a[^>]*class=["'][^"']*result__a[^"']*["'][^>]*href=["']([^"']+)["'][^>]*>(.*?)</a>""")
            val snippetRegex = Regex("""(?is)<(?:a|div)[^>]*class=["'][^"']*result__snippet[^"']*["'][^>]*>(.*?)</(?:a|div)>""")
            for (match in blocks.findAll(html)) {
                val block = match.groupValues[1]
                val link = linkRegex.find(block) ?: continue
                val rawUrl = Html.fromHtml(link.groupValues[1], Html.FROM_HTML_MODE_LEGACY).toString().trim()
                val title = Html.fromHtml(link.groupValues[2].replace(Regex("(?is)<[^>]+>"), ""), Html.FROM_HTML_MODE_LEGACY)
                    .toString().replace(Regex("\\s+"), " ").trim()
                val snippet = snippetRegex.find(block)?.groupValues?.get(1)?.let {
                    Html.fromHtml(it.replace(Regex("(?is)<[^>]+>"), ""), Html.FROM_HTML_MODE_LEGACY)
                        .toString().replace(Regex("\\s+"), " ").trim()
                }.orEmpty()
                val destination = unwrapUrl(rawUrl)
                if ((destination.startsWith("https://") || destination.startsWith("http://")) && title.isNotBlank()) {
                    results.add(WebSearchResult(title, destination, snippet))
                    if (results.size >= limit) break
                }
            }
            return results
        } finally {
            connection.disconnect()
        }
    }

    private fun unwrapUrl(raw: String): String {
        val uri = runCatching { java.net.URI(raw) }.getOrNull() ?: return raw
        val query = uri.rawQuery.orEmpty().split("&")
        val target = query.firstOrNull { it.startsWith("uddg=") }?.substringAfter("uddg=")
        if (target != null) return runCatching {
            java.net.URLDecoder.decode(target, "UTF-8")
        }.getOrDefault(raw)
        return raw
    }
}
