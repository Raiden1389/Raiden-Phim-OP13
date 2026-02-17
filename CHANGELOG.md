# Raiden Phim — Changelog

## v1.7.0 — 2026-02-18

### 🍿 Tab English — Phim Mỹ (MỚI)
- Tích hợp **Consumet API** (self-hosted trên Vercel) + **FlixHQ** provider
- Hero banner full-width với gradient overlay
- **🔥 Trending** — Phim hot nhất
- **🎬 Recent Movies** — Phim lẻ mới
- **📺 Recent TV Shows** — Phim bộ mới
- Shimmer loading + Error state với retry
- **Detail Screen** — Cover image, info badges, genre, cast, description
- **Season Selector** — Filter chips cho multi-season shows
- **Episode List** — Tap để play
- **English Player** — ExoPlayer với M3U8 streaming

### 🌐 Multi-Source Vietnamese Subtitle (MỚI)
- **5 nguồn sub chạy song song:**
  - 🟢 FlixHQ (Consumet) — sub kèm stream sẵn
  - 🟢 SubDL — REST API, sub Việt tốt
  - 🟢 SubSource — REST API, kho sub lớn
  - 🟢 Subscene — HTML scrape, kho sub Việt lớn nhất
  - ⏳ OpenSubtitles — sẵn code, cần API key
- **Auto-select Vietnamese** khi có sub Việt
- **Subtitle Picker** — Bottom sheet chọn sub [🇻🇳 VI] [🇬🇧 EN]
- Sort: Vietnamese ưu tiên trước → English → others

### 🏗️ Architecture
- `ConsumetApi.kt` — Retrofit interface (trending, recent, search, info, stream)
- `ConsumetModels.kt` — Data models (Item, Detail, Episode, Stream, Source, Subtitle)
- `ConsumetRepository.kt` — Repository với parallel fetch
- `SubtitleApis.kt` — SubDL + OpenSubtitles + SubSource Retrofit interfaces
- `SubtitleModels.kt` — Unified SubtitleResult + provider-specific models
- `SubtitleRepository.kt` — Multi-source aggregator (5 providers)
- `EnglishScreen.kt` — Tab UI (484 lines)
- `EnglishDetailScreen.kt` — Detail + Season selector + Episode list
- `EnglishPlayerScreen.kt` — ExoPlayer + subtitle picker + landscape mode
- `Screen.kt` — 3 routes mới (English, EnglishDetail, EnglishPlayer)
- `AppNavigation.kt` — Tab 🍿 + routes wired up

---

## v1.6.1 — 2026-02-17 (Hotfix)

### 🐛 Bugfix
- Fix crash Anime tab: `expected BEGIN_ARRAY but was BEGIN_OBJECT`
  - `latest-episode-posts` và `upcoming` trả về `{"data": [...]}` wrapper, không phải array trực tiếp
  - Thêm `Anime47DataWrapper` class để unwrap response
- Fix ảnh poster Anime không hiển thị:
  - Trending dùng `posterUrl`, Latest dùng `image` — cập nhật `displayImage` fallback chain: `poster → posterUrl → image`
- Thêm field `year`, `rank` cho `Anime47Item`

---

## v1.6.0 — 2026-02-17

### 🎌 Tab Anime (MỚI)
- Tích hợp **Anime47 API** (`anime47.love/api/`)
- Hero banner full-width với backdrop image + gradient overlay
- **🔥 Trending** — Carousel anime đang hot
- **📺 Mới Cập Nhật** — Tập mới nhất
- **🗓️ Sắp Ra Mắt** — Upcoming anime
- **🏷️ Thể Loại** — 79 genre chips (scrollable)
- **⭐ Nổi Bật** — Featured cards với backdrop
- Shimmer skeleton loading + Error state với retry

### 🏗️ Architecture
- `Anime47Api.kt` — Retrofit interface (7 endpoints)
- `Anime47Models.kt` — Data classes (Item, Search, Genre, Detail, Episode)
- `AnimeRepository.kt` — Repository với parallel fetch
- `AnimeScreen.kt` — Full UI composable

### 🧭 Navigation
- Bottom nav 5 tabs: **Phim → 🎌 Anime → Tìm kiếm → Lịch sử → Cài đặt**
- Route `anime` + `anime_detail/{id}/{slug}`
- AnimeDetail reuse DetailScreen qua slug

### 🎮 Player Gestures
- **Brightness** — Vuốt dọc bên trái
- **Volume** — Vuốt dọc bên phải
- Visual indicators (icon + %)

### 🏠 Home & UI
- **Shimmer Loading** — Skeleton animation thay spinner
- **Year Badge** — Năm phát hành trên MovieCard

