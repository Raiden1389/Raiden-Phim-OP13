package xyz.raidenhub.phim.notification

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.*
import xyz.raidenhub.phim.MainActivity
import xyz.raidenhub.phim.data.local.FavoriteManager
import xyz.raidenhub.phim.data.repository.MovieRepository
import java.util.concurrent.TimeUnit

/**
 * #34 — New Episode Notification
 * Periodically checks favorites for new episodes and sends notifications.
 * Uses WorkManager for background scheduling (every 6 hours).
 */
class EpisodeCheckWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    companion object {
        const val CHANNEL_ID = "new_episodes"
        const val CHANNEL_NAME = "Tập mới"
        const val WORK_NAME = "episode_check"
        private const val PREF_NAME = "episode_tracker"

        /**
         * Schedule periodic episode checks (every 6 hours)
         */
        fun schedule(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val request = PeriodicWorkRequestBuilder<EpisodeCheckWorker>(
                6, TimeUnit.HOURS,
                30, TimeUnit.MINUTES  // flex interval
            )
                .setConstraints(constraints)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 15, TimeUnit.MINUTES)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }

        /**
         * Cancel periodic checks
         */
        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
        }

        /**
         * Create notification channel (required for Android 8+)
         */
        fun createChannel(context: Context) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description = "Thông báo khi có tập phim mới"
                }
                val manager = context.getSystemService(NotificationManager::class.java)
                manager.createNotificationChannel(channel)
            }
        }
    }

    override suspend fun doWork(): Result {
        val favorites = FavoriteManager.getFavoritesOnce()
        if (favorites.isEmpty()) return Result.success()

        val prefs = applicationContext.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val editor = prefs.edit()
        var newEpisodesFound = 0

        for (fav in favorites.take(20)) { // Limit to 20 to avoid rate-limiting
            try {
                MovieRepository.getMovieDetail(fav.slug).onSuccess { detail ->
                    val movie = detail.movie
                    val totalEps = detail.episodes
                        .flatMap { it.serverData }
                        .size

                    // Compare with last known episode count
                    val key = "eps_${fav.slug}"
                    val lastKnown = prefs.getInt(key, 0)

                    if (lastKnown > 0 && totalEps > lastKnown) {
                        // New episodes found!
                        val newCount = totalEps - lastKnown
                        sendNotification(
                            title = "🎬 ${movie.name}",
                            message = "Có $newCount tập mới! (Tổng: $totalEps tập)",
                            slug = fav.slug,
                            notificationId = fav.slug.hashCode()
                        )
                        newEpisodesFound++
                    }

                    // Save current count
                    editor.putInt(key, totalEps)
                }
            } catch (_: Exception) { /* Skip failed requests */ }
        }

        editor.apply()
        return Result.success()
    }

    private fun sendNotification(title: String, message: String, slug: String, notificationId: Int) {
        // Check permission for Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    applicationContext,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) return
        }

        val intent = Intent(applicationContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("navigate_to", "detail/$slug")
        }

        val pendingIntent = PendingIntent.getActivity(
            applicationContext, notificationId, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(applicationContext).notify(notificationId, notification)
    }
}
