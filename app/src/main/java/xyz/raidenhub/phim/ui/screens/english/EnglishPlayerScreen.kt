package xyz.raidenhub.phim.ui.screens.english

import android.app.Activity
import android.content.pm.ActivityInfo
import android.net.Uri
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
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.hls.HlsMediaSource
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.ui.PlayerView
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import xyz.raidenhub.phim.data.api.models.ConsumetEpisode
import xyz.raidenhub.phim.data.api.models.SubtitleResult
import xyz.raidenhub.phim.data.repository.ConsumetRepository
import xyz.raidenhub.phim.data.repository.SubtitleRepository
import xyz.raidenhub.phim.ui.theme.C

// ═══ ViewModel ═══
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

    // Episode management
    private val _episodes = MutableStateFlow<List<ConsumetEpisode>>(emptyList())
    val episodes = _episodes.asStateFlow()
    private val _currentEpIndex = MutableStateFlow(0)
    val currentEpIndex = _currentEpIndex.asStateFlow()

    fun load(episodeId: String, mediaId: String, filmName: String = "") {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            // Fetch stream
            ConsumetRepository.getStreamLinks(episodeId, mediaId)
                .onSuccess { stream ->
                    // Save Referer header for ExoPlayer
                    _refererUrl.value = stream.headers["Referer"] ?: ""

                    // Get best source (prefer auto/highest quality M3U8)
                    val bestSource = stream.sources
                        .sortedByDescending { s ->
                            when {
                                s.quality == "auto" -> 999
                                s.quality.contains("1080") -> 100
                                s.quality.contains("720") -> 70
                                s.quality.contains("480") -> 40
                                else -> 10
                            }
                        }
                        .firstOrNull()

                    if (bestSource != null) {
                        _streamUrl.value = bestSource.url

                        // Fetch subtitles from all sources
                        val allSubs = SubtitleRepository.searchSubtitles(
                            filmName = filmName.ifBlank { _title.value },
                            consumetSubtitles = stream.subtitles
                        )
                        _subtitles.value = allSubs
                    } else {
                        _error.value = "No stream sources found"
                    }
                    _isLoading.value = false
                }
                .onFailure {
                    _error.value = it.message ?: "Failed to load stream"
                    _isLoading.value = false
                }
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

    // Force landscape + fullscreen (modern API)
    LaunchedEffect(Unit) {
        activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        val insetsController = WindowCompat.getInsetsController(activity.window, activity.window.decorView)
        insetsController.hide(WindowInsetsCompat.Type.systemBars())
        insetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
    }

    DisposableEffect(Unit) {
        onDispose {
            activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            val insetsController = WindowCompat.getInsetsController(activity.window, activity.window.decorView)
            insetsController.show(WindowInsetsCompat.Type.systemBars())
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

    // Auto-select Vietnamese subtitle when available
    LaunchedEffect(subtitles) {
        if (subtitles.isNotEmpty() && selectedSubtitleIndex == -1) {
            val viIndex = subtitles.indexOfFirst { it.language == "vi" }
            selectedSubtitleIndex = if (viIndex >= 0) viIndex else 0
        }
    }

    // ExoPlayer with Referer header support
    val player = remember(refererUrl) {
        val builder = ExoPlayer.Builder(context)
        if (refererUrl.isNotBlank()) {
            val okClient = OkHttpClient.Builder().build()
            val dataSourceFactory = OkHttpDataSource.Factory(okClient)
                .setDefaultRequestProperties(mapOf(
                    "Referer" to refererUrl,
                    "Origin" to refererUrl.substringBefore("/embed")
                ))
            builder.setMediaSourceFactory(DefaultMediaSourceFactory(dataSourceFactory))
        }
        builder.build().apply { playWhenReady = true }
    }

    DisposableEffect(Unit) {
        onDispose { player.release() }
    }

    // Set media when stream URL loaded
    LaunchedEffect(streamUrl, selectedSubtitleIndex, refererUrl) {
        if (streamUrl.isBlank()) return@LaunchedEffect

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
        val wasPlaying = player.isPlaying

        val mediaItem = MediaItem.Builder()
            .setUri(streamUrl)
            .setSubtitleConfigurations(subtitleConfigs)
            .build()

        player.setMediaItem(mediaItem)
        player.prepare()
        if (currentPos > 0) player.seekTo(currentPos)
        player.playWhenReady = wasPlaying || true
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

                // Subtitle button
                IconButton(
                    onClick = { showSubtitlePicker = !showSubtitlePicker },
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
