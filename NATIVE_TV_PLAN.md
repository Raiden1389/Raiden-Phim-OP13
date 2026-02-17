# 📺 Raiden Phim TV — Native App Plan

> **Mục tiêu:** Xây app xem phim native cho Android TV box, mượt như Netflix, dùng cho gia đình.
> **Ngày tạo:** 2026-02-13
> **Cập nhật:** 2026-02-14
> **Trạng thái:** � Phase 1-5 DONE — Đang polish perf + Phase 6-7

---

## 1. Tại sao chuyển Native?

### Vấn đề WebView (hiện tại)
| Vấn đề | Nguyên nhân | Đã fix? |
|---|---|---|
| Lag khi bấm D-pad giữa cards | WebView focus engine chậm trên chip yếu | ✅ Virtual focus (vẫn micro-lag) |
| Load lần đầu chậm | 5 API + decode 75 poster cùng lúc | ✅ Progressive render |
| Sidebar lag khi mở | Repaint toàn màn hình | ✅ Đã giảm |
| Player không fullscreen | WebView iframe limitation | ✅ CSS fix |

**Kết luận:** Đã tối ưu hết mức WebView cho phép. Vẫn lag vì **WebView trên Android TV chip yếu = có trần performance**. Netflix, Kodi, RoPhim mượt vì dùng **native UI**.

### WebView vs Native
| Tiêu chí | WebView | Native |
|---|---|---|
| D-pad focus | 🔴 Hack bằng JS, vẫn lag | 🟢 Native focus engine, 0 lag |
| Image decode | 🔴 Browser decode, chậm | 🟢 Coil/Glide, auto resize + cache |
| Player | 🔴 iframe embed | 🟢 ExoPlayer, hardware decode |
| Memory | 🔴 ~100MB RAM | 🟢 ~30-50MB |
| Virtualization | 🔴 Phải tự implement | 🟢 LazyRow/Column (standard Compose, TvLazy deprecated) |

---

## 2. Tech Stack

| Component | Thư viện | Lý do |
|---|---|---|
| **Language** | Kotlin | Ngôn ngữ chính cho Android |
| **UI** | Jetpack Compose for TV + Standard Compose | TV Material3 cho theme, **standard LazyRow/Column/Grid** thay TvLazy* (deprecated) |
| **Navigation** | Compose Navigation | Single Activity, nhiều Screen |
| **Player** | Media3 ExoPlayer | God-tier player, Netflix/YouTube cũng dùng |
| **Images** | Coil Compose | Tự resize theo Image size, disk cache |
| **Network** | Retrofit + OkHttp + Gson | Standard cho Android |
| **Storage** | DataStore Preferences | Async, không block UI thread. SharedPrefs choke khi data lớn |
| **Focus** | Compose TV built-in | D-pad handling tự động, không cần code |

### Không dùng:
- ❌ Room DB — quá phức tạp cho data đơn giản
- ❌ Hilt/Dagger — app nhỏ, không cần DI framework
- ❌ Flutter — chưa support TV chính thức
- ❌ React Native TV — bridge overhead, không mượt bằng native

---

## 3. API — Multi-Source (OPhim + KKPhim)

> ⚠️ **Quan trọng:** OPhim thiếu nhiều show (Running Man, variety shows).
> KKPhim (phimapi.com) có đầy đủ hơn, API format gần giống.
> → **Kết hợp cả 2 nguồn.**

### 3.1 OPhim (nguồn chính)

Base URL: `https://ophim1.com/v1/api`
Image CDN: `https://img.ophim.live/uploads/movies/`

| Endpoint | Mô tả |
|---|---|
| `GET /danh-sach/phim-moi-cap-nhat?page=1` | Phim mới |
| `GET /danh-sach/phim-bo?page=1` | Phim bộ |
| `GET /danh-sach/phim-le?page=1` | Phim lẻ |
| `GET /danh-sach/hoat-hinh?page=1` | Hoạt hình |
| `GET /danh-sach/tv-shows?page=1` | TV Shows |
| `GET /phim/{slug}` | Chi tiết + episodes |
| `GET /tim-kiem?keyword=xxx` | Tìm kiếm |

### 3.2 KKPhim (nguồn bổ sung)

Base URL: `https://phimapi.com`
Image CDN: `https://phimimg.com`

| Endpoint | Mô tả |
|---|---|
| `GET /danh-sach/phim-moi-cap-nhat?page=1` | Phim mới |
| `GET /v1/api/danh-sach/phim-bo?page=1` | Phim bộ |
| `GET /v1/api/danh-sach/phim-le?page=1` | Phim lẻ |
| `GET /v1/api/danh-sach/tv-shows?page=1` | TV Shows |
| `GET /phim/{slug}` | Chi tiết + episodes |
| `GET /v1/api/tim-kiem?keyword=xxx` | Tìm kiếm |

### 3.3 Strategy — Cách kết hợp

```kotlin
// Chiến lược: OPhim chính, KKPhim bổ sung
interface MovieSource {
    suspend fun getNewMovies(page: Int): List<Movie>
    suspend fun getDetail(slug: String): MovieDetail
    suspend fun search(keyword: String): List<Movie>
}

class OPhimSource : MovieSource { ... }
class KKPhimSource : MovieSource { ... }

// Aggregator: merge + deduplicate by slug
class MultiSourceRepository(
    private val ophim: OPhimSource,
    private val kkphim: KKPhimSource
) {
    // HomeScreen: load từ OPhim, bổ sung từ KKPhim nếu thiếu
    // Search: search cả 2, merge results, deduplicate by slug
    // Detail: thử OPhim trước, fallback KKPhim
}
```

**Ưu tiên:**
1. Danh sách phim mới / phim bộ / phim lẻ → **OPhim** (nhanh hơn)
2. Search → **Cả 2** (merge kết quả, loại trùng bằng slug)
3. Chi tiết phim → **OPhim trước**, fallback **KKPhim** nếu không tìm thấy
4. TV Shows / Variety Shows → **KKPhim** (OPhim thiếu)

### 3.4 Response Adapter

