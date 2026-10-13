package com.jesusvalverde.relevo.domain

import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate

class AssetRulesTest {
    private val today = LocalDate.of(2026, 10, 13)
    private val drill = Asset(name = "Taladro")
    @Test fun loanAndReturnPreserveIdentityAndClearBorrower() {
        val loan = AssetRules.lend(drill, "  Ana  ", "2026-10-14", today)
        assertEquals("Ana", loan.holder)
        assertEquals(drill.id, loan.id)
        assertEquals(AssetStatus.LOANED, loan.status)
        assertEquals(drill, AssetRules.returnAsset(loan))
    }
    @Test fun cannotLendAnAlreadyLoanedAsset() {
        val loan = AssetRules.lend(drill, "Ana", "2026-10-14", today)
        assertThrows(IllegalArgumentException::class.java) { AssetRules.lend(loan, "Luis", "2026-10-15", today) }
    }
    @Test fun cannotLendAnAssetUnderMaintenance() {
        assertThrows(IllegalArgumentException::class.java) { AssetRules.lend(drill.copy(status = AssetStatus.SERVICE), "Ana", "2026-10-14", today) }
    }
    @Test fun rejectsPastDateBlankHolderAndImpossibleDate() {
        assertThrows(IllegalArgumentException::class.java) { AssetRules.lend(drill, "Ana", "2026-10-12", today) }
        assertThrows(IllegalArgumentException::class.java) { AssetRules.lend(drill, " ", "2026-10-14", today) }
        assertFalse(AssetRules.validDate("2026-02-30"))
    }
    @Test fun dueTodayIsNotOverdueUntilTomorrow() {
        val loan = AssetRules.lend(drill, "Ana", "2026-10-13", today)
        assertFalse(AssetRules.overdue(loan, today))
        assertTrue(AssetRules.overdue(loan, today.plusDays(1)))
    }
    @Test fun maintenanceWindowIncludesSeventhDayAndOverdue() {
        assertTrue(AssetRules.maintenanceDue(drill.copy(maintenanceDate = "2026-10-20"), today))
        assertTrue(AssetRules.maintenanceDue(drill.copy(maintenanceDate = "2026-10-01"), today))
        assertFalse(AssetRules.maintenanceDue(drill.copy(maintenanceDate = "2026-10-21"), today))
        assertFalse(AssetRules.maintenanceDue(drill, today))
    }
    @Test fun cannotServiceAnAssetUntilReturned() {
        assertThrows(IllegalArgumentException::class.java) { AssetRules.toggleService(drill.copy(status = AssetStatus.LOANED)) }
    }
}
