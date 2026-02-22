package xyz.raidenhub.phim.data.local

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import xyz.raidenhub.phim.data.db.AppDatabase
import xyz.raidenhub.phim.data.db.entity.HeroFilterEntity

/**
 * H-1 — Hero Carousel Filter (Room-backed).
 */
object HeroFilterManager {
    private val scope = CoroutineScope(Dispatchers.IO)
    private lateinit var db: AppDatabase

    fun init(db: AppDatabase) {
        this.db = db
    }

    val hiddenSlugs: Flow<Set<String>>
        get() = db.heroFilterDao().getHiddenSlugs().map { it.toSet() }

    val hiddenCount: Flow<Int>
        get() = db.heroFilterDao().count()

    fun hide(slug: String) {
        scope.launch { db.heroFilterDao().hide(HeroFilterEntity(slug)) }
    }

    fun clearAll() {
        scope.launch { db.heroFilterDao().clearAll() }
    }
}
