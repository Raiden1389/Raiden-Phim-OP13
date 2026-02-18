package xyz.raidenhub.phim.util

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import xyz.raidenhub.phim.data.api.models.SubDLSubtitle
import xyz.raidenhub.phim.data.api.models.SubtitleResult
import java.io.File
import java.util.concurrent.TimeUnit
import java.util.zip.ZipInputStream

/**
 * Downloads SubDL subtitle archives (.zip) and extracts .srt/.vtt files
 * to the app's cache directory, returning a local file:// URI for ExoPlayer.
 */
object SubtitleDownloader {

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    /**
     * Download and extract a SubDL subtitle.
     * Returns SubtitleResult with local file:// URL, or null on failure.
     */
    suspend fun downloadSubDL(
        context: Context,
        subtitle: SubDLSubtitle
    ): SubtitleResult? = withContext(Dispatchers.IO) {
        try {
            val downloadUrl = "https://dl.subdl.com${subtitle.url}"

            val request = Request.Builder()
                .url(downloadUrl)
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                .build()

            val response = client.newCall(request).execute()
            if (!response.isSuccessful) return@withContext null

            val body = response.body ?: return@withContext null
            val bytes = body.bytes()

            // Create subtitle cache dir
            val subDir = File(context.cacheDir, "subtitles")
            subDir.mkdirs()

            // Determine file type
            val url = subtitle.url.lowercase()
            val extractedFile: File? = when {
                url.endsWith(".zip") -> extractFromZip(bytes, subDir)
                url.endsWith(".srt") -> saveDirect(bytes, subDir, "srt")
                url.endsWith(".vtt") -> saveDirect(bytes, subDir, "vtt")
                url.endsWith(".ass") -> saveDirect(bytes, subDir, "ass")
                // Try as zip by default (SubDL usually returns zip)
                else -> extractFromZip(bytes, subDir) ?: saveDirect(bytes, subDir, "srt")
            }

            if (extractedFile == null || !extractedFile.exists()) return@withContext null

            val langCode = when {
                subtitle.lang.contains("vietnam", ignoreCase = true) -> "vi"
                subtitle.lang.contains("english", ignoreCase = true) -> "en"
                else -> subtitle.lang.take(2).lowercase()
            }

            SubtitleResult(
                url = Uri.fromFile(extractedFile).toString(),
                language = langCode,
                languageLabel = subtitle.language.ifBlank { subtitle.lang.replaceFirstChar { it.uppercase() } },
                source = "SubDL ⬇",
                fileName = subtitle.releaseName,
                downloadCount = 0
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Extract first .srt/.vtt/.ass file from a zip archive
     */
    private fun extractFromZip(zipBytes: ByteArray, outDir: File): File? {
        try {
            ZipInputStream(zipBytes.inputStream()).use { zis ->
                var entry = zis.nextEntry
                while (entry != null) {
                    val name = entry.name.lowercase()
                    if (!entry.isDirectory && (name.endsWith(".srt") || name.endsWith(".vtt") || name.endsWith(".ass"))) {
                        val ext = name.substringAfterLast(".")
                        val outFile = File(outDir, "sub_${System.currentTimeMillis()}.$ext")
                        outFile.outputStream().use { fos ->
                            zis.copyTo(fos)
                        }
                        return outFile
                    }
                    entry = zis.nextEntry
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }

    /**
     * Save raw bytes directly as a subtitle file
     */
    private fun saveDirect(bytes: ByteArray, outDir: File, ext: String): File {
        val outFile = File(outDir, "sub_${System.currentTimeMillis()}.$ext")
        outFile.writeBytes(bytes)
        return outFile
    }

    /**
     * Clean up old cached subtitle files (older than 24h)
     */
    fun cleanCache(context: Context) {
        try {
            val subDir = File(context.cacheDir, "subtitles")
            if (subDir.exists()) {
                val cutoff = System.currentTimeMillis() - 24 * 60 * 60 * 1000
                subDir.listFiles()?.forEach { file ->
                    if (file.lastModified() < cutoff) file.delete()
                }
            }
        } catch (_: Exception) {}
    }
}
