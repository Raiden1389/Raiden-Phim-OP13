package xyz.raidenhub.phim.data.local

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

// CN-1: Home layout modes
enum class HomeLayout(val label: String, val emoji: String) {
    COMFORTABLE("Card lớn", "🖼️"),  // 2-col — default, best visual
    COMPACT("Lưới dày", "⋮"),              // 3-col — more content per screen
    LIST("Danh sách", "☰")               // 1-col — title + thumbnail row
}

object SettingsManager {
    private lateinit var prefs: SharedPreferences

    // ═══ Country filter ═══
    private val _selectedCountries = MutableStateFlow<Set<String>>(emptySet())
    val selectedCountries = _selectedCountries.asStateFlow()

    // ═══ Genre/Category filter ═══
    private val _selectedGenres = MutableStateFlow<Set<String>>(emptySet())
    val selectedGenres = _selectedGenres.asStateFlow()

    // ═══ Auto-play next episode ═══
    private val _autoPlayNext = MutableStateFlow(true)
    val autoPlayNext = _autoPlayNext.asStateFlow()

    // ═══ SE-1: Default playback quality ═══
    private val _defaultQuality = MutableStateFlow("auto") // auto / 360p / 720p / 1080p
    val defaultQuality = _defaultQuality.asStateFlow()
    val ALL_QUALITIES = listOf("auto" to "🔄 Tự động", "1080p" to "🔵 1080p HD", "720p" to "🟢 720p", "360p" to "🟡 360p")

    // ═══ N-1: New episode notifications ═══
    private val _notifyNewEpisode = MutableStateFlow(false)
    val notifyNewEpisode = _notifyNewEpisode.asStateFlow()

    // ═══ CN-1: Home Layout ═══
    private val _homeLayout = MutableStateFlow(HomeLayout.COMFORTABLE)
    val homeLayout = _homeLayout.asStateFlow()

    val activeFilterCount: Int
        get() = _selectedCountries.value.size + _selectedGenres.value.size

    // Available options
    val ALL_COUNTRIES = listOf(
        "han-quoc" to "🇰🇷 Hàn Quốc",
        "trung-quoc" to "🇨🇳 Trung Quốc",
        "au-my" to "🇺🇸 Âu Mỹ",
        "nhat-ban" to "🇯🇵 Nhật Bản",
        "thai-lan" to "🇹🇭 Thái Lan",
        "an-do" to "🇮🇳 Ấn Độ",
        "dai-loan" to "🇹🇼 Đài Loan",
        "hong-kong" to "🇭🇰 Hồng Kông",
        "philippines" to "🇵🇭 Philippines",
        "anh" to "🇬🇧 Anh",
    )

    val ALL_GENRES = listOf(
        "hanh-dong" to "🔥 Hành Động",
        "tinh-cam" to "💕 Tình Cảm",
        "hai-huoc" to "😂 Hài Hước",
        "co-trang" to "🏯 Cổ Trang",
        "tam-ly" to "🧠 Tâm Lý",
        "hinh-su" to "🔎 Hình Sự",
        "kinh-di" to "👻 Kinh Dị",
        "vien-tuong" to "🚀 Viễn Tưởng",
        "phieu-luu" to "🗺️ Phiêu Lưu",
        "vo-thuat" to "🥋 Võ Thuật",
        "hoc-duong" to "🎓 Học Đường",
        "bi-an" to "🕵️ Bí Ẩn",
        "chinh-kich" to "🎭 Chính Kịch",
        "gia-dinh" to "👨‍👩‍👧 Gia Đình",
        "chien-tranh" to "⚔️ Chiến Tranh",
        "am-nhac" to "🎵 Âm Nhạc",
        "than-thoai" to "🐉 Thần Thoại",
        "khoa-hoc" to "🔬 Khoa Học",
        "the-thao" to "⚽ Thể Thao",
        "tai-lieu" to "📹 Tài Liệu",
    )

    fun init(context: Context) {
        prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
        _selectedCountries.value = prefs.getStringSet("countries", null) ?: emptySet()
        _selectedGenres.value = prefs.getStringSet("genres", null) ?: emptySet()
        _autoPlayNext.value = prefs.getBoolean("autoPlayNext", true)
        _defaultQuality.value = prefs.getString("defaultQuality", "auto") ?: "auto"
        _notifyNewEpisode.value = prefs.getBoolean("notifyNewEpisode", false)
        _homeLayout.value = HomeLayout.values().find {
            it.name == prefs.getString("homeLayout", null)
        } ?: HomeLayout.COMFORTABLE
    }

