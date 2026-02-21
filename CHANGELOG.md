# Raiden Phim — Changelog

## v1.19.2 — 2026-02-21 (Phase 01: God Screen Split)

### 🔧 Refactoring — God Screen Split

Tách 4 màn hình "God Screen" monolithic thành các file nhỏ hơn, dễ bảo trì.

#### HomeScreen (798L → 3 files)
- **`HomeViewModel.kt`** — ViewModel + `HomeState` sealed class
- **`HomeComponents.kt`** — `HeroCarousel`, `MovieRowSection`, `ShimmerHomeScreen`, `MovieCard`
- **`HomeScreen.kt`** — UI composable only

#### SearchScreen (538L → 3 files)
- **`SearchViewModel.kt`** — ViewModel + `SearchSort`, `TRENDING_KEYWORDS`, `GENRE_CHIPS`
- **`SearchComponents.kt`** — `normalizeKeyword`, `SearchHistoryManager`
- **`SearchScreen.kt`** — UI composable only

#### DetailScreen (827L → 3 files)
- **`DetailViewModel.kt`** — ViewModel + `DetailState` sealed class
- **`DetailComponents.kt`** — `rememberDominantColor`, `Badge3`
- **`DetailScreen.kt`** — UI composable only

#### PlayerScreen (1298L → 2 files)
- **`PlayerViewModel.kt`** — ViewModel + `formatTime` utility
- **`PlayerScreen.kt`** — UI composable (OTT controls, sheets, dialogs)

### 🐛 Fixes
- **Deprecated Icons** — `Icons.Default.VolumeUp` → `Icons.AutoMirrored.Filled.VolumeUp`, `ViewList` → `AutoMirrored.Filled.ViewList`
- **Redundant `C.Badge`** — Removed duplicate extension in `DetailComponents.kt` (already exists in theme)

### 📦 Files Changed
- 4 files split → 13 files total (9 new, 4 rewritten)
- Zero functional changes — pure refactoring

## v1.19.1 — 2026-02-21 (Shimmer Loading & Screen Transitions & UI Polish)

### ✨ New Features

#### 💀 Shimmer Skeleton Loading
- **`ShimmerEffect.kt`** — Shared component: `rememberShimmerBrush()` gradient sweep animation, `ShimmerDetailScreen()` cho detail skeleton, `ShimmerGrid()` cho grid skeleton
- **DetailScreen** — Spinner → `ShimmerDetailScreen` (backdrop + title + badges + cast + episodes)
- **AnimeDetailScreen** — Spinner → `ShimmerDetailScreen`
- **CategoryScreen** — Spinner → `ShimmerGrid` (3 rows)
- **SearchScreen** — Spinner → `ShimmerGrid` (3 rows)
- **AnimeScreen (genre)** — Spinner → `ShimmerGrid` (2 rows)
- **AnimeScreen (donghua)** — Spinner → custom shimmer row (4 poster placeholders)

#### 🎬 Screen Transitions (Premium)
- **Forward** — `fadeIn + slideIn(1/5) + scaleIn(0.92→1.0)` với `FastOutSlowInEasing`
- **Exit** — `fadeOut + scaleOut(→0.95)` — co lại nhẹ khi rời
- **Pop back** — slide ngược + scale ngược, tự nhiên hơn
- **Before** — chỉ fade + slideIn đơn giản, thiếu depth
- **After** — hiệu ứng "zoom into content" premium

### 🔧 Technical
- **`ShimmerEffect.kt`** — New shared component: `rememberShimmerBrush()` (infinite gradient animation), `ShimmerDetailScreen()`, `ShimmerGrid(rows)`
- **`AppNavigation.kt`** — Refined transition specs, thêm `scaleIn/scaleOut`, `FastOutSlowInEasing`
- **6 screens updated** — Thay `CircularProgressIndicator` → Shimmer components
- **`tools/ram-watchdog/`** — VS Code extension monitor RAM usage, status bar live, kill process

#### 👆 Card Press Animation
- **MovieCard** — scaleDown `0.96f` khi press poster (điều chỉnh từ 0.94 → tinh tế hơn)

#### 🎭 Empty State Illustrations
- **`EmptyStateView.kt`** — Shared component: floating emoji animation + styled text
- **WatchHistoryScreen** — "🍿 Chưa xem phim nào" với emoji lơ lửng
- **SearchScreen** — "🔍 Không tìm thấy phim nào"
- **WatchlistScreen** — 3 empty states (Favorites, Playlists, Playlist detail)

#### � Pull-to-Refresh (Custom)
- **HomeScreen** — `PullToRefreshBox` Material3 với indicator màu Raiden (purple container + primary spinner)

### 🐛 Bug Fixes
- **Widget "Xem tiếp"** — Fix widget không cập nhật khi có phim mới. Thêm `notifyWidgetUpdate()` broadcast khi `saveContinue()` thay đổi data

