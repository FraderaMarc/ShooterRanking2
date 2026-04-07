package com.marcfradera.shooterranking.ui.screens

import android.content.Context
import android.content.Intent
import android.graphics.Paint
import android.graphics.Path as AndroidPath
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
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
import kotlin.math.sqrt

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
    val bestZoneT3: String,
    val label: String = jugador.nom_jugador,
    val isTotalRow: Boolean = false
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

private data class TeamPdfProgressPoint(
    val sessionIndex: Int,
    val sessionLabel: String,
    val value: Float?
)

private data class TeamPdfPlayerRow(
    val label: String,
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
)

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
    if (players.isEmpty()) return

    val playerRows = players
        .map { buildTeamExportRow(it) }
        .sortedWith(
            compareByDescending<TeamExportRow> { it.totalPct ?: -1f }
                .thenByDescending { it.totalMade }
                .thenBy { it.jugador.nom_jugador.lowercase() }
        )

    val totalRow = buildTeamTotalRow(players, playerRows)
    val bestValues = buildTeamExportBestValues(playerRows)
    val teamSessions = buildTeamSessionAggregates(players)

    val tripleData = teamSessions.mapIndexed { index, s ->
        TeamPdfProgressPoint(index, s.num_sessio.toString(), s.rankingThreePointPct())
    }
    val freeThrowData = teamSessions.mapIndexed { index, s ->
        TeamPdfProgressPoint(index, s.num_sessio.toString(), s.rankingFreeThrowPct())
    }
    val twoPointData = teamSessions.mapIndexed { index, s ->
        TeamPdfProgressPoint(index, s.num_sessio.toString(), s.rankingTwoPointPct())
    }

    val document = PdfDocument()

    var nextPageNumber = drawTeamSummaryPages(
        document = document,
        startPageNumber = 1,
        rows = playerRows,
        totalRow = totalRow,
        bestValues = bestValues,
        tripleData = tripleData,
        freeThrowData = freeThrowData,
        twoPointData = twoPointData
    )

    val playersById = players.associateBy { it.jugador.id_jugador }
    playerRows.forEach { row ->
        val player = playersById[row.jugador.id_jugador] ?: return@forEach
        nextPageNumber = drawTeamPlayerPages(
            document = document,
            startPageNumber = nextPageNumber,
            nomJugador = player.jugador.nom_jugador,
            sessions = player.sessions.sortedBy { it.num_sessio }
        )
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

private fun buildTeamExportBestValues(rows: List<TeamExportRow>): TeamExportBestValues {
    return TeamExportBestValues(
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
}

private fun buildTeamTotalRow(
    players: List<JugadorSessionsExport>,
    rows: List<TeamExportRow>
): TeamExportRow {
    val allSessions = players.flatMap { it.sessions }.sortedBy { it.num_sessio }
    val merged = mergeSessionsRanking(allSessions, rows.first().jugador.id_jugador)

    val tlMade = rows.sumOf { it.tlMade }
    val tlAttempted = rows.sumOf { it.tlAttempted }
    val t2Made = rows.sumOf { it.t2Made }
    val t2Attempted = rows.sumOf { it.t2Attempted }
    val t3Made = rows.sumOf { it.t3Made }
    val t3Attempted = rows.sumOf { it.t3Attempted }
    val totalMade = rows.sumOf { it.totalMade }
    val totalAttempted = rows.sumOf { it.totalAttempted }

    val rightMade = allSessions.sumOf { it.fets_pos_1 + it.fets_pos_4 + it.fets_pos_7 + it.fets_pos_10 }
    val rightAttempted = allSessions.sumOf { it.tirs_pos_1 + it.tirs_pos_4 + it.tirs_pos_7 + it.tirs_pos_10 }
    val leftMade = allSessions.sumOf { it.fets_pos_3 + it.fets_pos_5 + it.fets_pos_9 + it.fets_pos_11 }
    val leftAttempted = allSessions.sumOf { it.tirs_pos_3 + it.tirs_pos_5 + it.tirs_pos_9 + it.tirs_pos_11 }

    val rightPct = rankingPctOrNull(rightMade, rightAttempted)
    val leftPct = rankingPctOrNull(leftMade, leftAttempted)

    return TeamExportRow(
        jugador = rows.first().jugador,
        sessions = rows.sumOf { it.sessions },
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
        bestZoneT3 = rankingBestZoneT3Label(merged),
        label = "TOTAL EQUIP",
        isTotalRow = true
    )
}

private fun buildTeamSessionAggregates(players: List<JugadorSessionsExport>): List<Sessio> {
    return players
        .flatMap { it.sessions }
        .groupBy { it.num_sessio }
        .toSortedMap()
        .map { (numSessio, groupedSessions) ->
            val merged = mergeSessionsRanking(groupedSessions, "__team__$numSessio")
            Sessio(
                num_sessio = numSessio,
                id_jugador = merged.id_jugador,
                fets_pos_1 = merged.fets_pos_1,
                tirs_pos_1 = merged.tirs_pos_1,
                fets_pos_2 = merged.fets_pos_2,
                tirs_pos_2 = merged.tirs_pos_2,
                fets_pos_3 = merged.fets_pos_3,
                tirs_pos_3 = merged.tirs_pos_3,
                fets_pos_4 = merged.fets_pos_4,
                tirs_pos_4 = merged.tirs_pos_4,
                fets_pos_5 = merged.fets_pos_5,
                tirs_pos_5 = merged.tirs_pos_5,
                fets_pos_6 = merged.fets_pos_6,
                tirs_pos_6 = merged.tirs_pos_6,
                fets_pos_7 = merged.fets_pos_7,
                tirs_pos_7 = merged.tirs_pos_7,
                fets_pos_8 = merged.fets_pos_8,
                tirs_pos_8 = merged.tirs_pos_8,
                fets_pos_9 = merged.fets_pos_9,
                tirs_pos_9 = merged.tirs_pos_9,
                fets_pos_10 = merged.fets_pos_10,
                tirs_pos_10 = merged.tirs_pos_10,
                fets_pos_11 = merged.fets_pos_11,
                tirs_pos_11 = merged.tirs_pos_11
            )
        }
}

private fun drawTeamSummaryPages(
    document: PdfDocument,
    startPageNumber: Int,
    rows: List<TeamExportRow>,
    totalRow: TeamExportRow,
    bestValues: TeamExportBestValues,
    tripleData: List<TeamPdfProgressPoint>,
    freeThrowData: List<TeamPdfProgressPoint>,
    twoPointData: List<TeamPdfProgressPoint>
): Int {
    val pageWidth = 1650
    val pageHeight = 1000
    val margin = 45f
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

    val allRows = rows + totalRow

    var pageNumber = startPageNumber

    val chartArea = RectF(45f, 130f, 830f, 350f)
    val legendY = 430f
    val firstPageHeaderY = 520f
    val otherPageHeaderY = 120f

    val firstPageRows = (((pageHeight - firstPageHeaderY - 40f) / rowHeight).toInt() - 1).coerceAtLeast(1)
    val otherPageRows = (((pageHeight - otherPageHeaderY - 40f) / rowHeight).toInt() - 1).coerceAtLeast(1)

    val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = 28f
        isFakeBoldText = true
        color = android.graphics.Color.BLACK
    }
    val subtitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = 18f
        color = android.graphics.Color.DKGRAY
    }

    val firstPageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
    val firstPage = document.startPage(firstPageInfo)
    val firstCanvas = firstPage.canvas

    firstCanvas.drawText("Estadistiques equip", margin, 55f, titlePaint)
    firstCanvas.drawText("Gràfic global i taula global per jugadores", margin, 90f, subtitlePaint)

    drawRankingPdfProgressChart(
        canvas = firstCanvas,
        area = chartArea,
        title = "Gràfic global equip",
        tripleData = tripleData,
        freeThrowData = freeThrowData,
        twoPointData = twoPointData
    )
    drawRankingPdfLegend(firstCanvas, startX = 60f, y = legendY)

    val tableFitsOnFirstPage = allRows.size <= firstPageRows

    if (tableFitsOnFirstPage) {
        firstCanvas.drawText("Taula global per jugadores", margin, 490f, subtitlePaint)
        drawRankingPdfTableHeader(
            canvas = firstCanvas,
            startX = margin,
            startY = firstPageHeaderY,
            columns = columns,
            rowHeight = rowHeight,
            textSize = 16f
        )

        var currentY = firstPageHeaderY + rowHeight
        allRows.forEach { row ->
            drawTeamExportRow(
                canvas = firstCanvas,
                row = row,
                bestValues = bestValues,
                columns = columns,
                startX = margin,
                startY = currentY,
                rowHeight = rowHeight,
                textSize = 16f
            )
            currentY += rowHeight
        }

        document.finishPage(firstPage)
        return pageNumber + 1
    }

    document.finishPage(firstPage)
    pageNumber++

    allRows.chunked(otherPageRows).forEachIndexed { index, chunk ->
        val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
        val page = document.startPage(pageInfo)
        val canvas = page.canvas

        canvas.drawText("Estadistiques equip", margin, 55f, titlePaint)
        canvas.drawText(
            if (index == 0) "Taula global per jugadores" else "Taula global per jugadores (continuació)",
            margin,
            85f,
            subtitlePaint
        )

        drawRankingPdfTableHeader(
            canvas = canvas,
            startX = margin,
            startY = otherPageHeaderY,
            columns = columns,
            rowHeight = rowHeight,
            textSize = 16f
        )

        var y = otherPageHeaderY + rowHeight
        chunk.forEach { row ->
            drawTeamExportRow(
                canvas = canvas,
                row = row,
                bestValues = bestValues,
                columns = columns,
                startX = margin,
                startY = y,
                rowHeight = rowHeight,
                textSize = 16f
            )
            y += rowHeight
        }

        document.finishPage(page)
        pageNumber++
    }

    return pageNumber
}