2 API format gần giống, chỉ khác wrapper:

```kotlin
// OPhim: response.data.items
// KKPhim: response.items (không có data wrapper)
// → Adapter normalize về cùng List<Movie>
```

### Response structure:
```json
{
  "status": "success",
  "data": {
    "items": [
      {
        "name": "Tây Du Ký",
        "slug": "tay-du-ky",
        "thumb_url": "tay-du-ky-thumb.jpg",
        "poster_url": "tay-du-ky-poster.jpg",
        "year": 2024,
        "quality": "HD",
        "lang": "Vietsub",
        "episode_current": "Tập 20",
        "country": [{"name": "Trung Quốc", "slug": "trung-quoc"}],
        "category": [{"name": "Hành Động", "slug": "hanh-dong"}]
      }
    ]
  }
}
```

### Episode structure (từ chi tiết phim):
```json
{
  "episodes": [
    {
      "server_name": "Vietsub #1",
      "server_data": [
        {
          "name": "Tập 1",
          "slug": "tap-1",
          "link_embed": "https://player.xxx/embed/...",
          "link_m3u8": "https://xxx.m3u8"
        }
      ]
    }
  ]
}
```

---

## 4. Screens — Chi tiết

### 4.1 HomeScreen (Trang chủ)

**Layout:**
```
┌──────────────────────────────────────────────────┐
│ [Sidebar]  │  ┌─────────────────────────────────┐│
│  🏠 Home   │  │  🎬 HERO BANNER (full width)    ││
│  📺 Bộ     │  │  poster_url background, blur    ││
│  🎬 Lẻ     │  │                                 ││
│  🇰🇷 Hàn   │  │  Tây Du Ký                      ││
│  � Anime  │  │  2024 • Trung Quốc • HD         ││
│  ❤️ Thích  │  │  [▶ Xem Ngay]  [ℹ️ Chi Tiết]    ││
│  ⏱ Sử     │  └─────────────────────────────────┘│
│  🔍 Tìm    │                                     │
│            │  ⏯ Đang Xem: Cơn Say Mùa Xuân      │
│            │    Tập 12 • 14 phút trước            │
│            │  ──────────────────────────────────  │
│            │  🔥 Phim Mới Cập Nhật               │
│            │  [card][card][card][card][card]→     │
│            │  ──────────────────────────────────  │
│            │  📺 Phim Bộ                          │
│            │  [card][card][card][card][card]→     │
└──────────────────────────────────────────────────┘
```

**Components:**
- `HeroBanner` — Netflix-style: poster_url làm background (blur), hiện tên + info + 2 nút
- `ContinueWatchingCard` — 1 card to nằm ngay dưới hero, auto-focus khi mở app
- `MovieRow` — LazyRow (standard Compose) chứa movie cards
- `MovieCard` — Poster + tên + remember(ImageRequest) + size(240,340)
- `Sidebar` — NavigationDrawer của Compose TV

### 🎬 Hero Banner — Chi tiết

**Nguồn data:** Random 1 phim từ danh sách "Phim Mới Cập Nhật"

**Layout:**
- Background: `poster_url` full width, blur + gradient overlay đen từ dưới lên
- Góc trái dưới: Tên phim (font to) + Year + Country + Quality
- 2 nút: `[▶ Xem Ngay]` `[ℹ️ Chi Tiết]`
- Chiều cao: ~40% màn hình

**Behavior:**
- Focus mặc định vào nút "Xem Ngay" (nếu không có Continue Watching)
- Enter trên "Xem Ngay" → play tập 1
- Enter trên "Chi Tiết" → navigate tới DetailScreen

### 👀 Sneak Peek (Preview khi focus card)

**Cách hoạt động:**
- Khi focus card ≥ 2 giây → load `link_m3u8` tập 1 trong Hero Banner area (thay poster)
- Play muted, 15 giây preview
- Nếu di chuyển focus sang card khác → dừng preview, hiện poster card mới
- **Nếu box quá yếu:** có thể tắt sneak peek bằng setting

> ⚠️ Sneak Peek là **Phase 2 feature** — implement sau khi app cơ bản chạy ổn.
> Ưu tiên MVP trước, thêm preview sau.

**Behavior:**
- Mở app → focus vào Continue Watching (nếu có), không thì focus nút "Xem Ngay" trên Hero
- D-pad ↓ → xuống Continue Watching → rows
- D-pad ← ở cột đầu → mở sidebar
- D-pad → từ sidebar → đóng sidebar, focus card

**Data flow — Progressive Loading:**
```
App mở
  ┌─ NGAY LẬP TỨC (< 500ms)
  │  → Load Continue Watching từ DataStore (async, cached)
  │  → Load API "Phim Mới" (hero + row 1)
  │  → Render: Hero Banner + Continue Watching
  │
  ├─ SAU KHI HERO HIỆN (background)
  │  → Load "Phim Bộ" (row 2)
  │  → Load "Phim Lẻ" (row 3)
  │
  └─ KHI USER SCROLL XUỐNG (lazy trigger)
     → Load "Phim Hàn" (row 4)
     → Load thêm rows nếu cần

  ⚠️ Mỗi API response → filter country TẠI REPOSITORY
     → UI chỉ nhận data đã filter → không render rồi mới xóa
```

### 4.2 DetailScreen (Chi tiết phim)

**Layout:**
```
┌──────────────────────────────────────────┐
│ [Poster]  Tây Du Ký                      │
│  200x300  2024 • Trung Quốc • HD         │
│           Hành Động, Phiêu Lưu           │
│                                          │
│           [▶ XEM PHIM]  [❤️ YÊU THÍCH]   │
│                                          │
│  Server: [Vietsub #1] [Thuyết Minh]      │
│                                          │
│  [01] [02] [03] [04] [05] [06] [07] [08] │
│  [09] [10] [11] [12] [13] [14] [15] [16] │
│  ... (LazyGrid — chỉ render visible)     │
└──────────────────────────────────────────┘
```

