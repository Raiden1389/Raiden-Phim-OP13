package xyz.raidenhub.phim.data.repository

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import xyz.raidenhub.phim.BuildConfig
import xyz.raidenhub.phim.data.api.ApiClient
import xyz.raidenhub.phim.data.api.FshareApi
import xyz.raidenhub.phim.data.api.models.FshareDownloadRequest
import xyz.raidenhub.phim.data.api.models.FshareFile
import xyz.raidenhub.phim.data.api.models.FshareFolderRequest
import xyz.raidenhub.phim.data.api.models.FshareLoginRequest
import xyz.raidenhub.phim.data.api.models.FshareUserInfo

/**
 * Fshare Repository — manages auth, folder browsing, and link resolution
 *
 * Flow:
 *   login(email, pass) → token + session_id
 *   listFolder(url) → [FshareFile]
 *   resolveLink(fileUrl) → direct CDN URL (http://download802.fshare.vn/...)
 */
class FshareRepository private constructor(private val context: Context) {
    private val api: FshareApi = ApiClient.fshare
    companion object {
        private const val TAG = "FshareRepo"
        private const val PREF_FILE = "fshare_prefs"
        private const val KEY_EMAIL = "fs_email"
        private const val KEY_PASS = "fs_pass"
        private const val KEY_TOKEN = "fs_token"
        private const val KEY_SESSION = "fs_session"

        @Volatile
        private var INSTANCE: FshareRepository? = null

        fun getInstance(context: Context): FshareRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: FshareRepository(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    // In-memory session state (shared via singleton)
    private var token: String? = null
    private var sessionId: String? = null
    private var userInfo: FshareUserInfo? = null

    /** Encrypted preferences for storing credentials */
    private val prefs: SharedPreferences by lazy {
        try {
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()
            EncryptedSharedPreferences.create(
                context, PREF_FILE, masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (e: Exception) {
            Log.w(TAG, "EncryptedPrefs failed, fallback to regular", e)
            context.getSharedPreferences(PREF_FILE, Context.MODE_PRIVATE)
        }
    }

    // ═══ Auth ═══

    /**
     * Login to Fshare API
     * @return UserInfo on success, throws on failure
     */
    suspend fun login(email: String, password: String): FshareUserInfo {
        val response = api.login(FshareLoginRequest(user_email = email, password = password))

        if (response.code != 200 || response.token == null || response.session_id == null) {
            throw FshareAuthException(response.msg)
        }

        token = response.token
        sessionId = response.session_id

        // Save credentials for auto-login
        prefs.edit()
            .putString(KEY_EMAIL, email)
            .putString(KEY_PASS, password)
            .putString(KEY_TOKEN, token)
            .putString(KEY_SESSION, sessionId)
            .apply()

        // Fetch user info
        val info = api.getUserInfo("session_id=$sessionId")
        userInfo = info
        Log.d(TAG, "Login OK: ${info.email} (${info.account_type})")
        return info
    }

    /**
     * Try auto-login with saved credentials
     * Fallback: BuildConfig credentials (from local.properties at build time)
     * @return Result<FshareUserInfo>
     */
    suspend fun autoLogin(): Result<FshareUserInfo> {
        // Priority 1: saved credentials (from previous login)
        var email = prefs.getString(KEY_EMAIL, null)
        var pass = prefs.getString(KEY_PASS, null)

        // Priority 2: BuildConfig (hardcoded at build time from local.properties)
        if (email.isNullOrBlank() || pass.isNullOrBlank()) {
            val bcEmail = BuildConfig.FSHARE_EMAIL
            val bcPass = BuildConfig.FSHARE_PASSWORD
            if (bcEmail.isNotBlank() && bcPass.isNotBlank()) {
                Log.d(TAG, "Using BuildConfig credentials (fresh install)")
                email = bcEmail
                pass = bcPass
            } else {
                return Result.failure(FshareAuthException("Chưa nhập email Fshare"))
            }
        }

        return try {
            Result.success(login(email, pass))
        } catch (e: Exception) {
            Log.w(TAG, "Auto-login failed", e)
            Result.failure(e)
        }
    }

    /** Logout — clear all state */
    fun logout() {
        token = null
        sessionId = null
        userInfo = null
        prefs.edit().clear().apply()
    }

    /** Check if we have saved credentials */
    fun hasCredentials(): Boolean = prefs.getString(KEY_EMAIL, null) != null

    /** Check if currently logged in (token in memory) */
    fun isLoggedIn(): Boolean = token != null && sessionId != null

    /** Get saved email (for display in Settings) */
    fun getSavedEmail(): String? = prefs.getString(KEY_EMAIL, null)

    /** Get saved password (for display in Settings) */
    fun getSavedPassword(): String? = prefs.getString(KEY_PASS, null)

    /** Get cached user info */
    val currentUser: FshareUserInfo?
        get() = userInfo

    // ═══ Folder & Files ═══

    /**
     * List contents of a Fshare folder
     */
    suspend fun listFolder(folderUrl: String, page: Int = 0): List<FshareFile> {
        ensureLoggedIn()
        val safeToken = token ?: throw FshareAuthException("Token missing after login")
        val responseBody = api.getFolderList(
            "session_id=$sessionId",
            FshareFolderRequest(
                token = safeToken,
                url = folderUrl,
                pageIndex = page
            )
        )
        val json = responseBody.string()
        Log.d(TAG, "listFolder response: ${json.take(200)}")

        // Fshare API returns array on success, object on error
        val element = com.google.gson.JsonParser.parseString(json)
        if (element.isJsonArray) {
            return com.google.gson.Gson().fromJson(
                json, Array<FshareFile>::class.java
            ).toList()
        }

        // Error object: {"code": 201, "msg": "..."}
        val obj = element.asJsonObject
        val code = obj.get("code")?.asInt ?: -1
        val msg = obj.get("msg")?.asString ?: "Unknown error"
        Log.w(TAG, "listFolder error: code=$code, msg=$msg")

        if (code == 201 || msg.contains("login", ignoreCase = true)) {
            // Token expired → force re-login
            token = null
            sessionId = null
            throw FshareAuthException("Session hết hạn. Bấm lại để đăng nhập.")
        }
        throw FshareResolveException("Fshare error ($code): $msg")
    }

    // ═══ Link Resolution ═══

    /**
     * Resolve a Fshare file URL to a direct CDN streaming URL
     * @param fshareUrl e.g. "https://www.fshare.vn/file/XXXXX"
     * @return Direct CDN URL like "http://download802.fshare.vn/dl/..."
     */
    suspend fun resolveLink(fshareUrl: String): String {
        ensureLoggedIn()
        val safeToken = token ?: throw FshareAuthException("Token missing after login")
        // Add share param for community-shared files (same as VietMediaF)
        val url = if ("?" !in fshareUrl) "$fshareUrl?share=8805984"
                  else "$fshareUrl&share=8805984"
        val response = api.getDownloadLink(
            "session_id=$sessionId",
            FshareDownloadRequest(token = safeToken, url = url)
        )
        if (!response.isSuccess) {
            throw FshareResolveException(response.msg ?: "Failed to resolve link")
        }
        Log.d(TAG, "Resolved: ${fshareUrl.takeLast(20)} → CDN")
        return response.location!!
    }

    // ═══ Internal ═══

    /**
     * Ensure we have a valid session, auto-login if needed
     */
    private suspend fun ensureLoggedIn() {
        if (isLoggedIn()) return
        // Try restore from saved prefs
        val savedToken = prefs.getString(KEY_TOKEN, null)
        val savedSession = prefs.getString(KEY_SESSION, null)
        if (savedToken != null && savedSession != null) {
            token = savedToken
            sessionId = savedSession
            // Verify session is still valid
            try {
                userInfo = api.getUserInfo("session_id=$sessionId")
                return
            } catch (_: Exception) {
                // Session expired, re-login
            }
        }
        // Full re-login
        val result = autoLogin()
        if (result.isFailure) {
            throw FshareAuthException("Chưa đăng nhập Fshare. Vào Cài đặt → Fshare để đăng nhập.")
        }
    }
}

// ═══ Exceptions ═══

class FshareAuthException(message: String) : Exception(message)
class FshareResolveException(message: String) : Exception(message)
