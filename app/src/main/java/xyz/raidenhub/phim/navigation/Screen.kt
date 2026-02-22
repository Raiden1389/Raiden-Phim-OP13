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
    // C-4: Watchlist
    data object Watchlist : Screen("watchlist")
    // C-5: Playlists
    data object PlaylistList : Screen("playlist_list")
    data object PlaylistDetail : Screen("playlist/{id}/{name}") {
        fun createRoute(id: String, name: String) =
            "playlist/$id/${java.net.URLEncoder.encode(name, "UTF-8")}"
    }
    // C-2: Genre Hub
    data object GenreHub : Screen("genre_hub")
    // SuperStream (English content)
    data object SuperStream : Screen("superstream")
    data object SuperStreamDetail : Screen("superstream_detail/{tmdbId}/{type}") {
        fun createRoute(tmdbId: Int, type: String) = "superstream_detail/$tmdbId/$type"
    }
}
