package xyz.raidenhub.phim.ui.screens.fshare

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import xyz.raidenhub.phim.data.api.models.Category
import xyz.raidenhub.phim.data.api.models.Episode
import xyz.raidenhub.phim.data.api.models.EpisodeServer
import xyz.raidenhub.phim.data.api.models.MovieDetail
import xyz.raidenhub.phim.data.repository.FshareAuthException
import xyz.raidenhub.phim.data.repository.FshareRepository
import xyz.raidenhub.phim.data.repository.ThuVienCineRepository

/**
 * Fshare Detail ViewModel — dedicated to ThuVienCine + Fshare flow
 *
 * Flow:
 *   1. Scrape ThuVienCine detail page → movie info + Fshare folder URL
 *   2. Try listing folder contents → video files as episodes
 *   3. If folder listing fails (auth) → show expandable folder chip
 *   4. User clicks folder → expandFolder() → auto-login → list files
 */
class FshareDetailViewModel(application: android.app.Application) :
    androidx.lifecycle.AndroidViewModel(application) {

    private val cineRepo = ThuVienCineRepository()
    private val fshareRepo = FshareRepository.getInstance(application)

    // ═══ UI State ═══
    var movie by mutableStateOf<MovieDetail?>(null)
        private set
    var episodes by mutableStateOf<List<EpisodeServer>>(emptyList())
        private set
    var isLoading by mutableStateOf(false)
        private set
    var error by mutableStateOf<String?>(null)
        private set

    // ═══ Fshare-specific ═══
    var fshareUrl by mutableStateOf<String?>(null)
        private set
    var isFolderExpanding by mutableStateOf(false)
        private set
    var folderError by mutableStateOf<String?>(null)
        private set

    /** True when episode list contains only the folder placeholder */
    val isFolderPlaceholder: Boolean
        get() = episodes.firstOrNull()?.serverData?.firstOrNull()?.slug == FOLDER_SLUG

    private var loadedUrl = ""

    companion object {
        const val FOLDER_SLUG = "fshare-folder"
    }

    /**
     * Load movie detail from ThuVienCine + try Fshare folder listing
     */
    fun loadDetail(detailUrl: String) {
        if (detailUrl == loadedUrl) return
        loadedUrl = detailUrl

        viewModelScope.launch {
            try {
                isLoading = true
                error = null

                val detail = cineRepo.getDetailWithFshare(detailUrl)
                val fshareLink = detail.fshareLink
                fshareUrl = fshareLink?.folderUrl

                movie = MovieDetail(
                    name = detail.title,
                    slug = detailUrl.trimEnd('/').substringAfterLast('/'),
                    originName = detail.originName,
                    content = detail.description,
                    posterUrl = detail.posterUrl,
                    thumbUrl = detail.backdropUrl,
                    year = detail.year.toIntOrNull() ?: 0,
                    episodeCurrent = if (fshareLink?.isFolder == true) "Folder" else "",
                    quality = "",
                    lang = "",
                    category = emptyList(),
                    country = if (detail.country.isNotEmpty()) {
                        listOf(Category(
                            name = ThuVienCineRepository.countryDisplayName(detail.country),
                            slug = detail.country
                        ))
                    } else emptyList()
                )

                if (fshareLink != null) {
                    val episodeList = if (fshareLink.isFolder) {
                        tryListFolder(fshareLink.folderUrl)
                    } else {
                        listOf(
                            Episode(
                                name = "▶️ Play",
                                slug = "fshare-play",
                                linkEmbed = fshareLink.folderUrl,
                                linkM3u8 = fshareLink.folderUrl
                            )
                        )
                    }
                    episodes = listOf(
                        EpisodeServer(serverName = "Fshare", serverData = episodeList)
                    )
                }

                isLoading = false
            } catch (e: Exception) {
                error = "Lỗi tải Fshare: ${e.message}"
                isLoading = false
            }
        }
    }

    private suspend fun tryListFolder(folderUrl: String): List<Episode> {
        return try {
            val files = fshareRepo.listFolder(folderUrl)
                .filter { it.isVideo }
                .sortedBy { it.name }
            if (files.isNotEmpty()) {
                files.map { file ->
                    Episode(
                        name = "${file.episodeLabel} · ${file.quality} · ${file.sizeFormatted}",
                        slug = file.furl,
                        linkEmbed = file.furl,
                        linkM3u8 = file.furl
                    )
                }
            } else {
                listOf(folderPlaceholder(folderUrl, "📁 Folder trống"))
            }
        } catch (e: FshareAuthException) {
            listOf(folderPlaceholder(folderUrl, "📁 Bấm để đăng nhập & xem"))
        } catch (e: Exception) {
            listOf(folderPlaceholder(folderUrl, "📁 Bấm để mở folder"))
        }
    }

    private fun folderPlaceholder(url: String, label: String) = Episode(
        name = label,
        slug = FOLDER_SLUG,
        linkEmbed = url,
        linkM3u8 = url
    )

    /**
     * Expand folder: auto-login → list files → replace placeholder with real episodes
     */
    fun expandFolder() {
        val url = fshareUrl ?: return
        if (isFolderExpanding) return

        viewModelScope.launch {
            isFolderExpanding = true
            folderError = null
            try {
                val files = fshareRepo.listFolder(url)
                    .filter { it.isVideo }
                    .sortedBy { it.name }

                if (files.isEmpty()) {
                    folderError = "Folder trống hoặc không có video"
                    isFolderExpanding = false
                    return@launch
                }

                val episodeList = files.map { file ->
                    Episode(
                        name = "${file.episodeLabel} · ${file.quality} · ${file.sizeFormatted}",
                        slug = file.furl,
                        linkEmbed = file.furl,
                        linkM3u8 = file.furl
                    )
                }

                episodes = listOf(
                    EpisodeServer(serverName = "Fshare HD", serverData = episodeList)
                )
                movie = movie?.copy(episodeCurrent = "${files.size} tập")

            } catch (e: FshareAuthException) {
                folderError = "Chưa đăng nhập Fshare. Vào Cài đặt → Fshare để đăng nhập."
            } catch (e: Exception) {
                folderError = "Lỗi mở folder: ${e.message}"
            }
            isFolderExpanding = false
        }
    }

    /**
     * Load a raw Fshare folder/file URL directly (from Community sources).
     */
    fun loadFolderDirect(rawUrl: String, movieName: String = "Fshare", posterUrl: String = "") {
        if (rawUrl == loadedUrl) return
        loadedUrl = rawUrl
        fshareUrl = rawUrl

        val isFolder = "folder" in rawUrl

        movie = MovieDetail(
            name = movieName,
            slug = rawUrl.trimEnd('/').substringAfterLast('/'),
            originName = "",
            content = "",
            posterUrl = posterUrl,
            thumbUrl = posterUrl,
            year = 0,
            episodeCurrent = if (isFolder) "Folder" else "",
            quality = "",
            lang = "",
            category = listOf(Category(name = "Community")),
            country = emptyList()
        )

        if (isFolder) {
            viewModelScope.launch {
                isLoading = true
                error = null
                val episodeList = tryListFolder(rawUrl)
                episodes = listOf(
                    EpisodeServer(serverName = "Fshare", serverData = episodeList)
                )
                isLoading = false
            }
        } else {
            episodes = listOf(
                EpisodeServer(
                    serverName = "Fshare HD",
                    serverData = listOf(
                        Episode(
                            name = "▶️ Play",
                            slug = rawUrl,
                            linkEmbed = rawUrl,
                            linkM3u8 = rawUrl
                        )
                    )
                )
            )
        }
    }
}
