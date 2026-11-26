package com.localtube.app.feature.channel

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.localtube.app.data.ContentRepository
import com.localtube.app.data.db.VideoEntity
import com.localtube.app.di.AppContainer
import com.localtube.app.ui.VideoThumb
import com.localtube.app.ui.theme.Blue
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn

class ChannelViewModel(repo: ContentRepository, folderId: Long, channel: String) : ViewModel() {
    val episodes = repo.channelEpisodes(folderId, channel)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
}

@Composable
fun ChannelScreen(
    container: AppContainer,
    folderId: Long,
    channel: String,
    onOpenVideo: (String) -> Unit,
    onBack: () -> Unit,
) {
    val vm: ChannelViewModel = viewModel(
        factory = viewModelFactory { initializer { ChannelViewModel(container.repository, folderId, channel) } },
    )
    val episodes by vm.episodes.collectAsState()

    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        modifier = Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.systemBars),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item(span = { GridItemSpan(3) }) {
            Column(modifier = Modifier.fillMaxWidth().background(Blue).padding(20.dp)) {
                Row {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                }
                Text(channel, color = Color.White, fontSize = 30.sp, fontWeight = FontWeight.Bold)
                Text("Channel · ${episodes.size} episodes", color = Color.White, fontSize = 14.sp)
            }
        }
        items(episodes) { ep -> EpisodeCard(ep, onOpenVideo) }
    }
}

@Composable
private fun EpisodeCard(ep: VideoEntity, onOpen: (String) -> Unit) {
    Column(modifier = Modifier.clickable { onOpen(ep.documentUri) }.padding(horizontal = 16.dp)) {
        Box(
            modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f).clip(RoundedCornerShape(16.dp)).background(Blue),
        ) {
            VideoThumb(ep.documentUri, modifier = Modifier.fillMaxSize())
        }
        Text(ep.title, fontSize = 13.sp, fontWeight = FontWeight.Bold, maxLines = 2, modifier = Modifier.padding(top = 6.dp))
    }
}
