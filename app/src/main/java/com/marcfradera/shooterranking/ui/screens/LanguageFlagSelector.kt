package com.marcfradera.shooterranking.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.marcfradera.shooterranking.R
import com.marcfradera.shooterranking.localization.AppLanguageManager

@Composable
fun CompactLanguageFlagSelector(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var expanded by remember { mutableStateOf(false) }

    val currentTag =
        AppLanguageManager.currentLanguageTag(context)

    Box(
        modifier = modifier
    ) {
        /*
         * Intentionally subtle: no text, no arrow and no large outlined button.
         * The current flag is the whole affordance.
         */
        Surface(
            modifier = Modifier
                .width(38.dp)
                .height(30.dp),
            shape = MaterialTheme.shapes.small,
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.78f),
            tonalElevation = 1.dp,
            shadowElevation = 0.dp
        ) {
            IconButton(
                onClick = { expanded = true },
                modifier = Modifier.size(38.dp)
            ) {
                LanguageFlag(
                    languageTag = currentTag,
                    modifier = Modifier
                        .width(24.dp)
                        .height(16.dp)
                )
            }
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = {
                expanded = false
            }
        ) {
            AppLanguageManager.supportedLanguages
                .forEach { language ->
                    val description =
                        when (language.tag) {
                            "ca" ->
                                AppLanguageManager.text(
                                    context,
                                    R.string.language_catalan
                                )

                            "en" ->
                                AppLanguageManager.text(
                                    context,
                                    R.string.language_english
                                )

                            "fr" ->
                                AppLanguageManager.text(
                                    context,
                                    R.string.language_french
                                )

                            else ->
                                AppLanguageManager.text(
                                    context,
                                    R.string.language_spanish
                                )
                        }

                    DropdownMenuItem(
                        text = {
                            Box(
                                modifier = Modifier
                                    .width(42.dp)
                                    .semantics {
                                        contentDescription =
                                            description
                                    },
                                contentAlignment =
                                    Alignment.Center
                            ) {
                                LanguageFlag(
                                    languageTag =
                                        language.tag,
                                    modifier = Modifier
                                        .width(28.dp)
                                        .height(18.dp)
                                )
                            }
                        },
                        contentPadding = PaddingValues(
                            horizontal = 12.dp,
                            vertical = 4.dp
                        ),
                        onClick = {
                            expanded = false

                            if (
                                language.tag !=
                                AppLanguageManager
                                    .currentLanguageTag(context)
                            ) {
                                AppLanguageManager
                                    .setLanguage(language.tag)
                            }
                        }
                    )
                }
        }
    }
}

@Composable
private fun LanguageFlag(
    languageTag: String,
    modifier: Modifier
) {
    Canvas(
        modifier = modifier
    ) {
        when (languageTag) {
            "ca" -> {
                // Senyera.
                drawRect(
                    color = Color(0xFFFFD54F)
                )

                val stripeHeight =
                    size.height / 9f

                for (stripe in listOf(1, 3, 5, 7)) {
                    drawRect(
                        color = Color(0xFFD32F2F),
                        topLeft = Offset(
                            0f,
                            stripeHeight * stripe
                        ),
                        size = Size(
                            size.width,
                            stripeHeight
                        )
                    )
                }
            }

            "en" -> {
                // Union Jack simplified at small size.
                val blue = Color(0xFF012169)
                val white = Color.White
                val red = Color(0xFFC8102E)

                drawRect(blue)

                val diagonalWhite =
                    size.height * 0.20f
                val diagonalRed =
                    size.height * 0.09f

                drawLine(
                    white,
                    Offset(0f, 0f),
                    Offset(size.width, size.height),
                    strokeWidth = diagonalWhite
                )
                drawLine(
                    white,
                    Offset(size.width, 0f),
                    Offset(0f, size.height),
                    strokeWidth = diagonalWhite
                )

                drawLine(
                    red,
                    Offset(0f, 0f),
                    Offset(size.width, size.height),
                    strokeWidth = diagonalRed
                )
                drawLine(
                    red,
                    Offset(size.width, 0f),
                    Offset(0f, size.height),
                    strokeWidth = diagonalRed
                )

                drawRect(
                    white,
                    topLeft = Offset(
                        size.width * 0.40f,
                        0f
                    ),
                    size = Size(
                        size.width * 0.20f,
                        size.height
                    )
                )
                drawRect(
                    white,
                    topLeft = Offset(
                        0f,
                        size.height * 0.34f
                    ),
                    size = Size(
                        size.width,
                        size.height * 0.32f
                    )
                )

                drawRect(
                    red,
                    topLeft = Offset(
                        size.width * 0.455f,
                        0f
                    ),
                    size = Size(
                        size.width * 0.09f,
                        size.height
                    )
                )
                drawRect(
                    red,
                    topLeft = Offset(
                        0f,
                        size.height * 0.42f
                    ),
                    size = Size(
                        size.width,
                        size.height * 0.16f
                    )
                )
            }

            "fr" -> {
                val third =
                    size.width / 3f

                drawRect(
                    Color(0xFF0055A4),
                    size = Size(
                        third,
                        size.height
                    )
                )
                drawRect(
                    Color.White,
                    topLeft = Offset(
                        third,
                        0f
                    ),
                    size = Size(
                        third,
                        size.height
                    )
                )
                drawRect(
                    Color(0xFFEF4135),
                    topLeft = Offset(
                        third * 2f,
                        0f
                    ),
                    size = Size(
                        third,
                        size.height
                    )
                )
            }

            else -> {
                // Spain.
                drawRect(
                    Color(0xFFAA151B)
                )

                drawRect(
                    Color(0xFFF1BF00),
                    topLeft = Offset(
                        0f,
                        size.height * 0.25f
                    ),
                    size = Size(
                        size.width,
                        size.height * 0.50f
                    )
                )
            }
        }
    }
}
