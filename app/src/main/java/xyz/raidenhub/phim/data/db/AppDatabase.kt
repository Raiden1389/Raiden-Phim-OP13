package xyz.raidenhub.phim.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
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
    version = 1,
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

        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    DB_NAME
                )
                    // Fallback: nếu schema thay đổi mà chưa có migration → xóa DB, recreate
                    // TODO: thay bằng addMigrations() khi lên version 2+
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
