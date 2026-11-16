package com.xeno.launcher

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun JavaScreen() {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val clipboard = LocalClipboardManager.current
    var status by remember { mutableStateOf("") }
    var log by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var installed by remember { mutableStateOf(JavaRuntime.find(ctx)?.home?.name) }
    var lwjglInfo by remember { mutableStateOf(LwjglRuntime.summary(ctx)) }

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

    val lwjglPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        if (uris.isNotEmpty()) {
            busy = true
            status = "Импорт LWJGL…"
            scope.launch {
                try {
                    LwjglRuntime.importZips(ctx, uris) { status = it }
                } catch (e: Exception) {
                    status = "Ошибка: ${e.message}"
                }
                lwjglInfo = LwjglRuntime.summary(ctx)
                busy = false
            }
        }
    }

    LaunchedEffect(Unit) {
        while (true) {
            log = withContext(Dispatchers.IO) {
                val f = JavaRuntime.logFile(ctx)
                if (f.exists()) f.readText().takeLast(12000) else ""
            }
            delay(1000)
        }
    }

    Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Column(
            Modifier.width(250.dp).fillMaxHeight().verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                "Java",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Text(if (installed != null) "Установлена: $installed" else "Java не импортирована")
            Button(
                onClick = { picker.launch(arrayOf("*/*")) },
                enabled = !busy,
                modifier = Modifier.fillMaxWidth()
            ) { Text("Импорт Java") }
            Text(lwjglInfo, style = MaterialTheme.typography.bodySmall)
            Button(
                onClick = { lwjglPicker.launch(arrayOf("*/*")) },
                enabled = !busy,
                modifier = Modifier.fillMaxWidth()
            ) { Text("Импорт LWJGL (2 zip)") }
            if (status.isNotEmpty()) Text(status, style = MaterialTheme.typography.bodySmall)
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
                Icon(Icons.Filled.PlayArrow, null)
                Spacer(Modifier.width(6.dp))
                Text("java -version")
            }
            OutlinedButton(
                onClick = { clipboard.setText(AnnotatedString(log)) },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Копировать лог") }
        }
        GlassBox(Modifier.weight(1f).fillMaxHeight(), radius = 20.dp) {
            val scroll = rememberScrollState()
            LaunchedEffect(log) { scroll.scrollTo(scroll.maxValue) }
            Column(Modifier.verticalScroll(scroll).padding(12.dp)) {
                Text(log, fontFamily = FontFamily.Monospace, fontSize = 10.sp)
            }
        }
    }
}
