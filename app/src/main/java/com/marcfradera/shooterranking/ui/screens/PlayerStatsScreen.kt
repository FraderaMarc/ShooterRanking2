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
import androidx.compose.ui.text.style.TextAlign
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
import com.marcfradera.shooterranking.R
import com.marcfradera.shooterranking.data.FirebaseProvider
import com.marcfradera.shooterranking.data.model.Jugador
import com.marcfradera.shooterranking.data.model.JugadorRankingItem
import com.marcfradera.shooterranking.data.model.Sessio
import com.marcfradera.shooterranking.localization.AppLanguageManager
import com.marcfradera.shooterranking.ui.vm.JugadorSessionsExport
import com.marcfradera.shooterranking.ui.vm.JugadorsViewModel
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.io.File
import java.io.FileOutputStream
import kotlin.math.sqrt

private fun rankingText(resId: Int, vararg args: Any): String =
    AppLanguageManager.text(resId, *args)

private enum class RankingFilter(val labelRes: Int) {
    TOTAL(R.string.filter_total_shots),
    FREE_THROW(R.string.filter_free_throws),
    THREE_PT(R.string.filter_three_pointers),
    TWO_PT(R.string.filter_two_pointers)
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

private data class RankingPdfProZoneLayer(
    val zone: Int,
    val path: AndroidPath
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

    CenteredScaffold(title = rankingText(R.string.ranking), onBack = onBack) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = { showDialog = true },
                modifier = Modifier.weight(1f).height(64.dp)
            ) {
                Text(rankingText(R.string.add_player), textAlign = TextAlign.Center, maxLines = 2)
            }

