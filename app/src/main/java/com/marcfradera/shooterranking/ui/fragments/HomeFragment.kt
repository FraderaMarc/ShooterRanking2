package com.marcfradera.shooterranking.ui.fragments

import androidx.compose.runtime.Composable
import androidx.navigation.fragment.findNavController
import com.marcfradera.shooterranking.R
import com.marcfradera.shooterranking.ui.screens.HomeScreen

class HomeFragment : BaseComposeFragment() {
    @Composable
    override fun Render() {
        HomeScreen(
            onCoach = {
                findNavController().navigate(R.id.action_home_to_temporades)
            },
            onPlayer = {
                findNavController().navigate(R.id.action_home_to_personal_player)
            }
        )
    }
}
