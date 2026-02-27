package xyz.raidenhub.phim.ui.screens.player

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import xyz.raidenhub.phim.ui.theme.InterFamily
import androidx.media3.common.C as MediaC

/**
 * Subtitle selection dialog — glassmorphism style matching TrackSelectionDialog.
 * Sections: embedded tracks + online search (delegated to PlayerOnlineSubtitles).
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
    val accentColor = Color(0xFFE50914)

    // Embedded HLS text tracks
    val textTracks = remember(player.currentTracks) {
        val tracks = mutableListOf<Triple<Int, Int, Pair<String, Boolean>>>()
        for (group in player.currentTracks.groups) {
            if (group.type == MediaC.TRACK_TYPE_TEXT) {
                for (i in 0 until group.length) {
                    val fmt = group.getTrackFormat(i)
                    val label = fmt.label ?: fmt.language?.uppercase() ?: "Sub $i"
                    val sel = group.isTrackSelected(i)
                    tracks.add(Triple(
                        player.currentTracks.groups.indexOf(group), i,
                        Pair(label, sel)
                    ))
                }
            }
        }
        tracks
    }

    // ═══ Glassmorphism overlay ═══
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.75f))
            .clickable(indication = null, interactionSource = null) { onDismiss() },
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .widthIn(min = 300.dp, max = 420.dp)
                .heightIn(max = 520.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0xFF1A1A2E))
                .clickable(enabled = false) {}
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "🔤 Phụ đề",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = Color.White,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.weight(1f, fill = false)
            ) {
                // ── Off button ──
                item {
                    SubtitleRow(
                        label = "Tắt phụ đề",
                        isSelected = textTracks.none { it.third.second },
                        accentColor = accentColor,
                        onClick = {
                            player.trackSelectionParameters = player.trackSelectionParameters
                                .buildUpon().setTrackTypeDisabled(MediaC.TRACK_TYPE_TEXT, true).build()
                            onDismiss()
                        }
                    )
                }

                // ── Embedded tracks ──
                if (textTracks.isNotEmpty()) {
                    item {
                        SectionHeader("📺 Trong video")
                    }
                    items(textTracks.size) { idx ->
                        val (gIdx, tIdx, pair) = textTracks[idx]
                        val (label, selected) = pair
                        SubtitleRow(
                            label = label,
                            isSelected = selected,
                            accentColor = accentColor,
                            onClick = {
                                val grp = player.currentTracks.groups[gIdx]
                                player.trackSelectionParameters = player.trackSelectionParameters.buildUpon()
                                    .setTrackTypeDisabled(MediaC.TRACK_TYPE_TEXT, false)
                                    .setOverrideForType(TrackSelectionOverride(grp.mediaTrackGroup, tIdx))
                                    .build()
                                onDismiss()
                            }
                        )
                    }
                }

                // ── Online search (delegated) ──
                item { SectionHeader("🌐 Online") }
                onlineSubtitleItems(
                    this@LazyColumn, player, title, source, streamType,
                    streamSeason, streamEpisode, accentColor, context, onDismiss
                )
            }
        }
    }
}

// ═══ Shared UI components ═══

@Composable
internal fun SubtitleRow(
    label: String,
    isSelected: Boolean,
    accentColor: Color,
    onClick: () -> Unit
) {
    val bgColor by animateColorAsState(
        targetValue = if (isSelected) Color.White.copy(alpha = 0.12f) else Color.Transparent,
        label = "sub_bg"
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(bgColor)
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = if (isSelected) "●" else "○",
            fontSize = 14.sp,
            color = if (isSelected) accentColor else Color.White.copy(alpha = 0.4f)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = if (isSelected) Color.White else Color.White.copy(alpha = 0.85f),
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
            maxLines = 1
        )
    }
}

@Composable
internal fun SectionHeader(text: String) {
    Text(
        text, fontWeight = FontWeight.SemiBold, fontSize = 11.sp,
        color = Color.White.copy(0.45f), fontFamily = InterFamily,
        modifier = Modifier.padding(top = 8.dp, bottom = 2.dp)
    )
}
