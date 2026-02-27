package xyz.raidenhub.phim.data.repository

import android.util.Log
import com.google.gson.Gson
import com.google.gson.JsonObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URL

/**
 * CommunityRepository — reads VietMediaF community Google Sheets.
 *
 * Flow:
 *   1. Master sheet → list of community sources (name + sub-sheet URL + thumbnail)
 *   2. Source sheet → list of movies (name + fshare link + thumbnail + info)
 *   3. Fshare link → folder or file → play
 */
class CommunityRepository {

    companion object {
        private const val TAG = "CommunityRepo"

        // Master community sheet from VietMediaF
        private const val COMMUNITY_SHEET_ID = "1yCyQ1ZqIaeEkh5TYiXqPkTkRtrlbWkc6mL5jA2s6VqM"
        private const val COMMUNITY_GID = "0"

    }

    // ═══ Data Models ═══

    /** A community source (person who shared their collection) */
    data class CommunitySource(
        val name: String,
        val sheetUrl: String,       // Google Sheet URL or Fshare folder URL
        val thumbnailUrl: String = "",
        val description: String = ""
    ) {
        val isFshareFolder: Boolean get() = "fshare.vn/folder" in sheetUrl
        val isGoogleSheet: Boolean get() = "docs.google.com" in sheetUrl
    }

    /** A movie entry from a community source */
    data class CommunityMovie(
        val name: String,
        val link: String,           // fshare.vn/file/xxx or fshare.vn/folder/xxx or sub-sheet
        val thumbnailUrl: String = "",
        val description: String = "",
        val fanartUrl: String = "",
        val genre: String = "",
        val rating: Float = 0f
    ) {
        val isFshareFolder: Boolean get() = "fshare.vn/folder" in link
        val isFshareFile: Boolean get() = "fshare.vn/file" in link
        val isGoogleSheet: Boolean get() = "docs.google.com" in link
        val isPlayable: Boolean get() = !isFshareFolder && !isGoogleSheet
    }

    // ═══ API ═══

    /**
     * Curated community sources — verified alive 2026-02-27.
     * Dead sources removed: Meo Meo (410), HieuIT (401), Datcine (410),
     * Quý Phạm (401), sharehdxx (403), IPTV (irrelevant)
     */
    suspend fun getCommunityList(): List<CommunitySource> = listOf(
        CommunitySource("Zinzuno", "https://docs.google.com/spreadsheets/d/1S6iSi0tWvqKVk5en2NDx9N5XeDquwOWh4GlGKIEezyo/edit#gid=482627435"),
        CommunitySource("MrBenHien", "https://docs.google.com/spreadsheets/d/1-5Ou_oDhtHxaXLQSWGVpAFQS1C-dgTUgnNWNwx66ZFs/edit?usp=sharing"),
        CommunitySource("Canodinh", "https://docs.google.com/spreadsheets/d/1ETAFpPH71Y5kKO5JScvrDIJNu2M252f7ofd_2Qcy6aA/edit?gid=0#gid=0"),
        CommunitySource("Linh Huynh", "https://docs.google.com/spreadsheets/d/1-2rcg28cil-Hlw0gpvLp7J-CVhLu3rHGhukjvNfBSZI/edit?usp=sharing"),
        CommunitySource("THB", "https://docs.google.com/spreadsheets/d/1S6iSi0tWvqKVk5en2NDx9N5XeDquwOWh4GlGKIEezyo/edit#gid=1573953762"),
        CommunitySource("Kamenrider1997", "https://docs.google.com/spreadsheets/d/1i2_cgLiSmyY3q1RB4axBid_4jM29HBw3C7rZI9QK2J8/edit#gid=0"),
        CommunitySource("Kphung", "https://docs.google.com/spreadsheets/d/1tvD0F6l7Vm7LI9SDFDrnqKYg0G6cgppyxywAGR59C0o/edit?usp=sharing"),
        CommunitySource("Huỳnh Phước Pháp", "https://docs.google.com/spreadsheets/d/1uI8ZwdS_WbSdOLGORPsFhQUgHyUBZbXeGryjTqt4Yyo/edit#gid=0"),
        CommunitySource("Sontho22", "https://docs.google.com/spreadsheets/d/1WX7r75gIW8-sX72x5uzXLtam0Wruz9bMqtp5xcVPptI/edit?usp=sharing"),
        CommunitySource("Melodies of Life", "https://docs.google.com/spreadsheets/d/1QfG84of1a2OcUoIhFfPugXudyhwiRH3F-g2MLhaPjos/edit?usp=sharing"),
        CommunitySource("Tùng Bùi", "https://docs.google.com/spreadsheets/d/1MUofoMzCbElPAv0oFmruPzm6IXKoBWWcCwoDaLtLPGo/edit?gid=0#gid=0"),
        CommunitySource("Phim 4K 2024", "https://www.fshare.vn/folder/M6QUUDZLQPZL", description = "Kho phim 4K Ultra HD chọn lọc"),
    )

