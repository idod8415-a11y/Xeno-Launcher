package com.xeno.launcher

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@Composable
fun SettingsScreen(
    nick: String, onNick: (String) -> Unit,
    ram: Float, onRam: (Float) -> Unit,
    theme: XenoTheme, onTheme: (XenoTheme) -> Unit
) {
    val ctx = LocalContext.current
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            "Настройки",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            GlassBox(Modifier.weight(1f)) {
                Column(Modifier.padding(16.dp)) {
                    OutlinedTextField(
                        nick, onNick,
                        label = { Text("Ник (офлайн)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
            GlassBox(Modifier.weight(1f)) {
                Column(Modifier.padding(16.dp)) {
                    Text("Память для игры: ${ram.toInt()} МБ")
                    Slider(ram, onRam, valueRange = 1024f..4096f, steps = 5)
                }
            }
        }
        Text("Тема", style = MaterialTheme.typography.titleMedium)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            XenoTheme.values().forEach { t ->
                val ok = t != XenoTheme.DYNAMIC || dynamicAvailable()
                val swatch = schemeFor(t, ctx).primary
                val sel = theme == t
                val click: (() -> Unit)? = if (ok) ({ onTheme(t) }) else null
                GlassBox(
                    Modifier.weight(1f),
                    radius = 28.dp,
                    tint = swatch,
                    strength = if (sel) 2.2f else 1f,
                    onClick = click
                ) {
                    Column(
                        Modifier.fillMaxWidth().padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(swatch)
                                .border(
                                    2.dp,
                                    Color.White.copy(alpha = if (sel) 0.9f else 0.3f),
                                    CircleShape
                                )
                        )
                        Text(
                            if (ok) t.title else "${t.title} (недоступно)",
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}
