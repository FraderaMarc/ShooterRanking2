package com.marcfradera.shooterranking

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.marcfradera.shooterranking.data.FirebaseProvider
import com.marcfradera.shooterranking.legal.LegalDocuments
import com.marcfradera.shooterranking.localization.AppLanguageManager
import com.marcfradera.shooterranking.localization.AppSettingsDialogs
import com.marcfradera.shooterranking.ui.screens.SettingsScreen
import com.marcfradera.shooterranking.ui.theme.ShooterRankingTheme
import com.marcfradera.shooterranking.ui.vm.SettingsViewModel

class SettingsActivity : AppCompatActivity() {

    private val settingsViewModel:
            SettingsViewModel by viewModels()

    override fun onCreate(
        savedInstanceState: Bundle?
    ) {
        super.onCreate(savedInstanceState)

        FirebaseProvider.initialize(
            applicationContext
        )

        setContent {
            ShooterRankingTheme {

                SettingsScreen(
                    email =
                        settingsViewModel.currentEmail,

                    loading =
                        settingsViewModel.loading,

                    error =
                        settingsViewModel.error,

                    deletionRequestSent =
                        settingsViewModel
                            .deletionRequestSent,

                    onBack = {
                        finish()
                    },

                    onChangeLanguage = {
                        AppSettingsDialogs
                            .showLanguagePicker(
                                this@SettingsActivity
                            )
                    },

                    onLegalInformation = {
                        LegalDocuments
                            .showLegalMenu(
                                this@SettingsActivity
                            )
                    },

                    onLogout = {
                        settingsViewModel.signOut {
                            restartToMain()
                        }
                    },

                    onRequestAccountDeletion = {
                        settingsViewModel
                            .requestAccountDeletion(
                                AppLanguageManager
                                    .currentLanguageTag(
                                        this@SettingsActivity
                                    )
                            )
                    },

                    onDismissDeletionSent = {
                        settingsViewModel
                            .clearDeletionRequestSent()
                    }
                )
            }
        }
    }

    private fun restartToMain() {

        startActivity(
            Intent(
                this,
                MainActivity::class.java
            ).apply {

                flags =
                    Intent.FLAG_ACTIVITY_NEW_TASK or
                            Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
        )

        finish()
    }
}