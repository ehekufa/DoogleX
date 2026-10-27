package com.dooglex.app.privacy

import com.dooglex.app.model.WebPageSettings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PrivacyPoliciesTest {
    @Test
    fun `upgrades http without changing the rest of the URL`() {
        assertEquals(
            "https://example.com/search?q=private",
            upgradeToHttps("http://example.com/search?q=private"),
        )
        assertEquals(
            "https://example.com/already-secure",
            upgradeToHttps("https://example.com/already-secure"),
        )
    }

    @Test
    fun `blocks known third-party trackers and their subdomains`() {
        assertTrue(TrackerBlocklist.shouldBlock("google-analytics.com", "example.com"))
        assertTrue(TrackerBlocklist.shouldBlock("stats.doubleclick.net", "example.com"))
        assertFalse(TrackerBlocklist.shouldBlock("notdoubleclick.net", "example.com"))
    }

    @Test
    fun `does not block first-party or unrelated content`() {
        assertFalse(TrackerBlocklist.shouldBlock("google-analytics.com", "google-analytics.com"))
        assertFalse(TrackerBlocklist.shouldBlock("cdn.example.org", "example.org"))
        assertFalse(TrackerBlocklist.shouldBlock(null, "example.org"))
    }

    @Test
    fun `compatibility mode allows website resources while strict mode keeps controls`() {
        val compatibility = WebPageSettings()
        assertTrue(compatibility.compatibilityMode)
        assertFalse(compatibility.trackerBlockingActive)
        assertTrue(compatibility.thirdPartyCookiesAllowed)

        val strict = compatibility.copy(compatibilityMode = false)
        assertTrue(strict.trackerBlockingActive)
        assertFalse(strict.thirdPartyCookiesAllowed)
    }
}
