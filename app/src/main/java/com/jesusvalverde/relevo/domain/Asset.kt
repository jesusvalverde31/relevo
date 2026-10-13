package com.jesusvalverde.relevo.domain

import java.time.LocalDate
import java.util.UUID

enum class AssetStatus(val label: String) { AVAILABLE("Disponible"), LOANED("Prestado"), SERVICE("En revisión") }

data class Asset(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val category: String = "Herramientas",
    val location: String = "",
    val serial: String = "",
    val notes: String = "",
    val maintenanceDate: String = "",
    val status: AssetStatus = AssetStatus.AVAILABLE,
    val holder: String = "",
    val returnDate: String = "",
    val demo: Boolean = false,
)

data class AssetEvent(val id: String = UUID.randomUUID().toString(), val assetId: String, val assetName: String, val message: String, val timestamp: Long = System.currentTimeMillis())

object AssetRules {
    fun validDate(value: String): Boolean = value.isEmpty() || runCatching { LocalDate.parse(value) }.isSuccess
    fun validate(asset: Asset): String? = when {
        asset.name.isBlank() -> "Escribe un nombre para el equipo."
        asset.name.length > 100 -> "El nombre no puede superar 100 caracteres."
        !validDate(asset.maintenanceDate) -> "Revisa la fecha de mantenimiento (AAAA-MM-DD)."
        !validDate(asset.returnDate) -> "Revisa la fecha de devolución (AAAA-MM-DD)."
        asset.status == AssetStatus.LOANED && asset.holder.isBlank() -> "Indica quién tiene el equipo."
        else -> null
    }
    fun lend(asset: Asset, holder: String, due: String, today: LocalDate = LocalDate.now()): Asset {
        require(asset.status == AssetStatus.AVAILABLE) { "Solo puedes prestar un equipo disponible." }
        require(holder.isNotBlank()) { "Indica quién recibe el equipo." }
        require(due.isNotBlank() && validDate(due)) { "Indica una fecha de devolución válida." }
        require(!LocalDate.parse(due).isBefore(today)) { "La devolución no puede ser anterior a hoy." }
        return asset.copy(status = AssetStatus.LOANED, holder = holder.trim(), returnDate = due)
    }
    fun returnAsset(asset: Asset): Asset {
        require(asset.status == AssetStatus.LOANED) { "Este equipo no está prestado." }
        return asset.copy(status = AssetStatus.AVAILABLE, holder = "", returnDate = "")
    }
    fun toggleService(asset: Asset): Asset {
        require(asset.status != AssetStatus.LOANED) { "Registra la devolución antes de iniciar una revisión." }
        return asset.copy(status = if (asset.status == AssetStatus.SERVICE) AssetStatus.AVAILABLE else AssetStatus.SERVICE)
    }
    fun overdue(asset: Asset, today: LocalDate = LocalDate.now()): Boolean =
        asset.status == AssetStatus.LOANED && asset.returnDate.isNotEmpty() && LocalDate.parse(asset.returnDate).isBefore(today)
    fun maintenanceDue(asset: Asset, today: LocalDate = LocalDate.now()): Boolean =
        asset.maintenanceDate.isNotEmpty() && !LocalDate.parse(asset.maintenanceDate).isAfter(today.plusDays(7))
}
