package com.dooglex.app.data

import com.dooglex.app.model.SearchResult
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MetaSearchTest {
    private val mojeekUrl = "https://www.mojeek.com/search?q=kotlin".toHttpUrl()

    @Test
    fun `parses independent Mojeek results`() {
        val html = """
            <html><body><ul class="results-standard">
              <li>
                <a class="ob" href="https://kotlinlang.org/"></a>
                <h2><a href="https://kotlinlang.org/">Kotlin programming language</a></h2>
                <p class="s">Official Kotlin website.</p>
              </li>
            </ul></body></html>
        """.trimIndent()

        val results = MojeekParser.parse(html, mojeekUrl)

        assertEquals(1, results.size)
        assertEquals("Kotlin programming language", results[0].title)
        assertEquals("https://kotlinlang.org/", results[0].url)
        assertEquals("kotlinlang.org", results[0].domain)
        assertEquals("Mojeek", results[0].source)
    }

    @Test(expected = SearchUnavailableException::class)
    fun `reports Mojeek anti-bot challenge without breaking other sources`() {
        MojeekParser.parse("<html><title>Captcha</title><div>Verification required</div></html>", mojeekUrl)
    }

    @Test
    fun `parses Wikipedia and GitHub sources`() {
        val wikipedia = WikipediaParser.parse(
            """{"query":{"search":[{"title":"Kotlin","pageid":123,"snippet":"Язык <span class=\"searchmatch\">Kotlin</span>"}]}}""",
        )
        val github = GitHubParser.parse(
            """{"items":[{"full_name":"ehekufa/DoogleX","html_url":"https://github.com/ehekufa/DoogleX","description":"Android metasearch"}]}""",
        )

        assertEquals("https://ru.wikipedia.org/?curid=123", wikipedia[0].url)
        assertEquals("Язык Kotlin", wikipedia[0].snippet)
        assertEquals("Wikipedia", wikipedia[0].source)
        assertEquals("ehekufa/DoogleX", github[0].title)
        assertEquals("GitHub", github[0].source)
    }

    @Test
    fun `deduplicates URLs and credits every contributing source`() {
        val fromMojeek = SearchResult(
            title = "DoogleX",
            url = "https://github.com/ehekufa/DoogleX/?utm_source=test",
            domain = "github.com",
            snippet = "Short",
        )
        val fromGitHub = SearchResult(
            title = "DoogleX",
            url = "https://github.com/ehekufa/DoogleX",
            domain = "github.com",
            snippet = "Much longer repository description",
        )

        val ranked = MetaSearchRanker.rank(
            query = "DoogleX",
            sources = listOf(
                MetaSourceResults(MetaSource.MOJEEK, listOf(fromMojeek)),
                MetaSourceResults(MetaSource.GITHUB, listOf(fromGitHub)),
            ),
        )

        assertEquals(1, ranked.size)
        assertEquals("Mojeek + GitHub", ranked[0].source)
        assertEquals("Much longer repository description", ranked[0].snippet)
    }

    @Test
    fun `safe filter removes obvious adult results`() {
        val clean = SearchResult("Android", "https://example.org/android", "example.org", "Phone")
        val adult = SearchResult("XXX", "https://example.org/adult", "example.org", "Adult")
        val filtered = SafeSearchFilter.filter(listOf(clean, adult))
        assertEquals(1, filtered.size)
        assertFalse(filtered[0].title.contains("XXX"))
        assertTrue(filtered[0].title.contains("Android"))
    }
}
