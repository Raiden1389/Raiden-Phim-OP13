package xyz.raidenhub.phim.util

import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import retrofit2.HttpException

// ═══════════════════════════════════════════════════════════
//  AppError — Phân loại lỗi rõ ràng thay vì generic Exception
//  Rule:
//    NetworkError  → có thể retry (mạng yếu, timeout, offline)
//    HttpError     → KHÔNG retry (4xx, 5xx → server/client lỗi)
//    ParseError    → KHÔNG retry (JSON sai shape → code bug)
//    UnknownError  → KHÔNG retry (unexpected)
// ═══════════════════════════════════════════════════════════

sealed class AppError(
    override val message: String,
    override val cause: Throwable? = null
) : Exception(message, cause) {

    /** Mất mạng / timeout — có thể retry */
    data class NetworkError(
        override val message: String = "Không có kết nối mạng",
        override val cause: Throwable? = null
    ) : AppError(message, cause)

    /** HTTP 4xx / 5xx — KHÔNG retry */
    data class HttpError(
        val code: Int,
        override val message: String = httpMessage(code),
        override val cause: Throwable? = null
    ) : AppError(message, cause)

    /** JSON parse fail / null field — KHÔNG retry */
    data class ParseError(
        override val message: String = "Lỗi đọc dữ liệu từ server",
        override val cause: Throwable? = null
    ) : AppError(message, cause)

    /** Lỗi không xác định */
    data class UnknownError(
        override val message: String = "Đã xảy ra lỗi không xác định",
        override val cause: Throwable? = null
    ) : AppError(message, cause)

    /** User-friendly message để hiển thị trên UI */
    val userMessage: String get() = when (this) {
        is NetworkError -> "📶 Không có mạng — kiểm tra kết nối và thử lại"
        is HttpError    -> when (code) {
            404  -> "🔍 Không tìm thấy nội dung"
            429  -> "⏳ Quá nhiều yêu cầu — vui lòng chờ"
            in 500..599 -> "🛠️ Lỗi máy chủ ($code) — thử lại sau"
            else -> "❌ Lỗi kết nối ($code)"
        }
        is ParseError   -> "⚠️ Dữ liệu không đúng định dạng"
        is UnknownError -> message
    }

    /** Có nên hiện nút Retry không */
    val isRetryable: Boolean get() = this is NetworkError
}

// ═══ Factory — phân loại Throwable → AppError ═══

fun Throwable.toAppError(): AppError = when (this) {
    is AppError           -> this  // Already classified
    is UnknownHostException,
    is IOException,
    is SocketTimeoutException -> AppError.NetworkError(cause = this)
    is HttpException          -> AppError.HttpError(code = code(), cause = this)
    is NullPointerException,
    is IllegalStateException,
    is ClassCastException     -> AppError.ParseError(cause = this)
    else                      -> AppError.UnknownError(
        message = message ?: "Unknown error",
        cause = this
    )
}

private fun httpMessage(code: Int): String = when (code) {
    400 -> "Yêu cầu không hợp lệ (400)"
    401 -> "Chưa xác thực (401)"
    403 -> "Không có quyền truy cập (403)"
    404 -> "Không tìm thấy (404)"
    429 -> "Quá nhiều yêu cầu (429)"
    500 -> "Lỗi máy chủ nội bộ (500)"
    503 -> "Dịch vụ không khả dụng (503)"
    else -> "Lỗi HTTP $code"
}
