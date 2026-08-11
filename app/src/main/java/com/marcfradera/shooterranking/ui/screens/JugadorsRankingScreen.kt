package com.marcfradera.shooterranking.ui.screens

import android.content.Context
import android.content.Intent
import android.graphics.Paint
import android.graphics.Path as AndroidPath
import android.graphics.RectF
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
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
import kotlin.math.sqrt

private fun statsText(resId: Int, vararg args: Any): String =
    AppLanguageManager.text(resId, *args)

private enum class StatsFilter(val labelRes: Int) {
    ALL(R.string.filter_all),
    THREE_PT(R.string.filter_three_pointers),
    FREE_THROW(R.string.filter_free_throws),
    TWO_PT(R.string.filter_two_pointers)
}

private data class ProgressPoint(
    val sessionIndex: Int,
    val value: Float?
)

private data class SessionTableRow(
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

private data class PlayerPdfColumn(
    val title: String,
    val width: Float
)

private data class PlayerZoneStat(
    val label: String,
    val made: Int,
    val attempted: Int
) {
    fun pct(): Float = if (attempted <= 0) 0f else made.toFloat() / attempted.toFloat()
}

private data class PlayerPdfPageRows(
    val rows: List<SessionTableRow>,
    val showTotalRow: Boolean
)

private data class PlayerPdfProZoneLayer(
    val zone: Int,
    val path: AndroidPath
)

private val SessionStickyColumnWidth = 180.dp
private val BestZoneColumnWidth = 180.dp

@Composable
fun PlayerStatsScreen(
    idJugador: String,
    nomJugador: String,
    onBack: () -> Unit
) {
    val vm: ShotSessionViewModel = viewModel()
    var selectedFilter by remember { mutableStateOf(StatsFilter.ALL) }
    val context = LocalContext.current
    var tipusPista by remember(idJugador) { mutableStateOf("Amateur") }

    LaunchedEffect(idJugador) {
        vm.load(idJugador)
        tipusPista = loadTipusPistaByJugador(idJugador)
    }

    val sessionsState = vm.sessions
    val sessions = sessionsState.data ?: emptyList()

    val orderedSessions = remember(sessions) {
        sessions.sortedBy { it.num_sessio }
    }

    val tripleData = remember(orderedSessions, tipusPista) {
        orderedSessions.mapIndexed { index, s ->
            ProgressPoint(index, s.threePointPct(tipusPista))
        }
    }

    val freeThrowData = remember(orderedSessions) {
        orderedSessions.mapIndexed { index, s ->
            ProgressPoint(index, s.freeThrowPct())
        }
    }

    val twoPointData = remember(orderedSessions, tipusPista) {
        orderedSessions.mapIndexed { index, s ->
            ProgressPoint(index, s.twoPointPct(tipusPista))
        }
    }

    val tableRows = remember(orderedSessions, tipusPista) {
        orderedSessions.map { it.toTableRow(tipusPista) }
    }

    val totalRow = remember(orderedSessions, tipusPista) {
        buildTotalRow(orderedSessions, tipusPista)
    }

    CenteredScaffold(
        title = statsText(R.string.player_statistics_title, nomJugador),
        onBack = onBack
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 24.dp)
        ) {
            when {
                sessionsState.loading -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }

                sessionsState.error != null -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = sessionsState.error ?: statsText(R.string.error_loading_sessions),
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }

                orderedSessions.isEmpty() -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(statsText(R.string.no_player_sessions))
                    }
                }

                else -> {
                    Text(
                        text = statsText(R.string.progress_chart_by_session),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )

                    Spacer(Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        StatsFilter.entries.forEach { filter ->
                            FilterChip(
                                selected = selectedFilter == filter,
                                onClick = { selectedFilter = filter },
                                label = { Text(statsText(filter.labelRes)) }
                            )
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(430.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp)
                        ) {
                            ProgressChart(
                                sessionCount = orderedSessions.size,
                                showTriple = selectedFilter == StatsFilter.ALL || selectedFilter == StatsFilter.THREE_PT,
                                showFreeThrow = selectedFilter == StatsFilter.ALL || selectedFilter == StatsFilter.FREE_THROW,
                                showTwoPoint = selectedFilter == StatsFilter.ALL || selectedFilter == StatsFilter.TWO_PT,
                                tripleData = tripleData,
                                freeThrowData = freeThrowData,
                                twoPointData = twoPointData
                            )
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    LegendRow(
                        showTriple = selectedFilter == StatsFilter.ALL || selectedFilter == StatsFilter.THREE_PT,
                        showFreeThrow = selectedFilter == StatsFilter.ALL || selectedFilter == StatsFilter.FREE_THROW,
                        showTwoPoint = selectedFilter == StatsFilter.ALL || selectedFilter == StatsFilter.TWO_PT
                    )

                    Spacer(Modifier.height(20.dp))

                    Text(
                        text = statsText(R.string.sessions_table),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )

                    Spacer(Modifier.height(12.dp))

                    SessionStatsTable(
                        rows = tableRows,
                        totalRow = totalRow
                    )

                    Spacer(Modifier.height(16.dp))

                    Button(
                        onClick = {
                            try {
                                exportPlayerStatsPdfAndShare(
                                    context = context,
                                    nomJugador = nomJugador,
                                    sessions = orderedSessions,
                                    tipusPista = tipusPista
                                )
                            } catch (e: Exception) {
                                Toast.makeText(
                                    context,
                                    e.message ?: statsText(R.string.error_generate_pdf),
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(statsText(R.string.export_statistics_pdf))
                    }

                    Spacer(Modifier.height(24.dp))
                }
            }
        }
    }
}

@Composable
private fun ProgressChart(
    sessionCount: Int,
    showTriple: Boolean,
    showFreeThrow: Boolean,
    showTwoPoint: Boolean,
    tripleData: List<ProgressPoint>,
    freeThrowData: List<ProgressPoint>,
    twoPointData: List<ProgressPoint>
) {
    val tripleColor = Color(0xFF1565C0)
    val freeThrowColor = Color(0xFFD81B60)
    val twoPointColor = Color(0xFFEF6C00)
    val axisColor = Color.Black
    val gridColor = Color(0xFFD6D6D6)

    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        val leftPad = 110f
        val rightPad = 32f
        val topPad = 70f
        val bottomPad = 85f

        val chartWidth = size.width - leftPad - rightPad
        val chartHeight = size.height - topPad - bottomPad

        if (chartWidth <= 0f || chartHeight <= 0f) return@Canvas

        fun xFor(index: Int): Float {
            if (sessionCount <= 1) return leftPad + chartWidth / 2f
            return leftPad + (index.toFloat() / (sessionCount - 1).toFloat()) * chartWidth
        }

        fun yFor(value: Float): Float {
            val clamped = value.coerceIn(0f, 100f)
            return topPad + chartHeight - (clamped / 100f) * chartHeight
        }

        val labelPaint = Paint().apply {
            color = android.graphics.Color.BLACK
            textSize = 28f
            isAntiAlias = true
        }

        val titlePaint = Paint().apply {
            color = android.graphics.Color.BLACK
            textSize = 30f
            isAntiAlias = true
            isFakeBoldText = true
        }

        val yTicks = listOf(0, 25, 50, 75, 100)
        yTicks.forEach { tick ->
            val y = yFor(tick.toFloat())
            drawLine(
                color = gridColor,
                start = Offset(leftPad, y),
                end = Offset(leftPad + chartWidth, y),
                strokeWidth = 2f
            )
            drawContext.canvas.nativeCanvas.drawText(
                "$tick%",
                leftPad - 78f,
                y + 10f,
                labelPaint
            )
        }

        drawLine(
            color = axisColor,
            start = Offset(leftPad, topPad),
            end = Offset(leftPad, topPad + chartHeight),
            strokeWidth = 3f
        )

        drawLine(
            color = axisColor,
            start = Offset(leftPad, topPad + chartHeight),
            end = Offset(leftPad + chartWidth, topPad + chartHeight),
            strokeWidth = 3f
        )

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
            drawLine(
                color = axisColor,
                start = Offset(x, topPad + chartHeight),
                end = Offset(x, topPad + chartHeight + 10f),
                strokeWidth = 3f
            )
            drawContext.canvas.nativeCanvas.drawText(
                tick.toString(),
                x - 8f,
                topPad + chartHeight + 38f,
                labelPaint
            )
        }

        fun drawSeries(points: List<ProgressPoint>, color: Color) {
            data class ChartPoint(
                val sessionIndex: Int,
                val value: Float,
                val isReal: Boolean
            )

            if (points.isEmpty()) return

            val realPoints = points.mapNotNull { point ->
                point.value?.let { value ->
                    ChartPoint(
                        sessionIndex = point.sessionIndex,
                        value = value,
                        isReal = true
                    )
                }
            }

            if (realPoints.isEmpty()) return

            val linePoints = mutableListOf<ChartPoint>()
            val firstReal = realPoints.first()

            if (firstReal.sessionIndex > 0) {
                linePoints += ChartPoint(
                    sessionIndex = 0,
                    value = 0f,
                    isReal = false
                )
            }

            linePoints += realPoints

            if (linePoints.size == 1) {
                val point = linePoints.first()
                if (point.isReal) {
                    drawCircle(
                        color = color,
                        radius = 7f,
                        center = Offset(
                            xFor(point.sessionIndex),
                            yFor(point.value)
                        )
                    )
                }
                return
            }

            val path = Path()
            linePoints.forEachIndexed { i, point ->
                val x = xFor(point.sessionIndex)
                val y = yFor(point.value)

                if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }

            drawPath(
                path = path,
                color = color,
                style = Stroke(width = 5f, cap = StrokeCap.Round)
            )

            linePoints
                .filter { it.isReal }
                .forEach { point ->
                    drawCircle(
                        color = color,
                        radius = 6f,
                        center = Offset(
                            xFor(point.sessionIndex),
                            yFor(point.value)
                        )
                    )
                }
        }

        if (showTriple) drawSeries(tripleData, tripleColor)
        if (showFreeThrow) drawSeries(freeThrowData, freeThrowColor)
        if (showTwoPoint) drawSeries(twoPointData, twoPointColor)

        drawContext.canvas.nativeCanvas.drawText(
            statsText(R.string.hit_percentage),
            leftPad - 35f,
            topPad - 25f,
            titlePaint
        )

        drawContext.canvas.nativeCanvas.drawText(
            statsText(R.string.sessions_axis),
            leftPad + chartWidth / 2f - 55f,
            size.height - 18f,
            titlePaint
        )
    }
}