### �📁 Files modified
| File | Changes |
|------|---------|
| `ShimmerEffect.kt` | **NEW** — shared shimmer components |
| `EmptyStateView.kt` | **NEW** — shared empty state component |
| `DetailScreen.kt` | Spinner → ShimmerDetailScreen |
| `AnimeDetailScreen.kt` | Spinner → ShimmerDetailScreen |
| `CategoryScreen.kt` | Spinner → ShimmerGrid |
| `SearchScreen.kt` | Spinner → ShimmerGrid + EmptyStateView |
| `AnimeScreen.kt` | Spinner → ShimmerGrid + shimmer row |
| `AppNavigation.kt` | Transition specs upgrade |
| `MovieCard.kt` | Press scale 0.94→0.96 |
| `WatchHistoryScreen.kt` | EmptyStateView |
| `WatchlistScreen.kt` | EmptyStateView (3 spots) |
| `HomeScreen.kt` | PullToRefreshBox |
| `WatchHistoryManager.kt` | Widget update notification |
| `build.gradle.kts` | Version bump 53→54 |

---

## v1.18.0 — 2026-02-21 (Anime Player & Genre Browse & UI Premium)

### ✨ New Features

#### 🎌 Anime Player — Hướng B (A47-1)
- **Phát anime trực tiếp từ Anime47 API** — Fix lỗi "ARRAY OBJECT" root cause: Player trước đây luôn gọi KKPhim API với anime slug → fail. Giờ Anime47 dùng flow riêng: `AnimeDetailScreen` → truyền `IntArray` episodeIds → `PlayerActivity` → `PlayerViewModel.loadAnime47()` → `Anime47Api.getEpisodeStream(id)` → lấy M3U8/stream URL → ExoPlayer
- **Pre-fetch tập kế** — Khi tập hiện tại đã load xong, tập tiếp theo được pre-fetch stream ngầm → chuyển tập mượt mà
- **Fallback `bestStreamUrl`** — Ưu tiên: `streamUrl` → HLS source (`.m3u8`) → MP4 source → embed link

#### 🏷️ Anime Genre Browse (A47-2)
- **Genre chip filter theo slug chính xác** — Tap thể loại trên tab Anime → fetch `GET /anime/list?genre={slug}` thay vì search keyword → kết quả chính xác theo đúng thể loại
- **Fallback tự động** — Nếu endpoint `/anime/list?genre=` chưa có → tự fallback về keyword search, không bị crash
- **Hiển thị 30 thể loại** — Tăng từ 20 → 30 genre chips hiển thị

#### 🎨 Detail Screen — UI Premium
- **A-6: Parallax Backdrop** — Poster cuộn parallax 0.5x speed, scale-up depth effect, fade-out khi scroll. Gradient overlay cinematic + glass back button
- **A-8: Dynamic Color** — Trích xuất dominant color từ poster qua AndroidX Palette → tint nút Play + badge chất lượng. Animated color transition mượt mà
- **B-3: Entrance Animation** — Fade (0→1) + scale (0.95→1.0) với `FastOutSlowInEasing` khi mở Detail — tạo hiệu ứng card → full-screen
- **🎭 Actor Photos (TMDB)** — Gọi TMDB Credits API lấy ảnh diễn viên thật thay emoji 👤. Match tên exact → fallback theo vị trí index. AsyncImage với circular crop

#### ▶️ Player — UI Premium
- **B-5: Gradient Scrims** — Top/bottom gradient overlay cho player controls, tạo cảm giác cinematic. JakartaFamily cho title, InterFamily cho time
- **B-7: Episode Bottom Sheet** — Nút "Tập X" → ModalBottomSheet hiện grid tất cả tập. Dark theme, highlight tập đang xem, dismiss khi chọn

#### ✨ UI/UX Polish — 10 Items
- **S-1: Typography** — Áp dụng JakartaFamily (headers/titles) + InterFamily (body/time) xuyên suốt app
- **S-4: Micro-interactions** — Bounce, pulse, scale animations cho các interactive elements
- **A-2: Glassmorphism Bottom Nav** — Bottom navigation bar với hiệu ứng glass blur
- **C-9: Search Empty State** — Giao diện empty state đẹp mắt khi chưa tìm kiếm
- **C-10: Settings Visual** — Cải thiện giao diện Settings screen

### 🐛 Bug Fix
- **Anime player crash** — Root cause: `PlayerViewModel.load(animeSlug)` → KKPhim API → slug không tồn tại → parse fail → "ARRAY OBJECT" error. Fixed bằng source routing riêng biệt

