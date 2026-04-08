package com.marcfradera.shooterranking.ui.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.marcfradera.shooterranking.data.ShooterRepository
import com.marcfradera.shooterranking.data.model.JugadorRankingItem
import com.marcfradera.shooterranking.ui.vm.EquipDeletePreview
import com.marcfradera.shooterranking.ui.vm.EquipUiItem
import com.marcfradera.shooterranking.ui.vm.TemporadaDeletePreview
import com.marcfradera.shooterranking.ui.vm.TemporadaUiItem
import kotlinx.coroutines.launch

data class RecyclerState<T>(
    val loading: Boolean = false,
    val data: List<T> = emptyList(),
    val error: String? = null
)

class TemporadesLiveDataViewModel(
    private val repo: ShooterRepository = ShooterRepository()
) : ViewModel() {

    private val _state = MutableLiveData(RecyclerState<TemporadaUiItem>(loading = true))
    val state: LiveData<RecyclerState<TemporadaUiItem>> = _state

    fun load() = viewModelScope.launch {
        _state.value = RecyclerState(loading = true)
        try {
            _state.value = RecyclerState(data = repo.listTemporadesWithCounts())
        } catch (e: Exception) {
            _state.value = RecyclerState(error = e.message)
        }
    }

    fun create(anyInici: Int, anyFi: Int, onDone: () -> Unit, onError: (String) -> Unit) =
        viewModelScope.launch {
            try {
                repo.createTemporada(anyInici, anyFi)
                load()
                onDone()
            } catch (e: Exception) {
                onError(e.message ?: "No s'ha pogut crear la temporada.")
            }
        }
    fun update(
        idTemporada: String,
        anyInici: Int,
        anyFi: Int,
        onDone: () -> Unit,
        onError: (String) -> Unit
    ) = viewModelScope.launch {
        try {
            repo.updateTemporada(idTemporada, anyInici, anyFi)
            load()
            onDone()
        } catch (e: Exception) {
            onError(e.message ?: "No s'ha pogut actualitzar la temporada.")
        }
    }

    fun loadDeletePreview(
        idTemporada: String,
        onDone: (TemporadaDeletePreview) -> Unit,
        onError: (String) -> Unit
    ) = viewModelScope.launch {
        try {
            onDone(repo.getTemporadaDeletePreview(idTemporada))
        } catch (e: Exception) {
            onError(e.message ?: "No s'ha pogut carregar la previsualització.")
        }
    }

    fun delete(
        idTemporada: String,
        onDone: () -> Unit,
        onError: (String) -> Unit
    ) = viewModelScope.launch {
        try {
            repo.deleteTemporadaCascade(idTemporada)
            load()
            onDone()
        } catch (e: Exception) {
            onError(e.message ?: "No s'ha pogut eliminar la temporada.")
        }
    }
}

class EquipsLiveDataViewModel(
    private val repo: ShooterRepository = ShooterRepository()
) : ViewModel() {

    private val _state = MutableLiveData(RecyclerState<EquipUiItem>(loading = true))
    val state: LiveData<RecyclerState<EquipUiItem>> = _state

    fun load(temporadaId: String) = viewModelScope.launch {
        _state.value = RecyclerState(loading = true)
        try {
            _state.value = RecyclerState(data = repo.listEquipsWithCounts(temporadaId))
        } catch (e: Exception) {
            _state.value = RecyclerState(error = e.message)
        }
    }

    fun create(
        temporadaId: String,
        nomEquip: String,
        onDone: () -> Unit,
        onError: (String) -> Unit
    ) = viewModelScope.launch {
        try {
            repo.createEquip(nomEquip.trim(), temporadaId)
            load(temporadaId)
            onDone()
        } catch (e: Exception) {
            onError(e.message ?: "No s'ha pogut crear l'equip.")
        }
    }
    fun update(
        idEquip: String,
        temporadaId: String,
        nomEquip: String,
        onDone: () -> Unit,
        onError: (String) -> Unit
    ) = viewModelScope.launch {
        try {
            repo.updateEquip(idEquip, nomEquip.trim())
            load(temporadaId)
            onDone()
        } catch (e: Exception) {
            onError(e.message ?: "No s'ha pogut actualitzar l'equip.")
        }
    }

    fun loadDeletePreview(
        idEquip: String,
        onDone: (EquipDeletePreview) -> Unit,
        onError: (String) -> Unit
    ) = viewModelScope.launch {
        try {
            onDone(repo.getEquipDeletePreview(idEquip))
        } catch (e: Exception) {
            onError(e.message ?: "No s'ha pogut carregar la previsualització.")
        }
    }

    fun delete(
        idEquip: String,
        temporadaId: String,
        onDone: () -> Unit,
        onError: (String) -> Unit
    ) = viewModelScope.launch {
        try {
            repo.deleteEquipCascade(idEquip)
            load(temporadaId)
            onDone()
        } catch (e: Exception) {
            onError(e.message ?: "No s'ha pogut eliminar l'equip.")
        }
    }
}

