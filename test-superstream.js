/**
 * SuperStream Integration Test — Full Pipeline
 * 
 * Pipeline: TMDB Search → TMDB Episodes → NuvFeb Stream URLs
 * 
 * Usage:
 *   node test-superstream.js
 * 
 * Requires: Node.js 18+ (built-in fetch)
 */

// ═══════════════════════════════════════════════════════════════
// 🔑 CONFIG — Paste your FebBox UI cookie here
// ═══════════════════════════════════════════════════════════════
const FEBBOX_COOKIE = 'eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJpYXQiOjE3NzE2OTQ2MDIsIm5iZiI6MTc3MTY5NDYwMiwiZXhwIjoxODAyNzk4NjIyLCJkYXRhIjp7InVpZCI6MTQ1MjE3OCwidG9rZW4iOiJiM2EyYWYxNzA0MDI5NjI2NzA0Njc4OTYxMWYwMThkNSJ9fQ.oFssqGTDyS6EC2zc_QsIjHdtd1bWf9CoP8zFh0y5LBc';
// Get it from: Login to febbox.com with Google → DevTools → Application → Cookies → "ui" cookie value

const TMDB_API_KEY = '758905ef980c7b17abab5441e8033914';
const NUVFEB_DOMAINS = [
    'https://febapi.nuvioapp.space',
    'https://febbox.nuvioapp.space',
    'https://api.febbox.com',
];

// ═══════════════════════════════════════════════════════════════
// 🎬 Test Cases
// ═══════════════════════════════════════════════════════════════
const TEST_CASES = [
    // TV Shows
    { type: 'tv', name: 'Breaking Bad', tmdbId: 1396, season: 1, episode: 1 },
    { type: 'tv', name: 'Squid Game', tmdbId: 93405, season: 1, episode: 1 },
    // Movies
    { type: 'movie', name: 'The Shawshank Redemption', tmdbId: 278 },
    { type: 'movie', name: 'Inception', tmdbId: 27205 },
];

// ═══════════════════════════════════════════════════════════════
// 📡 API Functions
// ═══════════════════════════════════════════════════════════════

async function tmdbSearch(query) {
    const url = `https://api.themoviedb.org/3/search/multi?query=${encodeURIComponent(query)}&api_key=${TMDB_API_KEY}&language=vi-VN`;
    const r = await fetch(url);
    if (!r.ok) throw new Error(`TMDB search failed: ${r.status}`);
    const data = await r.json();
    return data.results?.slice(0, 3);
}

async function tmdbTvEpisodes(tvId, season) {
    const url = `https://api.themoviedb.org/3/tv/${tvId}/season/${season}?api_key=${TMDB_API_KEY}&language=vi-VN`;
    const r = await fetch(url);
    if (!r.ok) throw new Error(`TMDB episodes failed: ${r.status}`);
    return await r.json();
}

async function nuvfebGetStream(type, tmdbId, season, episode) {
    // Try each domain until one works
    for (const base of NUVFEB_DOMAINS) {
        let url;
        if (type === 'movie') {
            url = `${base}/api/media/movie/${tmdbId}?cookie=${FEBBOX_COOKIE}`;
        } else {
            url = `${base}/api/media/tv/${tmdbId}/${season}/${episode}?cookie=${FEBBOX_COOKIE}`;
        }

        console.log(`  📡 Trying: ${url.replace(FEBBOX_COOKIE, 'COOKIE_HIDDEN')}`);

        try {
            const r = await fetch(url, {
                headers: { 'Accept': 'application/json' },
                signal: AbortSignal.timeout(10000)
            });

            if (r.ok) {
                console.log(`  ✅ ${base} responded: ${r.status}`);
                return await r.json();
            } else {
                const body = await r.text();
                console.log(`  ⚠️ ${base}: ${r.status} — ${body.substring(0, 100)}`);
            }
        } catch (e) {
            console.log(`  ❌ ${base}: ${e.cause?.code || e.message}`);
        }
    }
    throw new Error('All NuvFeb domains failed');
}

// ═══════════════════════════════════════════════════════════════
// 🏃 Runner
// ═══════════════════════════════════════════════════════════════

