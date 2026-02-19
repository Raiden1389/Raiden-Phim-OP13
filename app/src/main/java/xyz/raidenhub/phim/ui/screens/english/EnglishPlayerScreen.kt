package xyz.raidenhub.phim.ui.screens.english

import android.app.Activity
import android.app.PictureInPictureParams
import android.content.Context
import android.content.pm.ActivityInfo
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.net.Uri
import android.os.Build
import android.util.Log
import android.util.Rational
import android.view.WindowManager
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.PictureInPicture
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.C as MediaC
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.hls.HlsMediaSource
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import xyz.raidenhub.phim.data.api.models.ConsumetEpisode
import xyz.raidenhub.phim.data.api.models.ConsumetSource
import xyz.raidenhub.phim.data.api.models.SubtitleResult
import xyz.raidenhub.phim.data.repository.ConsumetRepository
import xyz.raidenhub.phim.data.repository.SubtitleRepository
import xyz.raidenhub.phim.data.local.WatchHistoryManager
import xyz.raidenhub.phim.ui.theme.C
import xyz.raidenhub.phim.util.SubtitleDownloader

class EnglishPlayerViewModel : ViewModel() {
    private val _streamUrl = MutableStateFlow("")
    val streamUrl = _streamUrl.asStateFlow()
    private val _refererUrl = MutableStateFlow("")
    val refererUrl = _refererUrl.asStateFlow()
    private val _subtitles = MutableStateFlow<List<SubtitleResult>>(emptyList())
    val subtitles = _subtitles.asStateFlow()
    private val _title = MutableStateFlow("")
    val title = _title.asStateFlow()
    private val _isLoading = MutableStateFlow(true)
    val isLoading = _isLoading.asStateFlow()
    private val _error = MutableStateFlow<String?>(null)
    val error = _error.asStateFlow()

    // Quality sources
    private val _allSources = MutableStateFlow<List<ConsumetSource>>(emptyList())
    val allSources = _allSources.asStateFlow()
    private val _selectedQuality = MutableStateFlow("auto")
    val selectedQuality = _selectedQuality.asStateFlow()

    // Vietsub search state
    private val _isSearchingSubs = MutableStateFlow(false)
    val isSearchingSubs = _isSearchingSubs.asStateFlow()
    private val _subSearchMessage = MutableStateFlow<String?>(null)
    val subSearchMessage = _subSearchMessage.asStateFlow()
    private var _filmName = ""
    var thumbUrl = ""
        private set

    // Episode management
    private val _episodes = MutableStateFlow<List<ConsumetEpisode>>(emptyList())
    val episodes = _episodes.asStateFlow()
    private val _currentEpIndex = MutableStateFlow(0)
    val currentEpIndex = _currentEpIndex.asStateFlow()
    var currentEpisodeId = ""
        private set
    var currentMediaId = ""
        private set

