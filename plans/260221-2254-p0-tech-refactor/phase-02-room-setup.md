# Phase 02: Room DB Setup + Core Entities
Status: ⬜ Pending
Dependencies: Phase 01 (ViewModel đã tách → dễ swap data source)
Est: 1-2 sessions

## Objective
Thiết lập Room DB infrastructure và tạo Entity/DAO cho tất cả data hiện đang lưu trong SharedPreferences.

## Requirements

### 2.1 Setup Room Dependencies

- [ ] 2.1.1 Thêm Room dependencies vào `app/build.gradle.kts`:
  ```kotlin
  // Room 2.7.0 — dùng KSP, KHÔNG dùng KAPT
  val roomVersion = "2.7.0"
  implementation("androidx.room:room-runtime:$roomVersion")
  implementation("androidx.room:room-ktx:$roomVersion")  // Coroutine support
  ksp("androidx.room:room-compiler:$roomVersion")
  ```
- [ ] 2.1.2 Thêm KSP plugin nếu chưa có:
  ```kotlin
  // build.gradle.kts (project)
  id("com.google.devtools.ksp") version "2.2.20-1.0.29" apply false
  // app/build.gradle.kts
  id("com.google.devtools.ksp")
  ```
- [ ] 2.1.3 Build verify — KSP + Room compile OK

### 2.2 Design Database Schema

```
RaidenPhimDB (version = 1)
├── favorite_movies     — slug (PK), name, thumbUrl, posterUrl, year, quality, addedAt
├── watch_history       — slug (PK), name, thumbUrl, posterUrl, lastWatchedAt, episodeIdx, serverIdx, positionMs, totalDurationMs
├── continue_watching   — slug (PK), name, thumbUrl, server, episode, positionMs, timestamp
├── watchlist           — slug (PK), name, thumbUrl, posterUrl, year, addedAt
├── playlists           — id (PK), name, createdAt
├── playlist_items      — playlistId + movieSlug (composite PK), addedAt
├── hidden_heroes       — slug (PK), hiddenAt
├── section_order       — sectionId (PK), position
├── intro_outro_config  — key (PK: "series:{slug}" | "country:{code}"), introStart, introEnd, outroStart, outroEnd, updatedAt
├── settings            — key (PK), value (String — flexible key-value store)
└── search_history      — query (PK), searchedAt, count
```

### 2.3 Create Entities

- [ ] 2.3.1 `data/db/entity/FavoriteEntity.kt`
- [ ] 2.3.2 `data/db/entity/WatchHistoryEntity.kt` + `ContinueWatchingEntity.kt`
- [ ] 2.3.3 `data/db/entity/WatchlistEntity.kt`
- [ ] 2.3.4 `data/db/entity/PlaylistEntity.kt` + `PlaylistItemEntity.kt`
- [ ] 2.3.5 `data/db/entity/HeroFilterEntity.kt`
- [ ] 2.3.6 `data/db/entity/SectionOrderEntity.kt`
- [ ] 2.3.7 `data/db/entity/IntroOutroEntity.kt`
- [ ] 2.3.8 `data/db/entity/SettingEntity.kt`
- [ ] 2.3.9 `data/db/entity/SearchHistoryEntity.kt`

### 2.4 Create DAOs

- [ ] 2.4.1 `data/db/dao/FavoriteDao.kt`
  ```kotlin
  @Dao
  interface FavoriteDao {
      @Query("SELECT * FROM favorite_movies ORDER BY addedAt DESC")
      fun getAll(): Flow<List<FavoriteEntity>>

      @Query("SELECT EXISTS(SELECT 1 FROM favorite_movies WHERE slug = :slug)")
      fun isFavorite(slug: String): Flow<Boolean>

      @Insert(onConflict = OnConflictStrategy.REPLACE)
      suspend fun insert(fav: FavoriteEntity)

      @Query("DELETE FROM favorite_movies WHERE slug = :slug")
      suspend fun delete(slug: String)
  }
  ```
- [ ] 2.4.2 `data/db/dao/WatchHistoryDao.kt` — getAll, getContinueWatching (sorted), upsert, markWatched
- [ ] 2.4.3 `data/db/dao/WatchlistDao.kt` — getAll, toggle, isInWatchlist
- [ ] 2.4.4 `data/db/dao/PlaylistDao.kt` — CRUD playlists + items, getPlaylistWithItems
- [ ] 2.4.5 `data/db/dao/SettingsDao.kt` — key-value get/set
- [ ] 2.4.6 `data/db/dao/SearchHistoryDao.kt` — getRecent(limit), add, incrementCount

### 2.5 Create AppDatabase

- [ ] 2.5.1 `data/db/AppDatabase.kt`
  ```kotlin
  @Database(
      entities = [FavoriteEntity::class, WatchHistoryEntity::class, ...],
      version = 1,
      exportSchema = true  // quan trọng cho migration sau này
  )
  abstract class AppDatabase : RoomDatabase() {
      abstract fun favoriteDao(): FavoriteDao
      abstract fun watchHistoryDao(): WatchHistoryDao
      // ... other DAOs

      companion object {
          @Volatile private var INSTANCE: AppDatabase? = null
          fun getInstance(context: Context): AppDatabase {
              return INSTANCE ?: synchronized(this) {
                  Room.databaseBuilder(context, AppDatabase::class.java, "raiden_phim.db")
                      .build().also { INSTANCE = it }
              }
          }
      }
  }
  ```
- [ ] 2.5.2 Build verify — Room generates implementations OK

## Test Criteria
- [ ] Build thành công với Room dependencies
- [ ] Database instance tạo được
- [ ] Schema export JSON generated (cho future migration reference)
- [ ] Chưa kết nối với UI — tất cả Managers cũ vẫn hoạt động bình thường

## Notes
- **exportSchema = true** — Lưu schema JSON vào `schemas/` folder. Cần cho Room auto-migration
- **KSP thay KAPT** — Build nhanh hơn 2-3x. Kotlin 2.2+ phải dùng KSP
- **Flow return type** — Tất cả query trả về Flow<> để reactive với Compose collectAsState()

---
Next Phase: [phase-03-manager-migration.md]