### 🔧 Technical
- **`Anime47Models.kt`** — Thêm `Anime47EpisodeStream` (với `bestStreamUrl` computed property), `Anime47Source`, `Anime47EpisodeStreamWrapper`
- **`Anime47Api.kt`** — Thêm `getEpisodeStream(id)`, `getAnimeByGenre(slug, page)`, `getAnimeByCategory(category, page)`
- **`AnimeRepository.kt`** — Thêm `getEpisodeStream(episodeId)`, `getAnimeByGenre(slug, name)` với double-fallback
- **`PlayerViewModel`** — Thêm `loadAnime47(episodeIds, epIdx, title)`, `fetchAnime47Stream(id)`. Episode placeholder format: `slug = "anime47::{id}"` để lazy-fetch
- **`PlayerScreen`** — Thêm params `source`, `episodeIds`, `animeTitle`. Gradient scrims, episode bottom sheet, typography polish. `LaunchedEffect` branch theo source
- **`PlayerActivity`** — Đọc thêm extras: `source`, `episodeIds` (IntArray), `animeTitle`
- **`DetailScreen`** — Parallax scroll (`graphicsLayer`), dynamic color (`Palette + animateColorAsState`), entrance animation (`Animatable`), TMDB cast photos
- **`AnimeDetailScreen`** — Thay `onPlay(slug, server, ep)` → `onPlayAnime47(episodeIds, epIdx, title)`. Build `episodeIds` IntArray từ `latestEpisodes.map { it.id }`
- **`AppNavigation`** — Thêm `startAnime47PlayerActivity()` helper, pass `source="kkphim"` cho KKPhim flow
- **Dependency** — Thêm `androidx.palette:palette-ktx:1.0.0`

---

## v1.17.0 — 2026-02-21 (Home Screen Enhancements)

### ✨ New Features

