package com.localtube.app

import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.localtube.app.ui.LocalTubeRoot
import com.localtube.app.ui.theme.LocalTubeTheme
import java.io.File

class MainActivity : ComponentActivity() {
    // Force the whole app to light mode regardless of the device's dark theme.
    override fun attachBaseContext(newBase: Context) {
        val config = Configuration(newBase.resources.configuration)
        config.uiMode = (config.uiMode and Configuration.UI_MODE_NIGHT_MASK.inv()) or
            Configuration.UI_MODE_NIGHT_NO
        super.attachBaseContext(newBase.createConfigurationContext(config))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val container = (application as LocalTubeApp).container
        val crashFile = File(filesDir, LocalTubeApp.CRASH_FILE)
        setContent {
            LocalTubeTheme {
                var crash by remember {
                    mutableStateOf(if (crashFile.exists()) crashFile.readText() else null)
                }
                val text = crash
                if (text != null) {
                    CrashScreen(text) {
                        crashFile.delete()
                        crash = null
                    }
                } else {
                    LocalTubeRoot(container)
                }
            }
        }
    }
}

@Composable
private fun CrashScreen(text: String, onDismiss: () -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState()),
    ) {
        Text("LocalTube hit an error", fontSize = 20.sp)
        Button(onClick = onDismiss, modifier = Modifier.padding(vertical = 10.dp)) {
            Text("Dismiss")
        }
        Text(text, fontFamily = FontFamily.Monospace, fontSize = 11.sp)
    }
}
