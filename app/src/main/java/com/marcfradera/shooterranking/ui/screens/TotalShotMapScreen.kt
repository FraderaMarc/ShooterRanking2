package com.marcfradera.shooterranking.ui.screens

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas as AndroidCanvas
import android.graphics.Color as AndroidColor
import android.graphics.Paint
import android.graphics.Path as AndroidPath
import android.graphics.RectF
import android.graphics.Typeface
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.marcfradera.shooterranking.R
import com.marcfradera.shooterranking.data.FirebaseProvider
import com.marcfradera.shooterranking.data.model.Sessio
import com.marcfradera.shooterranking.localization.AppLanguageManager
import com.marcfradera.shooterranking.ui.vm.ShotSessionViewModel
import kotlinx.coroutines.tasks.await
import java.io.File
import java.io.FileOutputStream
import java.util.Locale
import kotlin.math.sqrt

private fun totalShotText(resId: Int, vararg args: Any): String =
    AppLanguageManager.text(resId, *args)

private data class TotalShotPlayerInfo(
    val name: String,
    val dorsal: Int,
    val position: String,
    val courtType: String
)

private data class TotalShotStat(
    val made: Int,
    val attempted: Int
) {
    fun percentLabel(): String {
        if (attempted <= 0) return "-"
        return "${((made.toFloat() / attempted.toFloat()) * 100f).toInt()}%"
    }
}

private data class TotalShotSummary(
    val freeThrow: TotalShotStat,
    val twoPoint: TotalShotStat,
    val threePoint: TotalShotStat
)

