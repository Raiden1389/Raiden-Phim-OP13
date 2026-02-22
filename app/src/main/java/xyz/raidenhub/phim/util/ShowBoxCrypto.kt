package xyz.raidenhub.phim.util

import android.util.Base64
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.security.MessageDigest
import java.util.concurrent.TimeUnit
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * ShowBox Encrypted API — Port of ShowboxAPI.js
 *
 * ShowBox uses TripleDES encryption + MD5 verification for all API calls.
 * Base URL: https://mbpapi.shegu.net/api/api_client/index/
 *
 * Flow:
 *   1. Search by title → ShowBox internal IDs
 *   2. Use internal ID with showbox.media/index/share_link → FebBox share_key
 */
object ShowBoxCrypto {

    private const val TAG = "ShowBoxCrypto"

    // ═══ Config (from ShowboxAPI.js) ═══
    private const val BASE_URL = "https://mbpapi.shegu.net/api/api_client/index/"
    private const val APP_KEY = "moviebox"
    private const val KEY = "123d6cedf626dy54233aa1w6"
    private const val IV = "wEiphTn!"

    // Default request params
    private const val CHILD_MODE = "0"
    private const val APP_VERSION = "11.5"
    private const val LANG = "en"
    private const val PLATFORM = "android"
    private const val CHANNEL = "Website"
    private const val APPID = "27"
    private const val VERSION = "129"
    private const val MEDIUM = "Website"

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    // ═══ Crypto Functions ═══

    /**
     * Encrypt data using TripleDES/CBC/PKCS5Padding
     * Matches CryptoJS.TripleDES.encrypt behavior
     */
    private fun encrypt(data: String): String {
        val keyBytes = KEY.toByteArray(Charsets.UTF_8).copyOf(24) // DESede needs 24 bytes
        val ivBytes = IV.toByteArray(Charsets.UTF_8).copyOf(8)    // IV needs 8 bytes

        val cipher = Cipher.getInstance("DESede/CBC/PKCS5Padding")
        val keySpec = SecretKeySpec(keyBytes, "DESede")
        val ivSpec = IvParameterSpec(ivBytes)
        cipher.init(Cipher.ENCRYPT_MODE, keySpec, ivSpec)

        val encrypted = cipher.doFinal(data.toByteArray(Charsets.UTF_8))
        return Base64.encodeToString(encrypted, Base64.NO_WRAP)
    }