**Components:**
- `DetailHero` — Poster + info
- `ServerTabs` — Chọn server (Vietsub, Thuyết Minh)
- `EpisodeGrid` — LazyVerticalGrid (quan trọng cho phim 60-80 tập!)

**Behavior:**
- Enter trên card ở Home → navigate tới Detail
- Focus mặc định vào nút "Xem Phim" (play tập gần nhất)
- D-pad ↓ → vào episode grid
- Back → quay lại Home

### 4.3 PlayerScreen (Xem phim)

**Layout:**
```
┌──────────────────────────────────────────┐
│                                          │
│          [ExoPlayer Fullscreen]          │
│                                          │
│   advancement bar                        │
│  ◀◀ -10s    ▶ Play/Pause    ▶▶ +10s     │
│  Tập 12/80         Tây Du Ký            │
│                                          │
└──────────────────────────────────────────┘
```

**Components:**
- ExoPlayer (Media3)
- Custom controls overlay (ẩn sau 3s)

**Behavior:**
- Load `link_m3u8` bằng ExoPlayer (HLS stream)
- Fallback: load `link_embed` trong WebView (cho server không có m3u8)
- D-pad ←/→ = tua 10s
- D-pad center = play/pause
- Khi hết tập → **auto play tập kế** (quan trọng cho vợ xem series!)
- Back → quay lại Detail
- Lưu progress vào SharedPreferences (continue watching)

**Player priority:**
1. `link_m3u8` → ExoPlayer native (mượt nhất)
2. `link_embed` → WebView fallback (nếu không có m3u8)

### 4.4 SearchScreen

**Layout:**
```
┌──────────────────────────────────────────┐
│  🔍 [_______________] (TV keyboard)      │
│                                          │
│  Kết quả:                                │
│  [card][card][card][card]                │
│  [card][card][card][card]                │
└──────────────────────────────────────────┘
```

**Behavior:**
- Dùng Android TV system keyboard
- Debounce search 500ms
- LazyGrid hiện kết quả

### 4.5 FavoriteScreen + HistoryScreen

- LazyGrid hiện movies đã lưu
- Data từ SharedPreferences
- Favorite: lưu slug + name + thumb
- History: lưu slug + name + thumb + last episode + timestamp

---

## 5. Data Models

```kotlin
// Movie (from API)
data class Movie(
    val name: String,
    val slug: String,
    val thumb_url: String,
    val poster_url: String,
    val year: Int?,
    val quality: String?,
    val lang: String?,
    val episode_current: String?,
    val country: List<Category>?,
    val category: List<Category>?,
    val content: String?,       // mô tả (HTML)
    val episodes: List<Server>? // chỉ có trong detail API
)

data class Category(val name: String, val slug: String)

data class Server(
    val server_name: String,
    val server_data: List<Episode>
)

data class Episode(
    val name: String,
    val slug: String,
    val link_embed: String?,
    val link_m3u8: String?
)

// Local storage
data class ContinueWatching(
    val slug: String,
    val name: String,
    val thumb: String,
    val serverIndex: Int,
    val episodeIndex: Int,
    val episodeName: String,
    val position: Long,    // playback position in ms
    val timestamp: Long    // khi nào xem
)

data class FavoriteMovie(
    val slug: String,
    val name: String,
    val thumb: String,
    val addedAt: Long
)
```

---

## 6. Project Structure

```
app/src/main/java/xyz/raidenhub/phimtv/
├── MainActivity.kt              — Entry point, single activity
├── RaidenApp.kt                 — Application class
│
├── data/
│   ├── api/
│   │   ├── OPhimApi.kt          — Retrofit interface
│   │   ├── ApiClient.kt         — Retrofit instance
│   │   └── models/              — API response models
│   │       ├── Movie.kt
│   │       ├── MovieDetail.kt
│   │       ├── Episode.kt
│   │       └── ApiResponse.kt
│   │
│   └── local/
│       ├── PrefsManager.kt      — SharedPreferences wrapper
│       ├── FavoriteStore.kt     — Favorites CRUD
│       ├── HistoryStore.kt      — Watch history CRUD
│       └── ContinueStore.kt    — Continue watching CRUD
│
├── ui/
│   ├── theme/
│   │   ├── Theme.kt            — Dark theme cho TV
│   │   ├── Color.kt            — Color palette
│   │   └── Type.kt             — Typography
│   │
│   ├── components/
│   │   ├── MovieCard.kt        — Poster + tên (ultra lite)
│   │   ├── MovieRow.kt         — LazyRow of cards (migrated from TvLazyRow)
│   │   ├── HeroBanner.kt      — Netflix-style hero (poster bg + buttons)
│   │   ├── ContinueCard.kt    — Card "Đang xem" to
│   │   ├── EpisodeGrid.kt     — LazyGrid episode buttons
│   │   └── ServerTabs.kt      — Server selector
│   │
│   ├── screens/
│   │   ├── HomeScreen.kt       — Trang chủ
│   │   ├── DetailScreen.kt     — Chi tiết phim
│   │   ├── PlayerScreen.kt     — ExoPlayer
│   │   ├── SearchScreen.kt     — Tìm kiếm
│   │   ├── FavoriteScreen.kt   — Yêu thích
│   │   └── HistoryScreen.kt    — Lịch sử
│   │
│   └── navigation/
│       └── AppNavigation.kt    — NavHost + routes
│
└── util/
    ├── ImageUtils.kt           — URL builder cho poster
    └── TimeUtils.kt            — Format "14 phút trước"
```

## 7. 🎨 UI/UX Design — Chi tiết

### 7.1 App Branding

| Item | Giá trị |
|---|---|
| **App Name** | **Raiden Phim** |
| **Package** | `xyz.raidenhub.phimtv` |
| **Icon** | Logo hiện tại (chữ R gradient) |
| **Hiện tên trong app?** | ❌ **KHÔNG** — như Netflix, không hiện tên app trong UI |
| **Splash screen** | Logo Raiden fade in → fade out → HomeScreen (1.5s) |

> Tại sao không hiện tên? Vì TV screen bé, mỗi pixel quý giá. Netflix, Disney+ đều không hiện tên app trên HomeScreen.

