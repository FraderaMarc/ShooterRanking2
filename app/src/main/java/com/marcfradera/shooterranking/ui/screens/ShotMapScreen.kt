package com.marcfradera.shooterranking.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.marcfradera.shooterranking.data.model.Jugador
import com.marcfradera.shooterranking.data.model.Sessio
import com.marcfradera.shooterranking.ui.vm.JugadorsViewModel
import com.marcfradera.shooterranking.ui.vm.ShotSessionViewModel
import kotlin.math.sqrt

@Composable
fun ShotMapScreen(
    idEquip: String,
    initialJugadorId: String,
    initialJugadorNom: String,
    onBack: () -> Unit,
    onJugadorChanged: (String, String) -> Unit
) {
    val sessionsVm: ShotSessionViewModel = viewModel()
    val playersVm: JugadorsViewModel = viewModel()

    var selectedJugadorId by remember { mutableStateOf(initialJugadorId) }
    var selectedJugadorNom by remember { mutableStateOf(initialJugadorNom) }

    var editingSessionNumber by remember { mutableStateOf<Int?>(null) }
    var zoneDialog by remember { mutableStateOf<Int?>(null) }
    var redrawKey by remember { mutableIntStateOf(0) }
    var sessionsExpanded by remember { mutableStateOf(false) }
    var playersExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(idEquip) {
        playersVm.load(idEquip)
    }

    val players = (playersVm.state.data ?: emptyList()).sortedBy { it.nom_jugador.lowercase() }

    LaunchedEffect(players, initialJugadorId, initialJugadorNom) {
        if (players.isEmpty()) return@LaunchedEffect

        val initialPlayer = players.firstOrNull { it.id_jugador == initialJugadorId } ?: players.first()

        if (selectedJugadorId.isBlank() || players.none { it.id_jugador == selectedJugadorId }) {
            selectedJugadorId = initialPlayer.id_jugador
            selectedJugadorNom = initialPlayer.nom_jugador
            onJugadorChanged(initialPlayer.id_jugador, initialPlayer.nom_jugador)
        }
    }

    LaunchedEffect(selectedJugadorId) {
        if (selectedJugadorId.isBlank()) return@LaunchedEffect
        sessionsVm.load(selectedJugadorId)
        sessionsVm.startNew(selectedJugadorId)
        editingSessionNumber = null
        zoneDialog = null
        sessionsExpanded = false
        redrawKey++
    }

    val savedSessions = sessionsVm.sessions.data.orEmpty().sortedByDescending { it.num_sessio }
    val activeSession = sessionsVm.draft

    CenteredScaffold(
        onBack = onBack,
        titleContent = {
            ShotMapTitle(
                currentPlayerName = selectedJugadorNom.ifBlank { "Jugadora" },
                expanded = playersExpanded,
                onToggleExpanded = { playersExpanded = !playersExpanded },
                onDismissPlayers = { playersExpanded = false },
                players = players,
                onSelectPlayer = { player ->
                    selectedJugadorId = player.id_jugador
                    selectedJugadorNom = player.nom_jugador
                    playersExpanded = false
                    onJugadorChanged(player.id_jugador, player.nom_jugador)
                }
            )
        }
    ) {
        when {
            playersVm.state.loading && players.isEmpty() -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            playersVm.state.error != null && players.isEmpty() -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = playersVm.state.error ?: "Error carregant jugadores",
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }

            players.isEmpty() -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Aquest equip encara no té jugadores")
                }
            }

            else -> {
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(
                        onClick = { sessionsExpanded = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(sessionSelectorLabel(editingSessionNumber, sessionsVm.draft))
                            Text("▼")
                        }
                    }

                    DropdownMenu(
                        expanded = sessionsExpanded,
                        onDismissRequest = { sessionsExpanded = false },
                        modifier = Modifier.fillMaxWidth(0.92f)
                    ) {
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = draftSessionLabel(sessionsVm.draft),
                                    color = MaterialTheme.colorScheme.primary
                                )
                            },
                            onClick = {
                                sessionsVm.startNew(selectedJugadorId)
                                editingSessionNumber = null
                                sessionsExpanded = false
                                redrawKey++
                            }
                        )

                        if (savedSessions.isEmpty()) {
                            DropdownMenuItem(
                                text = { Text("No hi ha sessions guardades") },
                                onClick = { sessionsExpanded = false },
                                enabled = false
                            )
                        } else {
                            savedSessions.forEach { session ->
                                DropdownMenuItem(
                                    text = { Text(savedSessionLabel(session)) },
                                    onClick = {
                                        editingSessionNumber = session.num_sessio
                                        sessionsVm.draft = session.copy()
                                        sessionsExpanded = false
                                        redrawKey++
                                    }
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                key(redrawKey) {
                    CourtMap(
                        session = activeSession,
                        enabled = true,
                        onZoneTap = { zone ->
                            zoneDialog = zone
                        }
                    )
                }

                sessionsVm.error?.let { error ->
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = error,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }

    zoneDialog?.let { zone ->
        val current = sessionsVm.draft
        val (currentMade, currentAttempted) = current.zoneMadeAttempted(zone)

        ZoneInputDialog(
            zone = zone,
            initialMade = currentMade,
            initialAttempted = currentAttempted,
            onDismiss = { zoneDialog = null },
            onSave = { made, attempted ->
                sessionsVm.setZoneForCurrentSession(
                    zone = zone,
                    made = made,
                    attempted = attempted,
                    editing = editingSessionNumber,
                    idJugador = selectedJugadorId
                )
                sessionsVm.saveCurrentSession(
                    idJugador = selectedJugadorId,
                    editing = editingSessionNumber
                ) { savedSessionNumber ->
                    sessionsVm.load(selectedJugadorId)
                    editingSessionNumber = savedSessionNumber
                    sessionsVm.draft = sessionsVm.draft?.copy(num_sessio = savedSessionNumber)
                    redrawKey++
                }
                zoneDialog = null
            }
        )
    }
}

@Composable
private fun ShotMapTitle(
    currentPlayerName: String,
    expanded: Boolean,
    onToggleExpanded: () -> Unit,
    onDismissPlayers: () -> Unit,
    players: List<Jugador>,
    onSelectPlayer: (Jugador) -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "Mapa de tir",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold
        )

        Box {
            TextButton(
                onClick = onToggleExpanded,
                contentPadding = androidx.compose.foundation.layout.PaddingValues(
                    horizontal = 8.dp,
                    vertical = 4.dp
                )
            ) {
                Text(
                    text = currentPlayerName,
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.titleSmall
                )
                Text(
                    text = " ▼",
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.titleSmall
                )
            }

            DropdownMenu(
                expanded = expanded,
                onDismissRequest = onDismissPlayers
            ) {
                players.forEach { player ->
                    DropdownMenuItem(
                        text = { Text(player.nom_jugador) },
                        onClick = { onSelectPlayer(player) }
                    )
                }
            }
        }
    }
}