async function testPipeline(testCase) {
    const { type, name, tmdbId, season, episode } = testCase;
    console.log(`\n${'═'.repeat(60)}`);
    console.log(`🎬 ${name} (${type}, TMDB ID: ${tmdbId})`);
    console.log('═'.repeat(60));

    // Step 1: TMDB Search (verify ID is correct)
    try {
        console.log('\n📌 Step 1: TMDB Search...');
        const results = await tmdbSearch(name);
        const match = results?.find(r => r.id === tmdbId);
        if (match) {
            console.log(`  ✅ Found: "${match.name || match.title}" (${match.media_type}, ID: ${match.id})`);
        } else {
            console.log(`  ⚠️ ID ${tmdbId} not in top 3 results. Results:`);
            results?.forEach(r => console.log(`     - ${r.name || r.title} (${r.media_type}, ID: ${r.id})`));
        }
    } catch (e) {
        console.log(`  ❌ TMDB Search Error: ${e.message}`);
    }

    // Step 2: TMDB Episodes (for TV shows)
    if (type === 'tv') {
        try {
            console.log(`\n📌 Step 2: TMDB Episodes (S${season})...`);
            const seasonData = await tmdbTvEpisodes(tmdbId, season);
            console.log(`  ✅ Season ${season}: ${seasonData.episodes?.length} episodes`);
            const ep = seasonData.episodes?.find(e => e.episode_number === episode);
            if (ep) {
                console.log(`  📺 Ep ${episode}: "${ep.name}" (${ep.air_date})`);
            }
        } catch (e) {
            console.log(`  ❌ TMDB Episodes Error: ${e.message}`);
        }
    }

    // Step 3: NuvFeb Stream URLs (THE KEY TEST!)
    if (FEBBOX_COOKIE === 'PASTE_YOUR_FEBBOX_UI_COOKIE_HERE') {
        console.log(`\n📌 Step 3: NuvFeb Stream URLs...`);
        console.log(`  ⚠️ SKIPPED — Paste your FebBox cookie at the top of this file first!`);
        return;
    }

    try {
        console.log(`\n📌 Step 3: NuvFeb Stream URLs...`);
        const streamData = await nuvfebGetStream(type, tmdbId, season, episode);

        console.log(`  ✅ Response received!`);

        // Parse stream URLs
        if (Array.isArray(streamData)) {
            console.log(`  📦 ${streamData.length} stream sources:`);
            streamData.forEach((src, i) => {
                const quality = src.quality || src.label || 'unknown';
                const url = src.url || src.link || src.file || '';
                const format = url.includes('.m3u8') ? 'M3U8' : url.includes('.mp4') ? 'MP4' : 'OTHER';
                console.log(`     ${i + 1}. [${quality}] ${format} — ${url.substring(0, 80)}...`);
            });
        } else if (streamData.data) {
            console.log(`  📦 Data:`, JSON.stringify(streamData.data).substring(0, 300));
        } else {
            console.log(`  📦 Raw:`, JSON.stringify(streamData).substring(0, 500));
        }

        // Check if any URL is playable
        const urls = Array.isArray(streamData)
            ? streamData.map(s => s.url || s.link || s.file).filter(Boolean)
            : [];

        if (urls.length > 0) {
            console.log(`\n  🎉 PLAYABLE! ${urls.length} stream URL(s) found.`);
            console.log(`  🔗 Best URL: ${urls[0]}`);

            // Quick HEAD check on first URL
            try {
                const headResp = await fetch(urls[0], { method: 'HEAD', signal: AbortSignal.timeout(5000) });
                const contentType = headResp.headers.get('content-type') || '';
                const contentLength = headResp.headers.get('content-length') || '?';
                console.log(`  📊 HEAD: ${headResp.status} | Type: ${contentType} | Size: ${contentLength}`);
            } catch (e) {
                console.log(`  📊 HEAD check failed (may need referer): ${e.message}`);
            }
        }

    } catch (e) {
        console.log(`  ❌ NuvFeb Error: ${e.message}`);
    }
}

async function main() {
    console.log('🚀 SuperStream Integration Test');
    console.log(`📅 ${new Date().toLocaleString('vi-VN')}`);
    console.log(`🔑 Cookie: ${FEBBOX_COOKIE === 'PASTE_YOUR_FEBBOX_UI_COOKIE_HERE' ? '⚠️ NOT SET' : '✅ Set (' + FEBBOX_COOKIE.length + ' chars)'}`);

    for (const tc of TEST_CASES) {
        await testPipeline(tc);
    }

    console.log(`\n${'═'.repeat(60)}`);
    console.log('✅ All tests complete!');
    console.log('═'.repeat(60));
}

main().catch(e => console.error('Fatal:', e));
