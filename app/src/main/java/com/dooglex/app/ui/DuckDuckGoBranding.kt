package com.dooglex.app.ui

import org.json.JSONObject

/** Builds an idempotent, DuckDuckGo-only visual layer with explicit provider attribution. */
internal object DuckDuckGoBranding {
    fun script(
        iconDataUri: String,
        pageBackground: String,
        pageText: String,
        link: String,
        fieldBackground: String,
        fieldText: String,
    ): String {
        val css = """
            html, body, body > div, #__next, #root, main, [role="main"] {
                background-color: $pageBackground !important;
                color: $pageText !important;
            }
            header, [role="banner"], .header, #header, [class*="header" i] {
                background-color: $pageBackground !important;
                color: $pageText !important;
                border-color: ${link}66 !important;
            }
            .result, article, [class*="result" i] {
                background-color: transparent !important;
                color: $pageText !important;
                border-color: ${link}55 !important;
            }
            input[name="q"], input[type="search"], textarea {
                background-color: $fieldBackground !important;
                color: $fieldText !important;
                border-color: $link !important;
                caret-color: $link !important;
                border-radius: 24px !important;
            }
            input[name="q"]::placeholder, input[type="search"]::placeholder, textarea::placeholder {
                color: $fieldText !important;
                opacity: 0.72 !important;
            }
            button, [role="button"] {
                color: $fieldText !important;
                border-color: ${link}77 !important;
            }
            a, a:visited {
                color: $link !important;
            }
            .dooglex-injected {
                box-sizing: border-box !important;
                font-family: system-ui, sans-serif !important;
            }
            #dooglex-provider-brand {
                display: flex !important;
                align-items: center !important;
                gap: 10px !important;
                width: max-content !important;
                max-width: 100% !important;
                padding: 8px 12px !important;
                color: $pageText !important;
                background: $fieldBackground !important;
                border: 1px solid ${link}66 !important;
                border-radius: 16px !important;
                margin: 8px 10px !important;
                position: relative !important;
                z-index: 20 !important;
            }
            #dooglex-provider-brand img {
                width: 38px !important;
                height: 38px !important;
                border-radius: 10px !important;
                object-fit: cover !important;
                flex: 0 0 auto !important;
            }
            #dooglex-provider-brand .dooglex-provider-copy {
                display: flex !important;
                flex-direction: column !important;
                min-width: 0 !important;
                line-height: 1.25 !important;
            }
            #dooglex-provider-brand strong {
                color: $pageText !important;
                font-size: 14px !important;
            }
            #dooglex-provider-brand small {
                color: $pageText !important;
                font-size: 11px !important;
                opacity: 0.82 !important;
            }
            .dooglex-provider-logo {
                width: 42px !important;
                height: 42px !important;
                object-fit: cover !important;
                border-radius: 11px !important;
                display: block !important;
            }
        """.trimIndent()

        return """
            (function() {
                'use strict';
                const css = ${JSONObject.quote(css)};
                const icon = ${JSONObject.quote(iconDataUri)};
                const title = ${JSONObject.quote("DoogleX × DuckDuckGo")};
                const disclosure = ${JSONObject.quote("Независимый интерфейс DoogleX · Поиск: DuckDuckGo")};
                let style = document.getElementById('dooglex-provider-style');
                if (!style) {
                    style = document.createElement('style');
                    style.id = 'dooglex-provider-style';
                    (document.head || document.documentElement).appendChild(style);
                }
                if (style.textContent !== css) style.textContent = css;

                function makeLogo(className) {
                    const image = document.createElement('img');
                    image.src = icon;
                    image.alt = title;
                    image.className = 'dooglex-injected ' + className;
                    return image;
                }

                function applyBrand() {
                    let badge = document.getElementById('dooglex-provider-brand');
                    if (!badge) {
                        badge = document.createElement('div');
                        badge.id = 'dooglex-provider-brand';
                        badge.className = 'dooglex-injected';
                        badge.appendChild(makeLogo('dooglex-provider-logo'));
                        const copy = document.createElement('span');
                        copy.className = 'dooglex-provider-copy';
                        const heading = document.createElement('strong');
                        heading.textContent = title;
                        const note = document.createElement('small');
                        note.textContent = disclosure;
                        copy.appendChild(heading);
                        copy.appendChild(note);
                        badge.appendChild(copy);
                        const host = document.querySelector('header, [role="banner"], .header, #header');
                        (host || document.body).prepend(badge);
                    }

                    const logos = document.querySelectorAll(
                        'header img, header svg, [class*="logo" i] img, [class*="logo" i] svg, ' +
                        '[id*="logo" i] img, [id*="logo" i] svg'
                    );
                    logos.forEach(function(node) {
                        if (node.closest('.dooglex-injected') || node.closest('.result, article')) return;
                        const description = [
                            node.getAttribute('alt') || '',
                            node.getAttribute('aria-label') || '',
                            node.getAttribute('src') || '',
                            node.getAttribute('class') || '',
                            node.parentElement ? node.parentElement.getAttribute('class') || '' : ''
                        ].join(' ');
                        if (!/duck|logo|ddg/i.test(description)) return;
                        node.replaceWith(makeLogo('dooglex-provider-logo'));
                    });
                }

                applyBrand();
                if (!window.__dooglexBrandObserver) {
                    let scheduled = false;
                    window.__dooglexBrandObserver = new MutationObserver(function() {
                        if (scheduled) return;
                        scheduled = true;
                        setTimeout(function() {
                            scheduled = false;
                            applyBrand();
                        }, 150);
                    });
                    window.__dooglexBrandObserver.observe(
                        document.documentElement,
                        { childList: true, subtree: true }
                    );
                }
            })();
        """.trimIndent()
    }
}
