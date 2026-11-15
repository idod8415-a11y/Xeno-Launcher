package com.xeno.launcher

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
