package xyz.raidenhub.phim.data.local

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.google.gson.reflect.TypeToken

/**
 * Manages intro/outro timing configs with 3-level hierarchy:
 * 1. Per-series (slug)
 * 2. Per-country default ("country:han-quoc")
 * 3. null → fallback
 */
object IntroOutroManager {
    private const val PREF_NAME = "intro_outro_configs"
    private const val KEY_CONFIGS = "configs"
    private const val COUNTRY_PREFIX = "country:"
    private const val TAG = "IntroOutro"

    private lateinit var prefs: SharedPreferences
    private val gson = Gson()
    private var cache: MutableMap<String, SeriesConfig> = mutableMapOf()

    data class SeriesConfig(
        @SerializedName("introStartMs") val introStartMs: Long = -1L,
        @SerializedName("introEndMs") val introEndMs: Long = -1L,
        @SerializedName("outroStartMs") val outroStartMs: Long = -1L
    ) {
        val hasIntro: Boolean get() = introStartMs >= 0 && introEndMs > introStartMs
        val hasIntroEnd: Boolean get() = introEndMs > 0
        val hasOutro: Boolean get() = outroStartMs > 0
    }

    fun init(context: Context) {
        prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        cache = loadAll().toMutableMap()
        Log.d(TAG, "init: loaded ${cache.size} configs")
    }

    fun getEffectiveConfig(slug: String, country: String): SeriesConfig? {
        return cache[slug] ?: cache["$COUNTRY_PREFIX$country"]
    }

    fun hasSeriesOverride(slug: String): Boolean {
        return cache.containsKey(slug)
    }

    fun getCountryDefault(country: String): SeriesConfig? {
        return cache["$COUNTRY_PREFIX$country"]
    }

    fun getCountryDisplayName(country: String): String {
        return when (country) {
            "han-quoc" -> "Hàn Quốc"
            "trung-quoc" -> "Trung Quốc"
            "nhat-ban" -> "Nhật Bản"
            "my" -> "Mỹ"
            "anh" -> "Anh"
            "au" -> "Úc"
            "phap" -> "Pháp"
            "thai-lan" -> "Thái Lan"
            "viet-nam" -> "Việt Nam"
            else -> country
        }
    }

    fun getConfig(slug: String): SeriesConfig? = cache[slug]

    fun saveIntroStart(slug: String, ms: Long) {
        val current = cache[slug] ?: SeriesConfig()
        cache[slug] = current.copy(introStartMs = ms)
        persistAll()
    }

    fun saveIntroEnd(slug: String, ms: Long) {
        val current = cache[slug] ?: SeriesConfig()
        cache[slug] = current.copy(introEndMs = ms)
        persistAll()
    }

    fun saveOutroStart(slug: String, ms: Long) {
        val current = cache[slug] ?: SeriesConfig()
        cache[slug] = current.copy(outroStartMs = ms)
        persistAll()
    }

    fun resetConfig(slug: String) {
        cache.remove(slug)
        persistAll()
    }

    fun promoteToCountryDefault(slug: String, country: String) {
        val seriesConfig = cache[slug] ?: return
        cache["$COUNTRY_PREFIX$country"] = seriesConfig
        persistAll()
    }

    fun saveCountryDefault(country: String, config: SeriesConfig) {
        cache["$COUNTRY_PREFIX$country"] = config
        persistAll()
    }

    fun resetCountryDefault(country: String) {
        cache.remove("$COUNTRY_PREFIX$country")
        persistAll()
    }

    private fun loadAll(): Map<String, SeriesConfig> {
        val json = prefs.getString(KEY_CONFIGS, null)
        if (json.isNullOrBlank()) return emptyMap()
        return try {
            // Map<String, SeriesConfig> — TypeToken cần giữ vì Map không có Array alternative
            // Nhưng SeriesConfig đã có @SerializedName nên field mapping OK
            val result: Map<String, SeriesConfig> = gson.fromJson(
                json, object : TypeToken<Map<String, SeriesConfig>>() {}.type
            )
            result
        } catch (e: Exception) {
            Log.e(TAG, "loadAll FAILED: ${e.message}")
            emptyMap()
        }
    }

    private fun persistAll() {
        prefs.edit().putString(KEY_CONFIGS, gson.toJson(cache)).commit()
    }
}
