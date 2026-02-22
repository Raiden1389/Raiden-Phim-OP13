package xyz.raidenhub.phim.data.db.dao

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import xyz.raidenhub.phim.data.db.entity.PlaylistEntity
import xyz.raidenhub.phim.data.db.entity.PlaylistItemEntity

data class PlaylistWithItems(
    @Embedded val playlist: PlaylistEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "playlistId"
    )
    val items: List<PlaylistItemEntity>
)

@Dao
interface PlaylistDao {
    // ═══ Playlists ═══

    @Query("SELECT * FROM playlists ORDER BY createdAt DESC")
    fun getAllPlaylists(): Flow<List<PlaylistEntity>>

    @Transaction
    @Query("SELECT * FROM playlists ORDER BY createdAt DESC")
    fun getAllPlaylistsWithItems(): Flow<List<PlaylistWithItems>>

    @Transaction
    @Query("SELECT * FROM playlists WHERE id = :id")
    fun getPlaylistWithItems(id: Long): Flow<PlaylistWithItems?>

    @Insert
    suspend fun createPlaylist(playlist: PlaylistEntity): Long

    @Query("UPDATE playlists SET name = :name WHERE id = :id")
    suspend fun renamePlaylist(id: Long, name: String)

    @Query("DELETE FROM playlists WHERE id = :id")
    suspend fun deletePlaylist(id: Long)   // CASCADE deletes items

    // ═══ Playlist Items ═══

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addItem(item: PlaylistItemEntity)

    @Query("DELETE FROM playlist_items WHERE playlistId = :playlistId AND movieSlug = :slug")
    suspend fun removeItem(playlistId: Long, slug: String)

    @Query("SELECT EXISTS(SELECT 1 FROM playlist_items WHERE playlistId = :playlistId AND movieSlug = :slug)")
    suspend fun isInPlaylist(playlistId: Long, slug: String): Boolean
}
