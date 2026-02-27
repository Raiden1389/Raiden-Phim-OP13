package xyz.raidenhub.phim.ui.screens.player

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.ui.AspectRatioFrameLayout
import xyz.raidenhub.phim.data.api.models.Episode
import xyz.raidenhub.phim.ui.theme.InterFamily

/**
 * PlayerBottomActions — Aspect ratio, CC, audio, episode button, skip intro.
 */
@Composable
fun PlayerBottomActions(
    aspectRatioMode: Int,
    subtitleTracks: List<TrackInfo>,
    audioTracks: List<TrackInfo>,
    episodes: List<Episode>,
    currentEp: Int,
    showSkipIntro: Boolean,
    onAspectRatioChange: (Int) -> Unit,
    onShowSubtitles: () -> Unit,
    onShowAudio: () -> Unit,
    onShowEpisodes: () -> Unit,
    onSkipIntro: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left: Aspect ratio + CC + Audio
        Row(verticalAlignment = Alignment.CenterVertically) {
            // Aspect ratio
            Surface(
                shape = RoundedCornerShape(8.dp), color = Color.White.copy(0.12f),
                modifier = Modifier.clickable {
                    onAspectRatioChange(
                        if (aspectRatioMode == AspectRatioFrameLayout.RESIZE_MODE_FIT)
                            AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                        else AspectRatioFrameLayout.RESIZE_MODE_FIT
                    )
                }
            ) {
                Icon(
                    if (aspectRatioMode == AspectRatioFrameLayout.RESIZE_MODE_FIT)
                        Icons.Default.FitScreen else Icons.Default.Fullscreen,
                    "Aspect Ratio", tint = Color.White,
                    modifier = Modifier.padding(8.dp).size(20.dp)
                )
            }
            Spacer(Modifier.width(8.dp))

            // CC button
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = if (subtitleTracks.isNotEmpty()) Color.White.copy(0.12f) else Color.White.copy(0.06f),
                modifier = Modifier.clickable { onShowSubtitles() }
            ) {
                Icon(
                    Icons.Default.ClosedCaption, "Subtitles",
                    tint = if (subtitleTracks.isNotEmpty()) Color.White else Color.White.copy(0.4f),
                    modifier = Modifier.padding(8.dp).size(20.dp)
                )
            }

            // Audio button
            if (audioTracks.size > 1) {
                Spacer(Modifier.width(8.dp))
                Surface(
                    shape = RoundedCornerShape(8.dp), color = Color.White.copy(0.12f),
                    modifier = Modifier.clickable { onShowAudio() }
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.VolumeUp, "Audio",
                        tint = Color.White, modifier = Modifier.padding(8.dp).size(20.dp)
                    )
                }
            }
        }

        Spacer(Modifier.width(12.dp))

        // Episode sheet trigger
        if (episodes.size > 1) {
            Surface(
                shape = RoundedCornerShape(8.dp), color = Color.White.copy(0.15f),
                modifier = Modifier.clickable { onShowEpisodes() }
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Icon(Icons.AutoMirrored.Filled.ViewList, "Episodes", tint = Color.White, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(
                        smartEpLabel(episodes.getOrNull(currentEp)?.name ?: "", currentEp),
                        color = Color.White, fontFamily = InterFamily,
                        fontSize = 12.sp, fontWeight = FontWeight.Medium
                    )
                }
            }
        } else {
            Spacer(Modifier.weight(1f))
        }

        // Skip Intro
        if (showSkipIntro) {
            Spacer(Modifier.width(12.dp))
            Surface(
                shape = RoundedCornerShape(20.dp), color = Color.White,
                modifier = Modifier.clickable { onSkipIntro() }
            ) {
                Text(
                    "Skip Intro", color = Color.Black,
                    fontSize = 13.sp, fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
        }
    }
}
