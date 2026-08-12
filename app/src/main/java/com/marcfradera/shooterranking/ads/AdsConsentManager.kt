package com.marcfradera.shooterranking.ads

import android.app.Activity
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform

/**
 * Centralizes Google UMP consent so ad requests are never made before the
 * current session's consent state has been refreshed.
 */
class AdsConsentManager(private val activity: Activity) {

    private val consentInformation: ConsentInformation =
        UserMessagingPlatform.getConsentInformation(activity)

    fun gatherConsent(onComplete: (canRequestAds: Boolean) -> Unit) {
        val params = ConsentRequestParameters.Builder().build()

        consentInformation.requestConsentInfoUpdate(
            activity,
            params,
            {
                UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) {
                    onComplete(consentInformation.canRequestAds())
                }
            },
            {
                // If refreshing fails, UMP can still expose a previously valid
                // consent state. Never request ads unless canRequestAds() says so.
                onComplete(consentInformation.canRequestAds())
            }
        )
    }

    fun isPrivacyOptionsRequired(): Boolean =
        consentInformation.privacyOptionsRequirementStatus ==
                ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED

    fun showPrivacyOptions(onComplete: (errorMessage: String?) -> Unit) {
        UserMessagingPlatform.showPrivacyOptionsForm(activity) { formError ->
            onComplete(formError?.message)
        }
    }
}