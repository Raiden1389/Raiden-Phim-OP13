# Plan: Deploy Consumet API lên Vercel + Integrate vào RaidenPhim

## 📋 Tổng quan
Consumet là API aggregator mã nguồn mở, scrape nhiều nguồn phim/anime.
Deploy lên Vercel (miễn phí) để dùng làm backend cho RaidenPhim — mở rộng kho phim Mỹ/English.

---

## 🎯 Mục tiêu
- App có thêm **Tab English** (hoặc gộp vào Home) — phim Mỹ, series Netflix/HBO với sub English
- Bổ sung nguồn anime English nếu Anime47 down

---

## Phase 1: Deploy Consumet lên Vercel (15 phút)

### Bước 1 — Fork repo
```
https://github.com/consumet/api.consumet.org → Fork về GitHub cá nhân
```

### Bước 2 — Deploy 1-click
- Vào Vercel Dashboard → "Add New Project" → Import từ GitHub fork
- Set env: `NODE_ENV=PROD`
- Deploy → nhận URL: `https://consumet-xyz.vercel.app/`

### Bước 3 — Test API
```
GET /movies/flixhq/trending         → Phim trending
GET /movies/flixhq/info?id=xxx      → Chi tiết phim
GET /movies/flixhq/watch?episodeId= → Link stream
GET /movies/flixhq/search?query=xxx → Tìm kiếm
GET /anime/gogoanime/trending       → Anime trending
```

### ⚠️ Lưu ý Vercel Free Tier
| Limit | Giá trị |
|-------|---------|
| Serverless Functions | 100GB-Hours/tháng |
| Bandwidth | 100GB/tháng |
| Timeout | 10 giây/request |
| | **Đủ dùng cho cá nhân** |

---

## Phase 2: Integrate vào RaidenPhim (2-3 giờ)

### 2.1 — Thêm Consumet API Client
```kotlin
// Constants.kt
const val CONSUMET_BASE_URL = "https://consumet-xyz.vercel.app/"

// ConsumetApi.kt — Retrofit interface
interface ConsumetApi {
    @GET("movies/flixhq/trending")
    suspend fun getTrending(@Query("page") page: Int = 1): ConsumetResponse

    @GET("movies/flixhq/{id}")
    suspend fun getInfo(@Path("id") id: String): ConsumetDetail

    @GET("movies/flixhq/watch")
    suspend fun getStreamLinks(@Query("episodeId") epId: String,
                                @Query("mediaId") mediaId: String): ConsumetStreamResponse

    @GET("movies/flixhq/{query}")
    suspend fun search(@Path("query") query: String): ConsumetSearchResponse
}
```

### 2.2 — Data Models
```kotlin
data class ConsumetResponse(
    val currentPage: Int,
    val hasNextPage: Boolean,
    val results: List<ConsumetItem>
)

data class ConsumetItem(
    val id: String,
    val title: String,
    val image: String,
    val type: String,          // "Movie" | "TV Series"
    val releaseDate: String?,
    val url: String?
)

data class ConsumetStreamResponse(
    val sources: List<StreamSource>,
    val subtitles: List<Subtitle>
)

data class StreamSource(
    val url: String,           // M3U8 link
    val quality: String,       // "1080p", "720p", etc.
    val isM3U8: Boolean
)

data class Subtitle(
    val url: String,
    val lang: String
)
```

### 2.3 — UI: Tab English hoặc Source Selector
**Option A**: Thêm tab "English" vào Bottom Nav (bên cạnh Anime)
**Option B**: Thêm "Source: OPhim | FlixHQ" filter trên Home

### 2.4 — Player Integration
- FlixHQ trả M3U8 links → ExoPlayer đã support sẵn
- **Bonus**: Có sẵn subtitles (English, Vietnamese) → load vào ExoPlayer subtitle track

---

## Phase 3: Providers có thể dùng

| Provider | Endpoint | Nội dung | Chất lượng |
|----------|----------|----------|------------|
| **FlixHQ** | `/movies/flixhq/` | Phim Mỹ, Netflix, HBO, Disney+ | 1080p |
| **ViewAsian** | `/movies/viewasian/` | Phim Hàn, drama châu Á | 720-1080p |
| **Gogoanime** | `/anime/gogoanime/` | Anime sub/dub English | 720-1080p |
| **9anime** | `/anime/9anime/` | Anime chất lượng cao | 1080p |
| **Zoro** | `/anime/zoro/` | Anime không quảng cáo | 1080p |

---

## ⚡ Tóm tắt Effort

| Phase | Thời gian | Độ khó |
|-------|-----------|--------|
| Deploy Vercel | 15 phút | ⭐ Dễ |
| API Client + Models | 1 giờ | ⭐⭐ Trung bình |
| UI Tab + Browse | 1-2 giờ | ⭐⭐ Trung bình |
| Player + Subtitles | 30 phút | ⭐ Dễ (ExoPlayer có sẵn) |
| **Tổng** | **~3 giờ** | |

---

## ❓ Câu hỏi cho đại ca
1. Muốn thêm tab riêng "English" hay gộp vào Home với source selector?
2. Ưu tiên FlixHQ (phim Mỹ) hay ViewAsian (phim Hàn) trước?
3. Có muốn thêm Subtitle support luôn không (Consumet trả sẵn)?
