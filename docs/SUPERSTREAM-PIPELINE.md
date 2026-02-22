# SuperStream Pipeline — Technical Reference

> Last updated: 2026-02-22
> Version: v1.20.0

## Overview

SuperStream là nguồn phim tiếng Anh cho Raiden Phim. Pipeline gồm 3 tầng:

```
TMDB (metadata) → NuvFeb API (stream URLs) → ExoPlayer (playback)
```

---

## 1. TMDB API (Metadata)

### Base URL
```
https://api.themoviedb.org/3
```

### Auth
```
Bearer Token (trong Constants.kt → TMDB_BEARER_TOKEN)
Header: Authorization: Bearer eyJhbGciOi...
```

### Endpoints Used

| Endpoint | Purpose | Response |
|----------|---------|----------|
| `GET /trending/movie/week` | Trending movies | `TmdbSearchResponse` |
| `GET /trending/tv/week` | Trending TV | `TmdbSearchResponse` |
| `GET /search/multi?query=...` | Search movies+TV | `TmdbSearchResponse` |
| `GET /movie/{id}` | Movie detail | `TmdbMovieDetail` |
| `GET /tv/{id}` | TV detail | `TmdbTvDetail` |
| `GET /tv/{id}/season/{num}` | Season episodes | `TmdbSeasonDetail` |

### Models
- `TmdbSearchItem` — id, title/name, posterPath, backdropPath, mediaType, voteAverage
- `TmdbMovieDetail` — title, overview, runtime, genres, posterUrl, backdropUrl
- `TmdbTvDetail` — name, overview, numberOfSeasons, seasons list
- `TmdbSeason` — seasonNumber, episodeCount
- `TmdbEpisode` — episodeNumber, name, overview, stillPath

### Poster URLs
```
Thumbnail: https://image.tmdb.org/t/p/w342{posterPath}
Full: https://image.tmdb.org/t/p/w500{posterPath}
Backdrop: https://image.tmdb.org/t/p/w780{backdropPath}
```

---

## 2. NuvFeb API (Stream Links) ⭐ CRITICAL

### Base URL
```
https://febapi.nuvioapp.space
```

### Auth
```
Query param: ?cookie={FEBBOX_UI_COOKIE}
Cookie = JWT from FebBox Google login (febbox.com)
```

### Endpoints

#### Movie Stream
```
GET /api/media/movie/{tmdbId}?cookie={JWT}
```

#### TV Episode Stream
```
GET /api/media/tv/{tmdbId}/{season}/{episode}?cookie={JWT}
```

### Response Format
```json
{
  "url": "https://...m3u8 or mp4",
  "qualities": [
    { "url": "...", "quality": "1080p" },
    { "url": "...", "quality": "720p" },
    { "url": "...", "quality": "360p" }
  ]
}
```

### Cookie Info
- **Source**: Google login to febbox.com → extract `ui` cookie
- **Format**: JWT (eyJhbGciOi...)
- **Expiry**: ~1 year (current expires 2027-02-17)
- **Storage**: `Constants.kt → FEBBOX_COOKIE`

### ⚠️ IMPORTANT NOTES
- Cookie là **required** — không có thì 401
- Chỉ cần **1 API call** để lấy stream URL (không cần ShowBox pipeline)
- Trả về direct MP4 + M3U8 links — ExoPlayer support cả hai
- Ưu tiên M3U8 (adaptive bitrate) > MP4

---

## 3. ShowBox API (Legacy/Alternative)

### Base URL
```
https://mbpapi.shegu.net/api/api_client/index/
```

### Auth
TripleDES encryption (see `ShowBoxCrypto.kt`):
- Key: `123d6cedf626dy54233ed1cfsfdeb26fg`
- IV: `wEIfh28ySYdnhBcQ` (first 8 bytes)
- Algorithm: `DESede/CBC/PKCS5Padding`

### Endpoints

| Endpoint | Body Param | Purpose |
|----------|------------|---------|
| `Search5` | `keyword`, `module=Search5` | Search |
| `TV_detail_v2` | `id={showboxId}`, `module=TV_detail_v2` | TV detail + share_key |
| `Movie_detail` | `id={showboxId}`, `module=Movie_detail` | Movie detail |

### Known Issues
- `showbox.media/index/share_link` → **403 Cloudflare** on ALL platforms
- ShowBox IDs ≠ TMDB IDs — cần search trước rồi map
- Pipeline phức tạp hơn NuvFeb → **DEPRECATED**, chỉ dùng làm fallback

---

## 4. Stream Flow (Code Path)

