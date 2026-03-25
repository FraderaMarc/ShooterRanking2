package com.marcfradera.shooterranking.ui.screens

import android.content.Context
import android.content.Intent
import android.graphics.Paint
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
import com.marcfradera.shooterranking.data.model.JugadorRankingItem
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
                vm.update(item.jugador.id_jugador, nom, dorsal, posicio, idEquip, selectedFilter.name) {
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

    val posicioValid = positionLabel(posicio) != "Posició desconeguda"
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

                OutlinedTextField(
                    value = posicio,
                    onValueChange = { posicio = it },
                    label = { Text("Posició") },
                    singleLine = true,
                    supportingText = { Text("Base, Escolta, Aler, Aler-Pivot o Pivot") }
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val d = dorsal.toIntOrNull()
                    if (nom.isNotBlank() && d != null && d in 0..100 && posicioValid) {
                        onCreate(nom.trim(), d, positionLabel(posicio))
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
    var posicio by remember { mutableStateOf(positionLabel(initialPosicio)) }

    val dorsalValue = dorsal.toIntOrNull()
    val dorsalValid =
        dorsal.isNotBlank() &&
                dorsal.all { it.isDigit() } &&
                dorsalValue != null &&
                dorsalValue in 0..100

    val posicioValid = positionLabel(posicio) != "Posició desconeguda"
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

                OutlinedTextField(
                    value = posicio,
                    onValueChange = { posicio = it },
                    label = { Text("Posició") },
                    singleLine = true,
                    supportingText = { Text("Base, Escolta, Aler, Aler-Pivot o Pivot") }
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val d = dorsal.toIntOrNull()
                    if (nom.isNotBlank() && d != null && d in 0..100 && posicioValid) {
                        onSave(nom.trim(), d, positionLabel(posicio))
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
    val document = PdfDocument()
    val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
    val page = document.startPage(pageInfo)
    val canvas = page.canvas

    val titlePaint = Paint().apply {
        textSize = 20f
        isFakeBoldText = true
    }

    val textPaint = Paint().apply {
        textSize = 12f
    }

    var y = 40f
    canvas.drawText("Exportació equip", 40f, y, titlePaint)
    y += 30f

    players.forEach { player ->
        canvas.drawText(player.jugador.nom_jugador, 40f, y, titlePaint)
        y += 22f

        player.sessions.sortedBy { it.num_sessio }.forEach { s ->
            val totalMade =
                s.fets_pos_1 + s.fets_pos_2 + s.fets_pos_3 + s.fets_pos_4 + s.fets_pos_5 +
                        s.fets_pos_6 + s.fets_pos_7 + s.fets_pos_8 + s.fets_pos_9 + s.fets_pos_10 + s.fets_pos_11
            val totalAttempted =
                s.tirs_pos_1 + s.tirs_pos_2 + s.tirs_pos_3 + s.tirs_pos_4 + s.tirs_pos_5 +
                        s.tirs_pos_6 + s.tirs_pos_7 + s.tirs_pos_8 + s.tirs_pos_9 + s.tirs_pos_10 + s.tirs_pos_11
            val pct = if (totalAttempted == 0) 0 else ((totalMade * 100f) / totalAttempted).toInt()

            canvas.drawText(
                "Sessió ${s.num_sessio}: $totalMade/$totalAttempted  $pct%",
                55f,
                y,
                textPaint
            )
            y += 18f

            if (y > 790f) {
                y = 790f
            }
        }

        y += 12f
    }

    document.finishPage(page)

    val file = File(context.cacheDir, "equip_export.pdf")
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