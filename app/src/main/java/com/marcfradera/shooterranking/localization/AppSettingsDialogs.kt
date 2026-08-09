package com.marcfradera.shooterranking.localization

import android.content.Context
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.marcfradera.shooterranking.R
import com.marcfradera.shooterranking.legal.LegalDocuments

object AppSettingsDialogs {

    fun showSettings(context: Context, onLogout: () -> Unit) {
        val items = arrayOf(
            AppLanguageManager.text(context, R.string.change_language),
            AppLanguageManager.text(context, R.string.legal_information),
            AppLanguageManager.text(context, R.string.logout)
        )

        MaterialAlertDialogBuilder(context)
            .setTitle(R.string.settings)
            .setItems(items) { _, which ->
                when (which) {
                    0 -> showLanguagePicker(context)
                    1 -> LegalDocuments.showLegalMenu(context)
                    2 -> onLogout()
                }
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun showLanguagePicker(context: Context) {
        val languages = AppLanguageManager.supportedLanguages
        val names = AppLanguageManager.languageDisplayNames()
        val selectedIndex = AppLanguageManager.selectedLanguageIndex(context)

        MaterialAlertDialogBuilder(context)
            .setTitle(R.string.select_language)
            .setSingleChoiceItems(names, selectedIndex) { dialog, which ->
                val selected = languages.getOrNull(which) ?: return@setSingleChoiceItems
                dialog.dismiss()
                if (selected.tag != AppLanguageManager.currentLanguageTag(context)) {
                    AppLanguageManager.setLanguage(selected.tag)
                }
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }
}
