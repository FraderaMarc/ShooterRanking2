package com.marcfradera.shooterranking.ui.screens

import android.content.Context
import android.content.Intent
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.marcfradera.shooterranking.data.model.Jugador
import com.marcfradera.shooterranking.data.model.JugadorRankingItem
import com.marcfradera.shooterranking.data.model.Sessio
import com.marcfradera.shooterranking.ui.vm.JugadorSessionsExport
import com.marcfradera.shooterranking.ui.vm.JugadorsViewModel
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream

private enum class RankingFilter(val label: String) {
    TOTAL("Tirs totals"),
    FREE_THROW("Tir lliure"),
    THREE_PT("Triples"),
    TWO_PT("Tirs de 2")
}

private val PLAYER_POSITIONS = listOf(
    "Base",
    "Escolta",
    "Aler",
    "Aler-Pivot",
    "Pivot"
)

private data class TeamExportRow(
    val jugador: Jugador,
    val sessions: Int,
    val tlMade: Int,
    val tlAttempted: Int,
    val t2Made: Int,
    val t2Attempted: Int,
    val t3Made: Int,
    val t3Attempted: Int,
    val totalMade: Int,
    val totalAttempted: Int,
    val rightPct: Float?,
    val leftPct: Float?,
    val bestSide: String,
    val bestZoneT2: String,
    val bestZoneT3: String
) {
    val tlPct: Float? get() = rankingPctOrNull(tlMade, tlAttempted)
    val t2Pct: Float? get() = rankingPctOrNull(t2Made, t2Attempted)
    val t3Pct: Float? get() = rankingPctOrNull(t3Made, t3Attempted)
    val totalPct: Float? get() = rankingPctOrNull(totalMade, totalAttempted)
}

private data class TeamExportBestValues(
    val sessions: Int,
    val tlMade: Int,
    val tlPct: Float?,
    val t2Made: Int,
    val t2Pct: Float?,
    val t3Made: Int,
    val t3Pct: Float?,
    val totalMade: Int,
    val totalPct: Float?,
    val rightPct: Float?,
    val leftPct: Float?
)

private data class RankingPdfColumn(
    val title: String,
    val width: Float
)

private data class RankingZoneStat(
    val label: String,
    val made: Int,
    val attempted: Int
) {
    fun pct(): Float = if (attempted <= 0) 0f else made.toFloat() / attempted.toFloat()
}

