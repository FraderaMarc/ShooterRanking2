package com.marcfradera.shooterranking.ui.fragments

import androidx.compose.runtime.Composable
import androidx.core.os.bundleOf
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.marcfradera.shooterranking.R
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
            onBack = { findNavController().navigateUp() },
            onOpenTotalShotMap = {
                findNavController().navigate(
                    R.id.action_player_stats_to_total_shot_map,
                    bundleOf(
                        TotalShotMapFragment.ARG_PLAYER_ID to state.jugadorId,
                        TotalShotMapFragment.ARG_PLAYER_NAME to state.jugadorNom
                    )
                )
            }
        )
    }
}
