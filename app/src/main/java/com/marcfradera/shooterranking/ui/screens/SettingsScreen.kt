package com.marcfradera.shooterranking.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.marcfradera.shooterranking.R

@Composable
fun SettingsScreen(
    email: String,
    loading: Boolean,
    error: String?,
    deletionRequestSent: Boolean,
    onBack: () -> Unit,
    onChangeLanguage: () -> Unit,
    onLegalInformation: () -> Unit,
    onLogout: () -> Unit,
    onRequestAccountDeletion: () -> Unit,
    onDismissDeletionSent: () -> Unit
) {

    var showDeleteConfirmation
            by remember {
                mutableStateOf(false)
            }

    CenteredScaffold(
        title =
            stringResource(
                R.string.settings
            ),

        onBack =
            onBack,

        showSettings =
            false,

        scrollableContent =
            true
    ) {

        Text(
            text =
                stringResource(
                    R.string
                        .settings_description
                ),

            style =
                MaterialTheme
                    .typography
                    .bodyMedium,

            color =
                MaterialTheme
                    .colorScheme
                    .onSurfaceVariant
        )

        Spacer(
            modifier =
                Modifier.height(
                    28.dp
                )
        )

        SettingsSectionTitle(
            text =
                stringResource(
                    R.string
                        .settings_general_section
                )
        )

        Spacer(
            modifier =
                Modifier.height(
                    10.dp
                )
        )

        SettingsOptionCard(
            title =
                stringResource(
                    R.string
                        .change_language
                ),

            description =
                stringResource(
                    R.string
                        .settings_language_description
                ),

            enabled =
                !loading,

            onClick =
                onChangeLanguage
        )

        Spacer(
            modifier =
                Modifier.height(
                    10.dp
                )
        )

        SettingsOptionCard(
            title =
                stringResource(
                    R.string
                        .legal_information
                ),

            description =
                stringResource(
                    R.string
                        .settings_legal_description
                ),

            enabled =
                !loading,

            onClick =
                onLegalInformation
        )

        Spacer(
            modifier =
                Modifier.height(
                    30.dp
                )
        )

        SettingsSectionTitle(
            text =
                stringResource(
                    R.string
                        .settings_account_section
                )
        )

        Spacer(
            modifier =
                Modifier.height(
                    10.dp
                )
        )

        Surface(
            modifier =
                Modifier.fillMaxWidth(),

            shape =
                RoundedCornerShape(
                    20.dp
                ),

            color =
                MaterialTheme
                    .colorScheme
                    .surfaceVariant,

            tonalElevation =
                2.dp
        ) {

            Column(
                modifier =
                    Modifier.padding(
                        18.dp
                    )
            ) {

                Text(
                    text =
                        stringResource(
                            R.string
                                .settings_current_account
                        ),

                    style =
                        MaterialTheme
                            .typography
                            .labelLarge,

                    color =
                        MaterialTheme
                            .colorScheme
                            .onSurfaceVariant
                )

                Spacer(
                    modifier =
                        Modifier.height(
                            6.dp
                        )
                )

                Text(
                    text =
                        if (
                            email.isNotBlank()
                        ) {
                            email
                        } else {
                            stringResource(
                                R.string
                                    .settings_no_email
                            )
                        },

                    style =
                        MaterialTheme
                            .typography
                            .titleMedium,

                    fontWeight =
                        FontWeight
                            .SemiBold,

                    color =
                        MaterialTheme
                            .colorScheme
                            .onSurface
                )

                Spacer(
                    modifier =
                        Modifier.height(
                            16.dp
                        )
                )

                OutlinedButton(
                    onClick =
                        onLogout,

                    enabled =
                        !loading,

                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .height(
                                54.dp
                            ),

                    shape =
                        RoundedCornerShape(
                            16.dp
                        )
                ) {

                    Text(
                        text =
                            stringResource(
                                R.string.logout
                            ),

                        fontWeight =
                            FontWeight
                                .SemiBold
                    )
                }
            }
        }

        Spacer(
            modifier =
                Modifier.height(
                    30.dp
                )
        )

        SettingsSectionTitle(
            text =
                stringResource(
                    R.string
                        .settings_danger_section
                ),

            error =
                true
        )

        Spacer(
            modifier =
                Modifier.height(
                    10.dp
                )
        )

        Surface(
            modifier =
                Modifier.fillMaxWidth(),

            shape =
                RoundedCornerShape(
                    20.dp
                ),

            color =
                MaterialTheme
                    .colorScheme
                    .error
                    .copy(
                        alpha = 0.08f
                    ),

            border =
                BorderStroke(
                    width =
                        1.dp,

                    color =
                        MaterialTheme
                            .colorScheme
                            .error
                            .copy(
                                alpha =
                                    0.40f
                            )
                )
        ) {

            Column(
                modifier =
                    Modifier.padding(
                        18.dp
                    )
            ) {

                Text(
                    text =
                        stringResource(
                            R.string
                                .delete_account
                        ),

                    style =
                        MaterialTheme
                            .typography
                            .titleMedium,

                    fontWeight =
                        FontWeight.Bold,

                    color =
                        MaterialTheme
                            .colorScheme
                            .error
                )

                Spacer(
                    modifier =
                        Modifier.height(
                            6.dp
                        )
                )

                Text(
                    text =
                        stringResource(
                            R.string
                                .delete_account_description
                        ),

                    style =
                        MaterialTheme
                            .typography
                            .bodyMedium,

                    color =
                        MaterialTheme
                            .colorScheme
                            .onSurfaceVariant
                )

                Spacer(
                    modifier =
                        Modifier.height(
                            8.dp
                        )
                )

                Text(
                    text =
                        stringResource(
                            R.string
                                .delete_account_email_note
                        ),

                    style =
                        MaterialTheme
                            .typography
                            .bodySmall,

                    color =
                        MaterialTheme
                            .colorScheme
                            .onSurfaceVariant
                )

                Spacer(
                    modifier =
                        Modifier.height(
                            18.dp
                        )
                )

                Button(
                    onClick = {
                        showDeleteConfirmation =
                            true
                    },

                    enabled =
                        !loading,

                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .height(
                                54.dp
                            ),

                    shape =
                        RoundedCornerShape(
                            16.dp
                        ),

                    colors =
                        ButtonDefaults
                            .buttonColors(
                                containerColor =
                                    MaterialTheme
                                        .colorScheme
                                        .error,

                                contentColor =
                                    MaterialTheme
                                        .colorScheme
                                        .onError
                            )
                ) {

                    Text(
                        text =
                            stringResource(
                                R.string
                                    .delete_account
                            ),

                        fontWeight =
                            FontWeight.Bold
                    )
                }
            }
        }

        if (loading) {

            Spacer(
                modifier =
                    Modifier.height(
                        22.dp
                    )
            )

            Row(
                modifier =
                    Modifier
                        .fillMaxWidth(),

                horizontalArrangement =
                    Arrangement.Center,

                verticalAlignment =
                    Alignment.CenterVertically
            ) {

                CircularProgressIndicator()
            }
        }

        if (
            !error.isNullOrBlank()
        ) {

            Spacer(
                modifier =
                    Modifier.height(
                        18.dp
                    )
            )

            Surface(
                modifier =
                    Modifier.fillMaxWidth(),

                shape =
                    RoundedCornerShape(
                        16.dp
                    ),

                color =
                    MaterialTheme
                        .colorScheme
                        .error
                        .copy(
                            alpha =
                                0.10f
                        )
            ) {

                Text(
                    text =
                        error,

                    modifier =
                        Modifier.padding(
                            14.dp
                        ),

                    style =
                        MaterialTheme
                            .typography
                            .bodyMedium,

                    textAlign =
                        TextAlign.Start,

                    color =
                        MaterialTheme
                            .colorScheme
                            .error
                )
            }
        }

        Spacer(
            modifier =
                Modifier.height(
                    20.dp
                )
        )
    }

    if (
        showDeleteConfirmation
    ) {

        AlertDialog(
            onDismissRequest = {
                if (!loading) {
                    showDeleteConfirmation =
                        false
                }
            },

            title = {

                Text(
                    text =
                        stringResource(
                            R.string
                                .delete_account_confirm_title
                        )
                )
            },

            text = {

                Text(
                    text =
                        stringResource(
                            R.string
                                .delete_account_confirm_message
                        )
                )
            },

            confirmButton = {

                TextButton(
                    onClick = {

                        showDeleteConfirmation =
                            false

                        onRequestAccountDeletion()
                    },

                    enabled =
                        !loading
                ) {

                    Text(
                        text =
                            stringResource(
                                R.string
                                    .delete_account_send_email
                            ),

                        color =
                            MaterialTheme
                                .colorScheme
                                .error,

                        fontWeight =
                            FontWeight.Bold
                    )
                }
            },

            dismissButton = {

                TextButton(
                    onClick = {
                        showDeleteConfirmation =
                            false
                    },

                    enabled =
                        !loading
                ) {

                    Text(
                        text =
                            stringResource(
                                R.string.cancel
                            )
                    )
                }
            }
        )
    }

    if (
        deletionRequestSent
    ) {

        AlertDialog(
            onDismissRequest =
                onDismissDeletionSent,

            title = {

                Text(
                    text =
                        stringResource(
                            R.string
                                .delete_account_email_sent_title
                        )
                )
            },

            text = {

                Text(
                    text =
                        stringResource(
                            R.string
                                .delete_account_email_sent_message,
                            email
                        )
                )
            },

            confirmButton = {

                TextButton(
                    onClick =
                        onDismissDeletionSent
                ) {

                    Text(
                        text =
                            stringResource(
                                R.string.close
                            )
                    )
                }
            }
        )
    }
}

