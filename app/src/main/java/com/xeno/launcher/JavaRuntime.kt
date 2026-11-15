package com.xeno.launcher

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.apache.commons.compress.compressors.xz.XZCompressorInputStream
import java.io.BufferedInputStream
import java.io.File
import java.io.FilterInputStream
import java.io.InputStream
import java.nio.file.Files
import java.nio.file.Paths
import java.util.zip.ZipInputStream

class JavaRt(val home: File, val jvm: File, val jli: File)

private class NoClose(i: InputStream) : FilterInputStream(i) {
    override fun close() {}
}

object JavaRuntime {
    fun dir(ctx: Context) = File(ctx.filesDir, "runtime")
    fun logFile(ctx: Context) = File(ctx.filesDir, "java.log")

    fun find(ctx: Context): JavaRt? {
        val d = dir(ctx)
        if (!d.exists()) return null
        val jvm = d.walkTopDown().firstOrNull { it.isFile && it.name == "libjvm.so" } ?: return null
        var p: File? = jvm.parentFile
        while (p != null && p.name != "lib") p = p.parentFile
        val home = p?.parentFile ?: return null
        val jli = d.walkTopDown().firstOrNull { it.isFile && it.name == "libjli.so" } ?: return null
        return JavaRt(home, jvm, jli)
    }

    fun ldPath(ctx: Context, rt: JavaRt): String {
        val l = File(rt.home, "lib")
        val dirs = listOf(
            l, File(l, "server"), File(l, "jli"),
            File(l, "aarch64"), File(l, "aarch64/server"), File(l, "aarch64/jli"),
            rt.jvm.parentFile, rt.jli.parentFile
        )
        return (dirs.filterNotNull().filter { it.isDirectory }.map { it.path } +
                ctx.applicationInfo.nativeLibraryDir).distinct().joinToString(":")
    }

    /** Принимает zip из GitHub (с .tar.xz внутри), саму .tar.xz или zip с файлами JRE. */
    suspend fun importFrom(ctx: Context, uri: Uri, status: (String) -> Unit) =
        withContext(Dispatchers.IO) {
            val dest = dir(ctx)
            dest.deleteRecursively()
            dest.mkdirs()
            val raw = ctx.contentResolver.openInputStream(uri)
                ?: throw IllegalStateException("Не удалось открыть файл")
            BufferedInputStream(raw, 1 shl 16).use { ins ->
                ins.mark(8)
                val head = ByteArray(6)
                ins.read(head)
                ins.reset()
                val isZip = head[0] == 0x50.toByte() && head[1] == 0x4B.toByte()
                val isXz = head[0] == 0xFD.toByte() && head[1] == 0x37.toByte()
                when {
                    isXz -> extractTarXz(NoClose(ins), dest, status)
                    isZip -> extractZip(ins, dest, status)
                    else -> throw IllegalStateException("Неизвестный формат: нужен .zip или .tar.xz")
                }
            }
            val rt = find(ctx)
                ?: throw IllegalStateException("В архиве не нашлась libjvm.so, это не JRE")
            status("Готово: ${rt.home.name}")
        }

    private fun extractZip(src: InputStream, dest: File, status: (String) -> Unit) {
        val root = dest.canonicalPath + File.separator
        val zis = ZipInputStream(src)
        var e = zis.nextEntry
        while (e != null) {
            if (!e.isDirectory) {
                if (e.name.endsWith(".tar.xz") || e.name.endsWith(".txz")) {
                    status("Распаковка ${e.name}…")
                    extractTarXz(NoClose(zis), dest, status)
                } else {
                    val out = File(dest, e.name.trimStart('/'))
                    if (out.canonicalPath.startsWith(root)) {
                        out.parentFile?.mkdirs()
                        out.outputStream().use { zis.copyTo(it) }
                    }
                }
            }
            e = zis.nextEntry
        }
    }

    private fun extractTarXz(src: InputStream, dest: File, status: (String) -> Unit) {
        val tar = TarArchiveInputStream(XZCompressorInputStream(BufferedInputStream(src, 1 shl 16)))
        val root = dest.canonicalPath + File.separator
        var count = 0
        while (true) {
            val e = tar.nextTarEntry ?: break
            val name = e.name.removePrefix("./").trimStart('/')
            if (name.isEmpty()) continue
            val out = File(dest, name)
            if (!out.canonicalPath.startsWith(root)) continue
            when {
                e.isDirectory -> out.mkdirs()
                e.isSymbolicLink -> {
                    out.parentFile?.mkdirs()
                    out.delete()
                    Files.createSymbolicLink(out.toPath(), Paths.get(e.linkName))
                }
                e.isLink -> {
                    out.parentFile?.mkdirs()
                    File(dest, e.linkName.removePrefix("./")).copyTo(out, overwrite = true)
                }
                else -> {
                    out.parentFile?.mkdirs()
                    out.outputStream().use { tar.copyTo(it) }
                    if ((e.mode and 0x49) != 0) out.setExecutable(true, false)
                }
            }
            count++
            if (count % 200 == 0) status("Распаковано файлов: $count")
        }
        status("Распаковано файлов: $count")
    }
}
