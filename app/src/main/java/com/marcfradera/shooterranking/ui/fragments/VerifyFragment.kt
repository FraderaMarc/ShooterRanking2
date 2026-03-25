package com.marcfradera.shooterranking.ui.fragments

import androidx.compose.runtime.Composable
import androidx.navigation.fragment.findNavController
import com.marcfradera.shooterranking.R
import com.marcfradera.shooterranking.ui.screens.VerifyEmailScreen

class VerifyFragment : BaseComposeFragment() {

    @Composable
    override fun Render() {
        VerifyEmailScreen(
            onContinue = {
                findNavController().navigate(R.id.action_verify_to_temporades)
            },
            onSignOut = {
                findNavController().popBackStack(R.id.welcomeFragment, false)
            }
        )
    }
}