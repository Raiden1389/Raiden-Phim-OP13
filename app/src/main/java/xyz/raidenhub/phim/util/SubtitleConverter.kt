package xyz.raidenhub.phim.util

/**
 * #46 — Subtitle Format Support
 * Convert .srt and .ass/.ssa to .vtt on-the-fly for ExoPlayer
 */
object SubtitleConverter {

    /**
     * Detect format from URL or content and convert to WebVTT if needed
     */
    fun convertToVtt(content: String, sourceUrl: String): String {
        return when {
            sourceUrl.endsWith(".vtt", ignoreCase = true) -> content
            sourceUrl.endsWith(".srt", ignoreCase = true) -> srtToVtt(content)
            sourceUrl.endsWith(".ass", ignoreCase = true) ||
            sourceUrl.endsWith(".ssa", ignoreCase = true) -> assToVtt(content)
            // Auto-detect from content
            content.trimStart().startsWith("WEBVTT") -> content
            content.contains("[Script Info]") || content.contains("[V4+ Styles]") -> assToVtt(content)
            content.trim().first().isDigit() -> srtToVtt(content)
            else -> content // Unknown — return as-is
        }
    }

    /**
     * Convert SRT to WebVTT
     * SRT format:
     * 1
     * 00:00:01,000 --> 00:00:04,000
     * Text here
     *
     * WebVTT format:
     * WEBVTT
     *
     * 00:00:01.000 --> 00:00:04.000
     * Text here
     */
    fun srtToVtt(srt: String): String {
        val sb = StringBuilder("WEBVTT\n\n")
        val lines = srt.replace("\r\n", "\n").replace("\r", "\n").split("\n")
        var i = 0
        while (i < lines.size) {
            val line = lines[i].trim()
            // Skip sequence number
            if (line.matches(Regex("^\\d+$"))) {
                i++
                continue
            }
            // Timestamp line: replace comma with dot
            if (line.contains("-->")) {
                sb.appendLine(line.replace(',', '.'))
                i++
                // Append text lines until blank
                while (i < lines.size && lines[i].trim().isNotEmpty()) {
                    sb.appendLine(lines[i].trim())
                    i++
                }
                sb.appendLine()
            } else {
                i++
            }
        }
        return sb.toString()
    }

    /**
     * Convert ASS/SSA to WebVTT
     * Parse [Events] section, extract Dialogue lines
     * Format: Dialogue: 0,0:00:01.00,0:00:04.00,Default,,0,0,0,,Text
     */
    fun assToVtt(ass: String): String {
        val sb = StringBuilder("WEBVTT\n\n")
        val lines = ass.replace("\r\n", "\n").replace("\r", "\n").split("\n")
        var inEvents = false
        var formatFields = listOf<String>()

        for (line in lines) {
            val trimmed = line.trim()
            if (trimmed == "[Events]") {
                inEvents = true
                continue
            }
            if (trimmed.startsWith("[") && trimmed != "[Events]") {
                inEvents = false
                continue
            }
            if (!inEvents) continue

            if (trimmed.startsWith("Format:")) {
                formatFields = trimmed.removePrefix("Format:").split(",").map { it.trim().lowercase() }
                continue
            }

            if (trimmed.startsWith("Dialogue:")) {
                val parts = trimmed.removePrefix("Dialogue:").trimStart()
                // Split by comma, but the last field (Text) may contain commas
                val fields = parts.split(",", limit = formatFields.size.coerceAtLeast(10))
                if (fields.size < 3) continue

                val startIdx = formatFields.indexOf("start").takeIf { it >= 0 } ?: 1
                val endIdx = formatFields.indexOf("end").takeIf { it >= 0 } ?: 2
                val textIdx = formatFields.indexOf("text").takeIf { it >= 0 } ?: (fields.size - 1)

                if (startIdx >= fields.size || endIdx >= fields.size) continue

                val start = assTimeToVtt(fields[startIdx].trim())
                val end = assTimeToVtt(fields[endIdx].trim())
                // Get text — rejoin anything after text index
                val text = if (textIdx < fields.size) {
                    fields.subList(textIdx, fields.size).joinToString(",")
                } else ""

                // Clean ASS formatting tags: {\an8}, {\b1}, {\pos(x,y)}, etc.
                val cleanText = text
                    .replace(Regex("\\{\\\\[^}]*\\}"), "")  // Remove override tags
                    .replace("\\N", "\n")                    // Line breaks
                    .replace("\\n", "\n")
                    .replace("\\h", " ")                     // Hard space
                    .trim()

                if (cleanText.isNotBlank()) {
                    sb.appendLine("$start --> $end")
                    sb.appendLine(cleanText)
                    sb.appendLine()
                }
            }
        }
        return sb.toString()
    }

    /**
     * Convert ASS time format (H:MM:SS.CC) to VTT format (HH:MM:SS.mmm)
     */
    private fun assTimeToVtt(assTime: String): String {
        val parts = assTime.split(":", limit = 3)
        if (parts.size != 3) return assTime
        val h = parts[0].padStart(2, '0')
        val m = parts[1].padStart(2, '0')
        val secParts = parts[2].split(".")
        val s = secParts[0].padStart(2, '0')
        // ASS uses centiseconds (2 digits), VTT uses milliseconds (3 digits)
        val ms = if (secParts.size > 1) {
            secParts[1].padEnd(3, '0').take(3)
        } else "000"
        return "$h:$m:$s.$ms"
    }

    /**
     * Detect subtitle MIME type from URL
     */
    fun getMimeType(url: String): String {
        return when {
            url.endsWith(".vtt", ignoreCase = true) -> "text/vtt"
            url.endsWith(".srt", ignoreCase = true) -> "application/x-subrip"
            url.endsWith(".ass", ignoreCase = true) ||
            url.endsWith(".ssa", ignoreCase = true) -> "text/x-ssa"
            else -> "text/vtt"
        }
    }

    /**
     * Check if URL needs conversion (non-VTT format)
     */
    fun needsConversion(url: String): Boolean {
        return url.endsWith(".srt", ignoreCase = true) ||
               url.endsWith(".ass", ignoreCase = true) ||
               url.endsWith(".ssa", ignoreCase = true)
    }
}
