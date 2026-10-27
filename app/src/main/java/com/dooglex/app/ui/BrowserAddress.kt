package com.dooglex.app.ui

import java.net.URI
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

/** Resolves a browser omnibox value to an HTTPS page or a DuckDuckGo search. */
internal object BrowserAddress {
    private val explicitScheme = Regex("^([A-Za-z][A-Za-z0-9+.-]*)://")

    fun directUrlOrNull(input: String): String? {
        val value = input.trim()
        if (value.isEmpty()) return null

        if (explicitScheme.containsMatchIn(value)) {
            val uri = runCatching { URI(value) }.getOrNull() ?: return null
            val scheme = uri.scheme?.lowercase() ?: return null
            if (scheme !in setOf("http", "https") || uri.host.isNullOrBlank()) return null
            return if (scheme == "http") {
                // Keep the original path, query and fragment byte-for-byte.
                "https${value.substring(value.indexOf(':'))}"
            } else {
                uri.toASCIIString()
            }
        }

        if (value.any(Char::isWhitespace)) return null
        val candidate = runCatching { URI("https://$value") }.getOrNull() ?: return null
        val host = candidate.host
        if (host.isNullOrBlank() || !host.looksLikeAddress()) return null
        return candidate.toASCIIString()
    }

    fun resolve(input: String): String? {
        val value = input.trim()
        if (value.isEmpty()) return null
        directUrlOrNull(value)?.let { return it }
        if (explicitScheme.containsMatchIn(value)) return null

        val query = URLEncoder.encode(value, StandardCharsets.UTF_8.name()).replace("+", "%20")
        return "https://duckduckgo.com/?q=$query"
    }

    private fun String.looksLikeAddress(): Boolean =
        contains('.') || contains(':') || equals("localhost", ignoreCase = true)
}
