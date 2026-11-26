package com.localtube.app.ui

import android.content.Context
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.decode.VideoFrameDecoder
import coil.request.ImageRequest
import coil.request.videoFramePercent

/** One shared Coil loader (decodes a frame from a local video file). */
private object ThumbLoader {
    @Volatile
    private var instance: ImageLoader? = null

    fun get(context: Context): ImageLoader =
        instance ?: synchronized(this) {
            instance ?: ImageLoader.Builder(context.applicationContext)
                .components { add(VideoFrameDecoder.Factory()) }
                .crossfade(true)
                .build()
                .also { instance = it }
        }
}

/** A video thumbnail generated from a frame of the file itself. */
@Composable
fun VideoThumb(uri: String, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val loader = remember { ThumbLoader.get(context) }
    Box(modifier) {
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(uri)
                // Grab a frame ~40% through so the thumbnail isn't a black
                // intro or a studio logo (works without knowing the duration).
                .videoFramePercent(0.4)
                .crossfade(true)
                .build(),
            imageLoader = loader,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
    }
}