@Composable
fun JugadorsRankingScreen(
    idEquip: String,
    onBack: () -> Unit,
    onOpenStats: (String, String) -> Unit,
    onOpenShotMap: (String, String) -> Unit
) {
    val vm: JugadorsViewModel = viewModel()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var showDialog by remember { mutableStateOf(false) }
    var selectedFilter by remember { mutableStateOf(RankingFilter.TOTAL) }
    var editItem by remember { mutableStateOf<JugadorRankingItem?>(null) }
    var deleteItem by remember { mutableStateOf<JugadorRankingItem?>(null) }

    LaunchedEffect(idEquip, selectedFilter) {
        vm.load(idEquip, selectedFilter.name)
    }

    CenteredScaffold(title = "Classificació", onBack = onBack) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = { showDialog = true },
                modifier = Modifier.weight(1f)
            ) {
                Text("Afegir jugador")
            }

            Button(
                onClick = {
                    scope.launch {
                        try {
                            val players = vm.loadPlayersForExport(idEquip)
                            if (players.isEmpty()) {
                                Toast.makeText(
                                    context,
                                    "No hi ha jugadores per exportar",
                                    Toast.LENGTH_LONG
                                ).show()
                            } else {
                                exportAllPlayersStatsPdfAndShare(
                                    context = context,
                                    players = players
                                )
                            }
                        } catch (e: Exception) {
                            Toast.makeText(
                                context,
                                e.message ?: "No s'ha pogut generar el PDF",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                },
                modifier = Modifier.weight(1f)
            ) {
                Text("Exportar equip PDF")
            }
        }

        Spacer(Modifier.height(16.dp))

        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            RankingFilter.entries.forEach { filter ->
                FilterChip(
                    selected = selectedFilter == filter,
                    onClick = { selectedFilter = filter },
                    label = { Text(filter.label) }
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        when {
            vm.ranking.loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            vm.ranking.error != null -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = vm.ranking.error ?: "Error",
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }

            else -> {
                val ranked = vm.ranking.data ?: emptyList()

                if (ranked.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Encara no hi ha jugadores.")
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        itemsIndexed(
                            items = ranked,
                            key = { _, item -> item.jugador.id_jugador }
                        ) { idx, item ->
                            PlayerRow(
                                rank = idx + 1,
                                item = item,
                                onStats = {
                                    onOpenStats(
                                        item.jugador.id_jugador,
                                        item.jugador.nom_jugador
                                    )
                                },
                                onShots = {
                                    onOpenShotMap(
                                        item.jugador.id_jugador,
                                        item.jugador.nom_jugador
                                    )
                                },
                                onEdit = { editItem = item },
                                onDelete = { deleteItem = item }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showDialog) {
        CreateJugadorDialog(
            onDismiss = { showDialog = false },
            onCreate = { nom, dorsal, posicio ->
                vm.create(nom, dorsal, posicio, idEquip) {
                    showDialog = false
                    vm.load(idEquip, selectedFilter.name)
                }
            }
        )
    }

    editItem?.let { item ->
        EditJugadorDialog(
            initialNom = item.jugador.nom_jugador,
            initialDorsal = item.jugador.numero_jugador,
            initialPosicio = item.jugador.posicio_jugador,
            onDismiss = { editItem = null },
            onSave = { nom, dorsal, posicio ->
                vm.update(
                    item.jugador.id_jugador,
                    nom,
                    dorsal,
                    posicio,
                    idEquip,
                    selectedFilter.name
                ) {
                    editItem = null
                }
            }
        )
    }

    deleteItem?.let { item ->
        DeleteJugadorDialog(
            nomJugador = item.jugador.nom_jugador,
            onDismiss = { deleteItem = null },
            onConfirm = {
                vm.delete(item.jugador.id_jugador, idEquip, selectedFilter.name) {
                    deleteItem = null
                }
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PlayerRow(
    rank: Int,
    item: JugadorRankingItem,
    onStats: () -> Unit,
    onShots: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = {},
                onLongClick = { expanded = true }
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "$rank. ${item.jugador.nom_jugador}",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = "Dorsal ${formatDorsal(item.jugador.numero_jugador)} · ${positionLabel(item.jugador.posicio_jugador)}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "Sessions: ${item.sessions}",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }

                    Box(
                        modifier = Modifier.padding(horizontal = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = buildAnnotatedString {
                                append("${item.made}/${item.attempted} ")
                                withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                                    append(formatRankingPct(item.pct))
                                }
                            },
                            style = MaterialTheme.typography.titleMedium
                        )
                    }

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        TextButton(onClick = onStats) {
                            Text("Stats")
                        }
                        TextButton(onClick = onShots) {
                            Text("Tir")
                        }
                    }
                }

                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Editar") },
                        onClick = {
                            expanded = false
                            onEdit()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Eliminar") },
                        onClick = {
                            expanded = false
                            onDelete()
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun CreateJugadorDialog(
    onDismiss: () -> Unit,
    onCreate: (String, Int, String) -> Unit
) {
    var nom by remember { mutableStateOf("") }
    var dorsal by remember { mutableStateOf("") }
    var posicio by remember { mutableStateOf("") }

    val dorsalValue = dorsal.toIntOrNull()
    val dorsalValid =
        dorsal.isNotBlank() &&
                dorsal.all { it.isDigit() } &&
                dorsalValue != null &&
                dorsalValue in 0..100

    val posicioValid = posicio in PLAYER_POSITIONS
    val formValid = nom.isNotBlank() && dorsalValid && posicioValid

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nou jugador") },
        text = {
            Column {
                OutlinedTextField(
                    value = nom,
                    onValueChange = { nom = it },
                    label = { Text("Nom") },
                    singleLine = true
                )

                Spacer(Modifier.height(8.dp))

                OutlinedTextField(
                    value = dorsal,
                    onValueChange = { input ->
                        if (input.all { it.isDigit() } && input.length <= 3) {
                            val value = input.toIntOrNull()
                            if (input.isEmpty() || (value != null && value in 0..100)) {
                                dorsal = input
                            }
                        }
                    },
                    label = { Text("Dorsal (0-100)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )

                Spacer(Modifier.height(8.dp))

                PositionDropdownField(
                    selected = posicio,
                    onSelect = { posicio = it }
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val d = dorsal.toIntOrNull()
                    if (nom.isNotBlank() && d != null && d in 0..100 && posicioValid) {
                        onCreate(nom.trim(), d, posicio)
                    }
                },
                enabled = formValid
            ) {
                Text("Crear")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel·lar")
            }
        }
    )
}

@Composable
private fun EditJugadorDialog(
    initialNom: String,
    initialDorsal: Int,
    initialPosicio: String,
    onDismiss: () -> Unit,
    onSave: (String, Int, String) -> Unit
) {
    var nom by remember { mutableStateOf(initialNom) }
    var dorsal by remember { mutableStateOf(initialDorsal.toString()) }
    var posicio by remember {
        mutableStateOf(
            positionLabel(initialPosicio).takeIf { it in PLAYER_POSITIONS } ?: ""
        )
    }

    val dorsalValue = dorsal.toIntOrNull()
    val dorsalValid =
        dorsal.isNotBlank() &&
                dorsal.all { it.isDigit() } &&
                dorsalValue != null &&
                dorsalValue in 0..100

    val posicioValid = posicio in PLAYER_POSITIONS
    val formValid = nom.isNotBlank() && dorsalValid && posicioValid

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Editar jugadora") },
        text = {
            Column {
                OutlinedTextField(
                    value = nom,
                    onValueChange = { nom = it },
                    label = { Text("Nom") },
                    singleLine = true
                )

                Spacer(Modifier.height(8.dp))

                OutlinedTextField(
                    value = dorsal,
                    onValueChange = { input ->
                        if (input.all { it.isDigit() } && input.length <= 3) {
                            val value = input.toIntOrNull()
                            if (input.isEmpty() || (value != null && value in 0..100)) {
                                dorsal = input
                            }
                        }
                    },
                    label = { Text("Dorsal (0-100)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )

                Spacer(Modifier.height(8.dp))

                PositionDropdownField(
                    selected = posicio,
                    onSelect = { posicio = it }
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val d = dorsal.toIntOrNull()
                    if (nom.isNotBlank() && d != null && d in 0..100 && posicioValid) {
                        onSave(nom.trim(), d, posicio)
                    }
                },
                enabled = formValid
            ) {
                Text("Guardar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel·lar")
            }
        }
    )
}

@Composable
private fun PositionDropdownField(
    selected: String,
    onSelect: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Column {
        Text(
            text = "Posició",
            style = MaterialTheme.typography.labelMedium
        )

        Spacer(Modifier.height(4.dp))

        Box(modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(
                onClick = { expanded = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = if (selected.isBlank()) "Selecciona posició" else selected,
                    modifier = Modifier.weight(1f)
                )
                Text("▼")
            }

            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier.fillMaxWidth(0.92f)
            ) {
                PLAYER_POSITIONS.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option) },
                        onClick = {
                            onSelect(option)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun DeleteJugadorDialog(
    nomJugador: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Eliminar jugadora") },
        text = {
            Text("Vols eliminar $nomJugador? També s'eliminaran totes les seves sessions de tir.")
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("Eliminar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel·lar")
            }
        }
    )
}

private fun formatRankingPct(pct: Double): String {
    val normalized = if (pct in 0.0..1.0) pct * 100.0 else pct
    return "${normalized.toInt()}%"
}

private fun formatDorsal(dorsal: Int): String {
    return if (dorsal == 100) "00" else dorsal.toString()
}

private fun positionLabel(posicio: String): String {
    return when (posicio.trim().lowercase()) {
        "1", "base" -> "Base"
        "2", "escolta" -> "Escolta"
        "3", "aler" -> "Aler"
        "4", "aler-pivot", "aler pivot", "aler-pivot " -> "Aler-Pivot"
        "5", "pivot", "pívot" -> "Pivot"
        else -> "Posició desconeguda"
    }
}

private fun exportAllPlayersStatsPdfAndShare(
    context: Context,
    players: List<JugadorSessionsExport>
) {
    val rows = players
        .map { buildTeamExportRow(it) }
        .sortedWith(
            compareByDescending<TeamExportRow> { it.totalPct ?: -1f }
                .thenByDescending { it.totalMade }
                .thenBy { it.jugador.nom_jugador.lowercase() }
        )

    val bestValues = TeamExportBestValues(
        sessions = rows.maxOfOrNull { it.sessions } ?: 0,
        tlMade = rows.maxOfOrNull { it.tlMade } ?: 0,
        tlPct = rows.mapNotNull { it.tlPct }.maxOrNull(),
        t2Made = rows.maxOfOrNull { it.t2Made } ?: 0,
        t2Pct = rows.mapNotNull { it.t2Pct }.maxOrNull(),
        t3Made = rows.maxOfOrNull { it.t3Made } ?: 0,
        t3Pct = rows.mapNotNull { it.t3Pct }.maxOrNull(),
        totalMade = rows.maxOfOrNull { it.totalMade } ?: 0,
        totalPct = rows.mapNotNull { it.totalPct }.maxOrNull(),
        rightPct = rows.mapNotNull { it.rightPct }.maxOrNull(),
        leftPct = rows.mapNotNull { it.leftPct }.maxOrNull()
    )

    val document = PdfDocument()
    val pageWidth = 1650
    val pageHeight = 1000
    val margin = 55f
    val headerTop = 120f
    val rowHeight = 42f

    val columns = listOf(
        RankingPdfColumn("Jugadora", 190f),
        RankingPdfColumn("Sess.", 65f),
        RankingPdfColumn("TL", 90f),
        RankingPdfColumn("TL %", 70f),
        RankingPdfColumn("T2", 90f),
        RankingPdfColumn("T2%", 70f),
        RankingPdfColumn("T3", 90f),
        RankingPdfColumn("T3%", 70f),
        RankingPdfColumn("TOTAL", 100f),
        RankingPdfColumn("TOTAL %", 80f),
        RankingPdfColumn("Dreta %", 85f),
        RankingPdfColumn("Esquerra %", 95f),
        RankingPdfColumn("Millor costat", 120f),
        RankingPdfColumn("Millor zona T2", 160f),
        RankingPdfColumn("Millor zona T3", 160f)
    )

    val rowsPerPage = ((pageHeight - headerTop - 80f) / rowHeight).toInt().coerceAtLeast(1)

    rows.chunked(rowsPerPage).forEachIndexed { pageIndex, chunk ->
        val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageIndex + 1).create()
        val page = document.startPage(pageInfo)
        val canvas = page.canvas

        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 28f
            isFakeBoldText = true
            color = android.graphics.Color.BLACK
        }
        val subtitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 16f
            color = android.graphics.Color.DKGRAY
        }

        canvas.drawText("Estadistiques equip", margin, 55f, titlePaint)
        canvas.drawText("Taula global per jugadores", margin, 85f, subtitlePaint)

        drawRankingPdfTableHeader(
            canvas = canvas,
            startX = margin,
            startY = headerTop,
            columns = columns,
            rowHeight = rowHeight
        )

        var currentY = headerTop + rowHeight
        chunk.forEach { row ->
            drawTeamExportRow(
                canvas = canvas,
                row = row,
                bestValues = bestValues,
                columns = columns,
                startX = margin,
                startY = currentY,
                rowHeight = rowHeight
            )
            currentY += rowHeight
        }

        document.finishPage(page)
    }

    val file = File(context.cacheDir, "equip_estadistiques.pdf")
    FileOutputStream(file).use { output ->
        document.writeTo(output)
    }
    document.close()

    val uri: Uri = FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        file
    )

    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "application/pdf"
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }

    context.startActivity(Intent.createChooser(intent, "Compartir PDF"))
}

private fun buildTeamExportRow(player: JugadorSessionsExport): TeamExportRow {
    val sessions = player.sessions.sortedBy { it.num_sessio }

    val tlMade = sessions.sumOf { it.fets_pos_6 }
    val tlAttempted = sessions.sumOf { it.tirs_pos_6 }

    val t2Made = sessions.sumOf { it.fets_pos_4 + it.fets_pos_5 + it.fets_pos_7 + it.fets_pos_8 + it.fets_pos_9 }
    val t2Attempted = sessions.sumOf { it.tirs_pos_4 + it.tirs_pos_5 + it.tirs_pos_7 + it.tirs_pos_8 + it.tirs_pos_9 }

    val t3Made = sessions.sumOf { it.fets_pos_1 + it.fets_pos_2 + it.fets_pos_3 + it.fets_pos_10 + it.fets_pos_11 }
    val t3Attempted = sessions.sumOf { it.tirs_pos_1 + it.tirs_pos_2 + it.tirs_pos_3 + it.tirs_pos_10 + it.tirs_pos_11 }

    val totalMade = tlMade + t2Made + t3Made
    val totalAttempted = tlAttempted + t2Attempted + t3Attempted

    val rightMade = sessions.sumOf { it.fets_pos_1 + it.fets_pos_4 + it.fets_pos_7 + it.fets_pos_10 }
    val rightAttempted = sessions.sumOf { it.tirs_pos_1 + it.tirs_pos_4 + it.tirs_pos_7 + it.tirs_pos_10 }

    val leftMade = sessions.sumOf { it.fets_pos_3 + it.fets_pos_5 + it.fets_pos_9 + it.fets_pos_11 }
    val leftAttempted = sessions.sumOf { it.tirs_pos_3 + it.tirs_pos_5 + it.tirs_pos_9 + it.tirs_pos_11 }

    val merged = mergeSessionsRanking(sessions, player.jugador.id_jugador)

    val rightPct = rankingPctOrNull(rightMade, rightAttempted)
    val leftPct = rankingPctOrNull(leftMade, leftAttempted)

    return TeamExportRow(
        jugador = player.jugador,
        sessions = sessions.size,
        tlMade = tlMade,
        tlAttempted = tlAttempted,
        t2Made = t2Made,
        t2Attempted = t2Attempted,
        t3Made = t3Made,
        t3Attempted = t3Attempted,
        totalMade = totalMade,
        totalAttempted = totalAttempted,
        rightPct = rightPct,
        leftPct = leftPct,
        bestSide = rankingBestSideLabel(rightPct, leftPct),
        bestZoneT2 = rankingBestZoneT2Label(merged),
        bestZoneT3 = rankingBestZoneT3Label(merged)
    )
}

private fun drawRankingPdfTableHeader(
    canvas: android.graphics.Canvas,
    startX: Float,
    startY: Float,
    columns: List<RankingPdfColumn>,
    rowHeight: Float
) {
    val backgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = android.graphics.Color.parseColor("#F0F0F0")
    }
    val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 1.5f
        color = android.graphics.Color.parseColor("#8A8A8A")
    }
    val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = 16f
        isFakeBoldText = true
        color = android.graphics.Color.BLACK
    }

    var x = startX
    columns.forEach { column ->
        val rect = RectF(x, startY, x + column.width, startY + rowHeight)
        canvas.drawRect(rect, backgroundPaint)
        canvas.drawRect(rect, borderPaint)
        val textY = startY + rowHeight / 2f - (textPaint.descent() + textPaint.ascent()) / 2f
        canvas.drawText(column.title, x + 8f, textY, textPaint)
        x += column.width
    }
}

private fun drawTeamExportRow(
    canvas: android.graphics.Canvas,
    row: TeamExportRow,
    bestValues: TeamExportBestValues,
    columns: List<RankingPdfColumn>,
    startX: Float,
    startY: Float,
    rowHeight: Float
) {
    val backgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }
    val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 1.5f
        color = android.graphics.Color.parseColor("#8A8A8A")
    }
    val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = 16f
        color = android.graphics.Color.BLACK
    }

    val values = listOf(
        row.jugador.nom_jugador,
        row.sessions.toString(),
        "${row.tlMade}/${row.tlAttempted}",
        row.tlPct.toRankingPdfPercent(),
        "${row.t2Made}/${row.t2Attempted}",
        row.t2Pct.toRankingPdfPercent(),
        "${row.t3Made}/${row.t3Attempted}",
        row.t3Pct.toRankingPdfPercent(),
        "${row.totalMade}/${row.totalAttempted}",
        row.totalPct.toRankingPdfPercent(),
        row.rightPct.toRankingPdfPercent(),
        row.leftPct.toRankingPdfPercent(),
        row.bestSide,
        row.bestZoneT2,
        row.bestZoneT3
    )

    var x = startX
    values.forEachIndexed { index, value ->
        val width = columns[index].width
        val highlight = when (index) {
            1 -> bestValues.sessions > 0 && row.sessions == bestValues.sessions
            2 -> bestValues.tlMade > 0 && row.tlMade == bestValues.tlMade
            3 -> bestValues.tlPct != null && row.tlPct != null && row.tlPct == bestValues.tlPct
            4 -> bestValues.t2Made > 0 && row.t2Made == bestValues.t2Made
            5 -> bestValues.t2Pct != null && row.t2Pct != null && row.t2Pct == bestValues.t2Pct
            6 -> bestValues.t3Made > 0 && row.t3Made == bestValues.t3Made
            7 -> bestValues.t3Pct != null && row.t3Pct != null && row.t3Pct == bestValues.t3Pct
            8 -> bestValues.totalMade > 0 && row.totalMade == bestValues.totalMade
            9 -> bestValues.totalPct != null && row.totalPct != null && row.totalPct == bestValues.totalPct
            10 -> bestValues.rightPct != null && row.rightPct != null && row.rightPct == bestValues.rightPct
            11 -> bestValues.leftPct != null && row.leftPct != null && row.leftPct == bestValues.leftPct
            else -> false
        }

        backgroundPaint.color = if (highlight) {
            android.graphics.Color.parseColor("#DFF3E3")
        } else {
            android.graphics.Color.WHITE
        }

        val rect = RectF(x, startY, x + width, startY + rowHeight)
        canvas.drawRect(rect, backgroundPaint)
        canvas.drawRect(rect, borderPaint)

        val textY = startY + rowHeight / 2f - (textPaint.descent() + textPaint.ascent()) / 2f
        canvas.drawText(value, x + 8f, textY, textPaint)
        x += width
    }
}

private fun mergeSessionsRanking(
    sessions: List<Sessio>,
    jugadorId: String
): Sessio {
    return Sessio(
        num_sessio = 0,
        id_jugador = jugadorId,
        fets_pos_1 = sessions.sumOf { it.fets_pos_1 },
        tirs_pos_1 = sessions.sumOf { it.tirs_pos_1 },
        fets_pos_2 = sessions.sumOf { it.fets_pos_2 },
        tirs_pos_2 = sessions.sumOf { it.tirs_pos_2 },
        fets_pos_3 = sessions.sumOf { it.fets_pos_3 },
        tirs_pos_3 = sessions.sumOf { it.tirs_pos_3 },
        fets_pos_4 = sessions.sumOf { it.fets_pos_4 },
        tirs_pos_4 = sessions.sumOf { it.tirs_pos_4 },
        fets_pos_5 = sessions.sumOf { it.fets_pos_5 },
        tirs_pos_5 = sessions.sumOf { it.tirs_pos_5 },
        fets_pos_6 = sessions.sumOf { it.fets_pos_6 },
        tirs_pos_6 = sessions.sumOf { it.tirs_pos_6 },
        fets_pos_7 = sessions.sumOf { it.fets_pos_7 },
        tirs_pos_7 = sessions.sumOf { it.tirs_pos_7 },
        fets_pos_8 = sessions.sumOf { it.fets_pos_8 },
        tirs_pos_8 = sessions.sumOf { it.tirs_pos_8 },
        fets_pos_9 = sessions.sumOf { it.fets_pos_9 },
        tirs_pos_9 = sessions.sumOf { it.tirs_pos_9 },
        fets_pos_10 = sessions.sumOf { it.fets_pos_10 },
        tirs_pos_10 = sessions.sumOf { it.tirs_pos_10 },
        fets_pos_11 = sessions.sumOf { it.fets_pos_11 },
        tirs_pos_11 = sessions.sumOf { it.tirs_pos_11 }
    )
}

private fun rankingBestZoneT2Label(s: Sessio): String {
    val zones = listOf(
        RankingZoneStat("Poste alt dreta", s.fets_pos_4, s.tirs_pos_4),
        RankingZoneStat("Poste alt esquerra", s.fets_pos_5, s.tirs_pos_5),
        RankingZoneStat("Poste baix dreta", s.fets_pos_7, s.tirs_pos_7),
        RankingZoneStat("Ampolla", s.fets_pos_8, s.tirs_pos_8),
        RankingZoneStat("Poste baix esquerra", s.fets_pos_9, s.tirs_pos_9)
    ).filter { it.attempted > 0 }

    if (zones.isEmpty()) return "-"

    return zones.maxWithOrNull(
        compareBy<RankingZoneStat> { it.pct() }.thenBy { it.attempted }
    )?.label ?: "-"
}

private fun rankingBestZoneT3Label(s: Sessio): String {
    val zones = listOf(
        RankingZoneStat("45 dreta", s.fets_pos_1, s.tirs_pos_1),
        RankingZoneStat("Mig", s.fets_pos_2, s.tirs_pos_2),
        RankingZoneStat("45 esquerra", s.fets_pos_3, s.tirs_pos_3),
        RankingZoneStat("Cantonada dreta", s.fets_pos_10, s.tirs_pos_10),
        RankingZoneStat("Cantonada esquerra", s.fets_pos_11, s.tirs_pos_11)
    ).filter { it.attempted > 0 }

    if (zones.isEmpty()) return "-"

    return zones.maxWithOrNull(
        compareBy<RankingZoneStat> { it.pct() }.thenBy { it.attempted }
    )?.label ?: "-"
}

private fun rankingBestSideLabel(rightPct: Float?, leftPct: Float?): String {
    return when {
        rightPct == null && leftPct == null -> "-"
        rightPct != null && leftPct == null -> "Dreta"
        rightPct == null && leftPct != null -> "Esquerra"
        rightPct!! > leftPct!! -> "Dreta"
        leftPct > rightPct -> "Esquerra"
        else -> "Igual"
    }
}

private fun rankingPctOrNull(made: Int, attempted: Int): Float? {
    if (attempted <= 0) return null
    return (made.toFloat() / attempted.toFloat()) * 100f
}

private fun Float?.toRankingPdfPercent(): String {
    return this?.let { "${it.toInt()}%" } ?: "-"
}