package com.localtube.app.feature.settings

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Switch
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
import androidx.compose.ui.platform.LocalContext
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
import com.localtube.app.data.SettingsStore
import com.localtube.app.data.db.FolderEntity
import com.localtube.app.di.AppContainer
import com.localtube.app.ui.theme.Coral
import com.localtube.app.ui.theme.SettingsBg
import com.localtube.app.update.UpdateChecker
import com.localtube.app.update.UpdateResult
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val repo: ContentRepository,
    private val settingsStore: SettingsStore,
    private val updateChecker: UpdateChecker,
) : ViewModel() {
    val folders = repo.folders.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val settings = settingsStore.settings.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        com.localtube.app.data.AppSettings(false, 120, false, 19 * 60 + 30, 7 * 60, false),
    )

    fun addFolder(name: String, treeUri: String) {
        viewModelScope.launch { repo.addFolder(name, treeUri, FolderType.VIDEOS) }
    }

    fun cycleType(folder: FolderEntity) {
        val next = when (FolderType.fromName(folder.type)) {
            FolderType.VIDEOS -> FolderType.MOVIES
            FolderType.MOVIES -> FolderType.CHANNELS
            FolderType.CHANNELS -> FolderType.VIDEOS
        }
        viewModelScope.launch {
            repo.setFolderType(folder, next)
            repo.rescanFolder(folder.copy(type = next.name))
        }
    }

    fun setVisible(folder: FolderEntity, visible: Boolean) {
        viewModelScope.launch { repo.setFolderVisible(folder, visible) }
    }

    fun setDailyLimit(minutes: Int) {
        viewModelScope.launch { settingsStore.setDailyLimit(minutes) }
    }

    fun setBedtime(enabled: Boolean, start: Int, end: Int) {
        viewModelScope.launch { settingsStore.setBedtime(enabled, start, end) }
    }

    suspend fun checkForUpdates(): UpdateResult = updateChecker.check()
}

private fun settingsVm(container: AppContainer) = viewModelFactory {
    initializer { SettingsViewModel(container.repository, container.settings, container.updateChecker) }
}

@Composable
fun SettingsScreen(
    container: AppContainer,
    onOpenFolders: () -> Unit,
    onOpenScreenTime: () -> Unit,
    onDone: () -> Unit,
) {
    val vm: SettingsViewModel = viewModel(factory = settingsVm(container))
    val folders by vm.folders.collectAsState()
    val settings by vm.settings.collectAsState()
    val scope = rememberCoroutineScope()
    var updateText by remember { mutableStateOf("Tap to check") }

    Column(modifier = Modifier.fillMaxSize().background(SettingsBg).padding(horizontal = 24.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 22.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Parent settings", fontSize = 28.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            Text("Done", color = Coral, fontWeight = FontWeight.Bold, modifier = Modifier.clickable { onDone() })
        }

        SettingsRow("Main folders", "${folders.size} folders · become kid categories", onOpenFolders)
        SettingsRow("Screen time", "${settings.dailyLimitMinutes} min/day", onOpenScreenTime)
        SettingsRow("Check for updates", updateText) {
            scope.launch {
                updateText = "Checking…"
                updateText = when (val r = vm.checkForUpdates()) {
                    is UpdateResult.Available -> "Update available: ${r.tag}"
                    UpdateResult.UpToDate -> "Up to date"
                    is UpdateResult.Failed -> "Couldn't check: ${r.reason}"
                }
            }
        }
    }
}

@Composable
private fun SettingsRow(title: String, value: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White)
            .clickable { onClick() }
            .padding(18.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Text(value, fontSize = 13.sp, color = Color.Gray)
        }
    }
}

@Composable
fun MainFoldersScreen(container: AppContainer, onBack: () -> Unit) {
    val vm: SettingsViewModel = viewModel(factory = settingsVm(container))
    val folders by vm.folders.collectAsState()
    val context = LocalContext.current

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri != null) {
            context.contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION,
            )
            val name = uri.lastPathSegment?.substringAfterLast('/')?.substringAfterLast(':') ?: "Folder"
            vm.addFolder(name, uri.toString())
        }
    }

    Column(modifier = Modifier.fillMaxSize().background(SettingsBg).padding(horizontal = 24.dp)) {
        Row(modifier = Modifier.padding(vertical = 18.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
            Text("Main folders", fontSize = 26.sp, fontWeight = FontWeight.Bold)
        }
        Text(
            "These become the categories kids see. A folder of subfolders becomes channels. Tap the type to change it.",
            color = Color.Gray,
            fontSize = 14.sp,
            modifier = Modifier.padding(bottom = 12.dp),
        )

        LazyColumn(verticalArrangement = Arrangement.spacedBy(11.dp), modifier = Modifier.weight(1f)) {
            items(folders) { folder ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.White)
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(folder.name, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        Text(folder.treeUri, fontSize = 11.sp, color = Color.Gray, maxLines = 1)
                    }
                    Text(
                        FolderType.fromName(folder.type).name,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Coral,
                        modifier = Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .background(Color(0xFFFFE3D1))
                            .clickable { vm.cycleType(folder) }
                            .padding(horizontal = 11.dp, vertical = 5.dp),
                    )
                    Switch(
                        checked = folder.visible,
                        onCheckedChange = { vm.setVisible(folder, it) },
                        modifier = Modifier.padding(start = 10.dp),
                    )
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
                .clip(RoundedCornerShape(16.dp))
                .clickable { picker.launch(null) }
                .background(Color(0x11000000))
                .padding(16.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text("+ Add folder (choose on device)", fontWeight = FontWeight.Bold, color = Color(0xFF8A7A66))
        }
    }
}

@Composable
fun ScreenTimeScreen(container: AppContainer, onBack: () -> Unit) {
    val vm: SettingsViewModel = viewModel(factory = settingsVm(container))
    val settings by vm.settings.collectAsState()

    Column(modifier = Modifier.fillMaxSize().background(SettingsBg).padding(24.dp)) {
        Row(modifier = Modifier.padding(bottom = 22.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
            Text("Screen time", fontSize = 26.sp, fontWeight = FontWeight.Bold)
        }

        Column(
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(Color.White).padding(22.dp),
        ) {
            Text("Daily limit", fontWeight = FontWeight.Bold, color = Color.Gray)
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(22.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Stepper("−") { vm.setDailyLimit((settings.dailyLimitMinutes - 15).coerceAtLeast(0)) }
                Text(
                    formatMinutes(settings.dailyLimitMinutes),
                    fontSize = 40.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                )
                Stepper("+") { vm.setDailyLimit(settings.dailyLimitMinutes + 15) }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(Color.White)
                .padding(22.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Bedtime (no videos 7:30 PM–7:00 AM)", fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            Switch(
                checked = settings.bedtimeEnabled,
                onCheckedChange = { vm.setBedtime(it, settings.bedtimeStartMinutes, settings.bedtimeEndMinutes) },
            )
        }
    }
}

@Composable
private fun Stepper(label: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(52.dp)
            .clip(RoundedCornerShape(26.dp))
            .background(Color(0xFFF3F4F6))
            .clickable { onClick() },
        contentAlignment = Alignment.Center,
    ) {
        Text(label, fontSize = 26.sp, fontWeight = FontWeight.Bold)
    }
}

private fun formatMinutes(total: Int): String {
    val h = total / 60
    val m = total % 60
    return if (h > 0) "${h}h ${m.toString().padStart(2, '0')}m" else "${m}m"
}
