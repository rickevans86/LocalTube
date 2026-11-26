package com.localtube.app.feature.player

import android.app.Activity
import android.content.pm.ActivityInfo
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.view.View
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.localtube.app.data.ContentRepository
import com.localtube.app.data.db.VideoEntity
import com.localtube.app.di.AppContainer
import com.localtube.app.ui.VideoThumb
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.videolan.libvlc.LibVLC
import org.videolan.libvlc.Media
import org.videolan.libvlc.MediaPlayer
import org.videolan.libvlc.util.VLCVideoLayout

class PlayerViewModel(private val repo: ContentRepository) : ViewModel() {
    val upNext = repo.homeFeed.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun record(uri: String, startedAt: Long, watchedMs: Long) {
        viewModelScope.launch { repo.recordWatch(uri, startedAt, watchedMs) }
    }
}

@Composable
fun PlayerScreen(
    container: AppContainer,
    videoUri: String,
    onBack: () -> Unit,
    onOpenVideo: (String) -> Unit,
) {
    val vm: PlayerViewModel = viewModel(
        factory = viewModelFactory { initializer { PlayerViewModel(container.repository) } },
    )
    val upNext by vm.upNext.collectAsState()
    val context = LocalContext.current
    val startedAt = remember { System.currentTimeMillis() }

    val libVlc = remember {
        LibVLC(context, arrayListOf("--no-drop-late-frames", "--no-skip-frames"))
    }
    val mediaPlayer = remember { MediaPlayer(libVlc) }
    val pfdRef = remember { mutableStateOf<ParcelFileDescriptor?>(null) }
    val started = remember { mutableStateOf(false) }
    var playing by remember { mutableStateOf(true) }

    // Open the SAF document as a fd and start playback. Called only once the VLC
    // surface view is attached to the window — starting earlier can crash natively.
    fun startPlayback() {
        if (started.value) return
        try {
            val pfd = context.contentResolver.openFileDescriptor(Uri.parse(videoUri), "r") ?: return
            pfdRef.value = pfd
            val media = Media(libVlc, pfd.fileDescriptor)
            media.setHWDecoderEnabled(true, false)
            mediaPlayer.media = media
            media.release()
            mediaPlayer.play()
            started.value = true
            playing = true
        } catch (_: Exception) {
            // Leave the surface blank; a user-facing error state is a follow-up.
        }
    }

    DisposableEffect(Unit) {
        val activity = context as? Activity
        val previousOrientation = activity?.requestedOrientation
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        onDispose {
            try {
                val watched = mediaPlayer.time.coerceAtLeast(0)
                vm.record(videoUri, startedAt, watched)
                mediaPlayer.stop()
                mediaPlayer.detachViews()
                mediaPlayer.release()
                libVlc.release()
            } catch (_: Exception) {
            }
            pfdRef.value?.close()
            if (previousOrientation != null) activity.requestedOrientation = previousOrientation
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(Color(0xFF101624))) {
        AndroidView(
            factory = { ctx ->
                VLCVideoLayout(ctx).apply {
                    keepScreenOn = true
                    addOnAttachStateChangeListener(object : View.OnAttachStateChangeListener {
                        override fun onViewAttachedToWindow(v: View) {
                            try {
                                mediaPlayer.attachViews(this@apply, null, false, false)
                            } catch (_: Exception) {
                            }
                            startPlayback()
                        }

                        override fun onViewDetachedFromWindow(v: View) {
                            try {
                                mediaPlayer.detachViews()
                            } catch (_: Exception) {
                            }
                        }
                    })
                }
            },
            modifier = Modifier.fillMaxSize(),
        )

        IconButton(
            onClick = onBack,
            modifier = Modifier.padding(16.dp).align(Alignment.TopStart),
        ) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
        }

        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .size(72.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.9f))
                .clickable {
                    try {
                        if (mediaPlayer.isPlaying) {
                            mediaPlayer.pause()
                            playing = false
                        } else {
                            mediaPlayer.play()
                            playing = true
                        }
                    } catch (_: Exception) {
                    }
                },
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                if (playing) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                contentDescription = if (playing) "Pause" else "Play",
                tint = Color(0xFF101624),
                modifier = Modifier.size(34.dp),
            )
        }

        if (upNext.isNotEmpty()) {
            Column(modifier = Modifier.align(Alignment.BottomStart).fillMaxWidth().padding(12.dp)) {
                Text("Up next", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                LazyRow(modifier = Modifier.padding(top = 6.dp)) {
                    items(upNext) { v -> UpNextThumb(v, onOpenVideo) }
                }
            }
        }
    }
}

@Composable
private fun UpNextThumb(video: VideoEntity, onOpen: (String) -> Unit) {
    Box(
        modifier = Modifier
            .padding(end = 10.dp)
            .width(118.dp)
            .size(width = 118.dp, height = 66.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(Color.DarkGray)
            .clickable { onOpen(video.documentUri) },
    ) {
        VideoThumb(video.documentUri, modifier = Modifier.fillMaxSize())
    }
}
