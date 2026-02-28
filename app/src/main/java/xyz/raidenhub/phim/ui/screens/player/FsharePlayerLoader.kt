package xyz.raidenhub.phim.ui.screens.player

import android.content.Context
import android.util.Log
import xyz.raidenhub.phim.data.api.models.Episode
import xyz.raidenhub.phim.data.api.models.FshareFile
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
    suspend fun load(context: Context, slug: String, episodeSlug: String, epIdx: Int = 0): Result {
        Log.d(TAG, "═══ FSHARE DIRECT PLAY ═══")
        Log.d(TAG, "slug=$slug, episodeSlug=$episodeSlug, epIdx=$epIdx")

        val fshareRepo = FshareRepository.getInstance(context)

        // Extract name and poster from enriched slug
        val parts = slug.split("|||")
        val movieName = parts.getOrNull(1)?.takeIf { it.isNotBlank() } ?: "Fshare"
        val posterUrl = parts.getOrNull(2)?.takeIf { it.isNotBlank() } ?: ""

        // Clean episodeSlug: strip "fshare:" prefix if present (corruption from old saves)
        val cleanEpSlug = episodeSlug.removePrefix("fshare:")

        // Build episode list first (need it to find correct file for old entries)
        val episodes = buildEpisodeList(fshareRepo, slug, cleanEpSlug, movieName)

        // Determine which episode URL to resolve
        val resolveUrl = when {
            // Direct file URL — use it
            cleanEpSlug.isNotBlank() && "fshare.vn/file/" in cleanEpSlug -> cleanEpSlug
            // Folder URL or empty — find episode by index from listed files
            episodes.isNotEmpty() -> {
                val ep = episodes.getOrNull(epIdx) ?: episodes.first()
                ep.slug.takeIf { it.startsWith("https://") } ?: cleanEpSlug
            }
            // Fallback
            else -> cleanEpSlug
        }

        Log.d(TAG, "Resolving URL: ${resolveUrl.take(80)}")
        val downloadUrl = fshareRepo.resolveLink(resolveUrl)
        Log.d(TAG, "CDN URL resolved (${downloadUrl.length} chars)")

        val cleanSlug = "fshare:$resolveUrl"

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
                val allFiles = listFolderRecursive(fshareRepo, folderUrl, depth = 0)
                    .sortedBy { it.name }

                if (allFiles.isNotEmpty()) {
                    Log.d(TAG, "Folder listed: ${allFiles.size} video files (recursive)")
                    return FshareFile.toEpisodes(allFiles)
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

    /**
     * Recursively list video files in Fshare folder.
     * If folder contains subfolders, drill into them (max depth 2).
     */
    private suspend fun listFolderRecursive(
        fshareRepo: FshareRepository,
        folderUrl: String,
        depth: Int,
        folderPrefix: String = ""
    ): List<FshareFile> {
        if (depth > 2) return emptyList()  // safety limit

        val items = fshareRepo.listFolder(folderUrl)
        val videoFiles = items.filter { it.isVideo }
        val subFolders = items.filter { it.isFolder }

        Log.d(TAG, "listFolderRecursive depth=$depth prefix='$folderPrefix': ${videoFiles.size} videos, ${subFolders.size} subfolders")

        return if (videoFiles.isNotEmpty()) {
            if (folderPrefix.isNotEmpty()) {
                videoFiles.map { it.copy(name = "[$folderPrefix] ${it.name}") }
            } else {
                videoFiles
            }
        } else if (subFolders.isNotEmpty()) {
            // Only subfolders → drill into each
            subFolders.flatMap { folder ->
                Log.d(TAG, "Drilling into subfolder: ${folder.name} → ${folder.furl}")
                listFolderRecursive(fshareRepo, folder.furl, depth + 1, folder.name)
            }
        } else {
            emptyList()
        }
    }
}
