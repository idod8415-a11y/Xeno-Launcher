package com.xeno.launcher

import android.content.Context
import android.os.Build
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.ui.graphics.Color

enum class XenoTheme(val title: String) {
    DARK("Тёмная"),
    GREEN("Зелёная"),
    CHERRY("Вишнёвая"),
    DYNAMIC("Android 12+")
}

fun dynamicAvailable(): Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

fun schemeFor(t: XenoTheme, ctx: Context): ColorScheme = when (t) {
    XenoTheme.DARK -> darkColorScheme(
        primary = Color(0xFFB8C4FF),
        background = Color(0xFF0F1115),
        surface = Color(0xFF1A1D23)
    )
    XenoTheme.GREEN -> darkColorScheme(
        primary = Color(0xFF7CFFB2),
        background = Color(0xFF0B1410),
        surface = Color(0xFF14231B)
    )
    XenoTheme.CHERRY -> darkColorScheme(
        primary = Color(0xFFFF8AA8),
        background = Color(0xFF160B10),
        surface = Color(0xFF26131A)
    )
    XenoTheme.DYNAMIC ->
        if (dynamicAvailable()) dynamicDarkColorScheme(ctx) else darkColorScheme()
}