private fun drawTeamPlayerPages(
    document: PdfDocument,
    startPageNumber: Int,
    nomJugador: String,
    sessions: List<Sessio>
): Int {
    val orderedSessions = sessions.sortedBy { it.num_sessio }
    val tableRows = orderedSessions.map { it.toTeamPdfPlayerRow() }
    val totalRow = buildTeamPdfPlayerTotalRow(orderedSessions)
    val rowsWithTotal = if (tableRows.isEmpty()) listOf(totalRow) else {
        tableRows + totalRow
    }
    val globalSession = mergeSessionsRanking(
        orderedSessions,
        orderedSessions.firstOrNull()?.id_jugador ?: ""
    )

    val tripleData = orderedSessions.mapIndexed { index, s ->
        TeamPdfProgressPoint(index, s.num_sessio.toString(), s.rankingThreePointPct())
    }
    val freeThrowData = orderedSessions.mapIndexed { index, s ->
        TeamPdfProgressPoint(index, s.num_sessio.toString(), s.rankingFreeThrowPct())
    }
    val twoPointData = orderedSessions.mapIndexed { index, s ->
        TeamPdfProgressPoint(index, s.num_sessio.toString(), s.rankingTwoPointPct())
    }

    val pageWidth = 1650
    val pageHeight = 1000
    val columns = listOf(
        RankingPdfColumn("Sessió", 85f),
        RankingPdfColumn("TL", 85f),
        RankingPdfColumn("TL %", 70f),
        RankingPdfColumn("T2", 85f),
        RankingPdfColumn("T2%", 70f),
        RankingPdfColumn("T3", 85f),
        RankingPdfColumn("T3%", 70f),
        RankingPdfColumn("TOTAL", 95f),
        RankingPdfColumn("TOTAL %", 85f),
        RankingPdfColumn("Dreta %", 85f),
        RankingPdfColumn("Esquerra %", 95f),
        RankingPdfColumn("Millor costat", 110f),
        RankingPdfColumn("Millor zona T2", 130f),
        RankingPdfColumn("Millor zona T3", 130f)
    )

    val maxRowsPerPage = 14
    var pageNumber = startPageNumber

    rowsWithTotal.chunked(maxRowsPerPage).forEachIndexed { pageIndex, chunk ->
        val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
        val page = document.startPage(pageInfo)
        val canvas = page.canvas

        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 28f
            isFakeBoldText = true
            color = android.graphics.Color.BLACK
        }
        val subtitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 18f
            color = android.graphics.Color.DKGRAY
        }

        canvas.drawText("Estadistiques $nomJugador", 45f, 48f, titlePaint)
        canvas.drawText(
            if (pageIndex == 0) "Gràfic, mapa de tir global i taula per sessions"
            else "Gràfic, mapa de tir global i taula per sessions (continuació)",
            45f,
            78f,
            subtitlePaint
        )

        val chartRect = RectF(45f, 110f, 835f, 385f)
        drawRankingPdfProgressChart(
            canvas = canvas,
            area = chartRect,
            title = "Gràfic de sessions",
            tripleData = tripleData,
            freeThrowData = freeThrowData,
            twoPointData = twoPointData
        )
        drawRankingPdfLegend(canvas, startX = 60f, y = 470f)

        val mapHeight = chartRect.height()
        val mapWidth = mapHeight * (453f / 339f)

        canvas.drawText("Mapa de tir global", 1115f, 95f, subtitlePaint)
        drawRankingPdfCourtMap(
            canvas = canvas,
            session = globalSession,
            left = 1115f,
            top = 110f,
            width = mapWidth,
            height = mapHeight
        )

        canvas.drawText("Taula per sessions", 45f, 520f, subtitlePaint)

        val headerY = 550f
        val availableHeight = pageHeight - headerY - 40f
        val rowHeight = ((availableHeight - 34f) / (chunk.size + 1)).coerceIn(20f, 30f)
        val textSize = (rowHeight * 0.42f).coerceIn(9f, 13f)

        drawRankingPdfTableHeader(
            canvas = canvas,
            startX = 45f,
            startY = headerY,
            columns = columns,
            rowHeight = rowHeight,
            textSize = textSize
        )

        var currentY = headerY + rowHeight
        chunk.forEach { row ->
            drawTeamPdfPlayerStatsRow(
                canvas = canvas,
                row = row,
                columns = columns,
                startX = 45f,
                startY = currentY,
                rowHeight = rowHeight,
                textSize = textSize,
                isTotal = row.label == "Total"
            )
            currentY += rowHeight
        }

        document.finishPage(page)
        pageNumber++
    }

    return pageNumber
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