    fun toggleCountry(slug: String) {
        val current = _selectedCountries.value.toMutableSet()
        if (slug in current) current.remove(slug) else current.add(slug)
        _selectedCountries.value = current
        prefs.edit().putStringSet("countries", current).apply()
    }

    fun toggleGenre(slug: String) {
        val current = _selectedGenres.value.toMutableSet()
        if (slug in current) current.remove(slug) else current.add(slug)
        _selectedGenres.value = current
        prefs.edit().putStringSet("genres", current).apply()
    }

    fun setAutoPlayNext(enabled: Boolean) {
        _autoPlayNext.value = enabled
        prefs.edit().putBoolean("autoPlayNext", enabled).apply()
    }

    fun setDefaultQuality(quality: String) {
        _defaultQuality.value = quality
        prefs.edit().putString("defaultQuality", quality).apply()
    }

    fun setNotifyNewEpisode(enabled: Boolean) {
        _notifyNewEpisode.value = enabled
        prefs.edit().putBoolean("notifyNewEpisode", enabled).apply()
    }

    fun setHomeLayout(layout: HomeLayout) {
        _homeLayout.value = layout
        prefs.edit().putString("homeLayout", layout.name).apply()
    }

    fun clearCountries() {
        _selectedCountries.value = emptySet()
        prefs.edit().remove("countries").apply()
    }

    fun clearGenres() {
        _selectedGenres.value = emptySet()
        prefs.edit().remove("genres").apply()
    }

    // ═══ SE-6: Export / Import backup ═══
    fun exportBackup(context: android.content.Context): String {
        val favPrefs = context.getSharedPreferences("favorites", android.content.Context.MODE_PRIVATE)
        val histPrefs = context.getSharedPreferences("watch_history", android.content.Context.MODE_PRIVATE)
        val watchlistPrefs = context.getSharedPreferences("watchlist", android.content.Context.MODE_PRIVATE)
        val playlistPrefs = context.getSharedPreferences("playlists", android.content.Context.MODE_PRIVATE)
        return org.json.JSONObject().apply {
            put("version", 1)
            put("exportedAt", System.currentTimeMillis())
            put("favorites", favPrefs.getString("favorites", "[]"))
            put("watchHistory", histPrefs.getString("watch_history_v2", "{}"))
            put("continueList", histPrefs.getString("continue_list_v2", "[]"))
            put("watchlist", watchlistPrefs.getString("watchlist_v1", "[]"))
            put("playlists", playlistPrefs.getString("playlists_v1", "[]"))
        }.toString()
    }

    fun importBackup(context: android.content.Context, json: String) {
        val obj = org.json.JSONObject(json)
        val favPrefs = context.getSharedPreferences("favorites", android.content.Context.MODE_PRIVATE)
        val histPrefs = context.getSharedPreferences("watch_history", android.content.Context.MODE_PRIVATE)
        val watchlistPrefs = context.getSharedPreferences("watchlist", android.content.Context.MODE_PRIVATE)
        val playlistPrefs = context.getSharedPreferences("playlists", android.content.Context.MODE_PRIVATE)

        obj.optString("favorites").takeIf { it.isNotBlank() }?.let {
            favPrefs.edit().putString("favorites", it).apply()
        }
        obj.optString("watchHistory").takeIf { it.isNotBlank() }?.let {
            histPrefs.edit().putString("watch_history_v2", it).apply()
        }
        obj.optString("continueList").takeIf { it.isNotBlank() }?.let {
            histPrefs.edit().putString("continue_list_v2", it).apply()
        }
        obj.optString("watchlist").takeIf { it.isNotBlank() }?.let {
            watchlistPrefs.edit().putString("watchlist_v1", it).apply()
        }
        obj.optString("playlists").takeIf { it.isNotBlank() }?.let {
            playlistPrefs.edit().putString("playlists_v1", it).apply()
        }
        // Note: Room-based managers (FavoriteManager, WatchHistoryManager, etc.)
        // are initialized in App.kt with DB — no reinit needed here.
        // Legacy SharedPrefs data written above is for migration only.
    }
}
