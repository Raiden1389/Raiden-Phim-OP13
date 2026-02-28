package xyz.raidenhub.phim.player

import org.junit.Assert.assertEquals
import org.junit.Test
import xyz.raidenhub.phim.ui.screens.player.cleanEpName

/**
 * Unit tests for cleanEpName() — strips quality/size suffixes from Fshare episode names.
 *
 * Regression: Continue Watching was showing "Tập 5 . 1080 3,3 GB" instead of "Tập 5"
 */
class CleanEpNameTest {

    @Test
    fun `strip quality and size suffix`() {
        assertEquals("Tập 5", cleanEpName("Tập 5 . 1080 3,3 GB"))
    }

    @Test
    fun `strip 720p variant`() {
        assertEquals("Tập 3", cleanEpName("Tập 3 . 720 1,2 GB"))
    }

    @Test
    fun `strip 1080p with p suffix`() {
        assertEquals("Episode 1", cleanEpName("Episode 1 . 1080p 4,5 GB"))
    }

    @Test
    fun `strip mkv extension`() {
        assertEquals("E05", cleanEpName("E05.mkv"))
    }

    @Test
    fun `strip mp4 extension`() {
        assertEquals("Movie.2024.BluRay.x264", cleanEpName("Movie.2024.BluRay.x264.mp4"))
    }

    @Test
    fun `strip avi extension`() {
        assertEquals("video", cleanEpName("video.avi"))
    }

    @Test
    fun `keep subfolder prefix while stripping quality`() {
        assertEquals("[Season 1] Tập 3", cleanEpName("[Season 1] Tập 3 . 720 1,2 GB"))
    }

    @Test
    fun `empty input returns empty`() {
        assertEquals("", cleanEpName(""))
    }

    @Test
    fun `name without quality stays unchanged`() {
        assertEquals("Tập 10", cleanEpName("Tập 10"))
    }

    @Test
    fun `complex fshare format`() {
        assertEquals("Tập 12", cleanEpName("Tập 12 . 1080 6,8 GB"))
    }
}
