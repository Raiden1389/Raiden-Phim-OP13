package xyz.raidenhub.phim.data.repository

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import xyz.raidenhub.phim.data.api.ApiClient
import xyz.raidenhub.phim.data.api.models.*
import xyz.raidenhub.phim.util.Constants
import xyz.raidenhub.phim.util.FebBoxWebViewHelper
import xyz.raidenhub.phim.util.ShowBoxCrypto

/**
 * SuperStream Repository — Coordinates TMDB + ShowBox + FebBox.
 *
 * Pipeline:
 *   1. TMDB search/detail → metadata (OkHttp)
 *   2. ShowBox share_link → share_key (OkHttp)
 *   3. FebBox file_share_list → seasons/episodes (WebView)
 *   4. FebBox player/video → m3u8 stream URLs (WebView)
 *   5. SubtitleRepository → subtitles (existing system)
 */
object SuperStreamRepository {

    private const val TAG = "SuperStreamRepo"

    private val tmdb = ApiClient.tmdb
    private val showbox = ApiClient.showbox
    private var febBoxHelper: FebBoxWebViewHelper? = null

    // ═══ Init ═══

    fun init(context: Context) {
        if (febBoxHelper == null) {
            febBoxHelper = FebBoxWebViewHelper(context.applicationContext)
        }
    }

    fun setFebBoxCookie(token: String) {
        febBoxHelper?.setCookie(token)
    }

    fun destroy() {
        febBoxHelper?.destroy()
        febBoxHelper = null
    }

    // ═══ TMDB Search (OkHttp) ═══

    suspend fun search(query: String, page: Int = 1): Result<TmdbSearchResponse> {
        return runCatching {
            tmdb.searchMulti(
                apiKey = Constants.TMDB_API_KEY,
                query = query,
                page = page
            )
        }.onFailure { Log.e(TAG, "Search error: ${it.message}") }
    }

    suspend fun trendingMovies(page: Int = 1): Result<List<TmdbSearchItem>> {
        return runCatching {
            tmdb.trendingMovies(apiKey = Constants.TMDB_API_KEY, page = page).results
        }.onFailure { Log.e(TAG, "Trending movies error: ${it.message}") }
    }

    suspend fun trendingTv(page: Int = 1): Result<List<TmdbSearchItem>> {
        return runCatching {
            tmdb.trendingTv(apiKey = Constants.TMDB_API_KEY, page = page).results
        }.onFailure { Log.e(TAG, "Trending TV error: ${it.message}") }
    }

    // ═══ TMDB Detail (OkHttp) ═══

    suspend fun getMovieDetail(tmdbId: Int): Result<TmdbMovieDetail> {
        return runCatching {
            tmdb.movieDetail(id = tmdbId, apiKey = Constants.TMDB_API_KEY)
        }.onFailure { Log.e(TAG, "Movie detail error: ${it.message}") }
    }

    suspend fun getTvDetail(tmdbId: Int): Result<TmdbTvDetail> {
        return runCatching {
            tmdb.tvDetail(id = tmdbId, apiKey = Constants.TMDB_API_KEY)
        }.onFailure { Log.e(TAG, "TV detail error: ${it.message}") }
    }

    suspend fun getTvSeasonDetail(tmdbId: Int, season: Int): Result<TmdbSeasonDetail> {
        return runCatching {
            tmdb.tvSeasonDetail(id = tmdbId, season = season, apiKey = Constants.TMDB_API_KEY)
        }.onFailure { Log.e(TAG, "Season detail error: ${it.message}") }
    }

    // ═══ ShowBox → FebBox Share Key ═══