private fun drawRankingPdfProgressChart(
    canvas: android.graphics.Canvas,
    area: RectF,
    title: String,
    tripleData: List<TeamPdfProgressPoint>,
    freeThrowData: List<TeamPdfProgressPoint>,
    twoPointData: List<TeamPdfProgressPoint>
) {
    val sessionCount = maxOf(tripleData.size, freeThrowData.size, twoPointData.size)

    val axisPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        strokeWidth = 2f
        color = android.graphics.Color.BLACK
    }
    val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        strokeWidth = 1f
        color = android.graphics.Color.LTGRAY
    }
    val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = 14f
        color = android.graphics.Color.BLACK
    }
    val smallTitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = 18f
        isFakeBoldText = true
        color = android.graphics.Color.BLACK
    }

    canvas.drawText(title, area.left, area.top - 12f, smallTitlePaint)

    val left = area.left + 48f
    val top = area.top + 8f
    val right = area.right
    val bottom = area.bottom
    val width = right - left
    val height = bottom - top

    val yTicks = listOf(0, 25, 50, 75, 100)
    yTicks.forEach { tick ->
        val y = bottom - (tick / 100f) * height
        canvas.drawLine(left, y, right, y, gridPaint)
        canvas.drawText("$tick%", area.left, y + 4f, textPaint)
    }

    canvas.drawLine(left, top, left, bottom, axisPaint)
    canvas.drawLine(left, bottom, right, bottom, axisPaint)

    fun xFor(index: Int): Float {
        if (sessionCount <= 1) return left + width / 2f
        return left + (index.toFloat() / (sessionCount - 1).toFloat()) * width
    }

    fun yFor(value: Float): Float {
        val clamped = value.coerceIn(0f, 100f)
        return bottom - (clamped / 100f) * height
    }

    fun drawSeries(points: List<TeamPdfProgressPoint>, color: Int) {
        val valid = points.mapNotNull { point ->
            point.value?.let { value -> point.sessionIndex to value }
        }
        if (valid.isEmpty()) return

        val path = AndroidPath()
        valid.forEachIndexed { index, pair ->
            val x = xFor(pair.first)
            val y = yFor(pair.second)
            if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }

        val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 3.5f
            this.color = color
        }
        val pointPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            this.color = color
        }

        canvas.drawPath(path, linePaint)
        valid.forEach { pair ->
            canvas.drawCircle(xFor(pair.first), yFor(pair.second), 3.5f, pointPaint)
        }
    }

    drawSeries(tripleData, android.graphics.Color.parseColor("#1565C0"))
    drawSeries(freeThrowData, android.graphics.Color.parseColor("#D81B60"))
    drawSeries(twoPointData, android.graphics.Color.parseColor("#EF6C00"))

    val xTicks = when {
        sessionCount <= 1 -> listOf(0)
        sessionCount <= 6 -> (0 until sessionCount).toList()
        else -> listOf(0, (sessionCount - 1) / 2, sessionCount - 1).distinct()
    }

    val labelsSource = listOf(tripleData, freeThrowData, twoPointData).firstOrNull { it.isNotEmpty() }.orEmpty()

    xTicks.forEach { tickIndex ->
        val x = xFor(tickIndex)
        canvas.drawLine(x, bottom, x, bottom + 7f, axisPaint)
        val label = labelsSource.getOrNull(tickIndex)?.sessionLabel ?: (tickIndex + 1).toString()
        canvas.drawText(label, x - 8f, bottom + 22f, textPaint)
    }

    canvas.drawText("Sessions", left + width / 4f - 25f, bottom + 44f, textPaint)
}

