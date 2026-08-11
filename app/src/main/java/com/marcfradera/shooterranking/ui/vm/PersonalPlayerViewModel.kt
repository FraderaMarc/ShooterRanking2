package com.marcfradera.shooterranking.ui.vm

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.marcfradera.shooterranking.data.PersonalPlayerRepository
import com.marcfradera.shooterranking.data.model.PersonalPlayerProfile
import kotlinx.coroutines.launch

class PersonalPlayerViewModel(
    private val repo: PersonalPlayerRepository = PersonalPlayerRepository()
) : ViewModel() {

    var state by mutableStateOf(UiState<PersonalPlayerProfile>(loading = true))
        private set

    fun load() = viewModelScope.launch {
        state = UiState(loading = true)
        try {
            state = UiState(data = repo.load())
        } catch (e: Exception) {
            state = UiState(error = e.message)
        }
    }

    fun create(
        nom: String,
        dorsal: Int,
        posicio: String,
        tipusPista: String
    ) = viewModelScope.launch {
        if (state.loading) return@launch
        state = state.copy(loading = true, error = null)
        try {
            val profile = repo.create(nom, dorsal, posicio, tipusPista)
            state = UiState(data = profile)
        } catch (e: Exception) {
            state = UiState(error = e.message)
        }
    }
}
