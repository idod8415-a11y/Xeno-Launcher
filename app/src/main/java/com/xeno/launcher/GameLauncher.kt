package com.xeno.launcher

import android.content.Context
import org.json.JSONObject
import java.io.File
import java.util.UUID

object GameLauncher {
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

    fun buildArgs(ctx: Context, id: String, nick: String, ramMb: Int): Array<String> {
        val root = Downloader.gameDir(ctx)
        val vDir = File(root, "versions/$id")
        val json = JSONObject(File(vDir, "$id.json").readText())

        val cp = mutableListOf<String>()
        val libs = json.getJSONArray("libraries")
        for (i in 0 until libs.length()) {
            val lib = libs.getJSONObject(i)
            if (!allowed(lib)) continue
            val art = lib.optJSONObject("downloads")?.optJSONObject("artifact") ?: continue
            val f = File(root, "libraries/${art.getString("path")}")
            if (f.exists()) cp += f.path
        }
        cp += File(vDir, "$id.jar").path

        val natives = File(ctx.filesDir, "natives").apply { mkdirs() }
        val uuid = UUID.nameUUIDFromBytes("OfflinePlayer:$nick".toByteArray())
            .toString().replace("-", "")
        val vars = mapOf(
            "auth_player_name" to nick,
            "version_name" to id,
            "game_directory" to root.path,
            "assets_root" to File(root, "assets").path,
            "assets_index_name" to json.getJSONObject("assetIndex").getString("id"),
            "auth_uuid" to uuid,
            "auth_access_token" to "0",
            "user_type" to "mojang",
            "version_type" to json.optString("type", "release")
        )
        fun sub(s: String) = Regex("\\$\\{(\\w+)}").replace(s) { vars[it.groupValues[1]] ?: "0" }

        val game = mutableListOf<String>()
        val newArgs = json.optJSONObject("arguments")?.optJSONArray("game")
        if (newArgs != null) {
            for (i in 0 until newArgs.length()) {
                val a = newArgs.get(i)
                if (a is String) game += sub(a)
            }
        } else {
            json.optString("minecraftArguments").split(" ")
                .filter { it.isNotEmpty() }.forEach { game += sub(it) }
        }

        return arrayOf(
            "-Xmx${ramMb}m",
            "-Djava.library.path=${natives.path}",
            "-cp", cp.joinToString(":"),
            json.getString("mainClass")
        ) + game
    }
}
