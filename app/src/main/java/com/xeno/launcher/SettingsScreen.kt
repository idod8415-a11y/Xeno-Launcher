package com.xeno.launcher

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun SettingsScreen(nick: String, onNick: (String) -> Unit, ram: Float, onRam: (Float) -> Unit) {
    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("Настройки", style = MaterialTheme.typography.headlineMedium)
        OutlinedTextField(nick, onNick, label = { Text("Ник (офлайн)") }, singleLine = true,
            modifier = Modifier.fillMaxWidth())
        Text("Память для игры: ${ram.toInt()} МБ")
        Slider(ram, onRam, valueRange = 1024f..4096f, steps = 5)
    }
}
