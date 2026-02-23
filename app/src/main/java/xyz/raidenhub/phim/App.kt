package xyz.raidenhub.phim

import android.app.Application
import coil3.ImageLoader
import coil3.SingletonImageLoader
import coil3.disk.DiskCache
import coil3.memory.MemoryCache
import coil3.request.allowHardware
import coil3.request.crossfade
import okio.Path.Companion.toOkioPath
import xyz.raidenhub.phim.data.api.ApiClient
import xyz.raidenhub.phim.data.db.AppDatabase
import xyz.raidenhub.phim.data.local.FavoriteManager
import xyz.raidenhub.phim.data.local.HeroFilterManager
import xyz.raidenhub.phim.data.local.IntroOutroManager
import xyz.raidenhub.phim.data.local.SectionOrderManager
import xyz.raidenhub.phim.data.local.PlaylistManager
import xyz.raidenhub.phim.data.local.SettingsManager
import xyz.raidenhub.phim.data.local.WatchHistoryManager
import xyz.raidenhub.phim.data.local.WatchlistManager
import xyz.raidenhub.phim.data.local.SearchHistoryManager
import xyz.raidenhub.phim.notification.EpisodeCheckWorker

class App : Application() {
    override fun onCreate() {
        super.onCreate()

        // ── Init Room DB (Phase 03) ──
        val db = AppDatabase.getInstance(this)

        // ── Init Room-backed managers ──
        ApiClient.cacheDir = cacheDir
        FavoriteManager.init(db)
        WatchHistoryManager.init(db)
        WatchlistManager.init(db)
        PlaylistManager.init(db)
        HeroFilterManager.init(db)
        SectionOrderManager.init(db)
        SearchHistoryManager.init(db)
        IntroOutroManager.init(db)

        // ── SettingsManager — giữ SharedPrefs (nhỏ, reactive đơn giản) ──
        SettingsManager.init(this)

        // SuperStream (English content) — init WebView + FebBox cookie
        xyz.raidenhub.phim.data.repository.SuperStreamRepository.init(this)
        xyz.raidenhub.phim.data.repository.SuperStreamRepository.setFebBoxCookie(
            xyz.raidenhub.phim.util.Constants.FEBBOX_COOKIE
        )

        // N-1: Episode notification channel + schedule nếu user đã bật
        EpisodeCheckWorker.createChannel(this)
        if (SettingsManager.notifyNewEpisode.value) {
            EpisodeCheckWorker.schedule(this)
        }

        // TD-3: Coil cache — 2GB disk (user có 100GB free) + 150MB memory
        SingletonImageLoader.setSafe {
            ImageLoader.Builder(this)
                .memoryCache {
                    MemoryCache.Builder()
                        .maxSizeBytes(150 * 1024 * 1024) // 150 MB
                        .build()
                }
                .diskCache {
                    DiskCache.Builder()
                        .directory(cacheDir.resolve("image_cache").toOkioPath())
                        .maxSizeBytes(2L * 1024 * 1024 * 1024) // 2 GB
                        .build()
                }
                .allowHardware(true) // GPU-accelerated bitmaps
                .crossfade(300)      // P7: Smooth fade-in
                .build()
        }
    }
}
