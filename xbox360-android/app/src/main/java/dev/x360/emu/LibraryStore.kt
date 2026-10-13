package dev.x360.emu

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/** Guarda la biblioteca de juegos en SharedPreferences como JSON. */
class LibraryStore(context: Context) {
    private val prefs = context.getSharedPreferences("library", Context.MODE_PRIVATE)

    fun load(): List<Game> {
        val arr = JSONArray(prefs.getString("games", "[]"))
        return (0 until arr.length()).map { arr.getJSONObject(it).let { o -> Game(o.getString("uri"), o.getString("title"), o.optString("titleId").ifEmpty { null }, o.optString("iconPath").ifEmpty { null }) } }
    }

    fun save(games: List<Game>) {
        val arr = JSONArray()
        games.sortedBy { it.title.lowercase() }.forEach { arr.put(JSONObject().put("uri", it.uri).put("title", it.title).put("titleId", it.titleId ?: "").put("iconPath", it.iconPath ?: "")) }
        prefs.edit().putString("games", arr.toString()).apply()
    }
}