private fun drawRankingPdfLegend(
    canvas: android.graphics.Canvas,
    startX: Float,
    y: Float
) {
    val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = 16f
        color = android.graphics.Color.BLACK
    }

    fun item(x: Float, label: String, color: String) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            this.color = android.graphics.Color.parseColor(color)
        }
        canvas.drawRoundRect(RectF(x, y - 10f, x + 26f, y), 8f, 8f, paint)
        canvas.drawText(label, x + 36f, y, textPaint)
    }

    item(startX, "Triples", "#1565C0")
    item(startX + 170f, "Tir lliure", "#D81B60")
    item(startX + 370f, "Tirs de 2", "#EF6C00")
}

private fun drawRankingPdfTableHeader(
    canvas: android.graphics.Canvas,
    startX: Float,
    startY: Float,
    columns: List<RankingPdfColumn>,
    rowHeight: Float,
    textSize: Float
) {
    val backgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = android.graphics.Color.parseColor("#F0F0F0")
    }
    val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 1.2f
        color = android.graphics.Color.parseColor("#8A8A8A")
    }
    val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.textSize = textSize
        isFakeBoldText = true
        color = android.graphics.Color.BLACK
    }

    var x = startX
    columns.forEach { column ->
        val rect = RectF(x, startY, x + column.width, startY + rowHeight)
        canvas.drawRect(rect, backgroundPaint)
        canvas.drawRect(rect, borderPaint)
        val textY = startY + rowHeight / 2f - (textPaint.descent() + textPaint.ascent()) / 2f
        canvas.drawText(column.title, x + 4f, textY, textPaint)
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
    rowHeight: Float,
    textSize: Float
) {
    val backgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }
    val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 1.2f
        color = android.graphics.Color.parseColor("#8A8A8A")
    }
    val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.textSize = textSize
        color = android.graphics.Color.BLACK
        isFakeBoldText = row.isTotalRow
    }

    val values = listOf(
        row.label,
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
        val highlight = if (row.isTotalRow) {
            false
        } else {
            when (index) {
                1 -> false
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
        }

        backgroundPaint.color = when {
            row.isTotalRow -> android.graphics.Color.parseColor("#F0F7FF")
            highlight -> android.graphics.Color.parseColor("#DFF3E3")
            else -> android.graphics.Color.WHITE
        }

        val rect = RectF(x, startY, x + columns[index].width, startY + rowHeight)
        canvas.drawRect(rect, backgroundPaint)
        canvas.drawRect(rect, borderPaint)
        val textY = startY + rowHeight / 2f - (textPaint.descent() + textPaint.ascent()) / 2f
        canvas.drawText(value, x + 4f, textY, textPaint)
        x += columns[index].width
    }
}

