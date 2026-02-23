package xyz.raidenhub.phim.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import xyz.raidenhub.phim.data.db.dao.*
import xyz.raidenhub.phim.data.db.entity.*

@Database(
    entities = [
        FavoriteEntity::class,
        ContinueWatchingEntity::class,
        WatchedEpisodeEntity::class,
        WatchlistEntity::class,
        PlaylistEntity::class,
        PlaylistItemEntity::class,
        HeroFilterEntity::class,
        SectionOrderEntity::class,
        IntroOutroEntity::class,
        SettingEntity::class,
        SearchHistoryEntity::class,
    ],
    version = 2,   // v2: Added indexes on lastWatched, watched_episodes.slug
    exportSchema = true   // Lưu schema JSON → schemas/ folder (cần cho Room auto-migration sau này)
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun favoriteDao(): FavoriteDao
    abstract fun watchHistoryDao(): WatchHistoryDao
    abstract fun watchlistDao(): WatchlistDao
    abstract fun playlistDao(): PlaylistDao
    abstract fun heroFilterDao(): HeroFilterDao
    abstract fun sectionOrderDao(): SectionOrderDao
    abstract fun introOutroDao(): IntroOutroDao
    abstract fun settingsDao(): SettingsDao
    abstract fun searchHistoryDao(): SearchHistoryDao

    companion object {
        private const val DB_NAME = "raiden_phim.db"

        // P4: Migration 1→2 — thêm index, không xóa data
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE INDEX IF NOT EXISTS index_continue_watching_lastWatched ON continue_watching(lastWatched)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_watched_episodes_slug ON watched_episodes(slug)")
            }
        }

        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    DB_NAME
                )
                    .addMigrations(MIGRATION_1_2)  // Safe migration — không mất data
                    .fallbackToDestructiveMigration(dropAllTables = true)  // fallback nếu có version khác
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
