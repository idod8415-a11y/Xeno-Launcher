package com.xeno.launcher

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.net.URL
import java.security.MessageDigest
import java.util.concurrent.atomic.AtomicInteger

object Downloader {
    private class Item(val url: String, val file: File, val sha1: String?, val size: Long)

    fun gameDir(ctx: Context): File = File(ctx.getExternalFilesDir(null), "game")

    private fun fetchText(url: String): String =
        URL(url).openConnection().apply {
            connectTimeout = 15000
            readTimeout = 30000
        }.getInputStream().bufferedReader().use { it.readText() }

    private fun sha1(f: File): String {
        val md = MessageDigest.getInstance("SHA-1")
        f.inputStream().use { s ->
            val buf = ByteArray(65536)
            while (true) {
                val n = s.read(buf)
                if (n < 0) break
                md.update(buf, 0, n)
            }
        }
        return md.digest().joinToString("") { "%02x".format(it) }
    }

    private fun isValid(i: Item): Boolean {
        if (!i.file.exists()) return false
        if (i.size > 0 && i.file.length() != i.size) return false
        if (i.sha1 != null && sha1(i.file) != i.sha1) return false
        return true
    }

    private fun download(i: Item) {
        if (isValid(i)) return
        i.file.parentFile?.mkdirs()
        var last: Exception? = null
        for (attempt in 1..3) {
            try {
                val tmp = File(i.file.path + ".part")
                val c = URL(i.url).openConnection().apply {
                    connectTimeout = 15000
                    readTimeout = 30000
                }
                c.getInputStream().use { inp -> tmp.outputStream().use { inp.copyTo(it) } }
                if (i.file.exists()) i.file.delete()
                if (!tmp.renameTo(i.file)) throw IOException("rename failed")
                return
            } catch (e: Exception) {
                last = e
            }
        }
        throw last ?: IOException("download failed")
    }

    private fun allowed(lib: JSONObject): Boolean {
        val rules = lib.optJSONArray("rules") ?: return true
        var ok = false
        for (n in 0 until rules.length()) {
            val r = rules.getJSONObject(n)
            val os = r.optJSONObject("os")
            if (os == null || os.optString("name") == "linux") {
                ok = r.getString("action") == "allow"
            }
        }
        return ok
    }

    suspend fun install(
        ctx: Context,
        v: McVersion,
        onProgress: (String, Float) -> Unit
    ) = withContext(Dispatchers.IO) {
        val root = gameDir(ctx)
        onProgress("Загрузка описания версии…", 0f)
        val vText = fetchText(v.url)
        val vJson = JSONObject(vText)
        val vDir = File(root, "versions/${v.id}")
        vDir.mkdirs()
        File(vDir, "${v.id}.json").writeText(vText)

        val items = mutableListOf<Item>()

        val client = vJson.getJSONObject("downloads").getJSONObject("client")
        items += Item(
            client.getString("url"), File(vDir, "${v.id}.jar"),
            client.getString("sha1"), client.getLong("size")
        )

        val libs = vJson.getJSONArray("libraries")
        for (n in 0 until libs.length()) {
            val lib = libs.getJSONObject(n)
            if (!allowed(lib)) continue
            val art = lib.optJSONObject("downloads")?.optJSONObject("artifact") ?: continue
            val url = art.optString("url")
            if (url.isEmpty()) continue
            items += Item(
                url, File(root, "libraries/${art.getString("path")}"),
                art.optString("sha1").ifEmpty { null }, art.optLong("size")
            )
        }

        onProgress("Загрузка списка ассетов…", 0f)
        val ai = vJson.getJSONObject("assetIndex")
        val aiText = fetchText(ai.getString("url"))
        val aiFile = File(root, "assets/indexes/${ai.getString("id")}.json")
        aiFile.parentFile?.mkdirs()
        aiFile.writeText(aiText)
        val objs = JSONObject(aiText).getJSONObject("objects")
        for (name in objs.keys()) {
            val o = objs.getJSONObject(name)
            val h = o.getString("hash")
            val p = h.substring(0, 2)
            items += Item(
                "https://resources.download.minecraft.net/$p/$h",
                File(root, "assets/objects/$p/$h"), null, o.getLong("size")
            )
        }

        val total = items.size
        val done = AtomicInteger(0)
        val sem = Semaphore(8)
        coroutineScope {
            items.map { item ->
                async {
                    sem.withPermit {
                        download(item)
                        val d = done.incrementAndGet()
                        if (d % 20 == 0 || d == total) {
                            onProgress("Скачано $d из $total", d.toFloat() / total)
                        }
                    }
                }
            }.awaitAll()
        }
        onProgress("Готово: ${v.id}", 1f)
    }
}