private fun Sessio.toTeamPdfPlayerRow(): TeamPdfPlayerRow {
    val tlMade = fets_pos_6
    val tlAttempted = tirs_pos_6

    val t2Made = fets_pos_4 + fets_pos_5 + fets_pos_7 + fets_pos_8 + fets_pos_9
    val t2Attempted = tirs_pos_4 + tirs_pos_5 + tirs_pos_7 + tirs_pos_8 + tirs_pos_9

    val t3Made = fets_pos_1 + fets_pos_2 + fets_pos_3 + fets_pos_10 + fets_pos_11
    val t3Attempted = tirs_pos_1 + tirs_pos_2 + tirs_pos_3 + tirs_pos_10 + tirs_pos_11

    val totalMade = tlMade + t2Made + t3Made
    val totalAttempted = tlAttempted + t2Attempted + t3Attempted

    val rightMade = fets_pos_1 + fets_pos_4 + fets_pos_7 + fets_pos_10
    val rightAttempted = tirs_pos_1 + tirs_pos_4 + tirs_pos_7 + tirs_pos_10

    val leftMade = fets_pos_3 + fets_pos_5 + fets_pos_9 + fets_pos_11
    val leftAttempted = tirs_pos_3 + tirs_pos_5 + tirs_pos_9 + tirs_pos_11

    val rightPct = rankingPctOrNull(rightMade, rightAttempted)
    val leftPct = rankingPctOrNull(leftMade, leftAttempted)

    return TeamPdfPlayerRow(
        label = "Sessió $num_sessio",
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
        bestZoneT2 = rankingBestZoneT2Label(this),
        bestZoneT3 = rankingBestZoneT3Label(this)
    )
}

