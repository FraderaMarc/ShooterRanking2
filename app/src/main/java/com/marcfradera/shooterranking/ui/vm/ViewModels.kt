package com.marcfradera.shooterranking.ui.vm

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.marcfradera.shooterranking.data.ShooterRepository
import com.marcfradera.shooterranking.data.model.Equip
import com.marcfradera.shooterranking.data.model.Jugador
import com.marcfradera.shooterranking.data.model.JugadorRankingItem
import com.marcfradera.shooterranking.data.model.Sessio
import com.marcfradera.shooterranking.data.model.Temporada
import com.marcfradera.shooterranking.data.model.ZoneAgg
import kotlinx.coroutines.launch

data class UiState<T>(
    val loading: Boolean = false,
    val data: T? = null,
    val error: String? = null
)

data class TemporadaUiItem(
    val temporada: Temporada,
    val equipsCount: Int
)

data class EquipUiItem(
    val equip: Equip,
    val jugadorsCount: Int
)

data class TemporadaDeletePreview(
    val equips: List<Equip> = emptyList(),
    val jugadorsCount: Int = 0,
    val sessionsCount: Int = 0
)

data class EquipDeletePreview(
    val jugadors: List<Jugador> = emptyList(),
    val sessionsCount: Int = 0
)

data class JugadorSessionsExport(
    val jugador: Jugador,
    val sessions: List<Sessio>
)

class AuthViewModel(
    private val repo: ShooterRepository = ShooterRepository()
) : ViewModel() {

    var isLoggedIn by mutableStateOf(repo.isLoggedIn())
        private set

    var emailConfirmed by mutableStateOf(repo.isEmailConfirmed())
        private set

    var currentEmail by mutableStateOf(repo.currentUserEmail())
        private set

    var error by mutableStateOf<String?>(null)
        private set

    var loading by mutableStateOf(false)
        private set

    suspend fun refreshAuthStateSuspend() {
        try {
            repo.reloadCurrentUser()
        } catch (e: Exception) {
            error = e.message
        }

        isLoggedIn = repo.isLoggedIn()
        emailConfirmed = repo.isEmailConfirmed()
        currentEmail = repo.currentUserEmail()
    }

    fun refreshAuthState(onDone: (() -> Unit)? = null) = viewModelScope.launch {
        refreshAuthStateSuspend()
        onDone?.invoke()
    }

    fun signIn(email: String, password: String, onDone: () -> Unit) = viewModelScope.launch {
        error = null
        loading = true
        try {
            repo.signIn(email, password)
            refreshAuthStateSuspend()
            onDone()
        } catch (e: Exception) {
            error = e.message
        } finally {
            loading = false
        }
    }

    fun signUp(email: String, password: String, username: String, onDone: () -> Unit) = viewModelScope.launch {
        error = null
        loading = true
        try {
            repo.signUp(email, password, username)
            refreshAuthStateSuspend()
            onDone()
        } catch (e: Exception) {
            error = e.message
        } finally {
            loading = false
        }
    }

    fun resendVerificationEmail(onDone: (() -> Unit)? = null) = viewModelScope.launch {
        error = null
        loading = true
        try {
            repo.resendVerificationEmail()
            onDone?.invoke()
        } catch (e: Exception) {
            error = e.message
        } finally {
            loading = false
        }
    }

    fun signOut(onDone: () -> Unit) = viewModelScope.launch {
        error = null
        loading = true
        try {
            repo.signOut()
            isLoggedIn = false
            emailConfirmed = false
            currentEmail = ""
            onDone()
        } catch (e: Exception) {
            error = e.message
        } finally {
            loading = false
        }
    }
}

class TemporadesViewModel(
    private val repo: ShooterRepository = ShooterRepository()
) : ViewModel() {

    var state by mutableStateOf(UiState<List<TemporadaUiItem>>(loading = true))
        private set

    var deletePreview by mutableStateOf<TemporadaDeletePreview?>(null)
        private set

    fun clearDeletePreview() {
        deletePreview = null
    }

    fun load() = viewModelScope.launch {
        state = UiState(loading = true)
        try {
            state = UiState(data = repo.listTemporadesWithCounts())
        } catch (e: Exception) {
            state = UiState(error = e.message)
        }
    }

    fun create(anyInici: Int, anyFi: Int, onDone: () -> Unit) = viewModelScope.launch {
        try {
            repo.createTemporada(anyInici, anyFi)
            load()
            onDone()
        } catch (e: Exception) {
            state = state.copy(error = e.message)
        }
    }

    fun update(idTemporada: String, anyInici: Int, anyFi: Int, onDone: () -> Unit) = viewModelScope.launch {
        try {
            repo.updateTemporada(idTemporada, anyInici, anyFi)
            load()
            onDone()
        } catch (e: Exception) {
            state = state.copy(error = e.message)
        }
    }

    fun loadDeletePreview(idTemporada: String) = viewModelScope.launch {
        deletePreview = null
        try {
            deletePreview = repo.getTemporadaDeletePreview(idTemporada)
        } catch (_: Exception) {
        }
    }

    fun delete(idTemporada: String, onDone: () -> Unit) = viewModelScope.launch {
        try {
            repo.deleteTemporadaCascade(idTemporada)
            load()
            onDone()
        } catch (e: Exception) {
            state = state.copy(error = e.message)
        }
    }
}

