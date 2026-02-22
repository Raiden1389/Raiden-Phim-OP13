package xyz.raidenhub.phim.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Flexible key-value settings store.
 * Keys defined in SettingsKeys.kt constants.
 */
@Entity(tableName = "settings")
data class SettingEntity(
    @PrimaryKey val key: String,
    val value: String
)

object SettingKeys {
    const val DEFAULT_QUALITY     = "default_quality"       // "auto" | "1080p" | "720p" | "480p"
    const val AUTO_PLAY_NEXT      = "auto_play_next"         // "true" | "false"
    const val AUTO_NEXT_QUALITY   = "auto_next_quality"      // "true" | "false"
    const val NOTIFICATION_ENABLED = "notification_enabled"  // "true" | "false"
    const val SUBTITLE_FONT_SIZE  = "subtitle_font_size"     // "14" | "18" | "22"
    const val SUBTITLE_COLOR      = "subtitle_color"         // hex "#FFFFFF"
    const val SUBTITLE_BG_OPACITY = "subtitle_bg_opacity"    // "0.0" … "1.0"
    const val SUBTITLE_POSITION_Y = "subtitle_position_y"    // "0.0" … "1.0" (normalized offset)
}
