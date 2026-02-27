package xyz.raidenhub.phim.ui.screens.player

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.MediaItem
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import kotlinx.coroutines.launch
import xyz.raidenhub.phim.data.repository.SubtitleRepository
import xyz.raidenhub.phim.ui.theme.C
import androidx.media3.common.C as MediaC

/**
 * Subtitle selection dialog — embedded tracks + online search.
 * Extracted from PlayerScreen.
 */
@OptIn(UnstableApi::class)
@Composable
fun PlayerSubtitleDialog(
    player: ExoPlayer,
    title: String,
    source: String,
    streamType: String,
    streamSeason: Int,
    streamEpisode: Int,
    onDismiss: () -> Unit,
    context: android.content.Context
) {
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
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) {
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
                        onDismiss()
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
                                onDismiss()
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
                                        onDismiss()
                                    }
                                }
                            ) {
                                Column(Modifier.padding(10.dp)) {
                                    val epInfo = Regex("""[Ss](\d+)[Ee](\d+)""").find(sub.fileName)
                                        ?.let { " • S${it.groupValues[1]}E${it.groupValues[2]}" } ?: ""
                                    Text("${sub.flag} ${sub.languageLabel}$epInfo",
                                        fontSize = 13.sp, maxLines = 1, color = Color.White.copy(0.95f),
                                        fontWeight = FontWeight.Medium)
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
