package xyz.raidenhub.phim.data.api.models

import androidx.compose.runtime.Immutable
import com.google.gson.annotations.SerializedName

// ═══════════════════════════════════════════════════
//  SuperStream Models — Tách biệt 100% khỏi existing
// ═══════════════════════════════════════════════════

// ═══ TMDB ═══

@Immutable
data class TmdbSearchResponse(
    @SerializedName("results") val results: List<TmdbSearchItem> = emptyList(),
    @SerializedName("total_results") val totalResults: Int = 0,
    @SerializedName("total_pages") val totalPages: Int = 0,
    @SerializedName("page") val page: Int = 1
)

@Immutable
data class TmdbSearchItem(
    @SerializedName("id") val id: Int = 0,
    @SerializedName("title") val title: String? = null,         // Movie
    @SerializedName("name") val name: String? = null,           // TV
    @SerializedName("original_title") val originalTitle: String? = null,
    @SerializedName("original_name") val originalName: String? = null,
    @SerializedName("overview") val overview: String = "",
    @SerializedName("poster_path") val posterPath: String? = null,
    @SerializedName("backdrop_path") val backdropPath: String? = null,
    @SerializedName("media_type") val mediaType: String = "",   // "movie" | "tv"
    @SerializedName("release_date") val releaseDate: String? = null,
    @SerializedName("first_air_date") val firstAirDate: String? = null,
    @SerializedName("vote_average") val voteAverage: Double = 0.0,
    @SerializedName("vote_count") val voteCount: Int = 0,
    @SerializedName("genre_ids") val genreIds: List<Int> = emptyList(),
    @SerializedName("popularity") val popularity: Double = 0.0
) {
    val displayTitle: String get() = title ?: name ?: originalTitle ?: originalName ?: ""
    val displayDate: String get() = releaseDate ?: firstAirDate ?: ""
    val year: Int get() = displayDate.take(4).toIntOrNull() ?: 0
    val type: String get() = when {
        mediaType == "movie" || title != null -> "movie"
        mediaType == "tv" || name != null -> "tv"
        else -> "movie"
    }
    val posterUrl: String get() = if (posterPath != null) "https://image.tmdb.org/t/p/w342$posterPath" else ""
    val backdropUrl: String get() = if (backdropPath != null) "https://image.tmdb.org/t/p/w780$backdropPath" else ""
}

@Immutable
data class TmdbMovieDetail(
    @SerializedName("id") val id: Int = 0,
    @SerializedName("title") val title: String = "",
    @SerializedName("original_title") val originalTitle: String = "",
    @SerializedName("overview") val overview: String = "",
    @SerializedName("poster_path") val posterPath: String? = null,
    @SerializedName("backdrop_path") val backdropPath: String? = null,
    @SerializedName("release_date") val releaseDate: String = "",
    @SerializedName("runtime") val runtime: Int = 0,
    @SerializedName("vote_average") val voteAverage: Double = 0.0,
    @SerializedName("vote_count") val voteCount: Int = 0,
    @SerializedName("genres") val genres: List<TmdbGenre> = emptyList(),
    @SerializedName("imdb_id") val imdbId: String? = null,
    @SerializedName("status") val status: String = "",
    @SerializedName("tagline") val tagline: String = ""
) {
    val year: Int get() = releaseDate.take(4).toIntOrNull() ?: 0
    val posterUrl: String get() = if (posterPath != null) "https://image.tmdb.org/t/p/w500$posterPath" else ""
    val backdropUrl: String get() = if (backdropPath != null) "https://image.tmdb.org/t/p/w1280$backdropPath" else ""
}

