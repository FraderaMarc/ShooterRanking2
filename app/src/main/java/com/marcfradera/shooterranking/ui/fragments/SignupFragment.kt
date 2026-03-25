package com.marcfradera.shooterranking.ui.fragments

import androidx.compose.runtime.Composable
import androidx.navigation.fragment.findNavController
import com.marcfradera.shooterranking.R
import com.marcfradera.shooterranking.ui.screens.SignupScreen

class SignupFragment : BaseComposeFragment() {

    @Composable
    override fun Render() {
        SignupScreen(
            onBack = { findNavController().popBackStack() },
            onSignedUp = {
                findNavController().navigate(R.id.action_signup_to_verify)
            }
        )
    }
}