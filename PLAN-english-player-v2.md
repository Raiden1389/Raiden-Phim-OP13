# 🎬 PLAN: English Player v2 — TMDB + Multi-Source Extractor

## Tổng quan
Thay thế Consumet API (FlixHQ scraper bị vỡ) bằng kiến trúc 2 tầng:
- **TMDB API** → metadata (search, info, poster, TMDB ID)
- **Multi-Source Extractor** → stream URL từ nhiều nguồn

## Kiến trúc

```
[User search] → [TMDB API] → Poster, Title, Year, TMDB ID, IMDB ID
                                    ↓
[User bấm Play] → [Extractor API] → Thử lần lượt:
                                      ├─ VidSrc.xyz  → stream URL ✅
                                      ├─ VidSrc.to   → stream URL ✅
                                      ├─ Autoembed   → stream URL ✅
                                      └─ 2Embed      → stream URL ✅
                                    ↓
                            [ExoPlayer] → Play native
```

---

## Phase 1: Local Test (Node.js) — 📍 BẮT ĐẦU TỪ ĐÂY

### 1.1 Setup TMDB API
- Đăng ký free API key tại https://www.themoviedb.org/settings/api
- Test endpoints:
  - `GET /search/movie?query=The+Housemaid` → tìm phim
  - `GET /movie/{id}` → chi tiết phim (poster, overview, IMDB ID)
  - `GET /tv/{id}` → chi tiết series
  - `GET /movie/{id}/videos` → trailer YouTube
  - `GET /trending/movie/week` → trending

### 1.2 Build Local Extractor (Node.js)
```
phimbox-apk/
  extractor/
    package.json
    index.js          ← Express server (port 3001)
    providers/
      vidsrc-xyz.js   ← VidSrc.xyz extractor
      vidsrc-to.js    ← VidSrc.to extractor
      autoembed.js    ← Autoembed extractor
      superembed.js   ← SuperEmbed extractor
    utils/
      decrypt.js      ← Stream URL decryption helpers
      proxy.js        ← CORS proxy for embed pages
```

### 1.3 Extractor API Design
```
GET /extract?tmdb_id=123456&type=movie
GET /extract?tmdb_id=123456&type=tv&season=1&episode=1

Response:
{
  "sources": [
    {
      "url": "https://xxx/master.m3u8",
      "quality": "auto",
      "provider": "vidsrc-xyz",
      "headers": { "Referer": "..." }
    },
    {
      "url": "https://yyy/master.m3u8",
      "quality": "1080p",
      "provider": "autoembed",
      "headers": {}
    }
  ],
  "subtitles": [
    { "url": "...", "lang": "vi", "label": "Vietnamese" },
    { "url": "...", "lang": "en", "label": "English" }
  ]
}
```

### 1.4 Test Checklist
- [ ] TMDB search trả kết quả đúng
- [ ] TMDB info trả poster, overview, IMDB ID
- [ ] Extractor lấy được ít nhất 1 stream URL
- [ ] Stream URL play được trong VLC/browser
- [ ] Subtitles (nếu có) đúng format

---

## Phase 2: Deploy Extractor (Vercel Serverless)

### 2.1 Convert sang Vercel Serverless Functions
```
extractor/
  api/
    extract.js     ← /api/extract?tmdb_id=xxx
    search.js      ← /api/search?q=xxx (proxy TMDB)
    info.js        ← /api/info?id=xxx (proxy TMDB)
  vercel.json
  package.json
```

### 2.2 Vercel Config
```json
{
  "rewrites": [
    { "source": "/api/:path*", "destination": "/api/:path*" }
  ],
  "headers": [
    {
      "source": "/api/(.*)",
      "headers": [
        { "key": "Access-Control-Allow-Origin", "value": "*" }
      ]
    }
  ]
}
```

### 2.3 Deploy Checklist
- [ ] Deploy lên Vercel 
- [ ] Test /api/extract từ browser
- [ ] UptimeRobot ping mỗi 5 phút
- [ ] Response time < 3s

---

## Phase 3: APK Integration

### 3.1 Thay thế Consumet Models
| Cũ (Consumet) | Mới (TMDB + Extractor) |
|---------------|----------------------|
| `ConsumetSearchResult` | `TMDBMovie` |
| `ConsumetEpisode` | `TMDBEpisode` |
| `ConsumetStreamResponse` | `ExtractorResponse` |
| `ConsumetRepository` | `TMDBRepository` + `ExtractorRepository` |

### 3.2 Files cần sửa trong APK
```
data/api/
  TMDBApi.kt           ← NEW: Retrofit interface cho TMDB
  ExtractorApi.kt      ← NEW: Retrofit interface cho Extractor
  models/
    TMDBModels.kt      ← NEW: Data classes cho TMDB response
    ExtractorModels.kt ← NEW: Data classes cho Extractor response

data/repository/
  TMDBRepository.kt    ← NEW: Search, info, trending
  ExtractorRepository.kt ← NEW: Get stream URLs

ui/screens/english/
  EnglishTab.kt        ← SỬA: Dùng TMDB search thay Consumet
  EnglishDetailScreen.kt ← SỬA: Dùng TMDB info
  EnglishPlayerScreen.kt ← SỬA: Dùng Extractor thay Consumet
  EnglishPlayerViewModel.kt ← SỬA: Call Extractor API

util/Constants.kt      ← SỬA: Thêm TMDB_API_KEY, EXTRACTOR_URL
```

### 3.3 EnglishPlayerViewModel Flow Mới
```kotlin
fun load(tmdbId: Int, type: String, season: Int?, episode: Int?) {
    viewModelScope.launch {
        _isLoading.value = true
        
        // 1. Get stream from Extractor
        ExtractorRepository.extract(tmdbId, type, season, episode)
            .onSuccess { response ->
                // 2. Pick best source
                val best = response.sources.firstOrNull()
                if (best != null) {
                    _streamUrl.value = best.url
                    _refererUrl.value = best.headers["Referer"] ?: ""
                    _subtitles.value = response.subtitles
                }
            }
            .onFailure { _error.value = it.message }
        
        _isLoading.value = false
    }
}
```

---

## Phase 4: Enhancements (Sau khi chạy ổn)

### 4.1 Caching (Upstash Redis)
- Cache TMDB search results (TTL 1 giờ)
- Cache stream URLs (TTL 30 phút)
- Cache subtitles (TTL 24 giờ)

### 4.2 Fallback Chain
```
VidSrc.xyz → fail → VidSrc.to → fail → Autoembed → fail → Error
```

### 4.3 Multi-server cho mỗi phim
- Hiển thị danh sách server (VidSrc, Auto, Super...)
- User chọn server → play

---

## Timeline ước tính

| Phase | Thời gian | Output |
|-------|----------|--------|
| Phase 1: Local Test | 1-2 ngày | Extractor chạy localhost, test OK |
| Phase 2: Deploy | 30 phút | Extractor trên Vercel |
| Phase 3: APK | 1-2 ngày | APK v2.0 với TMDB + Extractor |
| Phase 4: Cache | 1 ngày | Redis caching, multi-server |

**Tổng: ~4-5 ngày**

---

## Ưu tiên ngay bây giờ
1. ✅ Đăng ký TMDB API key
2. ✅ Tạo folder `extractor/`
3. ✅ Build + test VidSrc extractor local
4. ✅ Verify stream URL chạy được