private fun sessionSelectorLabel(
    editingSessionNumber: Int?,
    draft: Sessio?
): String {
    return if (editingSessionNumber != null) {
        "Sessió $editingSessionNumber"
    } else {
        draftSessionLabel(draft)
    }
}

private fun draftSessionLabel(draft: Sessio?): String {
    return draft?.let { "Nova sessió (${it.num_sessio})" } ?: "Nova sessió"
}

private fun savedSessionLabel(session: Sessio): String {
    return "Sessió ${session.num_sessio}"
}

@Composable
private fun CourtMap(
    session: Sessio?,
    enabled: Boolean,
    onZoneTap: (Int) -> Unit
) {
    val aspectRatio = 453f / 339f

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(aspectRatio)
            .pointerInput(enabled) {
                if (enabled) {
                    detectTapGestures { offset ->
                        val normalizedX = offset.x / size.width
                        val normalizedY = offset.y / size.height
                        detectZone(normalizedX, normalizedY)?.let(onZoneTap)
                    }
                }
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            val lineColor = Color.Black
            val strokeWidth = (w * 0.007f).coerceAtLeast(3f)
            val stroke = Stroke(width = strokeWidth)

            val topSplitY = 0.59f * h
            val lowerSplitY = 0.69f * h

            val third = w / 3f
            val paintLeft = third
            val paintRight = 2f * third

            val bigArcCenter = Offset(w * 0.5f, topSplitY + 0.10f * h)
            val freeThrowCenter = Offset(w * 0.5f, topSplitY)

            val rThree = 0.42f * w
            val rFT = (paintRight - paintLeft) / 2f

            val xLeftArc = w * 0.5f - rThree
            val xRightArc = w * 0.5f + rThree

            val dxPaint = paintLeft - bigArcCenter.x
            val arcPaintIntersectionY =
                bigArcCenter.y - kotlin.math.sqrt((rThree * rThree) - (dxPaint * dxPaint))

            fun drawZoneFill(zone: Int) {
                val (made, attempted) = session.zoneMadeAttempted(zone)
                val percentage = if (attempted > 0) made.toFloat() / attempted.toFloat() else null

                drawPath(
                    path = zonePath(
                        zone = zone,
                        width = w,
                        height = h,
                        topSplitY = topSplitY,
                        lowerSplitY = lowerSplitY,
                        paintLeft = paintLeft,
                        paintRight = paintRight,
                        leftArcX = xLeftArc,
                        rightArcX = xRightArc,
                        bigArcCenter = bigArcCenter,
                        threePointRadius = rThree,
                        freeThrowCenter = freeThrowCenter,
                        freeThrowRadius = rFT
                    ),
                    color = zoneColor(percentage),
                    style = Fill
                )
            }

            drawRect(Color.White)

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

            drawRect(
                color = lineColor,
                topLeft = Offset.Zero,
                size = Size(w, h),
                style = stroke
            )

            drawLine(
                color = lineColor,
                start = Offset(third, 0f),
                end = Offset(third, arcPaintIntersectionY),
                strokeWidth = strokeWidth
            )
            drawLine(
                color = lineColor,
                start = Offset(2f * third, 0f),
                end = Offset(2f * third, arcPaintIntersectionY),
                strokeWidth = strokeWidth
            )

            drawArc(
                color = lineColor,
                startAngle = 180f,
                sweepAngle = 180f,
                useCenter = false,
                topLeft = Offset(
                    bigArcCenter.x - rThree,
                    bigArcCenter.y - rThree
                ),
                size = Size(2f * rThree, 2f * rThree),
                style = stroke
            )

            drawLine(
                color = lineColor,
                start = Offset(w * 0.5f, bigArcCenter.y - rThree),
                end = Offset(w * 0.5f, freeThrowCenter.y - rFT),
                strokeWidth = strokeWidth
            )

            drawArc(
                color = lineColor,
                startAngle = 180f,
                sweepAngle = 180f,
                useCenter = false,
                topLeft = Offset(
                    freeThrowCenter.x - rFT,
                    freeThrowCenter.y - rFT
                ),
                size = Size(2f * rFT, 2f * rFT),
                style = stroke
            )

            drawLine(
                color = lineColor,
                start = Offset(paintLeft, 0f),
                end = Offset(paintLeft, arcPaintIntersectionY),
                strokeWidth = strokeWidth
            )
            drawLine(
                color = lineColor,
                start = Offset(paintRight, 0f),
                end = Offset(paintRight, arcPaintIntersectionY),
                strokeWidth = strokeWidth
            )

            drawLine(
                color = lineColor,
                start = Offset(paintLeft, topSplitY),
                end = Offset(paintLeft, h),
                strokeWidth = strokeWidth
            )
            drawLine(
                color = lineColor,
                start = Offset(paintRight, topSplitY),
                end = Offset(paintRight, h),
                strokeWidth = strokeWidth
            )

            drawLine(
                color = lineColor,
                start = Offset(xLeftArc, bigArcCenter.y),
                end = Offset(xLeftArc, h),
                strokeWidth = strokeWidth
            )
            drawLine(
                color = lineColor,
                start = Offset(xRightArc, bigArcCenter.y),
                end = Offset(xRightArc, h),
                strokeWidth = strokeWidth
            )

            drawLine(
                color = lineColor,
                start = Offset(paintLeft, topSplitY),
                end = Offset(paintRight, topSplitY),
                strokeWidth = strokeWidth
            )

            drawLine(
                color = lineColor,
                start = Offset(0f, lowerSplitY),
                end = Offset(paintLeft, lowerSplitY),
                strokeWidth = strokeWidth
            )
            drawLine(
                color = lineColor,
                start = Offset(paintRight, lowerSplitY),
                end = Offset(w, lowerSplitY),
                strokeWidth = strokeWidth
            )

            val hoopCenterX = w * 0.5f
            val hoopY = h * 0.93f
            val hoopLineWidth = w * 0.12f
            val hoopRadius = w * 0.018f

            drawLine(
                color = lineColor,
                start = Offset(hoopCenterX - hoopLineWidth / 2f, hoopY),
                end = Offset(hoopCenterX + hoopLineWidth / 2f, hoopY),
                strokeWidth = strokeWidth
            )

            drawCircle(
                color = lineColor,
                radius = hoopRadius,
                center = Offset(hoopCenterX, hoopY - hoopRadius * 1.8f),
                style = stroke
            )
        }
    }
}

