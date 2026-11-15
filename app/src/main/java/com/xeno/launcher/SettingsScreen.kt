package com.xeno.launcher

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun SettingsScreen(
    nick: String, onNick: (String) -> Unit,
    ram: Float, onRam: (Float) -> Unit,
    theme: XenoTheme, onTheme: (XenoTheme) -> Unit
) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Настройки", style = MaterialTheme.typography.headlineMedium)
        OutlinedTextField(nick, onNick, label = { Text("Ник (офлайн)") }, singleLine = true,
            modifier = Modifier.fillMaxWidth())
        Text("Память для игры: ${ram.toInt()} МБ")
        Slider(ram, onRam, valueRange = 1024f..4096f, steps = 5)
        Text("Тема", style = MaterialTheme.typography.titleMedium)
        XenoTheme.values().forEach { t ->
            val ok = t != XenoTheme.DYNAMIC || dynamicAvailable()
            Row(
                Modifier.fillMaxWidth().clickable(enabled = ok) { onTheme(t) },
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(theme == t, { onTheme(t) }, enabled = ok)
                Text(if (ok) t.title else "${t.title} (недоступно)")
            }
        }
    }
}
