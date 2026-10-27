package com.dooglex.app.data

import com.dooglex.app.model.DohProvider
import com.dooglex.app.model.SearchResult
import java.io.IOException
import java.net.InetAddress
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.dnsoverhttps.DnsOverHttps
import org.json.JSONObject
import org.jsoup.Jsoup
import kotlin.math.max

class SearchUnavailableException(
    message: String,
    val httpCode: Int? = null,
) : IOException(message)

internal data class MetaSearchResponse(
    val results: List<SearchResult>,
    val googleNextStart: Int? = null,
    val googleError: String? = null,
)

internal data class GoogleSearchPage(
    val results: List<SearchResult>,
    val nextStart: Int? = null,
)

private data class GoogleSourceFetch(
    val sourceResults: MetaSourceResults? = null,
    val nextStart: Int? = null,
    val error: String? = null,
)

/** DoogleX metasearch: independent fetching, deduplication and ranking above source indexes. */
class DoogleXMetaSearchRepository {
    private val systemDnsClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    private val dohClients = ConcurrentHashMap<DohProvider, OkHttpClient>()

    private fun dohClient(provider: DohProvider): OkHttpClient = dohClients.getOrPut(provider) {
        val (endpoint, bootstrapHosts) = when (provider) {
            DohProvider.CLOUDFLARE ->
                "https://cloudflare-dns.com/dns-query" to listOf("1.1.1.1", "1.0.0.1")
            DohProvider.GOOGLE ->
                "https://dns.google/dns-query" to listOf("8.8.8.8", "8.8.4.4")
            DohProvider.QUAD9 ->
                "https://dns.quad9.net/dns-query" to listOf("9.9.9.9", "149.112.112.112")
        }
        val dns = DnsOverHttps.Builder()
            .client(systemDnsClient)
            .url(endpoint.toHttpUrl())
            // Numeric bootstrap addresses avoid a system-DNS lookup for the DoH resolver itself.
            .bootstrapDnsHosts(*bootstrapHosts.map { InetAddress.getByName(it) }.toTypedArray())
            .post(true)
            .resolvePrivateAddresses(false)
            .build()
        systemDnsClient.newBuilder().dns(dns).build()
    }

    internal suspend fun search(
        query: String,
        safeSearch: Boolean,
        useDoh: Boolean,
        dohProvider: DohProvider,
        googleApiKey: String = "",
        googleSearchEngineId: String = "",
    ): MetaSearchResponse = withContext(Dispatchers.IO) {
        val client = if (useDoh) dohClient(dohProvider) else systemDnsClient
        val mojeek = async {
            fetchSource { MetaSourceResults(MetaSource.MOJEEK, fetchMojeek(client, query, safeSearch)) }
        }
        val wikipedia = async {
            fetchSource { MetaSourceResults(MetaSource.WIKIPEDIA, fetchWikipedia(client, query)) }
        }
        val github = async {
            fetchSource { MetaSourceResults(MetaSource.GITHUB, fetchGitHub(client, query)) }
        }
        val google = if (googleApiKey.isNotBlank() && googleSearchEngineId.isNotBlank()) {
            async {
                fetchGoogleSource(
                    client = client,
                    query = query,
                    safeSearch = safeSearch,
                    apiKey = googleApiKey.trim(),
                    searchEngineId = googleSearchEngineId.trim(),
                    start = 1,
                )
            }
        } else {
            null
        }

        val googleResult = google?.await()
        val sources = listOfNotNull(
            googleResult?.sourceResults,
            mojeek.await(),
            wikipedia.await(),
            github.await(),
        )
        if (sources.isEmpty()) {
            throw SearchUnavailableException(
                googleResult?.error ?: "Ни один источник метапоиска не ответил",
            )
        }
        val ranked = MetaSearchRanker.rank(query, sources)
        MetaSearchResponse(
            results = if (safeSearch) SafeSearchFilter.filter(ranked) else ranked,
            googleNextStart = googleResult?.nextStart,
            googleError = googleResult?.error,
        )
    }

    internal suspend fun loadGoogleResults(
        query: String,
        start: Int,
        safeSearch: Boolean,
        useDoh: Boolean,
        dohProvider: DohProvider,
        apiKey: String,
        searchEngineId: String,
    ): GoogleSearchPage = withContext(Dispatchers.IO) {
        if (apiKey.isBlank() || searchEngineId.isBlank()) {
            throw SearchUnavailableException("Google API не настроен")
        }
        val client = if (useDoh) dohClient(dohProvider) else systemDnsClient
        val page = fetchGoogle(
            client = client,
            query = query,
            safeSearch = safeSearch,
            apiKey = apiKey.trim(),
            searchEngineId = searchEngineId.trim(),
            start = start,
        )
        page.copy(results = if (safeSearch) SafeSearchFilter.filter(page.results) else page.results)
    }

