package com.xeno.launcher

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

private val XenoColors = darkColorScheme(
    primary = androidx.compose.ui.graphics.Color(0xFF7CFFB2),
    background = androidx.compose.ui.graphics.Color(0xFF0E1116),
    surface = androidx.compose.ui.graphics.Color(0xFF161B22),
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { MaterialTheme(colorScheme = XenoColors) { XenoApp() } }
    }
}

@Composable
fun XenoApp() {
    var tab by remember { mutableIntStateOf(0) }
    var selected by remember { mutableStateOf<McVersion?>(null) }
    var nick by remember { mutableStateOf("Player") }
    var ramMb by remember { mutableFloatStateOf(2048f) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            NavigationBar {
                NavigationBarItem(tab == 0, { tab = 0 },
                    { Icon(Icons.Filled.Home, null) }, label = { Text("Играть") })
                NavigationBarItem(tab == 1, { tab = 1 },
                    { Icon(Icons.Filled.Settings, null) }, label = { Text("Настройки") })
            }
        }
    ) { pad ->
        Box(Modifier.padding(pad).fillMaxSize()) {
            if (tab == 0) PlayScreen(selected, nick, { selected = it })
            else SettingsScreen(nick, { nick = it }, ramMb, { ramMb = it })
        }
    }
}

@Composable
fun PlayScreen(selected: McVersion?, nick: String, onSelect: (McVersion) -> Unit) {
    var versions by remember { mutableStateOf<List<McVersion>>(emptyList()) }
    var error by remember { mutableStateOf<String?>(null) }
    var snapshots by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        try { versions = VersionRepo.load() } catch (e: Exception) { error = e.message }
    }
    val shown = versions.filter { snapshots || it.type == "release" }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("Xeno Launcher", style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Switch(snapshots, { snapshots = it })
            Spacer(Modifier.width(8.dp))
            Text("Показывать снапшоты и старые версии")
        }
        Spacer(Modifier.height(8.dp))
        when {
            error != null -> Text("Ошибка загрузки: $error")
            versions.isEmpty() -> CircularProgressIndicator()
            else -> LazyColumn(Modifier.weight(1f)) {
                items(shown) { v ->
                    val sel = v.id == selected?.id
                    ListItem(
                        headlineContent = { Text(v.id) },
                        supportingContent = { Text(v.type) },
                        colors = ListItemDefaults.colors(
                            containerColor = if (sel) MaterialTheme.colorScheme.surface
                            else MaterialTheme.colorScheme.background),
                        modifier = Modifier.clickable { onSelect(v) }
                    )
                }
            }
        }
        Button(
            onClick = { /* этап 2-4: скачивание и запуск */ },
            enabled = selected != null,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Filled.PlayArrow, null)
            Spacer(Modifier.width(8.dp))
            Text(if (selected == null) "Выберите версию" else "Играть ${selected.id} как $nick")
        }
    }
}

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