private fun buildTeamPdfPlayerTotalRow(sessions: List<Sessio>): TeamPdfPlayerRow {
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

    val merged = mergeSessionsRanking(
        sessions,
        sessions.firstOrNull()?.id_jugador ?: ""
    )

    val rightPct = rankingPctOrNull(rightMade, rightAttempted)
    val leftPct = rankingPctOrNull(leftMade, leftAttempted)

    return TeamPdfPlayerRow(
        label = "Total",
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

private fun drawTeamPdfPlayerStatsRow(
    canvas: android.graphics.Canvas,
    row: TeamPdfPlayerRow,
    columns: List<RankingPdfColumn>,
    startX: Float,
    startY: Float,
    rowHeight: Float,
    textSize: Float,
    isTotal: Boolean
) {
    val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = if (isTotal) {
            android.graphics.Color.parseColor("#F0F7FF")
        } else {
            android.graphics.Color.WHITE
        }
    }
    val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 1.2f
        color = android.graphics.Color.parseColor("#8A8A8A")
    }
    val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.textSize = textSize
        color = android.graphics.Color.BLACK
        isFakeBoldText = isTotal
    }

    val values = listOf(
        row.label,
        "${row.tlMade}/${row.tlAttempted}",
        rankingFormatPct(row.tlMade, row.tlAttempted),
        "${row.t2Made}/${row.t2Attempted}",
        rankingFormatPct(row.t2Made, row.t2Attempted),
        "${row.t3Made}/${row.t3Attempted}",
        rankingFormatPct(row.t3Made, row.t3Attempted),
        "${row.totalMade}/${row.totalAttempted}",
        rankingFormatPct(row.totalMade, row.totalAttempted),
        row.rightPct.toRankingPdfPercent(),
        row.leftPct.toRankingPdfPercent(),
        row.bestSide,
        row.bestZoneT2,
        row.bestZoneT3
    )

    var x = startX
    values.forEachIndexed { index, value ->
        val rect = RectF(x, startY, x + columns[index].width, startY + rowHeight)
        canvas.drawRect(rect, bgPaint)
        canvas.drawRect(rect, borderPaint)
        val textY = startY + rowHeight / 2f - (textPaint.descent() + textPaint.ascent()) / 2f
        canvas.drawText(value, x + 4f, textY, textPaint)
        x += columns[index].width
    }
}

