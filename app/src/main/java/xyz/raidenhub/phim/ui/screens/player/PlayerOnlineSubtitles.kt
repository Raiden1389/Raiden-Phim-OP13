package xyz.raidenhub.phim.ui.screens.player

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import kotlinx.coroutines.launch
import xyz.raidenhub.phim.data.repository.SubtitleRepository
import androidx.media3.common.C as MediaC

/**
 * Online subtitle search items — injected into PlayerSubtitleDialog's LazyColumn.
 */
@OptIn(UnstableApi::class)
fun onlineSubtitleItems(
    listScope: LazyListScope,
    player: ExoPlayer,
    title: String,
    source: String,
    streamType: String,
    streamSeason: Int,
    streamEpisode: Int,
    accentColor: Color,
    context: android.content.Context,
    onDismiss: () -> Unit
) {
    listScope.item {
        OnlineSubtitleContent(
            player = player, title = title, source = source,
            streamType = streamType, streamSeason = streamSeason,
            streamEpisode = streamEpisode, accentColor = accentColor,
            context = context, onDismiss = onDismiss
        )
    }
}

@OptIn(UnstableApi::class)
@Composable
private fun OnlineSubtitleContent(
    player: ExoPlayer,
    title: String,
    source: String,
    streamType: String,
    streamSeason: Int,
    streamEpisode: Int,
    accentColor: Color,
    context: android.content.Context,
    onDismiss: () -> Unit
) {
    var subtitleResults by remember {
        mutableStateOf<List<xyz.raidenhub.phim.data.api.models.SubtitleResult>>(emptyList())
    }
    var isSearching by remember { mutableStateOf(true) }
    val scope = rememberCoroutineScope()

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

    if (isSearching) {
        Row(
            Modifier.fillMaxWidth().padding(12.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp, color = accentColor)
            Spacer(Modifier.width(8.dp))
            Text("Đang tìm...", fontSize = 12.sp, color = Color.White.copy(0.5f))
        }
    } else if (subtitleResults.isEmpty()) {
        Text(
            "Không tìm thấy phụ đề.",
            color = Color.White.copy(0.4f), fontSize = 12.sp,
            modifier = Modifier.padding(vertical = 8.dp)
        )
    } else {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            subtitleResults.take(30).forEach { sub ->
                OnlineSubRow(
                    sub = sub,
                    onClick = {
                        scope.launch {
                            try {
                                val subUrl = if (sub.url.endsWith(".zip")) {
                                    val sdl = xyz.raidenhub.phim.data.api.models.SubDLSubtitle(
                                        releaseName = sub.fileName,
                                        url = sub.url.removePrefix("https://dl.subdl.com"),
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
                                    player.setMediaItem(
                                        cur.buildUpon()
                                            .setSubtitleConfigurations(listOf(subCfg)).build(), pos
                                    )
                                    player.playWhenReady = wasPlaying
                                    player.prepare()
                                    player.trackSelectionParameters = player.trackSelectionParameters
                                        .buildUpon()
                                        .setTrackTypeDisabled(MediaC.TRACK_TYPE_TEXT, false)
                                        .build()
                                }
                            } catch (_: Exception) {}
                            onDismiss()
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun OnlineSubRow(
    sub: xyz.raidenhub.phim.data.api.models.SubtitleResult,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("○", fontSize = 14.sp, color = Color.White.copy(alpha = 0.4f))
        Column(modifier = Modifier.weight(1f)) {
            val epInfo = Regex("""[Ss](\d+)[Ee](\d+)""").find(sub.fileName)
                ?.let { " • S${it.groupValues[1]}E${it.groupValues[2]}" } ?: ""
            Text(
                "${sub.flag} ${sub.languageLabel}$epInfo",
                fontSize = 13.sp, maxLines = 1,
                color = Color.White.copy(0.95f),
                fontWeight = FontWeight.Medium
            )
            val releaseName = sub.fileName.take(40).let {
                if (sub.fileName.length > 40) "$it…" else it
            }
            Text(
                "${sub.source}${if (releaseName.isNotBlank()) " • $releaseName" else ""}${if (sub.downloadCount > 0) " • ${sub.downloadCount}↓" else ""}",
                fontSize = 10.sp, color = Color.White.copy(0.4f), maxLines = 1
            )
        }
    }
}
