---
title: SuperStream Pipeline Reference
createdAt: '2026-02-22T06:15:33.261Z'
updatedAt: '2026-02-22T06:15:33.261Z'
description: >-
  Complete technical reference for SuperStream English content integration -
  APIs, auth, stream flow, debugging
tags:
  - superstream
  - api
  - pipeline
  - tmdb
  - febbox
  - nuvfeb
---
# SuperStream Pipeline — Technical Reference

## Overview
SuperStream = English content source. Pipeline: **TMDB (metadata) → NuvFeb API (streams) → ExoPlayer**

## APIs

### TMDB (metadata)
- Base: `https://api.themoviedb.org/3`
- Auth: Bearer token (`Constants.TMDB_BEARER_TOKEN`)
- Endpoints: trending, search/multi, movie/{id}, tv/{id}, tv/{id}/season/{num}
- Rate limit: 40 req/10s (free tier)
- Poster: `https://image.tmdb.org/t/p/w500{posterPath}`

### NuvFeb (streams) ⭐ PRIMARY
- Base: `https://febapi.nuvioapp.space`
- Auth: `?cookie={FEBBOX_UI_COOKIE}` (JWT from Google login to febbox.com)
- Movie: `GET /api/media/movie/{tmdbId}?cookie=...`
- TV: `GET /api/media/tv/{tmdbId}/{season}/{episode}?cookie=...`
- Returns: direct M3U8 + MP4 links (1080p/720p/360p)
- Cookie expires ~1 year (current: 2027-02-17)

### ShowBox (legacy/fallback)
- Base: `https://mbpapi.shegu.net/api/api_client/index/`
- Auth: TripleDES encrypted body (`ShowBoxCrypto.kt`)
- ⚠️ showbox.media 403 Cloudflare — pipeline broken
- Status: DEPRECATED, NuvFeb is primary

## Stream Flow
```
Movie: DetailScreen → VM.playMovie() → Repository.streamMovie() → NuvFeb → StreamState.Ready → PlayerActivity
TV:    DetailScreen → VM.playTvEpisode(s,e) → Repository.streamTvEpisode() → NuvFeb → StreamState.Ready → PlayerActivity
```

## Favorites
- Slug: `ss_{type}_{tmdbId}` (e.g. `ss_movie_550`)
- Source tag: `"superstream"`
- Storage: WatchlistManager (SharedPreferences)

## Cookie Refresh
1. Browser → febbox.com → Google login
2. F12 → Application → Cookies → copy `ui` value
3. Update `Constants.kt → FEBBOX_COOKIE`

## File Map
```
data/api/SuperStreamApi.kt — Retrofit interfaces
data/api/models/SuperStreamModels.kt — TMDB data classes
data/repository/SuperStreamRepository.kt — Pipeline orchestration
ui/screens/superstream/ — All UI (Screen, Detail, ViewModels, Components)
util/ShowBoxCrypto.kt — TripleDES
util/Constants.kt — Keys, URLs, cookies
```

## Known Issues
1. NuvFeb single source — no auto-fallback if down
2. ShowBox 403 Cloudflare — pipeline broken
3. Cookie manual refresh needed when JWT expires
4. TMDB rate limit 40/10s

## Debug
- Test NuvFeb: `curl "https://febapi.nuvioapp.space/api/media/movie/550?cookie={JWT}"`
- Test TMDB: `curl -H "Authorization: Bearer {token}" "https://api.themoviedb.org/3/trending/movie/week"`
- Full doc: `docs/SUPERSTREAM-PIPELINE.md`
