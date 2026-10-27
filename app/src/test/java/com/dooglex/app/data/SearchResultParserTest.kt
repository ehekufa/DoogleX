package com.dooglex.app.data

import okhttp3.HttpUrl.Companion.toHttpUrl
import org.junit.Assert.assertEquals
import org.junit.Test

class SearchResultParserTest {
    private val requestUrl = "https://html.duckduckgo.com/html/?q=kotlin".toHttpUrl()

    @Test
    fun `parses direct and redirected results`() {
        val html = """
            <html><body>
              <div class="result results_links">
                <a class="result__a" href="//duckduckgo.com/l/?uddg=https%3A%2F%2Fru.wikipedia.org%2Fwiki%2FKotlin&amp;rut=abc">Kotlin — Википедия</a>
                <a class="result__snippet">Статья о языке программирования &amp; его возможностях.</a>
              </div>
              <div class="result results_links">
                <a class="result__a" href="https://kotlinlang.org/">Kotlin programming language</a>
                <div class="result__snippet">Official Kotlin website.</div>
              </div>
            </body></html>
        """.trimIndent()

        val results = SearchResultParser.parse(html, requestUrl)

        assertEquals(2, results.size)
        assertEquals("Kotlin — Википедия", results[0].title)
        assertEquals("https://ru.wikipedia.org/wiki/Kotlin", results[0].url)
        assertEquals("wikipedia.org", results[0].domain)
        assertEquals("Статья о языке программирования & его возможностях.", results[0].snippet)
        assertEquals("https://kotlinlang.org/", results[1].url)
        assertEquals("kotlinlang.org", results[1].domain)
    }

    @Test(expected = SearchUnavailableException::class)
    fun `reports anti-bot challenge as unavailable search`() {
        SearchResultParser.parse("<html><div class='anomaly-modal'>captcha</div></html>", requestUrl)
    }
}
