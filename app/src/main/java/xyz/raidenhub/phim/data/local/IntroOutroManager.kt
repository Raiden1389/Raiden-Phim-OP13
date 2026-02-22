package xyz.raidenhub.phim.data.local

import android.content.Context
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import xyz.raidenhub.phim.data.db.AppDatabase
import xyz.raidenhub.phim.data.db.entity.IntroOutroEntity

/**
 * IntroOutroManager — migrated to Room (Phase 03).
 * Lookup hierarchy: per-series > per-country > null (fall back to UI constants).
 */
object IntroOutroManager {
    private const val COUNTRY_PREFIX = "country:"
    private const val TAG = "IntroOutro"
    private val scope = CoroutineScope(Dispatchers.IO)
    private lateinit var db: AppDatabase

    /** UI-facing model — kept identical for backwards compatibility */
    data class SeriesConfig(
        val introStartMs: Long = -1L,
        val introEndMs: Long = -1L,
        val outroStartMs: Long = -1L
    ) {
        val hasIntro: Boolean get() = introStartMs >= 0 && introEndMs > introStartMs
        val hasIntroEnd: Boolean get() = introEndMs > 0
        val hasOutro: Boolean get() = outroStartMs > 0
    }

    fun init(db: AppDatabase) {
        this.db = db
        Log.d(TAG, "init: Room-backed IntroOutroManager ready")
    }

    /** Blocking lookup — called from player coroutine scope anyway */
    fun getEffectiveConfig(slug: String, country: String): SeriesConfig? = runBlocking(Dispatchers.IO) {
        db.introOutroDao().getForSeries(slug)?.toSeriesConfig()
            ?: db.introOutroDao().getForCountry(country)?.toSeriesConfig()
    }

    fun hasSeriesOverride(slug: String): Boolean = runBlocking(Dispatchers.IO) {
        db.introOutroDao().getForSeries(slug) != null
    }

    fun getCountryDefault(country: String): SeriesConfig? = runBlocking(Dispatchers.IO) {
        db.introOutroDao().getForCountry(country)?.toSeriesConfig()
    }

    fun getConfig(slug: String): SeriesConfig? = runBlocking(Dispatchers.IO) {
        db.introOutroDao().getForSeries(slug)?.toSeriesConfig()
    }

    fun getAllCountryDefaults(): Flow<List<IntroOutroEntity>> =
        db.introOutroDao().getAllCountryDefaults()

    fun getCountryDisplayName(country: String): String = when (country) {
        "han-quoc"    -> "Hàn Quốc"
        "trung-quoc"  -> "Trung Quốc"
        "nhat-ban"    -> "Nhật Bản"
        "my"          -> "Mỹ"
        "anh"         -> "Anh"
        "au"          -> "Úc"
        "phap"        -> "Pháp"
        "thai-lan"    -> "Thái Lan"
        "viet-nam"    -> "Việt Nam"
        else          -> country
    }

    fun saveIntroStart(slug: String, ms: Long) = updateConfig(slug) { it.copy(introStart = ms) }
    fun saveIntroEnd(slug: String, ms: Long)   = updateConfig(slug) { it.copy(introEnd = ms) }
    fun saveOutroStart(slug: String, ms: Long) = updateConfig(slug) { it.copy(outroStart = ms) }

    fun resetConfig(slug: String) {
        scope.launch { db.introOutroDao().delete("series:$slug") }
    }

    fun promoteToCountryDefault(slug: String, country: String) {
        scope.launch {
            val entity = db.introOutroDao().getForSeries(slug) ?: return@launch
            db.introOutroDao().upsert(entity.copy(key = "$COUNTRY_PREFIX$country"))
        }
    }

    fun saveCountryDefault(country: String, config: SeriesConfig) {
        scope.launch {
            db.introOutroDao().upsert(IntroOutroEntity(
                key = "$COUNTRY_PREFIX$country",
                introStart = config.introStartMs,
                introEnd = config.introEndMs,
                outroStart = config.outroStartMs
            ))
        }
    }

    fun resetCountryDefault(country: String) {
        scope.launch { db.introOutroDao().delete("$COUNTRY_PREFIX$country") }
    }

    // ═══ Helpers ═══

    private fun updateConfig(slug: String, block: (IntroOutroEntity) -> IntroOutroEntity) {
        scope.launch {
            val key = "series:$slug"
            val current = db.introOutroDao().get(key) ?: IntroOutroEntity(key = key)
            db.introOutroDao().upsert(block(current))
        }
    }

    private fun IntroOutroEntity.toSeriesConfig() = SeriesConfig(
        introStartMs = introStart,
        introEndMs = introEnd,
        outroStartMs = outroStart
    )
}
