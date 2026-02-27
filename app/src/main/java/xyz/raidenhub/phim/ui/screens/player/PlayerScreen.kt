package xyz.raidenhub.phim.ui.screens.player

import android.app.Activity
import android.content.Context
import android.media.AudioManager
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.ui.PlayerView

/**
 * PlayerScreen — Thin wiring shell.
 *
 * Delegates to:
 * - PlayerSessionEffects (fullscreen, audio focus, save progress)
 * - PlayerSourceLoader (slug, loading, prefetch, play media)
 * - PlayerAutoNextEffects (intro/outro, auto-next, playback ended)
 * - PlayerUiState (all mutable state + timers)
 * - PlayerGestureLayer (tap, drag, swipe seek)
 * - PlayerControlsOverlay (top bar, transport, seek, bottom actions)
 * - PlayerSettingsSheet, PlayerEpisodeSheet, PlayerSubtitleDialog, TrackSelectionDialog
 */
@OptIn(UnstableApi::class, ExperimentalMaterial3Api::class)
@Composable
fun PlayerScreen(
    slug: String,
    server: Int,
    episode: Int,
    startPositionMs: Long = 0L,
    source: String = "kkphim",
    streamUrl: String = "",
    streamTitle: String = "",
    streamSeason: Int = 0,
    streamEpisode: Int = 0,
    streamType: String = "",
    tmdbId: Int = 0,
    totalEpisodes: Int = 0,
    shareKey: String = "",
    fshareEpSlug: String = "",
    onBack: () -> Unit,
    vm: PlayerViewModel = viewModel()
) {
    val context = LocalContext.current
    val activity = context as? Activity ?: return
    val audioManager = remember { context.getSystemService(Context.AUDIO_SERVICE) as AudioManager }
    val maxVolume = remember { audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC) }

    // ═══ SESSION EFFECTS ═══
    FullscreenEffect(activity)

    // ═══ EXOPLAYER ═══
    val trackSelector = remember { androidx.media3.exoplayer.trackselection.DefaultTrackSelector(context) }
    val player = remember {
        val loadControl = DefaultLoadControl.Builder()
            .setBufferDurationsMs(30_000, 120_000, 2_500, 5_000)
            .setPrioritizeTimeOverSizeThresholds(true).build()
        val httpDsf = DefaultHttpDataSource.Factory()
            .setConnectTimeoutMs(15_000).setReadTimeoutMs(20_000).setAllowCrossProtocolRedirects(true)
        ExoPlayer.Builder(context).setLoadControl(loadControl).setTrackSelector(trackSelector)
            .setMediaSourceFactory(DefaultMediaSourceFactory(DefaultDataSource.Factory(context, httpDsf)))
            .setWakeMode(android.os.PowerManager.PARTIAL_WAKE_LOCK).setHandleAudioBecomingNoisy(true)
            .setAudioAttributes(
                androidx.media3.common.AudioAttributes.Builder()
                    .setUsage(androidx.media3.common.C.USAGE_MEDIA)
                    .setContentType(androidx.media3.common.C.AUDIO_CONTENT_TYPE_MOVIE).build(),
                true
            ).build().apply { playWhenReady = true }
    }

    AudioFocusEffect(player, audioManager)

    // ═══ TRACKS ═══
    @Suppress("DEPRECATION")
    val trackManager = remember(player) { PlayerTrackManager(player, context) }
    var audioTracks by remember { mutableStateOf<List<TrackInfo>>(emptyList()) }
    var subtitleTracks by remember { mutableStateOf<List<TrackInfo>>(emptyList()) }

    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onTracksChanged(tracks: androidx.media3.common.Tracks) {
                val (audio, sub) = trackManager.scanTracks()
                audioTracks = audio; subtitleTracks = sub
            }
        }
        player.addListener(listener)
        onDispose { player.removeListener(listener) }
    }

    // ═══ SOURCE LOADING ═══
    val effectiveSlug = remember(slug, source, tmdbId, streamType) {
        computeEffectiveSlug(slug, source, tmdbId, streamType, fshareEpSlug)
    }

    SourceLoadEffect(
        slug, source, server, episode, streamUrl, streamTitle,
        tmdbId, streamSeason, streamEpisode, streamType,
        totalEpisodes, shareKey, fshareEpSlug, context, vm
    )
    PrefetchEffect(source, context, vm)

    val episodes by vm.episodes.collectAsState()
    val currentEp by vm.currentEp.collectAsState()
    val title by vm.title.collectAsState()
    val autoNextMs by vm.autoNextMs.collectAsState()
    val movieCountry by vm.country.collectAsState()

    val isFetchingEp = remember(currentEp, episodes, source) {
        (source == "superstream" || source == "fshare")
                && episodes.getOrNull(currentEp)?.linkM3u8.isNullOrBlank()
                && episodes.isNotEmpty()
    }

    PlayMediaEffect(player, episodes, currentEp, episode, startPositionMs)

    // ═══ AUTO-NEXT ═══
    var effectiveConfig by rememberEffectiveConfig(slug, movieCountry)
    AutoNextEffect(player, source, currentEp, autoNextMs, effectiveConfig, episodes, context, vm)
    PlaybackEndedEffect(player, source, vm)

    // ═══ SAVE PROGRESS ═══
    SaveProgressEffect(player, effectiveSlug, title, source, currentEp, streamEpisode, tmdbId, episodes)

    // ═══ UI STATE ═══
    val ui = rememberPlayerUiState(activity, audioManager, maxVolume, player, effectiveConfig)

    // ═══ GESTURE STATE ═══
    val gs = remember {
        GestureState(
            showControls = ui.showControls, isLocked = ui.isLocked,
            brightness = ui.brightness, showBrightnessIndicator = ui.showBrightnessIndicator,
            volume = ui.volume, showVolumeIndicator = ui.showVolumeIndicator,
            seekAnimSide = ui.seekAnimSide, seekAnimAmount = ui.seekAnimAmount,
            isHSwipeSeeking = ui.isHSwipeSeeking, hSwipeSeekPos = ui.hSwipeSeekPos,
            currentPos = ui.currentPos,
        )
    }

    // ═══ PLAYER UI ═══
    Box(
        modifier = Modifier.fillMaxSize().background(Color.Black)
            .windowInsetsPadding(WindowInsets(0))
            .then(gestureModifiers(player, activity, audioManager, maxVolume, gs))
    ) {
        // ExoPlayer view
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    this.player = player; useController = false; keepScreenOn = true
                    resizeMode = ui.aspectRatioMode.value; fitsSystemWindows = false
                    setPadding(0, 0, 0, 0); clipToPadding = false
                    setBackgroundColor(android.graphics.Color.BLACK)
                    androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(this) { _, _ ->
                        androidx.core.view.WindowInsetsCompat.CONSUMED
                    }
                }
            },
            update = { view -> view.resizeMode = ui.aspectRatioMode.value },
            modifier = Modifier.fillMaxSize().background(Color.Black)
        )

        HorizontalSwipeSeekLayer(player, ui.isLocked.value, gs)
        SeekAnimationOverlay(ui.seekAnimSide.value, ui.seekAnimAmount.value)
        SwipeSeekBillboard(ui.isHSwipeSeeking.value, ui.hSwipeSeekPos.value, ui.duration.value)

        // Controls overlay
        PlayerControlsOverlay(
            player = player, showControls = ui.showControls.value, isLocked = ui.isLocked.value,
            isFetchingEp = isFetchingEp, currentEp = currentEp, title = title,
            episodes = episodes, speedIdx = ui.speedIdx.value, speeds = ui.speeds,
            brightness = ui.brightness.value, volume = ui.volume.value,
            aspectRatioMode = ui.aspectRatioMode.value,
            currentPos = ui.currentPos.value, duration = ui.duration.value,
            showRemaining = ui.showRemaining.value, showSkipIntro = ui.showSkipIntro.value,
            effectiveConfig = effectiveConfig,
            subtitleTracks = subtitleTracks, audioTracks = audioTracks,
            showBrightnessIndicator = ui.showBrightnessIndicator.value,
            showVolumeIndicator = ui.showVolumeIndicator.value,
            isSeekbarDragging = ui.isSeekbarDragging.value,
            seekbarDragFraction = ui.seekbarDragFraction.value,
            onBack = onBack,
            onToggleLock = { ui.isLocked.value = it },
            onToggleControls = { ui.showControls.value = !ui.showControls.value },
            onSpeedChange = { ui.speedIdx.value = it },
            onAspectRatioChange = { ui.aspectRatioMode.value = it },
            onSeek = { ui.currentPos.value = it },
            onSeekbarDrag = { ui.isSeekbarDragging.value = true; ui.seekbarDragFraction.value = it },
            onSeekbarDragEnd = { ui.isSeekbarDragging.value = false },
            onToggleRemaining = { ui.showRemaining.value = !ui.showRemaining.value },
            onShowSettings = { ui.showSettingsSheet.value = true },
            onShowEpisodes = { ui.showEpisodeSheet.value = true },
            onShowSubtitles = { ui.showSubtitleDialog.value = true },
            onShowAudio = { ui.showAudioDialog.value = true },
            onPrevEp = { vm.setEpisode(currentEp - 1) },
            onNextEp = { vm.nextEp() },
            onSkipIntro = { effectiveConfig?.let { cfg -> player.seekTo(cfg.introEndMs) } },
            hasNext = vm.hasNext(),
        )
    }

    // ═══ DIALOGS ═══
    PlayerSettingsSheet(
        showSheet = ui.showSettingsSheet.value, slug = slug, movieCountry = movieCountry,
        currentPos = ui.currentPos.value, playerCurrentPosition = player.currentPosition,
        effectiveConfig = effectiveConfig,
        onConfigChanged = {
            xyz.raidenhub.phim.data.local.IntroOutroManager.getEffectiveConfig(slug, movieCountry)
                .also { effectiveConfig = it }
        },
        onDismiss = { ui.showSettingsSheet.value = false }
    )

    PlayerEpisodeSheet(
        showSheet = ui.showEpisodeSheet.value, episodes = episodes, currentEp = currentEp,
        onEpisodeSelect = { idx -> vm.setEpisode(idx); ui.showEpisodeSheet.value = false },
        onDismiss = { ui.showEpisodeSheet.value = false }
    )

    if (ui.showSubtitleDialog.value) {
        PlayerSubtitleDialog(
            player = player, title = title,
            source = source, streamType = streamType,
            streamSeason = if (source == "superstream") streamSeason else 0,
            streamEpisode = if (source == "superstream") streamEpisode else currentEp + 1,
            onDismiss = { ui.showSubtitleDialog.value = false }, context = context
        )
    }

    if (ui.showAudioDialog.value && audioTracks.size > 1) {
        TrackSelectionDialog(
            title = "🔊 Âm thanh", tracks = audioTracks,
            onSelect = { idx -> audioTracks = trackManager.selectAudio(idx, audioTracks) },
            onDismiss = { ui.showAudioDialog.value = false }
        )
    }
}