@Immutable
data class TmdbTvDetail(
    @SerializedName("id") val id: Int = 0,
    @SerializedName("name") val name: String = "",
    @SerializedName("original_name") val originalName: String = "",
    @SerializedName("overview") val overview: String = "",
    @SerializedName("poster_path") val posterPath: String? = null,
    @SerializedName("backdrop_path") val backdropPath: String? = null,
    @SerializedName("first_air_date") val firstAirDate: String = "",
    @SerializedName("vote_average") val voteAverage: Double = 0.0,
    @SerializedName("vote_count") val voteCount: Int = 0,
    @SerializedName("genres") val genres: List<TmdbGenre> = emptyList(),
    @SerializedName("number_of_seasons") val numberOfSeasons: Int = 0,
    @SerializedName("number_of_episodes") val numberOfEpisodes: Int = 0,
    @SerializedName("seasons") val seasons: List<TmdbSeason> = emptyList(),
    @SerializedName("status") val status: String = "",
    @SerializedName("tagline") val tagline: String = "",
    @SerializedName("episode_run_time") val episodeRunTime: List<Int> = emptyList()
) {
    val year: Int get() = firstAirDate.take(4).toIntOrNull() ?: 0
    val posterUrl: String get() = if (posterPath != null) "https://image.tmdb.org/t/p/w500$posterPath" else ""
    val backdropUrl: String get() = if (backdropPath != null) "https://image.tmdb.org/t/p/w1280$backdropPath" else ""
}

@Immutable
data class TmdbGenre(
    @SerializedName("id") val id: Int = 0,
    @SerializedName("name") val name: String = ""
)

@Immutable
data class TmdbSeason(
    @SerializedName("id") val id: Int = 0,
    @SerializedName("season_number") val seasonNumber: Int = 0,
    @SerializedName("name") val name: String = "",
    @SerializedName("episode_count") val episodeCount: Int = 0,
    @SerializedName("poster_path") val posterPath: String? = null,
    @SerializedName("air_date") val airDate: String? = null,
    @SerializedName("overview") val overview: String = ""
)

@Immutable
data class TmdbSeasonDetail(
    @SerializedName("id") val id: Int = 0,
    @SerializedName("season_number") val seasonNumber: Int = 0,
    @SerializedName("name") val name: String = "",
    @SerializedName("episodes") val episodes: List<TmdbEpisode> = emptyList()
)

@Immutable
data class TmdbEpisode(
    @SerializedName("id") val id: Int = 0,
    @SerializedName("episode_number") val episodeNumber: Int = 0,
    @SerializedName("name") val name: String = "",
    @SerializedName("overview") val overview: String = "",
    @SerializedName("still_path") val stillPath: String? = null,
    @SerializedName("air_date") val airDate: String? = null,
    @SerializedName("runtime") val runtime: Int? = null,
    @SerializedName("vote_average") val voteAverage: Double = 0.0
) {
    val thumbUrl: String get() = if (stillPath != null) "https://image.tmdb.org/t/p/w300$stillPath" else ""
}

@Immutable
data class TmdbTrendingResponse(
    @SerializedName("results") val results: List<TmdbSearchItem> = emptyList(),
    @SerializedName("page") val page: Int = 1,
    @SerializedName("total_pages") val totalPages: Int = 0
)

// ═══ ShowBox ═══

data class ShowBoxShareResponse(
    @SerializedName("code") val code: Int = 0,
    @SerializedName("data") val data: ShowBoxShareData? = null,
    @SerializedName("msg") val msg: String = ""
)

data class ShowBoxShareData(
    @SerializedName("link") val link: String = ""
) {
    val shareKey: String get() = link.substringAfterLast("/share/", "")
}

// ═══ FebBox (parsed from WebView HTML/JS) ═══

@Immutable
data class FebBoxFile(
    val fid: Long = 0,
    val name: String = "",
    val size: String = "",
    val isFolder: Boolean = false,
    val thumbUrl: String = "",
    val updateTime: String = ""
)

@Immutable
data class FebBoxStream(
    val url: String = "",
    val quality: String = "",    // "AUTO", "1080p", "720p", "360p"
    val type: String = "hls"     // "hls" (m3u8)
)
