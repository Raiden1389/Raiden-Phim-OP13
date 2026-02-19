const express = require('express');
const cors = require('cors');

// Import providers
const vidsrcXyz = require('./providers/vidsrc-xyz');
const vidsrcTo = require('./providers/vidsrc-to');
const autoembed = require('./providers/autoembed');

const app = express();
app.use(cors());
app.use(express.json());

const PORT = process.env.PORT || 3001;

// ═══ Health Check ═══
app.get('/', (req, res) => {
    res.json({
        name: 'Raiden Extractor',
        version: '1.0.0',
        providers: ['vidsrc-xyz', 'vidsrc-to', 'autoembed'],
        endpoints: ['/extract', '/test']
    });
});

// ═══ Extract Stream URLs ═══
// GET /extract?tmdb_id=123456&type=movie
// GET /extract?tmdb_id=123456&type=tv&season=1&episode=1
// GET /extract?imdb_id=tt1234567&type=movie
app.get('/extract', async (req, res) => {
    const { tmdb_id, imdb_id, type = 'movie', season, episode } = req.query;

    if (!tmdb_id && !imdb_id) {
        return res.status(400).json({ error: 'tmdb_id or imdb_id required' });
    }

    const id = tmdb_id || imdb_id;
    const idType = tmdb_id ? 'tmdb' : 'imdb';

    console.log(`\n🎬 Extracting: ${idType}=${id} type=${type} S${season || '-'}E${episode || '-'}`);

    // Try each provider in order (fallback chain)
    const providers = [vidsrcXyz, vidsrcTo, autoembed];
    const allSources = [];
    const allSubtitles = [];

    for (const provider of providers) {
        try {
            console.log(`  ⏳ Trying ${provider.name}...`);
            const result = await provider.extract({
                tmdbId: tmdb_id,
                imdbId: imdb_id,
                type,
                season: season ? parseInt(season) : null,
                episode: episode ? parseInt(episode) : null
            });

            if (result && result.sources && result.sources.length > 0) {
                console.log(`  ✅ ${provider.name}: ${result.sources.length} sources found`);
                allSources.push(...result.sources.map(s => ({ ...s, provider: provider.name })));
                if (result.subtitles) {
                    allSubtitles.push(...result.subtitles);
                }
            } else {
                console.log(`  ❌ ${provider.name}: no sources`);
            }
        } catch (err) {
            console.log(`  ❌ ${provider.name}: ${err.message}`);
        }
    }

    if (allSources.length === 0) {
        return res.status(404).json({
            error: 'No streams found from any provider',
            tried: providers.map(p => p.name)
        });
    }

    res.json({
        sources: allSources,
        subtitles: allSubtitles
    });
});

// ═══ Test a specific provider ═══
app.get('/test/:provider', async (req, res) => {
    const { provider } = req.params;
    const { tmdb_id, imdb_id, type = 'movie', season, episode } = req.query;

    const providerMap = {
        'vidsrc-xyz': vidsrcXyz,
        'vidsrc-to': vidsrcTo,
        'autoembed': autoembed
    };

    const p = providerMap[provider];
    if (!p) {
        return res.status(400).json({ error: `Unknown provider: ${provider}`, available: Object.keys(providerMap) });
    }

    try {
        const result = await p.extract({
            tmdbId: tmdb_id,
            imdbId: imdb_id,
            type,
            season: season ? parseInt(season) : null,
            episode: episode ? parseInt(episode) : null
        });
        res.json({ provider: p.name, ...result });
    } catch (err) {
        res.status(500).json({ provider: p.name, error: err.message });
    }
});

app.listen(PORT, () => {
    console.log(`\n🚀 Raiden Extractor running at http://localhost:${PORT}`);
    console.log(`   Test: http://localhost:${PORT}/extract?tmdb_id=939243&type=movie`);
    console.log(`   (939243 = Sonic the Hedgehog 3)\n`);
});