    fun load(episodeId: String, mediaId: String, filmName: String = "") {
        _filmName = filmName
        currentEpisodeId = episodeId
        currentMediaId = mediaId
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            ConsumetRepository.getStreamLinks(episodeId, mediaId)
                .onSuccess { stream ->
                    // Store ALL sources for quality picker
                    _allSources.value = stream.sources
                    val source = stream.sources.firstOrNull()
                    if (source != null) {
                        _streamUrl.value = source.url
                        _selectedQuality.value = source.quality.ifBlank { "auto" }
                        _refererUrl.value = stream.headers?.get("Referer") ?: ""
                        _title.value = filmName.ifBlank { "English Movie" }

                        // Load subtitles from stream
                        val streamSubs = stream.subtitles?.map {
                            SubtitleResult(
                                url = it.url,
                                language = it.lang?.take(2)?.lowercase() ?: "en",
                                languageLabel = it.lang ?: "Unknown",
                                source = "stream"
                            )
                        } ?: emptyList()
                        _subtitles.value = streamSubs
                    } else {
                        _error.value = "No stream sources found"
                    }
                }
                .onFailure {
                    _error.value = it.message ?: "Failed to load stream"
                }

            _isLoading.value = false
        }
    }

    fun selectQuality(quality: String) {
        val source = _allSources.value.find { it.quality == quality }
        if (source != null) {
            _streamUrl.value = source.url
            _selectedQuality.value = quality
        }
    }

    fun searchVietsub(context: Context) {
        if (_filmName.isBlank()) return
        viewModelScope.launch {
            _isSearchingSubs.value = true
            _subSearchMessage.value = "Đang tìm vietsub cho \"$_filmName\"..."

            try {
                val response = SubtitleRepository.searchSubDLDirect(_filmName)
                val viSubs = response.subtitles.filter {
                    it.lang.contains("vietnam", ignoreCase = true) ||
                    it.language.contains("vietnam", ignoreCase = true)
                }

                if (viSubs.isEmpty()) {
                    _subSearchMessage.value = "Không tìm thấy vietsub trên SubDL"
                    _isSearchingSubs.value = false
                    return@launch
                }

                _subSearchMessage.value = "Tìm thấy ${viSubs.size} vietsub, đang tải..."

                // Download + extract (take top 3 to avoid too many downloads)
                val downloaded = viSubs.take(3).mapNotNull { sub ->
                    SubtitleDownloader.downloadSubDL(context, sub)
                }

                if (downloaded.isNotEmpty()) {
                    // Add to existing subtitle list (at the top)
                    _subtitles.value = downloaded + _subtitles.value
                    _subSearchMessage.value = "✅ Đã tải ${downloaded.size} vietsub!"
                } else {
                    _subSearchMessage.value = "❌ Tải vietsub thất bại"
                }
            } catch (e: Exception) {
                _subSearchMessage.value = "❌ Lỗi: ${e.message}"
            }
            _isSearchingSubs.value = false
        }
    }

    fun setEpisodes(eps: List<ConsumetEpisode>, currentIdx: Int) {
        _episodes.value = eps
        _currentEpIndex.value = currentIdx
    }

    fun hasNext() = _currentEpIndex.value < _episodes.value.size - 1
    fun nextEp() { if (hasNext()) _currentEpIndex.value++ }
}

