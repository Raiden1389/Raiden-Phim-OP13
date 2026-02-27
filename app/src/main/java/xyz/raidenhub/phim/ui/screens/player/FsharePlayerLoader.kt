package xyz.raidenhub.phim.ui.screens.player

import android.content.Context
import android.util.Log
import xyz.raidenhub.phim.data.api.models.Episode
import xyz.raidenhub.phim.data.repository.FshareRepository

/**
 * FsharePlayerLoader — Isolated Fshare loading logic for PlayerViewModel.
 *
 * Resolves Fshare file URL → CDN download URL.
 * Extracts movie name and poster from enriched slug format:
 *   "fshare-folder:URL|||NAME|||THUMB" or "fshare-file:URL|||NAME|||THUMB"
 *
 * For folders: lists all video files so player can navigate episodes.
 * For single files: creates a single-episode list.
 */
object FsharePlayerLoader {

    private const val TAG = "FshareLoader"

    data class Result(
        val movieName: String,
        val posterUrl: String,
        val cleanSlug: String,      // "fshare:URL" — WatchHistory key
        val downloadUrl: String,    // CDN streaming URL
        val episodes: List<Episode>,
    )

    /**
     * Resolve Fshare content for player.
     *
     * @param slug Enriched slug: "fshare-folder:URL|||NAME|||THUMB"
     * @param episodeSlug Raw Fshare URL: "https://www.fshare.vn/file/XXX"
     * @throws FshareAuthException if login fails
     * @throws FshareResolveException if CDN URL resolution fails
     */
    suspend fun load(context: Context, slug: String, episodeSlug: String): Result {
        Log.d(TAG, "═══ FSHARE DIRECT PLAY ═══")
        Log.d(TAG, "slug=$slug, episodeSlug=$episodeSlug")

        val fshareRepo = FshareRepository.getInstance(context)
        val downloadUrl = fshareRepo.resolveLink(episodeSlug)
        Log.d(TAG, "CDN URL resolved (${downloadUrl.length} chars)")

        // Extract name and poster from enriched slug
        val parts = slug.split("|||")
        val movieName = parts.getOrNull(1)?.takeIf { it.isNotBlank() } ?: "Fshare"
        val posterUrl = parts.getOrNull(2)?.takeIf { it.isNotBlank() } ?: ""
        val cleanSlug = "fshare:$episodeSlug"

        // Build episode list
        val episodes = buildEpisodeList(fshareRepo, slug, episodeSlug, movieName)

        Log.d(TAG, "Result: name=$movieName, episodes=${episodes.size}, cleanSlug=$cleanSlug")
        return Result(
            movieName = movieName,
            posterUrl = posterUrl,
            cleanSlug = cleanSlug,
            downloadUrl = downloadUrl,
            episodes = episodes,
        )
    }

    private suspend fun buildEpisodeList(
        fshareRepo: FshareRepository,
        slug: String,
        currentEpisodeSlug: String,
        movieName: String
    ): List<Episode> {
        val rawSlug = slug.split("|||").firstOrNull() ?: slug
        val isFolder = rawSlug.startsWith("fshare-folder:")
        val folderUrl = rawSlug.removePrefix("fshare-folder:").removePrefix("fshare-file:")

        Log.d(TAG, "buildEpisodeList: isFolder=$isFolder, folderUrl=$folderUrl")

        // Only list folder if URL is actually on fshare.vn (not ThuVienCine etc.)
        if (isFolder && folderUrl.isNotBlank() && "fshare.vn" in folderUrl) {
            try {
                val files = fshareRepo.listFolder(folderUrl)
                    .filter { it.isVideo }
                    .sortedBy { it.name }

                if (files.isNotEmpty()) {
                    Log.d(TAG, "Folder listed: ${files.size} video files")
                    return files.map { file ->
                        Episode(
                            name = "${file.episodeLabel} · ${file.quality} · ${file.sizeFormatted}",
                            slug = file.furl,
                            linkEmbed = file.furl,
                            linkM3u8 = file.furl
                        )
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Folder listing failed, fallback to single episode: ${e.message}")
            }
        }

        // Fallback: single episode
        return listOf(
            Episode(
                name = "",
                slug = currentEpisodeSlug,
                linkEmbed = currentEpisodeSlug,
                linkM3u8 = currentEpisodeSlug
            )
        )
    }
}
