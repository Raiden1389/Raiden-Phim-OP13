package xyz.raidenhub.phim.ui.screens.player

import xyz.raidenhub.phim.data.local.WatchHistoryManager

import android.app.Activity
import android.content.Context
import android.content.pm.ActivityInfo
import android.media.AudioManager
import android.view.View
import android.view.WindowInsetsController
import android.view.WindowManager
import android.widget.Toast
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import xyz.raidenhub.phim.data.api.models.Episode
import xyz.raidenhub.phim.data.api.models.EpisodeServer
import xyz.raidenhub.phim.data.local.SettingsManager
import xyz.raidenhub.phim.data.repository.MovieRepository
import xyz.raidenhub.phim.ui.theme.C
import xyz.raidenhub.phim.util.Constants
import xyz.raidenhub.phim.util.TextUtils

class PlayerViewModel : ViewModel() {
    private val _episodes = MutableStateFlow<List<Episode>>(emptyList())
    val episodes = _episodes.asStateFlow()
    private val _title = MutableStateFlow("")
    val title = _title.asStateFlow()
    private val _currentEp = MutableStateFlow(0)
    val currentEp = _currentEp.asStateFlow()

    // Country/Type-aware auto-next
    private val _autoNextMs = MutableStateFlow(Constants.AUTO_NEXT_BEFORE_END_MS)
    val autoNextMs = _autoNextMs.asStateFlow()
    private var _country = ""
    private var _type = ""

    fun load(slug: String, serverIdx: Int, epIdx: Int) {
        viewModelScope.launch {
            MovieRepository.getMovieDetail(slug)
                .onSuccess { result ->
                    _title.value = result.movie.name
                    val eps = result.episodes.getOrNull(serverIdx)?.serverData.orEmpty()
                    _episodes.value = eps
                    _currentEp.value = epIdx.coerceIn(0, (eps.size - 1).coerceAtLeast(0))
                    // Detect country + type for smart timing
                    _country = result.movie.country.firstOrNull()?.slug ?: ""
                    _type = result.movie.type
                    _autoNextMs.value = Constants.getAutoNextMs(_country, _type)
                }
        }
    }

    fun setEpisode(idx: Int) { _currentEp.value = idx }
    fun hasNext() = _currentEp.value < _episodes.value.size - 1
    fun nextEp() { if (hasNext()) _currentEp.value++ }
}

