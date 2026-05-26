(function() {
    function init() {
        // 0. Protect Yen currency values from being translated by Google Translate
        function protectCurrencies() {
            const currencyRegex = /(?:¥|￥)\s*-?\d+(?:,\d{3})*(?:\.\d+)?|(?:¥|￥)/g;
            const walker = document.createTreeWalker(
                document.body,
                NodeFilter.SHOW_TEXT,
                null,
                false
            );

            const nodesToProcess = [];
            let node;
            while (node = walker.nextNode()) {
                const text = node.nodeValue;
                if (text && (text.includes('¥') || text.includes('￥'))) {
                    let parent = node.parentNode;
                    if (!parent) continue;
                    const parentTagName = parent.tagName.toLowerCase();
                    if (parentTagName === 'script' || parentTagName === 'style' || parentTagName === 'textarea') {
                        continue;
                    }
                    // Check if already protected
                    let alreadyProtected = false;
                    let curr = parent;
                    while (curr && curr !== document.body) {
                        if (curr.classList && (curr.classList.contains('notranslate') || curr.getAttribute('translate') === 'no')) {
                            alreadyProtected = true;
                            break;
                        }
                        curr = curr.parentNode;
                    }
                    if (!alreadyProtected) {
                        nodesToProcess.push(node);
                    }
                }
            }

            nodesToProcess.forEach(textNode => {
                const parent = textNode.parentNode;
                if (!parent) return;

                const text = textNode.nodeValue;
                let lastIndex = 0;
                let match;
                const fragment = document.createDocumentFragment();

                // Reset regex lastIndex
                currencyRegex.lastIndex = 0;

                while ((match = currencyRegex.exec(text)) !== null) {
                    const matchIndex = match.index;
                    const matchText = match[0];

                    // Add preceding text
                    if (matchIndex > lastIndex) {
                        fragment.appendChild(document.createTextNode(text.substring(lastIndex, matchIndex)));
                    }

                    // Add protected currency node
                    const span = document.createElement('span');
                    span.className = 'notranslate';
                    span.setAttribute('translate', 'no');
                    span.textContent = matchText;
                    fragment.appendChild(span);

                    lastIndex = currencyRegex.lastIndex;
                }

                // Add remaining text
                if (lastIndex < text.length) {
                    fragment.appendChild(document.createTextNode(text.substring(lastIndex)));
                }

                if (fragment.childNodes.length > 0) {
                    parent.replaceChild(fragment, textNode);
                }
            });
        }

        // Run protection immediately
        protectCurrencies();

        // Observe dynamic DOM changes to protect new/updated currency elements
        const observer = new MutationObserver(() => {
            observer.disconnect();
            protectCurrencies();
            observer.observe(document.body, { childList: true, subtree: true, characterData: true });
        });
        observer.observe(document.body, { childList: true, subtree: true, characterData: true });

        // 1. Inject Stylesheet
        const link = document.createElement('link');
        link.rel = 'stylesheet';
        link.href = '/css/language-switcher.css';
        document.head.appendChild(link);

        // 2. Setup Google Translate Element Hidden Container
        const gtDiv = document.createElement('div');
        gtDiv.id = 'google_translate_element';
        gtDiv.style.display = 'none';
        document.body.appendChild(gtDiv);

        // 3. Register Google Translate Init Callback
        window.googleTranslateElementInit = function() {
            new google.translate.TranslateElement({
                pageLanguage: 'en',
                includedLanguages: 'en,ja',
                layout: google.translate.TranslateElement.InlineLayout.SIMPLE,
                autoDisplay: false
            }, 'google_translate_element');
        };

        // 4. Load Google Translate JS
        const gtScript = document.createElement('script');
        gtScript.type = 'text/javascript';
        gtScript.src = 'https://translate.google.com/translate_a/element.js?cb=googleTranslateElementInit';
        document.body.appendChild(gtScript);

        // Helper functions for Cookies
        function getCookie(name) {
            const value = `; ${document.cookie}`;
            const parts = value.split(`; ${name}=`);
            if (parts.length === 2) return parts.pop().split(';').shift();
            return null;
        }

        // Set cookie
        function setCookie(name, value, days) {
            let expires = "";
            if (days) {
                const date = new Date();
                date.setTime(date.getTime() + (days * 24 * 60 * 60 * 1000));
                expires = "; expires=" + date.toUTCString();
            }
            document.cookie = `${name}=${value}${expires}; path=/`;
        }

        // Determine current language from cookie
        const googtransVal = getCookie('googtrans');
        const currentLang = (googtransVal && googtransVal.includes('/ja')) ? 'ja' : 'en';

        // 5. Create and Inject the Toggle Widget
        const switcherContainer = document.createElement('div');
        switcherContainer.className = 'lang-switcher-container';
        switcherContainer.innerHTML = `
            <div class="lang-switch-wrapper">
                <span class="lang-label ${currentLang === 'en' ? 'active' : ''}" id="langLabelEN">EN</span>
                <button class="lang-toggle-btn ${currentLang === 'ja' ? 'active' : ''}" id="langToggleBtn" aria-label="Switch Language">
                    <span class="lang-slider"></span>
                </button>
                <span class="lang-label ${currentLang === 'ja' ? 'active' : ''}" id="langLabelJA">JA</span>
            </div>
        `;
        document.body.appendChild(switcherContainer);

        // 6. Handle click events
        const toggleBtn = document.getElementById('langToggleBtn');
        const labelEN = document.getElementById('langLabelEN');
        const labelJA = document.getElementById('langLabelJA');

        function switchLanguage(lang) {
            if (lang === 'ja') {
                setCookie('googtrans', '/en/ja', 30);
                document.cookie = "googtrans=/en/ja; path=/;";
            } else {
                setCookie('googtrans', '/en/en', 30);
                document.cookie = "googtrans=; expires=Thu, 01 Jan 1970 00:00:00 UTC; path=/;";
            }
            window.location.reload();
        }

        toggleBtn.addEventListener('click', () => {
            const targetLang = currentLang === 'en' ? 'ja' : 'en';
            switchLanguage(targetLang);
        });

        labelEN.addEventListener('click', () => {
            if (currentLang !== 'en') switchLanguage('en');
        });

        labelJA.addEventListener('click', () => {
            if (currentLang !== 'ja') switchLanguage('ja');
        });
    }

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', init);
    } else {
        init();
    }
})();
