package xyz.raidenhub.phim.util

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import android.webkit.CookieManager
import android.webkit.WebView
import android.webkit.WebViewClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import xyz.raidenhub.phim.data.api.models.FebBoxFile
import xyz.raidenhub.phim.data.api.models.FebBoxStream
import kotlin.coroutines.resume

/**
 * FebBox WebView Helper — Hidden WebView engine for FebBox API calls.
 *
 * FebBox blocks external HTTP requests (OkHttp/Node.js return 500).
 * Only same-origin browser requests work. This helper uses a hidden WebView
 * to make those requests, injecting JavaScript to extract data.
 *
 * Usage:
 *   val helper = FebBoxWebViewHelper(context)
 *   helper.setCookie(token)
 *   val files = helper.getFileList(shareKey)
 *   val streams = helper.getStreamUrls(shareKey, fid)
 *   helper.destroy()
 */
class FebBoxWebViewHelper(context: Context) {

    companion object {
        private const val TAG = "FebBoxWV"
        private const val FEBBOX_URL = "https://www.febbox.com"
        private const val TIMEOUT_MS = 20_000L
    }

    private var webView: WebView? = null
    private var isPageLoaded = false
    private var cookieToken: String = ""

    init {
        // WebView must be created on Main thread
        webView = WebView(context).apply {
            @SuppressLint("SetJavaScriptEnabled")
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.userAgentString = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) " +
                "AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"

            webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)
                    isPageLoaded = true
                    Log.d(TAG, "Page loaded: $url")
                }
            }
        }
    }

    /**
     * Set FebBox UI cookie for authentication.
     */
    fun setCookie(token: String) {
        cookieToken = token
        val cookieManager = CookieManager.getInstance()
        cookieManager.setAcceptCookie(true)
        cookieManager.setCookie(FEBBOX_URL, "ui=$token")
        cookieManager.setCookie("https://febbox.com", "ui=$token")
        cookieManager.flush()
        Log.d(TAG, "Cookie set via CookieManager (token length=${token.length})")
    }

    /**
     * Ensure WebView has loaded FebBox base page (needed for same-origin).
     */
    suspend fun ensureLoaded(shareKey: String) = withContext(Dispatchers.Main) {
        if (!isPageLoaded) {
            Log.d(TAG, "Loading FebBox page for share: $shareKey")
            webView?.loadUrl("$FEBBOX_URL/share/$shareKey")
            // Wait for page load
            withTimeout(TIMEOUT_MS) {
                while (!isPageLoaded) {
                    kotlinx.coroutines.delay(100)
                }
            }
            // Inject cookie via JS after page load
            if (cookieToken.isNotBlank()) {
                webView?.evaluateJavascript(
                    "document.cookie = 'ui=${cookieToken}; path=/; domain=.febbox.com';",
                    null
                )
                Log.d(TAG, "Cookie injected via JS")
                kotlinx.coroutines.delay(200)  // Small delay for cookie to take effect
            }
        }
    }

    /**
     * Get file list from FebBox share folder.
     * Root (parentId=0) returns season folders.
     * Season folder (parentId=seasonFid) returns episode files.
     */
    suspend fun getFileList(shareKey: String, parentId: Long = 0): List<FebBoxFile> {
        ensureLoaded(shareKey)

        val js = """
            (function() {
                try {
                    var xhr = new XMLHttpRequest();
                    xhr.open('GET', '/file/file_share_list?share_key=$shareKey&pwd=&parent_id=$parentId&is_html=1', false);
                    xhr.withCredentials = true;
                    xhr.send();
                    var d = JSON.parse(xhr.responseText);
                    console.log('FebBox API code=' + d.code + ' msg=' + d.msg);
                    
                    if (d.code !== 1) return JSON.stringify({error: 'code=' + d.code + ' msg=' + (d.msg || 'unknown'), files: []});
                    if (!d.html) return JSON.stringify({error: 'no html in response', files: []});
                    
                    console.log('FebBox HTML length=' + d.html.length);
                    
                    var files = [];
                    var parser = new DOMParser();
                    var doc = parser.parseFromString(d.html, 'text/html');
                    var items = doc.querySelectorAll('.file');
                    console.log('FebBox found ' + items.length + ' .file elements');
                    
                    if (items.length === 0) {
                        return JSON.stringify({error: 'no .file elements, HTML: ' + d.html.substring(0, 300), files: []});
                    }
                    
                    for (var i = 0; i < items.length; i++) {
                        var item = items[i];
                        var fid = item.getAttribute('data-id') || '0';
                        var isFolder = item.classList.contains('open_dir');
                        var nameEl = item.querySelector('.file_name');
                        var sizeEl = item.querySelector('.file_size');
                        var timeEl = item.querySelector('.file_time');
                        var imgEl = item.querySelector('.file_icon img');
                        
                        files.push({
                            fid: parseInt(fid),
                            name: nameEl ? nameEl.textContent.trim() : '',
                            size: sizeEl ? sizeEl.textContent.trim() : '',
                            isFolder: isFolder,
                            thumbUrl: imgEl ? imgEl.getAttribute('src') || '' : '',
                            updateTime: timeEl ? timeEl.textContent.trim() : ''
                        });
                    }
                    
                    return JSON.stringify({files: files});
                } catch (e) {
                    return JSON.stringify({error: e.message, files: []});
                }
            })()
        """.trimIndent()

        val result = evaluateJs(js)
        Log.d(TAG, "getFileList raw (first 300): ${result.take(300)}")
        
        return try {
            val gson = com.google.gson.Gson()
            val wrapper = gson.fromJson(result, com.google.gson.JsonObject::class.java)
            val error = wrapper?.get("error")?.asString
            if (!error.isNullOrBlank()) {
                Log.w(TAG, "FebBox error: $error")
            }
            val filesArray = wrapper?.getAsJsonArray("files")
            if (filesArray != null) {
                val type = object : com.google.gson.reflect.TypeToken<List<FebBoxFile>>() {}.type
                gson.fromJson<List<FebBoxFile>>(filesArray, type) ?: emptyList()
            } else {
                parseFileList(result)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Parse error: ${e.message}")
            parseFileList(result)
        }
    }

    /**
     * Get HLS stream URLs from FebBox player page.
     * Returns list of streams (AUTO, 1080p, 720p, 360p).
     */
    suspend fun getStreamUrls(shareKey: String, fid: Long): List<FebBoxStream> {
        ensureLoaded(shareKey)

        val js = """
            (function() {
                try {
                    var xhr = new XMLHttpRequest();
                    xhr.open('GET', '/file/player/video?share_key=$shareKey&fid=$fid', false);
                    xhr.withCredentials = true;
                    xhr.send();
                    var html = xhr.responseText;
                    console.log('FebBox player response length=' + html.length);
                    
                    // Extract sources array from embedded JavaScript
                    var match = html.match(/sources\s*[:=]\s*(\[[\s\S]*?\])/);
                    if (!match) {
                        console.log('No sources found. Preview: ' + html.substring(0, 300));
                        return JSON.stringify([]);
                    }
                    
                    var sources = JSON.parse(match[1]);
                    var streams = [];
                    for (var i = 0; i < sources.length; i++) {
                        var s = sources[i];
                        if (s.file && (!s.label || !s.label.startsWith('audio'))) {
                            streams.push({
                                url: s.file,
                                quality: s.label || 'AUTO',
                                type: 'hls'
                            });
                        }
                    }
                    
                    console.log('Found ' + streams.length + ' streams');
                    return JSON.stringify(streams);
                } catch (e) {
                    console.log('Stream error: ' + e.message);
                    return JSON.stringify([]);
                }
            })()
        """.trimIndent()

        val result = evaluateJs(js)
        Log.d(TAG, "getStreamUrls raw (first 200): ${result.take(200)}")
        return parseStreamList(result)
    }

    /**
     * Evaluate JavaScript in WebView and return result as String.
     */
    private suspend fun evaluateJs(js: String): String = withContext(Dispatchers.Main) {
        withTimeout(TIMEOUT_MS) {
            suspendCancellableCoroutine { cont ->
                webView?.evaluateJavascript(js) { value ->
                    // WebView returns JSON-encoded string (with outer quotes)
                    val clean = if (value.startsWith("\"") && value.endsWith("\"")) {
                        value.substring(1, value.length - 1)
                            .replace("\\\"", "\"")
                            .replace("\\\\", "\\")
                            .replace("\\/", "/")
                    } else {
                        value
                    }
                    cont.resume(clean)
                } ?: cont.resume("[]")
            }
        }
    }

    /**
     * Parse JSON string to List<FebBoxFile>.
     */
    private fun parseFileList(json: String): List<FebBoxFile> {
        return try {
            val gson = com.google.gson.Gson()
            val type = object : com.google.gson.reflect.TypeToken<List<FebBoxFile>>() {}.type
            gson.fromJson<List<FebBoxFile>>(json, type) ?: emptyList()
        } catch (e: Exception) {
            Log.e(TAG, "Parse file list error: ${e.message}")
            emptyList()
        }
    }

    /**
     * Parse JSON string to List<FebBoxStream>.
     */
    private fun parseStreamList(json: String): List<FebBoxStream> {
        return try {
            val gson = com.google.gson.Gson()
            val type = object : com.google.gson.reflect.TypeToken<List<FebBoxStream>>() {}.type
            gson.fromJson<List<FebBoxStream>>(json, type) ?: emptyList()
        } catch (e: Exception) {
            Log.e(TAG, "Parse stream list error: ${e.message}")
            emptyList()
        }
    }

    /**
     * Clean up WebView resources.
     */
    fun destroy() {
        webView?.destroy()
        webView = null
        isPageLoaded = false
    }
}
