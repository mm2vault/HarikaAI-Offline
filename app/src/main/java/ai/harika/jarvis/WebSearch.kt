package ai.harika.jarvis

import android.text.Html
import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import java.net.URI
import java.net.URL
import java.net.URLDecoder
import java.net.URLEncoder

data class WebSearchResult(
    val title: String,
    val url: String,
    val snippet: String
)

/**
 * API-key-free HTML search adapter.
 * Network failures are surfaced to the caller; no fabricated fallback results.
 */
object WebSearch {
    private const val MAX_RESPONSE_BYTES = 2_000_000

    fun search(query: String, limit: Int = 5): List<WebSearchResult> {
        val cleanQuery = query.trim().replace(Regex("\\s+"), " ")
        require(cleanQuery.isNotEmpty()) { "Arama sorgusu boş olamaz." }
        require(limit in 1..10) { "Sonuç sayısı 1-10 arasında olmalı." }

        val encoded = URLEncoder.encode(cleanQuery, "UTF-8")
        val connection = URL("https://html.duckduckgo.com/html/?q=$encoded")
            .openConnection() as HttpURLConnection
        connection.connectTimeout = 8000
        connection.readTimeout = 10000
        connection.instanceFollowRedirects = true
        connection.setRequestProperty(
            "User-Agent",
            "Mozilla/5.0 (Linux; Android 10) AppleWebKit/537.36 Chrome/120 Mobile Safari/537.36"
        )
        connection.setRequestProperty("Accept", "text/html,application/xhtml+xml")

        try {
            val code = connection.responseCode
            if (code !in 200..299) {
                throw IllegalStateException("Arama sağlayıcısı HTTP $code döndürdü.")
            }
            val html = connection.inputStream.use { input ->
                val output = ByteArrayOutputStream()
                val buffer = ByteArray(8192)
                var total = 0
                while (total < MAX_RESPONSE_BYTES) {
                    val read = input.read(buffer, 0, minOf(buffer.size, MAX_RESPONSE_BYTES - total))
                    if (read < 0) break
                    output.write(buffer, 0, read)
                    total += read
                }
                output.toString(Charsets.UTF_8.name())
            }

            val linkRegex = Regex(
                """(?is)<a\b[^>]*class=["'][^"']*result__a[^"']*["'][^>]*href=["']([^"']+)["'][^>]*>(.*?)</a>"""
            )
            val snippetRegex = Regex(
                """(?is)class=["'][^"']*result__snippet[^"']*["'][^>]*>(.*?)</(?:a|div|span)>"""
            )
            val links = linkRegex.findAll(html).toList()
            val results = mutableListOf<WebSearchResult>()

            for ((index, match) in links.withIndex()) {
                val rawUrl = decodeHtml(match.groupValues[1]).trim()
                val destination = unwrapUrl(rawUrl)
                val title = cleanHtml(match.groupValues[2])
                if (!destination.startsWith("https://") && !destination.startsWith("http://")) continue
                if (title.isBlank() || results.any { it.url == destination }) continue

                // Keep snippet parsing within this result, so it cannot borrow another result's text.
                val end = links.getOrNull(index + 1)?.range?.first ?: html.length
                val resultBlock = html.substring(match.range.first, end)
                val snippet = snippetRegex.find(resultBlock)?.groupValues?.get(1)?.let(::cleanHtml).orEmpty()
                results.add(WebSearchResult(title, destination, snippet))
                if (results.size >= limit) break
            }
            return results
        } finally {
            connection.disconnect()
        }
    }

    private fun cleanHtml(value: String): String =
        Html.fromHtml(value.replace(Regex("(?is)<[^>]+>"), ""), Html.FROM_HTML_MODE_LEGACY)
            .toString().replace(Regex("\\s+"), " ").trim()

    private fun decodeHtml(value: String): String =
        Html.fromHtml(value, Html.FROM_HTML_MODE_LEGACY).toString()

    private fun unwrapUrl(raw: String): String {
        val normalized = if (raw.startsWith("//")) "https:$raw" else raw
        val uri = runCatching { URI(normalized) }.getOrNull() ?: return normalized
        if (!uri.host.orEmpty().contains("duckduckgo.com")) return normalized

        val target = uri.rawQuery.orEmpty().split("&")
            .firstOrNull { it.startsWith("uddg=") }
            ?.substringAfter("uddg=")
            ?: return normalized

        return runCatching { URLDecoder.decode(target, "UTF-8") }.getOrDefault(normalized)
    }
}