### Movie
```
User clicks movie → SuperStreamDetailScreen
  → SuperStreamDetailViewModel.playMovie()
  → SuperStreamRepository.streamMovie(tmdbId, title, shareKey)
  → NuvFeb: GET /api/media/movie/{tmdbId}?cookie=...
  → StreamState.Ready(url)
  → Launch PlayerActivity with stream_url, stream_title
  → PlayerScreen → loadDirectStream(url, title)
  → ExoPlayer.setMediaItem(url) + prepare() + play()
```

### TV Episode
```
User clicks episode → TvDetailContent.onEpisodeClick
  → SuperStreamDetailViewModel.playTvEpisode(season, episode)
  → SuperStreamRepository.streamTvEpisode(tmdbId, season, episode, title, shareKey)
  → NuvFeb: GET /api/media/tv/{tmdbId}/{s}/{e}?cookie=...
  → StreamState.Ready(url, season, episode)
  → Launch PlayerActivity with stream_url, stream_season, stream_episode, stream_type
  → PlayerScreen → loadDirectStream(url, title)
  → ExoPlayer auto-play
```

---

## 5. Favorites System

```
Slug format: ss_{type}_{tmdbId}
Examples: ss_movie_550, ss_tv_82856
Source tag: "superstream"
Storage: WatchlistManager (SharedPreferences)
```

- **Add/Remove**: `WatchlistManager.toggle(slug, name, posterUrl, "superstream")`
- **Check**: `watchlistItems.any { it.slug == favSlug }`
- **Display**: Filter `watchlistItems` where `source == "superstream"`

---

## 6. Subtitle Integration

Khi play SuperStream content:
- `streamType` = "movie" hoặc "tv"
- `streamSeason` = season number (TV only)
- `streamEpisode` = episode number (TV only)

Passed to `SubtitleRepository.searchSubtitles(filmName, type, season, episode)` → SubDL search chính xác hơn.

Display format: `🇻🇳 Vietnamese • S01E03` (thay vì raw release name)

---

## 7. Debugging & Troubleshooting

### Stream không load
1. Check cookie expiry: `Constants.FEBBOX_COOKIE`
2. Test API: `curl "https://febapi.nuvioapp.space/api/media/movie/550?cookie={JWT}"`
3. Check NuvFeb domain status
4. Fallback: dùng ShowBox pipeline nếu NuvFeb down

### Anime47 crash (Gson)
- Error: `Expected BEGIN_ARRAY but was BEGIN_OBJECT`
- Fix: `SafeTypeAdapterFactory` trong lenient Gson (ApiClient.kt)
- Root cause: API trả object cho field expected array

### TMDB không có data
- Check bearer token validity
- Verify `language=en-US` param
- Test: `curl -H "Authorization: Bearer {token}" "https://api.themoviedb.org/3/trending/movie/week"`

### FebBox cookie hết hạn
1. Mở browser → febbox.com
2. Login bằng Google
3. F12 → Application → Cookies → copy `ui` value
4. Update `Constants.kt → FEBBOX_COOKIE`

---

## 8. File Map

```
data/api/
├── SuperStreamApi.kt          # Retrofit interfaces (TMDB + ShowBox)
├── ApiClient.kt               # HTTP clients (TMDB, ShowBox, FebBox)
├── SafeTypeAdapterFactory.kt  # Gson crash protection
└── models/
    └── SuperStreamModels.kt   # All TMDB data classes

data/repository/
└── SuperStreamRepository.kt   # Stream pipeline orchestration

ui/screens/superstream/
├── SuperStreamScreen.kt           # Browse + search + favorites
├── SuperStreamDetailScreen.kt     # Movie/TV detail + episodes
├── SuperStreamDetailViewModel.kt  # Detail state management
├── SuperStreamViewModel.kt        # Trending + search
└── SuperStreamComponents.kt       # Reusable UI components

util/
├── ShowBoxCrypto.kt           # TripleDES encryption
├── FebBoxWebViewHelper.kt     # WebView cookie helper
└── Constants.kt               # API keys, URLs, cookies
```

---

## 9. API Keys & Secrets (⚠️ SENSITIVE)

| Key | Location | Notes |
|-----|----------|-------|
| TMDB Bearer | `Constants.TMDB_BEARER_TOKEN` | Free tier, 40 req/10s |
| FebBox Cookie | `Constants.FEBBOX_COOKIE` | JWT, expires ~1 year |
| SubDL API Key | `Constants.SUBDL_API_KEY` | For subtitle search |

---

## 10. Known Limitations

1. **NuvFeb single source** — nếu NuvFeb down, không có fallback tự động
2. **ShowBox 403** — showbox.media Cloudflare block, pipeline không hoạt động
3. **Cookie manual refresh** — cần manual login lại khi JWT expire
4. **No offline** — không cache stream URLs (chúng thay đổi)
5. **TMDB rate limit** — 40 requests per 10 seconds (free tier)
