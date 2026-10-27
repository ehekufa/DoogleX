package com.dooglex.app.data

import com.dooglex.app.model.SearchResult
import java.io.IOException
import java.net.InetAddress
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

    private val dohClient by lazy {
        val dns = DnsOverHttps.Builder()
            .client(systemDnsClient)
            .url(DOH_ENDPOINT.toHttpUrl())
            // Numeric bootstrap addresses avoid a system-DNS lookup for the DoH resolver itself.
            .bootstrapDnsHosts(
                InetAddress.getByName("1.1.1.1"),
                InetAddress.getByName("1.0.0.1"),
            )
            .post(true)
            .resolvePrivateAddresses(false)
            .build()
        systemDnsClient.newBuilder().dns(dns).build()
    }

    suspend fun search(
        query: String,
        safeSearch: Boolean,
        useDoh: Boolean,
    ): List<SearchResult> = withContext(Dispatchers.IO) {
        val client = if (useDoh) dohClient else systemDnsClient
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
        const val DOH_ENDPOINT = "https://cloudflare-dns.com/dns-query"
        const val USER_AGENT = "DoogleX/1.2 (Android; native search client)"
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