#### 🏠 Home Screen
- **🚫 Hero Carousel Filter (H-1)** — Long press bất kỳ slide trên Hero Carousel → Dropdown menu "🚫 Bỏ qua phim này" → ẩn khỏi carousel. Slides còn lại slide in liền mạch. `HeroFilterManager` lưu persistent qua SharedPreferences
- **🗂️ Reorder Home Sections (H-6)** — Settings → mục "Sắp xếp trang chủ": nút ↑↓ cho từng row (Phim Mới / K-Drama / Phim Bộ / Phim Lẻ / Hoạt Hình / TV Shows). Thứ tự được ghi nhớ ngay lập tức. Nút "↺ Khôi phục mặc định". `SectionOrderManager` lưu persistent
- **📺 TV Shows Home Row Fix (#50b)** — KKPhim API trả 10 item/trang → Home row chỉ hiện 10 phim. Fix: fetch page 1 + page 2 song song (async) → merge → `distinctBy { slug }` dedup → ~20 item trên row

#### ⚙️ Settings
- **🚫 Phim bị ẩn khỏi Carousel (H-1)** — Section mới trong Settings: đếm số phim đang bị ẩn + nút "Hiện lại tất cả" để reset `HeroFilterManager`
- **🗂️ Sắp xếp trang chủ (H-6)** — Section mới trong Settings: danh sách 6 section với nút ↑↓ + "↺ Khôi phục mặc định"

### 🐛 Bug Fix
- **Continue Watching typo** — Fix "phìm" → "phim" trong badge đếm số lượng

### 🔧 Technical
- **`HeroFilterManager.kt`** — Object singleton, `SharedPreferences` + `MutableStateFlow<Set<String>>`. API: `hide(slug)`, `isHidden(slug)`, `clearAll()`, `hiddenCount`
- **`SectionOrderManager.kt`** — Object singleton, `SharedPreferences` + `MutableStateFlow<List<String>>`. API: `moveUp(id)`, `moveDown(id)`, `reorder(list)`, `reset()`, `getSectionInfo(id)`
- Init cả 2 manager trong `App.kt` cùng với các manager khác
- **`HomeScreen.kt`** — `sectionOrder` collected ở composable scope, iterate để render rows theo đúng thứ tự user đã set. `HeroCarousel` filter bằng `hiddenSlugs` trước khi pass `movies`
- **`MovieRepository.kt`** — TV Shows: `async { kkApi.getTvShows(1) } + async { kkApi.getTvShows(2) }` song song

---

## v1.16.0 — 2026-02-20 (UX Polish — Home, Search & Detail)

### ✨ New Features

#### 🏠 Home Screen
- **⚡ Quick Play (H-7)** — Long-press bất kỳ movie card trên Home (tất cả rows) → haptic feedback + launch player ngay (server 0, episode 0), bỏ qua màn hình Detail
- **🕐 Relative Timestamps (H-8)** — Continue Watching cards hiển thị thời gian tương đối ("3m trước", "2h trước", "2 ngày") thay vì timestamp tuyệt đối
- **🎬 Continue Watching Redesign** — Cards cũ (dọc 2:3) → landscape 16:9 theo phong cách Netflix: play icon overlay, progress bar dưới đáy, chip tập + chip thời gian, click → resume trực tiếp không qua Detail

#### 🔍 Search Screen
- **🏷️ In-results Filter (S-1)** — Sau khi có kết quả: chip row **Tất cả / 📺 Phim bộ / 🎬 Phim lẻ** + chip năm lấy từ danh sách kết quả (tối đa 6 năm gần nhất). Dùng `episodeCurrent` heuristic vì `Movie.type` không có trong search response
- **🎥 Genre Quick Search (S-2)** — Row 10 chip thể loại nổi bật (🥊 Hành động / 💖 Tình cảm / 👻 Kinh dị / 🎠 Hoạt hình / 🚀 Viễn tưởng / 🏯 Cổ trang...) hiển thị khi chưa gõ gì → tap → tìm kiếm ngay
- **🔤 Smart Keyword Normalize (S-3)** — Map từ không dấu → có dấu: "han quoc" → "Hàn Quốc", "hanh dong" → "Hành động", "kinh di" → "Kinh dị", "hoat hinh" → "Hoạt hình"... Áp dụng cả khi gõ thông thường và voice search
- **📊 Sort Search Results (S-4)** — Dropdown button bên phải result count: **🕒 Mới nhất** (year desc) / **📋 Cũ nhất** (year asc) / **🔤 Tên A-Z** (alphabetical)

#### 🎬 Detail Screen
- **🍅 TMDB Rating (D-3)** — Fetch TMDB score song song với IMDb (reuse cùng `LaunchedEffect`, cùng OkHttp client). Hiển thị "🍅 TMDB 7.8/10" kế bên "⭐ IMDb 8.1/10" trong info chip row
- **📖 Expand/Collapse Plot Redesign (D-7)** — Thêm gradient fade overlay phía dưới khi plot bị thu gọn (đẹp hơn, không bị cắt cứng). `lineHeight = 20.sp` để dễ đọc hơn. Nút "Xem thêm ▼ / Thu gọn ▲"

### 🔧 Technical
- **`MovieCard.kt`** — Thêm optional `onLongClick: (() -> Unit)? = null` parameter. Nếu caller truyền vào → override default (favorite toggle). Nếu không → giữ hành vi cũ. Cho phép HomeScreen inject Quick Play logic
- **`SearchScreen.kt`** — Full rewrite để fix cấu trúc file lộn xộn (package statement bị đẩy giữa file do partial apply). Thêm `KEYWORD_MAP`, `GENRE_CHIPS`, `SearchSort` enum, `normalizeKeyword()` function
- **`HomeScreen.kt`** — Fix import `HapticFeedbackType` từ `foundation.hapticfeedback` → `ui.hapticfeedback` (đúng package). Tương tự fix type annotation trong `MovieRowSection` parameter
- **`DetailScreen.kt`** — Refactor IMDb fetch: bỏ nested `Dispatchers.IO.let { }` wrapper → dùng `withContext` trực tiếp. TMDB fetch dùng chung `OkHttpClient` instance

### 🐛 Bugfix
- **`SearchScreen` compile error** — `Movie.type` không tồn tại trong data class (chỉ có trong `MovieDetail`). Fix: dùng `episodeCurrent.contains("full")` heuristic thay thế
- **`HomeScreen` compile error** — `HapticFeedbackType` resolve fail vì import sai package (`foundation.hapticfeedback` không tồn tại). Fix: dùng `ui.hapticfeedback`

---

## v1.15.0 — 2026-02-20 (Discovery & Library Update)

### ✨ New Features

#### 🗂️ Categories & Discovery
- **📅 Year Filter (C-1)** — Chip row năm (Tất cả / 2025 / 2024 ... 2018) trong CategoryScreen, filter phía client theo `movie.year`. Kết hợp với country filter đã có → 2 chiều lọc độc lập
- **🗺️ Genre Hub (C-2)** — Screen thể loại mới: grid icon các thể loại (Hành động, Kinh dị, Tình cảm...) → tap → CategoryScreen lọc theo genre. Truy cập qua bottom nav tab Khám phá

#### 🔖 Watchlist & Playlists
- **🔖 Xem Sau — Watchlist (C-4)** — Bookmark phim để xem sau. Icon 🔖 trên Detail screen. Screen riêng hiển thị grid thumbnail + timestamp tương đối. Swipe / long-press để xóa. Lưu persistent qua `SharedPreferences`
- **📋 User Playlists (C-5)** — Tạo playlist thủ công (\"Xem Cuối Tuần\", \"List Gia Đình\"...). PlaylistListScreen: tạo mới, đổi tên, xóa. PlaylistDetailScreen: grid phim trong playlist, remove item. Nút \"+ Playlist\" từ Detail screen mở bottom sheet chọn playlist

#### 🎬 Detail Screen
- **🎞️ Phim liên quan (D-5)** — Row \"Có thể bạn thích\" cuối Detail: fetch phim cùng thể loại đầu tiên, hiển thị LazyRow horizontal 12 poster, tap → Detail phim đó
- **🎭 Cast Grid (D-6)** — Danh sách diễn viên từ `actor` field dạng horizontal scroll với avatar placeholder và tên
- **🔀 Episode Sort Toggle (D-8)** — Button đảo thứ tự tập 1→N / N→1 bằng `reversedOrder` state, ghi nhớ trong session

#### ⚙️ Settings
- **🎯 Default Playback Quality (SE-1)** — Chọn chất lượng mặc định khi khởi động player: Auto / 360p / 480p / 720p / 1080p. Lưu qua `SettingsManager`
- **💾 Export / Import Backup (SE-6)** — Xuất favorites + watch history + watchlist + playlists ra file JSON (SAF file picker). Import từ file → confirm dialog trước khi ghi đè. Tương thích chia sẻ giữa thiết bị

### 🔧 Technical
- `WatchlistManager` — Singleton quản lý watchlist: `add`, `remove`, `toggle`, `isInWatchlist`, `clearAll`. State `MutableStateFlow<List<WatchlistItem>>`
- `PlaylistManager` — Singleton quản lý playlists: `createPlaylist`, `deletePlaylist`, `renamePlaylist`, `addToPlaylist`, `removeFromPlaylist`, `isInPlaylist`. State `MutableStateFlow<List<Playlist>>`
- `SettingsManager` — Thêm `defaultQuality`/`setDefaultQuality`, `exportBackup`/`importBackup`
- `App.kt` — Init `WatchlistManager` + `PlaylistManager` trong `onCreate`
- `Screen.kt` — Thêm routes: `Watchlist`, `PlaylistList`, `PlaylistDetail`, `GenreHub`
- `AppNavigation.kt` — Wire up 4 route mới; `DetailScreen` nhận `onMovieClick` cho related movies
- `ConsumetSubtitle` model — Thêm missing data class vào `SubtitleModels.kt` (pre-existing compile error)

### 🐛 Bugfix
- **DetailScreen compile error** — `remember`/`LaunchedEffect` trong `LazyListScope` không phải `@Composable` context → hoist `relatedMovies` state lên trước `LazyColumn`
- **CategoryScreen bracket mismatch** — Year filter chips bị nest trong country `Row` → tách ra `Row` độc lập
- **WatchlistScreen duplicate** — Conflict `WatchlistScreens.kt` vs `WatchlistScreen.kt` → xóa file thừa
- **`ExperimentalFoundationApi`** — Remove `@OptIn` + `combinedClickable` (chưa import đúng) → dùng `clickable`

---

## v1.14.0.1 — 2026-02-20 (Hotfix & Cleanup)

### 🗑️ Removed
- **🍿 English Tab** — Xóa toàn bộ Consumet/FlixHQ integration (EnglishScreen, EnglishDetailScreen, EnglishPlayerActivity, ConsumetApi). Lý do: Consumet API không ổn định (Vercel cold start timeout, lag, lỗi M3U8 intermittent)
- Dọn dẹp: xóa `episodeId`, `filmName`, `isEnglish` khỏi `ContinueItem` + `saveEnglishProgress()` khỏi `WatchHistoryManager`

### 🔧 Fix
- **🔄 Infinite scroll Phim Lẻ** — CategoryScreen chỉ hiện 10 items rồi dừng. Root cause: tự tính `totalPages` từ `totalItems/perPage` trong khi API đã trả sẵn `totalPages`. Fix: dùng `pagination.totalPages` trực tiếp, fallback tính toán nếu null
- **🔄 Infinite scroll TV Shows (KKPhim)** — Tương tự, cùng fix

---

## v1.14.0 — 2026-02-19 (English Player Features)

### ✨ New Features
- **❤️ English Favorites** — Nút yêu thích trên EnglishDetailScreen, lưu riêng source `"english"` → hiện trong ❤️ row trên HomeScreen, tap navigate đúng EnglishDetail
- **⏩ English Continue Watching** — Tự động lưu tiến độ xem phim English khi thoát player (`saveEnglishProgress`). Hiện trong "Xem tiếp" row trên HomeScreen + WatchHistoryScreen. Tap → resume đúng tập đúng vị trí
- **🎞️ Quality Selector** — Nút chọn chất lượng video (AUTO/720P/1080P) trong English Player. Picker panel slide-up giống subtitle picker

### 🔧 Technical Changes
- `WatchHistoryManager`: Thêm `episodeId`, `filmName`, `isEnglish` helper vào `ContinueItem` + method `saveEnglishProgress()`
- `EnglishPlayerViewModel`: Store all quality sources, expose `allSources`/`selectedQuality` flows, `selectQuality()` method
- `HomeScreen`: Route `eng:` prefix slug → EnglishDetail. Unique key `slug_source` cho LazyRow tránh collision
- `WatchHistoryScreen`: Thêm `onContinueEnglish` callback, phân biệt English vs Viet items
- `AppNavigation`: Pass `filmName` qua onPlay, English callbacks cho WatchHistoryScreen
- `EnglishDetailScreen`: `onPlay` signature mở rộng thêm `filmName` param

## v1.13.0.4 — 2026-02-19 (Fullscreen Refactor — Separate Activity)

### 🏗️ Architecture
- **🎬 Separate PlayerActivity** — Tách Player ra Activity riêng (pattern Netflix/YouTube/NewPipe). Không share window/insets với MainActivity → không còn conflict với Scaffold/MaterialTheme
- **🍿 Separate EnglishPlayerActivity** — Tương tự cho English Player
- **🎨 Theme.RaidenPhim.Fullscreen** — Theme XML riêng cho player: black background, transparent bars, tắt contrast enforcement, cutout shortEdges

### 🐛 Bugfix
- **🖥️ Scrim navy/xám trên Android 15+** — Root cause: `isNavigationBarContrastEnforced` + `isStatusBarContrastEnforced` — Android 15+ tự inject scrim vào gesture area. Fix: tắt enforcement ở cả XML level lẫn runtime
- **🖥️ Inset conflict** — Player cùng Activity với navigation → insets bị share. Fix: separate Activity = separate window = no conflict
- **🔢 Version hiển thị sai** — Settings hardcode `v1.6.1`. Fix: dùng `BuildConfig.VERSION_NAME` + `BuildConfig.VERSION_CODE` tự động

### 🔧 Technical
- `PlayerActivity.kt` — Window fullscreen setup TRƯỚC `super.onCreate()`: cutout ALWAYS, contrast OFF, transparent bars, hide systemBars
- `EnglishPlayerActivity.kt` — Tương tự, orientation sensorLandscape
- `AndroidManifest.xml` — Register 2 Activity mới với `Theme.RaidenPhim.Fullscreen`
- `AppNavigation.kt` — Thay `navController.navigate` → `startActivity(Intent)` cho cả 2 player
- `PlayerScreen.kt` — Đơn giản hóa DisposableEffect: chỉ keep-screen-on + re-hide bars
- `EnglishPlayerScreen.kt` — Bỏ `FLAG_FULLSCREEN` (deprecated), đơn giản hóa
- `SettingsScreen.kt` — Dynamic version display via BuildConfig

---

## v1.13.0.2 — 2026-02-18 (Hotfix)

### 🐛 Bugfix
- **🖥️ Player bar màu lạ** — `themes.xml` bị đổi sang `NoActionBar.Fullscreen` gây bar trống trên Android 15+. Revert về `NoActionBar`
- **🖥️ Video không fill màn hình / camera cutout** — `Theme.kt` SideEffect set `statusBarColor` mỗi recompose, can thiệp PlayerScreen. Fix: bỏ SideEffect khỏi Theme.kt, dùng `LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS` cho Android 11+
- **📺 Detail screen hiện `? Tập tập`** — Filter bỏ giá trị `?` từ API

---

## v1.13.0.1 — 2026-02-18 (Fullscreen Fix + Data Loss Fix)

### 🐛 Bugfix
- **🖥️ Player không fullscreen** — `Theme.kt` SideEffect set `statusBarColor` liên tục → đè lên `hideSystemUI()` trong PlayerScreen. Fix: bỏ hardcode color trong Theme, để `themes.xml` lo
- **📦 Favorites + Lịch sử mất khi update APK** — ProGuard rule `-keep class data.local.**` chỉ giữ top-level class, **KHÔNG giữ inner class** (`FavoriteItem`, `ContinueItem`, `SeriesConfig`). Mỗi build R8 đổi tên inner class → Gson fail → `catch` trả `emptyList()` → data "biến mất". Fix: thêm `-keep class **$*` cho nested classes

### 🔧 Technical
- `Theme.kt` — Bỏ `window.statusBarColor` + `window.navigationBarColor` trong SideEffect (đã set trong `themes.xml`)
- `themes.xml` — Theme `android:Theme.Material.NoActionBar`
- `proguard-rules.pro` — Thêm `-keep class data.local.**$* { *; }` + `-keepclassmembers` cho inner classes

---

## v1.13.0 — 2026-02-18 (Per-Country Intro/Outro Defaults)

### ✨ New Features
- **🌏 Per-country defaults** — Mark intro/outro 1 phim Hàn → áp dụng cho tất cả phim Hàn
  - 3-level hierarchy: Per-series → Per-country → Country-based fallback
  - Sau khi mark → dialog hỏi "Áp dụng cho tất cả phim [country]?"
  - Country auto-detected từ API (Hàn Quốc, Trung Quốc, Nhật Bản, Mỹ...)
- **📋 Config status display** — Bottom sheet hiển thị config source:
  - "📌 Config riêng (series)" nếu có override
  - "⭐ Mặc định Hàn Quốc" nếu dùng country default
  - Hiển thị cả 2 nếu có override + country default
- **🗑 Separate reset** — Xoá config riêng (series) hoặc mặc định (country) độc lập

### 🔧 Technical
- `IntroOutroManager.kt` — Thêm `getEffectiveConfig()`, `promoteToCountryDefault()`, `getCountryDefault()`, country display names
- `PlayerViewModel` — Expose `country` as StateFlow cho PlayerScreen
- `PlayerScreen.kt` — Promote dialog (AlertDialog), hierarchy-aware config display, separate reset buttons

---



### ✨ New Features
- **⚙️ Player Settings (Gear icon)** — Icon bánh răng trên top bar player, mở bottom sheet cài đặt
- **📌 Mark Intro/Outro per-series** — Đánh dấu intro start/end + outro start cho từng series
  - Tap "Intro End" tại vị trí kết thúc intro → Skip Intro pill tự hiện khi player trong intro window
  - Tap "Outro Start" tại vị trí bắt đầu credits → Auto-next trigger tại đó
  - Intro Start optional (mặc định = đầu tập)
  - Mỗi field độc lập — mark cái nào dùng cái đó
- **🔄 Smart Auto-next** — Ưu tiên mark-based, fallback country-based nếu chưa mark
- **🗑 Reset marks** — Xoá toàn bộ config cho series trong settings sheet

### 🐛 Bugfix
- **📦 Data mất khi cập nhật APK** — ProGuard obfuscate `FavoriteItem` + `ContinueItem` → Gson fail. Fix: `-keep class data.local.** { *; }`
- **🖥️ Xoá `enableEdgeToEdge()`** — Root cause fullscreen conflict, app xem phim không cần

### 🔧 Technical
- `IntroOutroManager.kt` — Mới: manager lưu per-series intro/outro config (SharedPreferences + Gson)
- `PlayerScreen.kt` — Gear icon + ModalBottomSheet + mark-based skip/auto-next + derivedStateOf cho showSkipIntro
- `proguard-rules.pro` — Thêm `-keep class data.local.** { *; }` fix data loss
- `MainActivity.kt` — Xoá `enableEdgeToEdge()` + init `IntroOutroManager`
- `build.gradle.kts` — Thêm `-opt-in=ExperimentalMaterial3Api` compiler flag

---

## v1.11.0 — 2026-02-18 (OTT Premium Player UI)

### ✨ New Features
- **🎬 Premium Player Controls** — Redesign hoàn toàn player overlay theo mockup OTT (Netflix/VieON style)
  - **Red gradient play/pause button** — Nút tròn đỏ gradient lớn ở giữa
  - **Vertical brightness slider** — Thanh trượt dọc bên trái (icon ☀️, track trắng)
  - **Vertical volume slider** — Thanh trượt dọc bên phải (icon 🔊, track đỏ)
  - **Episode strip** — Dải tập phim cuộn ngang ở bottom, highlight tập đang xem
  - **Aspect ratio + CC buttons** — Bottom left, icon buttons đẹp
  - **Skip Intro pill** — Nút trắng bo tròn góc phải dưới
  - **Speed pill** — Surface bo tròn thay vì Text background
- **🔊 Audio Focus Handling** — Tự pause khi có cuộc gọi, resume khi xong
- **📱 Picture-in-Picture (PiP)** — Hỗ trợ PiP (Android 8.0+) cho cả Vietnamese & English player
- **🔄 Aspect Ratio Toggle** — Chuyển FIT/FILL mode

### 🎨 UI/UX
- **3-zone double-tap seek** — Tap trái (-10s), phải (+10s), giữa (play/pause) + haptic feedback
- **Seek animation overlay** — Hiện ⏪/⏩ + số giây khi seek
- **OTT-style controls layout** — Bố cục giống Netflix: top bar, center play, side sliders, bottom strip

### 🔧 Technical
- `PlayerScreen.kt` — Rewrite controls overlay (~300 dòng) theo mockup
- `EnglishPlayerScreen.kt` — Thêm PiP + audio focus + giữ nguyên subtitle/vietsub
- `AndroidManifest.xml` — `supportsPictureInPicture + configChanges`
- Nuclear fullscreen: `FLAG_FULLSCREEN` + `WindowInsetsController` compat + native

### 🐛 Bugfix
- **📦 Data mất khi cập nhật APK** — R8/ProGuard obfuscate `FavoriteItem` + `ContinueItem` (package `data.local`) mỗi build khác tên class → Gson deserialize fail → `catch` trả `emptyList()` → data "mất". Fix: thêm `-keep class xyz.raidenhub.phim.data.local.** { *; }` vào `proguard-rules.pro`
- **🖥️ Xoá `enableEdgeToEdge()`** — Root cause fullscreen conflict. `enableEdgeToEdge()` dùng `WindowInsetsController` mới đè lên deprecated `systemUiVisibility` flags → player không fullscreen được. App xem phim không cần edge-to-edge

---

## v1.10.1 — 2026-02-18 (English Player Fix + Nuclear Fullscreen)

### 🐛 Bugfix
- **Video không load (403)** — Đổi stream server từ UpCloud → VidCloud. UpCloud trả URL one-time-use bị expired ngay, VidCloud trả URL reusable
- **Player không fullscreen** — Thay deprecated `systemUiVisibility` flags bằng `WindowInsetsController` hiện đại, tương thích `enableEdgeToEdge()`
- **Race condition Referer** — Thay `delay(300)` cố định bằng retry loop 10×300ms chờ Referer header arrive

### 🔧 Technical
- `ConsumetApi.kt` — Thêm `server=vidcloud` default cho `getStreamLinks()`
- `PlayerScreen.kt` + `EnglishPlayerScreen.kt` — Fullscreen dùng `WindowCompat.getInsetsController()` + `hide(systemBars())`

---

## v1.9.2 — 2026-02-18 (Vietsub & Player Fix)

### ✨ New Features
- **🔍 Tìm & Tải Vietsub** — Nút mới trong subtitle picker, gọi SubDL API → download zip → extract .srt → load vào player
- `SubtitleDownloader` utility — Download, giải nén zip, cache subtitle local

### 🐛 Bugfix
- **Player video 00:00** — Fix player leak: không tạo lại ExoPlayer khi refererUrl thay đổi, dùng `HlsMediaSource.Factory` với OkHttpDataSource inline thay vì rebuild player
- **Fullscreen bị override** — Thêm `FLAG_FULLSCREEN` + `setDecorFitsSystemWindows(false)` để chắc chắn ẩn system bars khi `enableEdgeToEdge()` active
- **SubSource API 400** — Fix sai tên parameter: `query` → `q`, thêm `searchType=text` (bắt buộc)

### 🔧 Technical
- `SubtitleDownloader.kt` — Download + extract zip subtitles to cache dir
- `EnglishPlayerViewModel.searchVietsub()` — Search SubDL API, download top 3 vietsub, add to list
- `SubtitleRepository.searchSubDLDirect()` — Public API cho direct SubDL search
- `SubSourceApi.searchMovies()` — Fix `@Query("q")` + `@Query("searchType")`

---

## v1.9.1 — 2026-02-18 (Bugfix & Performance)

### 🐛 Bugfix
- **Player Fullscreen** — Thay thế API deprecated `SYSTEM_UI_FLAG_*` bằng `WindowInsetsControllerCompat` cho cả Vietnamese và English player
- **English Video Loading** — Fix lỗi 403 khi load video: parse `Referer` header từ Consumet API response, dùng `OkHttpDataSource` gửi Referer + Origin cho ExoPlayer
- **Season Navigation** — Click vào chip "Phần X" trên DetailScreen giờ navigate đúng đến phần đó (trước đó handler rỗng)

### ⚡ Performance
- **Consumet API Optimization** — Trim 11 providers thừa (anime, manga, books, comics...), chỉ giữ FlixHQ → giảm bundle size, cold start nhanh hơn
- **Region Singapore** — Deploy Consumet API tại `sin1` (Singapore) thay vì US East → giảm ~200ms latency
- **Cron Keep-Warm** — Ping API mỗi 5 phút → gần như không còn cold start

### 🔧 Technical
- Thêm dependency `media3-datasource-okhttp:1.9.2`
- `ConsumetStreamResponse` thêm field `headers: Map<String, String>`
- `EnglishPlayerViewModel` thêm `refererUrl` state
- `DetailScreen` thêm callback `onSeasonClick`

---

## v1.9.0 — 2026-02-18 (Anime Enhancements)

### ✨ New Features
- **🐉 Donghua Section** — Mục Hoạt Hình Trung Quốc trên tab Anime
  - Tìm donghua thông qua search API với danh sách từ khóa curated (già thiên, đấu phá, tiên nghịch, vũ động càn khôn...)
  - Tự động dedup theo anime ID, giới hạn 15 kết quả
  - Hiển thị dạng LazyRow ngang giữa Trending và Mới Cập Nhật
- **🔍 Genre Search** — Bấm genre chip → search API trả kết quả anime theo thể loại
  - Loading indicator khi đang fetch
  - Hiển thị grid 3 cột kết quả
  - Message khi không tìm được kết quả
- **Xem thêm ›** — Section headers có nút "Xem thêm" cho Trending, Mới Cập Nhật, Sắp Chiếu

### 🔧 Technical
- `AnimeRepository.getDonghua()` — search-based donghua fetch với curated keywords
- `DonghuaSection` composable — self-contained với LaunchedEffect + loading state

---

## v1.8.0 — 2026-02-18 (Search & Anime)

### ✨ New Features
- **#10 Voice Search 🎤** — Nút micro trên search bar, nhận diện giọng nói tiếng Việt (`vi-VN`)
- **#13 Search Autocomplete 🔍** — Gõ ≥ 2 ký tự → hiện dropdown gợi ý từ lịch sử + trending
- **#17 IMDb Rating ⭐** — Hiện `⭐ IMDb X.X/10` trên trang chi tiết phim (via OMDB API)
- **#40 Season Grouping 📺** — Tự phát hiện phim nhiều phần (Phần X/Season X), hiện horizontal scroll chọn phần
- **#45 Anime Detail Screen 🎌** — Trang chi tiết riêng cho Anime từ Anime47 API
  - Backdrop + badges (quality/type/rating/status)
  - Genre chips, description expandable
  - Episode list với play buttons
  - API wrapper fix cho `/anime/info/{id}` response format

---

## v1.7.1 — 2026-02-18 (Hotfix)

### 🐛 Bugfix
- Fix crash English tab: `Expected BEGIN_OBJECT but was BEGIN_ARRAY`
  - `/recent-movies` và `/recent-shows` trả raw array `[...]`, không phải `{"results": [...]}`
  - Đổi return type sang `List<ConsumetItem>` + bỏ `.results` accessor

### ✨ Enhancement
- **Genre Chips hoạt động** — bấm thể loại Anime → filter hiển thị anime matching genre
  - Selected chip highlight màu accent
  - Grid 2 cột hiển thị kết quả filter
  - Toggle on/off khi bấm lại

---



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