@Composable
fun TotalShotMapScreen(
    idJugador: String,
    nomJugador: String,
    onBack: () -> Unit,
    forcedTipusPista: String? = null
) {
    val vm: ShotSessionViewModel = viewModel()
    val context = androidx.compose.ui.platform.LocalContext.current

    var playerInfo by remember(
        idJugador,
        nomJugador,
        forcedTipusPista
    ) {
        mutableStateOf<TotalShotPlayerInfo?>(null)
    }

    var playerInfoLoading by remember(idJugador) {
        mutableStateOf(true)
    }

    LaunchedEffect(idJugador, nomJugador, forcedTipusPista) {
        vm.load(idJugador)

        playerInfoLoading = true
        playerInfo = loadTotalShotPlayerInfo(
            idJugador = idJugador,
            fallbackName = nomJugador,
            forcedTipusPista = forcedTipusPista
        )
        playerInfoLoading = false
    }

    val sessionsState = vm.sessions
    val sessions = sessionsState.data.orEmpty()

    val aggregateSession = remember(sessions) {
        aggregateTotalShotSessions(sessions)
    }

    val courtType = playerInfo?.courtType
        ?: forcedTipusPista
        ?: "Amateur"

    val summary = remember(aggregateSession, courtType) {
        buildTotalShotSummary(
            session = aggregateSession,
            courtType = courtType
        )
    }

    val courtBitmap = remember(aggregateSession) {
        renderTotalShotCourtBitmap(
            session = aggregateSession,
            width = 906,
            height = 678
        )
    }

    CenteredScaffold(
        title = totalShotText(R.string.total_shot_map_title),
        onBack = onBack
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 24.dp)
        ) {
            Text(
                text = playerInfo?.name
                    ?.takeIf { it.isNotBlank() }
                    ?: nomJugador,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            Spacer(Modifier.height(4.dp))

            Text(
                text = totalShotText(R.string.total_shot_map_all_sessions),
                style = MaterialTheme.typography.bodyMedium
            )

            Spacer(Modifier.height(16.dp))

            when {
                sessionsState.loading || playerInfoLoading -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(280.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }

                sessionsState.error != null -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = sessionsState.error
                                ?: totalShotText(R.string.error_loading_sessions),
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }

                sessions.isEmpty() -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(totalShotText(R.string.no_player_sessions))
                    }
                }

                else -> {
                    Card(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Image(
                            bitmap = courtBitmap.asImageBitmap(),
                            contentDescription = totalShotText(
                                R.string.total_shot_map_title
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(453f / 339f)
                        )
                    }

                    Spacer(Modifier.height(14.dp))

                    Button(
                        onClick = {
                            try {
                                shareTotalShotMapPost(
                                    context = context,
                                    playerInfo = playerInfo
                                        ?: TotalShotPlayerInfo(
                                            name = nomJugador,
                                            dorsal = -1,
                                            position = "",
                                            courtType = courtType
                                        ),
                                    aggregateSession = aggregateSession,
                                    summary = summary
                                )
                            } catch (e: Exception) {
                                Toast.makeText(
                                    context,
                                    e.message
                                        ?: totalShotText(
                                            R.string.error_share_shot_map
                                        ),
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(totalShotText(R.string.share_shot_map))
                    }

                    Spacer(Modifier.height(16.dp))

                    Card(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = totalShotText(
                                    R.string.total_shot_statistics
                                ),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )

                            TotalShotStatRow(
                                label = totalShotText(
                                    R.string.free_throw_short
                                ),
                                stat = summary.freeThrow
                            )

                            TotalShotStatRow(
                                label = totalShotText(
                                    R.string.two_point_short
                                ),
                                stat = summary.twoPoint
                            )

                            TotalShotStatRow(
                                label = totalShotText(
                                    R.string.three_point_short
                                ),
                                stat = summary.threePoint
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TotalShotStatRow(
    label: String,
    stat: TotalShotStat
) {
    Text(
        text = "$label: ${stat.percentLabel()} · ${stat.made}/${stat.attempted}",
        style = MaterialTheme.typography.bodyLarge
    )
}

private suspend fun loadTotalShotPlayerInfo(
    idJugador: String,
    fallbackName: String,
    forcedTipusPista: String?
): TotalShotPlayerInfo {
    return try {
        val playerDoc = FirebaseProvider.firestore
            .collection("jugadors")
            .document(idJugador)
            .get()
            .await()

        val playerName = playerDoc
            .getString("nom_jugador")
            .orEmpty()
            .ifBlank { fallbackName }

        val dorsal = playerDoc
            .getLong("numero_jugador")
            ?.toInt()
            ?: -1

        val position = playerDoc
            .getString("posicio_jugador")
            .orEmpty()

        val directCourtType = playerDoc
            .getString("tipus_pista")
            ?.takeIf { it.isNotBlank() }

        val courtType = forcedTipusPista
            ?.takeIf { it.isNotBlank() }
            ?: directCourtType
            ?: loadTotalShotCourtTypeFromTeam(
                playerDoc.getString("id_equip").orEmpty()
            )

        TotalShotPlayerInfo(
            name = playerName,
            dorsal = dorsal,
            position = position,
            courtType = courtType
        )
    } catch (_: Exception) {
        TotalShotPlayerInfo(
            name = fallbackName,
            dorsal = -1,
            position = "",
            courtType = forcedTipusPista
                ?.takeIf { it.isNotBlank() }
                ?: "Amateur"
        )
    }
}

private suspend fun loadTotalShotCourtTypeFromTeam(
    idEquip: String
): String {
    if (idEquip.isBlank() || idEquip == "__personal_player__") {
        return "Amateur"
    }

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

private fun aggregateTotalShotSessions(
    sessions: List<Sessio>
): Sessio {
    return Sessio(
        id_jugador = sessions.firstOrNull()?.id_jugador.orEmpty(),

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

private fun buildTotalShotSummary(
    session: Sessio,
    courtType: String
): TotalShotSummary {
    val isBase = courtType.equals(
        "Base",
        ignoreCase = true
    )

    val twoMade = if (isBase) {
        session.fets_pos_8
    } else {
        session.fets_pos_4 +
            session.fets_pos_5 +
            session.fets_pos_7 +
            session.fets_pos_8 +
            session.fets_pos_9
    }

    val twoAttempted = if (isBase) {
        session.tirs_pos_8
    } else {
        session.tirs_pos_4 +
            session.tirs_pos_5 +
            session.tirs_pos_7 +
            session.tirs_pos_8 +
            session.tirs_pos_9
    }

    val threeMade = if (isBase) {
        session.fets_pos_1 +
            session.fets_pos_2 +
            session.fets_pos_3 +
            session.fets_pos_4 +
            session.fets_pos_5 +
            session.fets_pos_7 +
            session.fets_pos_9 +
            session.fets_pos_10 +
            session.fets_pos_11
    } else {
        session.fets_pos_1 +
            session.fets_pos_2 +
            session.fets_pos_3 +
            session.fets_pos_10 +
            session.fets_pos_11
    }

    val threeAttempted = if (isBase) {
        session.tirs_pos_1 +
            session.tirs_pos_2 +
            session.tirs_pos_3 +
            session.tirs_pos_4 +
            session.tirs_pos_5 +
            session.tirs_pos_7 +
            session.tirs_pos_9 +
            session.tirs_pos_10 +
            session.tirs_pos_11
    } else {
        session.tirs_pos_1 +
            session.tirs_pos_2 +
            session.tirs_pos_3 +
            session.tirs_pos_10 +
            session.tirs_pos_11
    }

    return TotalShotSummary(
        freeThrow = TotalShotStat(
            made = session.fets_pos_6,
            attempted = session.tirs_pos_6
        ),
        twoPoint = TotalShotStat(
            made = twoMade,
            attempted = twoAttempted
        ),
        threePoint = TotalShotStat(
            made = threeMade,
            attempted = threeAttempted
        )
    )
}

private fun renderTotalShotCourtBitmap(
    session: Sessio,
    width: Int,
    height: Int
): Bitmap {
    val bitmap = Bitmap.createBitmap(
        width,
        height,
        Bitmap.Config.ARGB_8888
    )

    val canvas = AndroidCanvas(bitmap)

    drawTotalShotCourt(
        canvas = canvas,
        session = session,
        left = 0f,
        top = 0f,
        width = width.toFloat(),
        height = height.toFloat()
    )

    return bitmap
}

private fun drawTotalShotCourt(
    canvas: AndroidCanvas,
    session: Sessio,
    left: Float,
    top: Float,
    width: Float,
    height: Float
) {
    val aspectRatio = 453f / 339f
    val drawHeight = minOf(
        height,
        width / aspectRatio
    )
    val drawWidth = drawHeight * aspectRatio

    val right = left + drawWidth
    val bottom = top + drawHeight

    val backgroundPaint = Paint(
        Paint.ANTI_ALIAS_FLAG
    ).apply {
        style = Paint.Style.FILL
        color = AndroidColor.WHITE
    }

    val linePaint = Paint(
        Paint.ANTI_ALIAS_FLAG
    ).apply {
        style = Paint.Style.STROKE
        strokeWidth = (drawWidth * 0.007f)
            .coerceAtLeast(3f)
        color = AndroidColor.BLACK
    }

    fun zonePaint(zone: Int): Paint {
        val (made, attempted) =
            totalShotZoneMadeAttempted(
                session,
                zone
            )

        val percentage =
            if (attempted > 0) {
                made.toFloat() /
                    attempted.toFloat()
            } else {
                null
            }

        return Paint(
            Paint.ANTI_ALIAS_FLAG
        ).apply {
            style = Paint.Style.FILL
            color = when {
                percentage == null ->
                    AndroidColor.WHITE

                percentage < 0.33f ->
                    AndroidColor.parseColor(
                        "#E53935"
                    )

                percentage <= 0.66f ->
                    AndroidColor.parseColor(
                        "#FDD835"
                    )

                else ->
                    AndroidColor.parseColor(
                        "#43A047"
                    )
            }
        }
    }

    fun rectPath(
        l: Float,
        t: Float,
        r: Float,
        b: Float
    ): AndroidPath {
        return AndroidPath().apply {
            addRect(
                RectF(l, t, r, b),
                AndroidPath.Direction.CW
            )
        }
    }

    fun quarterCirclePath(
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
                lineTo(
                    centerX - radius,
                    centerY
                )
                arcTo(
                    rect,
                    180f,
                    90f,
                    false
                )
                lineTo(centerX, centerY)
                close()
            } else {
                moveTo(centerX, centerY)
                lineTo(
                    centerX,
                    centerY - radius
                )
                arcTo(
                    rect,
                    270f,
                    90f,
                    false
                )
                lineTo(centerX, centerY)
                close()
            }
        }
    }

    fun freeThrowSemicirclePath(
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
            arcTo(
                rect,
                180f,
                180f,
                false
            )
            lineTo(
                centerX + radius,
                centerY
            )
            lineTo(
                centerX - radius,
                centerY
            )
            close()
        }
    }

    val topSplitY =
        top + 0.59f * drawHeight

    val lowerSplitY =
        top + 0.69f * drawHeight

    val third =
        drawWidth / 3f

    val paintLeft =
        left + third

    val paintRight =
        left + 2f * third

    val bigCenterX =
        left + drawWidth * 0.5f

    val bigCenterY =
        topSplitY + 0.10f * drawHeight

    val threePointRadius =
        0.42f * drawWidth

    val freeThrowCenterX =
        left + drawWidth * 0.5f

    val freeThrowCenterY =
        topSplitY

    val freeThrowRadius =
        (paintRight - paintLeft) / 2f

    val leftArcX =
        bigCenterX - threePointRadius

    val rightArcX =
        bigCenterX + threePointRadius

    val dxPaint =
        paintLeft - bigCenterX

    val arcPaintIntersectionY =
        bigCenterY -
            sqrt(
                (
                    threePointRadius *
                        threePointRadius
                    ) -
                    (
                        dxPaint *
                            dxPaint
                        )
            )

    fun zonePath(zone: Int): AndroidPath {
        return when (zone) {
            1 ->
                rectPath(
                    left,
                    top,
                    left + third,
                    lowerSplitY
                )

            2 ->
                rectPath(
                    left + third,
                    top,
                    left + 2f * third,
                    topSplitY
                )

            3 ->
                rectPath(
                    left + 2f * third,
                    top,
                    right,
                    lowerSplitY
                )

            4 ->
                quarterCirclePath(
                    bigCenterX,
                    bigCenterY,
                    threePointRadius,
                    true
                )

            5 ->
                quarterCirclePath(
                    bigCenterX,
                    bigCenterY,
                    threePointRadius,
                    false
                )

            6 ->
                freeThrowSemicirclePath(
                    freeThrowCenterX,
                    freeThrowCenterY,
                    freeThrowRadius
                )

            7 ->
                rectPath(
                    leftArcX,
                    lowerSplitY,
                    paintLeft,
                    bottom
                )

            8 ->
                rectPath(
                    paintLeft,
                    topSplitY,
                    paintRight,
                    bottom
                )

            9 ->
                rectPath(
                    paintRight,
                    lowerSplitY,
                    rightArcX,
                    bottom
                )

            10 ->
                rectPath(
                    left,
                    lowerSplitY,
                    leftArcX,
                    bottom
                )

            11 ->
                rectPath(
                    rightArcX,
                    lowerSplitY,
                    right,
                    bottom
                )

            else ->
                rectPath(
                    left,
                    top,
                    right,
                    bottom
                )
        }
    }

    canvas.drawRect(
        left,
        top,
        right,
        bottom,
        backgroundPaint
    )

    listOf(
        1,
        2,
        3,
        10,
        11,
        4,
        5,
        7,
        9,
        8,
        6
    ).forEach { zone ->
        canvas.drawPath(
            zonePath(zone),
            zonePaint(zone)
        )
    }

    canvas.drawRect(
        left,
        top,
        right,
        bottom,
        linePaint
    )

    canvas.drawLine(
        left + third,
        top,
        left + third,
        arcPaintIntersectionY,
        linePaint
    )

    canvas.drawLine(
        left + 2f * third,
        top,
        left + 2f * third,
        arcPaintIntersectionY,
        linePaint
    )

    canvas.drawArc(
        RectF(
            bigCenterX -
                threePointRadius,
            bigCenterY -
                threePointRadius,
            bigCenterX +
                threePointRadius,
            bigCenterY +
                threePointRadius
        ),
        180f,
        180f,
        false,
        linePaint
    )

    canvas.drawLine(
        bigCenterX,
        bigCenterY -
            threePointRadius,
        bigCenterX,
        freeThrowCenterY -
            freeThrowRadius,
        linePaint
    )

    canvas.drawArc(
        RectF(
            freeThrowCenterX -
                freeThrowRadius,
            freeThrowCenterY -
                freeThrowRadius,
            freeThrowCenterX +
                freeThrowRadius,
            freeThrowCenterY +
                freeThrowRadius
        ),
        180f,
        180f,
        false,
        linePaint
    )

    canvas.drawLine(
        paintLeft,
        top,
        paintLeft,
        arcPaintIntersectionY,
        linePaint
    )

    canvas.drawLine(
        paintRight,
        top,
        paintRight,
        arcPaintIntersectionY,
        linePaint
    )

    canvas.drawLine(
        paintLeft,
        topSplitY,
        paintLeft,
        bottom,
        linePaint
    )

    canvas.drawLine(
        paintRight,
        topSplitY,
        paintRight,
        bottom,
        linePaint
    )

    canvas.drawLine(
        leftArcX,
        bigCenterY,
        leftArcX,
        bottom,
        linePaint
    )

    canvas.drawLine(
        rightArcX,
        bigCenterY,
        rightArcX,
        bottom,
        linePaint
    )

    canvas.drawLine(
        paintLeft,
        topSplitY,
        paintRight,
        topSplitY,
        linePaint
    )

    canvas.drawLine(
        left,
        lowerSplitY,
        paintLeft,
        lowerSplitY,
        linePaint
    )

    canvas.drawLine(
        paintRight,
        lowerSplitY,
        right,
        lowerSplitY,
        linePaint
    )

    val hoopCenterX =
        left + drawWidth * 0.5f

    val hoopY =
        top + drawHeight * 0.93f

    val hoopLineWidth =
        drawWidth * 0.12f

    val hoopRadius =
        drawWidth * 0.018f

    canvas.drawLine(
        hoopCenterX -
            hoopLineWidth / 2f,
        hoopY,
        hoopCenterX +
            hoopLineWidth / 2f,
        hoopY,
        linePaint
    )

    canvas.drawCircle(
        hoopCenterX,
        hoopY -
            hoopRadius * 1.8f,
        hoopRadius,
        linePaint
    )
}

private fun totalShotZoneMadeAttempted(
    session: Sessio,
    zone: Int
): Pair<Int, Int> {
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

private fun shareTotalShotMapPost(
    context: Context,
    playerInfo: TotalShotPlayerInfo,
    aggregateSession: Sessio,
    summary: TotalShotSummary
) {
    val bitmap = createTotalShotSharePost(
        context = context,
        playerInfo = playerInfo,
        aggregateSession = aggregateSession,
        summary = summary
    )

    val safeName = playerInfo.name
        .trim()
        .lowercase(Locale.ROOT)
        .replace(
            Regex("[^a-z0-9]+"),
            "_"
        )
        .trim('_')
        .ifBlank { "player" }
        .take(40)

    val file = File(
        context.cacheDir,
        "shot_map_${safeName}_${System.currentTimeMillis()}.png"
    )

    FileOutputStream(file).use { output ->
        if (
            !bitmap.compress(
                Bitmap.CompressFormat.PNG,
                100,
                output
            )
        ) {
            throw IllegalStateException(
                totalShotText(
                    R.string.error_share_shot_map
                )
            )
        }
    }

    val uri: Uri = FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        file
    )

    val intent = Intent(
        Intent.ACTION_SEND
    ).apply {
        type = "image/png"
        putExtra(
            Intent.EXTRA_STREAM,
            uri
        )
        addFlags(
            Intent.FLAG_GRANT_READ_URI_PERMISSION
        )
    }

    context.startActivity(
        Intent.createChooser(
            intent,
            totalShotText(
                R.string.share_shot_map_chooser
            )
        )
    )
}



private fun createTotalShotSharePost(
    context: Context,
    playerInfo: TotalShotPlayerInfo,
    aggregateSession: Sessio,
    summary: TotalShotSummary
): Bitmap {
    val width = 1080
    val height = 1920

    val bitmap = Bitmap.createBitmap(
        width,
        height,
        Bitmap.Config.ARGB_8888
    )

    val canvas = AndroidCanvas(bitmap)

    /*
     * Fondo igualado al azul oscuro del título.
     */
    val backgroundColor = AndroidColor.parseColor("#292F59")
    val greenAccent = AndroidColor.parseColor("#7ADF49")
    val redAccent = AndroidColor.parseColor("#FF5C5C")
    val yellowAccent = AndroidColor.parseColor("#F2D53A")
    val whiteCard = AndroidColor.parseColor("#F8F8FB")
    val navyCard = AndroidColor.parseColor("#1D2348")

    canvas.drawColor(backgroundColor)

    val bitmapPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        isFilterBitmap = true
    }

    /*
     * Detalles decorativos tipo story / wrapped.
     */
    val decorPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }
    decorPaint.color = AndroidColor.argb(45, 122, 223, 73)
    canvas.drawCircle(120f, 190f, 110f, decorPaint)

    decorPaint.color = AndroidColor.argb(38, 255, 92, 92)
    canvas.drawCircle(960f, 1670f, 145f, decorPaint)

    decorPaint.color = AndroidColor.argb(28, 242, 213, 58)
    canvas.drawCircle(880f, 320f, 90f, decorPaint)

    val linePaintGreen = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = greenAccent
        style = Paint.Style.STROKE
        strokeWidth = 10f
    }

    val linePaintRed = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = redAccent
        style = Paint.Style.STROKE
        strokeWidth = 10f
    }

    canvas.drawLine(70f, 310f, 510f, 310f, linePaintGreen)
    canvas.drawLine(570f, 310f, 1005f, 310f, linePaintRed)

    val logo = BitmapFactory.decodeResource(
        context.resources,
        R.drawable.titulo_sr
    )

    if (logo != null) {
        canvas.drawBitmap(
            logo,
            null,
            RectF(
                120f,
                52f,
                960f,
                255f
            ),
            bitmapPaint
        )
    } else {
        val fallbackLogoPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = AndroidColor.WHITE
            textSize = 60f
            textAlign = Paint.Align.CENTER
            isFakeBoldText = true
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        }

        canvas.drawText(
            totalShotText(R.string.app_name).uppercase(),
            width / 2f,
            155f,
            fallbackLogoPaint
        )
    }

    /*
     * Card del mapa de tir.
     */
    val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = AndroidColor.argb(70, 0, 0, 0)
        style = Paint.Style.FILL
    }

    val mapCardShadow = RectF(118f, 352f, 970f, 1022f)
    canvas.drawRoundRect(mapCardShadow, 34f, 34f, shadowPaint)

    val mapCard = RectF(105f, 338f, 955f, 1008f)
    val mapCardPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = whiteCard
        style = Paint.Style.FILL
    }
    canvas.drawRoundRect(mapCard, 34f, 34f, mapCardPaint)

    val mapBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = AndroidColor.argb(75, 41, 47, 89)
        style = Paint.Style.STROKE
        strokeWidth = 3f
    }
    canvas.drawRoundRect(mapCard, 34f, 34f, mapBorderPaint)

    val courtRect = RectF(
        180f,
        405f,
        880f,
        930f
    )

    drawTotalShotCourt(
        canvas = canvas,
        session = aggregateSession,
        left = courtRect.left,
        top = courtRect.top,
        width = courtRect.width(),
        height = courtRect.height()
    )

    /*
     * Nombre + chips de posición y dorsal.
     */
    val namePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = AndroidColor.WHITE
        textAlign = Paint.Align.CENTER
        textSize = 78f
        typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
        setShadowLayer(6f, 0f, 3f, AndroidColor.parseColor("#1B2143"))
    }

    drawCenteredFittedText(
        canvas = canvas,
        text = playerInfo.name.ifBlank {
            totalShotText(R.string.player_fallback)
        },
        centerX = width / 2f,
        baselineY = 1125f,
        maxWidth = 860f,
        paint = namePaint,
        minTextSize = 42f
    )

    fun drawChip(
        rect: RectF,
        fillColor: Int,
        strokeColor: Int,
        textValue: String,
        textColor: Int = AndroidColor.WHITE
    ) {
        val chipShadow = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = AndroidColor.argb(55, 0, 0, 0)
            style = Paint.Style.FILL
        }

        canvas.drawRoundRect(
            RectF(rect.left + 6f, rect.top + 8f, rect.right + 6f, rect.bottom + 8f),
            30f,
            30f,
            chipShadow
        )

        val chipPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = fillColor
            style = Paint.Style.FILL
        }

        val chipBorder = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = strokeColor
            style = Paint.Style.STROKE
            strokeWidth = 3f
        }

        val chipText = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = textColor
            textAlign = Paint.Align.CENTER
            textSize = 34f
            isFakeBoldText = true
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        }

        canvas.drawRoundRect(rect, 30f, 30f, chipPaint)
        canvas.drawRoundRect(rect, 30f, 30f, chipBorder)

        val y = rect.centerY() - (chipText.descent() + chipText.ascent()) / 2f
        canvas.drawText(textValue, rect.centerX(), y, chipText)
    }

    val positionRect = RectF(165f, 1175f, 510f, 1248f)
    val dorsalRect = RectF(570f, 1175f, 915f, 1248f)

    drawChip(
        rect = positionRect,
        fillColor = greenAccent,
        strokeColor = AndroidColor.parseColor("#54B62C"),
        textValue = totalShotPositionLabel(playerInfo.position),
        textColor = AndroidColor.parseColor("#0F1838")
    )

    drawChip(
        rect = dorsalRect,
        fillColor = redAccent,
        strokeColor = AndroidColor.parseColor("#D64B4B"),
        textValue = totalShotDorsalLabel(playerInfo.dorsal),
        textColor = AndroidColor.WHITE
    )

    /*
     * Card estadística tipo wrapped.
     */
    val statsShadow = RectF(128f, 1298f, 966f, 1786f)
    canvas.drawRoundRect(statsShadow, 38f, 38f, shadowPaint)

    val statsCard = RectF(114f, 1284f, 950f, 1770f)
    val statsCardPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = whiteCard
        style = Paint.Style.FILL
    }
    canvas.drawRoundRect(statsCard, 38f, 38f, statsCardPaint)

    val statsCardBorder = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = AndroidColor.argb(85, 41, 47, 89)
        style = Paint.Style.STROKE
        strokeWidth = 3f
    }
    canvas.drawRoundRect(statsCard, 38f, 38f, statsCardBorder)

    val headerBand = RectF(statsCard.left, statsCard.top, statsCard.right, statsCard.top + 92f)
    val headerBandPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = navyCard
        style = Paint.Style.FILL
    }
    canvas.drawRoundRect(headerBand, 38f, 38f, headerBandPaint)
    canvas.drawRect(
        headerBand.left,
        headerBand.top + 46f,
        headerBand.right,
        headerBand.bottom,
        headerBandPaint
    )

    val statsTitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = AndroidColor.WHITE
        textAlign = Paint.Align.CENTER
        textSize = 38f
        isFakeBoldText = true
        typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
    }

    canvas.drawText(
        totalShotText(R.string.total_shot_statistics),
        statsCard.centerX(),
        statsCard.top + 58f,
        statsTitlePaint
    )

    val tableLeft = statsCard.left + 26f
    val tableTop = statsCard.top + 126f
    val tableRight = statsCard.right - 26f
    val rowHeight = 88f

    val col1 = tableLeft
    val col2 = tableLeft + 250f
    val col3 = tableLeft + 430f
    val col4 = tableRight

    val rowHeaderBottom = tableTop + rowHeight
    val row1Bottom = rowHeaderBottom + rowHeight
    val row2Bottom = row1Bottom + rowHeight
    val row3Bottom = row2Bottom + rowHeight

    val tableBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = AndroidColor.parseColor("#232949")
        style = Paint.Style.STROKE
        strokeWidth = 2.5f
    }

    val rowFillHeader = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = AndroidColor.parseColor("#E9ECFF")
        style = Paint.Style.FILL
    }

    val rowFillWhite = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = AndroidColor.parseColor("#FFFFFF")
        style = Paint.Style.FILL
    }

    val rowFillAlt = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = AndroidColor.parseColor("#F4F5FA")
        style = Paint.Style.FILL
    }

    fun drawTableRow(top: Float, bottom: Float, fillPaint: Paint) {
        canvas.drawRect(tableLeft, top, tableRight, bottom, fillPaint)
        canvas.drawRect(tableLeft, top, tableRight, bottom, tableBorderPaint)
        canvas.drawLine(col2, top, col2, bottom, tableBorderPaint)
        canvas.drawLine(col3, top, col3, bottom, tableBorderPaint)
    }

    drawTableRow(tableTop, rowHeaderBottom, rowFillHeader)
    drawTableRow(rowHeaderBottom, row1Bottom, rowFillWhite)
    drawTableRow(row1Bottom, row2Bottom, rowFillAlt)
    drawTableRow(row2Bottom, row3Bottom, rowFillWhite)

    val headerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = AndroidColor.parseColor("#111111")
        textSize = 30f
        textAlign = Paint.Align.CENTER
        isFakeBoldText = true
        typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
    }

    val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = AndroidColor.parseColor("#121212")
        textSize = 34f
        textAlign = Paint.Align.CENTER
        isFakeBoldText = true
        typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
    }

    val valuePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = AndroidColor.parseColor("#111111")
        textSize = 32f
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
    }

    fun centerY(top: Float, bottom: Float, paint: Paint): Float =
        (top + bottom) / 2f - (paint.descent() + paint.ascent()) / 2f

    fun centerX(left: Float, right: Float): Float = (left + right) / 2f

    canvas.drawText(
        totalShotText(R.string.total_shot_table_type),
        centerX(col1, col2),
        centerY(tableTop, rowHeaderBottom, headerPaint),
        headerPaint
    )
    canvas.drawText(
        totalShotText(R.string.total_shot_table_percentage),
        centerX(col2, col3),
        centerY(tableTop, rowHeaderBottom, headerPaint),
        headerPaint
    )
    canvas.drawText(
        totalShotText(R.string.total_shot_table_made_attempted),
        centerX(col3, col4),
        centerY(tableTop, rowHeaderBottom, headerPaint),
        headerPaint
    )

    val rows = listOf(
        Triple(
            totalShotText(R.string.free_throw_short),
            summary.freeThrow.percentLabel(),
            "${summary.freeThrow.made}/${summary.freeThrow.attempted}"
        ),
        Triple(
            totalShotText(R.string.two_point_short),
            summary.twoPoint.percentLabel(),
            "${summary.twoPoint.made}/${summary.twoPoint.attempted}"
        ),
        Triple(
            totalShotText(R.string.three_point_short),
            summary.threePoint.percentLabel(),
            "${summary.threePoint.made}/${summary.threePoint.attempted}"
        )
    )

    val bounds = listOf(
        rowHeaderBottom to row1Bottom,
        row1Bottom to row2Bottom,
        row2Bottom to row3Bottom
    )

    rows.zip(bounds).forEach { (row, range) ->
        val y1 = centerY(range.first, range.second, labelPaint)
        canvas.drawText(row.first, centerX(col1, col2), y1, labelPaint)
        canvas.drawText(row.second, centerX(col2, col3), y1, valuePaint)
        canvas.drawText(row.third, centerX(col3, col4), y1, valuePaint)
    }

    return bitmap
}

