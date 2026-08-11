package com.marcfradera.shooterranking.ui.fragments

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.core.os.bundleOf
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.marcfradera.shooterranking.R
import com.marcfradera.shooterranking.ui.screens.CenteredScaffold
import com.marcfradera.shooterranking.ui.screens.PersonalPlayerSection
import com.marcfradera.shooterranking.ui.screens.PersonalPlayerSetupScreen
import com.marcfradera.shooterranking.ui.screens.PersonalPlayerTabs
import com.marcfradera.shooterranking.ui.screens.PlayerStatsScreen
import com.marcfradera.shooterranking.ui.screens.ShotMapScreen
import com.marcfradera.shooterranking.ui.vm.PersonalPlayerViewModel

class PersonalPlayerFragment : BaseComposeFragment() {

    private val vm by viewModels<PersonalPlayerViewModel>()

    @Composable
    override fun Render() {
        var section by remember { mutableStateOf(PersonalPlayerSection.STATISTICS) }

        LaunchedEffect(Unit) {
            vm.load()
        }

        val state = vm.state
        val profile = state.data

        when {
            state.loading && profile == null -> {
                CenteredScaffold(
                    title = getString(R.string.home_player),
                    onBack = { findNavController().navigateUp() }
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
            }

            profile == null -> {
                PersonalPlayerSetupScreen(
                    onBack = { findNavController().navigateUp() },
                    loading = state.loading,
                    error = state.error,
                    onCreate = { nom, dorsal, posicio, tipusPista ->
                        vm.create(nom, dorsal, posicio, tipusPista)
                    }
                )
            }

            else -> {
                val tabs: @Composable () -> Unit = {
                    PersonalPlayerTabs(
                        selected = section,
                        onStatistics = { section = PersonalPlayerSection.STATISTICS },
                        onMap = { section = PersonalPlayerSection.MAP }
                    )
                }

                when (section) {
                    PersonalPlayerSection.STATISTICS -> {
                        PlayerStatsScreen(
                            idJugador = profile.id_jugador,
                            nomJugador = profile.nom_jugador,
                            onBack = { findNavController().navigateUp() },
                            forcedTipusPista = profile.tipus_pista,
                            topContent = tabs,
                            onOpenTotalShotMap = {
                                findNavController().navigate(
                                    R.id.action_personal_player_to_total_shot_map,
                                    bundleOf(
                                        TotalShotMapFragment.ARG_PLAYER_ID to profile.id_jugador,
                                        TotalShotMapFragment.ARG_PLAYER_NAME to profile.nom_jugador,
                                        TotalShotMapFragment.ARG_COURT_TYPE to profile.tipus_pista
                                    )
                                )
                            }
                        )
                    }

                    PersonalPlayerSection.MAP -> {
                        ShotMapScreen(
                            idEquip = "",
                            initialJugadorId = profile.id_jugador,
                            initialJugadorNom = profile.nom_jugador,
                            onBack = { findNavController().navigateUp() },
                            onJugadorChanged = { _, _ -> },
                            forcedTipusPista = profile.tipus_pista,
                            lockPlayer = true,
                            topContent = tabs
                        )
                    }
                }
            }
        }
    }
}
