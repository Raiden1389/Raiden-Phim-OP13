# Raiden Phim - Changelog

> User-facing changes only. Full details: [CHANGELOG.full.md](CHANGELOG.full.md)

---

## v1.22.1 — 2026-02-27 (FFmpeg Audio + Player Polish)

### Added
- FFmpeg audio decoder — hỗ trợ AC3, EAC3, DTS, TrueHD qua software decode (MKV files có tiếng)

### Changed
- Subtitle dialog redesign — glassmorphism style giống Audio dialog
- Tắt R8 minify — build nhanh hơn ~3-4x

### Fixed
- Episode name "Tập 5 . 1080 3,3 GB" → "Tập 5" (bỏ quality/size)
- Episode button hiện "Tập Tập 5" → "Tập 5" (bỏ duplicate prefix)
- Auto-play khi mở tập — bỏ conflict audio focus

---

## v1.22.0 — 2026-02-27 (Fshare HD + Player Refactor)

### Changed
- **Fshare Phim Lẻ & Phim Bộ rows** trên HomeScreen — load từ ThuVienCine, poster + quality badge + năm
- **FshareCategoryScreen** — grid listing với infinite scroll pagination khi bấm "Xem thêm →"
- **FshareDetailScreen** — poster scraping, episode listing cho folder, playback integration
- **Ẩn/hiện rows trong Settings** — toggle 👁/🚫 cho mỗi section
- **Fshare Login fix** — đổi User-Agent sang `kodivietmediaf-K58W6U`, bỏ Content-Type override gây 405
- **ThuVienCine URL detection fix** — chỉ coi URL là Fshare direct khi chứa `fshare.vn`, tránh gửi nhầm ThuVienCine URL tới Fshare API → fix 404 episode listing
- **PlayerScreen.kt**: 1540 → ~210 LOC (thin wiring shell)
- Tách thành **18 files** single-responsibility:

### Fixed
- **Listener leak fix** — `LaunchedEffect(player)` STATE_ENDED listener → `DisposableEffect` với `onDispose { removeListener }`
- **Unsafe activity cast fix** — `context as Activity` → `context as? Activity ?: return` (safe-cast, tránh crash preview/wrapper)
- **Duplicate audio focus fix** — gộp 2 audio focus request thành 1 luồng duy nhất trong `AudioFocusEffect`

---

## v1.20.8 — 2026-02-23 (Player UX + Episode Badge)

### Changed
- **Fshare Phim Lẻ & Phim Bộ rows** trên HomeScreen — load từ ThuVienCine, poster + quality badge + năm
- **FshareCategoryScreen** — grid listing với infinite scroll pagination khi bấm "Xem thêm →"
- **Ẩn/hiện rows trong Settings** — toggle 👁/🚫 cho mỗi section, hidden rows dimmed + không render trên Home
- **SectionOrderManager** — thêm `fshare_movies`, `fshare_series`, `visibleOrder` flow, `toggleVisibility()`
- **Fshare Login fix** — đổi User-Agent sang `kodivietmediaf-K58W6U`, bỏ Content-Type override gây 405
- **ThuVienCine URL detection fix** — chỉ coi URL là Fshare direct khi chứa `fshare.vn`, tránh gửi nhầm ThuVienCine URL tới Fshare API
- Swipe ngang trên màn hình player = seek liên tục
- 1px drag ≈ 200ms, full swipe ≈ ±3.6 phút

### Fixed
- **Root cause:** `markWatched()` không bao giờ được gọi cho SuperStream → episode không tick ✓ dù đã xem xong
- **Fix:** `PlayerScreen.onDispose` — nếu `source == "superstream"` và progress ≥ 70% → `WatchHistoryManager.markWatched("ss_tv_{tmdbId}", epIdx)`
- Hợp nhất stack `pointerInput + combinedClickable` thành 1 `detectTapGestures` — eliminating double-fire issue
- Single tap chỉ navigate, double tap → info popup, long press → context menu — hoạt động chính xác
- **Root cause:** `AnimatedVisibility(expandVertically)` trên label → layout shift → toàn bộ Column phình ra → trông như icon zoom
- **Fix:** Xóa `AnimatedVisibility`, thay bằng 1 `Text` duy nhất với `animateColorAsState(tween 250ms)` — label luôn chiếm space, chỉ đổi màu Primary ↔ TextSecondary
- Scale icon cố định `1f` — zero zoom effect
- **Root cause:** TMDB `/credits` API mặc định trả tên ngôn ngữ gốc (`김선호`, `金宣虎`)

### Perf
- `PendingDetailState` singleton: MovieCard set `thumbUrl + title` trước khi navigate
- `ShimmerDetailScreen` hiện ảnh poster thật + title ngay từ Coil memory cache (0ms)
- API data load xong → replace shimmer → transition smooth
- **Bỏ wsrv.nl proxy** — direct CDN URL thay vì route qua server EU
- Phone VN: CDN OPhim/KKPhim (Cloudflare Asia) đã đủ nhanh, không cần extra hop
- **Bonus:** card/shimmer/detail cùng 1 URL → Coil cache hit 100%, ảnh không fetch lại khi mở detail
- **Force API cache interceptor** — Override server `no-cache/no-store` headers → cache API response 5 phút
- **Coil cache tăng:** memory 50→80MB, disk 200→400MB