private fun drawCenteredFittedText(
    canvas: AndroidCanvas,
    text: String,
    centerX: Float,
    baselineY: Float,
    maxWidth: Float,
    paint: Paint,
    minTextSize: Float
) {
    while (
        paint.measureText(text) >
            maxWidth &&
        paint.textSize >
            minTextSize
    ) {
        paint.textSize -= 2f
    }

    canvas.drawText(
        text,
        centerX,
        baselineY,
        paint
    )
}

private fun totalShotPositionLabel(
    position: String
): String {
    return when (
        position
            .trim()
            .lowercase(Locale.ROOT)
    ) {
        "1",
        "base" ->
            totalShotText(
                R.string.position_point_guard
            )

        "2",
        "escolta" ->
            totalShotText(
                R.string.position_shooting_guard
            )

        "3",
        "aler" ->
            totalShotText(
                R.string.position_small_forward
            )

        "4",
        "aler-pivot",
        "aler pivot" ->
            totalShotText(
                R.string.position_power_forward
            )

        "5",
        "pivot",
        "pívot" ->
            totalShotText(
                R.string.position_center
            )

        else ->
            totalShotText(
                R.string.position_unknown
            )
    }
}

private fun totalShotDorsalLabel(
    dorsal: Int
): String {
    val formatted =
        when {
            dorsal < 0 ->
                "-"

            dorsal == 100 ->
                "00"

            else ->
                dorsal.toString()
        }

    return totalShotText(
        R.string.jersey_label,
        formatted
    )
}