    private fun fetchSource(block: () -> MetaSourceResults): MetaSourceResults? = try {
        block()
    } catch (error: CancellationException) {
        throw error
    } catch (_: Exception) {
        // A captcha, rate limit or network failure in one source must not hide other results.
        null
    }

    private fun fetchMojeek(
        client: OkHttpClient,
        query: String,
        safeSearch: Boolean,
    ): List<SearchResult> {
        val url = MOJEEK_ENDPOINT.newBuilder()
            .addQueryParameter("q", query)
            .addQueryParameter("safe", if (safeSearch) "1" else "0")
            .build()
        return fetch(client, url, "text/html", MojeekParser::parse)
    }

    private fun fetchWikipedia(client: OkHttpClient, query: String): List<SearchResult> {
        val url = WIKIPEDIA_ENDPOINT.newBuilder()
            .addQueryParameter("action", "query")
            .addQueryParameter("list", "search")
            .addQueryParameter("srsearch", query)
            .addQueryParameter("srnamespace", "0")
            .addQueryParameter("srlimit", "5")
            .addQueryParameter("utf8", "1")
            .addQueryParameter("format", "json")
            .build()
        return fetch(client, url, "application/json") { body, _ ->
            WikipediaParser.parse(body)
        }
    }

    private fun fetchGitHub(client: OkHttpClient, query: String): List<SearchResult> {
        val url = GITHUB_ENDPOINT.newBuilder()
            .addQueryParameter("q", "$query in:name,description")
            .addQueryParameter("per_page", "4")
            .build()
        return fetch(client, url, "application/vnd.github+json") { body, _ ->
            GitHubParser.parse(body)
        }
    }

    private fun fetchGoogleSource(
        client: OkHttpClient,
        query: String,
        safeSearch: Boolean,
        apiKey: String,
        searchEngineId: String,
        start: Int,
    ): GoogleSourceFetch = try {
        val page = fetchGoogle(client, query, safeSearch, apiKey, searchEngineId, start)
        GoogleSourceFetch(
            sourceResults = MetaSourceResults(MetaSource.GOOGLE, page.results),
            nextStart = page.nextStart,
        )
    } catch (error: CancellationException) {
        throw error
    } catch (error: Exception) {
        GoogleSourceFetch(error = GoogleSearchErrors.message(error))
    }

    private fun fetchGoogle(
        client: OkHttpClient,
        query: String,
        safeSearch: Boolean,
        apiKey: String,
        searchEngineId: String,
        start: Int,
    ): GoogleSearchPage {
        val url = GOOGLE_ENDPOINT.newBuilder()
            .addQueryParameter("key", apiKey)
            .addQueryParameter("cx", searchEngineId)
            .addQueryParameter("q", query)
            .addQueryParameter("num", "10")
            .addQueryParameter("start", start.coerceAtLeast(1).toString())
            .addQueryParameter("safe", if (safeSearch) "active" else "off")
            .build()
        return fetch(client, url, "application/json") { body, _ ->
            GoogleParser.parse(body)
        }
    }

    private fun <T> fetch(
        client: OkHttpClient,
        url: HttpUrl,
        accept: String,
        parser: (String, HttpUrl) -> T,
    ): T {
        val request = Request.Builder()
            .url(url)
            .header("Accept", accept)
            .header("Accept-Language", "ru,en;q=0.8")
            .header("User-Agent", USER_AGENT)
            .build()
        return client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw SearchUnavailableException(
                    "Источник вернул ошибку ${response.code}",
                    httpCode = response.code,
                )
            }
            val body = response.body?.string().orEmpty()
            if (body.isBlank()) throw SearchUnavailableException("Источник вернул пустой ответ")
            parser(body, response.request.url)
        }
    }

    private companion object {
        val GOOGLE_ENDPOINT = "https://customsearch.googleapis.com/customsearch/v1".toHttpUrl()
        val MOJEEK_ENDPOINT = "https://www.mojeek.com/search".toHttpUrl()
        val WIKIPEDIA_ENDPOINT = "https://ru.wikipedia.org/w/api.php".toHttpUrl()
        val GITHUB_ENDPOINT = "https://api.github.com/search/repositories".toHttpUrl()
        const val USER_AGENT = "DoogleX/1.6 (Android; metasearch client)"
    }
}

internal enum class MetaSource(val displayName: String, val weight: Double) {
    GOOGLE("Google", 30.0),
    MOJEEK("Mojeek", 26.0),
    WIKIPEDIA("Wikipedia", 20.0),
    GITHUB("GitHub", 18.0),
}

