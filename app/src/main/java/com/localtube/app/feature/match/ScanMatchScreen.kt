package com.localtube.app.feature.match

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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import coil.compose.AsyncImage
import com.localtube.app.data.ArtworkClient
import com.localtube.app.data.ContentRepository
import com.localtube.app.data.NameParser
import com.localtube.app.data.db.MetaEntity
import com.localtube.app.di.AppContainer
import com.localtube.app.ui.theme.Coral
import com.localtube.app.ui.theme.SettingsBg
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** One row to show in the scan/match list. */
data class MatchItem(val kind: String, val key: String, val label: String, val query: String)

class ScanMatchViewModel(private val repo: ContentRepository) : ViewModel() {
    val meta = repo.meta.map { list -> list.associateBy { it.itemKey } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())

    val items = combineItems()

    private fun combineItems() = kotlinx.coroutines.flow.combine(repo.movies, repo.channels) { movies, channels ->
        val m = movies.map {
            MatchItem("movie", it.documentUri, it.title, NameParser.cleanTitle(it.title))
        }
        val s = channels.map {
            MatchItem("series", ContentRepository.seriesKey(it.folderId, it.name), it.name, NameParser.cleanTitle(it.name))
        }
        m + s
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _scanning = MutableStateFlow(false)
    val scanning = _scanning.asStateFlow()

    fun rescan() {
        viewModelScope.launch {
            _scanning.value = true
            try {
                repo.matchAll()
            } finally {
                _scanning.value = false
            }
        }
    }

    suspend fun lookup(kind: String, text: String): List<ArtworkClient.SearchResult> {
        val q = text.trim()
        if (q.isEmpty()) return emptyList()
        return if (q.startsWith("tt")) listOfNotNull(ArtworkClient.metaById(kind, q)) else ArtworkClient.search(kind, q)
    }

    fun apply(item: MatchItem, result: ArtworkClient.SearchResult) {
        viewModelScope.launch {
            repo.saveMeta(
                MetaEntity(
                    itemKey = item.key,
                    kind = item.kind,
                    query = item.query,
                    imdbId = result.imdbId,
                    title = result.title,
                    year = result.year,
                    posterUrl = ArtworkClient.posterUrl(result.imdbId),
                    backdropUrl = ArtworkClient.backdropUrl(result.imdbId),
                    manual = true,
                    updatedAt = System.currentTimeMillis(),
                ),
            )
        }
    }
}

@Composable
fun ScanMatchScreen(container: AppContainer, onBack: () -> Unit) {
    val vm: ScanMatchViewModel = viewModel(
        factory = viewModelFactory { initializer { ScanMatchViewModel(container.repository) } },
    )
    val items by vm.items.collectAsState()
    val meta by vm.meta.collectAsState()
    val scanning by vm.scanning.collectAsState()
    var editing by remember { mutableStateOf<MatchItem?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SettingsBg)
            .windowInsetsPadding(WindowInsets.systemBars)
            .padding(horizontal = 20.dp),
    ) {
        Row(modifier = Modifier.padding(vertical = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
            Text("Scan movies & TV shows", fontSize = 22.sp, fontWeight = FontWeight.Bold)
        }

        Button(
            onClick = { vm.rescan() },
            enabled = !scanning,
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
        ) {
            if (scanning) {
                CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                Text("  Matching…", fontWeight = FontWeight.Bold)
            } else {
                Text("Rescan & match", fontWeight = FontWeight.Bold)
            }
        }

        Text(
            "Posters are matched automatically. Tap any item to fix a wrong match — search by name or paste an IMDb id (tt…).",
            color = Color.Gray,
            fontSize = 13.sp,
            modifier = Modifier.padding(bottom = 10.dp),
        )

        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.weight(1f)) {
            items(items) { item ->
                MatchRow(item, meta[item.key]) { editing = item }
            }
        }
    }

    editing?.let { item ->
        FixDialog(vm, item, onDismiss = { editing = null })
    }
}

@Composable
private fun MatchRow(item: MatchItem, matched: MetaEntity?, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color.White)
            .clickable { onClick() }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier.width(48.dp).aspectRatio(2f / 3f).clip(RoundedCornerShape(6.dp)).background(Color(0xFFE6E6E6)),
            contentAlignment = Alignment.Center,
        ) {
            if (matched?.posterUrl != null) {
                AsyncImage(
                    model = matched.posterUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
        Column(modifier = Modifier.weight(1f).padding(start = 12.dp)) {
            Text(item.label, fontSize = 14.sp, fontWeight = FontWeight.Bold, maxLines = 1)
            val sub = when {
                matched == null -> "No match yet"
                matched.manual -> "Fixed: ${matched.title ?: "—"}"
                else -> "${matched.title ?: "—"}${matched.year?.let { " ($it)" } ?: ""}"
            }
            Text(sub, fontSize = 12.sp, color = if (matched == null) Coral else Color.Gray, maxLines = 1)
        }
        Text("Fix", color = Coral, fontWeight = FontWeight.Bold, fontSize = 13.sp)
    }
}

@Composable
private fun FixDialog(vm: ScanMatchViewModel, item: MatchItem, onDismiss: () -> Unit) {
    var text by remember { mutableStateOf(item.query) }
    var results by remember { mutableStateOf<List<ArtworkClient.SearchResult>>(emptyList()) }
    var searching by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(Color.White)
                .padding(18.dp),
        ) {
            Text("Fix match", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Text(item.label, fontSize = 13.sp, color = Color.Gray, maxLines = 1, modifier = Modifier.padding(bottom = 10.dp))

            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                label = { Text("Title or IMDb id (tt…)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Button(
                onClick = {
                    scope.launch {
                        searching = true
                        results = vm.lookup(item.kind, text)
                        searching = false
                    }
                },
                modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
            ) {
                Text(if (searching) "Searching…" else "Search")
            }

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth().heightIn(max = 320.dp).padding(top = 12.dp),
            ) {
                items(results) { r ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFFF4F4F4))
                            .clickable {
                                vm.apply(item, r)
                                onDismiss()
                            }
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            modifier = Modifier.width(36.dp).aspectRatio(2f / 3f).clip(RoundedCornerShape(5.dp)).background(Color(0xFFDDDDDD)),
                        ) {
                            val poster = r.poster ?: ArtworkClient.posterUrl(r.imdbId)
                            AsyncImage(
                                model = poster,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize(),
                            )
                        }
                        Column(modifier = Modifier.weight(1f).padding(start = 10.dp)) {
                            Text(r.title, fontSize = 13.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                            Text("${r.year ?: ""}  ${r.imdbId}", fontSize = 11.sp, color = Color.Gray, maxLines = 1)
                        }
                    }
                }
            }
        }
    }
}
