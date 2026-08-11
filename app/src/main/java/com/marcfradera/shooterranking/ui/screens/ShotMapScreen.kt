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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
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
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.marcfradera.shooterranking.R
import com.marcfradera.shooterranking.data.FirebaseProvider
import com.marcfradera.shooterranking.data.model.Jugador
import com.marcfradera.shooterranking.data.model.Sessio
import com.marcfradera.shooterranking.localization.AppLanguageManager
import com.marcfradera.shooterranking.ui.vm.JugadorsViewModel
import com.marcfradera.shooterranking.ui.vm.ShotSessionViewModel
import kotlinx.coroutines.tasks.await
import java.text.DateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.sqrt

private fun shotText(resId: Int, vararg args: Any): String =
    AppLanguageManager.text(resId, *args)

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
    val context = LocalContext.current

    var selectedJugadorId by remember { mutableStateOf(initialJugadorId) }
    var selectedJugadorNom by remember { mutableStateOf(initialJugadorNom) }

    var editingSessionNumber by remember { mutableStateOf<Int?>(null) }
    var zoneDialog by remember { mutableStateOf<Int?>(null) }
    var redrawKey by remember { mutableIntStateOf(0) }
    var sessionsExpanded by remember { mutableStateOf(false) }
    var playersExpanded by remember { mutableStateOf(false) }
    var showDeleteSessionDialog by remember { mutableStateOf(false) }

    // El nombre se edita localmente. No se escribe en el ViewModel/Firestore
    // hasta pulsar explícitamente "Guardar sesión".
    var sessionNameInput by remember { mutableStateOf("") }

    var tipusPista by remember(idEquip) { mutableStateOf("FIBA") }
    var screenActive by remember { mutableStateOf(true) }

    DisposableEffect(Unit) {
        screenActive = true
        onDispose {
            screenActive = false
        }
    }

    LaunchedEffect(idEquip) {
        playersVm.load(idEquip)

        tipusPista = try {
            FirebaseProvider.firestore
                .collection("equips")
                .document(idEquip)
                .get()
                .await()
                .getString("tipus_pista")
                ?.takeIf { it.isNotBlank() }
                ?: "FIBA"
        } catch (_: Exception) {
            "FIBA"
        }
    }

    val players = (playersVm.state.data ?: emptyList())
        .sortedBy { it.nom_jugador.lowercase() }

    LaunchedEffect(players, initialJugadorId, initialJugadorNom) {
        if (players.isEmpty()) return@LaunchedEffect

        val initialPlayer =
            players.firstOrNull { it.id_jugador == initialJugadorId } ?: players.first()

        if (
            selectedJugadorId.isBlank() ||
            players.none { it.id_jugador == selectedJugadorId }
        ) {
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
        showDeleteSessionDialog = false
        redrawKey++
    }

    val savedSessions = sessionsVm.sessions.data
        .orEmpty()
        .sortedWith(
            compareByDescending<Sessio> { it.createdAt }
                .thenByDescending { it.num_sessio }
        )

    val activeSession = sessionsVm.draft
    val isSaving = sessionsVm.isSaving

    /*
     * Cuando cambiamos de sesión o jugador, cargamos su nombre guardado en el editor.
     * No usamos nom_sessio directamente en el TextField para evitar modificar la BD
     * mientras el usuario escribe.
     */
    val activeSessionKey = activeSession?.let { session ->
        "$selectedJugadorId:${session.num_sessio}"
    }

    LaunchedEffect(activeSessionKey) {
        activeSession?.let { session ->
            sessionNameInput = shotSessionDisplayName(session)
        }
    }

    val effectiveSessionName = activeSession?.let { session ->
        sessionNameInput
            .trim()
            .take(25)
            .ifBlank { shotText(R.string.session_number, session.num_sessio) }
    }.orEmpty()

    val normalizedEffectiveSessionName = normalizeSessionName(effectiveSessionName)

    /*
     * Validación instantánea para que no haya dos nombres iguales dentro
     * del mismo jugador. ShooterRepository debe volver a validarlo antes
     * de escribir, de modo que no dependemos solo de la UI.
     */
    val duplicateSessionName =
        activeSession != null &&
            normalizedEffectiveSessionName.isNotBlank() &&
            savedSessions.any { saved ->
                saved.num_sessio != activeSession.num_sessio &&
                    normalizeSessionName(shotSessionDisplayName(saved)) ==
                    normalizedEffectiveSessionName
            }

    val nextAvailableSessionNumber =
        (savedSessions.maxOfOrNull { it.num_sessio } ?: 0) + 1

    val newSessionDisplayNumber =
        if (editingSessionNumber == null) {
            activeSession?.num_sessio ?: nextAvailableSessionNumber
        } else {
            nextAvailableSessionNumber
        }

    fun saveFromMainButton() {
        val session = activeSession ?: return

        if (isSaving || duplicateSessionName) {
            return
        }

        val safeName = effectiveSessionName
            .ifBlank {
                shotText(R.string.session_number, session.num_sessio)
            }

        // Primero actualizamos el borrador y acto seguido hacemos UN único guardado.
        sessionNameInput = safeName
        sessionsVm.setSessionName(safeName)

        sessionsVm.saveCurrentSession(
            idJugador = selectedJugadorId,
            editing = editingSessionNumber
        ) { savedSessionNumber ->
            if (screenActive) {
                editingSessionNumber = savedSessionNumber
                sessionsExpanded = false

                // El ViewModel deja draft con el documento devuelto por Firestore,
                // incluyendo createdAt/updatedAt reales.
                sessionsVm.draft?.let { saved ->
                    sessionNameInput = shotSessionDisplayName(saved)
                }

                redrawKey++
            }
        }
    }

    CenteredScaffold(
        onBack = onBack,
        titleContent = {
            ShotMapTitle(
                currentPlayerName = selectedJugadorNom.ifBlank {
                    shotText(R.string.player_fallback)
                },
                expanded = playersExpanded,
                onToggleExpanded = {
                    if (!isSaving) {
                        playersExpanded = !playersExpanded
                    }
                },
                onDismissPlayers = {
                    playersExpanded = false
                },
                players = players,
                onSelectPlayer = { player ->
                    if (!isSaving) {
                        selectedJugadorId = player.id_jugador
                        selectedJugadorNom = player.nom_jugador
                        playersExpanded = false
                        onJugadorChanged(
                            player.id_jugador,
                            player.nom_jugador
                        )
                    }
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
                        text = playersVm.state.error
                            ?: shotText(R.string.error_loading_players),
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
                    Text(shotText(R.string.no_players_in_team))
                }
            }

            else -> {
                /*
                 * IMPORTANTE:
                 * esta columna tiene weight(1f) + verticalScroll.
                 * Así ocupa el espacio visible disponible del Scaffold y todo
                 * lo situado debajo del botón Guardar (nombre y fechas) se puede ver.
                 */
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(bottom = 32.dp)
                ) {
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedButton(
                            onClick = {
                                if (!isSaving) {
                                    sessionsExpanded = true
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp),
                            enabled = !isSaving
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    activeSession?.let(::shotSessionDisplayName)
                                        ?: shotText(
                                            R.string.new_session,
                                            newSessionDisplayNumber
                                        )
                                )
                                Text("▼")
                            }
                        }

                        /*
                         * DropdownMenu de Material3 ya es desplazable.
                         * Limitamos su alto para que con muchas sesiones se pueda
                         * hacer scroll sin ocupar toda la pantalla.
                         */
                        DropdownMenu(
                            expanded = sessionsExpanded,
                            onDismissRequest = {
                                sessionsExpanded = false
                            },
                            modifier = Modifier
                                .widthIn(min = 260.dp)
                                .heightIn(max = 420.dp)
                        ) {
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = shotText(
                                            R.string.new_session,
                                            newSessionDisplayNumber
                                        ),
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                },
                                enabled = !isSaving,
                                onClick = {
                                    if (!isSaving) {
                                        sessionsVm.startNew(selectedJugadorId)
                                        editingSessionNumber = null
                                        sessionsExpanded = false
                                        redrawKey++
                                    }
                                }
                            )

                            if (savedSessions.isEmpty()) {
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            shotText(
                                                R.string.no_saved_sessions
                                            )
                                        )
                                    },
                                    onClick = {
                                        sessionsExpanded = false
                                    },
                                    enabled = false
                                )
                            } else {
                                savedSessions.forEach { session ->
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                shotSessionDisplayName(session)
                                            )
                                        },
                                        enabled = !isSaving,
                                        onClick = {
                                            if (!isSaving) {
                                                editingSessionNumber =
                                                    session.num_sessio
                                                sessionsVm.draft =
                                                    session.copy()
                                                sessionsExpanded = false
                                                redrawKey++
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }

                    if (editingSessionNumber != null) {
                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedButton(
                            onClick = {
                                if (!isSaving) {
                                    showDeleteSessionDialog = true
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !isSaving
                        ) {
                            Text(shotText(R.string.delete_session))
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    key(redrawKey, tipusPista) {
                        CourtMap(
                            session = activeSession,
                            enabled = !isSaving,
                            tipusPista = tipusPista,
                            onZoneTap = { zone ->
                                if (!isSaving) {
                                    zoneDialog = zone
                                }
                            }
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    /*
                     * ÚNICO botón que escribe los cambios de la sesión.
                     * Mientras Firestore está guardando se bloquea la pantalla
                     * para impedir dobles envíos.
                     */
                    Button(
                        onClick = {
                            saveFromMainButton()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        enabled =
                            activeSession != null &&
                                !isSaving &&
                                !duplicateSessionName,
                        colors = ButtonDefaults.buttonColors(
                            containerColor =
                                MaterialTheme.colorScheme.primary,
                            contentColor =
                                MaterialTheme.colorScheme.onPrimary
                        )
                    ) {
                        if (isSaving) {
                            Row(
                                verticalAlignment =
                                    Alignment.CenterVertically,
                                horizontalArrangement =
                                    Arrangement.spacedBy(8.dp)
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp,
                                    color =
                                        MaterialTheme.colorScheme.onPrimary
                                )

                                Text(
                                    shotText(
                                        R.string.saving_session
                                    )
                                )
                            }
                        } else {
                            Text(
                                shotText(
                                    R.string.save_session
                                )
                            )
                        }
                    }

                    /*
                     * RECUADRO VISIBLE JUSTO DEBAJO DE "GUARDAR SESIÓN".
                     */
                    Spacer(modifier = Modifier.height(12.dp))

                    activeSession?.let { session ->
                        Card(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp)
                            ) {
                                Text(
                                    text =
                                        shotText(R.string.session_info),
                                    style =
                                        MaterialTheme.typography.titleMedium,
                                    fontWeight =
                                        FontWeight.SemiBold
                                )

                                Spacer(
                                    modifier =
                                        Modifier.height(10.dp)
                                )

                                OutlinedTextField(
                                    value = sessionNameInput,
                                    onValueChange = { value ->
                                        if (
                                            !isSaving &&
                                            value.length <= 25
                                        ) {
                                            sessionNameInput = value
                                        }
                                    },
                                    modifier =
                                        Modifier.fillMaxWidth(),
                                    label = {
                                        Text(
                                            shotText(
                                                R.string.session_name_label
                                            )
                                        )
                                    },
                                    supportingText = {
                                        if (duplicateSessionName) {
                                            Text(
                                                text = shotText(
                                                    R.string
                                                        .error_session_name_duplicate
                                                ),
                                                color =
                                                    MaterialTheme
                                                        .colorScheme
                                                        .error
                                            )
                                        } else {
                                            Text(
                                                "${sessionNameInput.length.coerceAtMost(25)}/25"
                                            )
                                        }
                                    },
                                    singleLine = true,
                                    isError =
                                        duplicateSessionName,
                                    enabled = !isSaving
                                )

                                Spacer(
                                    modifier =
                                        Modifier.height(12.dp)
                                )

                                Text(
                                    shotText(
                                        R.string.created_at_format,
                                        shotSessionTimestampText(
                                            context = context,
                                            millis =
                                                session.createdAt,
                                            pending =
                                                session.id_sessio
                                                    .isBlank()
                                        )
                                    )
                                )

                                Spacer(
                                    modifier =
                                        Modifier.height(6.dp)
                                )

                                Text(
                                    shotText(
                                        R.string.updated_at_format,
                                        shotSessionTimestampText(
                                            context = context,
                                            millis =
                                                session.updatedAt,
                                            pending =
                                                session.id_sessio
                                                    .isBlank()
                                        )
                                    )
                                )
                            }
                        }
                    }

                    sessionsVm.error?.let { error ->
                        Spacer(
                            modifier =
                                Modifier.height(12.dp)
                        )

                        Text(
                            text = error,
                            color =
                                MaterialTheme.colorScheme.error
                        )
                    }

                    Spacer(
                        modifier =
                            Modifier.height(16.dp)
                    )
                }
            }
        }
    }

    /*
     * Modificar una zona SOLO cambia el borrador local.
     * Ya no se llama a saveCurrentSession() desde este diálogo.
     * La escritura se hace únicamente con el botón principal.
     */
    zoneDialog?.let { zone ->
        val current = sessionsVm.draft
        val (currentMade, currentAttempted) =
            current.zoneMadeAttempted(zone)

        ZoneInputDialog(
            zone = zone,
            tipusPista = tipusPista,
            initialMade = currentMade,
            initialAttempted = currentAttempted,
            onDismiss = {
                zoneDialog = null
            },
            onSave = { made, attempted ->
                sessionsVm.setZoneForCurrentSession(
                    zone = zone,
                    made = made,
                    attempted = attempted,
                    editing = editingSessionNumber,
                    idJugador = selectedJugadorId
                )

                zoneDialog = null
                redrawKey++
            }
        )
    }

    val deletingSessionNumber = editingSessionNumber

    if (
        showDeleteSessionDialog &&
        deletingSessionNumber != null
    ) {
        AlertDialog(
            onDismissRequest = {
                if (!isSaving) {
                    showDeleteSessionDialog = false
                }
            },
            title = {
                Text(
                    shotText(
                        R.string.delete_session
                    )
                )
            },
            text = {
                Text(
                    shotText(
                        R.string.delete_named_session_question,
                        activeSession?.let(
                            ::shotSessionDisplayName
                        ) ?: shotText(
                            R.string.session_number,
                            deletingSessionNumber
                        )
                    )
                )
            },
            confirmButton = {
                TextButton(
                    enabled = !isSaving,
                    onClick = {
                        sessionsVm.deleteSession(
                            idJugador =
                                selectedJugadorId,
                            numSessio =
                                deletingSessionNumber
                        ) {
                            showDeleteSessionDialog =
                                false
                            editingSessionNumber =
                                null

                            sessionsVm.load(
                                selectedJugadorId
                            )
                            sessionsVm.startNew(
                                selectedJugadorId
                            )

                            redrawKey++
                        }
                    }
                ) {
                    Text(
                        shotText(
                            R.string.delete
                        )
                    )
                }
            },
            dismissButton = {
                TextButton(
                    enabled = !isSaving,
                    onClick = {
                        showDeleteSessionDialog =
                            false
                    }
                ) {
                    Text(
                        shotText(
                            R.string.cancel
                        )
                    )
                }
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
            text = shotText(R.string.shot_map),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold
        )

        Box {
            OutlinedButton(
                onClick = onToggleExpanded,
                modifier = Modifier
                    .widthIn(min = 190.dp)
                    .height(42.dp)
            ) {
                Text(
                    text = currentPlayerName,
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.weight(1f)
                )

                Text(
                    text = "▼",
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.titleSmall
                )
            }

            DropdownMenu(
                expanded = expanded,
                onDismissRequest = onDismissPlayers,
                modifier = Modifier.widthIn(min = 240.dp)
            ) {
                players.forEach { player ->
                    DropdownMenuItem(
                        text = {
                            Text(player.nom_jugador)
                        },
                        onClick = {
                            onSelectPlayer(player)
                        }
                    )
                }
            }
        }
    }
}

private fun shotSessionDisplayName(
    session: Sessio
): String =
    session.nom_sessio
        .trim()
        .takeIf { it.isNotBlank() }
        ?: shotText(
            R.string.session_number,
            session.num_sessio
        )

private fun normalizeSessionName(
    value: String
): String =
    value
        .trim()
        .replace(Regex("\\s+"), " ")
        .lowercase(Locale.ROOT)

private fun shotSessionTimestampText(
    context: android.content.Context,
    millis: Long,
    pending: Boolean
): String {
    if (millis <= 0L) {
        return if (pending) {
            shotText(
                R.string.date_pending_save
            )
        } else {
            shotText(
                R.string.date_not_available
            )
        }
    }

    val locale =
        Locale.forLanguageTag(
            AppLanguageManager
                .currentLanguageTag(context)
        )

    return DateFormat
        .getDateTimeInstance(
            DateFormat.MEDIUM,
            DateFormat.SHORT,
            locale
        )
        .format(Date(millis))
}

@Composable
private fun CourtMap(
    session: Sessio?,
    enabled: Boolean,
    tipusPista: String,
    onZoneTap: (Int) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(453f / 339f)
            .pointerInput(
                enabled,
                tipusPista
            ) {
                if (enabled) {
                    detectTapGestures { offset ->
                        val normalizedX =
                            offset.x / size.width

                        val normalizedY =
                            offset.y / size.height

                        detectZone(
                            normalizedX,
                            normalizedY
                        )?.let(
                            onZoneTap
                        )
                    }
                }
            }
    ) {
        Canvas(
            modifier = Modifier.fillMaxSize()
        ) {
            drawNormalCourtMap(
                session = session,
                width = size.width,
                height = size.height,
                strokeWidth =
                    (size.width * 0.007f)
                        .coerceAtLeast(3f)
            )
        }
    }
}

