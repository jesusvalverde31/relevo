package com.jesusvalverde.relevo.data

import com.jesusvalverde.relevo.domain.*
import org.junit.Assert.*
import org.junit.Test

class BackupCodecTest {
    @Test fun preservesUnicodeLoanAndHistoryOnRoundTrip() {
        val asset = Asset(name = "Taladro ñ", status = AssetStatus.LOANED, holder = "María", returnDate = "2026-10-14", notes = "Comillas: \"ok\"\nSegunda línea")
        val event = AssetEvent(assetId = asset.id, assetName = asset.name, message = "Préstamo a María")
        val result = BackupCodec.decode(BackupCodec.encode(listOf(asset), listOf(event)))
        assertEquals(listOf(asset), result.first)
        assertEquals(listOf(event), result.second)
    }
    @Test fun refusesDuplicateAssetIdentifiers() {
        val asset = Asset(name = "Taladro")
        assertThrows(IllegalArgumentException::class.java) { BackupCodec.decode(BackupCodec.encode(listOf(asset, asset), emptyList())) }
    }
    @Test fun refusesInvalidDatesBeforeRestoring() {
        val asset = Asset(name = "Taladro", maintenanceDate = "2026-02-30")
        assertThrows(IllegalArgumentException::class.java) { BackupCodec.decode(BackupCodec.encode(listOf(asset), emptyList())) }
    }
    @Test fun refusesUnknownBackupVersion() {
        val text = BackupCodec.encode(emptyList(), emptyList()).replace("\"version\": 1", "\"version\": 99")
        assertThrows(IllegalArgumentException::class.java) { BackupCodec.decode(text) }
    }
    @Test fun refusesInconsistentLoanState() {
        val missingDate = Asset(name = "Taladro", status = AssetStatus.LOANED, holder = "Ana")
        val staleHolder = Asset(name = "Taladro", holder = "Ana")
        listOf(missingDate, staleHolder).forEach { asset ->
            assertThrows(IllegalArgumentException::class.java) { BackupCodec.decode(BackupCodec.encode(listOf(asset), emptyList())) }
        }
    }
}
