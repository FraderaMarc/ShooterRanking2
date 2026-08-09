package com.marcfradera.shooterranking.legal

import android.content.Context
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.marcfradera.shooterranking.R

object LegalDocuments {
    const val TERMS_VERSION = "1.0"
    const val PRIVACY_VERSION = "1.0"

    fun showLegalMenu(context: Context) {
        val items = arrayOf(
            context.getString(R.string.privacy_policy),
            context.getString(R.string.terms_and_conditions)
        )
        MaterialAlertDialogBuilder(context)
            .setTitle(R.string.legal_information)
            .setItems(items) { _, which ->
                when (which) {
                    0 -> showPrivacyPolicy(context)
                    1 -> showTerms(context)
                }
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    fun showPrivacyPolicy(context: Context) = showDocument(
        context, R.string.privacy_policy, R.string.privacy_policy_body
    )

    fun showTerms(context: Context) = showDocument(
        context, R.string.terms_and_conditions, R.string.terms_conditions_body
    )

    private fun showDocument(context: Context, titleRes: Int, bodyRes: Int) {
        MaterialAlertDialogBuilder(context)
            .setTitle(titleRes)
            .setMessage(context.getString(bodyRes))
            .setPositiveButton(R.string.close, null)
            .show()
    }
}
