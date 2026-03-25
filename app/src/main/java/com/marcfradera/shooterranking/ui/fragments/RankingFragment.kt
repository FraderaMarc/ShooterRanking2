package com.marcfradera.shooterranking.ui.fragments

import androidx.compose.runtime.Composable
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.marcfradera.shooterranking.R
import com.marcfradera.shooterranking.shared.NavigationSharedViewModel
import com.marcfradera.shooterranking.ui.screens.JugadorsRankingScreen

class RankingFragment : BaseComposeFragment() {

    private val shared by activityViewModels<NavigationSharedViewModel>()

    @Composable
    override fun Render() {
        val state = shared.selection.value ?: return
        val equipId = state.equipId
        if (equipId.isBlank()) return

        JugadorsRankingScreen(
            idEquip = equipId,
            onBack = { requireActivity().onBackPressedDispatcher.onBackPressed() },
            onOpenStats = { jugadorId, nomJugador ->
                shared.setJugador(jugadorId, nomJugador)
                findNavController().navigate(R.id.action_ranking_to_player_stats)
            },
            onOpenShotMap = { jugadorId, nomJugador ->
                shared.setJugador(jugadorId, nomJugador)
                findNavController().navigate(R.id.action_ranking_to_shotmap)
            }
        )
    }
}