### ⚙️ Settings
- Xoá lịch sử tìm kiếm
- Hiện version app (v1.6.0)

## v1.5.0 — 2026-02-17

### 🎬 Detail Screen
- **#19 Cast & Director** — Hiện đạo diễn + diễn viên (top 8) trên Detail
- **#20 Continue from Last** — Nút "Tiếp tục Tập X" thay vì "Xem Phim" khi đã xem dở
- **#21 Episode Progress** — Progress bar dưới mỗi nút tập (partial watch)
- Genre chips dùng FlowRow (không bị cắt)

### 🔍 Search Screen
- **#9 Search History** — Lưu 15 từ khóa gần đây (persistent), xoá từng item hoặc tất cả
- **#11 Trending Keywords** — 10 từ khóa xu hướng khi chưa nhập
- **#12 Result Count** — Hiện số kết quả tìm được
- Clear button trên search bar

### 📜 Watch History Screen (#36)
- Tab **Lịch sử** mới trên bottom nav (4 tabs)
- Danh sách phim đang xem dở với thumbnail + progress bar
- Hiện thời gian còn lại (phút)
- Nút xoá từng item

### 🎮 Player Gestures
- **#23 Brightness** — Vuốt dọc bên trái để chỉnh độ sáng
- **#24 Volume** — Vuốt dọc bên phải để chỉnh âm lượng
- Visual indicators (icon + phần trăm) cho cả brightness lẫn volume

### 🏠 Home Screen
- **#2 Shimmer Loading** — Skeleton animation thay spinner loading
- Animated shimmer effect (pulsing alpha 0.3→0.8)

### ✨ Navigation & UX
- **#39 Animated Transitions** — Fade + slide transitions giữa các màn hình
- **#6 Year Badge** — Năm phát hành hiện trên card (góc phải khi chưa fav)

### ⚙️ Settings
- Xoá lịch sử tìm kiếm
- Hiện version app (v1.5.0)


## v1.4.0 — 2026-02-16

### ⚙️ Màn hình Cài đặt (Settings)
- Thêm tab **⚙️ Cài đặt** trên bottom navigation bar (3 tabs: Trang chủ, Tìm kiếm, Cài đặt)
- `SettingsManager.kt` — lưu preferences qua SharedPreferences, reactive StateFlow
- Settings persist vĩnh viễn — mở lại app vẫn giữ nguyên

### 🌍 Filter theo Quốc gia (persistent)
- Multi-select: 🇰🇷 Hàn Quốc, 🇨🇳 Trung Quốc, 🇺🇸 Âu Mỹ, 🇯🇵 Nhật Bản, 🇹🇭 Thái Lan, 🇮🇳 Ấn Độ, 🇹🇼 Đài Loan, 🇭🇰 Hồng Kông, 🇵🇭 Philippines, 🇬🇧 Anh
- Bỏ trống = hiện tất cả. Chọn quốc gia → Home chỉ hiện phim phù hợp
- Nút "Xoá bộ lọc" để reset nhanh

### 🎭 Filter theo Thể loại (persistent)
- Multi-select: 20 thể loại (Hành Động, Tình Cảm, Cổ Trang, Tâm Lý, Kinh Dị, Viễn Tưởng, Học Đường, v.v.)
- Kết hợp với filter quốc gia — cả 2 filter cùng áp dụng trên Home
- FlowRow chips UI với checkmark khi active

### 🏠 Home Screen
- Xoá filter chips inline trên Home (chuyển sang Settings để gọn hơn)
- Tất cả movie rows tự filter theo Settings, rows trống tự ẩn & dồn lại
- Category screen vẫn giữ filter chips riêng cho quick filter

## v1.3.0 — 2026-02-16

### 🐛 Bug Fixes
- **Infinite scroll không load thêm**: Fix `LaunchedEffect` key để re-trigger sau mỗi page load xong — TV Shows giờ load đủ 295 phim
- **KKPhim Home row chỉ 7 items**: Do API trả 10/page + filter Trailer. Ấn "Xem thêm" → Category với infinite scroll đầy đủ

## v1.2.0 — 2026-02-16

### 🌍 Country Filter
- **Home**: filter chips dưới hero carousel (🇰🇷🇨🇳🇯🇵🇺🇸🇹🇭🇹🇼🇭🇰)
- Chọn quốc gia → tất cả movie rows tự filter, rows trống tự ẩn
- **Category**: filter chips trên đầu grid (10 quốc gia)
- Client-side filter trên data đã fetch

