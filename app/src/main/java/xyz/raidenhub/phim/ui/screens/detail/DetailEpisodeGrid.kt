package xyz.raidenhub.phim.ui.screens.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import xyz.raidenhub.phim.data.api.models.Episode
import xyz.raidenhub.phim.data.api.models.EpisodeServer
import xyz.raidenhub.phim.data.local.ContinueItem
import xyz.raidenhub.phim.ui.theme.C
import xyz.raidenhub.phim.ui.theme.JakartaFamily

/** Server tabs — select which server to view episodes from */
@Composable
fun DetailServerTabs(
    episodes: List<EpisodeServer>,
    selectedServer: Int,
    onServerSelect: (Int) -> Unit
) {
    if (episodes.size > 1) {
        Row(Modifier.padding(horizontal = 16.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            episodes.forEachIndexed { idx, server ->
                val isActive = idx == selectedServer
                Text(
                    text = server.serverName.ifBlank { "Server ${idx + 1}" },
                    color = if (isActive) C.TextPrimary else C.TextSecondary,
                    fontSize = 13.sp,
                    fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isActive) C.Primary else C.Surface)
                        .clickable { onServerSelect(idx) }
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                )
            }
        }
    }
}

/** #21 — Episode grid with progress bars + D-8 sort toggle */
@Composable
fun DetailEpisodeGrid(
    eps: List<Episode>,
    slug: String,
    selectedServer: Int,
    watchedSet: Set<Int>,
    continueItem: ContinueItem?,
    episodeSortAsc: Boolean,
    onToggleSort: () -> Unit,
    onPlay: (slug: String, server: Int, episode: Int) -> Unit
) {
    val displayEps = if (episodeSortAsc) eps else eps.reversed()

    // Header + sort toggle
    Row(
        modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("Danh sách tập", color = C.TextPrimary, fontFamily = JakartaFamily, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        if (eps.size > 1) {
            Text(
                if (episodeSortAsc) "↓ Mới nhất" else "↑ Tập 1",
                color = C.Primary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(C.Primary.copy(0.15f))
                    .clickable { onToggleSort() }
                    .padding(horizontal = 10.dp, vertical = 5.dp)
            )
        }
    }

    // Episode grid
    Column(Modifier.padding(horizontal = 12.dp).padding(bottom = 8.dp)) {
        displayEps.chunked(5).forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                row.forEachIndexed { _, ep ->
                    val epIdx = eps.indexOf(ep)
                    val isWatched = watchedSet.contains(epIdx)
                    val epLabel = ep.name.ifBlank { "Tập ${epIdx + 1}" }
                    val epProgress = if (continueItem?.episode == epIdx) continueItem.progress else if (isWatched) 1f else 0f

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isWatched) "✓ $epLabel" else epLabel,
                            color = if (isWatched) C.Primary else C.TextPrimary,
                            fontSize = 13.sp,
                            fontWeight = if (isWatched) FontWeight.Bold else FontWeight.Normal,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                                .background(if (isWatched) C.Primary.copy(0.15f) else C.Surface)
                                .clickable { onPlay(slug, selectedServer, epIdx) }
                                .padding(vertical = 10.dp)
                                .wrapContentWidth(Alignment.CenterHorizontally)
                        )
                        if (epProgress > 0f && epProgress < 1f) {
                            LinearProgressIndicator(
                                progress = { epProgress },
                                modifier = Modifier.fillMaxWidth().height(3.dp),
                                color = C.Primary,
                                trackColor = C.Surface
                            )
                        } else if (isWatched) {
                            Box(Modifier.fillMaxWidth().height(3.dp).background(C.Primary))
                        } else {
                            Spacer(Modifier.fillMaxWidth().height(3.dp))
                        }
                    }
                }
                repeat(5 - row.size) {
                    Column(Modifier.weight(1f)) {
                        Spacer(Modifier.fillMaxWidth().height(43.dp))
                    }
                }
            }
            Spacer(Modifier.height(6.dp))
        }
    }
}
