package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Login
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.painterResource
import com.example.R
import com.example.data.repository.AccountManager
import com.example.ui.theme.NaturalDarkBackground
import com.example.ui.theme.NaturalDarkBorder
import com.example.ui.theme.NaturalDarkSurfaceVariant
import com.example.ui.theme.NaturalDarkText
import com.example.ui.theme.NaturalDarkTextMuted
import com.example.ui.theme.NaturalOchreAccent
import com.example.ui.theme.NaturalOnPrimary
import com.example.ui.theme.NaturalPrimary
import com.example.ui.theme.NaturalSageAccent
import com.example.viewmodel.MainViewModel

private enum class LoginMode { SIGN_IN, SIGN_UP, FORGOT }

/**
 * The SecureMind sign-in gate, shown at launch until the reader signs in or explicitly continues
 * without an account. Real Firebase authentication — no simulated sign-in: every failure is shown
 * as the sentence the account layer produced.
 */
@Composable
fun SecureMindLoginScreen(
    viewModel: MainViewModel,
    onDone: () -> Unit,
    modifier: Modifier = Modifier
) {
    var mode by remember { mutableStateOf(LoginMode.SIGN_IN) }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var displayName by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var errorText by remember { mutableStateOf<String?>(null) }
    var infoText by remember { mutableStateOf<String?>(null) }

    val cloudReady = viewModel.cloudAccountsAvailable

    fun finish() {
        viewModel.resolveAuthGate()
        onDone()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(NaturalDarkBackground)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(top = 56.dp, bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            // Mark
            Image(
                painter = painterResource(id = R.drawable.img_app_logo),
                contentDescription = "SecureMind",
                modifier = Modifier
                    .size(84.dp)
                    .clip(RoundedCornerShape(22.dp))
            )
            Spacer(modifier = Modifier.height(18.dp))
            Text(
                text = "SecureMind",
                color = NaturalDarkText,
                fontSize = 27.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = when (mode) {
                    LoginMode.SIGN_IN -> "Your library, private to you. Sign in to continue."
                    LoginMode.SIGN_UP -> "Create an account to back up and sync your library."
                    LoginMode.FORGOT -> "We'll email you a link to reset your password."
                },
                color = NaturalDarkTextMuted,
                fontSize = 13.sp,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(26.dp))

            if (!cloudReady) {
                Surface(
                    color = NaturalOchreAccent.copy(alpha = 0.14f),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(Icons.Default.Info, contentDescription = null, tint = NaturalOchreAccent, modifier = Modifier.size(18.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                "Cloud sign-in isn't available on this build",
                                color = NaturalDarkText,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                            Text(
                                "Firebase isn't configured, so accounts can't be created yet. " +
                                    "SecureMind still works fully offline — continue without an account.",
                                color = NaturalDarkTextMuted,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(18.dp))
            }

            if (cloudReady && mode == LoginMode.SIGN_UP) {
                LoginField(
                    value = displayName,
                    onValueChange = { displayName = it },
                    label = "Name (optional)",
                    icon = Icons.Default.Person,
                    keyboardType = KeyboardType.Text
                )
                Spacer(modifier = Modifier.height(12.dp))
            }

            if (cloudReady) {
                LoginField(
                    value = email,
                    onValueChange = { email = it; errorText = null },
                    label = "Email address",
                    icon = Icons.Default.Email,
                    keyboardType = KeyboardType.Email
                )

                if (mode != LoginMode.FORGOT) {
                    Spacer(modifier = Modifier.height(12.dp))
                    LoginPasswordField(
                        value = password,
                        onValueChange = { password = it; errorText = null },
                        visible = passwordVisible,
                        onToggle = { passwordVisible = !passwordVisible }
                    )
                }

                errorText?.let {
                    Spacer(modifier = Modifier.height(12.dp))
                    LoginBanner(it, isError = true)
                }
                infoText?.let {
                    Spacer(modifier = Modifier.height(12.dp))
                    LoginBanner(it, isError = false)
                }

                Spacer(modifier = Modifier.height(20.dp))

                val label = when (mode) {
                    LoginMode.SIGN_IN -> "Sign In"
                    LoginMode.SIGN_UP -> "Create Account"
                    LoginMode.FORGOT -> "Send Reset Link"
                }
                val icon = when (mode) {
                    LoginMode.SIGN_IN -> Icons.Default.Login
                    LoginMode.SIGN_UP -> Icons.Default.PersonAdd
                    LoginMode.FORGOT -> Icons.Default.Send
                }

                Button(
                    onClick = {
                        when (mode) {
                            LoginMode.SIGN_IN -> {
                                if (email.isBlank() || password.isBlank()) {
                                    errorText = "Enter your email and password."
                                } else {
                                    busy = true
                                    viewModel.signIn(email, password) { result ->
                                        busy = false
                                        when (result) {
                                            is AccountManager.AuthResult.Success -> finish()
                                            is AccountManager.AuthResult.Failure -> errorText = result.message
                                        }
                                    }
                                }
                            }
                            LoginMode.SIGN_UP -> when {
                                email.isBlank() || password.isBlank() -> errorText = "Enter an email and password."
                                password.length < 6 -> errorText = "Password must be at least 6 characters."
                                else -> {
                                    busy = true
                                    viewModel.signUp(email, password, displayName) { result ->
                                        busy = false
                                        when (result) {
                                            is AccountManager.AuthResult.Success -> finish()
                                            is AccountManager.AuthResult.Failure -> errorText = result.message
                                        }
                                    }
                                }
                            }
                            LoginMode.FORGOT -> {
                                if (email.isBlank()) {
                                    errorText = "Enter your email first."
                                } else {
                                    busy = true
                                    viewModel.sendPasswordReset(email) { result ->
                                        busy = false
                                        when (result) {
                                            is AccountManager.AuthResult.Success -> {
                                                infoText = "Reset link sent — check your inbox."
                                                errorText = null
                                                mode = LoginMode.SIGN_IN
                                            }
                                            is AccountManager.AuthResult.Failure -> errorText = result.message
                                        }
                                    }
                                }
                            }
                        }
                    },
                    enabled = !busy,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = NaturalPrimary, contentColor = NaturalOnPrimary)
                ) {
                    if (busy) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = NaturalOnPrimary)
                    } else {
                        Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(label, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }
                }

                when (mode) {
                    LoginMode.SIGN_IN -> {
                        TextButton(onClick = { mode = LoginMode.FORGOT; errorText = null; infoText = null }) {
                            Text("Forgot password?", color = NaturalPrimary, fontSize = 13.sp)
                        }
                        TextButton(onClick = { mode = LoginMode.SIGN_UP; errorText = null; infoText = null }) {
                            Text("New here? Create an account", color = NaturalPrimary, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        }
                    }
                    LoginMode.SIGN_UP -> {
                        TextButton(onClick = { mode = LoginMode.SIGN_IN; errorText = null; infoText = null }) {
                            Text("Already have an account? Sign in", color = NaturalPrimary, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        }
                    }
                    LoginMode.FORGOT -> {
                        TextButton(onClick = { mode = LoginMode.SIGN_IN; errorText = null; infoText = null }) {
                            Text("Back to sign in", color = NaturalPrimary, fontSize = 13.sp)
                        }
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    HorizontalDivider(modifier = Modifier.weight(1f), color = NaturalDarkBorder)
                    Text("or", color = NaturalDarkTextMuted, fontSize = 12.sp)
                    HorizontalDivider(modifier = Modifier.weight(1f), color = NaturalDarkBorder)
                }
            }

            OutlinedButton(
                onClick = { finish() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = NaturalPrimary)
            ) {
                Icon(Icons.Default.Shield, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    if (cloudReady) "Continue without an account" else "Continue offline",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp
                )
            }

            Spacer(modifier = Modifier.height(22.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.Top,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Lock, contentDescription = null, tint = NaturalSageAccent, modifier = Modifier.size(13.dp))
                Text(
                    text = "SecureMind keeps your reading private. Cloud backup only ever writes to your own " +
                        "account document, and on-device analysis never sends a page anywhere.",
                    color = NaturalDarkTextMuted,
                    fontSize = 10.sp,
                    lineHeight = 14.sp
                )
            }
        }
    }
}

@Composable
private fun LoginField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    icon: ImageVector,
    keyboardType: KeyboardType
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        leadingIcon = { Icon(icon, contentDescription = null, tint = NaturalPrimary, modifier = Modifier.size(18.dp)) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        shape = RoundedCornerShape(14.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = NaturalPrimary,
            unfocusedBorderColor = NaturalDarkBorder,
            cursorColor = NaturalPrimary,
            focusedLabelColor = NaturalPrimary,
            unfocusedContainerColor = NaturalDarkSurfaceVariant,
            focusedContainerColor = NaturalDarkSurfaceVariant
        ),
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun LoginPasswordField(
    value: String,
    onValueChange: (String) -> Unit,
    visible: Boolean,
    onToggle: () -> Unit
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text("Password") },
        leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = NaturalPrimary, modifier = Modifier.size(18.dp)) },
        trailingIcon = {
            IconButton(onClick = onToggle) {
                Icon(
                    if (visible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                    contentDescription = if (visible) "Hide password" else "Show password",
                    tint = NaturalDarkTextMuted,
                    modifier = Modifier.size(18.dp)
                )
            }
        },
        visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        shape = RoundedCornerShape(14.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = NaturalPrimary,
            unfocusedBorderColor = NaturalDarkBorder,
            cursorColor = NaturalPrimary,
            focusedLabelColor = NaturalPrimary,
            unfocusedContainerColor = NaturalDarkSurfaceVariant,
            focusedContainerColor = NaturalDarkSurfaceVariant
        ),
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun LoginBanner(message: String, isError: Boolean) {
    val accent = if (isError) MaterialTheme.colorScheme.error else NaturalSageAccent
    Surface(
        color = accent.copy(alpha = 0.12f),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = message,
            color = accent,
            fontSize = 12.sp,
            modifier = Modifier.padding(12.dp)
        )
    }
}
