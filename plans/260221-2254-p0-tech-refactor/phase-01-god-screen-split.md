# Phase 01: God Screen Split
Status: ⬜ Pending
Dependencies: None
Est: 1-2 sessions

## Objective
Tách 4 file lớn nhất thành cấu trúc chuẩn: Screen (UI) + ViewModel (logic) + Components (reusable).
Không thay đổi chức năng — chỉ restructure code.

## Thứ Tự Tách (Ít rủi ro → Nhiều rủi ro)

### 1. SearchScreen (538L → 3 files) — Dễ nhất, ít dependency

- [ ] 1.1 Tạo `search/SearchViewModel.kt`
  - Move `SearchViewModel` class (40 dòng) ra file riêng
  - Move `TRENDING_KEYWORDS`, `KEYWORD_MAP`, `normalizeKeyword()` ra `search/SearchConstants.kt`
  - Move `SearchHistoryManager` object ra `data/local/SearchHistoryManager.kt`
  - Move `SearchSort` enum ra file ViewModel
- [ ] 1.2 Tạo `search/SearchComponents.kt`
  - Extract: `SearchResultItem`, `EmptySearchState`, `TrendingChips`, `SortDropdown`
- [ ] 1.3 `SearchScreen.kt` chỉ còn layout + wiring (~150L)
- [ ] 1.4 Build + verify Search vẫn hoạt động

### 2. HomeScreen (798L → 4 files) — Medium

- [ ] 2.1 Tạo `home/HomeViewModel.kt`
  - Move `HomeViewModel` class + `HomeState` sealed class
- [ ] 2.2 Tạo `home/HomeComponents.kt`
  - Extract: `HeroCarousel` (170L — composable lớn nhất)
  - Extract: `MovieRowSection`
  - Extract: `ShimmerHomeScreen`
- [ ] 2.3 `HomeScreen.kt` chỉ còn Scaffold + PullToRefresh + sections wiring (~200L)
- [ ] 2.4 Build + verify Home vẫn hoạt động

### 3. DetailScreen (827L → 3 files) — Medium-Hard

- [ ] 3.1 Tạo `detail/DetailViewModel.kt`
  - Move `DetailViewModel` + `DetailState` + `rememberDominantColor()`
- [ ] 3.2 Tạo `detail/DetailComponents.kt`
  - Extract: `EpisodeGrid`, `MovieInfoSection`, `CastRow`, `Badge3()`
  - Extract: `RelatedMoviesRow`, `SubtitleSection`
- [ ] 3.3 `DetailScreen.kt` chỉ còn layout + state orchestration (~250L)
- [ ] 3.4 Build + verify Detail + Play flow hoạt động

### 4. PlayerScreen (1298L → 4 files) — Hardest, cần cẩn thận

- [ ] 4.1 Tạo `player/PlayerViewModel.kt` (đã có outline, ~80L)
  - Move `PlayerViewModel` bao gồm load(), loadAnime47(), fetchAnime47Stream()
- [ ] 4.2 Tạo `player/PlayerControls.kt`
  - Extract: OTT-style controls overlay (play/pause, seekbar, episode nav, speed, sub)
  - Extract: Intro/Outro skip button
  - Extract: Episode drawer
- [ ] 4.3 Tạo `player/PlayerGestures.kt`
  - Extract: Double-tap seek, brightness/volume swipe, long-press logic
- [ ] 4.4 `PlayerScreen.kt` chỉ còn ExoPlayer setup + state wiring (~200L)
- [ ] 4.5 Build + verify Player hoạt động (OPhim + Anime47 + subtitle + intro skip)

### 5. Bonus: AppNavigation Split

- [ ] 5.1 Tạo `navigation/GlassBottomNav.kt` — Extract bottom nav composable
- [ ] 5.2 `AppNavigation.kt` chỉ còn NavHost routes (~150L)

## Files to Create
```
ui/screens/search/SearchViewModel.kt
ui/screens/search/SearchConstants.kt
ui/screens/search/SearchComponents.kt
data/local/SearchHistoryManager.kt    (move from SearchScreen.kt)

ui/screens/home/HomeViewModel.kt
ui/screens/home/HomeComponents.kt

ui/screens/detail/DetailViewModel.kt
ui/screens/detail/DetailComponents.kt

ui/screens/player/PlayerViewModel.kt
ui/screens/player/PlayerControls.kt
ui/screens/player/PlayerGestures.kt

navigation/GlassBottomNav.kt
```

## Test Criteria
- [ ] Build thành công (no compilation errors)
- [ ] APK size không tăng đáng kể (cùng code, chỉ tách file)
- [ ] Home → Detail → Play flow hoạt động
- [ ] Search → Filter → Sort hoạt động
- [ ] Anime tab → AnimeDetail → Anime47 Player hoạt động
- [ ] Continue Watching → Resume hoạt động
- [ ] Intro/Outro skip hoạt động

## Risk & Mitigation
- **Risk:** Import sai → build fail
  - **Mitigation:** Tách từng file 1, build sau mỗi bước
- **Risk:** State leak khi move ViewModel
  - **Mitigation:** Verify StateFlow emissions vẫn đúng
- **Risk:** Compose preview break
  - **Mitigation:** Không ảnh hưởng vì app không dùng @Preview extensively

---
Next Phase: [phase-02-room-setup.md]
