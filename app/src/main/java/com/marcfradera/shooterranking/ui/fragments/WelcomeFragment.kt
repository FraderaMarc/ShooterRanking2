package com.marcfradera.shooterranking.ui.fragments

import androidx.compose.runtime.Composable
import androidx.navigation.fragment.findNavController
import com.marcfradera.shooterranking.R
import com.marcfradera.shooterranking.ui.screens.WelcomeScreenCustom

class WelcomeFragment : BaseComposeFragment() {

    @Composable
    override fun Render() {
        WelcomeScreenCustom(
            onLogin = {
                findNavController().navigate(R.id.action_welcome_to_login)
            },
            onSignup = {
                findNavController().navigate(R.id.action_welcome_to_signup)
            }
        )
    }
}