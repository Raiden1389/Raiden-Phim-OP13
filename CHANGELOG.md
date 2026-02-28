# Raiden Phim - Changelog

> User-facing changes only. Full details: [CHANGELOG.full.md](CHANGELOG.full.md)

---

## v1.23.0 — 2026-03-01 (Fshare Search + DetailScreen Refactor)

**Top Impact**: Fshare search integration • F badge on search results • DetailScreen 847→220 LOC refactor • Wrap-up squash workflow

### Added
- Fshare search — `SearchViewModel.search()` runs ophim + `FshareAggregator.search()` in parallel via `async/await`
- `CineMovie.toMovie()` extension — converts Fshare movie data to unified `Movie` model (source="fshare")
- Green "F" badge on `MovieCard` for Fshare-sourced results (`movie.source == "fshare"`)
- Result merging — ophim results first, then Fshare results, dedup by normalized title (`seen` set)

### Changed
- `DetailScreen.kt` — 847→220 LOC orchestrator, extracted 7 component files:
- `DetailAnimations.kt` — `AnimatedIntCounter` + `AnimatedFloatCounter` (45 LOC)
- `DetailBackdrop.kt` — Parallax backdrop + gradient + back button + title overlay (95 LOC)
- `DetailActionRow.kt` — Play/Continue + Favorite + Watchlist + Playlist buttons (95 LOC)
- `DetailInfoSection.kt` — Ratings, genres, cast, director, description (180 LOC)
- `DetailEpisodeGrid.kt` — Server tabs + episode grid with progress bars (130 LOC)
- `DetailSeasonRow.kt` — Season grouping chips row (70 LOC)
- `DetailRelatedRow.kt` — Related movies horizontal row (70 LOC)

---

## v1.22.3 — 2026-02-28 (Fshare Subfolder Browsing)

**Top Impact**: Subfolder browsing file-browser UX • Folder nav stack with Back • Unique key crash fix

### Added
- Subfolder browsing — detail screen shows 📁 subfolder entries as clickable items, click to drill in, Back to go up (file browser UX)
- Folder navigation stack — `folderStack` + `folderDepth` (`mutableIntStateOf` for Compose reactivity) + `BackHandler` intercepts Back within subfolders
- `folderEntry()` helper — creates Episode with 📁 prefix + FOLDER_SLUG for subfolder items

### Changed
- `FshareDetailViewModel.expandFolder(folderUrl)` — now accepts optional URL param for subfolder navigation
- `FshareEpisodePanel.onFolderClick` — `() -> Unit` → `(folderUrl: String) -> Unit`
- `tryListFolder()` — shows subfolders when folder contains only subfolders (not recursive flatten)

### Fixed
- `IllegalArgumentException: Key "fshare-folder" was already used` — `LazyVerticalGrid`/`LazyColumn` key duplicated when multiple subfolders. Fix: key = `"${slug}_$index"`
- Back not exiting detail — `folderStack` was `mutableListOf` (not Compose state) → `canNavigateBack` getter didn't trigger recomposition → `BackHandler` stuck enabled. Fix: `mutableIntStateOf(folderDepth)`

---

## v1.22.1 — 2026-02-27 (FFmpeg Audio + Player Polish)

**Top Impact**: FFmpeg audio decoder cho MKV/EAC3 • Episode name cleanup • Subtitle dialog redesign

### Added
- `nextlib-media3ext` integration — software decode AC3, EAC3, DTS, TrueHD, FLAC, Vorbis, Opus
- `NextRenderersFactory` thay `DefaultRenderersFactory` + `EXTENSION_RENDERER_MODE_PREFER`
- Media3 1.9.2 → 1.9.1 (match nextlib dependency)
- Native libs: `libavcodec.so`, `libmedia3ext.so`, `libswresample.so`, `libswscale.so` (arm64/armeabi/x86/x86_64)

### Changed
- `PlayerSubtitleDialog` — AlertDialog → glassmorphism overlay (match Audio `TrackSelectionDialog` style)
- Tách `PlayerOnlineSubtitles.kt` — online search logic riêng biệt
- Shared components: `SubtitleRow`, `SectionHeader` (internal)

### Fixed
- Episode name hiện "Tập 5 . 1080 3,3 GB" → "Tập 5" (strip quality/size suffix)
- Episode list trigger hiện "Tập Tập 5" → "Tập 5" (fix double prefix)
- Auto-play fix — bỏ duplicate `AudioFocusEffect` (ExoPlayer handles via `setAudioAttributes` internally)
- Tắt R8 minify + shrink resources → build nhanh hơn ~3-4x

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

> Older versions: see [CHANGELOG.full.md](CHANGELOG.full.md)
