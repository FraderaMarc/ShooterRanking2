package com.marcfradera.shooterranking.ui.fragments

import androidx.compose.runtime.Composable
import androidx.navigation.fragment.findNavController
import com.marcfradera.shooterranking.R
import com.marcfradera.shooterranking.ui.screens.LoginScreen

class LoginFragment : BaseComposeFragment() {

    @Composable
    override fun Render() {
        LoginScreen(
            onBack = { findNavController().popBackStack() },
            onLoggedIn = {
                findNavController().navigate(R.id.action_login_to_temporades)
            }
        )
    }
}