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
    private const val PREF_LANGUAGE_TAG = "selected_language_tag_v3"
    private const val LEGACY_PREF_LANGUAGE_TAG = "language_tag"

    @Volatile
    private var applicationContext: Context? = null

    val supportedLanguages: List<SupportedLanguage> = listOf(
        SupportedLanguage("es"),
        SupportedLanguage("ca"),
        SupportedLanguage("en"),
        SupportedLanguage("fr")
    )

    fun initialize(context: Context) {
        applicationContext = context.applicationContext

        val prefs = context.applicationContext
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        val explicitLocale = AppCompatDelegate.getApplicationLocales().get(0)

        if (explicitLocale != null) {
            val explicitTag =
                normalizeSupportedTag(explicitLocale.toLanguageTag())
                    ?: normalizeSupportedTag(explicitLocale.language)
                    ?: DEFAULT_LANGUAGE_TAG

            prefs.edit()
                .putString(PREF_LANGUAGE_TAG, explicitTag)
                .remove(LEGACY_PREF_LANGUAGE_TAG)
                .apply()

            return
        }

        val storedTag =
            prefs.getString(PREF_LANGUAGE_TAG, null)
                ?.let(::normalizeSupportedTag)
                ?: prefs.getString(LEGACY_PREF_LANGUAGE_TAG, null)
                    ?.let(::normalizeSupportedTag)
                ?: DEFAULT_LANGUAGE_TAG

        prefs.edit()
            .putString(PREF_LANGUAGE_TAG, storedTag)
            .remove(LEGACY_PREF_LANGUAGE_TAG)
            .apply()

        AppCompatDelegate.setApplicationLocales(
            LocaleListCompat.forLanguageTags(storedTag)
        )
    }

    fun setLanguage(languageTag: String) {
        val normalized = normalizeSupportedTag(languageTag)
            ?: throw IllegalArgumentException(
                "Unsupported language tag: $languageTag"
            )

        applicationContext
            ?.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            ?.edit()
            ?.putString(PREF_LANGUAGE_TAG, normalized)
            ?.apply()

        AppCompatDelegate.setApplicationLocales(
            LocaleListCompat.forLanguageTags(normalized)
        )
    }

    fun selectedLanguageIndex(context: Context): Int {
        val tag = currentLanguageTag(context)
        return supportedLanguages
            .indexOfFirst { it.tag == tag }
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

        val stored = context.applicationContext
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(PREF_LANGUAGE_TAG, null)

        return stored?.let(::normalizeSupportedTag)
            ?: DEFAULT_LANGUAGE_TAG
    }

    fun currentLanguageTag(): String {
        val context = applicationContext
            ?: error(
                "AppLanguageManager.initialize(context) must be called first."
            )

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
                    if (it.isLowerCase()) {
                        it.titlecase(locale)
                    } else {
                        it.toString()
                    }
                }
            }
        }.toTypedArray()

    fun text(
        @StringRes stringRes: Int,
        vararg args: Any
    ): String {
        val context = applicationContext
            ?: error(
                "AppLanguageManager.initialize(context) must be called first."
            )

        return text(context, stringRes, *args)
    }

    fun text(
        context: Context,
        @StringRes stringRes: Int,
        vararg args: Any
    ): String {
        return localizedContext(context)
            .resources
            .getString(stringRes, *args)
    }

    private fun localizedContext(context: Context): Context {
        val locale =
            AppCompatDelegate.getApplicationLocales().get(0)
                ?: Locale.forLanguageTag(
                    currentLanguageTag(context)
                )

        val configuration =
            Configuration(context.resources.configuration).apply {
                setLocale(locale)
                setLocales(android.os.LocaleList(locale))
            }

        return context.createConfigurationContext(configuration)
    }

    private fun normalizeSupportedTag(tag: String): String? {
        val locale = Locale.forLanguageTag(tag)

        return supportedLanguages
            .firstOrNull { supported ->
                supported.tag.equals(tag, ignoreCase = true) ||
                    Locale.forLanguageTag(supported.tag)
                        .language
                        .equals(locale.language, ignoreCase = true)
            }
            ?.tag
    }
}
