package com.localtube.app.feature.gate

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.localtube.app.data.SettingsStore
import com.localtube.app.di.AppContainer
import com.localtube.app.ui.theme.Coral
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class GateViewModel(private val settings: SettingsStore) : ViewModel() {
    val hasPin = settings.settings.map { it.hasPin }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), true)

    /** Returns true when the PIN is accepted (or newly set on first run). */
    suspend fun submit(pin: String): Boolean {
        val current = settings.current()
        return if (!current.hasPin) {
            settings.setPin(pin)
            true
        } else {
            settings.verifyPin(pin)
        }
    }
}

@Composable
fun ParentGateScreen(
    container: AppContainer,
    onUnlocked: () -> Unit,
    onCancel: () -> Unit,
) {
    val vm: GateViewModel = viewModel(
        factory = viewModelFactory { initializer { GateViewModel(container.settings) } },
    )
    val hasPin by vm.hasPin.collectAsState()
    val scope = rememberCoroutineScope()
    var entered by remember { mutableStateOf("") }
    var error by remember { mutableStateOf(false) }

    fun press(digit: String) {
        if (entered.length >= 4) return
        entered += digit
        error = false
        if (entered.length == 4) {
            val attempt = entered
            scope.launch {
                val ok = vm.submit(attempt)
                if (ok) onUnlocked() else {
                    error = true
                    entered = ""
                }
            }
        }
    }

    Box(
        modifier = Modifier.fillMaxSize().background(Color(0xFF263040)),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .width(400.dp)
                .clip(RoundedCornerShape(28.dp))
                .background(Color.White)
                .padding(34.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                if (hasPin) "Parents only" else "Create a parent PIN",
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
            )
            Text(
                if (error) "Try again" else if (hasPin) "Enter your PIN to continue" else "Choose a 4-digit PIN",
                color = if (error) Coral else Color.Gray,
                fontSize = 15.sp,
                modifier = Modifier.padding(top = 4.dp, bottom = 16.dp),
            )

            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                repeat(4) { i ->
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .clip(CircleShape)
                            .background(if (i < entered.length) Coral else Color(0xFFE3E3E3)),
                    )
                }
            }

            Column(modifier = Modifier.padding(top = 20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                listOf(listOf("1", "2", "3"), listOf("4", "5", "6"), listOf("7", "8", "9")).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        row.forEach { d -> Key(d) { press(d) } }
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    Box(modifier = Modifier.size(64.dp))
                    Key("0") { press("0") }
                    Key("⌫") { if (entered.isNotEmpty()) entered = entered.dropLast(1) }
                }
            }

            Text(
                "Cancel",
                color = Color.Gray,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 18.dp).clickable { onCancel() },
            )
        }
    }
}

@Composable
private fun Key(label: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(64.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFFF3F4F6))
            .clickable { onClick() },
        contentAlignment = Alignment.Center,
    ) {
        Text(label, fontSize = 22.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
    }
}
