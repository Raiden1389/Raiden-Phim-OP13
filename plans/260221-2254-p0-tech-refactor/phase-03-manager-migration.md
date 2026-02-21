# Phase 03: Manager Migration + Data Import
Status: ⬜ Pending
Dependencies: Phase 02 (Room DB + Entities + DAOs ready)
Est: 1 session

## Objective
Migrate từng Manager từ SharedPreferences → Room DB. Import data cũ để user không mất dữ liệu.

## Chiến Lược: Migrate Từng Manager — Không Big-Bang

**Thứ tự:** Ít rủi ro → Nhiều rủi ro

### 3.1 Migration Helper — SharedPrefs → Room

- [ ] 3.1.1 Tạo `data/db/migration/SharedPrefsImporter.kt`
  ```kotlin
  object SharedPrefsImporter {
      /**
       * Chạy 1 lần khi app upgrade. Đọc SharedPrefs → insert vào Room → đánh dấu "imported".
       * Flag lưu trong SharedPrefs riêng: "migration_done_v1" = true
       */
      suspend fun importIfNeeded(context: Context, db: AppDatabase) {
          val migrationPrefs = context.getSharedPreferences("migration", Context.MODE_PRIVATE)
          if (migrationPrefs.getBoolean("done_v1", false)) return
          
          importFavorites(context, db.favoriteDao())
          importWatchHistory(context, db.watchHistoryDao())
          importWatchlist(context, db.watchlistDao())
          importPlaylists(context, db.playlistDao())
          importHeroFilter(context, db.heroFilterDao())
          importSectionOrder(context, db.sectionOrderDao())
          importIntroOutro(context, db.introOutroDao())
          importSettings(context, db.settingsDao())
          importSearchHistory(context, db.searchHistoryDao())
          
          migrationPrefs.edit().putBoolean("done_v1", true).apply()
      }
  }
  ```
- [ ] 3.1.2 Gọi `SharedPrefsImporter.importIfNeeded()` trong `App.onCreate()` — chạy trên background thread

### 3.2 Migrate FavoriteManager (Đơn giản nhất)

- [ ] 3.2.1 Refactor `FavoriteManager` — đổi backing store từ SharedPrefs → `FavoriteDao`
  ```kotlin
  // TRƯỚC:
  private val _favorites = MutableStateFlow<Set<String>>(loadFromPrefs())
  
  // SAU:
  val favorites: Flow<List<FavoriteEntity>> = favoriteDao.getAll()
  fun isFavorite(slug: String): Flow<Boolean> = favoriteDao.isFavorite(slug)
  suspend fun toggle(movie: Movie) { /* insert or delete */ }
  ```
- [ ] 3.2.2 Update UI: DetailScreen, HomeScreen — `.collectAsState()` thay vì `.value`
- [ ] 3.2.3 Build + verify Favorites hoạt động (toggle, list, persist across restart)

### 3.3 Migrate WatchHistoryManager (Medium — nhiều data nhất)

- [ ] 3.3.1 Refactor `WatchHistoryManager` → `WatchHistoryDao`
- [ ] 3.3.2 `saveContinueWatching()` → `upsert` vào Room
- [ ] 3.3.3 `notifyWidgetUpdate()` vẫn giữ nguyên (trigger Glance refresh)
- [ ] 3.3.4 Update: HomeScreen (Continue Watching row), WatchHistoryScreen, Widget
- [ ] 3.3.5 Build + verify Continue Watching, History, Widget hoạt động

### 3.4 Migrate WatchlistManager + PlaylistManager

- [ ] 3.4.1 Refactor `WatchlistManager` → `WatchlistDao`
- [ ] 3.4.2 Refactor `PlaylistManager` → `PlaylistDao` (CRUD + playlist items)
- [ ] 3.4.3 Update: WatchlistScreen, PlaylistListScreen, PlaylistDetailScreen
- [ ] 3.4.4 Build + verify Watchlist + Playlists hoạt động

### 3.5 Migrate SettingsManager + SearchHistory

- [ ] 3.5.1 `SettingsManager` — key-value store → `SettingsDao`
  - Đặc biệt: export/import backup cần update (export Room → JSON, import JSON → Room)
- [ ] 3.5.2 `SearchHistoryManager` → `SearchHistoryDao`
- [ ] 3.5.3 Build + verify Settings, Search History hoạt động

### 3.6 Migrate Small Managers

- [ ] 3.6.1 `HeroFilterManager` → `HeroFilterDao`
- [ ] 3.6.2 `SectionOrderManager` → `SectionOrderDao`
- [ ] 3.6.3 `IntroOutroManager` → `IntroOutroDao`
  - Lưu ý: per-country defaults query = `WHERE key LIKE 'country:%'`
- [ ] 3.6.4 Build + verify tất cả hoạt động

### 3.7 Cleanup

- [ ] 3.7.1 Xoá SharedPreferences file cũ sau khi migration xong (giữ code migration 2 versions rồi xoá)
- [ ] 3.7.2 Remove Gson dependency nếu không còn dùng (chỉ API response parse — có thể giữ hoặc → TD-12)
- [ ] 3.7.3 Update ProGuard rules — bỏ `-keep class data.local.** { *; }` SharedPrefs rules, thêm Room annotation rules

## Test Criteria (Full Regression)

- [ ] Fresh install: app hoạt động bình thường (empty DB)
- [ ] Upgrade install: data cũ (favorites, history, watchlist, settings) được import đúng
- [ ] Favorites toggle OK — persist across restart
- [ ] Continue Watching row hiện đúng — resume đúng vị trí
- [ ] Watch History screen hiện đúng
- [ ] Watchlist + Playlists hoạt động
- [ ] Widget "Xem Tiếp" cập nhật khi xem phim
- [ ] Intro/Outro skip + per-country defaults hoạt động
- [ ] Settings (quality, filter, section order) persist
- [ ] Export/Import backup hoạt động
- [ ] Search history persist
- [ ] Hero filter (ẩn phim khỏi carousel) hoạt động
- [ ] **APK release build + ProGuard OK** (regression test cho Proguard data loss bug)

## ⚠️ Gotchas

1. **Room trên main thread crash** — Tất cả insert/update/delete phải `suspend` hoặc chạy trên `Dispatchers.IO`
2. **Migration flag** — Dùng SharedPrefs riêng (`"migration"`) để track, KHÔNG dùng DB vì DB chưa exist trước migration
3. **Gson inner class** — Sau khi remove Gson khỏi local data, ProGuard rule `-keep class **$* { *; }` có thể thu hẹp scope
4. **Widget update** — Glance widget đọc data từ Manager → Manager giờ đọc từ Room → widget update có thể hơi delay. Chấp nhận được.

---
Prev Phase: [phase-02-room-setup.md]