### 🐛 Bug Fixes
- **KKPhim cover ảnh bị mất**: Fix `source` tag → image CDN resolve đúng `phimimg.com`
- **Hero banner 2 màu khi zoom**: Thêm `clipToBounds()` cho Ken Burns effect
- **TV Shows chỉ hiện 6-7 phim**: KKPhim API trả 10/page (có 295 total) — pagination OK, scroll load thêm

## v1.1.0 — 2026-02-16

### 📺 KKPhim Integration
- Thêm **KKPhim API** (`phimapi.com`) làm nguồn phim thứ 2
- Fallback tự động: Detail tìm OPhim trước → KKPhim nếu không có
- Category "TV Shows" mới, lấy dữ liệu từ KKPhim
- Row "📺 TV Shows" trên Home

### ❤️ Favorites System
- **FavoriteManager** — lưu bằng SharedPreferences, reactive StateFlow
- **Detail screen**: nút ❤️ toggle bên cạnh "Xem Phim"
- **MovieCard**: long press để thêm/xoá favorite + toast notification
- **Home**: row "❤️ Yêu thích" với nút ✕ xoá nhanh + long press
- Heart indicator trên card đã favorite

### ▶️ Watch History & Continue Watching
- **WatchHistoryManager** — lưu vị trí xem dở + danh sách tập đã xem
- Auto-save progress khi thoát player (>30s mới lưu)
- Auto-mark "đã xem" khi xem >90% tập
- **Home**: row "▶️ Xem tiếp" với progress bar trên thumbnail
- **Detail**: episode grid hiện ✓ + highlight xanh cho tập đã xem
- Long press "Xem tiếp" card để xoá

### 🎠 Hero Banner Carousel
- **HorizontalPager** — carousel 5 phim nổi bật, auto-scroll 5s
- Page indicator dots
- Nút **"Xem Ngay"** trên mỗi slide
- **Ken Burns Effect** — zoom + pan animation cinematic (1.0→1.15x, 10s)

### 🎮 Player UI Overhaul (Phone)
- **Seek bar** + time display (current / total)
- **Lock/Unlock** button → ẩn toàn bộ controls khi khoá
- **Prev/Next** episode buttons ở center
- **Playback speed** control trên top bar
- **Red glow** effect trên play/pause button
- Skip Intro repositioned above seek bar

## v1.0.0 — 2026-02-16

### 🚀 Full Native Kotlin/Compose Rewrite

Chuyển đổi hoàn toàn từ Java WebView wrapper sang **Kotlin + Jetpack Compose** native app.
Cùng tech stack với Raiden PhimTV (Android Box).

### Tech Stack
- **Kotlin** 2.2.20 + **Jetpack Compose** (BOM 2026.02.00)
- **AGP** 8.10.0, **compileSdk** 36, **targetSdk** 35, **minSdk** 24
- **ExoPlayer** (Media3) 1.9.2 — HLS native player
- **Coil 3** — Image loading with wsrv.nl proxy
- **Retrofit 2** + **OkHttp 4** — API client
- **DataStore** — Local preferences
- **R8** minification + resource shrinking → **2.87 MB** APK

### 📱 Screens
| Screen | Description |
|--------|-------------|
| **HomeScreen** | Hero banner + horizontal movie rows (Phim Mới, K-Drama, Phim Bộ, Phim Lẻ, Hoạt Hình) |
| **DetailScreen** | Movie backdrop, info grid, genres, server tabs, episode grid |
| **PlayerScreen** | Native ExoPlayer, landscape fullscreen, double-tap seek, speed cycling, skip intro, auto-next |
| **SearchScreen** | Debounced search (400ms), 3-column grid results |
| **CategoryScreen** | Full category listing with server-side pagination |

### 🎬 Player Features
- Landscape + immersive fullscreen
- Double-tap seek ±10s
- Speed cycling: 0.5x → 0.75x → 1.0x → 1.25x → 1.5x → 2.0x
- Skip Intro button (first 2 min, skips 85s)
- Auto-next episode with toast notification
- Keep screen on during playback

### 🎨 UI/UX
- Dark theme (Netflix-inspired color palette)
- Bottom navigation (Home / Search)
- Movie cards with quality, language, episode badges
- wsrv.nl image proxy (WebP, optimized sizes)
- Smooth Compose animations
- Edge-to-edge display
- Splash screen (Material 3 SplashScreen API)

### 🏗️ Architecture
- **MVVM** — ViewModel per screen
- **Repository pattern** — MovieRepository with retry
- **OPhim API** — Primary data source
- **Country filter** — Configurable via Constants
- Same data layer as PhimTV → easy to share code

### 📦 Build
- R8 minification enabled
- Resource shrinking enabled
- ProGuard rules for Retrofit + Gson models
- Signed release APK
- APK size: **2.87 MB**