@OptIn(UnstableApi::class)
@Composable
fun PlayerScreen(
    slug: String,
    server: Int,
    episode: Int,
    onBack: () -> Unit,
    vm: PlayerViewModel = viewModel()
) {
    val context = LocalContext.current
    val activity = context as Activity

    // Force landscape + fullscreen
    LaunchedEffect(Unit) {
        activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        activity.window.decorView.systemUiVisibility = (
            View.SYSTEM_UI_FLAG_FULLSCREEN or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
            or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        )
    }

    DisposableEffect(Unit) {
        onDispose {
            activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            activity.window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_VISIBLE
        }
    }

    LaunchedEffect(slug) { vm.load(slug, server, episode) }

    val episodes by vm.episodes.collectAsState()
    val currentEp by vm.currentEp.collectAsState()
    val title by vm.title.collectAsState()
    val autoNextMs by vm.autoNextMs.collectAsState()

    val player = remember {
        ExoPlayer.Builder(context).build().apply {
            playWhenReady = true
        }
    }

    // Play current episode
    LaunchedEffect(currentEp, episodes) {
        if (episodes.isNotEmpty()) {
            val ep = episodes.getOrNull(currentEp) ?: return@LaunchedEffect
            val url = ep.linkM3u8
            if (url.isNotBlank()) {
                player.setMediaItem(MediaItem.fromUri(url))
                player.prepare()
            }
        }
    }

    // Auto-next: respect Settings toggle
    val autoPlayEnabled by SettingsManager.autoPlayNext.collectAsState()
    var autoNextTriggered by remember { mutableStateOf(false) }

    // Reset khi đổi tập
    LaunchedEffect(currentEp) { autoNextTriggered = false }

    // Check mỗi 3s, remaining <= autoNextMs → countdown 5s → next
    LaunchedEffect(player, currentEp, autoNextMs, autoPlayEnabled) {
        while (true) {
            delay(3000)
            if (!autoPlayEnabled || !vm.hasNext() || autoNextTriggered) continue
            val duration = player.duration
            val position = player.currentPosition
            if (duration <= 0) continue

            val remaining = duration - position
            if (remaining in 1..autoNextMs) {
                autoNextTriggered = true
                val nextEpName = episodes.getOrNull(currentEp + 1)?.name ?: "tiếp"
                // Countdown 5...4...3...2...1
                for (i in 5 downTo 1) {
                    Toast.makeText(context, "⏭ Tập $nextEpName trong ${i}s...", Toast.LENGTH_SHORT).show()
                    delay(1000)
                }
                Toast.makeText(context, "⏭ Chuyển sang Tập $nextEpName", Toast.LENGTH_SHORT).show()
                vm.nextEp()
            }
        }
    }

    // Fallback: auto-next khi hết tập hoàn toàn
    LaunchedEffect(player) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(state: Int) {
                if (state == Player.STATE_ENDED && vm.hasNext() && SettingsManager.autoPlayNext.value) {
                    vm.nextEp()
                }
            }
        }
        player.addListener(listener)
    }

    DisposableEffect(Unit) {
        onDispose {
            // Save watch progress before releasing
            val pos = player.currentPosition
            val dur = player.duration
            if (dur > 0 && pos > 0) {
                val epName = episodes.getOrNull(currentEp)?.name ?: "Tập ${currentEp + 1}"
                WatchHistoryManager.saveProgress(
                    slug = slug, name = title, thumbUrl = "", source = "",
                    server = server, episode = currentEp, epName = epName,
                    positionMs = pos, durationMs = dur
                )
            }
            player.release()
        }
    }

    var showControls by remember { mutableStateOf(true) }
    var speedIdx by remember { mutableIntStateOf(2) } // 1.0x default
    val speeds = Constants.PLAYBACK_SPEEDS
    var showSkipIntro by remember { mutableStateOf(false) }
    var isLocked by remember { mutableStateOf(false) }

    // #23 — Brightness control
    var brightness by remember {
        mutableStateOf(activity.window.attributes.screenBrightness.let {
            if (it < 0) 0.5f else it
        })
    }
    var showBrightnessIndicator by remember { mutableStateOf(false) }

    // #24 — Volume control
    val audioManager = remember { context.getSystemService(Context.AUDIO_SERVICE) as AudioManager }
    val maxVolume = remember { audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC) }
    var volume by remember { mutableStateOf(audioManager.getStreamVolume(AudioManager.STREAM_MUSIC).toFloat() / maxVolume) }
    var showVolumeIndicator by remember { mutableStateOf(false) }

    // Track position for seek bar
    var currentPos by remember { mutableStateOf(0L) }
    var duration by remember { mutableStateOf(0L) }

    // Update position every 500ms
    LaunchedEffect(player) {
        while (true) {
            currentPos = player.currentPosition.coerceAtLeast(0)
            duration = player.duration.coerceAtLeast(0)
            delay(500)
        }
    }

    // Hide controls after 4s
    LaunchedEffect(showControls) {
        if (showControls) {
            delay(4000)
            showControls = false
        }
    }

    // Show skip intro after 5s, hide after 2 min
    LaunchedEffect(currentEp) {
        showSkipIntro = false
        delay(5000)
        showSkipIntro = true
        delay(Constants.SKIP_INTRO_SHOW_UNTIL_MS - 5000)
        showSkipIntro = false
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { showControls = !showControls },
                    onDoubleTap = { offset ->
                        if (isLocked) return@detectTapGestures
                        val half = size.width / 2
                        if (offset.x < half) player.seekTo(player.currentPosition - 10000)
                        else player.seekTo(player.currentPosition + 10000)
                    }
                )
            }
            // #23 + #24 — Vertical drag: left = brightness, right = volume
            .pointerInput(isLocked) {
                if (isLocked) return@pointerInput
                detectVerticalDragGestures(
                    onDragEnd = {
                        showBrightnessIndicator = false
                        showVolumeIndicator = false
                    }
                ) { change, dragAmount ->
                    change.consume()
                    val isLeftSide = change.position.x < size.width / 2
                    val delta = -dragAmount / size.height  // up = positive

                    if (isLeftSide) {
                        // Brightness
                        brightness = (brightness + delta).coerceIn(0.01f, 1f)
                        val params = activity.window.attributes
                        params.screenBrightness = brightness
                        activity.window.attributes = params
                        showBrightnessIndicator = true
                    } else {
                        // Volume
                        volume = (volume + delta).coerceIn(0f, 1f)
                        audioManager.setStreamVolume(
                            AudioManager.STREAM_MUSIC,
                            (volume * maxVolume).toInt(),
                            0
                        )
                        showVolumeIndicator = true
                    }
                }
            }
    ) {
        // ExoPlayer view
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    this.player = player
                    useController = false
                    keepScreenOn = true
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // #23 — Brightness indicator (left side)
        if (showBrightnessIndicator) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = 24.dp)
                    .background(Color.Black.copy(0.7f), RoundedCornerShape(12.dp))
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        if (brightness > 0.5f) Icons.Default.LightMode else Icons.Default.BrightnessLow,
                        "Brightness", tint = Color.White, modifier = Modifier.size(24.dp)
                    )
                    Spacer(Modifier.height(4.dp))
                    Text("${(brightness * 100).toInt()}%", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // #24 — Volume indicator (right side)
        if (showVolumeIndicator) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 24.dp)
                    .background(Color.Black.copy(0.7f), RoundedCornerShape(12.dp))
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        if (volume > 0.5f) Icons.Default.VolumeUp
                        else if (volume > 0f) Icons.Default.VolumeDown
                        else Icons.Default.VolumeOff,
                        "Volume", tint = Color.White, modifier = Modifier.size(24.dp)
                    )
                    Spacer(Modifier.height(4.dp))
                    Text("${(volume * 100).toInt()}%", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Controls overlay
        if (showControls) {
            Box(
                modifier = Modifier.fillMaxSize().background(Color.Black.copy(0.45f))
            ) {
                if (!isLocked) {
                    // ═══ TOP BAR ═══
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                            .align(Alignment.TopStart),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Color.White)
                        }
                        // Title + episode
                        val epName = episodes.getOrNull(currentEp)?.name ?: ""
                        val displayTitle = if (epName.isNotBlank()) "$title — Tập $epName" else title
                        Text(
                            displayTitle,
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            modifier = Modifier.weight(1f).padding(horizontal = 8.dp)
                        )
                        // Speed button
                        Text(
                            "${speeds[speedIdx]}x",
                            color = if (speedIdx != 2) C.Accent else Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .background(Color.White.copy(0.15f), RoundedCornerShape(8.dp))
                                .clickable {
                                    speedIdx = (speedIdx + 1) % speeds.size
                                    player.setPlaybackSpeed(speeds[speedIdx])
                                }
                                .padding(horizontal = 10.dp, vertical = 5.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        // Lock button
                        IconButton(onClick = { isLocked = true }, modifier = Modifier.size(36.dp)) {
                            Icon(Icons.Default.Lock, "Lock", tint = Color.White.copy(0.7f), modifier = Modifier.size(20.dp))
                        }
                    }

                    // ═══ CENTER: Play/Pause ═══
                    Box(
                        modifier = Modifier.align(Alignment.Center),
                        contentAlignment = Alignment.Center
                    ) {
                        // Glow circle background
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .background(C.Primary.copy(0.2f), RoundedCornerShape(50))
                        )
                        IconButton(
                            onClick = { if (player.isPlaying) player.pause() else player.play() },
                            modifier = Modifier.size(64.dp)
                        ) {
                            Icon(
                                if (player.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                "Play/Pause",
                                tint = Color.White,
                                modifier = Modifier.size(44.dp)
                            )
                        }
                    }

                    // Prev/Next episode buttons
                    if (currentEp > 0) {
                        IconButton(
                            onClick = { vm.setEpisode(currentEp - 1) },
                            modifier = Modifier.align(Alignment.CenterStart).padding(start = 48.dp)
                        ) {
                            Icon(Icons.Default.SkipPrevious, "Prev", tint = Color.White, modifier = Modifier.size(36.dp))
                        }
                    }
                    if (vm.hasNext()) {
                        IconButton(
                            onClick = { vm.nextEp() },
                            modifier = Modifier.align(Alignment.CenterEnd).padding(end = 48.dp)
                        ) {
                            Icon(Icons.Default.SkipNext, "Next", tint = Color.White, modifier = Modifier.size(36.dp))
                        }
                    }

                    // ═══ BOTTOM: SeekBar + Time ═══
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp)
                    ) {
                        // Skip intro (above seekbar)
                        if (showSkipIntro) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
                            ) {
                                Text(
                                    "Skip Intro ⏭",
                                    color = Color.White,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier
                                        .background(C.Primary, RoundedCornerShape(8.dp))
                                        .clickable {
                                            player.seekTo(player.currentPosition + Constants.SKIP_INTRO_MS)
                                            showSkipIntro = false
                                        }
                                        .padding(horizontal = 16.dp, vertical = 8.dp)
                                )
                            }
                            Spacer(Modifier.height(4.dp))
                        }

                        // Seek bar
                        Slider(
                            value = if (duration > 0) currentPos.toFloat() / duration.toFloat() else 0f,
                            onValueChange = { fraction ->
                                val seekTo = (fraction * duration).toLong()
                                player.seekTo(seekTo)
                                currentPos = seekTo
                            },
                            colors = SliderDefaults.colors(
                                thumbColor = C.Primary,
                                activeTrackColor = C.Primary,
                                inactiveTrackColor = Color.White.copy(0.3f)
                            ),
                            modifier = Modifier.fillMaxWidth().height(20.dp)
                        )

                        // Time row
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                "${formatTime(currentPos)} / ${formatTime(duration)}",
                                color = Color.White.copy(0.8f),
                                fontSize = 12.sp
                            )
                            // Episode indicator
                            val epLabel = episodes.getOrNull(currentEp)?.name
                            if (!epLabel.isNullOrBlank()) {
                                Text(
                                    "Tập $epLabel/${episodes.size}",
                                    color = Color.White.copy(0.5f),
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                } else {
                    // ═══ LOCKED MODE ═══
                    IconButton(
                        onClick = { isLocked = false },
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .padding(start = 16.dp)
                            .background(Color.White.copy(0.15f), RoundedCornerShape(50))
                    ) {
                        Icon(Icons.Default.LockOpen, "Unlock", tint = Color.White, modifier = Modifier.size(24.dp))
                    }
                }
            }
        }
    }
}

private fun formatTime(ms: Long): String {
    val totalSec = ms / 1000
    val h = totalSec / 3600
    val m = (totalSec % 3600) / 60
    val s = totalSec % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s)
    else "%d:%02d".format(m, s)
}
