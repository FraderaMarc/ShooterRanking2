package com.marcfradera.shooterranking.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.marcfradera.shooterranking.R
import com.marcfradera.shooterranking.localization.AppLanguageManager
import com.marcfradera.shooterranking.ui.vm.AuthViewModel

@Composable
fun WelcomeScreen(onLogin: () -> Unit, onSignup: () -> Unit) {
    val background = MaterialTheme.colorScheme.background
    val primary = MaterialTheme.colorScheme.primary
    val onBackground = MaterialTheme.colorScheme.onBackground

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(background),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(110.dp)
                    .background(primary)
            )
            Spacer(Modifier.height(28.dp))
            Text(
                "SHOOTER RANKING",
                fontSize = 30.sp,
                fontWeight = FontWeight.Bold,
                color = onBackground
            )
            Spacer(Modifier.height(40.dp))
            Button(
                onClick = onLogin,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = primary)
            ) {
                Text(
                    stringResource(R.string.login).uppercase(),
                    fontSize = 16.sp,
                    textAlign = TextAlign.Center,
                    maxLines = 2
                )
            }
            Spacer(Modifier.height(16.dp))
            OutlinedButton(
                onClick = onSignup,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                Text(
                    stringResource(R.string.registration).uppercase(),
                    fontSize = 16.sp,
                    textAlign = TextAlign.Center,
                    maxLines = 2
                )
            }
        }
    }
}

@Composable
fun LoginScreen(onBack: () -> Unit, onLoggedIn: () -> Unit) {
    val vm: AuthViewModel = viewModel()
    var email by remember { mutableStateOf("") }
    var pass by remember { mutableStateOf("") }
    var localError by remember { mutableStateOf<String?>(null) }

    val emailRequired = stringResource(R.string.error_email_required)
    val passwordRequired = stringResource(R.string.error_password_required)

    fun validate(): Boolean {
        if (email.trim().isBlank()) {
            localError = emailRequired
            return false
        }
        if (pass.isBlank()) {
            localError = passwordRequired
            return false
        }
        localError = null
        return true
    }

    CenteredScaffold(
        title = stringResource(R.string.login),
        onBack = onBack,
        showSettings = false
    ) {
        OutlinedTextField(
            value = email,
            onValueChange = { email = it; localError = null },
            label = { Text(stringResource(R.string.email)) },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = pass,
            onValueChange = { pass = it; localError = null },
            label = { Text(stringResource(R.string.password)) },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(16.dp))
        Button(
            onClick = { if (validate()) vm.signIn(email.trim(), pass, onLoggedIn) },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            enabled = !vm.loading
        ) {
            if (vm.loading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp
                )
            } else {
                Text(stringResource(R.string.enter).uppercase(), textAlign = TextAlign.Center)
            }
        }
        (localError ?: vm.error)?.let {
            Spacer(Modifier.height(12.dp))
            Text(it, color = MaterialTheme.colorScheme.error)
        }
    }
}

