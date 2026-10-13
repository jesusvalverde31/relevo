package com.jesusvalverde.relevo.data

import com.jesusvalverde.relevo.domain.*
import org.json.JSONArray
import org.json.JSONObject

object BackupCodec {
    fun encode(assets: List<Asset>, events: List<AssetEvent>): String = JSONObject().apply {
        put("format", "relevo"); put("version", 1)
        put("assets", JSONArray().apply { assets.forEach { a -> put(JSONObject().apply {
            put("id", a.id); put("name", a.name); put("category", a.category); put("location", a.location)
            put("serial", a.serial); put("notes", a.notes); put("maintenanceDate", a.maintenanceDate)
            put("status", a.status.name); put("holder", a.holder); put("returnDate", a.returnDate); put("demo", a.demo)
        }) } })
        put("events", JSONArray().apply { events.forEach { e -> put(JSONObject().apply {
            put("id", e.id); put("assetId", e.assetId); put("assetName", e.assetName); put("message", e.message); put("timestamp", e.timestamp)
        }) } })
    }.toString(2)

    fun decode(text: String): Pair<List<Asset>, List<AssetEvent>> {
        val root = JSONObject(text)
        require(root.getString("format") == "relevo" && root.getInt("version") == 1) { "Formato de copia no compatible." }
        val rows = root.getJSONArray("assets")
        require(rows.length() <= 10000) { "La copia supera 10.000 equipos." }
        val assets = (0 until rows.length()).map { i -> rows.getJSONObject(i).let { a ->
            Asset(a.getString("id"), a.getString("name"), a.getString("category"), a.getString("location"), a.getString("serial"), a.getString("notes"), a.getString("maintenanceDate"), AssetStatus.valueOf(a.getString("status")), a.getString("holder"), a.getString("returnDate"), a.optBoolean("demo"))
        } }
        require(assets.all { it.id.isNotBlank() && AssetRules.validate(it) == null }) { "La copia contiene equipos inválidos." }
        require(assets.map { it.id }.distinct().size == assets.size) { "La copia contiene identificadores duplicados." }
        val eventsArray = root.getJSONArray("events")
        require(eventsArray.length() <= 100000) { "La copia contiene demasiados movimientos." }
        val events = (0 until eventsArray.length()).map { i -> eventsArray.getJSONObject(i).let { e ->
            AssetEvent(e.getString("id"), e.getString("assetId"), e.getString("assetName"), e.getString("message"), e.getLong("timestamp"))
        } }
        require(events.map { it.id }.distinct().size == events.size) { "La copia contiene movimientos duplicados." }
        return assets to events
    }
}
