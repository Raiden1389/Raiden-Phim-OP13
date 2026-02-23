package xyz.raidenhub.phim.ui.screens.detail

/**
 * Thin singleton để pass dữ liệu preview từ MovieCard → DetailScreen.
 * MovieCard set thumbUrl + title ngay trước khi gọi onClick() → navigate.
 * DetailScreen đọc trong Loading state → hiện ảnh/title ngay lập tức.
 *
 * Pattern: Optimistic UI — show card data instantly, replace khi API trả về.
 */
object PendingDetailState {
    var thumbUrl: String = ""
    var title: String = ""

    fun set(thumbUrl: String, title: String) {
        this.thumbUrl = thumbUrl
        this.title = title
    }

    fun clear() {
        thumbUrl = ""
        title = ""
    }
}
