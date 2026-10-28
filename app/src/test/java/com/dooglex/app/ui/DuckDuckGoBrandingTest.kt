package com.dooglex.app.ui

import org.junit.Assert.assertTrue
import org.junit.Test

class DuckDuckGoBrandingTest {
    @Test
    fun `contains DoogleX visuals and explicit DuckDuckGo attribution`() {
        val script = DuckDuckGoBranding.script(
            iconDataUri = "data:image/png;base64,dGVzdA==",
            pageBackground = "#D2E3FC",
            pageText = "#174EA6",
            link = "#1A73E8",
            fieldBackground = "#FFFFFF",
            fieldText = "#202124",
        )

        assertTrue(script.contains("dooglex-provider-style"))
        assertTrue(script.contains("dooglex-provider-brand"))
        assertTrue(script.contains("data:image/png;base64,dGVzdA=="))
        assertTrue(script.contains("#D2E3FC"))
        assertTrue(script.contains("#1A73E8"))
        assertTrue(script.contains("DoogleX × DuckDuckGo"))
        assertTrue(script.contains("Независимый интерфейс DoogleX · Поиск: DuckDuckGo"))
        assertTrue(script.contains("__dooglexBrandObserver"))
    }
}