private fun DrawScope.drawNormalCourtMap(
    session: Sessio?,
    width: Float,
    height: Float,
    strokeWidth: Float
) {
    val w = width
    val h = height

    val lineColor = Color.Black
    val stroke = Stroke(width = strokeWidth)

    val topSplitY = 0.59f * h
    val lowerSplitY = 0.69f * h

    val third = w / 3f
    val paintLeft = third
    val paintRight = 2f * third

    val bigArcCenter =
        Offset(
            w * 0.5f,
            topSplitY + 0.10f * h
        )

    val freeThrowCenter =
        Offset(
            w * 0.5f,
            topSplitY
        )

    val rThree = 0.42f * w
    val rFT =
        (paintRight - paintLeft) / 2f

    val xLeftArc =
        w * 0.5f - rThree

    val xRightArc =
        w * 0.5f + rThree

    val dxPaint =
        paintLeft - bigArcCenter.x

    val arcPaintIntersectionY =
        bigArcCenter.y -
            sqrt(
                (rThree * rThree) -
                    (dxPaint * dxPaint)
            )

    fun drawZoneFill(zone: Int) {
        val (made, attempted) =
            session.zoneMadeAttempted(zone)

        val percentage =
            if (attempted > 0) {
                made.toFloat() /
                    attempted.toFloat()
            } else {
                null
            }

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
                bigArcCenter =
                    bigArcCenter,
                threePointRadius =
                    rThree,
                freeThrowCenter =
                    freeThrowCenter,
                freeThrowRadius =
                    rFT
            ),
            color =
                zoneColor(percentage),
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
        start = Offset(
            third,
            0f
        ),
        end = Offset(
            third,
            arcPaintIntersectionY
        ),
        strokeWidth =
            strokeWidth
    )

    drawLine(
        color = lineColor,
        start = Offset(
            2f * third,
            0f
        ),
        end = Offset(
            2f * third,
            arcPaintIntersectionY
        ),
        strokeWidth =
            strokeWidth
    )

    drawArc(
        color = lineColor,
        startAngle = 180f,
        sweepAngle = 180f,
        useCenter = false,
        topLeft = Offset(
            bigArcCenter.x -
                rThree,
            bigArcCenter.y -
                rThree
        ),
        size = Size(
            2f * rThree,
            2f * rThree
        ),
        style = stroke
    )

    drawLine(
        color = lineColor,
        start = Offset(
            w * 0.5f,
            bigArcCenter.y -
                rThree
        ),
        end = Offset(
            w * 0.5f,
            freeThrowCenter.y -
                rFT
        ),
        strokeWidth =
            strokeWidth
    )

    drawArc(
        color = lineColor,
        startAngle = 180f,
        sweepAngle = 180f,
        useCenter = false,
        topLeft = Offset(
            freeThrowCenter.x -
                rFT,
            freeThrowCenter.y -
                rFT
        ),
        size = Size(
            2f * rFT,
            2f * rFT
        ),
        style = stroke
    )

    drawLine(
        color = lineColor,
        start = Offset(
            paintLeft,
            0f
        ),
        end = Offset(
            paintLeft,
            arcPaintIntersectionY
        ),
        strokeWidth =
            strokeWidth
    )

    drawLine(
        color = lineColor,
        start = Offset(
            paintRight,
            0f
        ),
        end = Offset(
            paintRight,
            arcPaintIntersectionY
        ),
        strokeWidth =
            strokeWidth
    )

    drawLine(
        color = lineColor,
        start = Offset(
            paintLeft,
            topSplitY
        ),
        end = Offset(
            paintLeft,
            h
        ),
        strokeWidth =
            strokeWidth
    )

    drawLine(
        color = lineColor,
        start = Offset(
            paintRight,
            topSplitY
        ),
        end = Offset(
            paintRight,
            h
        ),
        strokeWidth =
            strokeWidth
    )

    drawLine(
        color = lineColor,
        start = Offset(
            xLeftArc,
            bigArcCenter.y
        ),
        end = Offset(
            xLeftArc,
            h
        ),
        strokeWidth =
            strokeWidth
    )

    drawLine(
        color = lineColor,
        start = Offset(
            xRightArc,
            bigArcCenter.y
        ),
        end = Offset(
            xRightArc,
            h
        ),
        strokeWidth =
            strokeWidth
    )

    drawLine(
        color = lineColor,
        start = Offset(
            paintLeft,
            topSplitY
        ),
        end = Offset(
            paintRight,
            topSplitY
        ),
        strokeWidth =
            strokeWidth
    )

    drawLine(
        color = lineColor,
        start = Offset(
            0f,
            lowerSplitY
        ),
        end = Offset(
            paintLeft,
            lowerSplitY
        ),
        strokeWidth =
            strokeWidth
    )

    drawLine(
        color = lineColor,
        start = Offset(
            paintRight,
            lowerSplitY
        ),
        end = Offset(
            w,
            lowerSplitY
        ),
        strokeWidth =
            strokeWidth
    )

    val hoopCenterX =
        w * 0.5f

    val hoopY =
        h * 0.93f

    val hoopLineWidth =
        w * 0.12f

    val hoopRadius =
        w * 0.018f

    drawLine(
        color = lineColor,
        start = Offset(
            hoopCenterX -
                hoopLineWidth / 2f,
            hoopY
        ),
        end = Offset(
            hoopCenterX +
                hoopLineWidth / 2f,
            hoopY
        ),
        strokeWidth =
            strokeWidth
    )

    drawCircle(
        color = lineColor,
        radius = hoopRadius,
        center = Offset(
            hoopCenterX,
            hoopY -
                hoopRadius * 1.8f
        ),
        style = stroke
    )
}