private fun Sessio?.zoneMadeAttempted(zone: Int): Pair<Int, Int> {
    val session = this ?: return 0 to 0
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

private fun zoneColor(percentage: Float?): Color {
    return when {
        percentage == null -> Color.White
        percentage < 0.33f -> Color(0xFFE53935)
        percentage <= 0.66f -> Color(0xFFFDD835)
        else -> Color(0xFF43A047)
    }
}

private fun zonePath(
    zone: Int,
    width: Float,
    height: Float,
    topSplitY: Float,
    lowerSplitY: Float,
    paintLeft: Float,
    paintRight: Float,
    leftArcX: Float,
    rightArcX: Float,
    bigArcCenter: Offset,
    threePointRadius: Float,
    freeThrowCenter: Offset,
    freeThrowRadius: Float
): Path {
    val thirdWidth = width / 3f

    fun rectPath(left: Float, top: Float, right: Float, bottom: Float): Path =
        Path().apply { addRect(Rect(left, top, right, bottom)) }

    return when (zone) {
        1 -> rectPath(0f, 0f, thirdWidth, topSplitY)
        2 -> rectPath(thirdWidth, 0f, 2f * thirdWidth, topSplitY)
        3 -> rectPath(2f * thirdWidth, 0f, width, topSplitY)
        4 -> quarterCirclePath(bigArcCenter, threePointRadius, true)
        5 -> quarterCirclePath(bigArcCenter, threePointRadius, false)
        6 -> freeThrowSemicirclePath(freeThrowCenter, freeThrowRadius)
        10 -> rectPath(0f, topSplitY, leftArcX, height)
        7 -> rectPath(leftArcX, lowerSplitY, paintLeft, height)
        8 -> rectPath(paintLeft, topSplitY, paintRight, height)
        9 -> rectPath(paintRight, lowerSplitY, rightArcX, height)
        11 -> rectPath(rightArcX, topSplitY, width, height)
        else -> rectPath(0f, 0f, width, height)
    }
}

private fun freeThrowSemicirclePath(
    center: Offset,
    radius: Float
): Path {
    val rect = Rect(
        left = center.x - radius,
        top = center.y - radius,
        right = center.x + radius,
        bottom = center.y + radius
    )

    return Path().apply {
        arcTo(rect, 180f, 180f, false)
        lineTo(center.x + radius, center.y)
        lineTo(center.x - radius, center.y)
        close()
    }
}

private fun quarterCirclePath(
    center: Offset,
    radius: Float,
    left: Boolean
): Path {
    val rect = Rect(
        left = center.x - radius,
        top = center.y - radius,
        right = center.x + radius,
        bottom = center.y + radius
    )

    return Path().apply {
        if (left) {
            moveTo(center.x, center.y)
            lineTo(center.x - radius, center.y)
            arcTo(rect, 180f, 90f, false)
            lineTo(center.x, center.y)
            close()
        } else {
            moveTo(center.x, center.y)
            lineTo(center.x, center.y - radius)
            arcTo(rect, 270f, 90f, false)
            lineTo(center.x, center.y)
            close()
        }
    }
}

private fun detectZone(x: Float, y: Float): Int? {
    val topSplitY = 0.59f
    val lowerSplitY = 0.69f
    val third = 1f / 3f
    val paintLeft = third
    val paintRight = 2f * third

    val bigCenterX = 0.5f
    val bigCenterY = topSplitY + 0.10f
    val threePointRadius = 0.42f

    val freeThrowCenterX = 0.5f
    val freeThrowCenterY = topSplitY
    val freeThrowRadius = (paintRight - paintLeft) / 2f

    val leftArcX = bigCenterX - threePointRadius
    val rightArcX = bigCenterX + threePointRadius

    val dxBig = x - bigCenterX
    val dyBig = y - bigCenterY
    val bigDistance = sqrt(dxBig * dxBig + dyBig * dyBig)

    val dxFree = x - freeThrowCenterX
    val dyFree = y - freeThrowCenterY
    val freeDistance = sqrt(dxFree * dxFree + dyFree * dyFree)

    if (y <= freeThrowCenterY && freeDistance <= freeThrowRadius) return 6

    if (y <= bigCenterY && bigDistance <= threePointRadius) {
        return if (x < bigCenterX) 4 else 5
    }

    if (y < topSplitY) {
        return when {
            x < third -> 1
            x < 2f * third -> 2
            else -> 3
        }
    }

    if (x in paintLeft..paintRight) return 8

    if (x < paintLeft) {
        return if (x < leftArcX) 10 else if (y >= lowerSplitY) 7 else 4
    }

    return if (x < rightArcX) {
        if (y >= lowerSplitY) 9 else 5
    } else {
        11
    }
}

private fun zoneLabel(zone: Int): String {
    return when (zone) {
        1 -> "Triple 45 Dreta"
        2 -> "Triple Mig"
        3 -> "Triple 45 Esquerra"
        4 -> "Poste Alt Dreta"
        5 -> "Poste Alt Esquerra"
        6 -> "Tir Lliure"
        7 -> "Poste Baix Dreta"
        8 -> "Ampolla"
        9 -> "Poste Baix Esquerra"
        10 -> "Triple Cantonada Dreta"
        11 -> "Triple Cantonada Esquerra"
        else -> "Zona $zone"
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ZoneInputDialog(
    zone: Int,
    initialMade: Int,
    initialAttempted: Int,
    onDismiss: () -> Unit,
    onSave: (Int, Int) -> Unit
) {
    var made by remember(zone, initialMade) {
        mutableStateOf(if (initialAttempted == 0 && initialMade == 0) "" else initialMade.toString())
    }
    var attempted by remember(zone, initialAttempted) {
        mutableStateOf(if (initialAttempted == 0 && initialMade == 0) "" else initialAttempted.toString())
    }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(zoneLabel(zone)) },
        text = {
            Column {
                OutlinedTextField(
                    value = made,
                    onValueChange = { input ->
                        if (input.all { it.isDigit() }) {
                            made = input
                            errorMessage = null
                        }
                    },
                    label = { Text("Fets") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = attempted,
                    onValueChange = { input ->
                        if (input.all { it.isDigit() }) {
                            attempted = input
                            errorMessage = null
                        }
                    },
                    label = { Text("Tirats") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )

                errorMessage?.let { message ->
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = message,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val madeValue = made.toIntOrNull() ?: 0
                    val attemptedValue = attempted.toIntOrNull() ?: 0

                    errorMessage = when {
                        madeValue < 0 -> "Els tirs fets no poden ser inferiors a 0"
                        attemptedValue < 0 -> "Els tirs tirats no poden ser inferiors a 0"
                        madeValue > attemptedValue -> "No pots introduir més cistelles que tirs tirats"
                        else -> null
                    }

                    if (errorMessage == null) {
                        onSave(madeValue, attemptedValue)
                    }
                }
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