    /** Get movies from a specific community source sheet */
    suspend fun getSourceMovies(sheetUrl: String): List<CommunityMovie> = withContext(Dispatchers.IO) {
        try {
            val (sheetId, gid) = extractSheetIdAndGid(sheetUrl)
            val rows = fetchSheetRows(sheetId, gid)
            rows.mapNotNull { row ->
                val name = row.getOrNull(0) ?: return@mapNotNull null
                if (name.isBlank()) return@mapNotNull null

                // Some sheets use pipe-delimited format in column A: "name|link|thumb|info"
                if ("|" in name) {
                    val parts = name.split("|")
                    val movieName = stripKodiTags(parts.getOrNull(0)?.replace("*", "")?.replace("@", "")?.trim() ?: return@mapNotNull null)
                    val link = parts.getOrNull(1)?.trim() ?: return@mapNotNull null
                    if (!isValidLink(link)) return@mapNotNull null
                    CommunityMovie(
                        name = movieName,
                        link = link,
                        thumbnailUrl = parts.getOrNull(2)?.trim() ?: "",
                        description = parts.getOrNull(3)?.trim() ?: "",
                        fanartUrl = parts.getOrNull(4)?.trim() ?: ""
                    )
                } else {
                    val link = row.getOrNull(1)?.trim() ?: return@mapNotNull null
                    if (!isValidLink(link)) return@mapNotNull null
                    val ratingStr = row.getOrNull(6)?.trim() ?: ""
                    CommunityMovie(
                        name = stripKodiTags(name.trim()),
                        link = cleanLink(link),
                        thumbnailUrl = row.getOrNull(2)?.trim() ?: "",
                        description = row.getOrNull(3)?.trim() ?: "",
                        fanartUrl = row.getOrNull(4)?.trim() ?: "",
                        genre = row.getOrNull(5)?.trim() ?: "",
                        rating = ratingStr.toFloatOrNull() ?: 0f
                    )
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load source movies: $sheetUrl", e)
            emptyList()
        }
    }

    // ═══ Internal ═══

    /** Fetch rows from a Google Sheet via gviz API */
    private fun fetchSheetRows(sheetId: String, gid: String): List<List<String?>> {
        val url = "https://docs.google.com/spreadsheets/d/$sheetId/gviz/tq?gid=$gid&headers=1"
        val response = URL(url).readText()

        // Response is JSONP: google.visualization.Query.setResponse({...});
        val jsonStr = Regex("""\((\{.*\})\)""").find(response)?.groupValues?.get(1)
            ?: throw Exception("Invalid sheet response")

        val json = Gson().fromJson(jsonStr, JsonObject::class.java)
        val rows = json.getAsJsonObject("table")
            ?.getAsJsonArray("rows")
            ?: return emptyList()

        return rows.map { rowElement ->
            val cells = rowElement.asJsonObject.getAsJsonArray("c")
            (0 until cells.size()).map { i ->
                val cell = cells[i]
                if (cell.isJsonNull) null
                else cell.asJsonObject?.get("v")?.let {
                    if (it.isJsonNull) null else it.asString
                }
            }
        }
    }

    /** Extract sheet ID and GID from various Google Sheet URL formats */
    private fun extractSheetIdAndGid(url: String): Pair<String, String> {
        val sheetId = Regex("""/d/([a-zA-Z0-9_-]+)""").find(url)?.groupValues?.get(1)
            ?: throw Exception("Invalid sheet URL: $url")
        val gid = Regex("""gid=(\d+)""").find(url)?.groupValues?.get(1) ?: "0"
        return sheetId to gid
    }

    private fun isValidLink(link: String): Boolean {
        // Only accept Fshare and Google Sheet links — filter out spam/contact/donation rows
        return link.contains("fshare.vn") || link.contains("docs.google.com")
    }

    private fun cleanLink(link: String): String {
        // Remove ?token=xxx suffix
        val tokenMatch = Regex("""(https.+?)/\?token""").find(link)
        return tokenMatch?.groupValues?.get(1) ?: link
    }

    /** Strip Kodi-style formatting tags: [COLOR xxx], [/COLOR], [B], [/B], [I], [/I] */
    private fun stripKodiTags(text: String): String {
        return text
            .replace(Regex("""\[COLOR\s+[^\]]+]"""), "")
            .replace("[/COLOR]", "")
            .replace("[B]", "")
            .replace("[/B]", "")
            .replace("[I]", "")
            .replace("[/I]", "")
            .replace("*", "")
            .replace("@", "")
            .trim()
    }
}
