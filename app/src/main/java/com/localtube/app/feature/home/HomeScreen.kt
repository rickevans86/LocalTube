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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.lazy.items as rowItems
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Tv
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.localtube.app.data.ContentRepository
import com.localtube.app.data.FolderType
import com.localtube.app.data.db.ChannelRow
import com.localtube.app.data.db.FolderEntity
import com.localtube.app.di.AppContainer
import com.localtube.app.ui.VideoCard
import com.localtube.app.ui.theme.CategoryColors
import com.localtube.app.ui.theme.Coral
import com.localtube.app.ui.theme.Ink
import com.localtube.app.ui.theme.KidBg
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
    val channels by vm.channels.collectAsState()
    val selected by vm.selected.collectAsState()
    val folderVideos by vm.folderVideos.collectAsState()

    Column(modifier = Modifier.fillMaxSize().background(KidBg)) {
        TopBar(onOpenParentArea)

        if (folders.isNotEmpty()) {
            CategoryBar(folders = folders, selected = selected, onSelect = vm::select)
        }

        if (feed.isEmpty() && channels.isEmpty() && folders.isEmpty()) {
            EmptyState(onOpenParentArea)
            return@Column
        }

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier.weight(1f).padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            contentPadding = PaddingValues(vertical = 12.dp),
        ) {
            if (selected != null) {
                gridItems(folderVideos) { video -> VideoCard(video, onOpenVideo) }
            } else {
                if (channels.isNotEmpty()) {
                    item(span = { GridItemSpan(2) }) { SectionLabel("Shows") }
                    item(span = { GridItemSpan(2) }) {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                            rowItems(channels) { channel -> ChannelCard(channel, onOpenChannel) }
                        }
                    }
                    item(span = { GridItemSpan(2) }) { SectionLabel("Videos") }
                }
                gridItems(feed) { video -> VideoCard(video, onOpenVideo) }
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
        Text("LocalTube", fontSize = 26.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
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
            CategoryChip("All", Icons.Default.Apps, Coral, selected == null) { onSelect(null) }
        }
        itemsIndexed(folders) { index, folder ->
            val color = CategoryColors[(index + 1) % CategoryColors.size]
            CategoryChip(folder.name, iconFor(folder.type), color, selected == folder.id) {
                onSelect(folder.id)
            }
        }
    }
}

private fun iconFor(type: String): ImageVector = when (FolderType.fromName(type)) {
    FolderType.MOVIES -> Icons.Default.Movie
    FolderType.CHANNELS -> Icons.Default.Tv
    FolderType.VIDEOS -> Icons.Default.PlayCircle
}

@Composable
private fun CategoryChip(
    label: String,
    icon: ImageVector,
    accent: Color,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(if (selected) Ink else Color.White)
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = if (selected) Color.White else accent,
            modifier = Modifier.size(18.dp),
        )
        Text(
            label,
            color = if (selected) Color.White else Ink,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            modifier = Modifier.padding(start = 6.dp),
        )
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(text, fontSize = 20.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 8.dp))
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
