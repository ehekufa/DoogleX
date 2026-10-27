package com.dooglex.app.data

import com.dooglex.app.model.DohProvider
import com.dooglex.app.model.SearchResult
import java.io.IOException
import java.net.InetAddress
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.dnsoverhttps.DnsOverHttps
import org.jsoup.Jsoup

class SearchUnavailableException(message: String) : IOException(message)

class DuckDuckGoSearchRepository {
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

    suspend fun search(
        query: String,
        safeSearch: Boolean,
        useDoh: Boolean,
        dohProvider: DohProvider,
    ): List<SearchResult> = withContext(Dispatchers.IO) {
        val client = if (useDoh) dohClient(dohProvider) else systemDnsClient
        val url = SEARCH_ENDPOINT.toHttpUrl().newBuilder()
            .addQueryParameter("q", query)
            .addQueryParameter("kp", if (safeSearch) "1" else "-2")
            .build()
        val request = Request.Builder()
            .url(url)
            .header("Accept", "text/html")
            .header("User-Agent", USER_AGENT)
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw SearchUnavailableException("Поисковый сервер вернул ошибку ${response.code}")
            }
            val html = response.body?.string().orEmpty()
            if (html.isBlank()) {
                throw SearchUnavailableException("Поисковый сервер вернул пустой ответ")
            }
            SearchResultParser.parse(html, response.request.url)
        }
    }

    private companion object {
        const val SEARCH_ENDPOINT = "https://html.duckduckgo.com/html/"
        const val USER_AGENT = "DoogleX/1.3 (Android; native search client)"
    }
}

internal object SearchResultParser {
    fun parse(html: String, requestUrl: HttpUrl): List<SearchResult> {
        if (html.contains("anomaly-modal", ignoreCase = true) ||
            html.contains("captcha", ignoreCase = true)
        ) {
            throw SearchUnavailableException(
                "Поисковый сервер временно ограничил запросы. Попробуйте ещё раз позже.",
            )
        }

        val document = Jsoup.parse(html, requestUrl.toString())
        return document.select(".result")
            .mapNotNull { element ->
                val anchor = element.selectFirst("a.result__a") ?: return@mapNotNull null
                val title = anchor.text().trim()
                val rawUrl = anchor.attr("href")
                val resolvedUrl = requestUrl.resolve(rawUrl) ?: return@mapNotNull null
                val resultUrl = resolvedUrl.queryParameter("uddg") ?: resolvedUrl.toString()
                val parsedResultUrl = resultUrl.toHttpUrlOrNull() ?: return@mapNotNull null
                if (parsedResultUrl.scheme != "http" && parsedResultUrl.scheme != "https") {
                    return@mapNotNull null
                }
                if (title.isBlank()) return@mapNotNull null

                SearchResult(
                    title = title,
                    url = parsedResultUrl.toString(),
                    domain = parsedResultUrl.topPrivateDomain() ?: parsedResultUrl.host,
                    snippet = element.selectFirst(".result__snippet")?.text()?.trim().orEmpty(),
                )
            }
            .distinctBy { it.url }
    }
}
