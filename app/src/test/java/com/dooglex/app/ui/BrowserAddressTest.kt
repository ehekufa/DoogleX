package com.dooglex.app.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BrowserAddressTest {
    @Test
    fun `opens GitHub repository URLs directly`() {
        assertEquals(
            "https://github.com/ehekufa/DoogleX",
            BrowserAddress.resolve("github.com/ehekufa/DoogleX"),
        )
        assertEquals(
            "https://github.com/ehekufa/DoogleX",
            BrowserAddress.resolve("https://github.com/ehekufa/DoogleX"),
        )
    }

    @Test
    fun `upgrades HTTP and preserves repository path and query`() {
        assertEquals(
            "https://github.com/ehekufa/DoogleX/issues?q=is%3Aopen",
            BrowserAddress.resolve("http://github.com/ehekufa/DoogleX/issues?q=is%3Aopen"),
        )
    }

    @Test
    fun `uses web search for ordinary phrases`() {
        assertEquals(
            "https://duckduckgo.com/?q=DoogleX%20browser",
            BrowserAddress.resolve("DoogleX browser"),
        )
    }

    @Test
    fun `rejects local and unsupported schemes`() {
        assertNull(BrowserAddress.resolve("file:///etc/passwd"))
        assertNull(BrowserAddress.resolve("content://contacts"))
        assertNull(BrowserAddress.resolve("ftp://example.com/file"))
        assertNull(BrowserAddress.resolve("   "))
    }
}