    /**
     * Get share key using ShowBox encrypted search API.
     * 1. Search ShowBox by title → ShowBox internal ID
     * 2. Use internal ID with showbox.media/index/share_link → share_key
     *
     * ShowBox uses its OWN internal IDs, NOT TMDB IDs!
     */
    suspend fun getShareKeyByTitle(title: String, type: String): Result<String> {
        return runCatching {
            // Step 1: Search ShowBox to find internal ID
            val showBoxId = ShowBoxCrypto.findShowBoxId(title, type)
                ?: throw Exception("Not found on ShowBox: '$title'")

            Log.d(TAG, "ShowBox ID for '$title': $showBoxId")

            // Step 2: Use ShowBox internal ID to get share_link
            val showboxType = if (type == "movie") 1 else 2
            val response = showbox.getShareLink(id = showBoxId, type = showboxType)
            Log.d(TAG, "ShowBox share_link: code=${response.code}, link=${response.data?.link} (showboxId=$showBoxId)")

            if (response.code == 1 && response.data != null) {
                val key = response.data.shareKey
                if (key.isBlank()) throw Exception("Empty share key (showboxId=$showBoxId, link=${response.data.link})")
                Log.d(TAG, "✅ ShareKey: '$key' for '$title'")
                key
            } else {
                throw Exception("ShowBox error: ${response.msg}")
            }
        }.onFailure { Log.e(TAG, "ShareKey error: ${it.message}") }
    }

    /**
     * Legacy getShareKey — tries ShowBox ID directly (may work if IDs happen to match).
     * Kept as fallback.
     */
    suspend fun getShareKey(tmdbId: Int, type: String): Result<String> {
        return runCatching {
            val showboxType = if (type == "movie") 1 else 2
            val response = showbox.getShareLink(id = tmdbId, type = showboxType)
            Log.d(TAG, "ShowBox raw (tmdbId=$tmdbId): code=${response.code}, link=${response.data?.link}")
            if (response.code == 1 && response.data != null) {
                val key = response.data.shareKey
                if (key.isBlank()) throw Exception("Empty share key (tmdbId=$tmdbId)")
                key
            } else {
                throw Exception("ShowBox error: ${response.msg}")
            }
        }.onFailure { Log.e(TAG, "ShareKey fallback error: ${it.message}") }
    }

    // ═══ FebBox File List (WebView) ═══

    suspend fun getFileList(shareKey: String, parentId: Long = 0): Result<List<FebBoxFile>> {
        return withContext(Dispatchers.Main) {
            runCatching {
                febBoxHelper?.getFileList(shareKey, parentId)
                    ?: throw Exception("FebBox helper not initialized")
            }.onFailure { Log.e(TAG, "File list error: ${it.message}") }
        }
    }

    /**
     * Get seasons from root of share folder.
     */
    suspend fun getSeasons(shareKey: String): Result<List<FebBoxFile>> {
        return getFileList(shareKey, parentId = 0).map { files ->
            files.filter { it.isFolder }.sortedBy { it.name.lowercase() }
        }
    }

    /**
     * Get episodes from a season folder.
     */
    suspend fun getEpisodes(shareKey: String, seasonFid: Long): Result<List<FebBoxFile>> {
        return getFileList(shareKey, parentId = seasonFid).map { files ->
            files.filter { !it.isFolder && isVideoFile(it.name) }
                .sortedBy { extractEpisodeNumber(it.name) }
        }
    }

    // ═══ FebBox Stream URLs (WebView) ═══

    suspend fun getStreamUrls(shareKey: String, fid: Long): Result<List<FebBoxStream>> {
        return withContext(Dispatchers.Main) {
            runCatching {
                febBoxHelper?.getStreamUrls(shareKey, fid)
                    ?: throw Exception("FebBox helper not initialized")
            }.onFailure { Log.e(TAG, "Stream URL error: ${it.message}") }
        }
    }

    /**
     * Get best stream URL (prefer 1080p > 720p > AUTO).
     */
    suspend fun getBestStream(shareKey: String, fid: Long): Result<FebBoxStream> {
        return getStreamUrls(shareKey, fid).map { streams ->
            streams.firstOrNull { it.quality == "1080p" }
                ?: streams.firstOrNull { it.quality == "720p" }
                ?: streams.firstOrNull { it.quality == "AUTO" }
                ?: streams.firstOrNull()
                ?: throw Exception("No streams found")
        }
    }

    // ═══ Helpers ═══

    private fun isVideoFile(name: String): Boolean {
        val ext = name.substringAfterLast('.', "").lowercase()
        return ext in setOf("mp4", "mkv", "avi", "m4v", "webm", "mov", "ts")
    }

