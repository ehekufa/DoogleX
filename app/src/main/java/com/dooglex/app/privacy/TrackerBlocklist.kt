package com.dooglex.app.privacy

/** Built-in blocker for common third-party analytics and advertising hosts. */
object TrackerBlocklist {
    private val blockedDomains = setOf(
        "2mdn.net",
        "adform.net",
        "adservice.google.com",
        "adsymptotic.com",
        "amazon-adsystem.com",
        "amplitude.com",
        "appsflyer.com",
        "branch.io",
        "criteo.com",
        "criteo.net",
        "doubleclick.net",
        "facebook.net",
        "google-analytics.com",
        "googlesyndication.com",
        "googletagmanager.com",
        "hotjar.com",
        "mc.yandex.net",
        "mc.yandex.ru",
        "mixpanel.com",
        "moatads.com",
        "quantserve.com",
        "scorecardresearch.com",
        "segment.com",
        "segment.io",
        "taboola.com",
        "vungle.com",
    )

    fun shouldBlock(targetHost: String?, pageHost: String?): Boolean {
        val target = targetHost?.lowercase()?.trimEnd('.') ?: return false
        val page = pageHost?.lowercase()?.trimEnd('.')
        if (target == page) return false
        return blockedDomains.any { domain -> target == domain || target.endsWith(".$domain") }
    }
}
