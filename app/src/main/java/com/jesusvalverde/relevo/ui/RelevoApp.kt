@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.jesusvalverde.relevo.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.AssignmentReturn
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jesusvalverde.relevo.domain.*
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val Forest = Color(0xFF153E35)
private val Lime = Color(0xFFD8F77A)
private val LightColors = lightColorScheme(primary = Forest, onPrimary = Color.White, primaryContainer = Color(0xFFE3EEDD), secondary = Color(0xFF5C693E), background = Color(0xFFF7F8F2), surface = Color(0xFFFCFDF8), surfaceVariant = Color(0xFFE9EDE3), onSurface = Color(0xFF19291F))
private val DarkColors = darkColorScheme(primary = Lime, onPrimary = Forest, background = Color(0xFF111A16), surface = Color(0xFF19261F), primaryContainer = Forest)

@Composable fun RelevoApp(vm: RelevoViewModel = viewModel()) {
    MaterialTheme(colorScheme = if (isSystemInDarkTheme()) DarkColors else LightColors) {
        val state by vm.state.collectAsStateWithLifecycle()
        var tab by rememberSaveable { mutableIntStateOf(0) }
        var selectedId by rememberSaveable { mutableStateOf<String?>(null) }
        var editorId by rememberSaveable { mutableStateOf<String?>(null) }
        var creating by rememberSaveable { mutableStateOf(false) }
        var settings by rememberSaveable { mutableStateOf(false) }
        var restoreUri by remember { mutableStateOf<Uri?>(null) }
        val snackbar = remember { SnackbarHostState() }
        val export = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { it?.let(vm::export) }
        val restore = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { restoreUri = it }
        LaunchedEffect(state.message) { state.message?.let { snackbar.showSnackbar(it); vm.clearMessage() } }
        Scaffold(
            snackbarHost = { SnackbarHost(snackbar) },
            bottomBar = {
                NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                    listOf("Resumen" to Icons.Outlined.Dashboard, "Equipos" to Icons.Outlined.Inventory2, "Actividad" to Icons.Outlined.History).forEachIndexed { i, pair ->
                        NavigationBarItem(selected = tab == i, onClick = { tab = i }, icon = { Icon(pair.second, null) }, label = { Text(pair.first) })
                    }
                }
            },
            floatingActionButton = {
                if (!state.loading) ExtendedFloatingActionButton(onClick = { creating = true }, containerColor = Forest, contentColor = Lime, icon = { Icon(Icons.Outlined.Add, null) }, text = { Text("Nuevo equipo") })
            }
        ) { padding ->
            Column(Modifier.fillMaxSize().padding(padding)) {
                Row(Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Surface(color = Forest, shape = RoundedCornerShape(12.dp)) { Text("r", color = Lime, fontSize = 28.sp, fontWeight = FontWeight.Black, modifier = Modifier.padding(horizontal = 15.dp, vertical = 3.dp)) }
                    Spacer(Modifier.width(10.dp))
                    Text("relevo", fontSize = 27.sp, fontWeight = FontWeight.Bold, letterSpacing = (-1).sp, modifier = Modifier.weight(1f))
                    IconButton(onClick = { settings = true }) { Icon(Icons.Outlined.Settings, "Ajustes y copias de seguridad") }
                }
                if (state.loading) LinearProgressIndicator(Modifier.fillMaxWidth())
                when (tab) {
                    0 -> Dashboard(state, onSelect = { selectedId = it }, onAdd = { creating = true }, onDemo = vm::demo, onInventory = { tab = 1 })
                    1 -> Inventory(state.assets) { selectedId = it }
                    2 -> ActivityList(state.events)
                }
            }
        }
        val selected = state.assets.firstOrNull { it.id == selectedId }
        if (selected != null && !creating && editorId == null) {
            AssetDetail(selected, state.events.filter { it.assetId == selected.id }, state.loading, onDismiss = { selectedId = null }, onEdit = { editorId = selected.id }, onLend = { person, due -> vm.lend(selected.id, person, due) }, onReturn = { vm.returnAsset(selected.id) }, onService = { vm.service(selected.id) }, onDelete = { vm.delete(selected.id); selectedId = null })
        }
        if (creating || editorId != null) {
            val editing = state.assets.firstOrNull { it.id == editorId }
            AssetEditor(editing, onDismiss = { creating = false; editorId = null }, onSave = { vm.save(it); creating = false; editorId = null })
        }
        if (settings) ModalBottomSheet(onDismissRequest = { settings = false }) {
            Column(Modifier.fillMaxWidth().padding(24.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text("Tu espacio, tus datos", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text("Relevo 0.1 · Versión de prueba", color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("Tus equipos se guardan en este móvil. No necesitas cuenta y la app no envía datos a servidores. Exporta una copia antes de desinstalar o cambiar de dispositivo.")
                Button(onClick = { export.launch("relevo-${LocalDate.now()}.json"); settings = false }, enabled = !state.loading, modifier = Modifier.fillMaxWidth()) { Icon(Icons.Outlined.FileDownload, null); Spacer(Modifier.width(8.dp)); Text("Guardar copia de seguridad") }
                OutlinedButton(onClick = { restore.launch(arrayOf("application/json", "text/plain", "application/octet-stream")); settings = false }, enabled = !state.loading, modifier = Modifier.fillMaxWidth()) { Text("Restaurar una copia") }
                Text("La copia incluye los nombres de responsables y no está cifrada. Guárdala en un lugar privado.", style = MaterialTheme.typography.bodySmall)
                if (state.assets.any { it.demo }) TextButton(onClick = { vm.removeDemo(); settings = false }, enabled = !state.loading) { Text("Eliminar solo los ejemplos DEMO") }
                Spacer(Modifier.height(16.dp))
            }
        }
        restoreUri?.let { uri -> AlertDialog(onDismissRequest = { restoreUri = null }, title = { Text("¿Restaurar esta copia?") }, text = { Text("Reemplazará los equipos y el historial actuales. Guarda primero una copia si quieres conservarlos. Los archivos inválidos no modifican tus datos.") }, confirmButton = { TextButton(onClick = { vm.restore(uri); restoreUri = null }) { Text("Restaurar") } }, dismissButton = { TextButton(onClick = { restoreUri = null }) { Text("Cancelar") } }) }
    }
}

@Composable private fun Dashboard(state: RelevoState, onSelect: (String) -> Unit, onAdd: () -> Unit, onDemo: () -> Unit, onInventory: () -> Unit) {
    val assets = state.assets
    val alerts = assets.filter { AssetRules.overdue(it) || AssetRules.maintenanceDue(it) }
    LazyColumn(contentPadding = PaddingValues(start = 24.dp, end = 24.dp, top = 12.dp, bottom = 100.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
        item {
            Text("TODO BAJO CONTROL", style = MaterialTheme.typography.labelSmall, letterSpacing = 2.sp, color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(8.dp))
            Text("Menos buscar.\nMás hacer.", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Text("Tu equipo de trabajo, siempre localizado.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        item {
            Surface(color = Forest, shape = RoundedCornerShape(28.dp)) {
                Column(Modifier.fillMaxWidth().padding(24.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.Inventory2, null, tint = Lime)
                        Spacer(Modifier.width(10.dp))
                        Text("TU INVENTARIO", color = Lime, letterSpacing = 1.5.sp, style = MaterialTheme.typography.labelMedium)
                    }
                    Spacer(Modifier.height(20.dp))
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text("${assets.size}", color = Color.White, fontSize = 60.sp, fontWeight = FontWeight.Bold)
                        Text(" equipos registrados", color = Color(0xFFCCDCD1), modifier = Modifier.padding(bottom = 12.dp))
                    }
                    HorizontalDivider(Modifier.padding(vertical = 16.dp), color = Color.White.copy(alpha = .16f))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        HeroMetric("${assets.count { it.status == AssetStatus.AVAILABLE }}", "Disponibles")
                        HeroMetric("${assets.count { it.status == AssetStatus.LOANED }}", "Prestados")
                        HeroMetric("${assets.count { it.status == AssetStatus.SERVICE }}", "En revisión")
                    }
                }
            }
        }
        if (assets.isEmpty()) {
            item { EmptyCard("El orden empieza aquí", "Añade tu primer equipo. Después podrás registrar quién lo tiene y cuándo debe volver.", Icons.Outlined.AddBox) {
                Button(onClick = onAdd, enabled = !state.loading) { Text("Registrar mi primer equipo") }
                TextButton(onClick = onDemo, enabled = !state.loading) { Text("Explorar con ejemplos") }
            } }
        } else {
            if (assets.any { it.demo }) item { Text("Modo de ejemplo: los equipos DEMO son ficticios. Puedes eliminarlos desde Ajustes.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            item { SectionTitle("Necesita tu atención", "${alerts.size}") }
            if (alerts.isEmpty()) item { EmptyCard("Todo al día", "No hay devoluciones vencidas ni revisiones previstas en los próximos 7 días.", Icons.Outlined.CheckCircle) {} }
            items(alerts, key = { "alert-${it.id}" }) { asset ->
                AssetCard(asset, if (AssetRules.overdue(asset)) "Devolución pendiente · ${displayDate(asset.returnDate)}" else "Mantenimiento · ${displayDate(asset.maintenanceDate)}", onClick = { onSelect(asset.id) })
            }
            item { Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) { Text("A mano", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f)); TextButton(onClick = onInventory) { Text("Ver todos") } } }
            items(assets.take(3), key = { "recent-${it.id}" }) { asset -> AssetCard(asset, onClick = { onSelect(asset.id) }) }
        }
    }
}

@Composable private fun HeroMetric(value: String, label: String) { Column { Text(value, color = Color.White, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold); Text(label, color = Color(0xFFCCDCD1), style = MaterialTheme.typography.labelSmall) } }

@Composable private fun Inventory(assets: List<Asset>, onSelect: (String) -> Unit) {
    var query by rememberSaveable { mutableStateOf("") }
    var filter by rememberSaveable { mutableStateOf<String?>(null) }
    val visible = assets.filter { (filter == null || it.status.name == filter) && listOf(it.name, it.location, it.serial, it.holder, it.category).any { text -> text.contains(query.trim(), ignoreCase = true) } }
    LazyColumn(contentPadding = PaddingValues(24.dp, 12.dp, 24.dp, 100.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item { Text("Tus equipos", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold); Text("Cada cosa en su sitio.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
        item { OutlinedTextField(value = query, onValueChange = { query = it }, modifier = Modifier.fillMaxWidth(), singleLine = true, label = { Text("Buscar equipo, persona o ubicación") }, leadingIcon = { Icon(Icons.Outlined.Search, null) }, shape = RoundedCornerShape(18.dp)) }
        item {
            Column {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = filter == null, onClick = { filter = null }, label = { Text("Todos") })
                    FilterChip(selected = filter == AssetStatus.AVAILABLE.name, onClick = { filter = AssetStatus.AVAILABLE.name }, label = { Text("Disponibles") })
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = filter == AssetStatus.LOANED.name, onClick = { filter = AssetStatus.LOANED.name }, label = { Text("Prestados") })
                    FilterChip(selected = filter == AssetStatus.SERVICE.name, onClick = { filter = AssetStatus.SERVICE.name }, label = { Text("En revisión") })
                }
            }
        }
        item { Text("${visible.size} equipos", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        items(visible, key = { it.id }) { asset -> AssetCard(asset, onClick = { onSelect(asset.id) }) }
        if (visible.isEmpty()) item { EmptyCard("No hay equipos aquí", "Añade un equipo o cambia la búsqueda y los filtros.", Icons.Outlined.SearchOff) {} }
    }
}

@Composable private fun AssetCard(asset: Asset, alert: String? = null, onClick: () -> Unit) {
    Card(onClick = onClick, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), shape = RoundedCornerShape(22.dp)) {
        Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = RoundedCornerShape(14.dp)) { Icon(categoryIcon(asset.category), null, Modifier.padding(13.dp).size(25.dp), tint = MaterialTheme.colorScheme.primary) }
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(asset.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Text(asset.location.ifBlank { "Sin ubicación" }, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Icon(Icons.Outlined.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                StatusPill(asset.status)
                if (asset.demo) Text("DEMO", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (asset.holder.isNotEmpty()) Text(asset.holder, style = MaterialTheme.typography.labelMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            if (alert != null) Text(alert, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelMedium)
        }
    }
}

@Composable private fun StatusPill(status: AssetStatus) {
    val color = when (status) { AssetStatus.AVAILABLE -> MaterialTheme.colorScheme.primary; AssetStatus.LOANED -> if (isSystemInDarkTheme()) Color(0xFFFFCD80) else Color(0xFF805000); AssetStatus.SERVICE -> if (isSystemInDarkTheme()) Color(0xFFCAC0FF) else Color(0xFF64529B) }
    Surface(color = color.copy(alpha = .10f), contentColor = color, shape = RoundedCornerShape(8.dp)) { Text(status.label, Modifier.padding(horizontal = 9.dp, vertical = 5.dp), style = MaterialTheme.typography.labelSmall) }
}

@Composable private fun ActivityList(events: List<AssetEvent>) {
    LazyColumn(contentPadding = PaddingValues(24.dp, 12.dp, 24.dp, 100.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
        item { Text("Cada movimiento\ncuenta.", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold); Spacer(Modifier.height(8.dp)); Text("El historial de tus equipos en un solo lugar.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
        if (events.isEmpty()) item { EmptyCard("Una historia por empezar", "Aquí aparecerán las altas, préstamos, devoluciones y revisiones.", Icons.Outlined.History) {} }
        items(events, key = { it.id }) { EventRow(it) }
    }
}

@Composable private fun EventRow(event: AssetEvent) {
    Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        Icon(Icons.Outlined.RadioButtonChecked, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(top = 4.dp).size(18.dp))
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(event.assetName, fontWeight = FontWeight.SemiBold)
            Text(event.message, style = MaterialTheme.typography.bodyMedium)
            Text(Instant.ofEpochMilli(event.timestamp).atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("dd MMM · HH:mm")), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable private fun AssetDetail(asset: Asset, events: List<AssetEvent>, busy: Boolean, onDismiss: () -> Unit, onEdit: () -> Unit, onLend: (String, String) -> Unit, onReturn: () -> Unit, onService: () -> Unit, onDelete: () -> Unit) {
    var lendDialog by rememberSaveable { mutableStateOf(false) }
    var deleteDialog by rememberSaveable { mutableStateOf(false) }
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        LazyColumn(contentPadding = PaddingValues(24.dp, 8.dp, 24.dp, 36.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
            item { StatusPill(asset.status); Spacer(Modifier.height(14.dp)); Text(asset.name, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold); Text(asset.category, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            item { Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                if (asset.status == AssetStatus.AVAILABLE) Button(onClick = { lendDialog = true }, enabled = !busy, modifier = Modifier.weight(1f)) { Text("Prestar") }
                if (asset.status == AssetStatus.LOANED) Button(onClick = onReturn, enabled = !busy, modifier = Modifier.weight(1f)) { Icon(Icons.AutoMirrored.Outlined.AssignmentReturn, null); Spacer(Modifier.width(6.dp)); Text("Devuelto") }
                OutlinedButton(onClick = onEdit, enabled = !busy) { Text("Editar ficha") }
            } }
            item { Info("Ubicación", asset.location.ifBlank { "Sin registrar" }); Info("Número de serie", asset.serial.ifBlank { "Sin registrar" })
                if (asset.status == AssetStatus.LOANED) { Info("Responsable", asset.holder); Info("Devolución prevista", displayDate(asset.returnDate)) }
                Info("Próximo mantenimiento", if (asset.maintenanceDate.isBlank()) "Sin programar" else displayDate(asset.maintenanceDate))
                if (asset.notes.isNotBlank()) Info("Notas", asset.notes)
            }
            if (asset.status != AssetStatus.LOANED) item { OutlinedButton(onClick = onService, enabled = !busy, modifier = Modifier.fillMaxWidth()) { Icon(Icons.Outlined.Build, null); Spacer(Modifier.width(8.dp)); Text(if (asset.status == AssetStatus.SERVICE) "Finalizar revisión" else "Enviar a revisión") } }
            item { HorizontalDivider(); Spacer(Modifier.height(16.dp)); Text("Historial del equipo", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) }
            items(events, key = { it.id }) { EventRow(it) }
            item { TextButton(onClick = { deleteDialog = true }, enabled = !busy) { Text("Eliminar equipo", color = MaterialTheme.colorScheme.error) } }
        }
    }
    if (lendDialog) LoanDialog(asset, onDismiss = { lendDialog = false }, onConfirm = { person, due -> onLend(person, due); lendDialog = false })
    if (deleteDialog) AlertDialog(onDismissRequest = { deleteDialog = false }, title = { Text("¿Eliminar ${asset.name}?") }, text = { Text("La ficha se eliminará. Los movimientos anteriores se conservarán en Actividad.") }, confirmButton = { TextButton(onClick = { onDelete(); deleteDialog = false }) { Text("Eliminar") } }, dismissButton = { TextButton(onClick = { deleteDialog = false }) { Text("Cancelar") } })
}

@Composable private fun LoanDialog(asset: Asset, onDismiss: () -> Unit, onConfirm: (String, String) -> Unit) {
    var person by rememberSaveable { mutableStateOf("") }
    var due by rememberSaveable { mutableStateOf(LocalDate.now().plusDays(7).toString()) }
    var error by rememberSaveable { mutableStateOf<String?>(null) }
    AlertDialog(onDismissRequest = onDismiss, title = { Text("Registrar préstamo") }, text = {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(asset.name)
            OutlinedTextField(person, { person = it.take(100) }, label = { Text("Quién lo recibe") }, singleLine = true)
            OutlinedTextField(due, { due = it.take(10) }, label = { Text("Devolución · AAAA-MM-DD") }, singleLine = true)
            error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        }
    }, confirmButton = { TextButton(onClick = {
        val validation = runCatching { AssetRules.lend(asset, person, due) }
        if (validation.isSuccess) onConfirm(person.trim(), due) else error = validation.exceptionOrNull()?.message
    }) { Text("Registrar") } }, dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } })
}

@Composable private fun AssetEditor(existing: Asset?, onDismiss: () -> Unit, onSave: (Asset) -> Unit) {
    var name by rememberSaveable { mutableStateOf(existing?.name.orEmpty()) }
    var category by rememberSaveable { mutableStateOf(existing?.category ?: "Herramientas") }
    var location by rememberSaveable { mutableStateOf(existing?.location.orEmpty()) }
    var serial by rememberSaveable { mutableStateOf(existing?.serial.orEmpty()) }
    var notes by rememberSaveable { mutableStateOf(existing?.notes.orEmpty()) }
    var maintenance by rememberSaveable { mutableStateOf(existing?.maintenanceDate.orEmpty()) }
    var error by rememberSaveable { mutableStateOf<String?>(null) }
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Column(Modifier.fillMaxSize().safeDrawingPadding().imePadding()) {
                Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onDismiss) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Volver") }
                    Text(if (existing == null) "Nuevo equipo" else "Editar equipo", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                }
                Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text("Dale un lugar\na cada herramienta.", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                    Text("Solo el nombre es obligatorio. Completa el resto cuando quieras.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    EditorField(name, { name = it.take(100) }, "Nombre del equipo *")
                    EditorField(category, { category = it.take(60) }, "Categoría")
                    EditorField(location, { location = it.take(150) }, "Ubicación habitual")
                    EditorField(serial, { serial = it.take(100) }, "Número de serie")
                    EditorField(maintenance, { maintenance = it.take(10) }, "Mantenimiento · AAAA-MM-DD")
                    OutlinedTextField(notes, { notes = it.take(2000) }, label = { Text("Notas") }, minLines = 3, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp))
                    error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                    Spacer(Modifier.height(12.dp))
                }
                Button(onClick = {
                    val asset = (existing ?: Asset(name = name)).copy(name = name.trim(), category = category.trim().ifBlank { "Otros" }, location = location.trim(), serial = serial.trim(), notes = notes.trim(), maintenanceDate = maintenance.trim())
                    error = AssetRules.validate(asset)
                    if (error == null) onSave(asset)
                }, modifier = Modifier.fillMaxWidth().padding(24.dp).heightIn(min = 52.dp), shape = RoundedCornerShape(16.dp)) { Text("Guardar equipo") }
            }
        }
    }
}

@Composable private fun EditorField(value: String, onChange: (String) -> Unit, label: String) { OutlinedTextField(value, onChange, label = { Text(label) }, singleLine = true, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp)) }
@Composable private fun Info(label: String, value: String) { Column(Modifier.padding(vertical = 8.dp)) { Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant); Spacer(Modifier.height(4.dp)); Text(value) } }
@Composable private fun SectionTitle(title: String, count: String) { Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) { Text(title, Modifier.weight(1f), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold); Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(10.dp)) { Text(count, Modifier.padding(horizontal = 12.dp, vertical = 6.dp), fontWeight = FontWeight.Bold) } } }
@Composable private fun EmptyCard(title: String, text: String, icon: ImageVector, actions: @Composable ColumnScope.() -> Unit) {
    Surface(color = MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(22.dp)) {
        Column(Modifier.fillMaxWidth().padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) { Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(30.dp)); Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold); Text(text, color = MaterialTheme.colorScheme.onSurfaceVariant); actions() }
    }
}
private fun categoryIcon(category: String): ImageVector = when { category.contains("med", true) -> Icons.Outlined.Straighten; category.contains("acces", true) -> Icons.Outlined.Layers; else -> Icons.Outlined.Construction }
private fun displayDate(date: String): String = runCatching { LocalDate.parse(date).format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) }.getOrDefault(date)
