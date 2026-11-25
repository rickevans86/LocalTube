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
import coil.request.videoFrameMillis

/** Shared Coil loader that can decode a frame from a local video file. */
fun videoImageLoader(context: Context): ImageLoader =
    ImageLoader.Builder(context)
        .components { add(VideoFrameDecoder.Factory()) }
        .crossfade(true)
        .build()

/** A video thumbnail generated from a frame of the file itself. */
@Composable
fun VideoThumb(uri: String, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val loader = remember { videoImageLoader(context) }
    Box(modifier) {
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(uri)
                .videoFrameMillis(2000)
                .crossfade(true)
                .build(),
            imageLoader = loader,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
    }
}
