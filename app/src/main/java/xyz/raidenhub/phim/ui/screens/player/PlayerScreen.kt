package xyz.raidenhub.phim.ui.screens.player

import xyz.raidenhub.phim.data.local.WatchHistoryManager
import xyz.raidenhub.phim.data.local.IntroOutroManager

import android.app.Activity
import android.app.PictureInPictureParams
import android.content.Context
import android.content.pm.ActivityInfo
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Build
import android.util.Rational
import android.view.WindowManager
import android.widget.Toast
import androidx.annotation.OptIn
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import xyz.raidenhub.phim.data.local.SettingsManager
import xyz.raidenhub.phim.ui.theme.C
import xyz.raidenhub.phim.ui.theme.JakartaFamily
import xyz.raidenhub.phim.ui.theme.InterFamily
import xyz.raidenhub.phim.util.Constants
import androidx.media3.common.C as MediaC
import androidx.media3.common.TrackSelectionOverride
import xyz.raidenhub.phim.data.repository.SubtitleRepository

@OptIn(UnstableApi::class, ExperimentalMaterial3Api::class)
@Composable
fun PlayerScreen(
    slug: String,
    server: Int,
    episode: Int,
    startPositionMs: Long = 0L,
    source: String = "kkphim",          // "kkphim" | "superstream"
    streamUrl: String = "",             // SuperStream: direct m3u8 URL
    streamTitle: String = "",           // SuperStream: video title
    streamSeason: Int = 0,             // SuperStream: season number (for sub search)
    streamEpisode: Int = 0,            // SuperStream: episode number (for sub search)
    streamType: String = "",           // SuperStream: "movie" or "tv"
    tmdbId: Int = 0,                   // SuperStream: TMDB ID for fetching next episode
    totalEpisodes: Int = 0,            // SuperStream: total episodes in season
    shareKey: String = "",             // SuperStream: FebBox share key for next episode
    onBack: () -> Unit,
    vm: PlayerViewModel = viewModel()
) {
    val context = LocalContext.current
    val activity = context as Activity
    val haptic = LocalHapticFeedback.current

    // ═══ FULLSCREEN — Activity đã handle theme/cutout/bars ═══
    // PlayerScreen chỉ cần keep-screen-on + orientation
    DisposableEffect(Unit) {
        activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_FULL_SENSOR
        activity.window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        // Re-hide bars khi composable mount (phòng trường hợp bị show lại)
        val controller = WindowCompat.getInsetsController(activity.window, activity.window.decorView)
        controller.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        controller.hide(WindowInsetsCompat.Type.systemBars())

        onDispose {
            activity.window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    // ═══ AUDIO FOCUS ═══
    val audioManager = remember { context.getSystemService(Context.AUDIO_SERVICE) as AudioManager }
    val maxVolume = remember { audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC) }

    DisposableEffect(Unit) {
        val focusRequest = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MOVIE)
                        .build()
                )
                .setOnAudioFocusChangeListener { focusChange ->
                    when (focusChange) {
                        AudioManager.AUDIOFOCUS_LOSS,
                        AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                            // Pause when phone call or other app takes focus
                        }
                        AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                            // Lower volume temporarily
                        }
                        AudioManager.AUDIOFOCUS_GAIN -> {
                            // Resume — user can manually play
                        }
                    }
                }
                .build()
        } else null

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && focusRequest != null) {
            audioManager.requestAudioFocus(focusRequest)
        }

        onDispose {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && focusRequest != null) {
                audioManager.abandonAudioFocusRequest(focusRequest)
            }
        }
    }

    // Effective slug for SuperStream (slug is empty for SS content)
    val effectiveSlug = remember(slug, source, tmdbId, streamType) {
        if (source == "superstream" && slug.isBlank()) "ss_${streamType}_${tmdbId}"
        else slug
    }

    // ═══ LOAD — branch theo source ═══
    LaunchedEffect(slug, source) {
        if (source == "superstream" && streamUrl.isNotBlank()) {
            vm.loadSuperStream(streamUrl, streamTitle, tmdbId, streamSeason, streamEpisode, streamType, totalEpisodes, shareKey)
        } else {
            vm.load(slug, server, episode)
        }
    }

    // Pre-fetch stream khi chuyển tập (Anime47 + SuperStream)
    val episodesForPrefetch by vm.episodes.collectAsState()
    val currentEpIdx by vm.currentEp.collectAsState()
    
    LaunchedEffect(episodesForPrefetch, currentEpIdx) {
        // 1. Kiểm tra tập HIỆN TẠI (nếu chưa có link thì phải lấy ngay)
        val curEp = episodesForPrefetch.getOrNull(currentEpIdx)
        if (curEp != null && curEp.linkM3u8.isBlank()) {
            if (source == "superstream" && curEp.slug.startsWith("ss::")) {
                val parts = curEp.slug.split("::")
                if (parts.size == 4) {
                    val s = parts[2].toIntOrNull() ?: return@LaunchedEffect
                    val e = parts[3].toIntOrNull() ?: return@LaunchedEffect
                    vm.fetchSuperStreamEp(s, e)
                }
            }
        }

        // 2. Kiểm tra tập KẾ TIẾP (prefetch)
        val nextIdx = currentEpIdx + 1
        val nextEp = episodesForPrefetch.getOrNull(nextIdx)
        if (nextEp != null && nextEp.linkM3u8.isBlank()) {
            if (source == "superstream" && nextEp.slug.startsWith("ss::")) {
                val parts = nextEp.slug.split("::")
                if (parts.size == 4) {
                    val s = parts[2].toIntOrNull() ?: return@LaunchedEffect
                    val e = parts[3].toIntOrNull() ?: return@LaunchedEffect
                    vm.fetchSuperStreamEp(s, e)
                }
            }
        }
    }

    val episodes by vm.episodes.collectAsState()
    val currentEp by vm.currentEp.collectAsState()
    val title by vm.title.collectAsState()
    val autoNextMs by vm.autoNextMs.collectAsState()
    val movieCountry by vm.country.collectAsState()

    val player = remember {
        // ═══ Anti-rebuffer config ═══
        // Vấn đề: Video dừng chờ do ExoPlayer default buffer quá nhỏ (15s/50MB)
        // Fix: Tăng buffer 30s min → 120s max, retry 5 lần khi HTTP fail
        val loadControl = DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                /* minBufferMs    = */ 30_000,   // buffer ít nhất 30s trước khi play
                /* maxBufferMs    = */ 120_000,  // buffer tối đa 2 phút
                /* bufferForPlaybackMs = */ 2_500,  // bắt đầu play khi có 2.5s
                /* bufferForPlaybackAfterRebufferMs = */ 5_000  // sau stall, cần 5s mới chạy lại
            )
            .setPrioritizeTimeOverSizeThresholds(true)  // ưu tiên time-based buffer
            .build()

        val httpDataSourceFactory = DefaultHttpDataSource.Factory()
            .setConnectTimeoutMs(15_000)
            .setReadTimeoutMs(20_000)
            .setAllowCrossProtocolRedirects(true)  // cho phép http→https redirect

        val dataSourceFactory = DefaultDataSource.Factory(context, httpDataSourceFactory)

        ExoPlayer.Builder(context)
            .setLoadControl(loadControl)
            .setMediaSourceFactory(DefaultMediaSourceFactory(dataSourceFactory))
            .setWakeMode(android.os.PowerManager.PARTIAL_WAKE_LOCK)  // TV fix: giữ CPU+WiFi khi buffer, không bị power saving ngắt
            .setHandleAudioBecomingNoisy(true)  // tự pause khi rút tai nghe/HDMI
            .build()
            .apply {
                playWhenReady = true
            }
    }

    // Play current episode — seek to saved position only on initial episode load
    var hasSeekOnce by remember { mutableStateOf(false) }
    var lastLoadedUrl by remember { mutableStateOf("") }

    // SuperStream: ep URL đang được fetch (placeholder rỗng) → show loading
    val isFetchingEp = remember(currentEp, episodes, source) {
        source == "superstream" &&
        episodes.getOrNull(currentEp)?.linkM3u8.isNullOrBlank() &&
        episodes.isNotEmpty()
    }

    LaunchedEffect(currentEp, episodes) {
        if (episodes.isNotEmpty()) {
            val ep = episodes.getOrNull(currentEp) ?: return@LaunchedEffect
            val url = ep.linkM3u8
            if (url.isNotBlank() && url != lastLoadedUrl) {
                player.setMediaItem(MediaItem.fromUri(url))
                player.prepare()
                // Seek to saved position only on first episode load
                if (!hasSeekOnce && startPositionMs > 0L && currentEp == episode) {
                    player.seekTo(startPositionMs)
                    hasSeekOnce = true
                }
                player.playWhenReady = true
                player.play()
                lastLoadedUrl = url
            }
        }
    }

    // ═══ INTRO/OUTRO CONFIG (3-level hierarchy: series → country → null) ═══
    // P3: Initial load via LaunchedEffect (suspend fun)
    var effectiveConfig by remember { mutableStateOf<IntroOutroManager.SeriesConfig?>(null) }
    LaunchedEffect(slug, movieCountry) {
        effectiveConfig = IntroOutroManager.getEffectiveConfig(slug, movieCountry)
    }
    // Promote dialog state
    var showPromoteDialog by remember { mutableStateOf(false) }

    // ═══ AUTO-NEXT (mark-based + country fallback) ═══
    val autoPlayEnabled by SettingsManager.autoPlayNext.collectAsState()
    var autoNextTriggered by remember { mutableStateOf(false) }
    LaunchedEffect(currentEp) { autoNextTriggered = false }

    LaunchedEffect(player, currentEp, autoNextMs, autoPlayEnabled, effectiveConfig) {
        var prefetchTriggered = false  // chỉ fetch 1 lần/tập
        while (true) {
            delay(3000)
            val dur = player.duration
            val pos = player.currentPosition
            if (dur <= 0) continue

            // ─── SuperStream near-end prefetch ───
            // Trigger khi còn < 3 phút HOẶC < 15% thời lượng còn lại
            if (source == "superstream" && vm.hasNext() && !prefetchTriggered) {
                val timeRemaining = dur - pos
                val percentRemaining = timeRemaining.toFloat() / dur
                if (timeRemaining < 3 * 60 * 1000L || percentRemaining < 0.15f) {
                    val nextIdx = currentEp + 1
                    val nextEpItem = episodes.getOrNull(nextIdx)
                    if (nextEpItem != null && nextEpItem.linkM3u8.isBlank()
                        && nextEpItem.slug.startsWith("ss::")) {
                        val parts = nextEpItem.slug.split("::")
                        if (parts.size == 4) {
                            val s = parts[2].toIntOrNull()
                            val e = parts[3].toIntOrNull()
                            if (s != null && e != null) {
                                vm.fetchSuperStreamEp(s, e)
                                prefetchTriggered = true
                            }
                        }
                    } else {
                        prefetchTriggered = true  // URL đã có, đánh dấu luôn
                    }
                }
            }

            // ─── Auto-next logic (chỉ cho VN source, không áp cho SuperStream) ───
            if (!autoPlayEnabled || !vm.hasNext() || autoNextTriggered) continue
            if (source == "superstream") continue  // SS: user tự next hoặc tập kết thúc tự nhiên

            val shouldNext = if (effectiveConfig?.hasOutro == true) {
                pos >= effectiveConfig!!.outroStartMs
            } else {
                val remaining = dur - pos
                remaining in 1..autoNextMs
            }

            if (shouldNext) {
                autoNextTriggered = true
                val nextEpName = episodes.getOrNull(currentEp + 1)?.name ?: "tiếp"
                for (i in 5 downTo 1) {
                    Toast.makeText(context, "⏭ Tập $nextEpName trong ${i}s...", Toast.LENGTH_SHORT).show()
                    delay(1000)
                }
                Toast.makeText(context, "⏭ Chuyển sang Tập $nextEpName", Toast.LENGTH_SHORT).show()
                vm.nextEp()
            }
        }
    }

    // Fallback: auto-next khi hết tập hoàn toàn (chỉ cho VN, không cho SuperStream)
    LaunchedEffect(player) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(state: Int) {
                if (state == Player.STATE_ENDED && vm.hasNext()
                    && SettingsManager.autoPlayNext.value
                    && source != "superstream") {
                    vm.nextEp()
                }
            }
        }
        player.addListener(listener)
    }

    // ═══ Audio Focus Listener — pause/resume player ═══
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

    // Save watch progress on dispose
    DisposableEffect(Unit) {
        onDispose {
            val pos = player.currentPosition
            val dur = player.duration
            if (dur > 0 && pos > 0) {
                // SuperStream: episode index = streamEpisode - 1 (0-based)
                val saveEpIdx = if (source == "superstream") (streamEpisode - 1).coerceAtLeast(0) + currentEp else currentEp
                val epName = episodes.getOrNull(currentEp)?.name ?: "Tập ${saveEpIdx + 1}"
                WatchHistoryManager.updateContinue(
                    slug = effectiveSlug, name = title, thumbUrl = "", source = source,
                    episodeIdx = saveEpIdx, episodeName = epName,
                    positionMs = pos, durationMs = dur
                )
                // Watched Episodes fix: mark SuperStream episode as watched khi xem ≥ 70%
                if (source == "superstream" && tmdbId > 0) {
                    val progress = pos.toFloat() / dur
                    if (progress >= 0.70f) {
                        WatchHistoryManager.markWatched("ss_tv_$tmdbId", saveEpIdx)
                    }
                }
            }
            player.release()
        }
    }

    // ═══ UI STATES ═══
    val scope = rememberCoroutineScope()  // P3: for suspend IntroOutroManager calls
    var showControls by remember { mutableStateOf(true) }
    var speedIdx by remember { mutableIntStateOf(2) } // 1.0x default
    val speeds = Constants.PLAYBACK_SPEEDS
    var isLocked by remember { mutableStateOf(false) }
    var showSettingsSheet by remember { mutableStateOf(false) }
    var showEpisodeSheet by remember { mutableStateOf(false) } // B-7: Episode Bottom Sheet
    var showSubtitleDialog by remember { mutableStateOf(false) }

    // Brightness
    var brightness by remember {
        mutableStateOf(activity.window.attributes.screenBrightness.let {
            if (it < 0) 0.5f else it
        })
    }
    var showBrightnessIndicator by remember { mutableStateOf(false) }

    // Volume
    var volume by remember { mutableStateOf(audioManager.getStreamVolume(AudioManager.STREAM_MUSIC).toFloat() / maxVolume) }
    var showVolumeIndicator by remember { mutableStateOf(false) }

    // Seek animation (OTT double tap)
    var seekAnimSide by remember { mutableIntStateOf(0) } // -1 left, 1 right, 0 none
    var seekAnimAmount by remember { mutableIntStateOf(0) }

    // Aspect ratio toggle
    var aspectRatioMode by remember { mutableIntStateOf(AspectRatioFrameLayout.RESIZE_MODE_FIT) }

    // Track position for seek bar
    var currentPos by remember { mutableStateOf(0L) }
    var duration by remember { mutableStateOf(0L) }

    // PL-4: Remaining time toggle (tap time display)
    var showRemaining by remember { mutableStateOf(false) }
    // PL-3: Horizontal swipe seek
    var isHSwipeSeeking by remember { mutableStateOf(false) }
    var hSwipeSeekPos by remember { mutableStateOf(0L) }
    // PL-1 Opt C: Seekbar drag tooltip
    var isSeekbarDragging by remember { mutableStateOf(false) }
    var seekbarDragFraction by remember { mutableStateOf(0f) }

    // Skip Intro: derive from mark config + current position
    val showSkipIntro by remember {
        derivedStateOf {
            val cfg = effectiveConfig ?: return@derivedStateOf false
            if (!cfg.hasIntro && cfg.introEndMs <= 0) return@derivedStateOf false
            val introStart = if (cfg.introStartMs >= 0) cfg.introStartMs else 0L
            val introEnd = cfg.introEndMs
            currentPos in introStart..introEnd
        }
    }

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

    // Auto hide seek animation after 700ms
    LaunchedEffect(seekAnimSide, seekAnimAmount) {
        if (seekAnimSide != 0) {
            delay(700)
            seekAnimSide = 0
            seekAnimAmount = 0
        }
    }


    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .windowInsetsPadding(WindowInsets(0)) // ngăn Compose tự add insets padding
            // ═══ TAP + DOUBLE TAP (3-zone) ═══
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { showControls = !showControls },
                    onDoubleTap = { offset ->
                        if (isLocked) return@detectTapGestures
                        val third = size.width / 3

                        when {
                            // Left third → seek back
                            offset.x < third -> {
                                player.seekTo((player.currentPosition - 10000).coerceAtLeast(0))
                                seekAnimSide = -1
                                seekAnimAmount += 10
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            }
                            // Right third → seek forward
                            offset.x > size.width - third -> {
                                player.seekTo((player.currentPosition + 10000).coerceAtMost(player.duration))
                                seekAnimSide = 1
                                seekAnimAmount += 10
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            }
                            // Center → play/pause
                            else -> {
                                if (player.isPlaying) player.pause() else player.play()
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            }
                        }
                    }
                )
            }
            // ═══ Vertical drag: left = brightness, right = volume ═══
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
                    val delta = -dragAmount / size.height

                    if (isLeftSide) {
                        brightness = (brightness + delta).coerceIn(0.01f, 1f)
                        val params = activity.window.attributes
                        params.screenBrightness = brightness
                        activity.window.attributes = params
                        showBrightnessIndicator = true
                    } else {
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
        // ═══ ExoPlayer view ═══
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    this.player = player
                    useController = false
                    keepScreenOn = true
                    resizeMode = aspectRatioMode
                    fitsSystemWindows = false
                    setPadding(0, 0, 0, 0)
                    clipToPadding = false
                    setBackgroundColor(android.graphics.Color.BLACK)
                    // Consume hết insets — không để PlayerView tự handle
                    androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(this) { _, _ ->
                        androidx.core.view.WindowInsetsCompat.CONSUMED
                    }
                }
            },
            update = { view ->
                view.resizeMode = aspectRatioMode
            },
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        )

        // ═══ PL-3: Horizontal swipe seek — invisible overlay ═══
        if (!isLocked) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectHorizontalDragGestures(
                            onDragStart = {
                                isHSwipeSeeking = true
                                hSwipeSeekPos = player.currentPosition
                                showControls = true
                            },
                            onDragEnd = {
                                player.seekTo(hSwipeSeekPos)
                                currentPos = hSwipeSeekPos
                                isHSwipeSeeking = false
                            },
                            onDragCancel = { isHSwipeSeeking = false }
                        ) { change, delta ->
                            change.consume()
                            val seekMs = (delta * 200).toLong()
                            hSwipeSeekPos = (hSwipeSeekPos + seekMs)
                                .coerceIn(0L, player.duration.coerceAtLeast(0L))
                        }
                    }
            )
        }

        // ═══ SEEK ANIMATION OVERLAY (OTT style) ═══
        AnimatedVisibility(
            visible = seekAnimSide != 0,
            modifier = Modifier.align(
                if (seekAnimSide == -1) Alignment.CenterStart else Alignment.CenterEnd
            ),
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Box(
                modifier = Modifier
                    .padding(48.dp)
                    .background(Color.Black.copy(0.6f), RoundedCornerShape(16.dp))
                    .padding(horizontal = 20.dp, vertical = 14.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        if (seekAnimSide == -1) "⏪" else "⏩",
                        fontSize = 28.sp
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "${seekAnimAmount}s",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // PL-3: Horizontal swipe seek billboard
        AnimatedVisibility(
            visible = isHSwipeSeeking,
            enter = fadeIn(tween(80)),
            exit = fadeOut(tween(200)),
            modifier = Modifier.align(Alignment.Center)
        ) {
            Box(
                modifier = Modifier
                    .background(Color.Black.copy(0.78f), RoundedCornerShape(14.dp))
                    .padding(horizontal = 28.dp, vertical = 14.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "↔  ${formatTime(hSwipeSeekPos)}",
                        color = Color.White,
                        fontFamily = JakartaFamily,
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold
                    )
                    if (duration > 0) {
                        Text(
                            "/ ${formatTime(duration)}",
                            color = Color.White.copy(0.55f),
                            fontFamily = InterFamily,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }

        // ═══ CONTROLS OVERLAY (OTT Premium) ═══
        if (showControls) {
            Box(
                modifier = Modifier.fillMaxSize().background(Color.Black.copy(0.5f))
            ) {
                // B-5: Top gradient scrim
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                        .align(Alignment.TopCenter)
                        .background(
                            Brush.verticalGradient(
                                listOf(Color.Black.copy(0.7f), Color.Transparent)
                            )
                        )
                )
                // B-5: Bottom gradient scrim
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .align(Alignment.BottomCenter)
                        .background(
                            Brush.verticalGradient(
                                listOf(Color.Transparent, Color.Black.copy(0.8f))
                            )
                        )
                )
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
                        val epName = episodes.getOrNull(currentEp)?.name ?: ""
                        val displayTitle = if (epName.isNotBlank()) "$title — Tập $epName" else title
                        Text(
                            displayTitle,
                            color = Color.White,
                            fontFamily = JakartaFamily,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            modifier = Modifier.weight(1f).padding(horizontal = 8.dp)
                        )

                        // Speed pill
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = Color.White.copy(0.15f),
                            modifier = Modifier.clickable {
                                speedIdx = (speedIdx + 1) % speeds.size
                                player.setPlaybackSpeed(speeds[speedIdx])
                            }
                        ) {
                            Text(
                                "${speeds[speedIdx]}x",
                                color = if (speedIdx != 2) C.Accent else Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                        Spacer(Modifier.width(8.dp))

                        // PiP button
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            IconButton(
                                onClick = {
                                    val params = PictureInPictureParams.Builder()
                                        .setAspectRatio(Rational(16, 9))
                                        .build()
                                    activity.enterPictureInPictureMode(params)
                                },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(Icons.Default.PictureInPicture, "PiP", tint = Color.White, modifier = Modifier.size(22.dp))
                            }
                        }

                        // Lock button
                        IconButton(onClick = { isLocked = true }, modifier = Modifier.size(36.dp)) {
                            Icon(Icons.Default.Lock, "Lock", tint = Color.White, modifier = Modifier.size(22.dp))
                        }

                        // Gear icon (settings — mark intro/outro)
                        IconButton(
                            onClick = { showSettingsSheet = true },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                Icons.Default.Settings,
                                "Settings",
                                tint = if (effectiveConfig != null) C.Accent else Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    // ═══ LEFT: Brightness Vertical Slider ═══
                    Column(
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .padding(start = 16.dp)
                            .width(36.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            if (brightness > 0.5f) Icons.Default.LightMode else Icons.Default.BrightnessLow,
                            "Brightness",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.height(8.dp))
                        // Vertical slider via rotated Slider
                        Box(
                            modifier = Modifier
                                .height(120.dp)
                                .width(28.dp)
                                .background(Color.White.copy(0.1f), RoundedCornerShape(14.dp)),
                            contentAlignment = Alignment.BottomCenter
                        ) {
                            // Filled portion
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .fillMaxHeight(brightness)
                                    .background(Color.White.copy(0.6f), RoundedCornerShape(14.dp))
                                    .align(Alignment.BottomCenter)
                            )
                            // Thumb indicator
                            Box(
                                modifier = Modifier
                                    .offset(y = -(brightness * 112).dp)
                                    .size(14.dp)
                                    .background(Color.White, RoundedCornerShape(50))
                                    .align(Alignment.BottomCenter)
                            )
                        }
                    }

                    // ═══ RIGHT: Volume Vertical Slider ═══
                    Column(
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .padding(end = 16.dp)
                            .width(36.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.VolumeUp,
                            "Volume",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.height(8.dp))
                        Box(
                            modifier = Modifier
                                .height(120.dp)
                                .width(28.dp)
                                .background(Color.White.copy(0.1f), RoundedCornerShape(14.dp)),
                            contentAlignment = Alignment.BottomCenter
                        ) {
                            // Filled portion (red)
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .fillMaxHeight(volume)
                                    .background(C.Primary.copy(0.8f), RoundedCornerShape(14.dp))
                                    .align(Alignment.BottomCenter)
                            )
                            // Red thumb indicator
                            Box(
                                modifier = Modifier
                                    .offset(y = -(volume * 112).dp)
                                    .size(14.dp)
                                    .background(C.Primary, RoundedCornerShape(50))
                                    .align(Alignment.BottomCenter)
                            )
                        }
                    }

                    // ═══ CENTER: Play/Pause hoặc Loading (SuperStream fetch) ═══
                    Box(
                        modifier = Modifier.align(Alignment.Center),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isFetchingEp) {
                            // SuperStream đang fetch URL tập tiếp theo
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                CircularProgressIndicator(color = C.Primary, modifier = Modifier.size(48.dp), strokeWidth = 3.dp)
                                Spacer(Modifier.height(8.dp))
                                Text("⏳ Đang tải tập...", color = Color.White, fontSize = 13.sp)
                            }
                        } else {
                        // Skip previous
                        if (currentEp > 0) {
                            IconButton(
                                onClick = { vm.setEpisode(currentEp - 1) },
                                modifier = Modifier
                                    .align(Alignment.CenterStart)
                                    .offset(x = (-80).dp)
                            ) {
                                Icon(Icons.Default.SkipPrevious, "Prev", tint = Color.White, modifier = Modifier.size(36.dp))
                            }
                        }

                        // Main play/pause button with red gradient
                        Box(
                            modifier = Modifier
                                .size(76.dp)
                                .background(
                                    brush = Brush.radialGradient(
                                        colors = listOf(
                                            C.Primary.copy(0.7f),
                                            C.PrimaryDark.copy(0.4f),
                                            Color.Transparent
                                        ),
                                        radius = 120f
                                    ),
                                    shape = RoundedCornerShape(50)
                                )
                                .clickable {
                                    if (player.isPlaying) player.pause() else player.play()
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            // Inner circle
                            Box(
                                modifier = Modifier
                                    .size(62.dp)
                                    .background(C.Primary.copy(0.35f), RoundedCornerShape(50)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    if (player.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    "Play/Pause",
                                    tint = Color.White,
                                    modifier = Modifier.size(44.dp)
                                )
                            }
                        }

                        // Skip next
                        if (vm.hasNext()) {
                            IconButton(
                                onClick = { vm.nextEp() },
                                modifier = Modifier
                                    .align(Alignment.CenterEnd)
                                    .offset(x = 80.dp)
                            ) {
                                Icon(Icons.Default.SkipNext, "Next", tint = Color.White, modifier = Modifier.size(36.dp))
                            }
                        } // end SkipNext if
                        } // end else
                    } // end outer center Box

                    // ═══ BOTTOM SECTION ═══
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp)
                    ) {
                        // PL-1 Opt C: Seek tooltip (shows target time above slider while dragging)
                        AnimatedVisibility(
                            visible = isSeekbarDragging && duration > 0,
                            enter = fadeIn(tween(80)),
                            exit = fadeOut(tween(150))
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 2.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    formatTime((seekbarDragFraction * duration).toLong()),
                                    color = Color.White,
                                    fontFamily = JakartaFamily,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier
                                        .background(C.Primary.copy(0.88f), RoundedCornerShape(8.dp))
                                        .padding(horizontal = 12.dp, vertical = 3.dp)
                                )
                            }
                        }

                        // ═══ Red Seekbar ═══
                        Slider(
                            value = if (duration > 0) currentPos.toFloat() / duration.toFloat() else 0f,
                            onValueChange = { fraction ->
                                isSeekbarDragging = true
                                seekbarDragFraction = fraction
                                val seekTo = (fraction * duration).toLong()
                                player.seekTo(seekTo)
                                currentPos = seekTo
                            },
                            onValueChangeFinished = { isSeekbarDragging = false },
                            colors = SliderDefaults.colors(
                                thumbColor = C.Primary,
                                activeTrackColor = C.Primary,
                                inactiveTrackColor = Color.White.copy(0.25f)
                            ),
                            modifier = Modifier.fillMaxWidth().height(24.dp)
                        )

                        // PL-4: Time display — tap để toggle elapsed ↔ remaining
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(bottom = 2.dp),
                            horizontalArrangement = Arrangement.End
                        ) {
                            Text(
                                if (showRemaining && duration > 0)
                                    "-${formatTime(duration - currentPos)}"
                                else
                                    "${formatTime(currentPos)} / ${formatTime(duration)}",
                                color = Color.White.copy(0.8f),
                                fontFamily = InterFamily,
                                fontSize = 12.sp,
                                modifier = Modifier
                                    .background(Color.White.copy(0.08f), RoundedCornerShape(4.dp))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                                    .clickable { showRemaining = !showRemaining }
                            )
                        }

                        // ═══ Bottom Controls Row ═══
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Left: Aspect ratio + CC
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                // Aspect ratio button (square icon)
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color.White.copy(0.12f),
                                    modifier = Modifier.clickable {
                                        aspectRatioMode = if (aspectRatioMode == AspectRatioFrameLayout.RESIZE_MODE_FIT) {
                                            AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                                        } else {
                                            AspectRatioFrameLayout.RESIZE_MODE_FIT
                                        }
                                    }
                                ) {
                                    Icon(
                                        if (aspectRatioMode == AspectRatioFrameLayout.RESIZE_MODE_FIT)
                                            Icons.Default.FitScreen
                                        else
                                            Icons.Default.Fullscreen,
                                        "Aspect Ratio",
                                        tint = Color.White,
                                        modifier = Modifier.padding(8.dp).size(20.dp)
                                    )
                                }
                                Spacer(Modifier.width(8.dp))
                                // CC (subtitle) button
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color.White.copy(0.12f),
                                    modifier = Modifier.clickable {
                                        showSubtitleDialog = true
                                    }
                                ) {
                                    Icon(
                                        Icons.Default.ClosedCaption,
                                        "Subtitles",
                                        tint = Color.White,
                                        modifier = Modifier.padding(8.dp).size(20.dp)
                                    )
                                }
                            }

                            Spacer(Modifier.width(12.dp))

                            // Center: Episode sheet trigger button
                            if (episodes.size > 1) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color.White.copy(0.15f),
                                    modifier = Modifier.clickable {
                                        showEpisodeSheet = true
                                    }
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                                    ) {
                                        Icon(
                                            Icons.AutoMirrored.Filled.ViewList,
                                            "Episodes",
                                            tint = Color.White,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(Modifier.width(6.dp))
                                        Text(
                                            "Tập ${(episodes.getOrNull(currentEp)?.name ?: "${currentEp + 1}")}",
                                            color = Color.White,
                                            fontFamily = InterFamily,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }
                            } else {
                                Spacer(Modifier.weight(1f))
                            }

                            // Right: Skip Intro pill
                            if (showSkipIntro) {
                                Spacer(Modifier.width(12.dp))
                                Surface(
                                    shape = RoundedCornerShape(20.dp),
                                    color = Color.White,
                                    modifier = Modifier.clickable {
                                        effectiveConfig?.let { cfg ->
                                            player.seekTo(cfg.introEndMs)
                                        }
                                    }
                                ) {
                                    Text(
                                        "Skip Intro",
                                        color = Color.Black,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                                    )
                                }
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

        // ═══ Brightness indicator (when dragging) ═══
        if (showBrightnessIndicator && !showControls) {
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

        // ═══ Volume indicator (when dragging) ═══
        if (showVolumeIndicator && !showControls) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 24.dp)
                    .background(Color.Black.copy(0.7f), RoundedCornerShape(12.dp))
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.AutoMirrored.Filled.VolumeUp,
                        "Volume", tint = Color.White, modifier = Modifier.size(24.dp)
                    )
                    Spacer(Modifier.height(4.dp))
                    Text("${(volume * 100).toInt()}%", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    // ═══ SETTINGS BOTTOM SHEET (Mark Intro/Outro) ═══
    // P3: LaunchedEffect-driven state for suspend IntroOutroManager calls
    var settingsHasOverride by remember { mutableStateOf(false) }
    var settingsCountryDefault by remember { mutableStateOf<IntroOutroManager.SeriesConfig?>(null) }
    LaunchedEffect(showSettingsSheet, movieCountry) {
        if (showSettingsSheet) {
            settingsHasOverride = IntroOutroManager.hasSeriesOverride(slug)
            settingsCountryDefault = IntroOutroManager.getCountryDefault(movieCountry)
        }
    }

    if (showSettingsSheet) {
        val countryName = IntroOutroManager.getCountryDisplayName(movieCountry)
        val countryDefault = settingsCountryDefault
        val hasOverride = settingsHasOverride

        ModalBottomSheet(
            onDismissRequest = { showSettingsSheet = false },
            containerColor = C.Surface,
            contentColor = Color.White
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp)
                    .padding(bottom = 32.dp)
            ) {
                Text(
                    "⚙️ Player Settings",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "Đánh dấu intro/outro • $countryName",
                    fontSize = 13.sp,
                    color = C.TextSecondary
                )
                Spacer(Modifier.height(16.dp))

                // Config status display
                val cfg = effectiveConfig
                if (cfg != null) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = C.SurfaceVariant
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            val sourceLabel = if (hasOverride) "📌 Config riêng (series)" else "⭐ Mặc định $countryName"
                            Text(sourceLabel, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = C.Accent)
                            Spacer(Modifier.height(4.dp))
                            if (cfg.introStartMs >= 0) {
                                Text("   Intro Start: ${formatTime(cfg.introStartMs)}", fontSize = 12.sp, color = C.TextSecondary)
                            }
                            if (cfg.introEndMs > 0) {
                                Text("   Intro End: ${formatTime(cfg.introEndMs)}", fontSize = 12.sp, color = C.TextSecondary)
                            }
                            if (cfg.outroStartMs > 0) {
                                Text("   Outro Start: ${formatTime(cfg.outroStartMs)}", fontSize = 12.sp, color = C.TextSecondary)
                            }
                        }
                    }
                    Spacer(Modifier.height(6.dp))

                    // Show country default info if exists and not using it
                    if (hasOverride && countryDefault != null) {
                        Text(
                            "   ↳ Mặc định $countryName: Intro ${formatTime(countryDefault.introEndMs)}, Outro ${formatTime(countryDefault.outroStartMs)}",
                            fontSize = 11.sp,
                            color = C.TextMuted
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                } else {
                    Text("❌ Chưa có config", fontSize = 12.sp, color = C.TextMuted)
                    if (countryDefault != null) {
                        Text(
                            "   ⭐ Mặc định $countryName có sẵn",
                            fontSize = 11.sp,
                            color = C.Accent
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                }

                // Current position
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = C.Primary.copy(0.15f)
                ) {
                    Text(
                        "⏱ Vị trí hiện tại: ${formatTime(currentPos)}",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = C.Primary,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                    )
                }
                Spacer(Modifier.height(16.dp))

                // Mark buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            IntroOutroManager.saveIntroStart(slug, player.currentPosition)
                            scope.launch { effectiveConfig = IntroOutroManager.getEffectiveConfig(slug, movieCountry) }
                            Toast.makeText(context, "✅ Intro Start: ${formatTime(player.currentPosition)}", Toast.LENGTH_SHORT).show()
                            showPromoteDialog = true
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                    ) {
                        Text("📌 Intro\nStart", fontSize = 11.sp, lineHeight = 14.sp)
                    }

                    Button(
                        onClick = {
                            IntroOutroManager.saveIntroEnd(slug, player.currentPosition)
                            scope.launch { effectiveConfig = IntroOutroManager.getEffectiveConfig(slug, movieCountry) }
                            Toast.makeText(context, "✅ Intro End: ${formatTime(player.currentPosition)}", Toast.LENGTH_SHORT).show()
                            showPromoteDialog = true
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = C.Primary)
                    ) {
                        Text("📌 Intro\nEnd", fontSize = 11.sp, lineHeight = 14.sp)
                    }

                    Button(
                        onClick = {
                            IntroOutroManager.saveOutroStart(slug, player.currentPosition)
                            scope.launch { effectiveConfig = IntroOutroManager.getEffectiveConfig(slug, movieCountry) }
                            Toast.makeText(context, "✅ Outro Start: ${formatTime(player.currentPosition)}", Toast.LENGTH_SHORT).show()
                            showPromoteDialog = true
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = C.Accent)
                    ) {
                        Text("📌 Outro\nStart", fontSize = 11.sp, lineHeight = 14.sp, color = Color.Black)
                    }
                }

                Spacer(Modifier.height(12.dp))

                // Reset buttons
                if (hasOverride) {
                    TextButton(
                        onClick = {
                            IntroOutroManager.resetConfig(slug)
                            scope.launch { effectiveConfig = IntroOutroManager.getEffectiveConfig(slug, movieCountry) }
                            Toast.makeText(context, "🗑 Đã xoá config riêng → dùng mặc định $countryName", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Delete, "Reset", tint = C.Error, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Xoá config riêng (series)", color = C.Error, fontSize = 13.sp)
                    }
                }
                if (countryDefault != null) {
                    TextButton(
                        onClick = {
                            IntroOutroManager.resetCountryDefault(movieCountry)
                            scope.launch { effectiveConfig = IntroOutroManager.getEffectiveConfig(slug, movieCountry) }
                            Toast.makeText(context, "🗑 Đã xoá mặc định $countryName", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Delete, "Reset Country", tint = C.TextMuted, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Xoá mặc định $countryName", color = C.TextMuted, fontSize = 13.sp)
                    }
                }
            }
        }
    }

    // ═══ PROMOTE DIALOG (Mark xong → hỏi dùng cho tất cả?) ═══
    if (showPromoteDialog && movieCountry.isNotBlank()) {
        val countryName = IntroOutroManager.getCountryDisplayName(movieCountry)
        AlertDialog(
            onDismissRequest = { showPromoteDialog = false },
            containerColor = C.Surface,
            title = {
                Text("🌏 Áp dụng cho tất cả phim $countryName?", fontSize = 16.sp, color = Color.White)
            },
            text = {
                Text(
                    "Config vừa mark sẽ được dùng làm mặc định cho tất cả phim $countryName chưa có config riêng.",
                    fontSize = 13.sp,
                    color = C.TextSecondary
                )
            },
            dismissButton = {
                TextButton(onClick = { showPromoteDialog = false }) {
                    Text("Chỉ series này", color = C.TextSecondary)
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        IntroOutroManager.promoteToCountryDefault(slug, movieCountry)
                        scope.launch { effectiveConfig = IntroOutroManager.getEffectiveConfig(slug, movieCountry) }
                        Toast.makeText(context, "⭐ Đã đặt mặc định cho phim $countryName", Toast.LENGTH_SHORT).show()
                        showPromoteDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = C.Primary)
                ) {
                    Text("✅ Tất cả phim $countryName")
                }
            }
        )
    }

    // ═══ B-7: Episode Bottom Sheet ═══
    if (showEpisodeSheet && episodes.size > 1) {
        ModalBottomSheet(
            onDismissRequest = { showEpisodeSheet = false },
            containerColor = Color(0xFF1A1A2E),
            contentColor = Color.White,
            tonalElevation = 0.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 32.dp)
            ) {
                // Header
                Text(
                    "📋 Danh sách tập (${episodes.size})",
                    fontFamily = JakartaFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = Color.White,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                // Episode grid
                androidx.compose.foundation.lazy.grid.LazyVerticalGrid(
                    columns = androidx.compose.foundation.lazy.grid.GridCells.Adaptive(minSize = 72.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.heightIn(max = 400.dp)
                ) {
                    items(episodes.size) { idx ->
                        val isCurrentEp = idx == currentEp
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (isCurrentEp) C.Primary else Color.White.copy(0.1f),
                            border = if (isCurrentEp)
                                androidx.compose.foundation.BorderStroke(2.dp, C.Primary)
                            else null,
                            modifier = Modifier
                                .height(44.dp)
                                .clickable {
                                    vm.setEpisode(idx)
                                    showEpisodeSheet = false
                                }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    episodes[idx].name,
                                    color = if (isCurrentEp) Color.White else Color.White.copy(0.8f),
                                    fontFamily = InterFamily,
                                    fontSize = 13.sp,
                                    fontWeight = if (isCurrentEp) FontWeight.Bold else FontWeight.Normal,
                                    textAlign = TextAlign.Center,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // ═══ SUBTITLE SELECTION DIALOG ═══
    if (showSubtitleDialog) {
        var subtitleResults by remember { mutableStateOf<List<xyz.raidenhub.phim.data.api.models.SubtitleResult>>(emptyList()) }
        var isSearching by remember { mutableStateOf(true) }
        val scope = rememberCoroutineScope()

        // Search subtitles on dialog open
        LaunchedEffect(Unit) {
            val searchTitle = title.ifBlank { "Unknown" }
            try {
                subtitleResults = SubtitleRepository.searchSubtitles(
                    filmName = searchTitle,
                    type = streamType.ifBlank { null },
                    season = streamSeason.takeIf { it > 0 },
                    episode = streamEpisode.takeIf { it > 0 },
                    languages = if (source == "superstream") "en" else "vi,en"
                )
            } catch (_: Exception) {}
            isSearching = false
        }

        // Embedded HLS text tracks
        val textTracks = remember(player.currentTracks) {
            val tracks = mutableListOf<Triple<Int, Int, String>>()
            for (group in player.currentTracks.groups) {
                if (group.type == MediaC.TRACK_TYPE_TEXT) {
                    for (i in 0 until group.length) {
                        val fmt = group.getTrackFormat(i)
                        val label = fmt.label ?: fmt.language?.uppercase() ?: "Sub $i"
                        val sel = group.isTrackSelected(i)
                        tracks.add(Triple(player.currentTracks.groups.indexOf(group), i,
                            if (sel) "✅ $label" else label))
                    }
                }
            }
            tracks
        }

        AlertDialog(
            onDismissRequest = { showSubtitleDialog = false },
            confirmButton = {
                TextButton(onClick = { showSubtitleDialog = false }) {
                    Text("Đóng", color = C.Primary)
                }
            },
            title = { Text("🔤 Phụ đề", fontWeight = FontWeight.Bold) },
            text = {
                Column(modifier = Modifier.heightIn(max = 400.dp)) {
                    // Off button
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color.White.copy(0.08f),
                        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp).clickable {
                            player.trackSelectionParameters = player.trackSelectionParameters
                                .buildUpon().setTrackTypeDisabled(MediaC.TRACK_TYPE_TEXT, true).build()
                            showSubtitleDialog = false
                        }
                    ) { Text("❌ Tắt phụ đề", Modifier.padding(10.dp), fontSize = 13.sp) }

                    // Embedded tracks
                    if (textTracks.isNotEmpty()) {
                        Text("📺 Trong video", fontWeight = FontWeight.SemiBold, fontSize = 12.sp,
                            color = Color.Gray, modifier = Modifier.padding(top = 6.dp, bottom = 2.dp))
                        textTracks.forEach { (gIdx, tIdx, lbl) ->
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (lbl.startsWith("✅")) C.Primary.copy(0.2f) else Color.White.copy(0.08f),
                                modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp).clickable {
                                    val grp = player.currentTracks.groups[gIdx]
                                    player.trackSelectionParameters = player.trackSelectionParameters.buildUpon()
                                        .setTrackTypeDisabled(MediaC.TRACK_TYPE_TEXT, false)
                                        .setOverrideForType(TrackSelectionOverride(grp.mediaTrackGroup, tIdx))
                                        .build()
                                    showSubtitleDialog = false
                                }
                            ) { Text(lbl, Modifier.padding(10.dp), fontSize = 13.sp) }
                        }
                    }

                    // Online search
                    Text("🌐 Online", fontWeight = FontWeight.SemiBold, fontSize = 12.sp,
                        color = Color.Gray, modifier = Modifier.padding(top = 6.dp, bottom = 2.dp))

                    if (isSearching) {
                        Row(Modifier.fillMaxWidth().padding(12.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp, color = C.Primary)
                            Spacer(Modifier.width(8.dp))
                            Text("Đang tìm...", fontSize = 13.sp, color = Color.Gray)
                        }
                    } else if (subtitleResults.isEmpty()) {
                        Text("Không tìm thấy phụ đề.", color = Color.Gray, fontSize = 13.sp)
                    } else {
                        androidx.compose.foundation.lazy.LazyColumn(Modifier.heightIn(max = 260.dp)) {
                            items(subtitleResults.size.coerceAtMost(30)) { idx ->
                                val sub = subtitleResults[idx]
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color.White.copy(0.08f),
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp).clickable {
                                        scope.launch {
                                            try {
                                                // For SubDL zip files → use SubtitleDownloader
                                                val subUrl = if (sub.url.endsWith(".zip")) {
                                                    val sdl = xyz.raidenhub.phim.data.api.models.SubDLSubtitle(
                                                        releaseName = sub.fileName, url = sub.url.removePrefix("https://dl.subdl.com"),
                                                        lang = sub.language, language = sub.languageLabel
                                                    )
                                                    xyz.raidenhub.phim.util.SubtitleDownloader
                                                        .downloadSubDL(context, sdl)?.url ?: sub.url
                                                } else sub.url

                                                val subUri = android.net.Uri.parse(subUrl)
                                                val mime = xyz.raidenhub.phim.util.SubtitleConverter.getMimeType(subUrl)
                                                val subCfg = MediaItem.SubtitleConfiguration.Builder(subUri)
                                                    .setMimeType(mime)
                                                    .setLanguage(sub.language)
                                                    .setLabel(sub.languageLabel)
                                                    .setSelectionFlags(MediaC.SELECTION_FLAG_DEFAULT)
                                                    .build()
                                                val pos = player.currentPosition
                                                val wasPlaying = player.playWhenReady
                                                val cur = player.currentMediaItem
                                                if (cur != null) {
                                                    player.setMediaItem(cur.buildUpon()
                                                        .setSubtitleConfigurations(listOf(subCfg)).build(), pos)
                                                    player.playWhenReady = wasPlaying
                                                    player.prepare()
                                                    player.trackSelectionParameters = player.trackSelectionParameters
                                                        .buildUpon().setTrackTypeDisabled(MediaC.TRACK_TYPE_TEXT, false).build()
                                                }
                                            } catch (_: Exception) {}
                                            showSubtitleDialog = false
                                        }
                                    }
                                ) {
                                    Column(Modifier.padding(10.dp)) {
                                        // Primary: flag + language + episode info from fileName
                                        val epInfo = Regex("""[Ss](\d+)[Ee](\d+)""").find(sub.fileName)
                                            ?.let { " • S${it.groupValues[1]}E${it.groupValues[2]}" } ?: ""
                                        Text("${sub.flag} ${sub.languageLabel}$epInfo",
                                            fontSize = 13.sp, maxLines = 1, color = Color.White.copy(0.95f),
                                            fontWeight = FontWeight.Medium)
                                        // Secondary: source + release name (truncated)
                                        val releaseName = sub.fileName.take(35).let { if (sub.fileName.length > 35) "$it…" else it }
                                        Text("${sub.source}${if (releaseName.isNotBlank()) " • $releaseName" else ""}${if (sub.downloadCount > 0) " • ${sub.downloadCount}↓" else ""}",
                                            fontSize = 10.sp, color = Color.Gray, maxLines = 1)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        )
    }
}
