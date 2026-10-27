package com.dooglex.app.privacy

import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

fun upgradeToHttps(url: String): String {
    val parsed = url.toHttpUrlOrNull() ?: return url
    if (parsed.scheme != "http") return url
    return parsed.newBuilder().scheme("https").build().toString()
}