---

## v1.20.6 — 2026-02-23 (Visual Polish + Scope Lock + UX Fixes)

### Changed
- Rating IMDb/TMDB: count-up animation từ 0.0 → giá trị thực (`AnimatedFloatCounter`, 1s)
- Năm phát hành: count-up `AnimatedIntCounter` (0.9s), `FastOutSlowInEasing`
- Premium feel mỗi lần mở Detail screen
- 20 thể loại có gradient riêng biệt: Hành Động (đỏ cam), Kinh Dị (tím đen), Tình Cảm (hồng), Tâm Lý (xanh dương)...
- `GenreColors.kt` — util map `slug → GenrePalette(start, end, label)`
- GenreHub cards: gradient background thay vì flat `C.Surface`, text trắng
- Dễ reuse cho CategoryScreen header, SearchScreen chips sau
- 4 kiểu bo góc: **Bo mềm** (16dp iOS) / **Bo nhẹ** (8dp Android default) / **Vuông** (2dp cinematic) / **Nghệ** (asymmetric 0/12/12/0)

### Fixed
- **Root cause:** Race condition giữa 2 `LaunchedEffect` — `currentPage` update trước khi animation xong → `animateScrollToPage()` kéo pager ngược chiều
- **Fix:** Thay `LaunchedEffect(currentPage, isScrollInProgress)` bằng **`LaunchedEffect(settledPage)`** — chỉ fire sau khi animation hoàn toàn xong
- Đọc `currentNavRoute` trực tiếp từ `navController.currentBackStackEntry` thay vì stale closure
- Thêm guard `!isScrollInProgress` trong Nav→Pager sync để tránh fight khi user đang swipe
- **Root cause:** Compose known bug với `combinedClickable(onClick + onDoubleClick)` — đôi khi fire cả 2 cùng lúc → navigate to detail AND show popup đồng thời
- **Fix:** Tách double-tap thành **`pointerInput { detectTapGestures }`** riêng với timestamp tracking (threshold 300ms)
- `combinedClickable` chỉ còn `onClick` + `onLongClick` — không có conflict
- Jank eliminated: 3 animations (press scale + popup + navigate) không còn chạy song song

---

## v1.20.5 — 2026-02-22 (Micro-UX Batch: Swipe, Popup, Stats, Menu)

### Changed
- **HorizontalPager 5 tab** bọc toàn bộ main screens (Home, English, Search, History, Settings)
- Sync **2 chiều**: swipe → `NavController.navigate()`, tap tab icon → `pagerState.animateScrollToPage()`
- `beyondViewportPageCount = 1` để preload tab kế tiếp, không lag khi swipe
- Non-tab routes (Detail, Player, Category...) vẫn dùng `NavHost` bình thường
- **Double-click bất kỳ MovieCard** → Dialog popup thay vì phải vào Detail screen
- Popup: Poster 16:9 với gradient overlay, badges row (quality + lang + year), tên phim
- Info: country, `episodeCurrent`, action buttons ▶️ Xem / ❤️ Favorite / 🔖 Watchlist
- Dismiss bằng click ngoài popup

### Fixed
- **MovieCard** — xóa `onLongClick` param dư, replace bằng internal `showContextMenu` state
- **HomeComponents.kt** — `originName` không tồn tại trên `Movie` → `year + country.first().name`

---

## v1.20.2 — 2026-02-22 (Room DB Migration — Phase 3 Fix)

### Breaking/Migration
- **`PlayerScreen`** — `saveProgress()` → `updateContinue()` với params đúng (API rename)
- **`SearchViewModel`** — `history.value` (invalid trên Room Flow) → `_cachedHistory` pattern (collect trong `init {}`, cache cho sync access)
- **`SearchViewModel`** — `.distinct()` (Flow operator) → `.distinctBy { }` (List operator)
- **`SearchScreen`** — Remove `LaunchedEffect { init(context) }` — SearchHistoryManager đã init qua App.kt
- **`SettingsManager`** — Xoá `FavoriteManager/WatchHistoryManager/WatchlistManager/PlaylistManager.init(context)` cũ trong `restoreFromJson` (Room managers không reinit bằng Context)
- **`SuperStreamDetailScreen`** — `watchedEps.collectAsState()` (field không tồn tại) → `getWatchedEpisodes(slug).collectAsState(initial = emptyList())`
- **`HeroFilterManager.hiddenCount`** — Dùng như `Int` → `Flow<Int>.collectAsState(initial = 0)`

---

> Older versions: see [CHANGELOG.full.md](CHANGELOG.full.md)