### 7.2 Color Palette — Dark Theme (cố định)

TV **LUÔN** dùng dark mode. Không có light mode.

```kotlin
// Color.kt
object RaidenColors {
    // ═══ BACKGROUND ═══
    val Background     = Color(0xFF0D0D1A)  // near-black với hint xanh navy
    val Surface        = Color(0xFF1A1A2E)  // card background
    val SurfaceVariant = Color(0xFF252542)  // sidebar background, hơi sáng hơn

    // ═══ ACCENT ═══
    val Primary        = Color(0xFFE50914)  // đỏ Netflix-inspired (nút chính)
    val PrimaryDark    = Color(0xFFB20710)  // đỏ đậm khi pressed
    val Accent         = Color(0xFF00D4FF)  // cyan neon (focus ring, highlight)

    // ═══ TEXT ═══
    val TextPrimary    = Color(0xFFE8E8E8)  // trắng mềm (title, tên phim)
    val TextSecondary  = Color(0xFF9E9E9E)  // xám (year, quality, info)
    val TextMuted      = Color(0xFF5A5A7A)  // xám tối (placeholder)

    // ═══ FUNCTIONAL ═══
    val FocusBorder    = Color(0xFFE50914)  // viền đỏ khi card focused
    val ProgressBar    = Color(0xFFE50914)  // progress Continue Watching
    val Error          = Color(0xFFFF5252)  // lỗi
    val Overlay        = Color(0x99000000)  // 60% black overlay trên hero

    // ═══ GRADIENT ═══
    val HeroGradient = listOf(
        Color.Transparent,
        Color(0x40000000),   // 25%
        Color(0xCC0D0D1A),   // 80%
        Color(0xFF0D0D1A),   // 100% — merge vào background
    )
}
```

**Tại sao chọn bảng màu này?**
- **Background #0D0D1A** — đen nhưng KHÔNG true black (#000), mắt dễ chịu hơn trên TV
- **Accent đỏ** — tạo cảm giác premium, quen thuộc (Netflix)
- **Focus cyan** — nổi bật trên nền tối, dễ thấy bằng remote

### 7.3 Typography

```kotlin
// Type.kt
val RaidenTypography = Typography(
    // Hero title
    displayLarge = TextStyle(
        fontFamily = FontFamily.Default,   // Roboto (Android default, đẹp trên TV)
        fontWeight = FontWeight.Bold,
        fontSize = 36.sp
    ),
    // Section title (🔥 Phim Mới)
    headlineMedium = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp,
        color = RaidenColors.TextPrimary
    ),
    // Card title
    titleMedium = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        maxLines = 2
    ),
    // Subtitle (year, quality, country)
    bodySmall = TextStyle(
        fontSize = 12.sp,
        color = RaidenColors.TextSecondary
    )
)
```

> Dùng **Roboto** mặc định — đã tối ưu sẵn cho Android TV, không cần load custom font.

### 7.4 Sidebar — Behavior Chi Tiết

**Compose TV dùng `NavigationDrawer` built-in** — KHÔNG phải tự code sidebar.

```
TRẠNG THÁI 1: Thu gọn (mặc định)         TRẠNG THÁI 2: Mở rộng
┌──┐──────────────────────────┐        ┌──────────┐──────────────────┐
│🏠│  HERO BANNER             │        │ 🏠 Home  │ HERO BANNER      │
│📺│  ...                     │        │ 📺 Bộ    │ (bị che bớt)    │
│🎬│                          │        │ 🎬 Lẻ    │                  │
│🇰🇷│  Phim Mới               │        │ 🇰🇷 Hàn   │ Phim Mới        │
│🎌│  [card][card][card]→     │        │ 🎌 Anime │ [card][card]→    │
│❤│  ...                     │        │ ❤️ Thích  │ ...              │
│⏱│                          │        │ ⏱ Sử     │                  │
│🔍│                          │        │ 🔍 Tìm    │                  │
└──┘──────────────────────────┘        └──────────┘──────────────────┘
 56dp                                   200dp
```

**Behavior:**
| Action | Kết quả |
|---|---|
| D-pad ← ở card đầu hàng | Sidebar **mở rộng** (200dp), focus vào item |
| D-pad → từ sidebar | Sidebar **thu gọn** (56dp), focus vào card |
| Enter trên sidebar item | Navigate tới screen tương ứng |
| Back button khi sidebar mở | Thu gọn sidebar |

**Sidebar Items:**

```kotlin
enum class SidebarItem(val icon: ImageVector, val label: String, val route: String) {
    HOME     (Icons.Home,          "Trang chủ",  "home"),
    SERIES   (Icons.Tv,            "Phim Bộ",    "category/phim-bo"),
    MOVIES   (Icons.Movie,         "Phim Lẻ",    "category/phim-le"),
    KOREAN   (Icons.Flag,          "Hàn Quốc",   "category/han-quoc"),
    ANIME    (Icons.Animation,     "Anime",       "category/hoat-hinh"),
    FAVORITE (Icons.Favorite,      "Yêu thích",  "favorites"),
    HISTORY  (Icons.History,       "Lịch sử",    "history"),
    SEARCH   (Icons.Search,        "Tìm kiếm",   "search"),
}
```

**Khi thu gọn:** Chỉ hiện icon (56dp width)
**Khi mở rộng:** Icon + label (200dp width)

### 7.5 Focus System

```
┌─────────────────────┐
│  Card bình thường    │   Card focused
│  ┌─────────────┐    │   ┌─────────────────┐
│  │             │    │   │                 │  ← border 2dp đỏ
│  │   Poster    │    │   │    Poster       │  ← scale 1.05x
│  │   140x200   │    │   │    147x210      │
│  │             │    │   │                 │
│  └─────────────┘    │   └─────────────────┘
│  Tây Du Ký          │   Tây Du Ký            ← text sáng hơn
│  2024 • HD          │   2024 • HD
└─────────────────────┘   
```

