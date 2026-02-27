# 🚀 Fshare Port Plan — PhimBox-APK v1.21.0

> **Strategy:** New standalone components + shared player/continue watching
> **Principle:** Mọi Fshare/Community UI = file mới 100%. Player + Continue = extend existing.
> **Est. Total:** ~5 hours

---

## 🎯 Design Principle

```
SHARED (extend existing)           NEW (standalone files)
═══════════════════════            ═══════════════════════
PlayerScreen.kt     → +fshare     FshareApi.kt
PlayerViewModel.kt  → +loadFshare FshareModels.kt
PlayerActivity.kt   → +source     ThuVienCineModels.kt
WatchHistoryManager → +fshare tag FshareRepository.kt
Constants.kt        → +URLs       ThuVienCineRepository.kt
ApiClient.kt        → +client     CommunityRepository.kt
Screen.kt           → +routes     FshareDetailScreen.kt    ← NEW
AppNavigation.kt    → +composable FshareDetailViewModel.kt ← NEW
build.gradle.kts    → +buildconf  FshareActionButtons.kt   ← NEW
                                   FshareEpisodePanel.kt    ← NEW
                                   CommunityScreen.kt       ← NEW
                                   CommunityViewModel.kt    ← NEW
                                   FsharePlayerLoader.kt    ← NEW
```

**Xóa được toàn bộ block NEW → app cũ chạy y nguyên.**

---

## Phase 1: COPY — Data Layer (⭐ ~30 min)

### Task 1.1: Constants + BuildConfig
**Modify** `util/Constants.kt` — thêm:
```kotlin
const val FSHARE_BASE_URL  = "https://api.fshare.vn/api/"
const val THUVIENCINE_URL  = "https://thuviencine.com"
val FSHARE_EMAIL: String get() = BuildConfig.FSHARE_EMAIL
val FSHARE_PASSWORD: String get() = BuildConfig.FSHARE_PASSWORD
val FSHARE_APP_KEY: String get() = BuildConfig.FSHARE_APP_KEY
```

**Modify** `app/build.gradle.kts` — thêm buildConfigField cho Fshare credentials
**Modify** `local.properties` — thêm FSHARE_EMAIL, FSHARE_PASSWORD, FSHARE_APP_KEY

### Task 1.2: Copy API Files (3 files mới)
Copy từ PhimTV → PhimBox, chỉ đổi package `phimtv` → `phim`:

| New File | Source PhimTV | Size |
|---|---|---|
| `data/api/FshareApi.kt` | Copy | 1.8KB |
| `data/api/models/FshareModels.kt` | Copy | 4.8KB |
| `data/api/models/ThuVienCineModels.kt` | Copy | 3.2KB |

Verify: PhimBox `Episode` model (Models.kt) compatible với PhimTV `Episode`

### Task 1.3: ApiClient — Fshare Retrofit
**Modify** `data/api/ApiClient.kt` — thêm:
```kotlin
val fshareApi: FshareApi by lazy {
    Retrofit.Builder()
        .baseUrl(Constants.FSHARE_BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(FshareApi::class.java)
}
```

### Task 1.4: Copy Repositories (3 files mới)
Copy từ PhimTV → PhimBox, đổi package:

| New File | Source PhimTV | Size | Notes |
|---|---|---|---|
| `data/repository/FshareRepository.kt` | Copy | 9.5KB | Verify EncryptedSharedPrefs |
| `data/repository/ThuVienCineRepository.kt` | Copy | 18.8KB | Package rename only |
| `data/repository/CommunityRepository.kt` | Copy | 8.8KB | 12 sources hardcoded |

### Task 1.5: Continue Watching (extend existing)
**Modify** `data/local/WatchHistoryManager.kt`:
- Thêm `source` field support: `"fshare"` tag
- Thêm `getFshareItems()` filtered flow
- Fshare slug prefix: `fs_` (pattern giống SuperStream `ss_`)

> **Build check sau Phase 1** — đảm bảo compile OK trước khi qua Phase 2.

---

## Phase 2: ADAPT — UI Screens (🔧 ~2.5 hours)

> Tất cả file dưới đây = **FILE MỚI 100%**, tách biệt hoàn toàn.

### Task 2.1: CommunityViewModel (file mới)
**New** `ui/screens/community/CommunityViewModel.kt`
- Copy từ PhimTV, đổi package
- Pure ViewModel logic — không UI dependency
- Navigation stack (drill-down levels) giữ nguyên

### Task 2.2: CommunityScreen (file mới, adapt TV→Mobile)
**New** `ui/screens/community/CommunityScreen.kt`
- Copy từ PhimTV, adapt:

| Bỏ (TV) | Thay bằng (Mobile) |
|---|---|
| `focusable()`, `onKeyEvent`, `onPreviewKeyEvent` | `clickable()`, `onClick` |
| `androidx.tv.material3.*` | `androidx.compose.material3.*` |
| TV padding 32-48dp | Mobile padding 16-24dp |
| Large text (TV distance) | Standard mobile text |
| Focus management | Không cần |

- `BackHandler(enabled = level >= 2)` — giữ nguyên logic
- Poster grid: 3 columns portrait
- Spam filter `isValidLink()`: copy as-is

### Task 2.3: FshareDetailViewModel (file mới)
**New** `ui/screens/fshare/FshareDetailViewModel.kt`
- Copy từ PhimTV, đổi package
- Thay `ContinueStore` → `WatchHistoryManager` (existing)

### Task 2.4: FshareDetailScreen (file mới, adapt TV→Mobile)
**New** `ui/screens/fshare/FshareDetailScreen.kt`
- Copy từ PhimTV, adapt layout:

