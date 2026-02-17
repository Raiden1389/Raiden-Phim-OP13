package xyz.raidenhub.phim.navigation

sealed class Screen(val route: String) {
    data object Home : Screen("home")
    data object Detail : Screen("detail/{slug}") {
        fun createRoute(slug: String) = "detail/$slug"
    }
    data object Player : Screen("player/{slug}/{server}/{episode}") {
        fun createRoute(slug: String, server: Int, episode: Int) = "player/$slug/$server/$episode"
    }
    data object Search : Screen("search")
    data object Favorites : Screen("favorites")
    data object Settings : Screen("settings")
    data object WatchHistory : Screen("watch_history")
    data object Category : Screen("category/{slug}/{title}") {
        fun createRoute(slug: String, title: String) =
            "category/$slug/${java.net.URLEncoder.encode(title, "UTF-8")}"
    }
    data object Anime : Screen("anime")
    data object AnimeDetail : Screen("anime_detail/{id}/{slug}") {
        fun createRoute(id: Int, slug: String) = "anime_detail/$id/$slug"
    }
    data object English : Screen("english")
    data object EnglishDetail : Screen("english_detail/{mediaId}") {
        fun createRoute(mediaId: String) = "english_detail/${java.net.URLEncoder.encode(mediaId, "UTF-8")}"
    }
    data object EnglishPlayer : Screen("english_player/{episodeId}/{mediaId}/{filmName}") {
        fun createRoute(episodeId: String, mediaId: String, filmName: String) =
            "english_player/${java.net.URLEncoder.encode(episodeId, "UTF-8")}/${java.net.URLEncoder.encode(mediaId, "UTF-8")}/${java.net.URLEncoder.encode(filmName, "UTF-8")}"
    }
}

