package com.localtube.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.localtube.app.data.db.VideoEntity
import com.localtube.app.ui.theme.CategoryColors
import kotlin.math.abs

/** mm:ss or h:mm:ss, or null when duration is unknown. */
fun formatDuration(ms: Long): String? {
    if (ms <= 0) return null
    val totalSec = ms / 1000
    val h = totalSec / 3600
    val m = (totalSec % 3600) / 60
    val s = totalSec % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%d:%02d".format(m, s)
}

private fun avatarColor(seed: String): Color =
    CategoryColors[abs(seed.hashCode()) % CategoryColors.size]

/** YouTube Kids–style landscape card: thumbnail + duration, then avatar + title. */
@Composable
fun VideoCard(video: VideoEntity, onOpen: (String) -> Unit) {
    Column(modifier = Modifier.clickable { onOpen(video.documentUri) }) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0xFFCCD6DB)),
        ) {
            VideoThumb(video.documentUri, modifier = Modifier.fillMaxSize())
            formatDuration(video.durationMs)?.let { dur ->
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(6.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color.Black.copy(alpha = 0.7f))
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                ) {
                    Text(dur, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
        Row(modifier = Modifier.padding(top = 8.dp)) {
            Box(
                modifier = Modifier.size(32.dp).clip(CircleShape).background(avatarColor(video.title)),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    video.title.take(1).uppercase(),
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                )
            }
            Column(modifier = Modifier.padding(start = 10.dp)) {
                Text(
                    video.title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                )
                video.channelName?.let {
                    Text(it, fontSize = 12.sp, color = Color.Gray, maxLines = 1, modifier = Modifier.padding(top = 2.dp))
                }
            }
        }
    }
}
