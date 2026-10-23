package ai.harika.aura

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

        val connection = URL(
            "https://html.duckduckgo.com/html/?q=" + URLEncoder.encode(cleanQuery, "UTF-8")
        ).openConnection() as HttpURLConnection
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

            // The class and href attributes may appear in either order.
            val anchorRegex = Regex(
                """(?is)<a\b(?=[^>]*\bclass\s*=\s*["'][^"']*\bresult__a\b[^"']*["'])[^>]*>(.*?)</a>"""
            )
            val hrefRegex = Regex("""(?is)\bhref\s*=\s*["']([^"']+)["']""")
            val snippetRegex = Regex(
                """(?is)class\s*=\s*["'][^"']*\bresult__snippet\b[^"']*["'][^>]*>(.*?)</(?:a|div|span)>"""
            )
            val links = anchorRegex.findAll(html).toList()
            val results = mutableListOf<WebSearchResult>()

            for ((index, match) in links.withIndex()) {
                val openingTag = match.value.substringBefore(">")
                val rawUrl = hrefRegex.find(openingTag)?.groupValues?.get(1)
                    ?.let(::decodeHtml)?.trim().orEmpty()
                if (rawUrl.isBlank()) continue

                val destination = unwrapUrl(rawUrl)
                val title = cleanHtml(match.groupValues[1])
                val parsedUri = runCatching { URI(destination) }.getOrNull()
                val scheme = parsedUri?.scheme?.lowercase()
                if (scheme !in setOf("http", "https") || parsedUri?.host.isNullOrBlank()) continue
                if (title.isBlank() || results.any { it.url == destination }) continue

                val end = links.getOrNull(index + 1)?.range?.first ?: html.length
                val resultBlock = html.substring(match.range.first, end)
                val snippet = snippetRegex.find(resultBlock)?.groupValues?.get(1)
                    ?.let(::cleanHtml).orEmpty()
                results.add(WebSearchResult(title, destination, snippet))
                if (results.size >= limit) break
            }
            return results
        } finally {
            connection.disconnect()
        }
    }

    private fun cleanHtml(value: String): String =
        Html.fromHtml(value, Html.FROM_HTML_MODE_LEGACY)
            .toString().replace(Regex("\\s+"), " ").trim()

    private fun decodeHtml(value: String): String =
        Html.fromHtml(value, Html.FROM_HTML_MODE_LEGACY).toString()

    private fun unwrapUrl(raw: String): String {
        val normalized = if (raw.startsWith("//")) "https:$raw" else raw
        val uri = runCatching { URI(normalized) }.getOrNull() ?: return normalized
        val host = uri.host.orEmpty().lowercase()
        if (host != "duckduckgo.com" && !host.endsWith(".duckduckgo.com")) return normalized

        val target = uri.rawQuery.orEmpty().split("&")
            .firstOrNull { it.startsWith("uddg=") }
            ?.substringAfter("uddg=")
            ?: return normalized

        return runCatching { URLDecoder.decode(target, "UTF-8") }.getOrDefault(normalized)
    }
}
