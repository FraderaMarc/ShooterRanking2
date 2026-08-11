package com.marcfradera.shooterranking.ui.fragments

import androidx.compose.runtime.Composable
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.marcfradera.shooterranking.shared.NavigationSharedViewModel
import com.marcfradera.shooterranking.ui.screens.TotalShotMapScreen

class TotalShotMapFragment : BaseComposeFragment() {

    private val shared by activityViewModels<NavigationSharedViewModel>()

    @Composable
    override fun Render() {
        val sharedState = shared.selection.value

        val playerId = arguments
            ?.getString(ARG_PLAYER_ID)
            .orEmpty()
            .ifBlank {
                sharedState
                    ?.jugadorId
                    .orEmpty()
            }

        val playerName = arguments
            ?.getString(ARG_PLAYER_NAME)
            .orEmpty()
            .ifBlank {
                sharedState
                    ?.jugadorNom
                    .orEmpty()
            }

        val forcedCourtType = arguments
            ?.getString(ARG_COURT_TYPE)
            ?.takeIf { it.isNotBlank() }

        if (playerId.isBlank()) return

        TotalShotMapScreen(
            idJugador = playerId,
            nomJugador = playerName,
            onBack = {
                findNavController().navigateUp()
            },
            forcedTipusPista = forcedCourtType
        )
    }

    companion object {
        const val ARG_PLAYER_ID = "total_shot_player_id"
        const val ARG_PLAYER_NAME = "total_shot_player_name"
        const val ARG_COURT_TYPE = "total_shot_court_type"
    }
}