            Button(
                onClick = {
                    scope.launch {
                        try {
                            val players = vm.loadPlayersForExport(idEquip)
                            val tipusPista = loadTipusPistaByEquip(idEquip)

                            if (players.isEmpty()) {
                                Toast.makeText(
                                    context,
                                    rankingText(R.string.no_players_to_export),
                                    Toast.LENGTH_LONG
                                ).show()
                            } else {
                                exportAllPlayersStatsPdfAndShare(
                                    context = context,
                                    players = players,
                                    tipusPista = tipusPista
                                )
                            }
                        } catch (e: Exception) {
                            Toast.makeText(
                                context,
                                e.message ?: rankingText(R.string.error_generate_pdf),
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                },
                modifier = Modifier.weight(1f).height(64.dp)
            ) {
                Text(rankingText(R.string.export_team_pdf), textAlign = TextAlign.Center, maxLines = 2)
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
                    label = { Text(rankingText(filter.labelRes)) }
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
                        Text(rankingText(R.string.no_players))
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

    Box {
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
                                text = rankingText(R.string.jersey_position_format, formatDorsal(item.jugador.numero_jugador), positionDisplayLabel(positionLabel(item.jugador.posicio_jugador))),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )

                            Spacer(Modifier.height(4.dp))

                            Text(
                                text = rankingText(R.string.sessions_count, item.sessions),
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
                    }

                    Spacer(Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = onStats,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(rankingText(R.string.statistics), textAlign = TextAlign.Center)
                        }

                        OutlinedButton(
                            onClick = onShots,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(rankingText(R.string.map_short), textAlign = TextAlign.Center)
                        }
                    }
                }
            }
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            DropdownMenuItem(
                text = { Text(rankingText(R.string.statistics)) },
                onClick = {
                    expanded = false
                    onStats()
                }
            )
            DropdownMenuItem(
                text = { Text(rankingText(R.string.shot_map)) },
                onClick = {
                    expanded = false
                    onShots()
                }
            )
            DropdownMenuItem(
                text = { Text(rankingText(R.string.edit)) },
                onClick = {
                    expanded = false
                    onEdit()
                }
            )
            DropdownMenuItem(
                text = { Text(rankingText(R.string.delete)) },
                onClick = {
                    expanded = false
                    onDelete()
                }
            )
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
        title = { Text(rankingText(R.string.add_player)) },
        text = {
            Column {
                OutlinedTextField(
                    value = nom,
                    onValueChange = { nom = it },
                    label = { Text(rankingText(R.string.name)) },
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
                    label = { Text(rankingText(R.string.jersey_number_range)) },
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
                Text(rankingText(R.string.create))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(rankingText(R.string.cancel))
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
        title = { Text(rankingText(R.string.edit_player)) },
        text = {
            Column {
                OutlinedTextField(
                    value = nom,
                    onValueChange = { nom = it },
                    label = { Text(rankingText(R.string.name)) },
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
                    label = { Text(rankingText(R.string.jersey_number_range)) },
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
                Text(rankingText(R.string.save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(rankingText(R.string.cancel))
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
            text = rankingText(R.string.position),
            style = MaterialTheme.typography.labelMedium
        )

        Spacer(Modifier.height(4.dp))

        Box(modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(
                onClick = { expanded = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = if (selected.isBlank()) rankingText(R.string.select_position) else positionDisplayLabel(selected),
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
                        text = { Text(positionDisplayLabel(option)) },
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
        title = { Text(rankingText(R.string.delete_player)) },
        text = {
            Text(rankingText(R.string.delete_player_question, nomJugador))
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(rankingText(R.string.delete))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(rankingText(R.string.cancel))
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

private fun positionDisplayLabel(canonical: String): String = when (canonical) {
    "Base" -> rankingText(R.string.position_point_guard)
    "Escolta" -> rankingText(R.string.position_shooting_guard)
    "Aler" -> rankingText(R.string.position_small_forward)
    "Aler-Pivot" -> rankingText(R.string.position_power_forward)
    "Pivot" -> rankingText(R.string.position_center)
    else -> rankingText(R.string.position_unknown)
}
private fun exportAllPlayersStatsPdfAndShare(
    context: Context,
    players: List<JugadorSessionsExport>,
    tipusPista: String
) {
    if (players.isEmpty()) return

    val playerRows = players
        .map { buildTeamExportRow(it, tipusPista) }
        .sortedWith(
            compareByDescending<TeamExportRow> { it.totalPct ?: -1f }
                .thenByDescending { it.totalMade }
                .thenBy { it.jugador.nom_jugador.lowercase() }
        )

    val totalRow = buildTeamTotalRow(players, playerRows, tipusPista)
    val bestValues = buildTeamExportBestValues(playerRows)
    val teamSessions = buildTeamSessionAggregates(players)

    val tripleData = teamSessions.mapIndexed { index, s ->
        TeamPdfProgressPoint(index, s.num_sessio.toString(), s.rankingThreePointPct(tipusPista))
    }
    val freeThrowData = teamSessions.mapIndexed { index, s ->
        TeamPdfProgressPoint(index, s.num_sessio.toString(), s.rankingFreeThrowPct())
    }
    val twoPointData = teamSessions.mapIndexed { index, s ->
        TeamPdfProgressPoint(index, s.num_sessio.toString(), s.rankingTwoPointPct(tipusPista))
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
            sessions = player.sessions.sortedBy { it.num_sessio },
            tipusPista = tipusPista
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

    context.startActivity(Intent.createChooser(intent, rankingText(R.string.share_pdf)))
}

private fun buildTeamExportRow(
    player: JugadorSessionsExport,
    tipusPista: String
): TeamExportRow {
    val sessions = player.sessions.sortedBy { it.num_sessio }

    val tlMade = sessions.sumOf { it.fets_pos_6 }
    val tlAttempted = sessions.sumOf { it.tirs_pos_6 }

    val t2Made = sessions.sumOf { it.rankingTwoPointMade(tipusPista) }
    val t2Attempted = sessions.sumOf { it.rankingTwoPointAttempted(tipusPista) }

    val t3Made = sessions.sumOf { it.rankingThreePointMade(tipusPista) }
    val t3Attempted = sessions.sumOf { it.rankingThreePointAttempted(tipusPista) }

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
        bestZoneT2 = rankingBestZoneT2Label(merged, tipusPista),
        bestZoneT3 = rankingBestZoneT3Label(merged, tipusPista)
    )
}

private fun buildTeamTotalRow(
    players: List<JugadorSessionsExport>,
    rows: List<TeamExportRow>,
    tipusPista: String
): TeamExportRow {
    val allSessions = players.flatMap { it.sessions }.sortedBy { it.num_sessio }
    val merged = mergeSessionsRanking(
        allSessions,
        rows.firstOrNull()?.jugador?.id_jugador ?: ""
    )

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

    val jugador = rows.firstOrNull()?.jugador ?: Jugador(nom_jugador = rankingText(R.string.team_total))

    return TeamExportRow(
        jugador = jugador,
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
        bestZoneT2 = rankingBestZoneT2Label(merged, tipusPista),
        bestZoneT3 = rankingBestZoneT3Label(merged, tipusPista),
        label = rankingText(R.string.team_total),
        isTotalRow = true
    )
}

private fun buildTeamExportBestValues(rows: List<TeamExportRow>): TeamExportBestValues {
    val regularRows = rows.filterNot { it.isTotalRow }

    return TeamExportBestValues(
        sessions = regularRows.maxOfOrNull { it.sessions } ?: 0,
        tlMade = regularRows.maxOfOrNull { it.tlMade } ?: 0,
        tlPct = regularRows.mapNotNull { it.tlPct }.maxOrNull(),
        t2Made = regularRows.maxOfOrNull { it.t2Made } ?: 0,
        t2Pct = regularRows.mapNotNull { it.t2Pct }.maxOrNull(),
        t3Made = regularRows.maxOfOrNull { it.t3Made } ?: 0,
        t3Pct = regularRows.mapNotNull { it.t3Pct }.maxOrNull(),
        totalMade = regularRows.maxOfOrNull { it.totalMade } ?: 0,
        totalPct = regularRows.mapNotNull { it.totalPct }.maxOrNull(),
        rightPct = regularRows.mapNotNull { it.rightPct }.maxOrNull(),
        leftPct = regularRows.mapNotNull { it.leftPct }.maxOrNull()
    )
}

private fun buildTeamSessionAggregates(players: List<JugadorSessionsExport>): List<Sessio> {
    val allSessions = players.flatMap { it.sessions }
    if (allSessions.isEmpty()) return emptyList()

    return allSessions
        .groupBy { it.num_sessio }
        .toSortedMap()
        .map { (sessionNumber, sessions) ->
            mergeSessionsRanking(
                sessions = sessions,
                jugadorId = "team"
            ).copy(num_sessio = sessionNumber)
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
    val rowHeight = 30f

    val columns = listOf(
        RankingPdfColumn(rankingText(R.string.player), 170f),
        RankingPdfColumn(rankingText(R.string.sessions_axis), 85f),
        RankingPdfColumn(rankingText(R.string.free_throw_short), 85f),
        RankingPdfColumn(rankingText(R.string.free_throw_percent_short), 70f),
        RankingPdfColumn(rankingText(R.string.two_point_short), 85f),
        RankingPdfColumn(rankingText(R.string.two_point_percent_short), 70f),
        RankingPdfColumn(rankingText(R.string.three_point_short), 85f),
        RankingPdfColumn(rankingText(R.string.three_point_percent_short), 70f),
        RankingPdfColumn(rankingText(R.string.total_upper), 95f),
        RankingPdfColumn(rankingText(R.string.total_percent), 85f),
        RankingPdfColumn(rankingText(R.string.right_percent), 85f),
        RankingPdfColumn(rankingText(R.string.left_percent), 95f),
        RankingPdfColumn(rankingText(R.string.best_side), 110f),
        RankingPdfColumn(rankingText(R.string.best_zone_2pt), 130f),
        RankingPdfColumn(rankingText(R.string.best_zone_3pt), 130f)
    )

    val allRows = rows + totalRow
    val firstPageRows = 12
    val otherPageRows = 24
    var pageNumber = startPageNumber

    val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
    val firstPage = document.startPage(pageInfo)
    val firstCanvas = firstPage.canvas

    val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = 28f
        isFakeBoldText = true
        color = android.graphics.Color.BLACK
    }
    val subtitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = 18f
        color = android.graphics.Color.DKGRAY
    }

    firstCanvas.drawText(rankingText(R.string.team_statistics), margin, 48f, titlePaint)
    firstCanvas.drawText(rankingText(R.string.team_chart_and_global_table), margin, 78f, subtitlePaint)

    val chartRect = RectF(45f, 120f, 850f, 420f)
    drawRankingPdfProgressChart(
        canvas = firstCanvas,
        area = chartRect,
        title = rankingText(R.string.sessions_chart),
        tripleData = tripleData,
        freeThrowData = freeThrowData,
        twoPointData = twoPointData
    )

    drawRankingPdfLegend(firstCanvas, startX = 60f, y = 505f)

    val firstPageHeaderY = 570f
    val tableFitsOnFirstPage = allRows.size <= firstPageRows

    if (tableFitsOnFirstPage) {
        firstCanvas.drawText(rankingText(R.string.global_players_table), margin, 545f, subtitlePaint)

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
        val pageInfoChunk = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
        val page = document.startPage(pageInfoChunk)
        val canvas = page.canvas

        canvas.drawText(rankingText(R.string.team_statistics), margin, 55f, titlePaint)
        canvas.drawText(
            if (index == 0) rankingText(R.string.global_players_table) else rankingText(R.string.global_players_table_continued),
            margin,
            85f,
            subtitlePaint
        )

        val headerY = 120f

        drawRankingPdfTableHeader(
            canvas = canvas,
            startX = margin,
            startY = headerY,
            columns = columns,
            rowHeight = rowHeight,
            textSize = 16f
        )

        var y = headerY + rowHeight
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
    sessions: List<Sessio>,
    tipusPista: String
): Int {
    val orderedSessions = sessions.sortedBy { it.num_sessio }
    val tableRows = orderedSessions.map { it.toTeamPdfPlayerRow(tipusPista) }
    val totalRow = buildTeamPdfPlayerTotalRow(orderedSessions, tipusPista)
    val rowsWithTotal = if (tableRows.isEmpty()) listOf(totalRow) else tableRows + totalRow

    val globalSession = mergeSessionsRanking(
        orderedSessions,
        orderedSessions.firstOrNull()?.id_jugador ?: ""
    )

    val tripleData = orderedSessions.mapIndexed { index, s ->
        TeamPdfProgressPoint(index, s.num_sessio.toString(), s.rankingThreePointPct(tipusPista))
    }
    val freeThrowData = orderedSessions.mapIndexed { index, s ->
        TeamPdfProgressPoint(index, s.num_sessio.toString(), s.rankingFreeThrowPct())
    }
    val twoPointData = orderedSessions.mapIndexed { index, s ->
        TeamPdfProgressPoint(index, s.num_sessio.toString(), s.rankingTwoPointPct(tipusPista))
    }

    val pageWidth = 1650
    val pageHeight = 1000
    val columns = listOf(
        RankingPdfColumn(rankingText(R.string.session_header), 85f),
        RankingPdfColumn(rankingText(R.string.free_throw_short), 85f),
        RankingPdfColumn(rankingText(R.string.free_throw_percent_short), 70f),
        RankingPdfColumn(rankingText(R.string.two_point_short), 85f),
        RankingPdfColumn(rankingText(R.string.two_point_percent_short), 70f),
        RankingPdfColumn(rankingText(R.string.three_point_short), 85f),
        RankingPdfColumn(rankingText(R.string.three_point_percent_short), 70f),
        RankingPdfColumn(rankingText(R.string.total_upper), 95f),
        RankingPdfColumn(rankingText(R.string.total_percent), 85f),
        RankingPdfColumn(rankingText(R.string.right_percent), 85f),
        RankingPdfColumn(rankingText(R.string.left_percent), 95f),
        RankingPdfColumn(rankingText(R.string.best_side), 110f),
        RankingPdfColumn(rankingText(R.string.best_zone_2pt), 130f),
        RankingPdfColumn(rankingText(R.string.best_zone_3pt), 130f)
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

        canvas.drawText(rankingText(R.string.player_statistics_title, nomJugador), 45f, 48f, titlePaint)
        canvas.drawText(
            if (pageIndex == 0) rankingText(R.string.player_pdf_summary)
            else rankingText(R.string.player_pdf_summary_continued),
            45f,
            78f,
            subtitlePaint
        )

        val chartRect = RectF(45f, 110f, 835f, 385f)
        drawRankingPdfProgressChart(
            canvas = canvas,
            area = chartRect,
            title = rankingText(R.string.sessions_chart),
            tripleData = tripleData,
            freeThrowData = freeThrowData,
            twoPointData = twoPointData
        )

        drawRankingPdfLegend(canvas, startX = 60f, y = 500f)

        val mapHeight = chartRect.height()
        val mapWidth = mapHeight * (453f / 339f)

        canvas.drawText(rankingText(R.string.global_shot_map), 1115f, 95f, subtitlePaint)
        drawRankingPdfCourtMap(
            canvas = canvas,
            session = globalSession,
            left = 1115f,
            top = 110f,
            width = mapWidth,
            height = mapHeight,
            tipusPista = tipusPista
        )

        canvas.drawText(rankingText(R.string.sessions_table), 45f, 540f, subtitlePaint)

        val headerY = 570f
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
                isTotal = row.label == rankingText(R.string.total)
            )
            currentY += rowHeight
        }

        document.finishPage(page)
        pageNumber++
    }

    return pageNumber
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
        sessionCount <= 1 -> listOf(1)
        sessionCount <= 6 -> (1..sessionCount).toList()
        else -> {
            val middle = ((sessionCount - 1) / 2) + 1
            listOf(1, middle, sessionCount).distinct()
        }
    }

    xTicks.forEach { tick ->
        val x = xFor(tick - 1)
        canvas.drawLine(x, bottom, x, bottom + 7f, axisPaint)
        canvas.drawText(tick.toString(), x - 4f, bottom + 22f, textPaint)
    }

    canvas.drawText(rankingText(R.string.sessions_axis), left + width / 2f - 25f, bottom + 44f, textPaint)
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

    item(startX, rankingText(R.string.filter_three_pointers), "#1565C0")
    item(startX + 170f, rankingText(R.string.filter_free_throws), "#D81B60")
    item(startX + 370f, rankingText(R.string.filter_two_pointers), "#EF6C00")
}


private fun drawRankingPdfTextFitted(
    canvas: android.graphics.Canvas,
    text: String,
    rect: RectF,
    paint: Paint,
    horizontalPadding: Float = 4f,
    minTextSize: Float = 8f
) {
    val originalSize = paint.textSize
    val availableWidth = (rect.width() - horizontalPadding * 2f).coerceAtLeast(1f)
    while (paint.measureText(text) > availableWidth && paint.textSize > minTextSize) {
        paint.textSize -= 0.5f
    }
    val textY = rect.centerY() - (paint.descent() + paint.ascent()) / 2f
    canvas.drawText(text, rect.left + horizontalPadding, textY, paint)
    paint.textSize = originalSize
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
        drawRankingPdfTextFitted(canvas, column.title, rect, textPaint)

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
        }

        backgroundPaint.color = when {
            row.isTotalRow -> android.graphics.Color.parseColor("#F0F7FF")
            highlight -> android.graphics.Color.parseColor("#DFF3E3")
            else -> android.graphics.Color.WHITE
        }

        val rect = RectF(x, startY, x + columns[index].width, startY + rowHeight)
        canvas.drawRect(rect, backgroundPaint)
        canvas.drawRect(rect, borderPaint)
        drawRankingPdfTextFitted(canvas, value, rect, textPaint)

        x += columns[index].width
    }
}

private fun Sessio.toTeamPdfPlayerRow(tipusPista: String): TeamPdfPlayerRow {
    val tlMade = fets_pos_6
    val tlAttempted = tirs_pos_6

    val t2Made = rankingTwoPointMade(tipusPista)
    val t2Attempted = rankingTwoPointAttempted(tipusPista)

    val t3Made = rankingThreePointMade(tipusPista)
    val t3Attempted = rankingThreePointAttempted(tipusPista)

    val totalMade = tlMade + t2Made + t3Made
    val totalAttempted = tlAttempted + t2Attempted + t3Attempted

    val rightMade = fets_pos_1 + fets_pos_4 + fets_pos_7 + fets_pos_10
    val rightAttempted = tirs_pos_1 + tirs_pos_4 + tirs_pos_7 + tirs_pos_10

    val leftMade = fets_pos_3 + fets_pos_5 + fets_pos_9 + fets_pos_11
    val leftAttempted = tirs_pos_3 + tirs_pos_5 + tirs_pos_9 + tirs_pos_11

    val rightPct = rankingPctOrNull(rightMade, rightAttempted)
    val leftPct = rankingPctOrNull(leftMade, leftAttempted)

    return TeamPdfPlayerRow(
        label = rankingText(R.string.session_number, num_sessio),
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
        bestZoneT2 = rankingBestZoneT2Label(this, tipusPista),
        bestZoneT3 = rankingBestZoneT3Label(this, tipusPista)
    )
}

private fun buildTeamPdfPlayerTotalRow(
    sessions: List<Sessio>,
    tipusPista: String
): TeamPdfPlayerRow {
    val tlMade = sessions.sumOf { it.fets_pos_6 }
    val tlAttempted = sessions.sumOf { it.tirs_pos_6 }

    val t2Made = sessions.sumOf { it.rankingTwoPointMade(tipusPista) }
    val t2Attempted = sessions.sumOf { it.rankingTwoPointAttempted(tipusPista) }

    val t3Made = sessions.sumOf { it.rankingThreePointMade(tipusPista) }
    val t3Attempted = sessions.sumOf { it.rankingThreePointAttempted(tipusPista) }

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
        label = rankingText(R.string.total),
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
        bestZoneT2 = rankingBestZoneT2Label(merged, tipusPista),
        bestZoneT3 = rankingBestZoneT3Label(merged, tipusPista)
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
        drawRankingPdfTextFitted(canvas, value, rect, textPaint)

        x += columns[index].width
    }
}
private fun drawRankingPdfCourtMap(
    canvas: android.graphics.Canvas,
    session: Sessio,
    left: Float,
    top: Float,
    width: Float,
    height: Float,
    tipusPista: String
) {
    if (tipusPista.equals("Pro", ignoreCase = true)) {
        drawRankingPdfProCourtMap(
            canvas = canvas,
            session = session,
            left = left,
            top = top,
            width = width,
            height = height
        )
        return
    }

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
            1 -> buildRectPath(left, top, left + third, lowerSplitY)
            2 -> buildRectPath(left + third, top, left + 2f * third, topSplitY)
            3 -> buildRectPath(left + 2f * third, top, right, lowerSplitY)

            4 -> buildQuarterCirclePath(bigCenterX, bigCenterY, threePointRadius, true)
            5 -> buildQuarterCirclePath(bigCenterX, bigCenterY, threePointRadius, false)
            6 -> buildFreeThrowSemicirclePath(freeThrowCenterX, freeThrowCenterY, freeThrowRadius)

            7 -> buildRectPath(leftArcX, lowerSplitY, paintLeft, bottom)
            8 -> buildRectPath(paintLeft, topSplitY, paintRight, bottom)
            9 -> buildRectPath(paintRight, lowerSplitY, rightArcX, bottom)

            10 -> buildRectPath(left, lowerSplitY, leftArcX, bottom)
            11 -> buildRectPath(rightArcX, lowerSplitY, right, bottom)

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

private fun drawRankingPdfProCourtMap(
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

    val backgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = android.graphics.Color.WHITE
    }

    val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
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

    canvas.drawRect(left, top, right, bottom, backgroundPaint)

    val layers = rankingPdfProZoneLayers(
        left = left,
        top = top,
        width = drawWidth,
        height = drawHeight
    )

    layers.forEach { layer ->
        canvas.drawPath(layer.path, zonePaint(layer.zone))
    }

    layers.forEach { layer ->
        canvas.drawPath(layer.path, borderPaint)
    }

    drawRankingPdfProCourtOverlay(
        canvas = canvas,
        left = left,
        top = top,
        width = drawWidth,
        height = drawHeight
    )
}

private fun rankingPdfProZoneLayers(
    left: Float,
    top: Float,
    width: Float,
    height: Float
): List<RankingPdfProZoneLayer> {
    val centerX = left + width * 0.5f
    val centerY = top + height * 0.88f

    val innerRadius = width * 0.23f
    val lowRadius = width * 0.36f
    val midRadius = width * 0.52f
    val outerRadius = width * 0.96f

    val cornerWidth = width * 0.12f
    val cornerTop = top + height * 0.48f

    return listOf(
        RankingPdfProZoneLayer(
            zone = 10,
            path = AndroidPath().apply {
                addRect(
                    RectF(
                        left,
                        cornerTop,
                        left + cornerWidth,
                        top + height
                    ),
                    AndroidPath.Direction.CW
                )
            }
        ),
        RankingPdfProZoneLayer(
            zone = 11,
            path = AndroidPath().apply {
                addRect(
                    RectF(
                        left + width - cornerWidth,
                        cornerTop,
                        left + width,
                        top + height
                    ),
                    AndroidPath.Direction.CW
                )
            }
        ),

        RankingPdfProZoneLayer(
            zone = 1,
            path = rankingPdfAnnularSectorPath(
                centerX = centerX,
                centerY = centerY,
                innerRadius = midRadius,
                outerRadius = outerRadius,
                startAngle = 200f,
                sweepAngle = 50f
            )
        ),
        RankingPdfProZoneLayer(
            zone = 2,
            path = rankingPdfAnnularSectorPath(
                centerX = centerX,
                centerY = centerY,
                innerRadius = midRadius,
                outerRadius = outerRadius,
                startAngle = 250f,
                sweepAngle = 40f
            )
        ),
        RankingPdfProZoneLayer(
            zone = 3,
            path = rankingPdfAnnularSectorPath(
                centerX = centerX,
                centerY = centerY,
                innerRadius = midRadius,
                outerRadius = outerRadius,
                startAngle = 290f,
                sweepAngle = 50f
            )
        ),

        RankingPdfProZoneLayer(
            zone = 4,
            path = rankingPdfAnnularSectorPath(
                centerX = centerX,
                centerY = centerY,
                innerRadius = lowRadius,
                outerRadius = midRadius,
                startAngle = 205f,
                sweepAngle = 65f
            )
        ),
        RankingPdfProZoneLayer(
            zone = 5,
            path = rankingPdfAnnularSectorPath(
                centerX = centerX,
                centerY = centerY,
                innerRadius = lowRadius,
                outerRadius = midRadius,
                startAngle = 270f,
                sweepAngle = 65f
            )
        ),

        RankingPdfProZoneLayer(
            zone = 7,
            path = rankingPdfAnnularSectorPath(
                centerX = centerX,
                centerY = centerY,
                innerRadius = innerRadius,
                outerRadius = lowRadius,
                startAngle = 190f,
                sweepAngle = 55f
            )
        ),
        RankingPdfProZoneLayer(
            zone = 6,
            path = rankingPdfAnnularSectorPath(
                centerX = centerX,
                centerY = centerY,
                innerRadius = innerRadius,
                outerRadius = lowRadius,
                startAngle = 245f,
                sweepAngle = 50f
            )
        ),
        RankingPdfProZoneLayer(
            zone = 9,
            path = rankingPdfAnnularSectorPath(
                centerX = centerX,
                centerY = centerY,
                innerRadius = innerRadius,
                outerRadius = lowRadius,
                startAngle = 295f,
                sweepAngle = 55f
            )
        ),

        RankingPdfProZoneLayer(
            zone = 8,
            path = AndroidPath().apply {
                addOval(
                    RectF(
                        centerX - innerRadius,
                        centerY - innerRadius,
                        centerX + innerRadius,
                        centerY + innerRadius
                    ),
                    AndroidPath.Direction.CW
                )
            }
        )
    )
}

private fun rankingPdfAnnularSectorPath(
    centerX: Float,
    centerY: Float,
    innerRadius: Float,
    outerRadius: Float,
    startAngle: Float,
    sweepAngle: Float
): AndroidPath {
    val outerRect = RectF(
        centerX - outerRadius,
        centerY - outerRadius,
        centerX + outerRadius,
        centerY + outerRadius
    )

    val innerRect = RectF(
        centerX - innerRadius,
        centerY - innerRadius,
        centerX + innerRadius,
        centerY + innerRadius
    )

    return AndroidPath().apply {
        arcTo(outerRect, startAngle, sweepAngle, true)
        arcTo(innerRect, startAngle + sweepAngle, -sweepAngle, false)
        close()
    }
}

private fun drawRankingPdfProCourtOverlay(
    canvas: android.graphics.Canvas,
    left: Float,
    top: Float,
    width: Float,
    height: Float
) {
    val courtPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 2.5f
        color = android.graphics.Color.parseColor("#546E7A")
    }

    val hoopCenterX = left + width * 0.5f
    val hoopCenterY = top + height * 0.88f

    val threePointRadius = width * 0.52f
    val paintLeft = left + width * 0.37f
    val paintRight = left + width * 0.63f
    val paintTop = top + height * 0.51f
    val paintBottom = top + height

    val freeThrowCenterX = left + width * 0.5f
    val freeThrowCenterY = paintTop
    val freeThrowRadius = (paintRight - paintLeft) / 2f

    val cornerLineLeftX = left + width * 0.12f
    val cornerLineRightX = left + width * 0.88f
    val cornerLineTop = top + height * 0.48f

    canvas.drawArc(
        RectF(
            hoopCenterX - threePointRadius,
            hoopCenterY - threePointRadius,
            hoopCenterX + threePointRadius,
            hoopCenterY + threePointRadius
        ),
        200f,
        140f,
        false,
        courtPaint
    )

    canvas.drawLine(
        cornerLineLeftX,
        cornerLineTop,
        cornerLineLeftX,
        paintBottom,
        courtPaint
    )

    canvas.drawLine(
        cornerLineRightX,
        cornerLineTop,
        cornerLineRightX,
        paintBottom,
        courtPaint
    )

    canvas.drawLine(paintLeft, paintTop, paintLeft, paintBottom, courtPaint)
    canvas.drawLine(paintRight, paintTop, paintRight, paintBottom, courtPaint)
    canvas.drawLine(paintLeft, paintTop, paintRight, paintTop, courtPaint)

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
        courtPaint
    )

    val boardY = top + height * 0.93f
    val boardHalfWidth = width * 0.06f
    val hoopRadius = width * 0.018f
    val hoopY = boardY - hoopRadius * 1.8f

    canvas.drawLine(
        hoopCenterX - boardHalfWidth,
        boardY,
        hoopCenterX + boardHalfWidth,
        boardY,
        courtPaint
    )

    canvas.drawCircle(
        hoopCenterX,
        hoopY,
        hoopRadius,
        courtPaint
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

private fun rankingBestZoneT2Label(
    s: Sessio,
    tipusPista: String
): String {
    val zones = if (rankingIsBaseCourt(tipusPista)) {
        listOf(
            RankingZoneStat(rankingText(R.string.zone_paint), s.fets_pos_8, s.tirs_pos_8)
        )
    } else {
        listOf(
            RankingZoneStat(rankingText(R.string.zone_high_post_right), s.fets_pos_4, s.tirs_pos_4),
            RankingZoneStat(rankingText(R.string.zone_high_post_left), s.fets_pos_5, s.tirs_pos_5),
            RankingZoneStat(rankingText(R.string.zone_low_post_right), s.fets_pos_7, s.tirs_pos_7),
            RankingZoneStat(rankingText(R.string.zone_paint), s.fets_pos_8, s.tirs_pos_8),
            RankingZoneStat(rankingText(R.string.zone_low_post_left), s.fets_pos_9, s.tirs_pos_9)
        )
    }.filter { it.attempted > 0 }

    if (zones.isEmpty()) return "-"

    return zones.maxWithOrNull(
        compareBy<RankingZoneStat> { it.pct() }.thenBy { it.attempted }
    )?.label ?: "-"
}

private fun rankingBestZoneT3Label(
    s: Sessio,
    tipusPista: String
): String {
    val zones = if (rankingIsBaseCourt(tipusPista)) {
        listOf(
            RankingZoneStat(rankingText(R.string.zone_three_45_right), s.fets_pos_1, s.tirs_pos_1),
            RankingZoneStat(rankingText(R.string.zone_three_middle), s.fets_pos_2, s.tirs_pos_2),
            RankingZoneStat(rankingText(R.string.zone_three_45_left), s.fets_pos_3, s.tirs_pos_3),
            RankingZoneStat(rankingText(R.string.zone_three_high_right), s.fets_pos_4, s.tirs_pos_4),
            RankingZoneStat(rankingText(R.string.zone_three_high_left), s.fets_pos_5, s.tirs_pos_5),
            RankingZoneStat(rankingText(R.string.zone_three_low_right), s.fets_pos_7, s.tirs_pos_7),
            RankingZoneStat(rankingText(R.string.zone_three_low_left), s.fets_pos_9, s.tirs_pos_9),
            RankingZoneStat(rankingText(R.string.zone_three_corner_right), s.fets_pos_10, s.tirs_pos_10),
            RankingZoneStat(rankingText(R.string.zone_three_corner_left), s.fets_pos_11, s.tirs_pos_11)
        )
    } else {
        listOf(
            RankingZoneStat(rankingText(R.string.zone_three_45_right), s.fets_pos_1, s.tirs_pos_1),
            RankingZoneStat(rankingText(R.string.zone_three_middle), s.fets_pos_2, s.tirs_pos_2),
            RankingZoneStat(rankingText(R.string.zone_three_45_left), s.fets_pos_3, s.tirs_pos_3),
            RankingZoneStat(rankingText(R.string.zone_three_corner_right), s.fets_pos_10, s.tirs_pos_10),
            RankingZoneStat(rankingText(R.string.zone_three_corner_left), s.fets_pos_11, s.tirs_pos_11)
        )
    }.filter { it.attempted > 0 }

    if (zones.isEmpty()) return "-"

    return zones.maxWithOrNull(
        compareBy<RankingZoneStat> { it.pct() }.thenBy { it.attempted }
    )?.label ?: "-"
}

private fun rankingBestSideLabel(rightPct: Float?, leftPct: Float?): String {
    return when {
        rightPct == null && leftPct == null -> "-"
        rightPct != null && leftPct == null -> rankingText(R.string.right)
        rightPct == null && leftPct != null -> rankingText(R.string.left)
        else -> {
            val right = rightPct ?: 0f
            val left = leftPct ?: 0f

            when {
                right > left -> rankingText(R.string.right)
                left > right -> rankingText(R.string.left)
                else -> rankingText(R.string.equal)
            }
        }
    }
}

private fun rankingFormatPct(made: Int, attempted: Int): String {
    val pct = rankingPctOrNull(made, attempted) ?: return "-"
    return "${pct.toInt()}%"
}

private fun Float?.toRankingPdfPercent(): String {
    return this?.let { "${it.toInt()}%" } ?: "-"
}

private fun rankingPctOrNull(made: Int, attempted: Int): Float? {
    if (attempted <= 0) return null
    return (made.toFloat() / attempted.toFloat()) * 100f
}

private fun Sessio.rankingThreePointPct(tipusPista: String): Float? {
    return rankingPctOrNull(
        made = rankingThreePointMade(tipusPista),
        attempted = rankingThreePointAttempted(tipusPista)
    )
}

private fun Sessio.rankingFreeThrowPct(): Float? {
    return rankingPctOrNull(fets_pos_6, tirs_pos_6)
}

private fun Sessio.rankingTwoPointPct(tipusPista: String): Float? {
    return rankingPctOrNull(
        made = rankingTwoPointMade(tipusPista),
        attempted = rankingTwoPointAttempted(tipusPista)
    )
}

private fun Sessio.rankingThreePointMade(tipusPista: String): Int {
    return if (rankingIsBaseCourt(tipusPista)) {
        fets_pos_1 + fets_pos_2 + fets_pos_3 +
                fets_pos_4 + fets_pos_5 +
                fets_pos_7 + fets_pos_9 +
                fets_pos_10 + fets_pos_11
    } else {
        fets_pos_1 + fets_pos_2 + fets_pos_3 +
                fets_pos_10 + fets_pos_11
    }
}

private fun Sessio.rankingThreePointAttempted(tipusPista: String): Int {
    return if (rankingIsBaseCourt(tipusPista)) {
        tirs_pos_1 + tirs_pos_2 + tirs_pos_3 +
                tirs_pos_4 + tirs_pos_5 +
                tirs_pos_7 + tirs_pos_9 +
                tirs_pos_10 + tirs_pos_11
    } else {
        tirs_pos_1 + tirs_pos_2 + tirs_pos_3 +
                tirs_pos_10 + tirs_pos_11
    }
}

private fun Sessio.rankingTwoPointMade(tipusPista: String): Int {
    return if (rankingIsBaseCourt(tipusPista)) {
        fets_pos_8
    } else {
        fets_pos_4 + fets_pos_5 + fets_pos_7 + fets_pos_8 + fets_pos_9
    }
}

private fun Sessio.rankingTwoPointAttempted(tipusPista: String): Int {
    return if (rankingIsBaseCourt(tipusPista)) {
        tirs_pos_8
    } else {
        tirs_pos_4 + tirs_pos_5 + tirs_pos_7 + tirs_pos_8 + tirs_pos_9
    }
}

private fun rankingIsBaseCourt(tipusPista: String): Boolean {
    return tipusPista.equals("Base", ignoreCase = true)
}

private suspend fun loadTipusPistaByEquip(idEquip: String): String {
    return try {
        FirebaseProvider.firestore
            .collection("equips")
            .document(idEquip)
            .get()
            .await()
            .getString("tipus_pista")
            ?.takeIf { it.isNotBlank() }
            ?: "Amateur"
    } catch (_: Exception) {
        "Amateur"
    }
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