    /**
     * Extract episode number from filename like "S01E05.mkv" or "Episode 5.mp4".
     */
    private fun extractEpisodeNumber(name: String): Int {
        // Try S01E05 format
        val seMatch = Regex("""[Ss]\d+[Ee](\d+)""").find(name)
        if (seMatch != null) return seMatch.groupValues[1].toIntOrNull() ?: 0

        // Try "Season 1" or "season 12" format (for folder names)
        val seasonMatch = Regex("""(?:season|Season|SEASON)\s*(\d+)""", RegexOption.IGNORE_CASE).find(name)
        if (seasonMatch != null) return seasonMatch.groupValues[1].toIntOrNull() ?: 0

        // Try "Episode 5" or "Ep 5" format
        val epMatch = Regex("""(?:Episode|Ep)\s*(\d+)""", RegexOption.IGNORE_CASE).find(name)
        if (epMatch != null) return epMatch.groupValues[1].toIntOrNull() ?: 0

        // Try any leading number
        val numMatch = Regex("""^(\d+)""").find(name.substringAfterLast("/"))
        if (numMatch != null) return numMatch.groupValues[1].toIntOrNull() ?: 0

        // Try trailing number (e.g. "folder 3")
        val trailMatch = Regex("""(\d+)\s*$""").find(name)
        return trailMatch?.groupValues?.get(1)?.toIntOrNull() ?: 0
    }

    // ═══ FebBox Direct (OkHttp — no WebView!) ═══