class EquipsViewModel(
    private val repo: ShooterRepository = ShooterRepository()
) : ViewModel() {

    var state by mutableStateOf(UiState<List<EquipUiItem>>(loading = true))
        private set

    var deletePreview by mutableStateOf<EquipDeletePreview?>(null)
        private set

    fun clearDeletePreview() {
        deletePreview = null
    }

    fun load(idTemporada: String) = viewModelScope.launch {
        state = UiState(loading = true)
        try {
            state = UiState(data = repo.listEquipsWithCounts(idTemporada))
        } catch (e: Exception) {
            state = UiState(error = e.message)
        }
    }

    fun create(nom: String, idTemporada: String, onDone: () -> Unit) = viewModelScope.launch {
        try {
            repo.createEquip(nom, idTemporada)
            load(idTemporada)
            onDone()
        } catch (e: Exception) {
            state = state.copy(error = e.message)
        }
    }

    fun update(idEquip: String, nom: String, idTemporada: String, onDone: () -> Unit) = viewModelScope.launch {
        try {
            repo.updateEquip(idEquip, nom)
            load(idTemporada)
            onDone()
        } catch (e: Exception) {
            state = state.copy(error = e.message)
        }
    }

    fun loadDeletePreview(idEquip: String) = viewModelScope.launch {
        deletePreview = null
        try {
            deletePreview = repo.getEquipDeletePreview(idEquip)
        } catch (_: Exception) {
        }
    }

    fun delete(idEquip: String, idTemporada: String, onDone: () -> Unit) = viewModelScope.launch {
        try {
            repo.deleteEquipCascade(idEquip)
            load(idTemporada)
            onDone()
        } catch (e: Exception) {
            state = state.copy(error = e.message)
        }
    }
}

class JugadorsViewModel(
    private val repo: ShooterRepository = ShooterRepository()
) : ViewModel() {

    var state by mutableStateOf(UiState<List<Jugador>>(loading = true))
        private set

    var ranking by mutableStateOf(UiState<List<JugadorRankingItem>>(loading = true))
        private set

    fun load(idEquip: String, filter: String = "TOTAL") = viewModelScope.launch {
        state = UiState(loading = true)
        ranking = UiState(loading = true)

        try {
            val jugadors = repo.listJugadors(idEquip)
            state = UiState(data = jugadors)

            val ranked: List<JugadorRankingItem> = jugadors.map { jugador ->
                val sessions = repo.listSessions(jugador.id_jugador)

                val made = when (filter) {
                    "FREE_THROW" -> sessions.sumOf { it.fets_pos_6 }
                    "THREE_PT" -> sessions.sumOf {
                        it.fets_pos_1 + it.fets_pos_2 + it.fets_pos_3 +
                                it.fets_pos_10 + it.fets_pos_11
                    }
                    "TWO_PT" -> sessions.sumOf {
                        it.fets_pos_4 + it.fets_pos_5 +
                                it.fets_pos_7 + it.fets_pos_8 + it.fets_pos_9
                    }
                    else -> sessions.sumOf {
                        it.fets_pos_1 + it.fets_pos_2 + it.fets_pos_3 + it.fets_pos_4 +
                                it.fets_pos_5 + it.fets_pos_6 + it.fets_pos_7 + it.fets_pos_8 +
                                it.fets_pos_9 + it.fets_pos_10 + it.fets_pos_11
                    }
                }

                val attempted = when (filter) {
                    "FREE_THROW" -> sessions.sumOf { it.tirs_pos_6 }
                    "THREE_PT" -> sessions.sumOf {
                        it.tirs_pos_1 + it.tirs_pos_2 + it.tirs_pos_3 +
                                it.tirs_pos_10 + it.tirs_pos_11
                    }
                    "TWO_PT" -> sessions.sumOf {
                        it.tirs_pos_4 + it.tirs_pos_5 +
                                it.tirs_pos_7 + it.tirs_pos_8 + it.tirs_pos_9
                    }
                    else -> sessions.sumOf {
                        it.tirs_pos_1 + it.tirs_pos_2 + it.tirs_pos_3 + it.tirs_pos_4 +
                                it.tirs_pos_5 + it.tirs_pos_6 + it.tirs_pos_7 + it.tirs_pos_8 +
                                it.tirs_pos_9 + it.tirs_pos_10 + it.tirs_pos_11
                    }
                }

                val pct = if (attempted == 0) 0.0 else made.toDouble() / attempted.toDouble()

                JugadorRankingItem(
                    jugador = jugador,
                    sessions = sessions.size,
                    made = made,
                    attempted = attempted,
                    pct = pct
                )
            }.sortedWith(
                compareByDescending<JugadorRankingItem> { it.pct }
                    .thenByDescending { it.made }
                    .thenBy { it.jugador.nom_jugador.lowercase() }
            )

            ranking = UiState(data = ranked)
        } catch (e: Exception) {
            state = UiState(error = e.message)
            ranking = UiState(error = e.message)
        }
    }

    suspend fun loadPlayersForExport(idEquip: String): List<JugadorSessionsExport> {
        val jugadors = repo.listJugadors(idEquip)
        return jugadors.map { jugador ->
            JugadorSessionsExport(
                jugador = jugador,
                sessions = repo.listSessions(jugador.id_jugador).sortedBy { it.num_sessio }
            )
        }
    }

    fun create(
        nom: String,
        dorsal: Int,
        posicio: String,
        idEquip: String,
        onDone: () -> Unit
    ) = viewModelScope.launch {
        try {
            repo.createJugador(nom, dorsal, posicio, idEquip)
            load(idEquip)
            onDone()
        } catch (e: Exception) {
            state = state.copy(error = e.message)
        }
    }

    fun update(
        idJugador: String,
        nom: String,
        dorsal: Int,
        posicio: String,
        idEquip: String,
        filter: String,
        onDone: () -> Unit
    ) = viewModelScope.launch {
        try {
            repo.updateJugador(idJugador, nom, dorsal, posicio)
            load(idEquip, filter)
            onDone()
        } catch (e: Exception) {
            ranking = ranking.copy(error = e.message)
        }
    }

    fun delete(
        idJugador: String,
        idEquip: String,
        filter: String,
        onDone: () -> Unit
    ) = viewModelScope.launch {
        try {
            repo.deleteJugadorCascade(idJugador)
            load(idEquip, filter)
            onDone()
        } catch (e: Exception) {
            ranking = ranking.copy(error = e.message)
        }
    }
}