```kotlin
// Composable modifier cho card
Modifier
    .onFocusChanged { state ->
        isFocused = state.isFocused
    }
    .scale(if (isFocused) 1.05f else 1.0f)
    .border(
        width = if (isFocused) 2.dp else 0.dp,
        color = if (isFocused) RaidenColors.FocusBorder else Color.Transparent,
        shape = RoundedCornerShape(8.dp)
    )
```

### 7.6 Performance — ĐÃ IMPLEMENT ✅ (Phase 8)

#### Image (Coil Global Cache)
- **RaidenApp** implements `ImageLoaderFactory` — Coil configured globally
- Memory cache: **50MB LRU**
- Disk cache: **100MB** at `cacheDir/coil_cache`
- `respectCacheHeaders(false)` — bỏ qua server no-cache
- MovieCard: `remember(fullUrl)` + `.size(240, 340)` — decode đúng card size
- HeroBanner: `remember(heroUrl)` + `.size(1280, 720)` — decode 720p
- DetailScreen: backdrop `.size(1280, 720)`, poster `.size(280, 390)`

#### Focus
- Border highlight `2dp` khi focus (không scale → tránh layout push)
- **Sidebar:** `graphicsLayer.translationX` animation — GPU-only, 0 recomposition

#### Data Immutability
- `@Immutable` trên: `Movie`, `Category`, `MovieDetail`, `Episode`, `EpisodeServer`
- Movie.source: `val` (immutable) + `tagSource()` dùng `.copy()`
- `MovieRepository` → **object singleton** — shared cache, 1 instance

#### Caching
- **OkHttp 50MB disk cache** — HTTP responses cached tự động
- **HomeCache (DataStore)** — 5 rows phim cached, mở app < 100ms, TTL 30 phút
- **Coil image cache** — 50MB RAM + 100MB disk

#### Memory
- Static shapes in MovieCard (no alloc per recompose)
- Debug-only OkHttp logging (`BuildConfig.DEBUG`)
- Proguard rules cho Gson/Retrofit/OkHttp/Compose

#### Compose Migration (TvLazy* → standard Lazy*)
- `TvLazyRow` → `LazyRow` (MovieRow)
- `TvLazyColumn` → `LazyColumn` (HomeScreen)
- `TvLazyVerticalGrid` → `LazyVerticalGrid` (Category, Search, Favorites, Detail)
- `rememberTvLazyGridState` → `rememberLazyGridState`
- **Lý do:** TvLazy* deprecated, standard Compose lists có pausable composition + prefetching tốt hơn

#### Network
- 5 API calls song song bằng coroutines
- Timeout 10s
- Retry 1 lần nếu fail

---

## 8. Country Filter (quan trọng — yêu cầu gia đình)

**Nhà sếp chỉ xem 3 nước:**
- 🇨🇳 Trung Quốc (vợ xem chính)
- 🇺🇸 Mỹ / Âu Mỹ
- 🇰🇷 Hàn Quốc

**Không hiện** phim từ các nước khác (Thái, Ấn Độ, Nhật, Đài Loan, etc.)

Filter client-side sau khi fetch API — loại bỏ movie nếu `country.slug` không nằm trong whitelist:
```kotlin
val ALLOWED_COUNTRIES = setOf("trung-quoc", "au-my", "han-quoc")
```

> ⚠️ Đây là business requirement cứng — không thay đổi.

---

## 8.5 📁 Project Structure + Component Architecture — Chi Tiết

### File Structure (Mỗi file = 1 trách nhiệm)

```
phimtv/app/src/main/java/xyz/raidenhub/phimtv/
│
├── MainActivity.kt                   — Entry point, setContent { RaidenApp() }
├── RaidenApp.kt                      — Theme + NavigationDrawer + NavHost
│
├── data/
│   ├── api/
│   │   ├── OPhimApi.kt               — Retrofit interface cho OPhim
│   │   ├── KKPhimApi.kt              — Retrofit interface cho KKPhim  
│   │   ├── ApiClient.kt              — Retrofit builder, OkHttp config
│   │   └── models/
│   │       ├── Movie.kt              — Data class cho list item
│   │       ├── MovieDetail.kt        — Data class cho chi tiết
│   │       ├── Episode.kt            — Data class cho tập phim
│   │       ├── ServerData.kt         — Data class cho server + link
│   │       └── ApiResponse.kt        — Wrapper response (OPhim vs KKPhim)
│   │
│   ├── repository/
│   │   ├── MovieRepository.kt        — Aggregator: merge 2 nguồn, dedup, filter country
│   │   └── PlayerRepository.kt       — Quản lý server selection, failover  
│   │
│   └── local/
│       ├── PrefsManager.kt           — SharedPreferences singleton
│       ├── FavoriteStore.kt          — CRUD favorites (slug, name, poster)
│       ├── HistoryStore.kt           — CRUD watch history
│       └── ContinueStore.kt          — CRUD continue watching (slug, episode, position, timestamp)
│
├── ui/
│   ├── theme/
│   │   ├── Theme.kt                  — MaterialTheme(darkColorScheme, typography)
│   │   ├── Color.kt                  — RaidenColors object
│   │   └── Type.kt                   — RaidenTypography
│   │
│   ├── components/
│   │   ├── MovieCard.kt              — Focusable card: poster + title (props: Movie, onFocus, onClick)
│   │   ├── MovieRow.kt               — Section header + LazyRow of MovieCards
│   │   ├── HeroBanner.kt             — Full-width hero: bg poster + gradient + info + buttons
│   │   ├── ContinueWatchingRow.kt    — LazyRow of ContinueCards (3-5 items) [NOT YET CREATED]
│   │   ├── ContinueCard.kt           — Poster + title + episode + progress bar
│   │   ├── EpisodeGrid.kt            — LazyVerticalGrid of episode buttons
│   │   ├── ServerSelector.kt         — Row of server tabs
│   │   ├── PlayerOverlay.kt          — Play/pause, seek bar, speed, time
│   │   ├── ErrorView.kt              — "Không thể tải" + Thử lại button
│   │   └── LoadingView.kt            — Centered CircularProgressIndicator
│   │
│   ├── screens/
│   │   ├── HomeScreen.kt             — Orchestrator: Hero + ContinueRow + MovieRows
│   │   ├── HomeViewModel.kt          — State: movies, heroIndex, continueList, loading, error
│   │   ├── DetailScreen.kt           — Poster + info + ServerSelector + EpisodeGrid
│   │   ├── DetailViewModel.kt        — State: movieDetail, selectedServer, episodes
│   │   ├── PlayerScreen.kt           — ExoPlayer surface + PlayerOverlay
│   │   ├── PlayerViewModel.kt        — State: playbackState, speed, currentServer
│   │   ├── SearchScreen.kt           — Search input + results grid
│   │   ├── SearchViewModel.kt        — State: query, results, loading
│   │   ├── FavoriteScreen.kt         — Grid of saved movies
│   │   └── HistoryScreen.kt          — List of watched movies
│   │
│   └── navigation/
│       └── AppNavigation.kt          — NavHost + route definitions + arguments
│
└── util/
    ├── ImageUtils.kt                 — buildImageUrl(source, path) → full URL
    ├── TimeUtils.kt                  — formatTimeAgo(timestamp) → "14 phút trước"
    ├── TextUtils.kt                  — normalize(name) cho dedup
    └── Constants.kt                  — API URLs, allowed countries, config values
```

