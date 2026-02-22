package xyz.raidenhub.phim

import android.app.Application
import coil3.ImageLoader
import coil3.SingletonImageLoader
import coil3.disk.DiskCache
import coil3.memory.MemoryCache
import coil3.request.allowHardware
import okio.Path.Companion.toOkioPath
import xyz.raidenhub.phim.data.api.ApiClient
import xyz.raidenhub.phim.data.local.FavoriteManager
import xyz.raidenhub.phim.data.local.HeroFilterManager
import xyz.raidenhub.phim.data.local.IntroOutroManager
import xyz.raidenhub.phim.data.local.SectionOrderManager
import xyz.raidenhub.phim.data.local.PlaylistManager
import xyz.raidenhub.phim.data.local.SettingsManager
import xyz.raidenhub.phim.data.local.WatchHistoryManager
import xyz.raidenhub.phim.data.local.WatchlistManager
import xyz.raidenhub.phim.notification.EpisodeCheckWorker

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        // Init managers ở Application level — chạy TRƯỚC mọi Activity
        ApiClient.cacheDir = cacheDir
        FavoriteManager.init(this)
        WatchHistoryManager.init(this)
        SettingsManager.init(this)
        IntroOutroManager.init(this)
        WatchlistManager.init(this)
        PlaylistManager.init(this)
        HeroFilterManager.init(this)
        SectionOrderManager.init(this)

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

        // TD-3: Coil cache tuning — explicit 200MB disk + 50MB memory + hardware bitmaps
        SingletonImageLoader.setSafe {
            ImageLoader.Builder(this)
                .memoryCache {
                    MemoryCache.Builder()
                        .maxSizeBytes(50 * 1024 * 1024) // 50 MB
                        .build()
                }
                .diskCache {
                    DiskCache.Builder()
                        .directory(cacheDir.resolve("image_cache").toOkioPath())
                        .maxSizeBytes(200 * 1024 * 1024) // 200 MB
                        .build()
                }
                .allowHardware(true) // GPU-accelerated bitmaps
                .build()
        }
    }
}

