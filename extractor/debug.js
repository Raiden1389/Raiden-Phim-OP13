const axios = require('axios');
const cheerio = require('cheerio');

(async () => {
    try {
        // Step 1-3: same chain
        const { data: embedHtml } = await axios.get(
            'https://vidsrc.net/embed/movie?tmdb=1368166',
            { headers: { 'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36' }, timeout: 15000 }
        );
        const $ = cheerio.load(embedHtml);
        const iframe = $('iframe').attr('src');
        const BASEDOM = new URL(iframe.startsWith('//') ? 'https:' + iframe : iframe).origin;
        const hash = iframe.match(/\/rcp\/(.+)/)[1];

        const { data: rcpHtml } = await axios.get(
            BASEDOM + '/rcp/' + hash,
            { headers: { 'User-Agent': 'Mozilla/5.0', 'Referer': 'https://vidsrc.net/' }, timeout: 10000 }
        );
        const pid = rcpHtml.match(/src:\s*'([^']*)'/)[1].replace('/prorcp/', '');

        const { data: proHtml } = await axios.get(
            BASEDOM + '/prorcp/' + pid,
            { headers: { 'User-Agent': 'Mozilla/5.0', 'Referer': BASEDOM }, timeout: 10000 }
        );

        const $p = cheerio.load(proHtml);

        // Extract the file URL from inline script
        let fileUrl = null;
        $p('script').each((i, el) => {
            const inline = $p(el).html();
            if (inline && inline.length > 10000) {
                // Find file: "..." in Playerjs config
                const fileMatch = inline.match(/file:\s*["']([^"']+)/);
                if (fileMatch) {
                    fileUrl = fileMatch[1];
                }
            }
        });

        console.log('Raw file URL:', fileUrl ? fileUrl.substring(0, 200) : 'NOT FOUND');

        if (!fileUrl) return;

        // Check if it has {v1}/{v2} placeholders
        const hasPlaceholder = fileUrl.includes('{v1}') || fileUrl.includes('{v2}');
        console.log('Has placeholder:', hasPlaceholder);

        // Find the document.write script that loads the decrypt JS
        let decryptJsUrl = null;
        $p('script').each((i, el) => {
            const inline = $p(el).html();
            if (inline && inline.includes('document.write')) {
                const srcMatch = inline.match(/src=['"]([^'"]+\.js[^'"]*)['"]/);
                if (srcMatch) {
                    decryptJsUrl = srcMatch[1];
                }
            }
        });

        console.log('Decrypt JS URL:', decryptJsUrl);

        if (!decryptJsUrl) return;

        // Fetch decrypt JS
        const fullDecryptUrl = decryptJsUrl.startsWith('/') ? BASEDOM + decryptJsUrl : decryptJsUrl;
        const { data: decryptJs } = await axios.get(fullDecryptUrl, {
            headers: { 'User-Agent': 'Mozilla/5.0', 'Referer': BASEDOM },
            timeout: 10000,
        });

        // Find window['something'] = 'value' at the beginning
        const windowMatch = decryptJs.match(/window\['([^']+)'\]\s*=\s*'([^']+)'/);
        if (windowMatch) {
            console.log('\nWindow key:', windowMatch[1]);
            console.log('Window value (first 100):', windowMatch[2].substring(0, 100));
            console.log('Window value length:', windowMatch[2].length);
        }

        // The decode function pattern: look for what processes {v1} and {v2}
        // Search inline script for {v1} usage
        $p('script').each((i, el) => {
            const inline = $p(el).html();
            if (inline && inline.length > 10000) {
                // Find v1/v2 substitution pattern
                const v1Matches = inline.match(/\{v1\}.*?\{v2\}/s);
                if (v1Matches) {
                    console.log('\n=== v1/v2 context ===');
                    const idx = inline.indexOf('{v1}');
                    console.log(inline.substring(Math.max(0, idx - 300), idx + 300));
                }

                // Find pass_path which tells us the domain
                const passPath = inline.match(/pass_path\s*=\s*["']([^"']+)/);
                if (passPath) {
                    console.log('\npass_path:', passPath[1]);
                    // Extract domain from pass_path
                    const domain = passPath[1].match(/\/\/([^/]+)/);
                    if (domain) console.log('Domain from pass_path:', domain[1]);
                }
            }
        });

        // Try: domain from pass_path = tmstr2.cloudnestra.com
        // Replace {v1} with cloudnestra.com
        const pass_path = '//tmstr2.cloudnestra.com/rt_ping.php';
        const domain = 'cloudnestra.com';

        // Split "or" URLs
        const urls = fileUrl.split(' or ');
        console.log('\n=== Resolved URLs ===');
        for (const u of urls) {
            const resolved = u.replace('{v1}', domain).replace('{v2}', domain);
            console.log(resolved.substring(0, 150));
        }

        // Test: try to fetch the master.m3u8
        const firstUrl = urls[0].replace('{v1}', domain);
        console.log('\nTesting:', firstUrl.substring(0, 120));
        try {
            const { status, data: m3u8Data } = await axios.get(firstUrl, {
                headers: {
                    'User-Agent': 'Mozilla/5.0',
                    'Referer': BASEDOM + '/',
                    'Origin': BASEDOM
                },
                timeout: 10000,
            });
            console.log('Status:', status);
            console.log('First 500 chars:');
            console.log(m3u8Data.substring(0, 500));
        } catch (e) {
            console.log('Fetch error:', e.message.substring(0, 100));
            // Also try without Referer
            try {
                const { status, data } = await axios.get(firstUrl, {
                    headers: { 'User-Agent': 'Mozilla/5.0' },
                    timeout: 10000,
                });
                console.log('No-referer Status:', status);
                console.log(data.substring(0, 300));
            } catch (e2) {
                console.log('No-referer also failed:', e2.message.substring(0, 80));
            }
        }

    } catch (e) {
        console.log('ERROR:', e.message);
    }
})();
