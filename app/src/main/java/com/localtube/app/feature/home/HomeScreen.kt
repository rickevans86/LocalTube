package com.localtube.app.feature.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.lazy.items as rowItems
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.runtime.collectAsState
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
import com.localtube.app.data.db.FolderEntity
import com.localtube.app.data.db.VideoEntity
import com.localtube.app.di.AppContainer
import com.localtube.app.ui.VideoThumb
import com.localtube.app.ui.theme.CategoryColors
import com.localtube.app.ui.theme.Coral
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModel(repo: ContentRepository) : ViewModel() {
    val folders = repo.folders.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val feed = repo.homeFeed.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val movies = repo.movies.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val channels = repo.channels.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val selectedId = MutableStateFlow<Long?>(null)
    val selected = selectedId.asStateFlow()

    val folderVideos = selectedId.flatMapLatest { id ->
        if (id == null) flowOf(emptyList()) else repo.videosInFolder(id)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun select(id: Long?) {
        selectedId.value = id
    }
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
    val folders by vm.folders.collectAsState()
    val feed by vm.feed.collectAsState()
    val movies by vm.movies.collectAsState()
    val channels by vm.channels.collectAsState()
    val selected by vm.selected.collectAsState()
    val folderVideos by vm.folderVideos.collectAsState()

    Column(modifier = Modifier.fillMaxSize()) {
        TopBar(onOpenParentArea)

        if (folders.isNotEmpty()) {
            CategoryBar(folders = folders, selected = selected, onSelect = vm::select)
        }

        val nothing = feed.isEmpty() && movies.isEmpty() && channels.isEmpty() && folders.isEmpty()
        when {
            nothing -> EmptyState(onOpenParentArea)

            selected != null -> LazyVerticalGrid(
                columns = GridCells.Fixed(4),
                modifier = Modifier.weight(1f).padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                contentPadding = PaddingValues(vertical = 12.dp),
            ) {
                gridItems(folderVideos) { video -> VideoCard(video, onOpenVideo) }
            }

            else -> LazyVerticalGrid(
                columns = GridCells.Fixed(4),
                modifier = Modifier.weight(1f).padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                contentPadding = PaddingValues(vertical = 12.dp),
            ) {
                if (feed.isNotEmpty()) {
                    item(span = { GridItemSpan(4) }) { SectionLabel("Shuffled for you") }
                    gridItems(feed) { video -> VideoCard(video, onOpenVideo) }
                }
                if (movies.isNotEmpty()) {
                    item(span = { GridItemSpan(4) }) { SectionLabel("Movies") }
                    item(span = { GridItemSpan(4) }) {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                            rowItems(movies) { movie -> PosterCard(movie, onOpenVideo) }
                        }
                    }
                }
                if (channels.isNotEmpty()) {
                    item(span = { GridItemSpan(4) }) { SectionLabel("Shows") }
                    item(span = { GridItemSpan(4) }) {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                            rowItems(channels) { channel -> ChannelCard(channel, onOpenChannel) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TopBar(onOpenParentArea: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 14.dp),
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
private fun CategoryBar(folders: List<FolderEntity>, selected: Long?, onSelect: (Long?) -> Unit) {
    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            CategoryChip(label = "All", color = Coral, selected = selected == null) { onSelect(null) }
        }
        itemsIndexed(folders) { index, folder ->
            val color = CategoryColors[(index + 1) % CategoryColors.size]
            CategoryChip(label = folder.name, color = color, selected = selected == folder.id) {
                onSelect(folder.id)
            }
        }
    }
}

@Composable
private fun CategoryChip(label: String, color: Color, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(if (selected) color else color.copy(alpha = 0.15f))
            .clickable { onClick() }
            .padding(horizontal = 18.dp, vertical = 10.dp),
    ) {
        Text(
            label,
            color = if (selected) Color.White else color,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
        )
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