private fun drawRankingPdfCourtMap(
    canvas: android.graphics.Canvas,
    session: Sessio,
    left: Float,
    top: Float,
    width: Float,
    height: Float
) {
    val aspectRatio = 453f / 339f
    val drawHeight = minOf(height, width / aspectRatio)
    val drawWidth = drawHeight * aspectRatio

    val right = left + drawWidth
    val bottom = top + drawHeight

    val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = android.graphics.Color.WHITE
    }
    val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 3f
        color = android.graphics.Color.BLACK
    }

    fun zonePaint(zone: Int): Paint {
        val (made, attempted) = rankingZoneMadeAttempted(session, zone)
        val percentage = if (attempted > 0) made.toFloat() / attempted.toFloat() else null

        return Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            color = when {
                percentage == null -> android.graphics.Color.WHITE
                percentage < 0.33f -> android.graphics.Color.parseColor("#E53935")
                percentage <= 0.66f -> android.graphics.Color.parseColor("#FDD835")
                else -> android.graphics.Color.parseColor("#43A047")
            }
        }
    }

    fun buildRectPath(l: Float, t: Float, r: Float, b: Float): AndroidPath {
        return AndroidPath().apply {
            addRect(RectF(l, t, r, b), AndroidPath.Direction.CW)
        }
    }

    fun buildQuarterCirclePath(
        centerX: Float,
        centerY: Float,
        radius: Float,
        leftSide: Boolean
    ): AndroidPath {
        val rect = RectF(
            centerX - radius,
            centerY - radius,
            centerX + radius,
            centerY + radius
        )

        return AndroidPath().apply {
            if (leftSide) {
                moveTo(centerX, centerY)
                lineTo(centerX - radius, centerY)
                arcTo(rect, 180f, 90f, false)
                lineTo(centerX, centerY)
                close()
            } else {
                moveTo(centerX, centerY)
                lineTo(centerX, centerY - radius)
                arcTo(rect, 270f, 90f, false)
                lineTo(centerX, centerY)
                close()
            }
        }
    }

    fun buildFreeThrowSemicirclePath(
        centerX: Float,
        centerY: Float,
        radius: Float
    ): AndroidPath {
        val rect = RectF(
            centerX - radius,
            centerY - radius,
            centerX + radius,
            centerY + radius
        )

        return AndroidPath().apply {
            arcTo(rect, 180f, 180f, false)
            lineTo(centerX + radius, centerY)
            lineTo(centerX - radius, centerY)
            close()
        }
    }

    canvas.drawRect(left, top, right, bottom, bgPaint)

    val topSplitY = top + 0.59f * drawHeight
    val lowerSplitY = top + 0.69f * drawHeight

    val third = drawWidth / 3f
    val paintLeft = left + third
    val paintRight = left + 2f * third

    val bigCenterX = left + drawWidth * 0.5f
    val bigCenterY = topSplitY + 0.10f * drawHeight
    val threePointRadius = 0.42f * drawWidth

    val freeThrowCenterX = left + drawWidth * 0.5f
    val freeThrowCenterY = topSplitY
    val freeThrowRadius = (paintRight - paintLeft) / 2f

    val leftArcX = bigCenterX - threePointRadius
    val rightArcX = bigCenterX + threePointRadius

    val dxPaint = paintLeft - bigCenterX
    val arcPaintIntersectionY =
        bigCenterY - sqrt((threePointRadius * threePointRadius) - (dxPaint * dxPaint))

    fun zonePath(zone: Int): AndroidPath {
        return when (zone) {
            1 -> buildRectPath(left, top, left + third, topSplitY)
            2 -> buildRectPath(left + third, top, left + 2f * third, topSplitY)
            3 -> buildRectPath(left + 2f * third, top, right, topSplitY)
            4 -> buildQuarterCirclePath(bigCenterX, bigCenterY, threePointRadius, true)
            5 -> buildQuarterCirclePath(bigCenterX, bigCenterY, threePointRadius, false)
            6 -> buildFreeThrowSemicirclePath(freeThrowCenterX, freeThrowCenterY, freeThrowRadius)
            7 -> buildRectPath(leftArcX, lowerSplitY, paintLeft, bottom)
            8 -> buildRectPath(paintLeft, topSplitY, paintRight, bottom)
            9 -> buildRectPath(paintRight, lowerSplitY, rightArcX, bottom)
            10 -> buildRectPath(left, topSplitY, leftArcX, bottom)
            11 -> buildRectPath(rightArcX, topSplitY, right, bottom)
            else -> buildRectPath(left, top, right, bottom)
        }
    }

    fun drawZoneFill(zone: Int) {
        canvas.drawPath(zonePath(zone), zonePaint(zone))
    }

    drawZoneFill(1)
    drawZoneFill(2)
    drawZoneFill(3)
    drawZoneFill(10)
    drawZoneFill(11)
    drawZoneFill(4)
    drawZoneFill(5)
    drawZoneFill(7)
    drawZoneFill(9)
    drawZoneFill(8)
    drawZoneFill(6)

    canvas.drawRect(left, top, right, bottom, linePaint)

    canvas.drawLine(left + third, top, left + third, arcPaintIntersectionY, linePaint)
    canvas.drawLine(left + 2f * third, top, left + 2f * third, arcPaintIntersectionY, linePaint)

    canvas.drawArc(
        RectF(
            bigCenterX - threePointRadius,
            bigCenterY - threePointRadius,
            bigCenterX + threePointRadius,
            bigCenterY + threePointRadius
        ),
        180f,
        180f,
        false,
        linePaint
    )

    canvas.drawLine(
        bigCenterX,
        bigCenterY - threePointRadius,
        bigCenterX,
        freeThrowCenterY - freeThrowRadius,
        linePaint
    )

    canvas.drawArc(
        RectF(
            freeThrowCenterX - freeThrowRadius,
            freeThrowCenterY - freeThrowRadius,
            freeThrowCenterX + freeThrowRadius,
            freeThrowCenterY + freeThrowRadius
        ),
        180f,
        180f,
        false,
        linePaint
    )

    canvas.drawLine(paintLeft, top, paintLeft, arcPaintIntersectionY, linePaint)
    canvas.drawLine(paintRight, top, paintRight, arcPaintIntersectionY, linePaint)

    canvas.drawLine(paintLeft, topSplitY, paintLeft, bottom, linePaint)
    canvas.drawLine(paintRight, topSplitY, paintRight, bottom, linePaint)

    canvas.drawLine(leftArcX, bigCenterY, leftArcX, bottom, linePaint)
    canvas.drawLine(rightArcX, bigCenterY, rightArcX, bottom, linePaint)

    canvas.drawLine(paintLeft, topSplitY, paintRight, topSplitY, linePaint)
    canvas.drawLine(left, lowerSplitY, paintLeft, lowerSplitY, linePaint)
    canvas.drawLine(paintRight, lowerSplitY, right, lowerSplitY, linePaint)

    val hoopCenterX = left + drawWidth * 0.5f
    val hoopCenterY = top + drawHeight * 0.88f
    val hoopRadius = drawWidth * 0.022f

    canvas.drawCircle(
        hoopCenterX,
        hoopCenterY,
        hoopRadius,
        linePaint
    )
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
        else -> {
            val right = rightPct ?: 0f
            val left = leftPct ?: 0f
            when {
                right > left -> "Dreta"
                left > right -> "Esquerra"
                else -> "Igual"
            }
        }
    }
}

