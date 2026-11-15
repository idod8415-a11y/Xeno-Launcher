package com.xeno.launcher

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URL

data class McVersion(val id: String, val type: String, val url: String)

object VersionRepo {
    private const val MANIFEST =
        "https://piston-meta.mojang.com/mc/game/version_manifest_v2.json"

    suspend fun load(): List<McVersion> = withContext(Dispatchers.IO) {
        val text = URL(MANIFEST).openConnection().apply {
            connectTimeout = 15000
            readTimeout = 15000
        }.getInputStream().bufferedReader().use { it.readText() }
        val arr = JSONObject(text).getJSONArray("versions")
        List(arr.length()) { i ->
            val o = arr.getJSONObject(i)
            McVersion(o.getString("id"), o.getString("type"), o.getString("url"))
        }
    }
}
