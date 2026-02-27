package xyz.raidenhub.phim.ui.screens.player

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import xyz.raidenhub.phim.data.api.models.Episode
import xyz.raidenhub.phim.ui.theme.C
import xyz.raidenhub.phim.ui.theme.JakartaFamily
import xyz.raidenhub.phim.ui.theme.InterFamily

/**
 * PlayerEpisodeSheet — Episode list bottom sheet.
 * Extracted from PlayerScreen.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerEpisodeSheet(
    showSheet: Boolean,
    episodes: List<Episode>,
    currentEp: Int,
    onEpisodeSelect: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    if (showSheet && episodes.size > 1) {
        ModalBottomSheet(
            onDismissRequest = onDismiss,
            containerColor = Color(0xFF1A1A2E),
            contentColor = Color.White,
            tonalElevation = 0.dp
        ) {
            Column(
                modifier = Modifier.fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 32.dp)
            ) {
                Text(
                    "📋 Danh sách tập (${episodes.size})",
                    fontFamily = JakartaFamily, fontWeight = FontWeight.Bold,
                    fontSize = 18.sp, color = Color.White,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                androidx.compose.foundation.lazy.grid.LazyVerticalGrid(
                    columns = androidx.compose.foundation.lazy.grid.GridCells.Adaptive(minSize = 72.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.heightIn(max = 400.dp)
                ) {
                    items(episodes.size) { idx ->
                        val isCurrentEp = idx == currentEp
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (isCurrentEp) C.Primary else Color.White.copy(0.1f),
                            border = if (isCurrentEp)
                                androidx.compose.foundation.BorderStroke(2.dp, C.Primary)
                            else null,
                            modifier = Modifier.height(44.dp).clickable { onEpisodeSelect(idx) }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    cleanEpName(episodes[idx].name),
                                    color = if (isCurrentEp) Color.White else Color.White.copy(0.8f),
                                    fontFamily = InterFamily, fontSize = 13.sp,
                                    fontWeight = if (isCurrentEp) FontWeight.Bold else FontWeight.Normal,
                                    textAlign = TextAlign.Center, maxLines = 1
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
