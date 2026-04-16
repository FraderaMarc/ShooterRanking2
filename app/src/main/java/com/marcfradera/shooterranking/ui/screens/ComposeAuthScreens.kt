package com.marcfradera.shooterranking.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
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
                    .height(55.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = primary
                )
            ) {
                Text("INICIAR SESSIÓ", fontSize = 16.sp)
            }
            Spacer(Modifier.height(16.dp))
            OutlinedButton(
                onClick = onSignup,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(55.dp)
            ) {
                Text("REGISTRE", fontSize = 16.sp)
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

    fun validate(): Boolean {
        if (email.trim().isBlank()) {
            localError = "Introdueix el correu electrònic."
            return false
        }
        if (pass.isBlank()) {
            localError = "Introdueix la contrasenya."
            return false
        }
        localError = null
        return true
    }

    CenteredScaffold(
        title = "Iniciar sessió",
        onBack = onBack,
        showSettings = false
    ) {
        OutlinedTextField(
            value = email,
            onValueChange = { email = it; localError = null },
            label = { Text("Correu electrònic") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = pass,
            onValueChange = { pass = it; localError = null },
            label = { Text("Contrasenya") },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(16.dp))
        Button(
            onClick = { if (validate()) vm.signIn(email.trim(), pass, onLoggedIn) },
            modifier = Modifier.fillMaxWidth(),
            enabled = !vm.loading
        ) {
            if (vm.loading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp
                )
            } else {
                Text("ENTRAR")
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
    var username by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var pass by remember { mutableStateOf("") }
    var pass2 by remember { mutableStateOf("") }
    var localError by remember { mutableStateOf<String?>(null) }

    fun validate(): Boolean {
        if (username.trim().length < 3) {
            localError = "El nom d'usuari ha de tenir mínim 3 caràcters."
            return false
        }
        if (email.trim().isBlank()) {
            localError = "Introdueix el correu electrònic."
            return false
        }
        if (pass.length < 6) {
            localError = "La contrasenya ha de tenir mínim 6 caràcters."
            return false
        }
        if (pass != pass2) {
            localError = "Les contrasenyes no coincideixen."
            return false
        }
        localError = null
        return true
    }

    CenteredScaffold(
        title = "Registre",
        onBack = onBack,
        showSettings = false
    ) {
        OutlinedTextField(
            value = username,
            onValueChange = { username = it; localError = null },
            label = { Text("Nom d'usuari") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = email,
            onValueChange = { email = it; localError = null },
            label = { Text("Correu electrònic") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = pass,
            onValueChange = { pass = it; localError = null },
            label = { Text("Contrasenya") },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = pass2,
            onValueChange = { pass2 = it; localError = null },
            label = { Text("Confirmar contrasenya") },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(16.dp))
        Button(
            onClick = { if (validate()) vm.signUp(email.trim(), pass, username.trim(), onSignedUp) },
            modifier = Modifier.fillMaxWidth(),
            enabled = !vm.loading
        ) {
            if (vm.loading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp
                )
            } else {
                Text("CREAR COMPTE")
            }
        }
        (localError ?: vm.error)?.let {
            Spacer(Modifier.height(12.dp))
            Text(it, color = MaterialTheme.colorScheme.error)
        }
    }
}

@Composable
fun VerifyEmailScreen(onContinue: () -> Unit, onSignOut: () -> Unit) {
    val vm: AuthViewModel = viewModel()
    CenteredScaffold(
        title = "Verificació",
        showSettings = false
    ) {
        Text(
            buildString {
                append("T'hem enviat un correu de verificació")
                if (vm.currentEmail.isNotBlank()) append(" a ${vm.currentEmail}")
                append(".")
            }
        )
        Spacer(Modifier.height(12.dp))
        Button(onClick = onContinue, modifier = Modifier.fillMaxWidth()) {
            Text("JA HE CONFIRMAT")
        }
        Spacer(Modifier.height(8.dp))
        Button(onClick = { vm.resendVerificationEmail() }, modifier = Modifier.fillMaxWidth()) {
            Text("TORNAR A ENVIAR EL CORREU")
        }
        Spacer(Modifier.height(8.dp))
        Button(onClick = onSignOut, modifier = Modifier.fillMaxWidth()) {
            Text("SORTIR")
        }
        vm.error?.let {
            Spacer(Modifier.height(8.dp))
            Text(it, color = MaterialTheme.colorScheme.error)
        }
    }
}
