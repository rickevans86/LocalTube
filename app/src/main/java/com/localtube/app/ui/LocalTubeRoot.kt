package com.localtube.app.ui

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.localtube.app.di.AppContainer
import com.localtube.app.feature.channel.ChannelScreen
import com.localtube.app.feature.gate.ParentGateScreen
import com.localtube.app.feature.home.HomeScreen
import com.localtube.app.feature.player.PlayerScreen
import com.localtube.app.feature.settings.MainFoldersScreen
import com.localtube.app.feature.settings.ScreenTimeScreen
import com.localtube.app.feature.settings.SettingsScreen

object Routes {
    const val HOME = "home"
    const val CHANNEL = "channel/{folderId}/{channel}"
    const val PLAYER = "player/{uri}"
    const val GATE = "gate"
    const val SETTINGS = "settings"
    const val FOLDERS = "folders"
    const val SCREEN_TIME = "screentime"

    fun channel(folderId: Long, channel: String) = "channel/$folderId/${Uri.encode(channel)}"
    fun player(videoUri: String) = "player/${Uri.encode(videoUri)}"
}

@Composable
fun LocalTubeRoot(container: AppContainer) {
    val nav = rememberNavController()
    NavHost(navController = nav, startDestination = Routes.HOME) {
        composable(Routes.HOME) {
            HomeScreen(
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
                onOpenVideo = { nav.navigate(Routes.player(it)) },
                onBack = { nav.popBackStack() },
            )
        }
        composable(
            Routes.PLAYER,
            arguments = listOf(navArgument("uri") { type = NavType.StringType }),
        ) { entry ->
            val uri = Uri.decode(entry.arguments?.getString("uri").orEmpty())
            PlayerScreen(
                container = container,
                videoUri = uri,
                onBack = { nav.popBackStack() },
                onOpenVideo = { nav.navigate(Routes.player(it)) { popUpTo(Routes.HOME) } },
            )
        }
        composable(Routes.GATE) {
            ParentGateScreen(
                container = container,
                onUnlocked = { nav.navigate(Routes.SETTINGS) { popUpTo(Routes.HOME) } },
                onCancel = { nav.popBackStack() },
            )
        }
        composable(Routes.SETTINGS) {
            SettingsScreen(
                container = container,
                onOpenFolders = { nav.navigate(Routes.FOLDERS) },
                onOpenScreenTime = { nav.navigate(Routes.SCREEN_TIME) },
                onDone = { nav.navigate(Routes.HOME) { popUpTo(Routes.HOME) { inclusive = true } } },
            )
        }
        composable(Routes.FOLDERS) {
            MainFoldersScreen(container = container, onBack = { nav.popBackStack() })
        }
        composable(Routes.SCREEN_TIME) {
            ScreenTimeScreen(container = container, onBack = { nav.popBackStack() })
        }
    }
}
