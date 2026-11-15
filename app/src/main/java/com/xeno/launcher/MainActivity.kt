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
import androidx.compose.ui.Modifier
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
    val names = listOf("Играть", "Java", "Настройки")

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            Column {
                DiagonalHeader("XENO", names[tab])
                NavigationBar(windowInsets = WindowInsets(0.dp)) {
                    NavigationBarItem(tab == 0, { tab = 0 },
                        { Icon(Icons.Filled.Home, null) }, label = { Text("Играть") })
                    NavigationBarItem(tab == 1, { tab = 1 },
                        { Icon(Icons.Filled.Build, null) }, label = { Text("Java") })
                    NavigationBarItem(tab == 2, { tab = 2 },
                        { Icon(Icons.Filled.Settings, null) }, label = { Text("Настройки") })
                }
            }
        }
    ) { pad ->
        Box(Modifier.padding(pad).fillMaxSize()) {
            when (tab) {
                0 -> PlayScreen(selected) { selected = it }
                1 -> JavaScreen()
                else -> SettingsScreen(nick, { nick = it }, ramMb, { ramMb = it }, theme, onTheme)
            }
        }
    }
}
