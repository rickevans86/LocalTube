package com.localtube.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.localtube.app.ui.LocalTubeRoot
import com.localtube.app.ui.theme.LocalTubeTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val container = (application as LocalTubeApp).container
        setContent {
            LocalTubeTheme {
                LocalTubeRoot(container)
            }
        }
    }
}
