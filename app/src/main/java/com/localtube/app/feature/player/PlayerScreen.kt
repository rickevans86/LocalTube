package com.localtube.app.feature.player

import android.app.Activity
import android.content.pm.ActivityInfo
import android.net.Uri
import android.view.View
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.localtube.app.data.ContentRepository
import com.localtube.app.data.db.VideoEntity
import com.localtube.app.di.AppContainer
import coil.compose.AsyncImage
import com.localtube.app.ui.VideoThumb
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class PlayerViewModel(
    private val repo: ContentRepository,
    folderId: Long?,
    channel: String?,
    movies: Boolean,
) : ViewModel() {
    // Up-next scope: a channel's episodes, all movies, a single folder, or —
    // when opened from "All" — the shuffled whole-library feed.
    private val source = when {
        folderId != null && channel != null -> repo.channelEpisodes(folderId, channel)
        movies -> repo.movies
        folderId != null -> repo.videosInFolder(folderId)
        else -> repo.homeFeed
    }
    val upNext = source.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val meta = repo.meta.map { list -> list.associateBy { it.itemKey } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())

    fun record(uri: String, startedAt: Long, watchedMs: Long) {
        viewModelScope.launch { repo.recordWatch(uri, startedAt, watchedMs) }
    }
}

@OptIn(UnstableApi::class)
@Composable
fun PlayerScreen(
    container: AppContainer,
    videoUri: String,
    folderId: Long?,
    channel: String?,
    movies: Boolean,
    onBack: () -> Unit,
    onOpenVideo: (String) -> Unit,
) {
    val vm: PlayerViewModel = viewModel(
        factory = viewModelFactory {
            initializer { PlayerViewModel(container.repository, folderId, channel, movies) }
        },
    )
    val upNext by vm.upNext.collectAsState()
    val meta by vm.meta.collectAsState()
    val context = LocalContext.current
    val startedAt = remember { System.currentTimeMillis() }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var controlsVisible by remember { mutableStateOf(false) }

    val player = remember {
        ExoPlayer.Builder(context).build().apply {
            addListener(object : Player.Listener {
                override fun onPlayerError(error: PlaybackException) {
                    // Surface the reason on screen instead of failing silently.
                    // Walk to the root cause so IO_UNSPECIFIED tells us *why*.
                    val root = generateSequence(error.cause) { it.cause }.lastOrNull()
                    errorMessage = buildString {
                        append("Can't play this video\n")
                        append(error.errorCodeName)
                        if (root != null) {
                            append("\n")
                            append(root.javaClass.simpleName)
                            root.message?.let { append(": "); append(it) }
                        }
                    }
                }
            })
            setMediaItem(MediaItem.fromUri(Uri.parse(videoUri)))
            // Keep playing: advance through the queue and loop when it ends.
            repeatMode = Player.REPEAT_MODE_ALL
            prepare()
            playWhenReady = true
        }
    }

    // Queue what comes next so a finished video auto-advances. In a channel the
    // episodes play in order starting after the current one (wrapping around);
    // otherwise it's the rest of the shuffled feed.
    LaunchedEffect(upNext) {
        if (upNext.isNotEmpty() && player.mediaItemCount <= 1) {
            val idx = upNext.indexOfFirst { it.documentUri == videoUri }
            val ordered = if (idx >= 0) upNext.drop(idx + 1) + upNext.take(idx)
            else upNext.filter { it.documentUri != videoUri }
            val items = ordered.map { MediaItem.fromUri(Uri.parse(it.documentUri)) }
            if (items.isNotEmpty()) player.addMediaItems(items)
        }
    }

    DisposableEffect(Unit) {
        val activity = context as? Activity
        val previousOrientation = activity?.requestedOrientation
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        // Immersive: hide the system bars so the nav buttons don't cover the player.
        val insets = activity?.window?.let { WindowCompat.getInsetsController(it, it.decorView) }
        insets?.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        insets?.hide(WindowInsetsCompat.Type.systemBars())
        onDispose {
            val watched = player.currentPosition.coerceAtLeast(0)
            vm.record(videoUri, startedAt, watched)
            player.release()
            insets?.show(WindowInsetsCompat.Type.systemBars())
            if (previousOrientation != null) activity.requestedOrientation = previousOrientation
        }
    }

    // Pause playback when the app goes to the background (no background play).
    val lifecycleOwner = context as? LifecycleOwner
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) player.pause()
        }
        lifecycleOwner?.lifecycle?.addObserver(observer)
        onDispose { lifecycleOwner?.lifecycle?.removeObserver(observer) }
    }

    Box(modifier = Modifier.fillMaxSize().background(Color(0xFF101624))) {
        Column(modifier = Modifier.fillMaxSize()) {
            AndroidView(
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        this.player = player
                        useController = true
                        keepScreenOn = true
                        // No playlist, so hide the next/previous buttons.
                        setShowNextButton(false)
                        setShowPreviousButton(false)
                        // Reveal the up-next row with the controls (shown on tap).
                        setControllerVisibilityListener(
                            PlayerView.ControllerVisibilityListener { visibility ->
                                controlsVisible = visibility == View.VISIBLE
                            },
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth().weight(1f),
            )

            // Sits BELOW the player so it never covers the seek bar; the video
            // shrinks slightly while it's shown.
            AnimatedVisibility(
                visible = controlsVisible && upNext.isNotEmpty(),
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.Black.copy(alpha = 0.55f))
                        .padding(12.dp),
                ) {
                    Text("Up next", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    LazyRow(modifier = Modifier.padding(top = 6.dp)) {
                        items(upNext) { v ->
                            val m = meta[v.documentUri]
                            UpNextThumb(v, m?.backdropUrl ?: m?.posterUrl, onOpenVideo)
                        }
                    }
                }
            }
        }

        IconButton(
            onClick = onBack,
            modifier = Modifier.padding(16.dp).align(Alignment.TopStart),
        ) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
        }

        errorMessage?.let { msg ->
            Text(
                msg,
                color = Color.White,
                fontFamily = FontFamily.Monospace,
                fontSize = 13.sp,
                modifier = Modifier.align(Alignment.Center).padding(24.dp),
            )
        }
    }
}

@Composable
private fun UpNextThumb(video: VideoEntity, artUrl: String?, onOpen: (String) -> Unit) {
    Box(
        modifier = Modifier
            .padding(end = 10.dp)
            .width(118.dp)
            .size(width = 118.dp, height = 66.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(Color.DarkGray)
            .clickable { onOpen(video.documentUri) },
    ) {
        if (artUrl != null) {
            AsyncImage(
                model = artUrl,
                contentDescription = video.title,
                contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            VideoThumb(video.documentUri, modifier = Modifier.fillMaxSize())
        }
    }
}
