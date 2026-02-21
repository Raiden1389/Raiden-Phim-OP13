package xyz.raidenhub.phim.widget

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.*
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.items
import androidx.glance.appwidget.provideContent
import androidx.glance.layout.*
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import xyz.raidenhub.phim.MainActivity

/**
 * N-3 — "Xem tiếp" Home Screen Widget (4×2)
 * Hiển thị các phim đang xem dở, tap → mở app và navigate đến Detail
 */
class ContinueWatchingWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        // Load continue list from SharedPreferences directly (widget runs in separate process)
        val items = loadContinueItems(context).take(4)

        provideContent {
            WidgetContent(items, context)
        }
    }

    @Composable
    private fun WidgetContent(
        items: List<WidgetContinueItem>,
        context: Context
    ) {
        val bgColor = ColorProvider(Color(0xFF1A1A2E))
        val textPrimary = ColorProvider(Color.White)
        val textSecondary = ColorProvider(Color(0xFFB0B0C0))
        val accentColor = ColorProvider(Color(0xFFE94560))
        val surfaceColor = ColorProvider(Color(0xFF16213E))

        Box(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(bgColor)
                .padding(12.dp)
        ) {
            if (items.isEmpty()) {
                // Empty state
                Column(
                    modifier = GlanceModifier.fillMaxSize(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "▶️",
                        style = TextStyle(fontSize = 28.sp)
                    )
                    Spacer(GlanceModifier.height(8.dp))
                    Text(
                        "Chưa có phim đang xem",
                        style = TextStyle(
                            color = textSecondary,
                            fontSize = 13.sp
                        )
                    )
                }
            } else {
                Column(modifier = GlanceModifier.fillMaxSize()) {
                    // Header
                    Row(
                        modifier = GlanceModifier.fillMaxWidth().padding(bottom = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "▶ Xem tiếp",
                            style = TextStyle(
                                color = accentColor,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }

                    // Item list
                    items.forEach { item ->
                        val intent = Intent(context, MainActivity::class.java).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
                            putExtra("navigate_to", "detail/${item.slug}")
                        }

                        Row(
                            modifier = GlanceModifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .background(surfaceColor)
                                .clickable(actionStartActivity(intent))
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Progress indicator strip
                            Box(
                                modifier = GlanceModifier
                                    .width(3.dp)
                                    .height(32.dp)
                                    .background(accentColor)
                            ) {}

                            Spacer(GlanceModifier.width(10.dp))

                            Column(modifier = GlanceModifier.defaultWeight()) {
                                Text(
                                    item.name,
                                    style = TextStyle(
                                        color = textPrimary,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold
                                    ),
                                    maxLines = 1
                                )
                                Text(
                                    buildString {
                                        if (item.epName.isNotBlank()) append("Tập ${item.epName} • ")
                                        val pct = (item.progress * 100).toInt()
                                        append("$pct%")
                                    },
                                    style = TextStyle(
                                        color = textSecondary,
                                        fontSize = 11.sp
                                    )
                                )
                            }

                            Text(
                                "›",
                                style = TextStyle(
                                    color = textSecondary,
                                    fontSize = 18.sp
                                )
                            )
                        }

                        Spacer(GlanceModifier.height(2.dp))
                    }
                }
            }
        }
    }

    private fun loadContinueItems(context: Context): List<WidgetContinueItem> {
        return try {
            val prefs = context.getSharedPreferences("watch_history", Context.MODE_PRIVATE)
            val json = prefs.getString("continue_list", null) ?: return emptyList()
            val arr = Gson().fromJson(json, Array<WidgetContinueItem>::class.java)
            arr?.toList() ?: emptyList()
        } catch (_: Exception) {
            emptyList()
        }
    }

    /** Lightweight mirror of ContinueItem for widget (no Flow dependency) */
    data class WidgetContinueItem(
        @SerializedName("slug") val slug: String = "",
        @SerializedName("name") val name: String = "",
        @SerializedName("thumbUrl") val thumbUrl: String = "",
        @SerializedName("episode") val episode: Int = 0,
        @SerializedName("epName") val epName: String = "",
        @SerializedName("positionMs") val positionMs: Long = 0,
        @SerializedName("durationMs") val durationMs: Long = 0,
    ) {
        val progress: Float get() = if (durationMs > 0) positionMs.toFloat() / durationMs else 0f
    }
}

/** BroadcastReceiver để Android biết widget này tồn tại */
class ContinueWatchingWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget = ContinueWatchingWidget()
}
