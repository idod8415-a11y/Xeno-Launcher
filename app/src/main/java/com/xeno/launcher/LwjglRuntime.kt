package com.xeno.launcher

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.zip.ZipInputStream

object LwjglRuntime {
    fun dir(ctx: Context) = File(ctx.filesDir, "lwjgl")

    fun jars(ctx: Context): List<File> =
        dir(ctx).walkTopDown()
            .filter { it.isFile && it.name.endsWith(".jar") }
            .sortedBy { it.name }
            .toList()

    fun nativesDir(ctx: Context): File? =
        dir(ctx).walkTopDown()
            .firstOrNull { it.isFile && it.name == "liblwjgl.so" }
            ?.parentFile

    fun summary(ctx: Context): String {
        val j = jars(ctx).size
        val n = nativesDir(ctx)
        return if (j == 0 && n == null) "LWJGL не импортирован"
        else "LWJGL: jar-файлов $j, нативные библиотеки: ${if (n != null) "есть" else "нет"}"
    }

    suspend fun importZips(ctx: Context, uris: List<Uri>, status: (String) -> Unit) =
        withContext(Dispatchers.IO) {
            val dest = dir(ctx)
            dest.deleteRecursively()
            dest.mkdirs()
            val root = dest.canonicalPath + File.separator
            var done = 0
            for (uri in uris) {
                val ins = ctx.contentResolver.openInputStream(uri) ?: continue
                ZipInputStream(ins).use { zis ->
                    var e = zis.nextEntry
                    while (e != null) {
                        if (!e.isDirectory) {
                            val out = File(dest, e.name.trimStart('/'))
                            if (out.canonicalPath.startsWith(root)) {
                                out.parentFile?.mkdirs()
                                out.outputStream().use { zis.copyTo(it) }
                            }
                        }
                        e = zis.nextEntry
                    }
                }
                done++
                status("Распаковано архивов: $done")
            }
        }
}
