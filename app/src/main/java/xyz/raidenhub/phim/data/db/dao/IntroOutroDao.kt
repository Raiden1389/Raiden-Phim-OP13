package xyz.raidenhub.phim.data.db.dao

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import xyz.raidenhub.phim.data.db.entity.IntroOutroEntity

@Dao
interface IntroOutroDao {
    @Query("SELECT * FROM intro_outro_config WHERE `key` = :key")
    suspend fun get(key: String): IntroOutroEntity?

    /** Lookup series-specific config */
    @Query("SELECT * FROM intro_outro_config WHERE `key` = 'series:' || :slug")
    suspend fun getForSeries(slug: String): IntroOutroEntity?

    /** Lookup country default */
    @Query("SELECT * FROM intro_outro_config WHERE `key` = 'country:' || :countryCode")
    suspend fun getForCountry(countryCode: String): IntroOutroEntity?

    /** All country defaults — for Managing/Displaying in Settings */
    @Query("SELECT * FROM intro_outro_config WHERE `key` LIKE 'country:%'")
    fun getAllCountryDefaults(): Flow<List<IntroOutroEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: IntroOutroEntity)

    @Query("DELETE FROM intro_outro_config WHERE `key` = :key")
    suspend fun delete(key: String)

    @Query("UPDATE intro_outro_config SET skipCount = skipCount + 1 WHERE `key` = :key")
    suspend fun incrementSkipCount(key: String)
}