@Composable
private fun LegendRow(
    showTriple: Boolean,
    showFreeThrow: Boolean,
    showTwoPoint: Boolean
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (showTriple) {
            LegendItem(
                color = Color(0xFF1565C0),
                text = statsText(R.string.filter_three_pointers)
            )
        }

        if (showFreeThrow) {
            LegendItem(
                color = Color(0xFFD81B60),
                text = statsText(R.string.filter_free_throws)
            )
        }

        if (showTwoPoint) {
            LegendItem(
                color = Color(0xFFEF6C00),
                text = statsText(R.string.filter_two_pointers)
            )
        }
    }
}

@Composable
private fun LegendItem(
    color: Color,
    text: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .width(28.dp)
                .height(10.dp)
                .background(color, RoundedCornerShape(50))
        )
        Text(text)
    }
}

@Composable
private fun SessionStatsTable(
    rows: List<SessionTableRow>,
    totalRow: SessionTableRow
) {
    val scroll = rememberScrollState()
    val borderColor = Color(0xFFBDBDBD)

    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .border(1.dp, borderColor)
        ) {
            Column {
                HeaderCell(statsText(R.string.session_header), SessionStickyColumnWidth)

                rows.forEach { row ->
                    SessionTableStickyCell(
                        text = row.label,
                        isTotal = false
                    )
                }

                SessionTableStickyCell(
                    text = totalRow.label,
                    isTotal = true
                )
            }

            Box(
                modifier = Modifier.horizontalScroll(scroll)
            ) {
                Column {
                    SessionTableScrollableHeader()

                    rows.forEach { row ->
                        SessionTableScrollableDataRow(
                            row = row,
                            isTotal = false
                        )
                    }

                    SessionTableScrollableDataRow(
                        row = totalRow,
                        isTotal = true
                    )
                }
            }
        }
    }
}

