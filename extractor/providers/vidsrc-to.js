/**
 * VidSrc.to Provider  
 * API docs: https://vidsrc.to/#api
 * Embed: https://vidsrc.to/embed/movie/{tmdb_id}
 */
const axios = require('axios');
const cheerio = require('cheerio');

const name = 'vidsrc-to';

async function extract({ tmdbId, imdbId, type, season, episode }) {
    const id = tmdbId || imdbId;
    if (!id) throw new Error('tmdb_id or imdb_id required');

    // Build embed URL
    let embedUrl;
    if (type === 'tv') {
        embedUrl = `https://vidsrc.to/embed/tv/${id}/${season}/${episode}`;
    } else {
        embedUrl = `https://vidsrc.to/embed/movie/${id}`;
    }

    console.log(`    📎 Embed: ${embedUrl}`);

    const { data: embedHtml } = await axios.get(embedUrl, {
        headers: {
            'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36',
            'Referer': 'https://vidsrc.to/',
        },
        timeout: 10000,
    });

    const $ = cheerio.load(embedHtml);
    const sources = [];
    const subtitles = [];

    // VidSrc.to uses data-id attributes for server selection
    // and loads streams via AJAX
    const serverItems = $('[data-id]');
    console.log(`    🖥️ Found ${serverItems.length} servers`);

    // Try to find iframe src
    const iframeSrc = $('iframe').attr('src');
    if (iframeSrc) {
        console.log(`    🔗 iframe: ${iframeSrc}`);

        const iframeUrl = iframeSrc.startsWith('//') ? `https:${iframeSrc}` :
            iframeSrc.startsWith('/') ? `https://vidsrc.to${iframeSrc}` : iframeSrc;

        try {
            const { data: playerHtml } = await axios.get(iframeUrl, {
                headers: {
                    'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36',
                    'Referer': embedUrl,
                },
                timeout: 10000,
            });

            // Extract m3u8 URLs
            const m3u8Matches = playerHtml.match(/https?:[^\s'"]*\.m3u8[^\s'"]*/gi);
            if (m3u8Matches) {
                for (const url of [...new Set(m3u8Matches)]) {
                    sources.push({
                        url: url.replace(/\\/g, ''),
                        quality: 'auto',
                        isM3U8: true,
                        headers: { 'Referer': iframeUrl }
                    });
                }
            }

            // Extract subtitles
            const subMatches = playerHtml.match(/https?:[^\s'"]*\.(vtt|srt)[^\s'"]*/gi);
            if (subMatches) {
                for (const url of [...new Set(subMatches)]) {
                    subtitles.push({
                        url: url.replace(/\\/g, ''),
                        lang: 'en',
                        label: 'Auto'
                    });
                }
            }
        } catch (e) {
            console.log(`    ⚠️ iframe error: ${e.message}`);
        }
    }

    // Also try VidSrc.to API endpoint for sources
    try {
        const apiUrl = `https://vidsrc.to/ajax/embed/movie/${id}`;
        const { data: apiData } = await axios.get(apiUrl, {
            headers: {
                'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64)',
                'Referer': embedUrl,
                'X-Requested-With': 'XMLHttpRequest',
            },
            timeout: 10000,
        });

        if (apiData && typeof apiData === 'object') {
            console.log(`    📡 API response keys: ${Object.keys(apiData).join(', ')}`);
        }
    } catch (e) {
        // API might not exist, that's OK
    }

    return { sources, subtitles };
}

module.exports = { name, extract };