| TV Layout | Mobile Layout |
|---|---|
| Landscape: poster trái, info phải | Portrait: poster trên (parallax), info dưới |
| D-pad focus navigation | Touch scroll |
| `onKeyEvent` handlers | Không cần |

Sections: Poster hero + Movie info + Action buttons + Episode list

### Task 2.5: FshareActionButtons (file mới, adapt)
**New** `ui/screens/fshare/FshareActionButtons.kt`
- `tv.material3.Button` → `material3.Button`
- Bỏ `focusable()`, thêm touch ripple
- Standard mobile button sizes

### Task 2.6: FshareEpisodePanel (file mới, adapt)
**New** `ui/screens/fshare/FshareEpisodePanel.kt`
- TV: side panel slide-in → **Mobile: ModalBottomSheet**
- Giữ: episode indicator, ĐANG XEM badge, name strip

### Task 2.7: FsharePlayerLoader (file mới)
**New** `ui/screens/player/FsharePlayerLoader.kt`
- Copy từ PhimTV, đổi package
- Enriched slug format: `fshare-folder:URL|||NAME|||THUMB` — giữ nguyên
- Resolve Fshare URL → CDN download URL

> **Build check sau Phase 2** — compile OK trước Phase 3.

---

## Phase 3: WIRE — Connect vào App (🔌 ~1 hour)

### Task 3.1: Routes
**Modify** `navigation/Screen.kt` — thêm:
```kotlin
data object Community : Screen("community")
data object FshareDetail : Screen("fshare_detail/{slug}") {
    fun createRoute(slug: String) = "fshare_detail/${URLEncoder.encode(slug, "UTF-8")}"
}
```

### Task 3.2: Navigation Graph
**Modify** `navigation/AppNavigation.kt` — thêm:
- `composable(Screen.Community.route)` → `CommunityScreen`
- `composable(Screen.FshareDetail.route)` → `FshareDetailScreen`
- Bottom nav: thêm tab "Cộng đồng" (`Icons.Default.Cloud`)
- Helper: `startFsharePlayer(movieSlug, episodeSlug)` → PlayerActivity

### Task 3.3: Player Integration
**Modify** `ui/screens/player/PlayerViewModel.kt` — thêm:
```kotlin
fun loadFshare(context: Context, movieSlug: String, episodeSlug: String) {
    viewModelScope.launch {
        val result = FsharePlayerLoader.load(context, movieSlug, episodeSlug)
        _title.value = result.movieName
        _episodes.value = result.episodes
        _currentEp.value = result.episodes.indexOfFirst {
            it.slug == episodeSlug
        }.coerceAtLeast(0)
    }
}
```

**Modify** `PlayerActivity.kt` — thêm `"fshare"` source branch trong `onCreate`

**Modify** `PlayerScreen.kt` — khi source=`"fshare"`:
- `ProgressiveMediaSource` (MP4/MKV trực tiếp, không HLS)
- Fshare CDN headers
- Save progress qua `WatchHistoryManager` với `"fshare"` tag

### Task 3.4: Settings — Fshare Login
**Modify** `ui/screens/settings/SettingsScreen.kt` — thêm section Fshare login/logout

### Task 3.5: Version Bump
**Modify** `app/build.gradle.kts`:
- versionCode 63 → 64
- versionName "1.20.8" → "1.21.0"

---

## Phase 4: TEST (✅ ~1 hour)

| # | AC | Test | Expected |
|---|---|---|---|
| 1 | Login | Settings → Fshare → credentials | Token saved, VIP shown |
| 2 | VIP badge | Login → Home | "F VIP 🟢" visible |
| 3 | Browse | Community → Zinzuno → Phim Lẻ | ≥10 movies |
| 4 | Detail | Tap Fshare movie | Poster + title + episodes |
| 5 | Play | Play 3 movies from 3 sources | All play < 5s |
| 6 | Resume | Play → exit 50% → re-enter | Resume ±5s |
| 7 | Back flow | Community drill → Back×3 | Each Back = 1 level up |
| 8 | Continue | Play → exit → Home "Xem tiếp" | Shows with poster |
| 9 | Spam filter | Open Melodies of Life | No junk rows |
| 10 | Kill switch | Set flag=false | Tab hidden, no crash |

---

## 📁 Summary

### 13 New Files (standalone, dễ debug)
```
data/api/FshareApi.kt
data/api/models/FshareModels.kt
data/api/models/ThuVienCineModels.kt
data/repository/FshareRepository.kt
data/repository/ThuVienCineRepository.kt
data/repository/CommunityRepository.kt
ui/screens/fshare/FshareDetailScreen.kt
ui/screens/fshare/FshareDetailViewModel.kt
ui/screens/fshare/FshareActionButtons.kt
ui/screens/fshare/FshareEpisodePanel.kt
ui/screens/community/CommunityScreen.kt
ui/screens/community/CommunityViewModel.kt
ui/screens/player/FsharePlayerLoader.kt
```

### 9 Modified Files (minimal changes)
```
util/Constants.kt         → +5 lines (URLs + BuildConfig)
data/api/ApiClient.kt     → +8 lines (Fshare Retrofit)
data/local/WatchHistoryManager.kt → +Fshare source tag
navigation/Screen.kt      → +5 lines (2 routes)
navigation/AppNavigation.kt → +composable entries + tab
PlayerViewModel.kt        → +loadFshare() method
PlayerActivity.kt         → +fshare source branch
PlayerScreen.kt           → +fshare media source
build.gradle.kts           → +version + buildConfigField
```

### ⚡ Execution Order
```
Phase 1 → build ✓ → Phase 2 → build ✓ → Phase 3 → build ✓ → Phase 4 test
```
