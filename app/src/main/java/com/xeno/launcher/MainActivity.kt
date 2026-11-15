package com.xeno.launcher

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val prefs = getSharedPreferences("xeno", Context.MODE_PRIVATE)
        setContent {
            var theme by remember {
                mutableStateOf(
                    runCatching { XenoTheme.valueOf(prefs.getString("theme", "GREEN") ?: "GREEN") }
                        .getOrDefault(XenoTheme.GREEN)
                )
            }
            MaterialTheme(colorScheme = schemeFor(theme, this)) {
                XenoApp(theme) {
                    theme = it
                    prefs.edit().putString("theme", it.name).apply()
                }
            }
        }
    }
}

@Composable
fun XenoApp(theme: XenoTheme, onTheme: (XenoTheme) -> Unit) {
    var tab by remember { mutableIntStateOf(0) }
    var selected by remember { mutableStateOf<McVersion?>(null) }
    var nick by remember { mutableStateOf("Player") }
    var ramMb by remember { mutableFloatStateOf(2048f) }

    GlassBackdrop {
        Row(Modifier.fillMaxSize()) {
            Sidebar(tab) { tab = it }
            Box(Modifier.weight(1f).fillMaxHeight().padding(8.dp)) {
                when (tab) {
                    0 -> PlayScreen(selected) { selected = it }
                    1 -> JavaScreen()
                    else -> SettingsScreen(nick, { nick = it }, ramMb, { ramMb = it }, theme, onTheme)
                }
            }
        }
    }
}

@Composable
fun Sidebar(tab: Int, onTab: (Int) -> Unit) {
    val items: List<Pair<String, ImageVector>> = listOf(
        "Играть" to Icons.Filled.Home,
        "Java" to Icons.Filled.Build,
        "Настройки" to Icons.Filled.Settings
    )
    GlassBox(Modifier.width(200.dp).fillMaxHeight().padding(8.dp), radius = 28.dp) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                "XENO",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Text("Minecraft Java", style = MaterialTheme.typography.bodySmall)
            Spacer(Modifier.height(8.dp))
            items.forEachIndexed { i, item ->
                val sel = tab == i
                GlassBox(
                    Modifier.fillMaxWidth(),
                    radius = 18.dp,
                    tint = if (sel) MaterialTheme.colorScheme.primary else Color.White,
                    strength = if (sel) 1.8f else 1f,
                    onClick = { onTab(i) }
                ) {
                    Row(
                        Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(item.second, null)
                        Spacer(Modifier.width(10.dp))
                        Text(item.first)
                    }
                }
            }
        }
    }
}
