package xyz.raidenhub.phim.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Intro/Outro config — stores per-series AND per-country defaults.
 *
 * Key format:
 *   "series:{slug}"    → intro/outro for specific series
 *   "country:{code}"   → default for country (KR, CN, JP, etc.)
 *
 * Lookup hierarchy: series → country → fallback constant
 */
@Entity(tableName = "intro_outro_config")
data class IntroOutroEntity(
    @PrimaryKey val key: String,          // "series:vo-lam-truyen-ky" | "country:KR"
    val introStart: Long = -1L,           // ms, -1 = not set
    val introEnd: Long = -1L,
    val outroStart: Long = -1L,
    val outroEnd: Long = -1L,
    val skipCount: Int = 0,               // how many times user confirmed this
    val updatedAt: Long = System.currentTimeMillis()
)