class RankingLiveDataViewModel(
    private val repo: ShooterRepository = ShooterRepository()
) : ViewModel() {

    private val _state = MutableLiveData(RecyclerState<JugadorRankingItem>(loading = true))
    val state: LiveData<RecyclerState<JugadorRankingItem>> = _state

    fun load(equipId: String) = viewModelScope.launch {
        _state.value = RecyclerState(loading = true)

        try {
            val jugadors = repo.listJugadors(equipId)

            val ranked: List<JugadorRankingItem> = jugadors.map { jugador ->
                val sessions = repo.listSessions(jugador.id_jugador)

                val made = sessions.sumOf {
                    it.fets_pos_1 + it.fets_pos_2 + it.fets_pos_3 + it.fets_pos_4 +
                            it.fets_pos_5 + it.fets_pos_6 + it.fets_pos_7 + it.fets_pos_8 +
                            it.fets_pos_9 + it.fets_pos_10 + it.fets_pos_11
                }

                val attempted = sessions.sumOf {
                    it.tirs_pos_1 + it.tirs_pos_2 + it.tirs_pos_3 + it.tirs_pos_4 +
                            it.tirs_pos_5 + it.tirs_pos_6 + it.tirs_pos_7 + it.tirs_pos_8 +
                            it.tirs_pos_9 + it.tirs_pos_10 + it.tirs_pos_11
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

            _state.value = RecyclerState(data = ranked)
        } catch (e: Exception) {
            _state.value = RecyclerState(error = e.message)
        }
    }

    fun createJugador(
        equipId: String,
        nom: String,
        dorsal: Int,
        posicio: String,
        onDone: () -> Unit,
        onError: (String) -> Unit
    ) = viewModelScope.launch {
        try {
            repo.createJugador(
                nom = nom.trim(),
                dorsal = dorsal,
                posicio = posicio.trim(),
                idEquip = equipId
            )
            load(equipId)
            onDone()
        } catch (e: Exception) {
            onError(e.message ?: "No s'ha pogut crear la jugadora.")
        }
    }

    fun loadDeletePreview(
        idJugador: String,
        onDone: (Int) -> Unit,
        onError: (String) -> Unit
    ) = viewModelScope.launch {
        try {
            onDone(repo.getJugadorDeleteSessionsCount(idJugador))
        } catch (e: Exception) {
            onError(e.message ?: "No s'ha pogut carregar la previsualització.")
        }
    }

    fun deleteJugador(
        idJugador: String,
        equipId: String,
        onDone: () -> Unit,
        onError: (String) -> Unit
    ) = viewModelScope.launch {
        try {
            repo.deleteJugadorCascade(idJugador)
            load(equipId)
            onDone()
        } catch (e: Exception) {
            onError(e.message ?: "No s'ha pogut eliminar la jugadora.")
        }
    }
}