internal data class MetaSourceResults(
    val source: MetaSource,
    val results: List<SearchResult>,
)

internal object GoogleParser {
    fun parse(json: String): GoogleSearchPage {
        val root = JSONObject(json)
        val items = root.optJSONArray("items")
        val results = buildList {
            if (items != null) {
                for (index in 0 until items.length()) {
                    val item = items.optJSONObject(index) ?: continue
                    createWebResult(
                        title = item.optString("title").trim(),
                        url = item.optString("link"),
                        snippet = item.optString("snippet").trim(),
                        source = MetaSource.GOOGLE.displayName,
                    )?.let(::add)
                }
            }
        }
        val nextPages = root.optJSONObject("queries")?.optJSONArray("nextPage")
        val nextStart = nextPages?.optJSONObject(0)?.optInt("startIndex")?.takeIf { it > 1 }
        return GoogleSearchPage(results = results, nextStart = nextStart)
    }
}

internal object GoogleSearchErrors {
    fun message(error: Exception): String {
        val code = (error as? SearchUnavailableException)?.httpCode
        return when (code) {
            400 -> "Google API отклонил запрос. Проверьте CX и включите поиск по всему вебу."
            401, 403 -> "Google API недоступен. Проверьте ключ, включение API и квоту аккаунта."
            429 -> "Лимит запросов Google API исчерпан. Остальные источники продолжают работать."
            else -> "Google API не ответил. Проверьте сеть, DoH и параметры подключения."
        }
    }
}

internal object MojeekParser {
    fun parse(html: String, requestUrl: HttpUrl): List<SearchResult> {
        if (html.contains("altcha", ignoreCase = true) ||
            html.contains("Verification required", ignoreCase = true)
        ) {
            throw SearchUnavailableException("Mojeek запросил проверку в браузере")
        }
        val document = Jsoup.parse(html, requestUrl.toString())
        return document.select("ul.results-standard > li").mapNotNull { element ->
            val anchor = element.selectFirst("a.ob") ?: return@mapNotNull null
            val title = element.selectFirst("h2")?.text()?.trim().orEmpty()
            val resolved = requestUrl.resolve(anchor.attr("href")) ?: return@mapNotNull null
            createWebResult(
                title = title.ifBlank { anchor.text().trim() },
                url = resolved.toString(),
                snippet = element.selectFirst("p.s")?.text()?.trim().orEmpty(),
                source = MetaSource.MOJEEK.displayName,
            )
        }.distinctBy { it.url }
    }
}

internal object WikipediaParser {
    fun parse(json: String): List<SearchResult> {
        val items = JSONObject(json).getJSONObject("query").getJSONArray("search")
        return buildList {
            for (index in 0 until items.length()) {
                val item = items.getJSONObject(index)
                val pageId = item.optInt("pageid")
                val title = item.optString("title").trim()
                if (pageId <= 0 || title.isBlank()) continue
                createWebResult(
                    title = title,
                    url = "https://ru.wikipedia.org/?curid=$pageId",
                    snippet = Jsoup.parse(item.optString("snippet")).text().trim(),
                    source = MetaSource.WIKIPEDIA.displayName,
                )?.let(::add)
            }
        }
    }
}

internal object GitHubParser {
    fun parse(json: String): List<SearchResult> {
        val items = JSONObject(json).optJSONArray("items") ?: return emptyList()
        return buildList {
            for (index in 0 until items.length()) {
                val item = items.getJSONObject(index)
                val title = item.optString("full_name").ifBlank { item.optString("name") }.trim()
                createWebResult(
                    title = title,
                    url = item.optString("html_url"),
                    snippet = item.optString("description").trim(),
                    source = MetaSource.GITHUB.displayName,
                )?.let(::add)
            }
        }
    }
}

private fun createWebResult(
    title: String,
    url: String,
    snippet: String,
    source: String,
): SearchResult? {
    val parsed = url.toHttpUrlOrNull() ?: return null
    if (parsed.scheme !in setOf("http", "https") || title.isBlank()) return null
    return SearchResult(
        title = title,
        url = parsed.toString(),
        domain = parsed.topPrivateDomain() ?: parsed.host,
        snippet = snippet,
        source = source,
    )
}

internal object MetaSearchRanker {
    private data class Candidate(
        var result: SearchResult,
        var bestScore: Double,
        val sources: MutableSet<MetaSource>,
    )

    private data class ScoredResult(val result: SearchResult, val score: Double)

