package com.marcfradera.shooterranking.ui.fragments

import androidx.compose.runtime.Composable
import androidx.fragment.app.activityViewModels
import com.marcfradera.shooterranking.shared.NavigationSharedViewModel
import com.marcfradera.shooterranking.ui.screens.PlayerStatsScreen

class PlayerStatsFragment : BaseComposeFragment() {

    private val shared by activityViewModels<NavigationSharedViewModel>()

    @Composable
    override fun Render() {
        val state = shared.selection.value ?: return

        PlayerStatsScreen(
            idJugador = state.jugadorId,
            nomJugador = state.jugadorNom,
            onBack = { requireActivity().onBackPressedDispatcher.onBackPressed() }
        )
    }
}