@Composable
fun SignupScreen(onBack: () -> Unit, onSignedUp: () -> Unit) {
    val vm: AuthViewModel = viewModel()
    val context = LocalContext.current
    var username by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var pass by remember { mutableStateOf("") }
    var pass2 by remember { mutableStateOf("") }
    var termsAccepted by remember { mutableStateOf(false) }
    var privacyAcknowledged by remember { mutableStateOf(false) }
    var showTerms by remember { mutableStateOf(false) }
    var showPrivacy by remember { mutableStateOf(false) }
    var localError by remember { mutableStateOf<String?>(null) }

    val usernameMinError = stringResource(R.string.error_username_min_3)
    val emailRequired = stringResource(R.string.error_email_required)
    val passwordMinError = stringResource(R.string.error_password_min_6)
    val passwordMismatch = stringResource(R.string.error_passwords_do_not_match)
    val legalRequired = stringResource(R.string.error_legal_required)

    fun validate(): Boolean {
        if (username.trim().length < 3) { localError = usernameMinError; return false }
        if (email.trim().isBlank()) { localError = emailRequired; return false }
        if (pass.length < 6) { localError = passwordMinError; return false }
        if (pass != pass2) { localError = passwordMismatch; return false }
        if (!termsAccepted || !privacyAcknowledged) { localError = legalRequired; return false }
        localError = null
        return true
    }

    CenteredScaffold(
        title = stringResource(R.string.registration),
        onBack = onBack,
        showSettings = false,
        scrollableContent = true
    ) {
        OutlinedTextField(
            value = username,
            onValueChange = { username = it; localError = null },
            label = { Text(stringResource(R.string.username)) },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = email,
            onValueChange = { email = it; localError = null },
            label = { Text(stringResource(R.string.email)) },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = pass,
            onValueChange = { pass = it; localError = null },
            label = { Text(stringResource(R.string.password)) },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = pass2,
            onValueChange = { pass2 = it; localError = null },
            label = { Text(stringResource(R.string.confirm_password)) },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(16.dp))
        OutlinedButton(
            onClick = { showTerms = true },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(stringResource(R.string.read_terms), textAlign = TextAlign.Center, maxLines = 2)
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(
                checked = termsAccepted,
                onCheckedChange = { termsAccepted = it; localError = null }
            )
            Text(stringResource(R.string.accept_terms_checkbox), modifier = Modifier.weight(1f))
        }

        Spacer(Modifier.height(4.dp))
        OutlinedButton(
            onClick = { showPrivacy = true },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(stringResource(R.string.read_privacy_policy), textAlign = TextAlign.Center, maxLines = 2)
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(
                checked = privacyAcknowledged,
                onCheckedChange = { privacyAcknowledged = it; localError = null }
            )
            Text(stringResource(R.string.privacy_read_checkbox), modifier = Modifier.weight(1f))
        }

        Spacer(Modifier.height(12.dp))
        Button(
            onClick = {
                if (validate()) {
                    vm.signUp(
                        email = email.trim(),
                        password = pass,
                        username = username.trim(),
                        termsAccepted = termsAccepted,
                        privacyAcknowledged = privacyAcknowledged,
                        legalLanguage = AppLanguageManager.currentLanguageTag(context),
                        onDone = onSignedUp
                    )
                }
            },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            enabled = !vm.loading
        ) {
            if (vm.loading) {
                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
            } else {
                Text(stringResource(R.string.create_account).uppercase(), textAlign = TextAlign.Center)
            }
        }
        (localError ?: vm.error)?.let {
            Spacer(Modifier.height(12.dp))
            Text(it, color = MaterialTheme.colorScheme.error)
        }
    }

    if (showTerms) {
        LegalDocumentDialog(
            title = stringResource(R.string.terms_and_conditions),
            body = stringResource(R.string.terms_conditions_body),
            onDismiss = { showTerms = false }
        )
    }
    if (showPrivacy) {
        LegalDocumentDialog(
            title = stringResource(R.string.privacy_policy),
            body = stringResource(R.string.privacy_policy_body),
            onDismiss = { showPrivacy = false }
        )
    }
}

@Composable
private fun LegalDocumentDialog(title: String, body: String, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, fontWeight = FontWeight.Bold) },
        text = {
            Box(
                modifier = Modifier
                    .heightIn(max = 520.dp)
                    .verticalScroll(rememberScrollState())
            ) { Text(body) }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.close)) }
        }
    )
}

@Composable
fun VerifyEmailScreen(onContinue: () -> Unit, onSignOut: () -> Unit) {
    val vm: AuthViewModel = viewModel()
    CenteredScaffold(
        title = stringResource(R.string.verification),
        showSettings = false
    ) {
        Text(
            if (vm.currentEmail.isNotBlank()) {
                stringResource(R.string.verification_email_sent_to, vm.currentEmail)
            } else {
                stringResource(R.string.verification_email_sent)
            }
        )
        Spacer(Modifier.height(12.dp))
        Button(
            onClick = onContinue,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
        ) {
            Text(stringResource(R.string.already_confirmed).uppercase(), textAlign = TextAlign.Center, maxLines = 2)
        }
        Spacer(Modifier.height(8.dp))
        Button(
            onClick = { vm.resendVerificationEmail() },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
        ) {
            Text(stringResource(R.string.resend_verification_email).uppercase(), textAlign = TextAlign.Center, maxLines = 2)
        }
        Spacer(Modifier.height(8.dp))
        Button(
            onClick = onSignOut,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
        ) {
            Text(stringResource(R.string.exit).uppercase(), textAlign = TextAlign.Center)
        }
        vm.error?.let {
            Spacer(Modifier.height(8.dp))
            Text(it, color = MaterialTheme.colorScheme.error)
        }
    }
}
