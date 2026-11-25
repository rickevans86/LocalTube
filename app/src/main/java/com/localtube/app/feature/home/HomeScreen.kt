package com.localtube.app.feature.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
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
import com.localtube.app.data.db.ChannelRow
import com.localtube.app.data.db.VideoEntity
import com.localtube.app.di.AppContainer
import com.localtube.app.ui.VideoThumb
import com.localtube.app.ui.theme.CategoryColors
import com.localtube.app.ui.theme.Coral
import androidx.compose.runtime.collectAsState
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn

class HomeViewModel(repo: ContentRepository) : ViewModel() {
    val feed = repo.homeFeed.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val movies = repo.movies.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val channels = repo.channels.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
}

@Composable
fun HomeScreen(
    container: AppContainer,
    onOpenVideo: (String) -> Unit,
    onOpenChannel: (Long, String) -> Unit,
    onOpenParentArea: () -> Unit,
) {
    val vm: HomeViewModel = viewModel(
        factory = viewModelFactory { initializer { HomeViewModel(container.repository) } },
    )
    val feed by vm.feed.collectAsState()
    val movies by vm.movies.collectAsState()
    val channels by vm.channels.collectAsState()

    LazyVerticalGrid(
        columns = GridCells.Fixed(4),
        modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(4) }) {
            TopBar(onOpenParentArea)
        }

        if (feed.isEmpty() && movies.isEmpty() && channels.isEmpty()) {
            item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(4) }) {
                EmptyState(onOpenParentArea)
            }
            return@LazyVerticalGrid
        }

        if (feed.isNotEmpty()) {
            item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(4) }) {
                SectionLabel("Shuffled for you")
            }
            items(feed) { video -> VideoCard(video, onOpenVideo) }
        }

        if (movies.isNotEmpty()) {
            item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(4) }) {
                SectionLabel("Movies")
            }
            item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(4) }) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    items(movies) { movie -> PosterCard(movie, onOpenVideo) }
                }
            }
        }

        if (channels.isNotEmpty()) {
            item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(4) }) {
                SectionLabel("Shows")
            }
            item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(4) }) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    items(channels) { channel -> ChannelCard(channel, onOpenChannel) }
                }
            }
        }
    }
}

@Composable
private fun TopBar(onOpenParentArea: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Default.Search, contentDescription = "Search", tint = Coral)
        Text(
            "LocalTube",
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 12.dp).weight(1f),
        )
        IconButton(onClick = onOpenParentArea) {
            Icon(Icons.Default.Lock, contentDescription = "Parent area")
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(text, fontSize = 20.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 8.dp))
}

@Composable
private fun VideoCard(video: VideoEntity, onOpen: (String) -> Unit, color: Color = Coral) {
    Column(modifier = Modifier.clickable { onOpen(video.documentUri) }) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .clip(RoundedCornerShape(16.dp))
                .background(color),
            contentAlignment = Alignment.Center,
        ) {
            VideoThumb(video.documentUri, modifier = Modifier.fillMaxSize())
            Box(
                modifier = Modifier.size(44.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.9f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = null, tint = color)
            }
        }
        Text(
            video.title,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 2,
            modifier = Modifier.padding(top = 6.dp),
        )
    }
}

@Composable
private fun PosterCard(movie: VideoEntity, onOpen: (String) -> Unit) {
    Column(modifier = Modifier.width(150.dp).clickable { onOpen(movie.documentUri) }) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(2f / 3f)
                .clip(RoundedCornerShape(16.dp))
                .background(CategoryColors.random()),
        ) {
            VideoThumb(movie.documentUri, modifier = Modifier.fillMaxSize())
        }
        Text(movie.title, fontSize = 13.sp, fontWeight = FontWeight.Bold, maxLines = 2, modifier = Modifier.padding(top = 6.dp))
    }
}

@Composable
private fun ChannelCard(channel: ChannelRow, onOpen: (Long, String) -> Unit) {
    Column(
        modifier = Modifier.width(280.dp).clickable { onOpen(channel.folderId, channel.name) },
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 7f)
                .clip(RoundedCornerShape(16.dp))
                .background(CategoryColors.random())
                .padding(14.dp),
        ) {
            Text(channel.name, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        }
        Text(
            "Channel · ${channel.episodeCount} episodes",
            fontSize = 12.sp,
            color = Color.Gray,
            modifier = Modifier.padding(top = 6.dp),
        )
    }
}

@Composable
private fun EmptyState(onOpenParentArea: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(top = 80.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("No videos yet", fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Text(
            "A grown-up can add folders in the parent area.",
            fontSize = 14.sp,
            color = Color.Gray,
            modifier = Modifier.padding(top = 8.dp),
        )
        Box(
            modifier = Modifier
                .padding(top = 20.dp)
                .clip(RoundedCornerShape(999.dp))
                .background(Coral)
                .clickable { onOpenParentArea() }
                .padding(horizontal = 24.dp, vertical = 12.dp),
        ) {
            Text("Open parent area", color = Color.White, fontWeight = FontWeight.Bold)
        }
    }
}
