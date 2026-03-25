package com.marcfradera.shooterranking.shared

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

data class NavigationSelection(
    val temporadaId: String = "",
    val temporadaLabel: String = "",
    val equipId: String = "",
    val equipNom: String = "",
    val jugadorId: String = "",
    val jugadorNom: String = ""
)

class NavigationSharedViewModel : ViewModel() {

    private val _selection = MutableLiveData(NavigationSelection())
    val selection: LiveData<NavigationSelection> = _selection

    fun setTemporada(id: String, label: String) {
        val current = _selection.value ?: NavigationSelection()
        _selection.value = current.copy(
            temporadaId = id,
            temporadaLabel = label,
            equipId = "",
            equipNom = "",
            jugadorId = "",
            jugadorNom = ""
        )
    }

    fun setEquip(id: String, nom: String) {
        val current = _selection.value ?: NavigationSelection()
        _selection.value = current.copy(
            equipId = id,
            equipNom = nom,
            jugadorId = "",
            jugadorNom = ""
        )
    }

    fun setJugador(id: String, nom: String) {
        val current = _selection.value ?: NavigationSelection()
        _selection.value = current.copy(
            jugadorId = id,
            jugadorNom = nom
        )
    }
}