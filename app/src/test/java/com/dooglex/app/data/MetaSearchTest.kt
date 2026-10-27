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
    fun `parses Google web results and next page`() {
        val page = GoogleParser.parse(
            """
            {
              "queries": {"nextPage": [{"startIndex": 11}]},
              "items": [{
                "title": "Small independent site",
                "link": "https://blog.example.net/post",
                "displayLink": "blog.example.net",
                "snippet": "A long-tail web result"
              }]
            }
            """.trimIndent(),
        )

        assertEquals(1, page.results.size)
        assertEquals(11, page.nextStart)
        assertEquals("Small independent site", page.results[0].title)
        assertEquals("https://blog.example.net/post", page.results[0].url)
        assertEquals("example.net", page.results[0].domain)
        assertEquals("Google", page.results[0].source)
    }

    @Test
    fun `handles empty Google page without inventing pagination`() {
        val page = GoogleParser.parse("""{"queries":{"request":[{"startIndex":1}]}}""")

        assertTrue(page.results.isEmpty())
        assertEquals(null, page.nextStart)
    }

    @Test
    fun `appends next Google page and merges source credit`() {
        val existing = SearchResult(
            title = "DoogleX",
            url = "https://github.com/ehekufa/DoogleX/?utm_source=test",
            domain = "github.com",
            snippet = "Short",
            source = "Google",
        )
        val duplicate = SearchResult(
            title = "DoogleX",
            url = "https://github.com/ehekufa/DoogleX",
            domain = "github.com",
            snippet = "Much longer repository description",
            source = "Mojeek",
        )
        val newResult = SearchResult(
            title = "Independent site",
            url = "https://small.example.org/",
            domain = "small.example.org",
            snippet = "Long-tail result",
            source = "Google",
        )

        val merged = MetaSearchResultMerger.append(
            existing = listOf(existing),
            additional = listOf(duplicate, newResult),
        )

        assertEquals(2, merged.size)
        assertEquals("Google + Mojeek", merged[0].source)
        assertEquals("Much longer repository description", merged[0].snippet)
        assertEquals("Independent site", merged[1].title)
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