    /**
     * Get file list from FebBox using OkHttp directly.
     */
    suspend fun getFileListDirect(shareKey: String, parentId: Long = 0): Result<List<FebBoxFile>> {
        return withContext(Dispatchers.IO) {
            runCatching {
                val url = "${Constants.FEBBOX_BASE_URL}file/file_share_list?share_key=$shareKey&pwd=&parent_id=$parentId&is_html=1"
                Log.d(TAG, "FebBox direct call: $url")
                
                val request = okhttp3.Request.Builder().url(url).get().build()
                val response = ApiClient.febboxClient.newCall(request).execute()
                val body = response.body?.string() ?: throw Exception("Empty response")
                
                Log.d(TAG, "FebBox response (first 300): ${body.take(300)}")
                
                val json = com.google.gson.JsonParser.parseString(body).asJsonObject
                val code = json.get("code")?.asInt ?: -1
                val msg = json.get("msg")?.asString ?: ""
                
                if (code != 1) throw Exception("FebBox API error: code=$code msg=$msg")
                
                val html = json.get("html")?.asString 
                    ?: throw Exception("No HTML in response")
                
                Log.d(TAG, "FebBox HTML length: ${html.length}")
                
                // Parse HTML to extract files
                val files = mutableListOf<FebBoxFile>()
                
                // Split HTML by file entry divs and parse each one
                // Actual HTML: <div class="file open_dir" data-id="2633059" data-path="season 12">
                //              ...  <p class="file_name">season 12</p>
                val entryPattern = Regex("""<div\s+class="file[^"]*"[^>]*data-id="(\d+)"[^>]*>[\s\S]*?(?=<div\s+class="file[^"]*"\s+data-id|\z)""")
                val entries = entryPattern.findAll(html).toList()
                
                if (entries.isNotEmpty()) {
                    for (entry in entries) {
                        val block = entry.value
                        val fid = entry.groupValues[1].toLongOrNull() ?: continue
                        val isFolder = block.contains("open_dir")
                        
                        // Extract file name from <p class="file_name">...</p>
                        val nameMatch = Regex("""class="file_name">([^<]+)""").find(block)
                        val name = nameMatch?.groupValues?.get(1)?.trim() ?: "unknown"
                        
                        files.add(FebBoxFile(fid = fid, name = name, isFolder = isFolder))
                    }
                }
                
                // Fallback: simpler approach — find all data-id + data-path pairs
                if (files.isEmpty()) {
                    Log.d(TAG, "Entry regex no match, trying data-id+data-path fallback")
                    val blockPattern = Regex("""class="file\s+([^"]*)"[^>]*data-id="(\d+)"[^>]*data-path="([^"]*)"""")
                    for (match in blockPattern.findAll(html)) {
                        val classes = match.groupValues[1]
                        val fid = match.groupValues[2].toLongOrNull() ?: continue
                        val name = match.groupValues[3].trim()
                        val isFolder = classes.contains("open_dir")
                        files.add(FebBoxFile(fid = fid, name = name, isFolder = isFolder))
                    }
                }
                
                Log.d(TAG, "Parsed ${files.size} files: ${files.map { "${it.name}(folder=${it.isFolder},fid=${it.fid})" }}")
                files.toList()
            }.onFailure { Log.e(TAG, "getFileListDirect error: ${it.message}") }
        }
    }

    /**
     * Get stream URLs from FebBox using OkHttp directly.
     */
    suspend fun getStreamUrlsDirect(shareKey: String, fid: Long): Result<List<FebBoxStream>> {
        return withContext(Dispatchers.IO) {
            runCatching {
                val url = "${Constants.FEBBOX_BASE_URL}file/player/video?share_key=$shareKey&fid=$fid"
                Log.d(TAG, "FebBox stream call: $url")
                
                val request = okhttp3.Request.Builder().url(url).get().build()
                val response = ApiClient.febboxClient.newCall(request).execute()
                val html = response.body?.string() ?: throw Exception("Empty response")
                
                Log.d(TAG, "FebBox player response length: ${html.length}")
                
                // Extract sources array
                val match = Regex("""sources\s*[:=]\s*(\[[\s\S]*?\])""").find(html)
                if (match == null) {
                    Log.w(TAG, "No sources found in player. Preview: ${html.take(300)}")
                    throw Exception("No stream sources found")
                }
                
                val gson = com.google.gson.Gson()
                val sourcesJson = match.groupValues[1]
                val sources = gson.fromJson(sourcesJson, com.google.gson.JsonArray::class.java)
                
                val streams = mutableListOf<FebBoxStream>()
                for (source in sources) {
                    val obj = source.asJsonObject
                    val file = obj.get("file")?.asString ?: continue
                    val label = obj.get("label")?.asString ?: "AUTO"
                    if (!label.startsWith("audio")) {
                        streams.add(FebBoxStream(url = file, quality = label, type = "hls"))
                    }
                }
                
                Log.d(TAG, "Found ${streams.size} streams: ${streams.map { it.quality }}")
                streams.toList()
            }.onFailure { Log.e(TAG, "getStreamUrlsDirect error: ${it.message}") }
        }
    }

    /**
     * Get best stream URL (prefer 1080p > 720p > AUTO) using direct OkHttp.
     */
    suspend fun getBestStreamDirect(shareKey: String, fid: Long): Result<FebBoxStream> {
        return getStreamUrlsDirect(shareKey, fid).map { streams ->
            streams.firstOrNull { it.quality == "1080p" }
                ?: streams.firstOrNull { it.quality == "720p" }
                ?: streams.firstOrNull { it.quality == "AUTO" }
                ?: streams.firstOrNull()
                ?: throw Exception("No streams found")
        }
    }

    // ═══ Stream via ShowBox + FebBox Direct Pipeline ═══

    /**
     * Movie stream: ShowBox share_key → FebBox root files → best stream
     * Digs into subfolders if no video at root level.
     */
    suspend fun streamMovie(tmdbId: Int, title: String? = null, cachedShareKey: String? = null): Result<FebBoxStream> {
        return runCatching {
            Log.d(TAG, "streamMovie tmdbId=$tmdbId title=$title cachedKey=${cachedShareKey?.take(8)}")

            // Use cached share key, or resolve via title search, or fallback to tmdbId
            val shareKey = cachedShareKey
                ?: title?.let { getShareKeyByTitle(it, "movie").getOrNull() }
                ?: getShareKey(tmdbId, "movie").getOrThrow()
            Log.d(TAG, "Using share key: $shareKey")

            // Step 2: Get root file list from FebBox (direct OkHttp)
            val files = getFileListDirect(shareKey, 0).getOrThrow()
            Log.d(TAG, "Got ${files.size} files from FebBox root: ${files.map { "${it.name}(folder=${it.isFolder})" }}")

            // Step 3: Find video file — check root first, then dig into folders
            var videoFile = files.firstOrNull { !it.isFolder && isVideoFile(it.name) }

            if (videoFile == null) {
                // Dig into first folder
                for (folder in files.filter { it.isFolder }) {
                    Log.d(TAG, "Digging into folder: ${folder.name} (fid=${folder.fid})")
                    val subFiles = getFileListDirect(shareKey, folder.fid).getOrThrow()
                    Log.d(TAG, "Subfolder has ${subFiles.size} files: ${subFiles.map { it.name }}")
                    videoFile = subFiles.firstOrNull { !it.isFolder && isVideoFile(it.name) }
                    if (videoFile != null) break

                    // One more level deep
                    for (subFolder in subFiles.filter { it.isFolder }) {
                        val deepFiles = getFileListDirect(shareKey, subFolder.fid).getOrThrow()
                        videoFile = deepFiles.firstOrNull { !it.isFolder && isVideoFile(it.name) }
                        if (videoFile != null) break
                    }
                    if (videoFile != null) break
                }
            }

            if (videoFile == null) {
                // Last resort: try any non-folder file
                videoFile = files.firstOrNull { !it.isFolder }
                if (videoFile == null) throw Exception("No video file found (${files.size} items, all folders)")
            }

            Log.d(TAG, "Found video: ${videoFile.name} (fid=${videoFile.fid})")

            // Step 4: Get stream URLs (direct OkHttp)
            val stream = getBestStreamDirect(shareKey, videoFile.fid).getOrThrow()
            Log.d(TAG, "Got stream: ${stream.quality} - ${stream.url.take(80)}")
            stream
        }.onFailure { Log.e(TAG, "streamMovie error: ${it.message}") }
    }

    /**
     * TV episode stream: ShowBox share_key → FebBox seasons → episode file → best stream
     */
    suspend fun streamTvEpisode(tmdbId: Int, season: Int, episode: Int, title: String? = null, cachedShareKey: String? = null): Result<FebBoxStream> {
        return runCatching {
            Log.d(TAG, "streamTvEpisode tmdbId=$tmdbId S${season}E${episode} title=$title cachedKey=${cachedShareKey?.take(8)}")

            // Use cached share key, or resolve via title search, or fallback to tmdbId
            val shareKey = cachedShareKey
                ?: title?.let { getShareKeyByTitle(it, "tv").getOrNull() }
                ?: getShareKey(tmdbId, "tv").getOrThrow()
            Log.d(TAG, "Using share key: $shareKey")

            // Step 2: Get seasons from FebBox root (direct OkHttp)
            val seasons = getFileListDirect(shareKey, 0).getOrThrow().filter { it.isFolder }
            Log.d(TAG, "Got ${seasons.size} season folders")

            // Step 3: Find matching season folder
            val seasonFolder = seasons.firstOrNull { folder ->
                val num = extractEpisodeNumber(folder.name)
                num == season
            } ?: seasons.getOrNull(season - 1)
            ?: throw Exception("Season $season not found in FebBox")

            // Step 4: Get episodes from season folder (direct OkHttp)
            val episodes = getFileListDirect(shareKey, seasonFolder.fid).getOrThrow()
            Log.d(TAG, "Got ${episodes.size} episodes in season")

            // Step 5: Find matching episode file
            val epFile = episodes.firstOrNull { file ->
                extractEpisodeNumber(file.name) == episode
            } ?: episodes.getOrNull(episode - 1)
            ?: throw Exception("Episode $episode not found")

            // Step 6: Get stream URLs (direct OkHttp)
            val stream = getBestStreamDirect(shareKey, epFile.fid).getOrThrow()
            Log.d(TAG, "Got stream: ${stream.quality} - ${stream.url.take(80)}")
            stream
        }.onFailure { Log.e(TAG, "streamTvEpisode error: ${it.message}") }
    }
}
