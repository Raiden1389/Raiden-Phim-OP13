/**
 * VidSrc Provider — Working Extractor
 * 
 * Flow:
 * 1. vidsrc.net/embed → iframe → cloudnestra.com/rcp/{hash}
 * 2. /rcp/{hash} → /prorcp/{pid}
 * 3. ProRCP page inline script → file URL with {v1}/{v2} placeholders
 * 4. pass_path in inline script → resolve domain (e.g. cloudnestra.com)
 * 5. Replace {v1} → get real m3u8 URL
 */
const axios = require('axios');
const cheerio = require('cheerio');

const name = 'vidsrc-me';

const UA = 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Safari/537.36';

async function extract({ tmdbId, imdbId, type, season, episode }) {
    const id = tmdbId || imdbId;
    if (!id) throw new Error('tmdb_id or imdb_id required');

    // Step 1: Fetch embed page
    const url = type === 'tv'
        ? `https://vidsrc.net/embed/${type}?tmdb=${id}&season=${season}&episode=${episode}`
        : `https://vidsrc.net/embed/${type}?tmdb=${id}`;

    console.log(`    📎 Embed: ${url}`);

    const { data: embedHtml } = await axios.get(url, {
        headers: { 'User-Agent': UA },
        timeout: 15000,
    });

    const $ = cheerio.load(embedHtml);
    const iframeSrc = $('iframe').attr('src') || '';

    if (!iframeSrc) {
        console.log('    ⚠️ No iframe found in embed page');
        return { sources: [], subtitles: [] };
    }

    const fullIframeUrl = iframeSrc.startsWith('//') ? `https:${iframeSrc}` : iframeSrc;
    let BASEDOM;
    try {
        BASEDOM = new URL(fullIframeUrl).origin;
    } catch (e) {
        console.log('    ⚠️ Invalid iframe URL:', iframeSrc);
        return { sources: [], subtitles: [] };
    }
    console.log(`    🌐 BASEDOM: ${BASEDOM}`);

    // Step 2: Extract hash from iframe and fetch RCP
    const hashMatch = iframeSrc.match(/\/rcp\/(.+)/);
    if (!hashMatch) {
        console.log('    ⚠️ No /rcp/ hash in iframe');
        return { sources: [], subtitles: [] };
    }
    const hash = hashMatch[1];

    console.log(`    🔗 Hash: ${hash.substring(0, 30)}... (${hash.length} chars)`);

    const { data: rcpHtml } = await axios.get(
        `${BASEDOM}/rcp/${hash}`,
        { headers: { 'User-Agent': UA, 'Referer': 'https://vidsrc.net/' }, timeout: 10000 }
    );

    // Step 3: Get /prorcp/ path from RCP
    const srcMatch = rcpHtml.match(/src:\s*'([^']*)'/);
    if (!srcMatch || !srcMatch[1].startsWith('/prorcp/')) {
        console.log(`    ⚠️ No /prorcp/ found. src=${srcMatch ? srcMatch[1] : 'none'}`);
        console.log(`    📄 RCP response (${rcpHtml.length} chars): ${rcpHtml.substring(0, 200)}`);
        return { sources: [], subtitles: [] };
    }

    const prorcpId = srcMatch[1].replace('/prorcp/', '');
    console.log(`    📡 ProRCP: ${prorcpId.substring(0, 30)}...`);

    // Step 4: Fetch ProRCP page
    const { data: proHtml } = await axios.get(
        `${BASEDOM}/prorcp/${prorcpId}`,
        { headers: { 'User-Agent': UA, 'Referer': BASEDOM }, timeout: 10000 }
    );

    const $p = cheerio.load(proHtml);

    // Step 5: Extract file URL and domain from inline script
    let rawFileUrl = null;
    let domain = null;
    let subtitlesStr = null;

    $p('script').each((_, el) => {
        const inline = $p(el).html();
        if (!inline || inline.length < 1000) return;

        // Extract file URL from Playerjs config
        if (!rawFileUrl) {
            const fileMatch = inline.match(/file:\s*["']([^"']+)/);
            if (fileMatch) rawFileUrl = fileMatch[1];
        }

        // Extract domain from pass_path
        if (!domain) {
            const passMatch = inline.match(/pass_path\s*=\s*["']\/\/([^/]+)/);
            if (passMatch) {
                // pass_path = //tmstr2.cloudnestra.com/rt_ping.php
                // Domain for {v1} = remove "tmstr2." prefix from host
                const host = passMatch[1]; // tmstr2.cloudnestra.com
                const parts = host.split('.');
                // Domain = everything after first part (tmstr2.xxx.com → xxx.com)
                domain = parts.length > 2 ? parts.slice(1).join('.') : host;
            }
        }

        // Extract subtitles
        if (!subtitlesStr) {
            const subMatch = inline.match(/default_subtitles\s*=\s*"([^"]*)"/);
            if (subMatch && subMatch[1]) subtitlesStr = subMatch[1];
        }
    });

    if (!rawFileUrl) {
        console.log('    ⚠️ No file URL found in inline script');
        return { sources: [], subtitles: [] };
    }

    console.log(`    📁 Raw file: ${rawFileUrl.substring(0, 60)}...`);
    console.log(`    🌍 Domain: ${domain || 'unknown'}`);

    // Step 6: Resolve {v1}/{v2} placeholders
    // Split by " or " to get multiple quality URLs
    const rawUrls = rawFileUrl.split(' or ');
    const sources = [];

    for (const raw of rawUrls) {
        let resolved = raw;
        if (domain) {
            resolved = resolved.replace(/\{v1\}/g, domain);
            resolved = resolved.replace(/\{v2\}/g, domain);
        }
        // Skip URLs with unresolved placeholders
        if (resolved.includes('{v')) continue;

        // Ensure URL starts with https://
        if (resolved.startsWith('//')) resolved = 'https:' + resolved;

        sources.push({
            url: resolved,
            quality: 'auto',
            isM3U8: resolved.includes('.m3u8') || resolved.includes('/pl/'),
            headers: { 'Referer': BASEDOM + '/' }
        });
    }

    // Parse subtitles
    const subtitles = [];
    if (subtitlesStr) {
        try {
            const subs = JSON.parse(subtitlesStr);
            if (Array.isArray(subs)) {
                subs.forEach(s => subtitles.push(s));
            }
        } catch (e) {
            // subtitles may be comma-separated URLs
            subtitlesStr.split(',').forEach((url, i) => {
                if (url.trim()) {
                    subtitles.push({ url: url.trim(), lang: `sub${i}` });
                }
            });
        }
    }

    // Deduplicate sources (same base URL)
    const seen = new Set();
    const uniqueSources = sources.filter(s => {
        const key = s.url.split('?')[0];
        if (seen.has(key)) return false;
        seen.add(key);
        return true;
    });

    console.log(`    ✅ ${uniqueSources.length} sources, ${subtitles.length} subtitles`);
    if (uniqueSources.length > 0) {
        console.log(`    🎬 ${uniqueSources[0].url.substring(0, 80)}...`);
    }

    return { sources: uniqueSources, subtitles };
}

module.exports = { name, extract };