@OptIn(UnstableApi::class)
@Composable
fun EnglishPlayerScreen(
    episodeId: String,
    mediaId: String,
    filmName: String = "",
    onBack: () -> Unit,
    vm: EnglishPlayerViewModel = viewModel()
) {
    val context = LocalContext.current
    val activity = context as Activity

    // ═══ FULLSCREEN — Activity đã handle theme/cutout/bars ═══
    DisposableEffect(Unit) {
        activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        activity.window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        val controller = WindowCompat.getInsetsController(activity.window, activity.window.decorView)
        controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        controller.hide(WindowInsetsCompat.Type.systemBars())

        onDispose {
            activity.window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    // ═══ AUDIO FOCUS ═══
    val audioManager = remember { context.getSystemService(Context.AUDIO_SERVICE) as AudioManager }

    DisposableEffect(Unit) {
        val focusListener = AudioManager.OnAudioFocusChangeListener { focusChange ->
            // Will be connected to player below
        }
        @Suppress("DEPRECATION")
        audioManager.requestAudioFocus(
            focusListener,
            AudioManager.STREAM_MUSIC,
            AudioManager.AUDIOFOCUS_GAIN
        )
        onDispose {
            @Suppress("DEPRECATION")
            audioManager.abandonAudioFocus(focusListener)
        }
    }

    // Load stream + subtitles
    LaunchedEffect(episodeId, mediaId) {
        vm.load(episodeId, mediaId, filmName)
    }

    val streamUrl by vm.streamUrl.collectAsState()
    val refererUrl by vm.refererUrl.collectAsState()
    val subtitles by vm.subtitles.collectAsState()
    val title by vm.title.collectAsState()
    val isLoading by vm.isLoading.collectAsState()
    val error by vm.error.collectAsState()

    // Subtitle state
    var selectedSubtitleIndex by remember { mutableIntStateOf(-1) } // -1 = none
    var showSubtitlePicker by remember { mutableStateOf(false) }
    var showQualityPicker by remember { mutableStateOf(false) }
    val allSources by vm.allSources.collectAsState()
    val currentQuality by vm.selectedQuality.collectAsState()

    // Auto-select Vietnamese subtitle when available
    LaunchedEffect(subtitles) {
        if (subtitles.isNotEmpty() && selectedSubtitleIndex == -1) {
            val viIndex = subtitles.indexOfFirst { it.language == "vi" }
            selectedSubtitleIndex = if (viIndex >= 0) viIndex else 0
        }
    }

    // Single ExoPlayer instance — never recreated
    val player = remember {
        ExoPlayer.Builder(context).build().apply { playWhenReady = true }
    }

    // Audio focus → pause player on call
    DisposableEffect(player) {
        val focusListener = AudioManager.OnAudioFocusChangeListener { focusChange ->
            when (focusChange) {
                AudioManager.AUDIOFOCUS_LOSS,
                AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> player.pause()
                AudioManager.AUDIOFOCUS_GAIN -> { /* user manually resumes */ }
            }
        }
        @Suppress("DEPRECATION")
        audioManager.requestAudioFocus(
            focusListener,
            AudioManager.STREAM_MUSIC,
            AudioManager.AUDIOFOCUS_GAIN
        )
        onDispose {
            @Suppress("DEPRECATION")
            audioManager.abandonAudioFocus(focusListener)
        }
    }

    // Error listener for debugging
    DisposableEffect(Unit) {
        val errorListener = object : Player.Listener {
            override fun onPlayerError(error: PlaybackException) {
                Log.e("EnglishPlayer", "Playback error: ${error.errorCodeName} — ${error.message}", error)
                android.os.Handler(android.os.Looper.getMainLooper()).post {
                    Toast.makeText(context, "Player error: ${error.errorCodeName}", Toast.LENGTH_LONG).show()
                }
            }
        }
        player.addListener(errorListener)
        onDispose {
            // Save progress for continue watching
            val pos = player.currentPosition
            val dur = player.duration
            if (dur > 0 && pos > 0) {
                val epList = vm.episodes.value
                val epIdx = vm.currentEpIndex.value
                val epName = epList.getOrNull(epIdx)?.let { ep ->
                    if (ep.title.isNotBlank()) ep.title else "Episode ${ep.number}"
                } ?: "Episode"
                WatchHistoryManager.saveEnglishProgress(
                    mediaId = vm.currentMediaId,
                    name = title,
                    thumbUrl = vm.thumbUrl,
                    episodeId = vm.currentEpisodeId,
                    filmName = title,
                    epName = epName,
                    positionMs = pos,
                    durationMs = dur
                )
            }
            player.removeListener(errorListener)
            player.release()
        }
    }

    // Set media when stream URL loaded — wait for BOTH streamUrl AND refererUrl
    LaunchedEffect(streamUrl, refererUrl, selectedSubtitleIndex) {
        if (streamUrl.isBlank()) return@LaunchedEffect

        // Wait for refererUrl with retry loop
        var finalReferer = vm.refererUrl.value
        if (finalReferer.isBlank()) {
            repeat(10) {
                kotlinx.coroutines.delay(300)
                finalReferer = vm.refererUrl.value
                if (finalReferer.isNotBlank()) return@repeat
            }
        }

        Log.d("EnglishPlayer", "Stream: ${streamUrl.take(80)}...")
        Log.d("EnglishPlayer", "Referer: $finalReferer")

        val subtitleConfigs = if (selectedSubtitleIndex >= 0 && selectedSubtitleIndex < subtitles.size) {
            val sub = subtitles[selectedSubtitleIndex]
            if (sub.url.isNotBlank()) {
                listOf(
                    MediaItem.SubtitleConfiguration.Builder(Uri.parse(sub.url))
                        .setMimeType(
                            xyz.raidenhub.phim.util.SubtitleConverter.getMimeType(sub.url)
                        )
                        .setLanguage(sub.language)
                        .setLabel(sub.displayName)
                        .setSelectionFlags(MediaC.SELECTION_FLAG_DEFAULT)
                        .build()
                )
            } else emptyList()
        } else emptyList()

        val currentPos = player.currentPosition.takeIf { it > 0 } ?: 0L

        val mediaItem = MediaItem.Builder()
            .setUri(streamUrl)
            .setSubtitleConfigurations(subtitleConfigs)
            .build()

        val okClient = OkHttpClient.Builder().build()
        val headers = mutableMapOf<String, String>()
        if (finalReferer.isNotBlank()) {
            headers["Referer"] = finalReferer
            try {
                val refUri = Uri.parse(finalReferer)
                headers["Origin"] = "${refUri.scheme}://${refUri.host}"
            } catch (_: Exception) {}
        }
        val dataSourceFactory = OkHttpDataSource.Factory(okClient)
            .setDefaultRequestProperties(headers)
        val hlsSource = HlsMediaSource.Factory(dataSourceFactory)
            .createMediaSource(mediaItem)
        player.setMediaSource(hlsSource)

        player.prepare()
        if (currentPos > 0) player.seekTo(currentPos)
        player.playWhenReady = true
    }

    // ═══ UI ═══
    Box(
        modifier = Modifier.fillMaxSize().background(Color.Black)
    ) {
        when {
            isLoading -> {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = C.Primary
                )
            }
            error != null -> {
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("❌ $error", color = Color.White, fontSize = 16.sp)
                    Spacer(Modifier.height(12.dp))
                    Button(
                        onClick = { vm.load(episodeId, mediaId, filmName) },
                        colors = ButtonDefaults.buttonColors(containerColor = C.Primary)
                    ) {
                        Text("Retry")
                    }
                }
            }
            else -> {
                // Video player
                AndroidView(
                    factory = { ctx ->
                        PlayerView(ctx).apply {
                            this.player = player
                            useController = true
                            setShowBuffering(PlayerView.SHOW_BUFFERING_ALWAYS)
                            resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )

                // Back button overlay
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(8.dp)
                ) {
                    Icon(Icons.Default.ArrowBack, "Back", tint = Color.White)
                }

                // PiP button
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    IconButton(
                        onClick = {
                            val params = PictureInPictureParams.Builder()
                                .setAspectRatio(Rational(16, 9))
                                .build()
                            activity.enterPictureInPictureMode(params)
                        },
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(top = 8.dp, end = 96.dp)
                    ) {
                        Icon(Icons.Default.PictureInPicture, "PiP", tint = Color.White)
                    }
                }

                // Subtitle button
                IconButton(
                    onClick = { showSubtitlePicker = !showSubtitlePicker; showQualityPicker = false },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                ) {
                    Icon(
                        Icons.Default.Subtitles,
                        "Subtitles",
                        tint = if (selectedSubtitleIndex >= 0) C.Primary else Color.White
                    )
                }

                // Quality picker button
                if (allSources.size > 1) {
                    Surface(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(top = 8.dp, end = 48.dp)
                            .clickable { showQualityPicker = !showQualityPicker; showSubtitlePicker = false },
                        shape = RoundedCornerShape(6.dp),
                        color = Color.Black.copy(0.5f)
                    ) {
                        Text(
                            currentQuality.uppercase(),
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                        )
                    }
                }

                // Subtitle info badge
                if (selectedSubtitleIndex >= 0 && selectedSubtitleIndex < subtitles.size) {
                    Surface(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(top = 50.dp, end = 8.dp),
                        shape = RoundedCornerShape(12.dp),
                        color = Color.Black.copy(0.6f)
                    ) {
                        Text(
                            subtitles[selectedSubtitleIndex].let {
                                "${it.flag} ${it.languageLabel}"
                            },
                            color = Color.White,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }

        // ═══ Subtitle Picker Bottom Sheet ═══
        AnimatedVisibility(
            visible = showSubtitlePicker,
            enter = slideInVertically { it },
            exit = slideOutVertically { it },
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 300.dp),
                shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
                color = Color(0xEE1A1A2E)
            ) {
                Column {
                    // Header
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "🎬 Subtitles (${subtitles.size})",
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        TextButton(onClick = { showSubtitlePicker = false }) {
                            Text("Close", color = C.Primary)
                        }
                    }

                    // Off option
                    SubtitleOptionItem(
                        label = "🚫 Off",
                        isSelected = selectedSubtitleIndex == -1,
                        onClick = {
                            selectedSubtitleIndex = -1
                            showSubtitlePicker = false
                        }
                    )

                    // Subtitle list
                    LazyColumn {
                        items(subtitles.size) { index ->
                            val sub = subtitles[index]
                            SubtitleOptionItem(
                                label = sub.displayName,
                                isSelected = index == selectedSubtitleIndex,
                                onClick = {
                                    selectedSubtitleIndex = index
                                    showSubtitlePicker = false
                                    Toast.makeText(
                                        context,
                                        "${sub.flag} ${sub.languageLabel} selected",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            )
                        }

                        // ═══ Tìm Vietsub Button ═══
                        item {
                            val isSearchingSubs by vm.isSearchingSubs.collectAsState()
                            val subSearchMessage by vm.subSearchMessage.collectAsState()

                            Spacer(Modifier.height(8.dp))
                            HorizontalDivider(color = Color.White.copy(0.1f))
                            Spacer(Modifier.height(8.dp))

                            // Search button
                            Button(
                                onClick = { vm.searchVietsub(context) },
                                enabled = !isSearchingSubs,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF2196F3)
                                ),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                if (isSearchingSubs) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(18.dp),
                                        color = Color.White,
                                        strokeWidth = 2.dp
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text("Đang tìm...", color = Color.White)
                                } else {
                                    Text("🔍 Tìm & Tải Vietsub", color = Color.White)
                                }
                            }

                            // Status message
                            if (subSearchMessage != null) {
                                Text(
                                    subSearchMessage!!,
                                    color = Color.White.copy(0.7f),
                                    fontSize = 12.sp,
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                                )
                            }

                            Spacer(Modifier.height(8.dp))
                        }
                    }
                }
            }
        }

        // ═══ Quality Picker Bottom Sheet ═══
        AnimatedVisibility(
            visible = showQualityPicker,
            enter = slideInVertically { it },
            exit = slideOutVertically { it },
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 250.dp),
                shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
                color = Color(0xEE1A1A2E)
            ) {
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "🎞️ Quality",
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        TextButton(onClick = { showQualityPicker = false }) {
                            Text("Close", color = C.Primary)
                        }
                    }

                    LazyColumn {
                        items(allSources.size) { index ->
                            val source = allSources[index]
                            val label = source.quality.ifBlank { "auto" }
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        vm.selectQuality(source.quality)
                                        showQualityPicker = false
                                        Toast.makeText(context, "Quality: $label", Toast.LENGTH_SHORT).show()
                                    }
                                    .padding(horizontal = 16.dp, vertical = 2.dp),
                                shape = RoundedCornerShape(8.dp),
                                color = if (currentQuality == source.quality) C.Primary.copy(0.2f) else Color.Transparent
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        label,
                                        color = if (currentQuality == source.quality) C.Primary else Color.White,
                                        fontSize = 14.sp,
                                        fontWeight = if (currentQuality == source.quality) FontWeight.Bold else FontWeight.Normal,
                                        modifier = Modifier.weight(1f)
                                    )
                                    if (currentQuality == source.quality) {
                                        Text("✓", color = C.Primary, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SubtitleOptionItem(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 2.dp),
        shape = RoundedCornerShape(8.dp),
        color = if (isSelected) C.Primary.copy(0.2f) else Color.Transparent
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                label,
                color = if (isSelected) C.Primary else Color.White,
                fontSize = 14.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                modifier = Modifier.weight(1f)
            )
            if (isSelected) {
                Text("✓", color = C.Primary, fontWeight = FontWeight.Bold)
            }
        }
    }
}
