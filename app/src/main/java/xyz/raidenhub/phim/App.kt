package xyz.raidenhub.phim

import android.app.Application
import xyz.raidenhub.phim.data.api.ApiClient
import xyz.raidenhub.phim.data.local.FavoriteManager
import xyz.raidenhub.phim.data.local.IntroOutroManager
import xyz.raidenhub.phim.data.local.SettingsManager
import xyz.raidenhub.phim.data.local.WatchHistoryManager

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        // Init managers ở Application level — chạy TRƯỚC mọi Activity
        // Application context sống suốt process lifetime
        ApiClient.cacheDir = cacheDir
        FavoriteManager.init(this)
        WatchHistoryManager.init(this)
        SettingsManager.init(this)
        IntroOutroManager.init(this)
    }
}