    fun rank(
        query: String,
        sources: List<MetaSourceResults>,
        limit: Int = 30,
    ): List<SearchResult> {
        if (limit <= 0) return emptyList()
        val merged = LinkedHashMap<String, Candidate>()

        sources.forEach { sourceResults ->
            sourceResults.results.forEachIndexed { index, result ->
                val score = sourceResults.source.weight +
                    50.0 / (index + 1) +
                    lexicalScore(query, result)
                val key = canonicalMetaSearchUrl(result.url)
                val existing = merged[key]
                if (existing == null) {
                    merged[key] = Candidate(result, score, mutableSetOf(sourceResults.source))
                } else {
                    existing.bestScore = max(existing.bestScore, score)
                    existing.sources += sourceResults.source
                    if (result.snippet.length > existing.result.snippet.length) {
                        existing.result = result
                    }
                }
            }
        }

        val remaining = merged.values.map { candidate ->
            val label = candidate.sources.sortedBy { it.ordinal }.joinToString(" + ") { it.displayName }
            ScoredResult(
                result = candidate.result.copy(source = label),
                score = candidate.bestScore + (candidate.sources.size - 1) * 22.0,
            )
        }.sortedWith(
            compareByDescending<ScoredResult> { it.score }.thenBy { it.result.url },
        ).toMutableList()

        val selected = mutableListOf<SearchResult>()
        val domainCounts = mutableMapOf<String, Int>()
        while (remaining.isNotEmpty() && selected.size < limit) {
            val next = remaining.maxByOrNull { candidate ->
                val domain = candidate.result.domain.lowercase()
                candidate.score - (domainCounts[domain] ?: 0) * 14.0
            } ?: break
            remaining.remove(next)
            selected += next.result
            val domain = next.result.domain.lowercase()
            domainCounts[domain] = (domainCounts[domain] ?: 0) + 1
        }
        return selected
    }

    private fun lexicalScore(query: String, result: SearchResult): Double {
        val phrase = query.trim().lowercase()
        val title = result.title.lowercase()
        val snippet = result.snippet.lowercase()
        val domain = result.domain.lowercase()
        val tokens = phrase.split(Regex("[^\\p{L}\\p{N}]+")).filter { it.length >= 2 }
        var score = if (phrase.length >= 3 && title.contains(phrase)) 12.0 else 0.0
        tokens.forEach { token ->
            if (title.contains(token)) score += 8.0
            if (snippet.contains(token)) score += 3.0
            if (domain.contains(token)) score += 2.0
        }
        return score
    }

}

internal fun canonicalMetaSearchUrl(raw: String): String {
    val url = raw.toHttpUrlOrNull() ?: return raw.trim()
    val defaultPort = if (url.scheme == "https") 443 else 80
    val port = if (url.port == defaultPort) "" else ":${url.port}"
    val path = url.encodedPath.trimEnd('/')
    val query = (0 until url.querySize)
        .map { index -> url.queryParameterName(index) to url.queryParameterValue(index) }
        .filterNot { (name, _) ->
            name.startsWith("utm_") || name in setOf("fbclid", "gclid", "yclid")
        }
        .sortedWith(compareBy<Pair<String, String?>> { it.first }.thenBy { it.second.orEmpty() })
        .joinToString("&") { (name, value) -> "$name=${value.orEmpty()}" }
    return "${url.host.removePrefix("www.")}$port$path|$query"
}

internal object MetaSearchResultMerger {
    fun append(existing: List<SearchResult>, additional: List<SearchResult>): List<SearchResult> {
        val merged = LinkedHashMap<String, SearchResult>()
        existing.forEach { result -> merged[canonicalMetaSearchUrl(result.url)] = result }
        additional.forEach { candidate ->
            val key = canonicalMetaSearchUrl(candidate.url)
            val previous = merged[key]
            if (previous == null) {
                merged[key] = candidate
            } else {
                val preferred = if (candidate.snippet.length > previous.snippet.length) {
                    candidate
                } else {
                    previous
                }
                merged[key] = preferred.copy(
                    source = combineSources(previous.source, candidate.source),
                )
            }
        }
        return merged.values.toList()
    }

    private fun combineSources(first: String, second: String): String {
        val labels = (first.split(" + ") + second.split(" + "))
            .filter { it.isNotBlank() }
            .toSet()
        val known = MetaSource.entries.filter { it.displayName in labels }.map { it.displayName }
        val unknown = labels.filterNot { label ->
            MetaSource.entries.any { it.displayName == label }
        }.sorted()
        return (known + unknown).joinToString(" + ")
    }
}

internal object SafeSearchFilter {
    private val adultMarkers = listOf("porn", "porno", "pornography", "xxx", "порно")

    fun filter(results: List<SearchResult>): List<SearchResult> = results.filterNot { result ->
        val text = "${result.title} ${result.snippet} ${result.url}".lowercase()
        adultMarkers.any { marker -> text.contains(marker) }
    }
}
