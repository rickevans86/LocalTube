package com.localtube.app.feature.search

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.localtube.app.data.ContentRepository
import com.localtube.app.di.AppContainer
import com.localtube.app.ui.VideoCard
import com.localtube.app.ui.theme.KidBg
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn

@OptIn(ExperimentalCoroutinesApi::class)
class SearchViewModel(repo: ContentRepository) : ViewModel() {
    private val queryFlow = MutableStateFlow("")
    val query = queryFlow.asStateFlow()

    val results = queryFlow.flatMapLatest { q ->
        if (q.isBlank()) flowOf(emptyList()) else repo.search(q.trim())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun setQuery(value: String) {
        queryFlow.value = value
    }
}

@Composable
fun SearchScreen(container: AppContainer, onOpenVideo: (String) -> Unit) {
    val vm: SearchViewModel = viewModel(
        factory = viewModelFactory { initializer { SearchViewModel(container.repository) } },
    )
    val query by vm.query.collectAsState()
    val results by vm.results.collectAsState()

    Column(modifier = Modifier.fillMaxSize().background(KidBg).padding(horizontal = 20.dp)) {
        OutlinedTextField(
            value = query,
            onValueChange = vm::setQuery,
            singleLine = true,
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            placeholder = { Text("Search videos") },
            shape = RoundedCornerShape(999.dp),
            modifier = Modifier.fillMaxWidth().padding(vertical = 14.dp),
        )

        if (query.isNotBlank() && results.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No matches", color = androidx.compose.ui.graphics.Color.Gray)
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                verticalArrangement = Arrangement.spacedBy(18.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                contentPadding = PaddingValues(vertical = 8.dp),
            ) {
                items(results) { video -> VideoCard(video, onOpenVideo) }
            }
        }
    }
}
