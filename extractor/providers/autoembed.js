/**
 * Autoembed Provider
 * Embed: https://autoembed.cc/embed/movie/{tmdb_id}
 * Also: https://autoembed.cc/embed/tv/{tmdb_id}/{season}/{episode}
 */
const axios = require('axios');
const cheerio = require('cheerio');

const name = 'autoembed';

async function extract({ tmdbId, imdbId, type, season, episode }) {
    const id = tmdbId || imdbId;
    if (!id) throw new Error('tmdb_id or imdb_id required');

    let embedUrl;
    if (type === 'tv') {
        embedUrl = `https://autoembed.cc/embed/tv/${id}/${season}/${episode}`;
    } else {
        embedUrl = `https://autoembed.cc/embed/movie/${id}`;
    }

    console.log(`    📎 Embed: ${embedUrl}`);

    const { data: embedHtml } = await axios.get(embedUrl, {
        headers: {
            'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36',
            'Referer': 'https://autoembed.cc/',
        },
        timeout: 10000,
        maxRedirects: 5,
    });

    const $ = cheerio.load(embedHtml);
    const sources = [];
    const subtitles = [];

    // Check for iframes
    const iframes = $('iframe');
    console.log(`    🔗 Found ${iframes.length} iframes`);

    for (let i = 0; i < iframes.length; i++) {
        let src = $(iframes[i]).attr('src');
        if (!src) continue;

        if (src.startsWith('//')) src = `https:${src}`;
        if (src.startsWith('/')) src = `https://autoembed.cc${src}`;

        console.log(`    🔗 iframe[${i}]: ${src}`);

        try {
            const { data: playerHtml } = await axios.get(src, {
                headers: {
                    'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36',
                    'Referer': embedUrl,
                },
                timeout: 10000,
            });

            // Extract m3u8
            const m3u8Matches = playerHtml.match(/https?:[^\s'"]*\.m3u8[^\s'"]*/gi);
            if (m3u8Matches) {
                for (const url of [...new Set(m3u8Matches)]) {
                    sources.push({
                        url: url.replace(/\\/g, ''),
                        quality: 'auto',
                        isM3U8: true,
                        headers: { 'Referer': src }
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
            console.log(`    ⚠️ iframe[${i}] error: ${e.message}`);
        }
    }

    // Also check for direct m3u8 in main page
    const directM3u8 = embedHtml.match(/https?:[^\s'"]*\.m3u8[^\s'"]*/gi);
    if (directM3u8) {
        for (const url of [...new Set(directM3u8)]) {
            if (!sources.find(s => s.url === url)) {
                sources.push({
                    url: url.replace(/\\/g, ''),
                    quality: 'auto',
                    isM3U8: true,
                    headers: { 'Referer': embedUrl }
                });
            }
        }
    }

    return { sources, subtitles };
}

module.exports = { name, extract };