private fun rankingFormatPct(made: Int, attempted: Int): String {
    val pct = rankingPctOrNull(made, attempted) ?: return "-"
    return "${pct.toInt()}%"
}

private fun Sessio.rankingThreePointPct(): Float? {
    val made = fets_pos_1 + fets_pos_2 + fets_pos_3 + fets_pos_10 + fets_pos_11
    val attempted = tirs_pos_1 + tirs_pos_2 + tirs_pos_3 + tirs_pos_10 + tirs_pos_11
    return rankingPctOrNull(made, attempted)
}

private fun Sessio.rankingFreeThrowPct(): Float? {
    return rankingPctOrNull(fets_pos_6, tirs_pos_6)
}

private fun Sessio.rankingTwoPointPct(): Float? {
    val made = fets_pos_4 + fets_pos_5 + fets_pos_7 + fets_pos_8 + fets_pos_9
    val attempted = tirs_pos_4 + tirs_pos_5 + tirs_pos_7 + tirs_pos_8 + tirs_pos_9
    return rankingPctOrNull(made, attempted)
}

fun rankingPctOrNull(made: Int, attempted: Int): Float? {
    if (attempted <= 0) return null
    return (made.toFloat() / attempted.toFloat()) * 100f
}

private fun Float?.toRankingPdfPercent(): String {
    return this?.let { "${it.toInt()}%" } ?: "-"
}

private fun rankingZoneMadeAttempted(session: Sessio, zone: Int): Pair<Int, Int> {
    return when (zone) {
        1 -> session.fets_pos_1 to session.tirs_pos_1
        2 -> session.fets_pos_2 to session.tirs_pos_2
        3 -> session.fets_pos_3 to session.tirs_pos_3
        4 -> session.fets_pos_4 to session.tirs_pos_4
        5 -> session.fets_pos_5 to session.tirs_pos_5
        6 -> session.fets_pos_6 to session.tirs_pos_6
        7 -> session.fets_pos_7 to session.tirs_pos_7
        8 -> session.fets_pos_8 to session.tirs_pos_8
        9 -> session.fets_pos_9 to session.tirs_pos_9
        10 -> session.fets_pos_10 to session.tirs_pos_10
        11 -> session.fets_pos_11 to session.tirs_pos_11
        else -> 0 to 0
    }
}