### Component Props & State — Chi tiết mỗi component

#### `MovieCard.kt`
```kotlin
@Composable
fun MovieCard(
    movie: Movie,               // data
    onFocus: (Movie) -> Unit,   // callback: hero banner đổi khi focus
    onClick: (Movie) -> Unit,   // callback: navigate tới detail
    modifier: Modifier = Modifier
)
// Internal state: isFocused (Boolean)
// Renders: Card { AsyncImage + Column { Text(title), Text(year) } }
```

#### `MovieRow.kt`
```kotlin
@Composable
fun MovieRow(
    title: String,              // "🔥 Phim Mới Cập Nhật"
    movies: List<Movie>,
    onMovieFocus: (Movie) -> Unit,
    onMovieClick: (Movie) -> Unit,
    modifier: Modifier = Modifier
)
// Renders: Column { Text(title) + LazyRow { items(movies) { MovieCard() } } }
// LazyRow tự handle D-pad ← → + virtualization (migrated from TvLazyRow)
```

#### `HeroBanner.kt`
```kotlin
@Composable
fun HeroBanner(
    movies: List<Movie>,        // top 5 hot movies
    currentIndex: Int,          // auto rotate index
    onWatchClick: (Movie) -> Unit,
    onDetailClick: (Movie) -> Unit
)
// Internal: Crossfade animation khi index thay đổi
// Renders: Box {
//   AsyncImage(poster, blur)
//   Gradient overlay (bottom → top)
//   Column { Text(name), Text(info), Row { Button("Xem Ngay"), Button("Chi Tiết") } }
// }
```

#### `ContinueWatchingRow.kt`
```kotlin
@Composable
fun ContinueWatchingRow(
    items: List<ContinueWatching>,   // sorted by timestamp desc
    onClick: (ContinueWatching) -> Unit
)
// Renders: Column { Text("⏯ Đang Xem") + TvLazyRow { items { ContinueCard() } } }
// Ẩn hoàn toàn nếu items.isEmpty()
```

#### `ContinueCard.kt`
```kotlin
@Composable
fun ContinueCard(
    item: ContinueWatching,
    onClick: () -> Unit
)
// Renders: Card { Row { AsyncImage(poster) + Column { title, "Tập X", LinearProgressIndicator } } }
```

#### `HomeViewModel.kt`
```kotlin
class HomeViewModel : ViewModel() {
    // ─── STATES ───
    val movies      = mutableStateOf<Map<String, List<Movie>>>(emptyMap())  // key = row title
    val heroMovies  = mutableStateOf<List<Movie>>(emptyList())   // top 5 cho hero
    val heroIndex   = mutableIntStateOf(0)                        // auto rotate
    val continueList = mutableStateOf<List<ContinueWatching>>(emptyList())
    val isLoading   = mutableStateOf(true)
    val error       = mutableStateOf<String?>(null)

    // ─── ACTIONS ───
    fun loadHome()          // fetch all rows parallel (coroutines)
    fun refreshHero()       // rotate hero index
    fun retryLoad()         // retry khi error

    // ─── INIT ───
    init { loadHome() }
}
```

#### `DetailViewModel.kt`
```kotlin
class DetailViewModel(slug: String) : ViewModel() {
    val movieDetail    = mutableStateOf<MovieDetail?>(null)
    val selectedServer = mutableIntStateOf(0)             // index trong episodes list
    val episodes       = mutableStateOf<List<Episode>>(emptyList())
    val isLoading      = mutableStateOf(true)
    val error          = mutableStateOf<String?>(null)

    fun loadDetail(slug: String)
    fun selectServer(index: Int)
    fun getCurrentEpisodeIndex(): Int    // cho auto-scroll
}
```

#### `PlayerViewModel.kt`
```kotlin
class PlayerViewModel : ViewModel() {
    val playbackSpeed      = mutableFloatStateOf(1.0f)
    val currentServerIndex = mutableIntStateOf(0)
    val servers            = mutableStateOf<List<ServerData>>(emptyList())
    val showOverlay        = mutableStateOf(false)
    val isPlaying          = mutableStateOf(true)

    fun cycleSpeed()              // 1.0 → 1.25 → 1.5 → 2.0 → 0.75 → 1.0
    fun tryNextServer()           // failover khi stream lỗi
    fun saveProgress(slug, ep, position)  // lưu continue watching
}
```

### Component Tree — Mỗi screen

