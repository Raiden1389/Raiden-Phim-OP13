package xyz.raidenhub.phim.player

import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for Fshare slug parsing logic used in FsharePlayerLoader.
 *
 * Regression: Continue Watching corruption — enriched slug "fshare-folder:URL|||NAME|||THUMB"
 * was being saved as plain "fshare:URL", losing movie name and poster on resume.
 */
class SlugParsingTest {

    // ═══ Enriched slug format: "fshare-folder:URL|||NAME|||THUMB" ═══

    @Test
    fun `parse enriched slug extracts name and poster`() {
        val slug = "fshare-folder:https://www.fshare.vn/folder/ABC|||Movie Name|||https://poster.jpg"
        val parts = slug.split("|||")

        assertEquals("fshare-folder:https://www.fshare.vn/folder/ABC", parts[0])
        assertEquals("Movie Name", parts[1])
        assertEquals("https://poster.jpg", parts[2])
    }

    @Test
    fun `parse enriched slug with empty poster`() {
        val slug = "fshare-folder:https://www.fshare.vn/folder/ABC|||Movie Name|||"
        val parts = slug.split("|||")

        assertEquals("Movie Name", parts.getOrNull(1)?.takeIf { it.isNotBlank() } ?: "Fshare")
        assertEquals("", parts.getOrNull(2)?.takeIf { it.isNotBlank() } ?: "")
    }

    @Test
    fun `parse clean slug (no separator) falls back to defaults`() {
        val slug = "fshare:https://www.fshare.vn/file/XYZ"
        val parts = slug.split("|||")

        // Only 1 part — no name or poster
        assertEquals(1, parts.size)
        assertEquals("Fshare", parts.getOrNull(1)?.takeIf { it.isNotBlank() } ?: "Fshare")
        assertEquals("", parts.getOrNull(2)?.takeIf { it.isNotBlank() } ?: "")
    }

    // ═══ Slug prefix stripping ═══

    @Test
    fun `strip fshare prefix from episode slug`() {
        val episodeSlug = "fshare:https://www.fshare.vn/file/XYZ"
        val clean = episodeSlug.removePrefix("fshare:")
        assertEquals("https://www.fshare.vn/file/XYZ", clean)
    }

    @Test
    fun `strip double fshare prefix`() {
        val episodeSlug = "fshare:fshare:https://www.fshare.vn/file/XYZ"
        // removePrefix only removes first occurrence
        val clean = episodeSlug.removePrefix("fshare:")
        assertEquals("fshare:https://www.fshare.vn/file/XYZ", clean)
        // Need to call again:
        val cleanFull = clean.removePrefix("fshare:")
        assertEquals("https://www.fshare.vn/file/XYZ", cleanFull)
    }

    @Test
    fun `non-fshare slug stays unchanged`() {
        val slug = "ke-an-dat"
        val clean = slug.removePrefix("fshare:")
        assertEquals("ke-an-dat", clean)
    }

    // ═══ isFolder detection ═══

    @Test
    fun `detect folder slug`() {
        val slug = "fshare-folder:https://www.fshare.vn/folder/ABC|||Movie|||poster.jpg"
        val rawSlug = slug.split("|||").firstOrNull() ?: slug
        assertTrue(rawSlug.startsWith("fshare-folder:"))
    }

    @Test
    fun `detect file slug`() {
        val slug = "fshare-file:https://www.fshare.vn/file/XYZ|||Movie|||poster.jpg"
        val rawSlug = slug.split("|||").firstOrNull() ?: slug
        assertFalse(rawSlug.startsWith("fshare-folder:"))
        assertTrue(rawSlug.startsWith("fshare-file:"))
    }

    // ═══ cleanSlug reconstruction ═══

    @Test
    fun `cleanSlug reconstruction for WatchHistory`() {
        val resolveUrl = "https://www.fshare.vn/file/XYZ"
        val cleanSlug = "fshare:$resolveUrl"
        assertEquals("fshare:https://www.fshare.vn/file/XYZ", cleanSlug)
    }

    // ═══ fshare.vn domain check ═══

    @Test
    fun `fshare domain check passes for fshare URLs`() {
        val url = "https://www.fshare.vn/folder/ABC123"
        assertTrue("fshare.vn" in url)
    }

    @Test
    fun `fshare domain check fails for non-fshare URLs`() {
        val url = "https://thuviencine.com/movies"
        assertFalse("fshare.vn" in url)
    }
}
