package com.xeno.launcher

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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
                    { Icon(Icons.Filled.Build, null) }, label = { Text("Java") })
                NavigationBarItem(tab == 2, { tab = 2 },
                    { Icon(Icons.Filled.Settings, null) }, label = { Text("Настройки") })
            }
        }
    ) { pad ->
        Box(Modifier.padding(pad).fillMaxSize()) {
            when (tab) {
                0 -> PlayScreen(selected) { selected = it }
                1 -> JavaScreen()
                else -> SettingsScreen(nick, { nick = it }, ramMb, { ramMb = it })
            }
        }
    }
}

@Composable
fun JavaScreen() {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var status by remember { mutableStateOf("") }
    var log by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var installed by remember { mutableStateOf(JavaRuntime.find(ctx)?.home?.name) }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            busy = true
            status = "Импорт…"
            scope.launch {
                try {
                    JavaRuntime.importFrom(ctx, uri) { status = it }
                } catch (e: Exception) {
                    status = "Ошибка: ${e.message}"
                }
                installed = JavaRuntime.find(ctx)?.home?.name
                busy = false
            }
        }
    }

    LaunchedEffect(Unit) {
        while (true) {
            log = withContext(Dispatchers.IO) {
                val f = JavaRuntime.logFile(ctx)
                if (f.exists()) f.readText().takeLast(6000) else ""
            }
            delay(1000)
        }
    }

    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Java", style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary)
        Text(if (installed != null) "Установлена: $installed" else "Java не импортирована")
        Button(
            onClick = { picker.launch(arrayOf("*/*")) },
            enabled = !busy,
            modifier = Modifier.fillMaxWidth()
        ) { Text("Импорт Java (zip или tar.xz)") }
        if (status.isNotEmpty()) Text(status)
        Button(
            onClick = {
                log = "Запуск…"
                ctx.startService(
                    Intent(ctx, JavaService::class.java)
                        .putExtra("args", arrayOf("-version"))
                )
            },
            enabled = installed != null && !busy,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Filled.Play