```
MainActivity
└── RaidenApp (Theme + NavigationDrawer)
    ├── Sidebar (NavigationDrawer content)
    │   └── SidebarItem × 8
    │
    └── NavHost
        ├── HomeScreen
        │   ├── HeroBanner (top 5, auto rotate)
        │   ├── ContinueWatchingRow (3-5 cards, hoặc ẩn)
        │   ├── MovieRow ("🔥 Phim Mới", movies)
        │   ├── MovieRow ("📺 Phim Bộ", movies)
        │   ├── MovieRow ("🎬 Phim Lẻ", movies)
        │   └── MovieRow ("🇰🇷 Phim Hàn", movies)
        │
        ├── DetailScreen
        │   ├── AsyncImage (poster)
        │   ├── Text (title, info)
        │   ├── ServerSelector (tabs)
        │   └── EpisodeGrid (auto scroll to current)
        │
        ├── PlayerScreen
        │   ├── ExoPlayer Surface
        │   └── PlayerOverlay (controls, speed, time)
        │
        ├── SearchScreen
        │   ├── TextField (search input)
        │   └── LazyVerticalGrid (results as MovieCards)
        │
        ├── FavoriteScreen
        │   └── LazyVerticalGrid (saved MovieCards)
        │
        └── HistoryScreen
            └── LazyColumn (watched items)
```

---

## 9. Build & Deploy

### Build
```bash
# Đường dẫn project mới
C:\Users\Admin\.gemini\antigravity\scratch\phimtv\

# Build APK
./gradlew assembleRelease

# Output
app/build/outputs/apk/release/app-release.apk
```

### Signing
- Sử dụng keystore từ project WebView cũ: `raidenphim.jks`
- Hoặc tạo keystore mới cho native app

### Deploy
- Copy APK vào USB → cài trên TV box
- Hoặc `adb install` qua mạng LAN

---

## 10. Timeline & Trạng thái thực tế

| Phase | Nội dung | Trạng thái | Files |
|---|---|---|---|
| **Phase 1** | Project setup + API layer + Theme | ✅ **DONE** | ApiClient, OPhimApi, KKPhimApi, Theme, Color, Type, Constants |
| **Phase 2** | HomeScreen + Hero + Cards + Sidebar | ✅ **DONE** | HomeScreen, HomeViewModel, HeroBanner, MovieCard, MovieRow, RaidenTVApp, Sidebar |
| **Phase 3** | DetailScreen + Episode grid | ✅ **DONE** | DetailScreen, DetailViewModel, Episode.kt |
| **Phase 4** | PlayerScreen (ExoPlayer + auto next) | ✅ **DONE** | PlayerScreen, PlayerViewModel |
| **Phase 5** | Search + Favorites + History | ✅ **DONE** | SearchScreen, SearchViewModel, FavoritesScreen, HistoryStore |
| **Phase 6** | Continue Watching multi-item + polish | 🟡 **PARTIAL** | ContinueStore ✅, ContinueWatchingRow ❌, Resume playback ❌ |
| **Phase 7** | Test trên TV box + fix bugs | 🟡 **IN PROGRESS** | Cần test real device |
| **Phase 8** | **Performance optimizations** | ✅ **DONE** | See PERFORMANCE.md — Coil cache, OkHttp cache, HomeCache, @Immutable, TvLazy→Lazy migration, GPU sidebar animation, image size limits, singleton repo |

**42 Kotlin files đã tạo** — App compile thành công, sẵn sàng test.

### Còn thiếu so với plan:
| Item | Plan | Thực tế | Priority |
|---|---|---|---|
| `ContinueWatchingRow.kt` | LazyRow 3-5 continue cards | ❌ Chưa tạo | 🔴 HIGH |
| `ContinueCard.kt` | Card với progress bar | ❌ Chưa tạo | 🔴 HIGH |
| `PlayerOverlay.kt` | Custom controls overlay | ❌ Chưa tạo (dùng built-in?) | 🟡 MEDIUM |
| `ErrorView.kt` | Global error component | ❌ Chưa tạo (inline error) | 🟢 LOW |
| `LoadingView.kt` | Centered loading | ❌ Chưa tạo (inline Text) | 🟢 LOW |
| `ServerSelector.kt` | Server tabs component | ✅ Inline trong DetailScreen | ✅ Done |
| `EpisodeGrid.kt` | Episode grid component | ✅ Inline trong DetailScreen | ✅ Done |
| Resume playback | Bấm continue → vào thẳng đúng tập + vị trí | ❌ ContinueStore có, UI chưa | 🔴 HIGH |
| Hero auto rotate | 5 phim rotate 8-10s | ❌ Dùng seeded daily random | 🟡 MEDIUM |
| Sneak peek / poster fade | Focus card → hero đổi poster | ❌ | 🟢 LOW |
| Hidden all countries toggle | Long-press 5 lần unlock | ❌ | 🟢 LOW |
| CategoryScreen | Grid + pagination + auto-load | ✅ DONE | ✅ |

---

## 11. Giữ song song

| Platform | Codebase | URL/Install |
|---|---|---|
| **Web (PC/Phone)** | `phimbox/` (HTML/CSS/JS) | `m.raidenhub.xyz` |
| **TV (Native)** | `phimtv/` (Kotlin/Compose) | APK cài trực tiếp |

Web không bị ảnh hưởng. TV app hoàn toàn độc lập.

---

## 12. Risks & Mitigation

| Risk | Impact | Mitigation |
|---|---|---|
| OPhim API thay đổi/die | 🔴 App không load phim | Auto fallback sang KKPhim |
| `link_m3u8` không hoạt động | 🟡 Player không play | **Auto switch server** (server 1 fail → server 2) |
| Stream chậm/buffering | 🟡 UX kém | Hiển thị loading + retry |
| Compose TV bugs trên box cũ | 🟡 UI glitch | Test sớm trên box thật (TV360 = S905X2, đủ mạnh) |
| Cả 2 API cùng die | 🔴 Hiếm | Global error UI: "Không thể tải dữ liệu — [Thử lại]" |

---

## 13. Bổ sung — Các tính năng nâng cấp

> Những ý hay từ quá trình review, sắp xếp theo priority.

### 13.1 🎬 Hero Auto Rotate (Phase 2)

Thay vì random 1 phim cố định:
- Rotate giữa **5 phim hot nhất** mỗi 8-10 giây
- Preload poster phim kế trước khi rotate
- **Pause** rotation khi user focus vào nút/row
- Fade transition giữa các poster (không dùng video preview)

