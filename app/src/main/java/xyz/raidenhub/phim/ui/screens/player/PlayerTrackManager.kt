package xyz.raidenhub.phim.ui.screens.player

import android.content.Context
import android.widget.Toast
import androidx.media3.common.C
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector

/**
 * TrackInfo — model for audio/subtitle track
 */
data class TrackInfo(
    val index: Int,
    val label: String,
    val language: String = "",
    val isSelected: Boolean = false
)

/**
 * Manages audio & subtitle track selection for ExoPlayer.
 * Ported from PhimTV — same logic, mobile context.
 */
@UnstableApi
class PlayerTrackManager(
    private val exoPlayer: ExoPlayer,
    private val context: Context
) {
    private val trackSelector get() = exoPlayer.trackSelector as? DefaultTrackSelector

    var subtitlesDisabled: Boolean = false
        private set

    fun scanTracks(): Pair<List<TrackInfo>, List<TrackInfo>> {
        val audioList = mutableListOf<TrackInfo>()
        val subList = mutableListOf<TrackInfo>()
        var audioIdx = 0
        var subIdx = 0

        for (group in exoPlayer.currentTracks.groups) {
            when (group.type) {
                C.TRACK_TYPE_AUDIO -> {
                    for (i in 0 until group.length) {
                        val format = group.getTrackFormat(i)
                        val lang = format.language ?: ""
                        val label = format.label
                            ?: java.util.Locale.forLanguageTag(lang.ifEmpty { "und" }).displayLanguage
                                .replaceFirstChar { it.uppercase() }
                        audioList.add(TrackInfo(
                            index = audioIdx,
                            label = label,
                            language = lang,
                            isSelected = group.isTrackSelected(i)
                        ))
                        audioIdx++
                    }
                }
                C.TRACK_TYPE_TEXT -> {
                    for (i in 0 until group.length) {
                        val format = group.getTrackFormat(i)
                        val lang = format.language ?: ""
                        val label = format.label
                            ?: java.util.Locale.forLanguageTag(lang.ifEmpty { "und" }).displayLanguage
                                .replaceFirstChar { it.uppercase() }
                        subList.add(TrackInfo(
                            index = subIdx,
                            label = label,
                            language = lang,
                            isSelected = group.isTrackSelected(i)
                        ))
                        subIdx++
                    }
                }
            }
        }
        return audioList to subList
    }

    fun selectAudio(trackIndex: Int, audioTracks: List<TrackInfo>): List<TrackInfo> {
        trackSelector?.let { selector ->
            val params = selector.buildUponParameters()
            var audioGroupIndex = 0
            for (group in exoPlayer.currentTracks.groups) {
                if (group.type == C.TRACK_TYPE_AUDIO) {
                    for (i in 0 until group.length) {
                        if (audioGroupIndex == trackIndex) {
                            params.addOverride(TrackSelectionOverride(group.mediaTrackGroup, listOf(i)))
                        }
                    }
                    audioGroupIndex++
                }
            }
            selector.setParameters(params)
            Toast.makeText(context, "🔊 ${audioTracks.getOrNull(trackIndex)?.label ?: ""}", Toast.LENGTH_SHORT).show()
        }
        return audioTracks.mapIndexed { idx, t -> t.copy(isSelected = idx == trackIndex) }
    }

    fun selectSubtitle(trackIndex: Int, subtitleTracks: List<TrackInfo>): List<TrackInfo> {
        trackSelector?.let { selector ->
            if (trackIndex == -1) {
                val params = selector.buildUponParameters()
                    .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, true)
                selector.setParameters(params)
                subtitlesDisabled = true
                Toast.makeText(context, "💬 Tắt phụ đề", Toast.LENGTH_SHORT).show()
                return subtitleTracks.map { it.copy(isSelected = false) }
            } else {
                val params = selector.buildUponParameters()
                    .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, false)
                var subGroupIndex = 0
                for (group in exoPlayer.currentTracks.groups) {
                    if (group.type == C.TRACK_TYPE_TEXT) {
                        for (i in 0 until group.length) {
                            if (subGroupIndex == trackIndex) {
                                params.addOverride(TrackSelectionOverride(group.mediaTrackGroup, listOf(i)))
                            }
                        }
                        subGroupIndex++
                    }
                }
                selector.setParameters(params)
                subtitlesDisabled = false
                Toast.makeText(context, "💬 ${subtitleTracks.getOrNull(trackIndex)?.label ?: ""}", Toast.LENGTH_SHORT).show()
                return subtitleTracks.mapIndexed { idx, t -> t.copy(isSelected = idx == trackIndex) }
            }
        }
        return subtitleTracks
    }
}