@Composable
private fun SettingsSectionTitle(
    text: String,
    error: Boolean = false
) {

    Text(
        text =
            text.uppercase(),

        style =
            MaterialTheme
                .typography
                .labelLarge,

        fontWeight =
            FontWeight.Bold,

        color =
            if (error) {
                MaterialTheme
                    .colorScheme
                    .error
            } else {
                MaterialTheme
                    .colorScheme
                    .primary
            }
    )
}

@Composable
private fun SettingsOptionCard(
    title: String,
    description: String,
    enabled: Boolean,
    onClick: () -> Unit
) {

    Surface(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable(
                    enabled =
                        enabled,

                    onClick =
                        onClick
                ),

        shape =
            RoundedCornerShape(
                20.dp
            ),

        color =
            MaterialTheme
                .colorScheme
                .surfaceVariant,

        tonalElevation =
            2.dp
    ) {

        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal =
                            18.dp,

                        vertical =
                            16.dp
                    )
        ) {

            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(
                            end =
                                36.dp
                        )
            ) {

                Text(
                    text =
                        title,

                    style =
                        MaterialTheme
                            .typography
                            .titleMedium,

                    fontWeight =
                        FontWeight
                            .SemiBold,

                    color =
                        MaterialTheme
                            .colorScheme
                            .onSurface
                )

                Spacer(
                    modifier =
                        Modifier.height(
                            4.dp
                        )
                )

                Text(
                    text =
                        description,

                    style =
                        MaterialTheme
                            .typography
                            .bodySmall,

                    color =
                        MaterialTheme
                            .colorScheme
                            .onSurfaceVariant
                )
            }

            Text(
                text =
                    "›",

                modifier =
                    Modifier.align(
                        Alignment.CenterEnd
                    ),

                style =
                    MaterialTheme
                        .typography
                        .headlineSmall,

                color =
                    MaterialTheme
                        .colorScheme
                        .primary
            )
        }
    }
}