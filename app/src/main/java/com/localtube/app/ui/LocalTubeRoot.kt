package com.localtube.app.ui

import android.net.Uri
import android.util.Base64
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import kotlinx.coroutines.launch
import com.localtube.app.di.AppContainer
import com.localtube.app.feature.channel.ChannelScreen
import com.localtube.app.feature.gate.ParentGateScreen
import com.localtube.app.feature.home.HomeScreen
import com.localtube.app.feature.library.YourStuffScreen
import com.localtube.app.feature.match.ScanMatchScreen
import com.localtube.app.feature.player.PlayerScreen
import com.localtube.app.feature.search.SearchScreen
import com.localtube.app.feature.settings.MainFoldersScreen
import com.localtube.app.feature.settings.ScreenTimeScreen
import com.localtube.app.feature.settings.SettingsScreen

object Routes {
    const val MAIN = "main"
    const val CHANNEL = "channel/{folderId}/{channel}"
    const val PLAYER = "player/{uri}?folderId={folderId}&channel={channel}"
    const val GATE = "gate"
    const val SETTINGS = "settings"
    const val FOLDERS = "folders"
    const val SCREEN_TIME = "screentime"
    const val SCAN_MATCH = "scanmatch"

    fun channel(folderId: Long, channel: String) = "channel/$folderId/${Uri.encode(channel)}"

    // Content URIs contain '/' which Navigation mishandles in a route arg, so the
    // video URI is passed URL-safe Base64-encoded and decoded in the player.
    // When opened from a channel, the folderId + channel are carried along so
    // the player's up-next is limited to that channel's episodes.
    fun player(videoUri: String, folderId: Long? = null, channel: String? = null): String {
        val encoded = Base64.encodeToString(
            videoUri.toByteArray(),
            Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING,
        )
        return if (folderId != null && channel != null) {
            "player/$encoded?folderId=$folderId&channel=${Uri.encode(channel)}"
        } else {
            "player/$encoded"
        }
    }
}

@Composable
fun LocalTubeRoot(container: AppContainer) {
    val nav = rememberNavController()

    // Refresh the library every time the app is brought to the foreground, so
    // files added to or removed from the folders show up without re-adding them.
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_START) {
                scope.launch { runCatching { container.repository.refreshLibrary() } }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    NavHost(navController = nav, startDestination = Routes.MAIN) {
        composable(Routes.MAIN) {
            MainScaffold(
                container = container,
                onOpenVideo = { nav.navigate(Routes.player(it)) },
                onOpenChannel = { folderId, channel -> nav.navigate(Routes.channel(folderId, channel)) },
                onOpenParentArea = { nav.navigate(Routes.GATE) },
            )
        }
        composable(
            Routes.CHANNEL,
            arguments = listOf(
                navArgument("folderId") { type = NavType.LongType },
                navArgument("channel") { type = NavType.StringType },
            ),
        ) { entry ->
            val folderId = entry.arguments?.getLong("folderId") ?: 0L
            val channel = Uri.decode(entry.arguments?.getString("channel").orEmpty())
            ChannelScreen(
                container = container,
                folderId = folderId,
                channel = channel,
                onOpenVideo = { nav.navigate(Routes.player(it, folderId, channel)) },
                onBack = { nav.popBackStack() },
            )
        }
        composable(
            Routes.PLAYER,
            arguments = listOf(
                navArgument("uri") { type = NavType.StringType },
                navArgument("folderId") { type = NavType.LongType; defaultValue = -1L },
                navArgument("channel") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                },
            ),
        ) { entry ->
            val uri = runCatching {
                String(
                    Base64.decode(
                        entry.arguments?.getString("uri").orEmpty(),
                        Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING,
                    )
                )
            }.getOrDefault("")
            val folderId = entry.arguments?.getLong("folderId")?.takeIf { it >= 0 }
            val channel = entry.arguments?.getString("channel")?.let { Uri.decode(it) }
            PlayerScreen(
                container = container,
                videoUri = uri,
                folderId = folderId,
                channel = channel,
                onBack = { nav.popBackStack() },
                onOpenVideo = {
                    // Keep the same channel context when switching within the player.
                    nav.navigate(Routes.player(it, folderId, channel)) { popUpTo(Routes.MAIN) }
                },
            )
        }
        composable(Routes.GATE) {
            ParentGateScreen(
                container = container,
                onUnlocked = { nav.navigate(Routes.SETTINGS) { popUpTo(Routes.MAIN) } },
                onCancel = { nav.popBackStack() },
            )
        }
        composable(Routes.SETTINGS) {
            SettingsScreen(
                container = container,
                onOpenFolders = { nav.navigate(Routes.FOLDERS) },
                onOpenScreenTime = { nav.navigate(Routes.SCREEN_TIME) },
                onOpenScanMatch = { nav.navigate(Routes.SCAN_MATCH) },
                onDone = { nav.navigate(Routes.MAIN) { popUpTo(Routes.MAIN) { inclusive = true } } },
            )
        }
        composable(Routes.FOLDERS) {
            MainFoldersScreen(container = container, onBack = { nav.popBackStack() })
        }
        composable(Routes.SCREEN_TIME) {
            ScreenTimeScreen(container = container, onBack = { nav.popBackStack() })
        }
        composable(Routes.SCAN_MATCH) {
            ScanMatchScreen(container = container, onBack = { nav.popBackStack() })
        }
    }
}

@Composable
private fun MainScaffold(
    container: AppContainer,
    onOpenVideo: (String) -> Unit,
    onOpenChannel: (Long, String) -> Unit,
    onOpenParentArea: () -> Unit,
) {
    var tab by remember { mutableIntStateOf(0) }
    Scaffold(
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = tab == 0,
                    onClick = { tab = 0 },
                    icon = { Icon(Icons.Default.Home, contentDescription = null) },
                    label = { Text("Home") },
                )
                NavigationBarItem(
                    selected = tab == 1,
                    onClick = { tab = 1 },
                    icon = { Icon(Icons.Default.Search, contentDescription = null) },
                    label = { Text("Search") },
                )
                NavigationBarItem(
                    selected = tab == 2,
                    onClick = { tab = 2 },
                    icon = { Icon(Icons.Default.VideoLibrary, contentDescription = null) },
                    label = { Text("Your stuff") },
                )
            }
        },
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when (tab) {
                0 -> HomeScreen(container, onOpenVideo, onOpenChannel, onOpenParentArea)
                1 -> SearchScreen(container, onOpenVideo)
                else -> YourStuffScreen(container, onOpenVideo)
            }
        }
    }
}
