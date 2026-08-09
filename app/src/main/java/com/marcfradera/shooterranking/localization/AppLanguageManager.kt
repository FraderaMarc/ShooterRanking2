package com.marcfradera.shooterranking.localization

import android.content.Context
import android.content.res.Configuration
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import java.util.Locale

data class SupportedLanguage(val tag: String)

object AppLanguageManager {

    private const val DEFAULT_LANGUAGE_TAG = "es"
    private const val PREFS_NAME = "shooter_ranking_language"
    private const val LEGACY_PREF_LANGUAGE_TAG = "language_tag"
    private const val PREF_INITIALIZED = "language_initialized_v2"

    @Volatile
    private var applicationContext: Context? = null

    val supportedLanguages: List<SupportedLanguage> = listOf(
        SupportedLanguage("es"),
        SupportedLanguage("ca"),
        SupportedLanguage("en"),
        SupportedLanguage("fr")
    )

    /**
     * Must be called after Activity.onCreate() when using AppCompat autoStoreLocales.
     * FirebaseProvider.initialize() already does that in MainActivity.
     *
     * First installation: Spanish.
     * Existing installation: preserve an AppCompat/system app locale, or migrate the
     * language_tag used by the previous Shooter Ranking implementation once.
     */
    fun initialize(context: Context) {
        applicationContext = context.applicationContext

        val current = AppCompatDelegate.getApplicationLocales().get(0)
        if (current != null) return

        val prefs = context.applicationContext
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        if (prefs.getBoolean(PREF_INITIALIZED, false)) return

        val legacyTag = prefs.getString(LEGACY_PREF_LANGUAGE_TAG, null)
            ?.let(::normalizeSupportedTag)

        val initialTag = legacyTag ?: DEFAULT_LANGUAGE_TAG

        prefs.edit()
            .putBoolean(PREF_INITIALIZED, true)
            .remove(LEGACY_PREF_LANGUAGE_TAG)
            .apply()

        AppCompatDelegate.setApplicationLocales(
            LocaleListCompat.forLanguageTags(initialTag)
        )
    }

    /**
     * AppCompat recreates the AppCompatActivity when the locale changes, so XML and
     * Compose resources are rebuilt using the selected language.
     */
    fun setLanguage(languageTag: String) {
        val normalized = normalizeSupportedTag(languageTag)
            ?: throw IllegalArgumentException("Unsupported language tag: $languageTag")

        AppCompatDelegate.setApplicationLocales(
            LocaleListCompat.forLanguageTags(normalized)
        )
    }

    fun selectedLanguageIndex(context: Context): Int {
        val tag = currentLanguageTag(context)
        return supportedLanguages.indexOfFirst { it.tag == tag }
            .takeIf { it >= 0 }
            ?: 0
    }

    fun currentLanguageTag(context: Context): String {
        val explicit = AppCompatDelegate.getApplicationLocales().get(0)
        if (explicit != null) {
            return normalizeSupportedTag(explicit.toLanguageTag())
                ?: normalizeSupportedTag(explicit.language)
                ?: DEFAULT_LANGUAGE_TAG
        }

        return DEFAULT_LANGUAGE_TAG
    }

    fun currentLanguageTag(): String {
        val context = applicationContext
            ?: error("AppLanguageManager.initialize(context) must be called first.")
        return currentLanguageTag(context)
    }

    fun languageDisplayNames(): Array<String> =
        supportedLanguages.map { supported ->
            val locale = Locale.forLanguageTag(supported.tag)
            val displayName = locale.getDisplayName(locale)
            if (displayName.isBlank()) {
                supported.tag
            } else {
                displayName.replaceFirstChar {
                    if (it.isLowerCase()) it.titlecase(locale) else it.toString()
                }
            }
        }.toTypedArray()

    fun text(@StringRes stringRes: Int, vararg args: Any): String {
        val context = applicationContext
            ?: error("AppLanguageManager.initialize(context) must be called first.")
        return text(context, stringRes, *args)
    }

    fun text(context: Context, @StringRes stringRes: Int, vararg args: Any): String {
        return localizedContext(context).resources.getString(stringRes, *args)
    }

    /**
     * Non-Activity code (repository/PDF helpers) needs an explicitly localized
     * Configuration context on Android versions where the application context does
     * not automatically reflect the AppCompat locale.
     */
    private fun localizedContext(context: Context): Context {
        val locale = AppCompatDelegate.getApplicationLocales().get(0)
            ?: Locale.forLanguageTag(DEFAULT_LANGUAGE_TAG)

        val configuration = Configuration(context.resources.configuration).apply {
            setLocale(locale)
            setLocales(android.os.LocaleList(locale))
        }
        return context.createConfigurationContext(configuration)
    }

    private fun normalizeSupportedTag(tag: String): String? {
        val locale = Locale.forLanguageTag(tag)
        return supportedLanguages.firstOrNull { supported ->
            supported.tag.equals(tag, ignoreCase = true) ||
                Locale.forLanguageTag(supported.tag).language.equals(locale.language, ignoreCase = true)
        }?.tag
    }
}
