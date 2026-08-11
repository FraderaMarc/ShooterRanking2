package com.marcfradera.shooterranking.ui.screens

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.marcfradera.shooterranking.R
import com.marcfradera.shooterranking.localization.AppSettingsDialogs
import com.marcfradera.shooterranking.ui.vm.AuthViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CenteredScaffold(
    title: String? = null,
    onBack: (() -> Unit)? = null,
    titleContent: (@Composable () -> Unit)? = null,
    showSettings: Boolean = true,
    scrollableContent: Boolean = false,
    navigationExtra: (@Composable () -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val context = LocalContext.current
    val authVm: AuthViewModel = viewModel()

    fun restartApp() {
        val launchIntent =
            context.packageManager.getLaunchIntentForPackage(
                context.packageName
            )

        launchIntent?.addFlags(
            Intent.FLAG_ACTIVITY_NEW_TASK or
                Intent.FLAG_ACTIVITY_CLEAR_TASK
        )

        if (launchIntent != null) {
            context.startActivity(launchIntent)
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
                    actionIconContentColor = MaterialTheme.colorScheme.onSurface
                ),
                title = {
                    if (titleContent != null) {
                        titleContent()
                    } else {
                        Text(title.orEmpty())
                    }
                },
                navigationIcon = {
                    if (onBack != null || navigationExtra != null) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (onBack != null) {
                                IconButton(onClick = onBack) {
                                    Icon(
                                        painter = painterResource(R.drawable.ic_back_sr),
                                        contentDescription = stringResource(R.string.back),
                                        modifier = Modifier.size(24.dp),
                                        tint = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }

                            navigationExtra?.invoke()
                        }
                    }
                },
                actions = {
                    if (showSettings) {
                        IconButton(
                            onClick = {
                                AppSettingsDialogs.showSettings(context) {
                                    authVm.signOut {
                                        restartApp()
                                    }
                                }
                            }
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_settings_sr),
                                contentDescription = stringResource(R.string.settings),
                                modifier = Modifier.size(24.dp),
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            )
        }
    ) { padding ->
        val contentModifier =
            Modifier
                .fillMaxSize()
                .background(
                    MaterialTheme.colorScheme.background
                )
                .padding(padding)
                .padding(16.dp)
                .let { base ->
                    if (scrollableContent) {
                        base.verticalScroll(
                            rememberScrollState()
                        )
                    } else {
                        base
                    }
                }

        Column(
            modifier = contentModifier,
            content = content
        )
    }
}