private fun Sessio?.zoneMadeAttempted(
    zone: Int
): Pair<Int, Int> {
    val session =
        this ?: return 0 to 0

    return when (zone) {
        1 ->
            session.fets_pos_1 to
                session.tirs_pos_1

        2 ->
            session.fets_pos_2 to
                session.tirs_pos_2

        3 ->
            session.fets_pos_3 to
                session.tirs_pos_3

        4 ->
            session.fets_pos_4 to
                session.tirs_pos_4

        5 ->
            session.fets_pos_5 to
                session.tirs_pos_5

        6 ->
            session.fets_pos_6 to
                session.tirs_pos_6

        7 ->
            session.fets_pos_7 to
                session.tirs_pos_7

        8 ->
            session.fets_pos_8 to
                session.tirs_pos_8

        9 ->
            session.fets_pos_9 to
                session.tirs_pos_9

        10 ->
            session.fets_pos_10 to
                session.tirs_pos_10

        11 ->
            session.fets_pos_11 to
                session.tirs_pos_11

        else ->
            0 to 0
    }
}

private fun zoneColor(
    percentage: Float?
): Color {
    return when {
        percentage == null ->
            Color.White

        percentage < 0.33f ->
            Color(0xFFE53935)

        percentage <= 0.66f ->
            Color(0xFFFDD835)

        else ->
            Color(0xFF43A047)
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
    val thirdWidth =
        width / 3f

    fun rectPath(
        left: Float,
        top: Float,
        right: Float,
        bottom: Float
    ): Path =
        Path().apply {
            addRect(
                Rect(
                    left,
                    top,
                    right,
                    bottom
                )
            )
        }

    return when (zone) {
        1 ->
            rectPath(
                0f,
                0f,
                thirdWidth,
                lowerSplitY
            )

        2 ->
            rectPath(
                thirdWidth,
                0f,
                2f * thirdWidth,
                topSplitY
            )

        3 ->
            rectPath(
                2f * thirdWidth,
                0f,
                width,
                lowerSplitY
            )

        4 ->
            quarterCirclePath(
                bigArcCenter,
                threePointRadius,
                true
            )

        5 ->
            quarterCirclePath(
                bigArcCenter,
                threePointRadius,
                false
            )

        6 ->
            freeThrowSemicirclePath(
                freeThrowCenter,
                freeThrowRadius
            )

        8 ->
            rectPath(
                paintLeft,
                topSplitY,
                paintRight,
                height
            )

        7 ->
            rectPath(
                leftArcX,
                lowerSplitY,
                paintLeft,
                height
            )

        9 ->
            rectPath(
                paintRight,
                lowerSplitY,
                rightArcX,
                height
            )

        10 ->
            rectPath(
                0f,
                lowerSplitY,
                leftArcX,
                height
            )

        11 ->
            rectPath(
                rightArcX,
                lowerSplitY,
                width,
                height
            )

        else ->
            rectPath(
                0f,
                0f,
                width,
                height
            )
    }
}

private fun freeThrowSemicirclePath(
    center: Offset,
    radius: Float
): Path {
    val rect =
        Rect(
            left =
                center.x - radius,
            top =
                center.y - radius,
            right =
                center.x + radius,
            bottom =
                center.y + radius
        )

    return Path().apply {
        arcTo(
            rect,
            180f,
            180f,
            false
        )
        lineTo(
            center.x + radius,
            center.y
        )
        lineTo(
            center.x - radius,
            center.y
        )
        close()
    }
}

private fun quarterCirclePath(
    center: Offset,
    radius: Float,
    left: Boolean
): Path {
    val rect =
        Rect(
            left =
                center.x - radius,
            top =
                center.y - radius,
            right =
                center.x + radius,
            bottom =
                center.y + radius
        )

    return Path().apply {
        if (left) {
            moveTo(
                center.x,
                center.y
            )
            lineTo(
                center.x - radius,
                center.y
            )
            arcTo(
                rect,
                180f,
                90f,
                false
            )
            lineTo(
                center.x,
                center.y
            )
            close()
        } else {
            moveTo(
                center.x,
                center.y
            )
            lineTo(
                center.x,
                center.y - radius
            )
            arcTo(
                rect,
                270f,
                90f,
                false
            )
            lineTo(
                center.x,
                center.y
            )
            close()
        }
    }
}

private fun detectZone(
    x: Float,
    y: Float
): Int? {
    val topSplitY =
        0.59f

    val lowerSplitY =
        0.69f

    val third =
        1f / 3f

    val paintLeft =
        third

    val paintRight =
        2f * third

    val bigCenterX =
        0.5f

    val bigCenterY =
        topSplitY + 0.10f

    val threePointRadius =
        0.42f

    val freeThrowCenterX =
        0.5f

    val freeThrowCenterY =
        topSplitY

    val freeThrowRadius =
        (paintRight -
            paintLeft) / 2f

    val leftArcX =
        bigCenterX -
            threePointRadius

    val rightArcX =
        bigCenterX +
            threePointRadius

    val dxBig =
        x - bigCenterX

    val dyBig =
        y - bigCenterY

    val bigDistance =
        sqrt(
            dxBig * dxBig +
                dyBig * dyBig
        )

    val dxFree =
        x - freeThrowCenterX

    val dyFree =
        y - freeThrowCenterY

    val freeDistance =
        sqrt(
            dxFree * dxFree +
                dyFree * dyFree
        )

    if (
        y <= freeThrowCenterY &&
        freeDistance <= freeThrowRadius
    ) {
        return 6
    }

    if (
        y <= bigCenterY &&
        bigDistance <= threePointRadius
    ) {
        return if (x < bigCenterX) {
            4
        } else {
            5
        }
    }

    if (y < topSplitY) {
        return when {
            x < third ->
                1

            x < 2f * third ->
                2

            else ->
                3
        }
    }

    if (
        x in paintLeft..paintRight
    ) {
        return 8
    }

    if (x < paintLeft) {
        return when {
            x < leftArcX &&
                y < lowerSplitY ->
                1

            x < leftArcX ->
                10

            y >= lowerSplitY ->
                7

            else ->
                4
        }
    }

    return if (
        x < rightArcX
    ) {
        if (
            y >= lowerSplitY
        ) {
            9
        } else {
            5
        }
    } else {
        if (
            y < lowerSplitY
        ) {
            3
        } else {
            11
        }
    }
}

private fun zoneLabel(
    zone: Int,
    tipusPista: String
): String {
    val isBase =
        tipusPista.equals(
            "Base",
            ignoreCase = true
        )

    if (isBase) {
        return when (zone) {
            1 ->
                shotText(
                    R.string.zone_three_45_right
                )

            2 ->
                shotText(
                    R.string.zone_three_middle
                )

            3 ->
                shotText(
                    R.string.zone_three_45_left
                )

            4 ->
                shotText(
                    R.string.zone_three_high_right
                )

            5 ->
                shotText(
                    R.string.zone_three_high_left
                )

            6 ->
                shotText(
                    R.string.zone_free_throw
                )

            7 ->
                shotText(
                    R.string.zone_three_low_right
                )

            8 ->
                shotText(
                    R.string.zone_paint
                )

            9 ->
                shotText(
                    R.string.zone_three_low_left
                )

            10 ->
                shotText(
                    R.string.zone_three_corner_right
                )

            11 ->
                shotText(
                    R.string.zone_three_corner_left
                )

            else ->
                shotText(
                    R.string.zone_generic,
                    zone
                )
        }
    }

    return when (zone) {
        1 ->
            shotText(
                R.string.zone_three_45_right
            )

        2 ->
            shotText(
                R.string.zone_three_middle
            )

        3 ->
            shotText(
                R.string.zone_three_45_left
            )

        4 ->
            shotText(
                R.string.zone_high_post_right
            )

        5 ->
            shotText(
                R.string.zone_high_post_left
            )

        6 ->
            shotText(
                R.string.zone_free_throw
            )

        7 ->
            shotText(
                R.string.zone_low_post_right
            )

        8 ->
            shotText(
                R.string.zone_paint
            )

        9 ->
            shotText(
                R.string.zone_low_post_left
            )

        10 ->
            shotText(
                R.string.zone_three_corner_right
            )

        11 ->
            shotText(
                R.string.zone_three_corner_left
            )

        else ->
            shotText(
                R.string.zone_generic,
                zone
            )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ZoneInputDialog(
    zone: Int,
    tipusPista: String,
    initialMade: Int,
    initialAttempted: Int,
    onDismiss: () -> Unit,
    onSave: (Int, Int) -> Unit
) {
    var made by remember(
        zone,
        initialMade
    ) {
        mutableStateOf(
            if (
                initialAttempted == 0 &&
                initialMade == 0
            ) {
                ""
            } else {
                initialMade.toString()
            }
        )
    }

    var attempted by remember(
        zone,
        initialAttempted
    ) {
        mutableStateOf(
            if (
                initialAttempted == 0 &&
                initialMade == 0
            ) {
                ""
            } else {
                initialAttempted.toString()
            }
        )
    }

    var errorMessage by remember {
        mutableStateOf<String?>(null)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                zoneLabel(
                    zone,
                    tipusPista
                )
            )
        },
        text = {
            Column {
                OutlinedTextField(
                    value = made,
                    onValueChange = { input ->
                        if (
                            input.all {
                                it.isDigit()
                            }
                        ) {
                            made = input
                            errorMessage = null
                        }
                    },
                    label = {
                        Text(
                            shotText(
                                R.string.made_shots
                            )
                        )
                    },
                    singleLine = true,
                    keyboardOptions =
                        KeyboardOptions(
                            keyboardType =
                                KeyboardType.Number
                        )
                )

                Spacer(
                    modifier =
                        Modifier.height(8.dp)
                )

                OutlinedTextField(
                    value = attempted,
                    onValueChange = { input ->
                        if (
                            input.all {
                                it.isDigit()
                            }
                        ) {
                            attempted =
                                input

                            errorMessage =
                                null
                        }
                    },
                    label = {
                        Text(
                            shotText(
                                R.string.attempted_shots
                            )
                        )
                    },
                    singleLine = true,
                    keyboardOptions =
                        KeyboardOptions(
                            keyboardType =
                                KeyboardType.Number
                        )
                )

                errorMessage?.let { message ->
                    Spacer(
                        modifier =
                            Modifier.height(8.dp)
                    )

                    Text(
                        text = message,
                        color =
                            MaterialTheme
                                .colorScheme
                                .error
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val madeValue =
                        made.toIntOrNull()
                            ?: 0

                    val attemptedValue =
                        attempted
                            .toIntOrNull()
                            ?: 0

                    errorMessage =
                        when {
                            madeValue < 0 ->
                                shotText(
                                    R.string
                                        .error_made_negative
                                )

                            attemptedValue < 0 ->
                                shotText(
                                    R.string
                                        .error_attempted_negative
                                )

                            madeValue >
                                attemptedValue ->
                                shotText(
                                    R.string
                                        .error_more_made_than_attempted
                                )

                            else ->
                                null
                        }

                    if (
                        errorMessage ==
                        null
                    ) {
                        onSave(
                            madeValue,
                            attemptedValue
                        )
                    }
                }
            ) {
                Text(
                    shotText(
                        R.string.save
                    )
                )
            }
        },
        dismissButton = {
            TextButton(
                onClick =
                    onDismiss
            ) {
                Text(
                    shotText(
                        R.string.cancel
                    )
                )
            }
        }
    )
}
