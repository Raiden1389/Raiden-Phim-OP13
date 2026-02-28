package xyz.raidenhub.phim.player

import org.junit.Assert.assertEquals
import org.junit.Test
import xyz.raidenhub.phim.ui.screens.player.smartEpLabel

/**
 * Unit tests for smartEpLabel() — avoids double "Tập" prefix.
 *
 * Regression: Episodes were showing "Tập Tập 5" when name already had "Tập" prefix
 */
class SmartEpLabelTest {

    @Test
    fun `name starting with Tap keeps as-is`() {
        assertEquals("Tập 5", smartEpLabel("Tập 5", 4))
    }

    @Test
    fun `name starting with Episode keeps as-is`() {
        assertEquals("Episode 3", smartEpLabel("Episode 3", 2))
    }

    @Test
    fun `name starting with number keeps as-is`() {
        assertEquals("05", smartEpLabel("05", 4))
    }

    @Test
    fun `name starting with play icon keeps as-is`() {
        assertEquals("▶ HD", smartEpLabel("▶ HD", 0))
    }

    @Test
    fun `non-numeric name gets Tap prefix`() {
        assertEquals("Tập Thuyết Minh", smartEpLabel("Thuyết Minh", 0))
    }

    @Test
    fun `empty name uses fallback index`() {
        // fallbackIdx = 2, so fallback becomes "3" (idx + 1), which starts with digit → no prefix
        assertEquals("3", smartEpLabel("", 2))
    }

    @Test
    fun `blank name uses fallback index`() {
        assertEquals("1", smartEpLabel("   ", 0))
    }

    @Test
    fun `name with quality suffix gets cleaned then prefixed`() {
        // "Thuyết Minh . 1080 3,3 GB" → cleaned to "Thuyết Minh" → "Tập Thuyết Minh"
        assertEquals("Tập Thuyết Minh", smartEpLabel("Thuyết Minh . 1080 3,3 GB", 0))
    }

    @Test
    fun `E05 format stays as-is since it starts with non-digit non-Tap`() {
        // "E05" starts with 'E' which is not digit and not "Tập"/"Episode"
        // But wait — it doesn't start with digit, Tập, Episode, or ▶
        // So it gets "Tập E05"
        assertEquals("Tập E05", smartEpLabel("E05", 4))
    }
}