class StatsViewModel(
    private val repo: ShooterRepository = ShooterRepository()
) : ViewModel() {

    var zones by mutableStateOf(UiState<List<ZoneAgg>>(loading = true))
        private set

    var totalPct by mutableStateOf(UiState(loading = true, data = 0.0))
        private set

    fun load(idJugador: String) = viewModelScope.launch {
        zones = UiState(loading = true)
        totalPct = UiState(loading = true)

        try {
            zones = UiState(data = repo.aggregateZones(idJugador))
            totalPct = UiState(data = repo.totalPct(idJugador))
        } catch (e: Exception) {
            zones = UiState(error = e.message)
            totalPct = UiState(error = e.message)
        }
    }
}

class ShotSessionViewModel(
    private val repo: ShooterRepository = ShooterRepository()
) : ViewModel() {

    var sessions by mutableStateOf(UiState<List<Sessio>>(loading = true))
        private set

    var draft by mutableStateOf<Sessio?>(null)
        set

    var error by mutableStateOf<String?>(null)
        private set

    fun load(idJugador: String) = viewModelScope.launch {
        sessions = UiState(loading = true)
        sessions = UiState(data = repo.listSessions(idJugador))
    }

    fun startNew(idJugador: String) = viewModelScope.launch {
        val n = repo.nextSessionNumber(idJugador)
        draft = Sessio(num_sessio = n, id_jugador = idJugador)
    }

    private fun findSession(num: Int?) =
        sessions.data?.firstOrNull { it.num_sessio == num }

    fun setZoneForCurrentSession(
        zone: Int,
        made: Int,
        attempted: Int,
        editing: Int?,
        idJugador: String
    ) {
        val base = draft ?: findSession(editing) ?: return

        draft = when (zone) {
            1 -> base.copy(fets_pos_1 = made, tirs_pos_1 = attempted)
            2 -> base.copy(fets_pos_2 = made, tirs_pos_2 = attempted)
            3 -> base.copy(fets_pos_3 = made, tirs_pos_3 = attempted)
            4 -> base.copy(fets_pos_4 = made, tirs_pos_4 = attempted)
            5 -> base.copy(fets_pos_5 = made, tirs_pos_5 = attempted)
            6 -> base.copy(fets_pos_6 = made, tirs_pos_6 = attempted)
            7 -> base.copy(fets_pos_7 = made, tirs_pos_7 = attempted)
            8 -> base.copy(fets_pos_8 = made, tirs_pos_8 = attempted)
            9 -> base.copy(fets_pos_9 = made, tirs_pos_9 = attempted)
            10 -> base.copy(fets_pos_10 = made, tirs_pos_10 = attempted)
            11 -> base.copy(fets_pos_11 = made, tirs_pos_11 = attempted)
            else -> base
        }
    }

    fun saveCurrentSession(
        idJugador: String,
        editing: Int?,
        onDone: (Int) -> Unit
    ) = viewModelScope.launch {

        val session = draft ?: return@launch

        if (editing == null) {
            repo.createSession(session)
        } else {
            repo.updateSession(session)
        }

        onDone(session.num_sessio)
    }
}