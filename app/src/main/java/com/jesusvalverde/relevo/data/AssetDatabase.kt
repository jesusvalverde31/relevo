package com.jesusvalverde.relevo.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.jesusvalverde.relevo.domain.*

/** All calls run on the ViewModel's IO dispatcher. Asset and audit writes are atomic. */
class AssetDatabase(context: Context) : SQLiteOpenHelper(context, "relevo.db", null, 1) {
    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("CREATE TABLE assets (id TEXT PRIMARY KEY, name TEXT NOT NULL, category TEXT NOT NULL, location TEXT NOT NULL, serial TEXT NOT NULL, notes TEXT NOT NULL, maintenanceDate TEXT NOT NULL, status TEXT NOT NULL, holder TEXT NOT NULL, returnDate TEXT NOT NULL, demo INTEGER NOT NULL)")
        db.execSQL("CREATE TABLE events (id TEXT PRIMARY KEY, assetId TEXT NOT NULL, assetName TEXT NOT NULL, message TEXT NOT NULL, timestamp INTEGER NOT NULL)")
        db.execSQL("CREATE INDEX events_time ON events(timestamp DESC)")
    }
    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) { error("Missing migration from $oldVersion to $newVersion") }

    fun assets(): List<Asset> = readableDatabase.query("assets", null, null, null, null, null, "name COLLATE NOCASE").use { c ->
        buildList { while (c.moveToNext()) {
            fun s(key: String) = c.getString(c.getColumnIndexOrThrow(key))
            add(Asset(s("id"), s("name"), s("category"), s("location"), s("serial"), s("notes"), s("maintenanceDate"), AssetStatus.valueOf(s("status")), s("holder"), s("returnDate"), s("demo") == "1"))
        } }
    }
    fun events(): List<AssetEvent> = readableDatabase.query("events", null, null, null, null, null, "timestamp DESC").use { c ->
        buildList { while (c.moveToNext()) {
            fun s(key: String) = c.getString(c.getColumnIndexOrThrow(key))
            add(AssetEvent(s("id"), s("assetId"), s("assetName"), s("message"), c.getLong(c.getColumnIndexOrThrow("timestamp"))))
        } }
    }
    fun save(asset: Asset, message: String) {
        require(AssetRules.validate(asset) == null) { AssetRules.validate(asset).orEmpty() }
        transaction { db ->
            db.insertOrThrow("assets", null, values(asset).also {
                db.delete("assets", "id = ?", arrayOf(asset.id))
            })
            insertEvent(db, AssetEvent(assetId = asset.id, assetName = asset.name, message = message))
        }
    }
    fun delete(asset: Asset) = transaction { db ->
        db.delete("assets", "id = ?", arrayOf(asset.id))
        insertEvent(db, AssetEvent(assetId = asset.id, assetName = asset.name, message = "Equipo eliminado"))
    }
    fun replaceAll(assets: List<Asset>, events: List<AssetEvent>) = transaction { db ->
        db.delete("assets", null, null); db.delete("events", null, null)
        assets.forEach { db.insertOrThrow("assets", null, values(it)) }
        events.forEach { insertEvent(db, it) }
    }
    private fun values(a: Asset) = ContentValues().apply {
        put("id", a.id); put("name", a.name); put("category", a.category); put("location", a.location)
        put("serial", a.serial); put("notes", a.notes); put("maintenanceDate", a.maintenanceDate)
        put("status", a.status.name); put("holder", a.holder); put("returnDate", a.returnDate); put("demo", if (a.demo) 1 else 0)
    }
    private fun insertEvent(db: SQLiteDatabase, e: AssetEvent) {
        db.insertOrThrow("events", null, ContentValues().apply { put("id", e.id); put("assetId", e.assetId); put("assetName", e.assetName); put("message", e.message); put("timestamp", e.timestamp) })
    }
    private fun transaction(block: (SQLiteDatabase) -> Unit) {
        val db = writableDatabase
        db.beginTransaction()
        try { block(db); db.setTransactionSuccessful() } finally { db.endTransaction() }
    }
}
