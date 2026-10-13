package com.jesusvalverde.relevo

import android.graphics.Bitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.jesusvalverde.relevo.data.AssetDatabase
import com.jesusvalverde.relevo.domain.Asset
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.Assert.*
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class InventoryFlowTest {
    @get:Rule val compose = createEmptyComposeRule()
    private lateinit var scenario: ActivityScenario<MainActivity>
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext
    @Before fun start() {
        context.deleteDatabase("relevo.db")
        scenario = ActivityScenario.launch(MainActivity::class.java)
    }
    @After fun stop() { scenario.close() }

    @Test fun createEditLoanAndReturn() {
        compose.onNodeWithText("Nuevo equipo").performClick()
        compose.onNodeWithText("Nombre del equipo *").performTextInput("Taladro de prueba")
        compose.onNodeWithText("Ubicación habitual").performTextInput("Almacén")
        compose.onNodeWithText("Guardar equipo").performClick()
        compose.onNodeWithText("Equipos", useUnmergedTree = true).performClick()
        compose.waitUntil(15000) { compose.onAllNodesWithText("Taladro de prueba").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("Taladro de prueba").performClick()
        compose.onNodeWithText("Prestar").performClick()
        compose.onNodeWithText("Quién lo recibe").performTextInput("Ana")
        compose.onNodeWithText("Registrar", substring = false).performClick()
        compose.waitUntil(15000) { compose.onAllNodesWithText("Devuelto").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("Devuelto").performClick()
        compose.waitUntil(15000) { compose.onAllNodesWithText("Prestar", substring = false).fetchSemanticsNodes().isNotEmpty() }
        scenario.recreate()
        compose.waitForIdle()
        AssetDatabase(context).use { db ->
            val item = db.assets().single()
            assertEquals("Taladro de prueba", item.name)
            assertEquals("Almacén", item.location)
            assertEquals("", item.holder)
            assertEquals(3, db.events().size)
        }
    }

    @Test fun demoIsOptionalAndCaptureInventory() {
        AssetDatabase(context).use { assertTrue(it.assets().isEmpty()) }
        compose.onNodeWithText("Explorar con ejemplos").performScrollTo().performClick()
        compose.waitUntil(15000) { compose.onAllNodesWithText("Modo de ejemplo:", substring = true).fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("Equipos", useUnmergedTree = true).performClick()
        compose.waitForIdle()
        val directory = File(context.getExternalFilesDir(null), "screenshots").apply { mkdirs() }
        File(directory, "inventory.png").outputStream().use { compose.onRoot().captureToImage().asAndroidBitmap().compress(Bitmap.CompressFormat.PNG, 100, it) }
        compose.onNodeWithText("Resumen", useUnmergedTree = true).performClick()
        compose.waitForIdle()
        File(directory, "dashboard.png").outputStream().use { compose.onRoot().captureToImage().asAndroidBitmap().compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    @Test fun failedRestoreRollsBackOriginalInventory() {
        AssetDatabase(context).use { db ->
            val original = Asset(name = "Original")
            db.save(original, "Alta")
            val duplicate = Asset(name = "Duplicado")
            runCatching { db.replaceAll(listOf(duplicate, duplicate), emptyList()) }
            assertEquals(listOf(original), db.assets())
            assertEquals(1, db.events().size)
        }
    }
}
