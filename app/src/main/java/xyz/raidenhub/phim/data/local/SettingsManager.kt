package xyz.raidenhub.phim.data.local

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

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

    // Active filter count for badge
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

    fun clearCountries() {
        _selectedCountries.value = emptySet()
        prefs.edit().remove("countries").apply()
    }

    fun clearGenres() {
        _selectedGenres.value = emptySet()
        prefs.edit().remove("genres").apply()
    }
}
