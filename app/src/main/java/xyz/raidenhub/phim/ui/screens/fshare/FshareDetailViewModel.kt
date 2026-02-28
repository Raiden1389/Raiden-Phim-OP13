package xyz.raidenhub.phim.ui.screens.fshare

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import xyz.raidenhub.phim.data.api.models.Category
import xyz.raidenhub.phim.data.api.models.Episode
import xyz.raidenhub.phim.data.api.models.EpisodeServer
import xyz.raidenhub.phim.data.api.models.FshareFile
import xyz.raidenhub.phim.data.api.models.MovieDetail
import xyz.raidenhub.phim.data.repository.FshareAuthException
import xyz.raidenhub.phim.data.repository.FshareRepository
import xyz.raidenhub.phim.data.repository.FshareAggregator
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

    private val fshareHub = FshareAggregator()
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

    /** True when episode list contains only folder entries */
    val isFolderPlaceholder: Boolean
        get() = episodes.firstOrNull()?.serverData?.firstOrNull()?.slug == FOLDER_SLUG

    /** Folder navigation stack — for Back button support */
    private val folderStack = mutableListOf<List<EpisodeServer>>()
    private var folderDepth by mutableIntStateOf(0)
    val canNavigateBack: Boolean get() = folderDepth > 0

    fun navigateBack(): Boolean {
        if (folderStack.isEmpty()) return false
        episodes = folderStack.removeLast()
        folderDepth = folderStack.size
        return true
    }

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

                val detail = fshareHub.getDetailWithFshare(detailUrl)
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

    /**
     * Try listing folder contents — shows subfolders as clickable items
     */
    private suspend fun tryListFolder(folderUrl: String): List<Episode> {
        return try {
            val items = fshareRepo.listFolder(folderUrl)
            val videoFiles = items.filter { it.isVideo }.sortedBy { it.name }
            val subFolders = items.filter { it.isFolder }.sortedBy { it.name }

            when {
                videoFiles.isNotEmpty() -> FshareFile.toEpisodes(videoFiles)
                subFolders.isNotEmpty() -> subFolders.map { folderEntry(it) }
                else -> listOf(folderPlaceholder(folderUrl, "📁 Folder trống"))
            }
        } catch (e: FshareAuthException) {
            listOf(folderPlaceholder(folderUrl, "📁 Bấm để đăng nhập & xem"))
        } catch (e: Exception) {
            listOf(folderPlaceholder(folderUrl, "📁 Bấm để mở folder"))
        }
    }

    /** Create a clickable folder entry */
    private fun folderEntry(file: FshareFile) = Episode(
        name = "📁 ${file.name}",
        slug = FOLDER_SLUG,
        linkEmbed = file.furl,
        linkM3u8 = file.furl
    )

    private fun folderPlaceholder(url: String, label: String) = Episode(
        name = label,
        slug = FOLDER_SLUG,
        linkEmbed = url,
        linkM3u8 = url
    )

    /**
     * Expand/open a folder: list contents → show subfolders or video files.
     * @param folderUrl folder URL to open (null = use root fshareUrl)
     */
    fun expandFolder(folderUrl: String? = null) {
        val url = folderUrl ?: fshareUrl ?: return
        if (isFolderExpanding) return

        // Save current state for Back navigation
        if (episodes.isNotEmpty()) {
            folderStack.add(episodes)
            folderDepth = folderStack.size
        }

        viewModelScope.launch {
            isFolderExpanding = true
            folderError = null
            try {
                val items = fshareRepo.listFolder(url)
                val videoFiles = items.filter { it.isVideo }.sortedBy { it.name }
                val subFolders = items.filter { it.isFolder }.sortedBy { it.name }

                val episodeList = when {
                    videoFiles.isNotEmpty() -> FshareFile.toEpisodes(videoFiles)
                    subFolders.isNotEmpty() -> subFolders.map { folderEntry(it) }
                    else -> {
                        folderError = "Folder trống hoặc không có video"
                        isFolderExpanding = false
                        return@launch
                    }
                }

                episodes = listOf(
                    EpisodeServer(serverName = "Fshare HD", serverData = episodeList)
                )

                val count = if (videoFiles.isNotEmpty()) "${videoFiles.size} tập" else "${subFolders.size} thư mục"
                movie = movie?.copy(episodeCurrent = count)

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