    /**
     * MD5 hash (matches CryptoJS.MD5)
     */
    private fun md5(input: String): String {
        val md = MessageDigest.getInstance("MD5")
        val digest = md.digest(input.toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { "%02x".format(it) }
    }

    /**
     * Generate verify hash: MD5(MD5(APP_KEY) + KEY + encryptedData)
     */
    private fun generateVerify(encryptedData: String): String {
        return md5(md5(APP_KEY) + KEY + encryptedData)
    }

    /**
     * Generate random 32-char hex token (matches nanoid behavior)
     */
    private fun generateToken(): String {
        val chars = "0123456789abcdef"
        return (1..32).map { chars.random() }.joinToString("")
    }

    // ═══ API Request ═══

    /**
     * Make encrypted request to ShowBox API
     * Mirrors ShowboxAPI.request() from JS
     */
    private suspend fun request(module: String, params: Map<String, String> = emptyMap()): JSONObject {
        return withContext(Dispatchers.IO) {
            // Build request data JSON
            val requestData = JSONObject().apply {
                put("childmode", CHILD_MODE)
                put("app_version", APP_VERSION)
                put("lang", LANG)
                put("platform", PLATFORM)
                put("channel", CHANNEL)
                put("appid", APPID)
                put("version", VERSION)
                put("medium", MEDIUM)
                put("expired_date", (System.currentTimeMillis() / 1000 + 60 * 60 * 12).toString())
                put("module", module)
                params.forEach { (k, v) -> put(k, v) }
            }

            // Encrypt the request data
            val encryptedData = encrypt(requestData.toString())

            // Build the body JSON (app_key, verify, encrypt_data)
            val bodyJson = JSONObject().apply {
                put("app_key", md5(APP_KEY))
                put("verify", generateVerify(encryptedData))
                put("encrypt_data", encryptedData)
            }

            // Base64 encode the body
            val dataBase64 = Base64.encodeToString(
                bodyJson.toString().toByteArray(Charsets.UTF_8),
                Base64.NO_WRAP
            )

            // Build form body string (matching JS: URLSearchParams + &token{nanoid})
            val formBody = "data=$dataBase64&appid=$APPID&platform=$PLATFORM&version=$VERSION&medium=$MEDIUM&token${generateToken()}"

            Log.d(TAG, "Request module=$module")

            val request = Request.Builder()
                .url(BASE_URL)
                .post(formBody.toRequestBody("application/x-www-form-urlencoded".toMediaType()))
                .addHeader("Platform", PLATFORM)
                .addHeader("Content-Type", "application/x-www-form-urlencoded")
                .addHeader("User-Agent", "okhttp/3.2.0")
                .build()

            val response = client.newCall(request).execute()
            val body = response.body?.string() ?: "{}"
            Log.d(TAG, "Response ($module): ${body.take(500)}")
            JSONObject(body)
        }
    }

    // ═══ Public API ═══

    /**
     * Search ShowBox for movies or TV shows by title.
     * Returns list of results with ShowBox internal IDs.
     *
     * @param title Search query
     * @param type "movie", "tv", or "all"
     * @return List of (id, title, box_type, quality_tag, year, imdb_rating)
     */
    data class ShowBoxResult(
        val id: Int,
        val title: String,
        val boxType: Int,      // 1 = movie, 2 = tv
        val year: String,
        val imdbRating: String,
        val qualityTag: String
    )

    suspend fun search(title: String, type: String = "all"): List<ShowBoxResult> {
        return try {
            val typeParam = when (type) {
                "movie" -> "movie"
                "tv" -> "tv"
                else -> "all"
            }
            val response = request("Search5", mapOf(
                "keyword" to title,
                "type" to typeParam,
                "page" to "1",
                "pagelimit" to "20"
            ))

            val dataArray = response.optJSONArray("data") ?: return emptyList()
            val results = mutableListOf<ShowBoxResult>()

            for (i in 0 until dataArray.length()) {
                val item = dataArray.getJSONObject(i)
                results.add(ShowBoxResult(
                    id = item.optInt("id", 0),
                    title = item.optString("title", ""),
                    boxType = item.optInt("box_type", 0),
                    year = item.optString("year", ""),
                    imdbRating = item.optString("imdb_rating", ""),
                    qualityTag = item.optString("quality_tag", "")
                ))
            }

            Log.d(TAG, "Search '$title' → ${results.size} results")
            results.forEach { Log.d(TAG, "  [${it.id}] ${it.title} (${it.year}) type=${it.boxType}") }
            results
        } catch (e: Exception) {
            Log.e(TAG, "Search error: ${e.message}")
            emptyList()
        }
    }

    /**
     * Find ShowBox internal ID for a TMDB title.
     * Searches ShowBox and matches by title similarity.
     *
     * @param title Movie/TV show title from TMDB
     * @param type "movie" or "tv"
     * @return ShowBox internal ID, or null if not found
     */
    suspend fun findShowBoxId(title: String, type: String): Int? {
        val results = search(title, type)
        if (results.isEmpty()) return null

        // Find best match (exact match first, then first result)
        val normalized = title.lowercase().trim()
        val exactMatch = results.find { it.title.lowercase().trim() == normalized }
        if (exactMatch != null) {
            Log.d(TAG, "Exact match: [${exactMatch.id}] ${exactMatch.title}")
            return exactMatch.id
        }

        // Fallback to first result of matching type
        val boxType = if (type == "movie") 1 else 2
        val typeMatch = results.find { it.boxType == boxType }
        if (typeMatch != null) {
            Log.d(TAG, "Type match: [${typeMatch.id}] ${typeMatch.title}")
            return typeMatch.id
        }

        // Last resort: first result
        Log.d(TAG, "First result: [${results[0].id}] ${results[0].title}")
        return results[0].id
    }
}
