package com.jesusvalverde.relevo.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.jesusvalverde.relevo.data.*
import com.jesusvalverde.relevo.domain.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate

data class RelevoState(val assets: List<Asset> = emptyList(), val events: List<AssetEvent> = emptyList(), val loading: Boolean = true, val message: String? = null)

class RelevoViewModel(app: Application) : AndroidViewModel(app) {
    private val db = AssetDatabase(app)
    private val mutable = MutableStateFlow(RelevoState())
    private var initialized = false
    val state = mutable.asStateFlow()
    init { operate { } }
    private fun operate(success: String? = null, action: suspend () -> Unit) {
        if (mutable.value.loading && initialized) return
        initialized = true
        mutable.value = mutable.value.copy(loading = true)
        viewModelScope.launch {
            try {
                val loaded = withContext(Dispatchers.IO) { action(); db.assets() to db.events() }
                mutable.value = RelevoState(loaded.first, loaded.second, false, success)
            } catch (e: Exception) {
                mutable.value = mutable.value.copy(loading = false, message = e.message ?: "No se pudo completar la operación.")
            }
        }
    }
    fun clearMessage() { mutable.value = mutable.value.copy(message = null) }
    fun save(asset: Asset) = operate("Equipo guardado") {
        db.save(asset, if (db.assets().any { it.id == asset.id }) "Ficha actualizada" else "Equipo registrado")
    }
    private fun current(id: String) = db.assets().firstOrNull { it.id == id } ?: error("Este equipo ya no existe.")
    fun lend(id: String, person: String, due: String) = operate("Préstamo registrado") {
        db.save(AssetRules.lend(current(id), person, due), "Préstamo a ${person.trim()} · devolución $due")
    }
    fun returnAsset(id: String) = operate("Devolución registrada") {
        val asset = current(id)
        db.save(AssetRules.returnAsset(asset), "Devuelto por ${asset.holder}")
    }
    fun service(id: String) = operate("Estado actualizado") {
        val asset = AssetRules.toggleService(current(id))
        db.save(asset, if (asset.status == AssetStatus.SERVICE) "Revisión iniciada" else "Revisión finalizada")
    }
    fun delete(id: String) = operate("Equipo eliminado; se conserva su historial") { db.delete(current(id)) }
    fun demo() = operate("Ejemplos cargados. Están marcados como DEMO.") {
        require(db.assets().isEmpty()) { "Los ejemplos solo se pueden cargar con el inventario vacío." }
        val today = LocalDate.now()
        val assets = listOf(
            Asset(name = "Taladro percutor", category = "Herramientas", location = "Almacén principal", serial = "DEMO-001", notes = "Ejemplo ficticio. Maletín y dos baterías.", demo = true),
            Asset(name = "Nivel láser", category = "Medición", location = "Obra centro", serial = "DEMO-002", status = AssetStatus.LOANED, holder = "Alex · ejemplo", returnDate = today.minusDays(1).toString(), demo = true),
            Asset(name = "Escalera telescópica", category = "Accesorios", location = "Furgoneta 01", maintenanceDate = today.plusDays(3).toString(), demo = true),
            Asset(name = "Lijadora orbital", category = "Herramientas", location = "Taller", status = AssetStatus.SERVICE, demo = true)
        )
        assets.forEach { db.save(it, "Ejemplo de demostración") }
    }
    fun removeDemo() = operate("Ejemplos eliminados") {
        val demoIds = db.assets().filter { it.demo }.map { it.id }.toSet()
        db.replaceAll(db.assets().filterNot { it.demo }, db.events().filterNot { it.assetId in demoIds })
    }
    fun export(uri: Uri) = operate("Copia guardada") {
        val text = BackupCodec.encode(db.assets(), db.events())
        val resolver = getApplication<Application>().contentResolver
        requireNotNull(resolver.openOutputStream(uri, "wt")) { "No se pudo abrir el destino." }.bufferedWriter(Charsets.UTF_8).use { it.write(text) }
    }
    fun restore(uri: Uri) = operate("Copia restaurada") {
        val resolver = getApplication<Application>().contentResolver
        val bytes = requireNotNull(resolver.openInputStream(uri)) { "No se pudo abrir el archivo." }.use { input ->
            val output = java.io.ByteArrayOutputStream()
            val buffer = ByteArray(8192)
            var count = input.read(buffer)
            while (count != -1) {
                require(output.size() + count <= 10 * 1024 * 1024) { "El archivo supera 10 MB." }
                output.write(buffer, 0, count)
                count = input.read(buffer)
            }
            output.toByteArray()
        }
        require(bytes.size <= 10 * 1024 * 1024) { "El archivo supera 10 MB." }
        val (assets, events) = BackupCodec.decode(bytes.toString(Charsets.UTF_8))
        db.replaceAll(assets, events)
    }
    override fun onCleared() { db.close() }
}