```kotlin
LaunchedEffect(Unit) {
    while (true) {
        delay(8000)
        if (!userInteracting) {
            heroIndex = (heroIndex + 1) % 5
        }
    }
}
```

### 13.2 ⏯ Continue Watching — Multi Items (Phase 6)

Thay vì 1 card to:
- **TvLazyRow** chứa 3-5 items (gia đình xem nhiều phim song song)
- Mỗi card: poster nhỏ + tên + "Tập X" + progress bar
- Sắp xếp theo timestamp giảm dần (phim xem gần nhất lên đầu)

```
⏯ Đang Xem
[Cơn Say Mùa Xuân  ] [Running Man   ] [Tây Du Ký    ]
 Tập 12 ▓▓▓▓▓░░ 55%   Tập 789 ▓▓░░ 30%  Tập 45 ▓▓▓░░ 60%
```

### 13.3 � Episode Grid — Auto Scroll (Phase 3)

Khi vào DetailScreen, tự động scroll tới tập đang xem:

```kotlin
LaunchedEffect(currentEpisode) {
    gridState.scrollToItem(currentEpisodeIndex)
}
```

**Quan trọng cho phim 60-80 tập** — không cần cuộn tay từ đầu.

### 13.4 ⏩ Playback Speed Control (Phase 4)

ExoPlayer hỗ trợ sẵn:

```kotlin
// D-pad ↑ = tăng speed, ↓ = giảm speed
val speeds = listOf(0.75f, 1.0f, 1.25f, 1.5f, 2.0f)
player.setPlaybackSpeed(speeds[currentSpeedIndex])
```

Hiện tốc độ trên overlay: `1.25x ▶`

### 13.5 🔄 Auto Server Failover (Phase 4)

Khi stream fail:
```kotlin
player.addListener(object : Player.Listener {
    override fun onPlayerError(error: PlaybackException) {
        // Tự động thử server kế
        if (currentServerIndex < servers.size - 1) {
            currentServerIndex++
            playFromServer(currentServerIndex)
        } else {
            showError("Tất cả server đều lỗi")
        }
    }
})
```

Thứ tự thử:
1. `link_m3u8` server hiện tại
2. `link_m3u8` server kế
3. `link_embed` (WebView fallback)

### 13.6 ⚠️ Global Error UI (Phase 6)

Mọi screen đều có error state:

```
┌──────────────────────────┐
│                          │
│    😵 Không thể tải      │
│    dữ liệu phim         │
│                          │
│    [🔄 Thử lại]          │
│                          │
└──────────────────────────┘
```

### 13.7 🔍 API Dedup — Sửa logic (Phase 1)

GPT đúng: slug 2 nguồn có thể khác nhau cho cùng 1 phim.

```kotlin
// ❌ Sai: dedupe bằng slug
movies.distinctBy { it.slug }

// ✅ Đúng: dedupe bằng normalized name + year
movies.distinctBy { normalize(it.name) + "_" + it.year }

fun normalize(name: String): String {
    return name.lowercase().trim()
        .replace(Regex("[^a-z0-9\\p{L}]"), "")
}
```

### 13.8 👀 Sneak Peek → Poster Fade (Thay đổi)

~~Video preview auto play~~ → **BỎ HOÀN TOÀN**

Thay bằng:
- Focus card ≥ 1.5s → Hero Banner **fade chuyển sang poster** của phim đó
- Crossfade animation 300ms
- Nhẹ hơn video preview **100 lần**

### 13.9 🌏 Hidden "All Countries" Toggle (Phase 6)

Trong Settings (ẩn):
- Mặc định: chỉ hiện Trung/Mỹ/Hàn
- Long-press Settings 5 lần → unlock "Hiện tất cả quốc gia"
- Cho sếp xem anime Nhật khi cần 😏

### 13.10 ▶️ Resume Playback Auto — Killer Feature (Phase 4)

**Vợ mở app → bấm vào phim đang xem → VÀO THẲNG đúng tập + đúng vị trí.**

Không cần: chọn server → chọn tập → tua tới chỗ cũ.

```kotlin
// Khi bấm ContinueCard:
fun resumePlayback(item: ContinueWatching) {
    // item đã lưu: slug, episodeSlug, serverIndex, positionMs
    navigate(
        PlayerScreen(
            slug = item.slug,
            episode = item.episodeSlug,
            server = item.serverIndex,
            startAt = item.positionMs   // tua tới đúng chỗ
        )
    )
}

// ExoPlayer seek to position:
player.seekTo(startAtMs)
```

**Data lưu trong DataStore (mỗi phim):**
```kotlin
data class ContinueWatching(
    val slug: String,            // "con-say-mua-xuan"
    val name: String,            // "Cơn Say Mùa Xuân"
    val posterUrl: String,       // cho hiện card
    val episodeSlug: String,     // "tap-12"
    val episodeName: String,     // "Tập 12"
    val serverIndex: Int,        // server đã xem
    val positionMs: Long,        // vị trí tính bằng ms
    val durationMs: Long,        // tổng thời lượng (cho progress bar)
    val updatedAt: Long,         // timestamp để sort
    val source: String           // "ophim" hoặc "kkphim"
)
```

**Auto-save:** Mỗi 10 giây khi đang play → update positionMs vào DataStore.
**Khi kết thúc tập:** Xóa position, cập nhật episodeSlug sang tập kế.

---

## Appendix: Giữ lại từ WebView

Những logic tái sử dụng:
- ✅ Country filter list
- ✅ OPhim API endpoints + response format
- ✅ Image URL builder
- ✅ Favorites/History data structure
- ✅ Continue Watching logic
- ✅ Keystore signing config

## Appendix B: Hardware Target

| Box | Chip | RAM | GPU | Kết luận |
|---|---|---|---|---|
| **Viettel TV360** | S905X2 | 2GB | Mali-G31 MP2 | 🟢 Chạy Compose TV mượt, decode 4K |

> Box chạy Kodi 4K ầm ầm → Compose TV app = 0 vấn đề performance.
> Lag trước đây 100% do WebView, không phải hardware.

