package com.xeno.launcher

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

@Composable
fun PlayScreen(selected: McVersion?, onSelect: (McVersion) -> Unit) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var versions by remember { mutableStateOf<List<McVersion>>(emptyList()) }
    var error by remember { mutableStateOf<String?>(null) }
    var snapshots by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf("") }
    var progress by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(Unit) {
        try { versions = VersionRepo.load() } catch (e: Exception) { error = e.message }
    }
    val shown = versions.filter { snapshots || it.type == "release" }

    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
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
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clip(SlantShape(48f))
                            .background(
                                if (sel) MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
                                else MaterialTheme.colorScheme.surface
                            )
                            .clickable { onSelect(v) }
                            .padding(horizontal = 32.dp, vertical = 12.dp)
                    ) {
                        Column {
                            Text(v.id, style = MaterialTheme.typography.titleMedium)
                            Text(v.type, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }
        if (status.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            Text(status)
            LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(8.dp))
        }
        Button(
            onClick = {
                val v = selected
                if (v != null && !busy) {
                    busy = true
                    progress = 0f
                    status = "Начинаю…"
                    scope.launch {
                        try {
                            Downloader.install(ctx, v) { s, p -> status = s; progress = p }
                        } catch (e: Exception) {
                            status = "Ошибка: ${e.message}"
                        }
                        busy = false
                    }
                }
            },
            enabled = selected != null && !busy,
            shape = SlantShape(40f),
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Filled.PlayArrow, null)
            Spacer(Modifier.width(8.dp))
            Text(
                when {
                    busy -> "Скачиваю…"
                    selected == null -> "Выберите версию"
                    else -> "Скачать ${selected.id}"
                }
            )
        }
        Spacer(Modifier.height(8.dp))
        OutlinedButton(
            onClick = {
                val v = selected
                if (v != null) {
                    try {
                        val a = GameLauncher.buildArgs(ctx, v.id, "Player", 2048)
                        ctx.startService(
                            Intent(ctx, JavaService::class.java).putExtra("args", a)
                        )
                        status = "Запущено, лог на вкладке Java"
                    } catch (e: Exception) {
                        status = "Ошибка: ${e.message}"
                    }
                }
            },
            enabled = selected != null && !busy,
            shape = SlantShape(40f),
            modifier = Modifier.fillMaxWidth()
        ) { Text("Запустить (тест)") }
        Spacer(Modifier.height(8.dp))
    }
}