@Composable
private fun SessionTableStickyCell(
    text: String,
    isTotal: Boolean
) {
    Box(
        modifier = Modifier
            .width(SessionStickyColumnWidth)
            .border(1.dp, Color(0xFF8A8A8A))
            .background(if (isTotal) Color(0xFFF0F7FF) else Color.White)
            .padding(vertical = 8.dp, horizontal = 6.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Text(
            text = text,
            fontWeight = if (isTotal) FontWeight.Bold else FontWeight.Normal,
            style = MaterialTheme.typography.bodyMedium,
            color = Color.Black,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun SessionTableScrollableHeader() {
    Row {
        HeaderCell(statsText(R.string.free_throw_short), 90.dp)
        HeaderCell(statsText(R.string.free_throw_percent_short), 80.dp)
        HeaderCell(statsText(R.string.two_point_short), 90.dp)
        HeaderCell(statsText(R.string.two_point_percent_short), 80.dp)
        HeaderCell(statsText(R.string.three_point_short), 90.dp)
        HeaderCell(statsText(R.string.three_point_percent_short), 80.dp)
        HeaderCell(statsText(R.string.total_upper), 100.dp)
        HeaderCell(statsText(R.string.total_percent), 90.dp)
        HeaderCell(statsText(R.string.right_percent), 90.dp)
        HeaderCell(statsText(R.string.left_percent), 100.dp)
        HeaderCell(statsText(R.string.best_side), 110.dp)
        HeaderCell(statsText(R.string.best_zone_2pt), BestZoneColumnWidth)
        HeaderCell(statsText(R.string.best_zone_3pt), BestZoneColumnWidth)
    }
}

@Composable
private fun SessionTableScrollableDataRow(
    row: SessionTableRow,
    isTotal: Boolean
) {
    val weight = if (isTotal) FontWeight.Bold else FontWeight.Normal

    Row {
        DataCell("${row.tlMade}/${row.tlAttempted}", 90.dp, weight, isTotal)
        DataCell(playerFormatPct(row.tlMade, row.tlAttempted), 80.dp, weight, isTotal)
        DataCell("${row.t2Made}/${row.t2Attempted}", 90.dp, weight, isTotal)
        DataCell(playerFormatPct(row.t2Made, row.t2Attempted), 80.dp, weight, isTotal)
        DataCell("${row.t3Made}/${row.t3Attempted}", 90.dp, weight, isTotal)
        DataCell(playerFormatPct(row.t3Made, row.t3Attempted), 80.dp, weight, isTotal)
        DataCell("${row.totalMade}/${row.totalAttempted}", 100.dp, weight, isTotal)
        DataCell(playerFormatPct(row.totalMade, row.totalAttempted), 90.dp, weight, isTotal)
        DataCell(row.rightPct.toPlayerPercentOrDash(), 90.dp, weight, isTotal)
        DataCell(row.leftPct.toPlayerPercentOrDash(), 100.dp, weight, isTotal)
        DataCell(row.bestSide, 110.dp, weight, isTotal)
        DataCell(row.bestZoneT2, BestZoneColumnWidth, weight, isTotal)
        DataCell(row.bestZoneT3, BestZoneColumnWidth, weight, isTotal)
    }
}

@Composable
private fun HeaderCell(text: String, width: Dp) {
    Box(
        modifier = Modifier
            .width(width)
            .border(1.dp, Color(0xFF8A8A8A))
            .background(Color(0xFFF5F5F5))
            .padding(vertical = 8.dp, horizontal = 6.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Text(
            text = text,
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.bodyMedium,
            color = Color.Black
        )
    }
}

@Composable
private fun DataCell(
    text: String,
    width: Dp,
    weight: FontWeight,
    isTotal: Boolean = false
) {
    Box(
        modifier = Modifier
            .width(width)
            .border(1.dp, Color(0xFF8A8A8A))
            .background(if (isTotal) Color(0xFFF0F7FF) else Color.White)
            .padding(vertical = 8.dp, horizontal = 6.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Text(
            text = text,
            fontWeight = weight,
            style = MaterialTheme.typography.bodyMedium,
            color = Color.Black
        )
    }
}

private fun Sessio.toTableRow(tipusPista: String): SessionTableRow {
    val tlMade = fets_pos_6
    val tlAttempted = tirs_pos_6

    val t2Made = twoPointMade(tipusPista)
    val t2Attempted = twoPointAttempted(tipusPista)

    val t3Made = threePointMade(tipusPista)
    val t3Attempted = threePointAttempted(tipusPista)

    val totalMade = tlMade + t2Made + t3Made
    val totalAttempted = tlAttempted + t2Attempted + t3Attempted

    val rightMade = fets_pos_1 + fets_pos_4 + fets_pos_7 + fets_pos_10
    val rightAttempted = tirs_pos_1 + tirs_pos_4 + tirs_pos_7 + tirs_pos_10

    val leftMade = fets_pos_3 + fets_pos_5 + fets_pos_9 + fets_pos_11
    val leftAttempted = tirs_pos_3 + tirs_pos_5 + tirs_pos_9 + tirs_pos_11

    val rightPct = playerPctOrNull(rightMade, rightAttempted)
    val leftPct = playerPctOrNull(leftMade, leftAttempted)

    return SessionTableRow(
        label = statsText(R.string.session_number, num_sessio),
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
        bestZoneT2 = playerBestZoneT2Label(this, tipusPista),
        bestZoneT3 = playerBestZoneT3Label(this, tipusPista)
    )
}

private fun buildTotalRow(
    sessions: List<Sessio>,
    tipusPista: String
): SessionTableRow {
    val tlMade = sessions.sumOf { it.fets_pos_6 }
    val tlAttempted = sessions.sumOf { it.tirs_pos_6 }

    val t2Made = sessions.sumOf { it.twoPointMade(tipusPista) }
    val t2Attempted = sessions.sumOf { it.twoPointAttempted(tipusPista) }

    val t3Made = sessions.sumOf { it.threePointMade(tipusPista) }
    val t3Attempted = sessions.sumOf { it.threePointAttempted(tipusPista) }

    val totalMade = tlMade + t2Made + t3Made
    val totalAttempted = tlAttempted + t2Attempted + t3Attempted

    val rightMade = sessions.sumOf { it.fets_pos_1 + it.fets_pos_4 + it.fets_pos_7 + it.fets_pos_10 }
    val rightAttempted = sessions.sumOf { it.tirs_pos_1 + it.tirs_pos_4 + it.tirs_pos_7 + it.tirs_pos_10 }

    val leftMade = sessions.sumOf { it.fets_pos_3 + it.fets_pos_5 + it.fets_pos_9 + it.fets_pos_11 }
    val leftAttempted = sessions.sumOf { it.tirs_pos_3 + it.tirs_pos_5 + it.tirs_pos_9 + it.tirs_pos_11 }

    val merged = playerAggregateSessions(sessions)
    val rightPct = playerPctOrNull(rightMade, rightAttempted)
    val leftPct = playerPctOrNull(leftMade, leftAttempted)

    return SessionTableRow(
        label = statsText(R.string.total),
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
        bestZoneT2 = playerBestZoneT2Label(merged, tipusPista),
        bestZoneT3 = playerBestZoneT3Label(merged, tipusPista)
    )
}

private fun playerBestZoneT2Label(
    s: Sessio,
    tipusPista: String
): String {
    val zones = if (isBaseCourt(tipusPista)) {
        listOf(
            PlayerZoneStat(statsText(R.string.zone_paint), s.fets_pos_8, s.tirs_pos_8)
        )
    } else {
        listOf(
            PlayerZoneStat(statsText(R.string.zone_high_post_right), s.fets_pos_4, s.tirs_pos_4),
            PlayerZoneStat(statsText(R.string.zone_high_post_left), s.fets_pos_5, s.tirs_pos_5),
            PlayerZoneStat(statsText(R.string.zone_low_post_right), s.fets_pos_7, s.tirs_pos_7),
            PlayerZoneStat(statsText(R.string.zone_paint), s.fets_pos_8, s.tirs_pos_8),
            PlayerZoneStat(statsText(R.string.zone_low_post_left), s.fets_pos_9, s.tirs_pos_9)
        )
    }.filter { it.attempted > 0 }

    if (zones.isEmpty()) return "-"

    return zones.maxWithOrNull(
        compareBy<PlayerZoneStat> { it.pct() }.thenBy { it.attempted }
    )?.label ?: "-"
}

private fun playerBestZoneT3Label(
    s: Sessio,
    tipusPista: String
): String {
    val zones = if (isBaseCourt(tipusPista)) {
        listOf(
            PlayerZoneStat(statsText(R.string.zone_three_45_right), s.fets_pos_1, s.tirs_pos_1),
            PlayerZoneStat(statsText(R.string.zone_three_middle), s.fets_pos_2, s.tirs_pos_2),
            PlayerZoneStat(statsText(R.string.zone_three_45_left), s.fets_pos_3, s.tirs_pos_3),
            PlayerZoneStat(statsText(R.string.zone_three_high_right), s.fets_pos_4, s.tirs_pos_4),
            PlayerZoneStat(statsText(R.string.zone_three_high_left), s.fets_pos_5, s.tirs_pos_5),
            PlayerZoneStat(statsText(R.string.zone_three_low_right), s.fets_pos_7, s.tirs_pos_7),
            PlayerZoneStat(statsText(R.string.zone_three_low_left), s.fets_pos_9, s.tirs_pos_9),
            PlayerZoneStat(statsText(R.string.zone_three_corner_right), s.fets_pos_10, s.tirs_pos_10),
            PlayerZoneStat(statsText(R.string.zone_three_corner_left), s.fets_pos_11, s.tirs_pos_11)
        )
    } else {
        listOf(
            PlayerZoneStat(statsText(R.string.zone_three_45_right), s.fets_pos_1, s.tirs_pos_1),
            PlayerZoneStat(statsText(R.string.zone_three_middle), s.fets_pos_2, s.tirs_pos_2),
            PlayerZoneStat(statsText(R.string.zone_three_45_left), s.fets_pos_3, s.tirs_pos_3),
            PlayerZoneStat(statsText(R.string.zone_three_corner_right), s.fets_pos_10, s.tirs_pos_10),
            PlayerZoneStat(statsText(R.string.zone_three_corner_left), s.fets_pos_11, s.tirs_pos_11)
        )
    }.filter { it.attempted > 0 }

    if (zones.isEmpty()) return "-"

    return zones.maxWithOrNull(
        compareBy<PlayerZoneStat> { it.pct() }.thenBy { it.attempted }
    )?.label ?: "-"
}

private fun rankingBestSideLabel(rightPct: Float?, leftPct: Float?): String {
    return when {
        rightPct == null && leftPct == null -> "-"
        rightPct != null && leftPct == null -> statsText(R.string.right)
        rightPct == null && leftPct != null -> statsText(R.string.left)
        else -> {
            val right = rightPct!!
            val left = leftPct!!
            when {
                right > left -> statsText(R.string.right)
                left > right -> statsText(R.string.left)
                else -> statsText(R.string.equal)
            }
        }
    }
}

private fun playerFormatPct(made: Int, attempted: Int): String {
    val pct = playerPctOrNull(made, attempted) ?: return "-"
    return "${pct.toInt()}%"
}

private fun Float?.toPlayerPercentOrDash(): String {
    return this?.let { "${it.toInt()}%" } ?: "-"
}

private fun Sessio.threePointPct(tipusPista: String): Float? {
    return playerPctOrNull(
        made = threePointMade(tipusPista),
        attempted = threePointAttempted(tipusPista)
    )
}

private fun Sessio.freeThrowPct(): Float? {
    return playerPctOrNull(fets_pos_6, tirs_pos_6)
}

private fun Sessio.twoPointPct(tipusPista: String): Float? {
    return playerPctOrNull(
        made = twoPointMade(tipusPista),
        attempted = twoPointAttempted(tipusPista)
    )
}

private fun playerPctOrNull(made: Int, attempted: Int): Float? {
    if (attempted <= 0) return null
    return (made.toFloat() / attempted.toFloat()) * 100f
}

private fun isBaseCourt(tipusPista: String): Boolean {
    return tipusPista.equals("Base", ignoreCase = true)
}

private fun Sessio.threePointMade(tipusPista: String): Int {
    return if (isBaseCourt(tipusPista)) {
        fets_pos_1 + fets_pos_2 + fets_pos_3 +
                fets_pos_4 + fets_pos_5 +
                fets_pos_7 + fets_pos_9 +
                fets_pos_10 + fets_pos_11
    } else {
        fets_pos_1 + fets_pos_2 + fets_pos_3 +
                fets_pos_10 + fets_pos_11
    }
}

private fun Sessio.threePointAttempted(tipusPista: String): Int {
    return if (isBaseCourt(tipusPista)) {
        tirs_pos_1 + tirs_pos_2 + tirs_pos_3 +
                tirs_pos_4 + tirs_pos_5 +
                tirs_pos_7 + tirs_pos_9 +
                tirs_pos_10 + tirs_pos_11
    } else {
        tirs_pos_1 + tirs_pos_2 + tirs_pos_3 +
                tirs_pos_10 + tirs_pos_11
    }
}

private fun Sessio.twoPointMade(tipusPista: String): Int {
    return if (isBaseCourt(tipusPista)) {
        fets_pos_8
    } else {
        fets_pos_4 + fets_pos_5 + fets_pos_7 + fets_pos_8 + fets_pos_9
    }
}

private fun Sessio.twoPointAttempted(tipusPista: String): Int {
    return if (isBaseCourt(tipusPista)) {
        tirs_pos_8
    } else {
        tirs_pos_4 + tirs_pos_5 + tirs_pos_7 + tirs_pos_8 + tirs_pos_9
    }
}

private suspend fun loadTipusPistaByJugador(idJugador: String): String {
    return try {
        val jugadorDoc = FirebaseProvider.firestore
            .collection("jugadors")
            .document(idJugador)
            .get()
            .await()

        val idEquip = jugadorDoc.getString("id_equip").orEmpty()

        if (idEquip.isBlank()) {
            "Amateur"
        } else {
            FirebaseProvider.firestore
                .collection("equips")
                .document(idEquip)
                .get()
                .await()
                .getString("tipus_pista")
                ?.takeIf { it.isNotBlank() }
                ?: "Amateur"
        }
    } catch (_: Exception) {
        "Amateur"
    }
}

private fun playerAggregateSessions(sessions: List<Sessio>): Sessio {
    return Sessio(
        num_sessio = 0,
        id_jugador = sessions.firstOrNull()?.id_jugador ?: "",
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

private fun exportPlayerStatsPdfAndShare(
    context: Context,
    nomJugador: String,
    sessions: List<Sessio>,
    tipusPista: String
) {
    val orderedSessions = sessions.sortedBy { it.num_sessio }
    val tableRows = orderedSessions.map { it.toTableRow(tipusPista) }
    val totalRow = buildTotalRow(orderedSessions, tipusPista)
    val globalSession = playerAggregateSessions(orderedSessions)

    val tripleData = orderedSessions.mapIndexed { index, s ->
        ProgressPoint(index, s.threePointPct(tipusPista))
    }
    val freeThrowData = orderedSessions.mapIndexed { index, s ->
        ProgressPoint(index, s.freeThrowPct())
    }
    val twoPointData = orderedSessions.mapIndexed { index, s ->
        ProgressPoint(index, s.twoPointPct(tipusPista))
    }

    val document = PdfDocument()
    val pages = buildPlayerPdfPages(tableRows)

    pages.forEachIndexed { index, pageRows ->
        drawPlayerPdfSinglePage(
            document = document,
            pageNumber = index + 1,
            nomJugador = nomJugador,
            pageRows = pageRows.rows,
            totalRow = if (pageRows.showTotalRow) totalRow else null,
            sessionCount = orderedSessions.size,
            tripleData = tripleData,
            freeThrowData = freeThrowData,
            twoPointData = twoPointData,
            globalSession = globalSession,
            tipusPista = tipusPista
        )
    }

    val file = File(context.cacheDir, "stats_${nomJugador.replace(" ", "_")}.pdf")
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

    context.startActivity(Intent.createChooser(intent, statsText(R.string.share_pdf)))
}

private fun buildPlayerPdfPages(
    tableRows: List<SessionTableRow>,
    maxRowsPerPage: Int = 12
): List<PlayerPdfPageRows> {
    val safeMaxRowsPerPage = maxOf(2, maxRowsPerPage)

    if (tableRows.isEmpty()) {
        return listOf(PlayerPdfPageRows(rows = emptyList(), showTotalRow = true))
    }

    val pages = mutableListOf<PlayerPdfPageRows>()
    var startIndex = 0

    while (startIndex < tableRows.size) {
        val remainingRegularRows = tableRows.size - startIndex
        val canFitTotalOnThisPage = remainingRegularRows + 1 <= safeMaxRowsPerPage
        val regularRowsOnPage = if (canFitTotalOnThisPage) {
            remainingRegularRows
        } else {
            safeMaxRowsPerPage
        }

        val endIndex = (startIndex + regularRowsOnPage).coerceAtMost(tableRows.size)
        pages += PlayerPdfPageRows(
            rows = tableRows.subList(startIndex, endIndex),
            showTotalRow = canFitTotalOnThisPage
        )
        startIndex = endIndex
    }

    if (pages.lastOrNull()?.showTotalRow != true) {
        pages += PlayerPdfPageRows(rows = emptyList(), showTotalRow = true)
    }

    return pages
}

private fun drawPlayerPdfSinglePage(
    document: PdfDocument,
    pageNumber: Int,
    nomJugador: String,
    pageRows: List<SessionTableRow>,
    totalRow: SessionTableRow?,
    sessionCount: Int,
    tripleData: List<ProgressPoint>,
    freeThrowData: List<ProgressPoint>,
    twoPointData: List<ProgressPoint>,
    globalSession: Sessio,
    tipusPista: String
) {
    val pageWidth = 1650
    val pageHeight = 1000
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

    canvas.drawText(statsText(R.string.player_statistics_title, nomJugador), 45f, 48f, titlePaint)

    val chartRect = RectF(45f, 110f, 835f, 385f)
    drawPlayerPdfCompactChart(
        canvas = canvas,
        area = chartRect,
        sessionCount = sessionCount,
        tripleData = tripleData,
        freeThrowData = freeThrowData,
        twoPointData = twoPointData
    )

    drawPlayerPdfLegend(canvas, startX = 60f, y = 470f)

    val mapHeight = chartRect.height()
    val mapWidth = mapHeight * (453f / 339f)

    canvas.drawText(statsText(R.string.global_shot_map), 1115f, 95f, subtitlePaint)
    drawPlayerPdfCourtMap(
        canvas = canvas,
        session = globalSession,
        left = 1115f,
        top = 110f,
        width = mapWidth,
        height = mapHeight,
        tipusPista = tipusPista
    )

    canvas.drawText(statsText(R.string.sessions_table), 45f, 520f, subtitlePaint)

    val allRows = if (totalRow != null) pageRows + totalRow else pageRows
    val columns = listOf(
        PlayerPdfColumn(statsText(R.string.session_header), 180f),
        PlayerPdfColumn(statsText(R.string.free_throw_short), 85f),
        PlayerPdfColumn(statsText(R.string.free_throw_percent_short), 70f),
        PlayerPdfColumn(statsText(R.string.two_point_short), 85f),
        PlayerPdfColumn(statsText(R.string.two_point_percent_short), 70f),
        PlayerPdfColumn(statsText(R.string.three_point_short), 85f),
        PlayerPdfColumn(statsText(R.string.three_point_percent_short), 70f),
        PlayerPdfColumn(statsText(R.string.total_upper), 95f),
        PlayerPdfColumn(statsText(R.string.total_percent), 85f),
        PlayerPdfColumn(statsText(R.string.right_percent), 85f),
        PlayerPdfColumn(statsText(R.string.left_percent), 95f),
        PlayerPdfColumn(statsText(R.string.best_side), 110f),
        PlayerPdfColumn(statsText(R.string.best_zone_2pt), 165f),
        PlayerPdfColumn(statsText(R.string.best_zone_3pt), 165f)
    )

    val headerY = 550f
    val availableHeight = pageHeight - headerY - 40f
    val rowHeight = ((availableHeight - 34f) / (allRows.size + 1)).coerceIn(14f, 30f)
    val textSize = (rowHeight * 0.42f).coerceIn(9f, 13f)

    drawPlayerPdfTableHeader(
        canvas = canvas,
        startX = 45f,
        startY = headerY,
        columns = columns,
        rowHeight = rowHeight,
        textSize = textSize
    )

    var currentY = headerY + rowHeight
    allRows.forEach { row ->
        drawPlayerPdfStatsRow(
            canvas = canvas,
            row = row,
            columns = columns,
            startX = 45f,
            startY = currentY,
            rowHeight = rowHeight,
            textSize = textSize,
            isTotal = totalRow != null && row == totalRow
        )
        currentY += rowHeight
    }

    document.finishPage(page)
}

private fun drawPlayerPdfCompactChart(
    canvas: android.graphics.Canvas,
    area: RectF,
    sessionCount: Int,
    tripleData: List<ProgressPoint>,
    freeThrowData: List<ProgressPoint>,
    twoPointData: List<ProgressPoint>
) {
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

    canvas.drawText(statsText(R.string.sessions_chart), area.left, area.top - 12f, smallTitlePaint)

    val left = area.left + 42f
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

    fun drawSeries(points: List<ProgressPoint>, color: Int) {
        data class PdfChartPoint(
            val sessionIndex: Int,
            val value: Float,
            val isReal: Boolean
        )

        if (points.isEmpty()) return

        val realPoints = points.mapNotNull { point ->
            point.value?.let { value ->
                PdfChartPoint(
                    sessionIndex = point.sessionIndex,
                    value = value,
                    isReal = true
                )
            }
        }

        if (realPoints.isEmpty()) return

        val linePoints = mutableListOf<PdfChartPoint>()
        val firstReal = realPoints.first()

        if (firstReal.sessionIndex > 0) {
            linePoints += PdfChartPoint(
                sessionIndex = 0,
                value = 0f,
                isReal = false
            )
        }

        linePoints += realPoints

        if (linePoints.size == 1) {
            val point = linePoints.first()
            if (point.isReal) {
                val pointPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    style = Paint.Style.FILL
                    this.color = color
                }
                canvas.drawCircle(xFor(point.sessionIndex), yFor(point.value), 3.5f, pointPaint)
            }
            return
        }

        val path = AndroidPath()
        linePoints.forEachIndexed { index, point ->
            val x = xFor(point.sessionIndex)
            val y = yFor(point.value)
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
        linePoints
            .filter { it.isReal }
            .forEach { point ->
                canvas.drawCircle(xFor(point.sessionIndex), yFor(point.value), 3.5f, pointPaint)
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

    canvas.drawText(statsText(R.string.sessions_axis), left + width / 2f - 25f, bottom + 44f, textPaint)
}

private fun drawPlayerPdfLegend(
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

    item(startX, statsText(R.string.filter_three_pointers), "#1565C0")
    item(startX + 170f, statsText(R.string.filter_free_throws), "#D81B60")
    item(startX + 370f, statsText(R.string.filter_two_pointers), "#EF6C00")
}


private fun drawPlayerPdfTextFitted(
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

private fun drawPlayerPdfSessionNameFitted(
    canvas: android.graphics.Canvas,
    text: String,
    rect: RectF,
    paint: Paint,
    horizontalPadding: Float = 4f,
    minTextSize: Float = 9f
) {
    val originalSize = paint.textSize
    val availableWidth = (rect.width() - horizontalPadding * 2f).coerceAtLeast(1f)

    while (paint.measureText(text) > availableWidth && paint.textSize > minTextSize) {
        paint.textSize -= 0.5f
    }

    var fittedText = text

    if (paint.measureText(fittedText) > availableWidth) {
        val ellipsis = "…"

        while (
            fittedText.length > 1 &&
            paint.measureText(fittedText.dropLast(1).trimEnd() + ellipsis) > availableWidth
        ) {
            fittedText = fittedText.dropLast(1)
        }

        fittedText = fittedText.trimEnd()
        fittedText = if (fittedText.length > 1) {
            fittedText.dropLast(1).trimEnd() + ellipsis
        } else {
            ellipsis
        }
    }

    val textY = rect.centerY() - (paint.descent() + paint.ascent()) / 2f
    canvas.drawText(fittedText, rect.left + horizontalPadding, textY, paint)
    paint.textSize = originalSize
}

private fun drawPlayerPdfTableHeader(
    canvas: android.graphics.Canvas,
    startX: Float,
    startY: Float,
    columns: List<PlayerPdfColumn>,
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
        drawPlayerPdfTextFitted(canvas, column.title, rect, textPaint)
        x += column.width
    }
}

private fun drawPlayerPdfStatsRow(
    canvas: android.graphics.Canvas,
    row: SessionTableRow,
    columns: List<PlayerPdfColumn>,
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
        playerFormatPct(row.tlMade, row.tlAttempted),
        "${row.t2Made}/${row.t2Attempted}",
        playerFormatPct(row.t2Made, row.t2Attempted),
        "${row.t3Made}/${row.t3Attempted}",
        playerFormatPct(row.t3Made, row.t3Attempted),
        "${row.totalMade}/${row.totalAttempted}",
        playerFormatPct(row.totalMade, row.totalAttempted),
        row.rightPct.toPlayerPercentOrDash(),
        row.leftPct.toPlayerPercentOrDash(),
        row.bestSide,
        row.bestZoneT2,
        row.bestZoneT3
    )

    var x = startX
    values.forEachIndexed { index, value ->
        val rect = RectF(x, startY, x + columns[index].width, startY + rowHeight)
        canvas.drawRect(rect, bgPaint)
        canvas.drawRect(rect, borderPaint)

        if (index == 0 && !isTotal) {
            drawPlayerPdfSessionNameFitted(canvas, value, rect, textPaint)
        } else {
            drawPlayerPdfTextFitted(canvas, value, rect, textPaint)
        }

        x += columns[index].width
    }
}

private fun drawPlayerPdfCourtMap(
    canvas: android.graphics.Canvas,
    session: Sessio,
    left: Float,
    top: Float,
    width: Float,
    height: Float,
    tipusPista: String
) {
    if (tipusPista.equals("Pro", ignoreCase = true)) {
        drawPlayerPdfProCourtMap(
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

    val whitePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = android.graphics.Color.WHITE
    }
    val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 3f
        color = android.graphics.Color.BLACK
    }

    fun zonePaint(zone: Int): Paint {
        val (made, attempted) = playerZoneMadeAttempted(session, zone)
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

    val right = left + drawWidth
    val bottom = top + drawHeight

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

    canvas.drawRect(left, top, right, bottom, whitePaint)

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

private fun drawPlayerPdfProCourtMap(
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
        val (made, attempted) = playerZoneMadeAttempted(session, zone)
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

    val layers = playerPdfProZoneLayers(
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

    drawPlayerPdfProCourtOverlay(
        canvas = canvas,
        left = left,
        top = top,
        width = drawWidth,
        height = drawHeight
    )
}

private fun playerPdfProZoneLayers(
    left: Float,
    top: Float,
    width: Float,
    height: Float
): List<PlayerPdfProZoneLayer> {
    val centerX = left + width * 0.5f
    val centerY = top + height * 0.88f

    val innerRadius = width * 0.23f
    val lowRadius = width * 0.36f
    val midRadius = width * 0.52f
    val outerRadius = width * 0.96f

    val cornerWidth = width * 0.12f
    val cornerTop = top + height * 0.48f

    return listOf(
        PlayerPdfProZoneLayer(
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
        PlayerPdfProZoneLayer(
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

        PlayerPdfProZoneLayer(
            zone = 1,
            path = playerPdfAnnularSectorPath(
                centerX = centerX,
                centerY = centerY,
                innerRadius = midRadius,
                outerRadius = outerRadius,
                startAngle = 200f,
                sweepAngle = 50f
            )
        ),
        PlayerPdfProZoneLayer(
            zone = 2,
            path = playerPdfAnnularSectorPath(
                centerX = centerX,
                centerY = centerY,
                innerRadius = midRadius,
                outerRadius = outerRadius,
                startAngle = 250f,
                sweepAngle = 40f
            )
        ),
        PlayerPdfProZoneLayer(
            zone = 3,
            path = playerPdfAnnularSectorPath(
                centerX = centerX,
                centerY = centerY,
                innerRadius = midRadius,
                outerRadius = outerRadius,
                startAngle = 290f,
                sweepAngle = 50f
            )
        ),

        PlayerPdfProZoneLayer(
            zone = 4,
            path = playerPdfAnnularSectorPath(
                centerX = centerX,
                centerY = centerY,
                innerRadius = lowRadius,
                outerRadius = midRadius,
                startAngle = 205f,
                sweepAngle = 65f
            )
        ),
        PlayerPdfProZoneLayer(
            zone = 5,
            path = playerPdfAnnularSectorPath(
                centerX = centerX,
                centerY = centerY,
                innerRadius = lowRadius,
                outerRadius = midRadius,
                startAngle = 270f,
                sweepAngle = 65f
            )
        ),

        PlayerPdfProZoneLayer(
            zone = 7,
            path = playerPdfAnnularSectorPath(
                centerX = centerX,
                centerY = centerY,
                innerRadius = innerRadius,
                outerRadius = lowRadius,
                startAngle = 190f,
                sweepAngle = 55f
            )
        ),
        PlayerPdfProZoneLayer(
            zone = 6,
            path = playerPdfAnnularSectorPath(
                centerX = centerX,
                centerY = centerY,
                innerRadius = innerRadius,
                outerRadius = lowRadius,
                startAngle = 245f,
                sweepAngle = 50f
            )
        ),
        PlayerPdfProZoneLayer(
            zone = 9,
            path = playerPdfAnnularSectorPath(
                centerX = centerX,
                centerY = centerY,
                innerRadius = innerRadius,
                outerRadius = lowRadius,
                startAngle = 295f,
                sweepAngle = 55f
            )
        ),

        PlayerPdfProZoneLayer(
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

private fun playerPdfAnnularSectorPath(
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

private fun drawPlayerPdfProCourtOverlay(
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

private fun playerZoneMadeAttempted(session: Sessio, zone: Int): Pair<Int, Int> {
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