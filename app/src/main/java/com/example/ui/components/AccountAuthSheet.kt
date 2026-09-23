package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.repository.AccountManager
import com.example.data.repository.BackupOperationState
import com.example.ui.theme.*
import com.example.viewmodel.MainViewModel
import kotlinx.coroutines.launch

/**
 * Cloud account sheet: sign up, sign in, forgot password, and — once signed in —
 * upload/download of the library to the user's private cloud document.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountAuthSheet(
    viewModel: MainViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val accountState by viewModel.accountState.collectAsState()

    var mode by remember { mutableStateOf(AuthMode.SIGN_IN) }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var displayName by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var errorText by remember { mutableStateOf<String?>(null) }
    var infoText by remember { mutableStateOf<String?>(null) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 22.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(NaturalPrimary),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.CloudQueue, contentDescription = null, tint = NaturalOnPrimary, modifier = Modifier.size(22.dp))
                }
                Column {
                    Text(
                        text = "Cloud Account",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Back up & sync your library across devices",
                        style = MaterialTheme.typography.bodySmall,
                        color = NaturalDarkTextMuted
                    )
                }
            }

            if (accountState.isSignedIn) {
                SignedInContent(
                    viewModel = viewModel,
                    email = accountState.email ?: "",
                    isEmailVerified = accountState.isEmailVerified,
                    onDone = onDismiss
                )
            } else {
                when (mode) {
                    AuthMode.SIGN_IN -> {
                        AuthTextField(
                            value = email,
                            onValueChange = { email = it; errorText = null },
                            label = "Email address",
                            icon = Icons.Default.Email,
                            keyboardType = KeyboardType.Email
                        )
                        AuthPasswordField(password, { password = it; errorText = null }, passwordVisible, { passwordVisible = it })

                        errorText?.let {
                            ErrorBanner(it)
                        }
                        infoText?.let {
                            InfoBanner(it)
                        }

                        AuthButton("Sign In", Icons.Default.Login, busy) {
                            if (email.isBlank() || password.isBlank()) {
                                errorText = "Enter your email and password."
                            } else {
                                busy = true
                                viewModel.signIn(email, password) { result ->
                                    busy = false
                                    when (result) {
                                        is AccountManager.AuthResult.Success -> onDismiss()
                                        is AccountManager.AuthResult.Failure -> errorText = result.message
                                    }
                                }
                            }
                        }

                        TextButton(onClick = { mode = AuthMode.FORGOT; errorText = null; infoText = null }, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                            Text("Forgot password?", color = NaturalPrimary, fontSize = 13.sp)
                        }
                        TextButton(onClick = { mode = AuthMode.SIGN_UP; errorText = null; infoText = null }, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                            Text("New here? Create an account", color = NaturalPrimary, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        }
                    }

                    AuthMode.SIGN_UP -> {
                        AuthTextField(
                            value = displayName,
                            onValueChange = { displayName = it },
                            label = "Name (optional)",
                            icon = Icons.Default.Person,
                            keyboardType = KeyboardType.Text
                        )
                        AuthTextField(
                            value = email,
                            onValueChange = { email = it; errorText = null },
                            label = "Email address",
                            icon = Icons.Default.Email,
                            keyboardType = KeyboardType.Email
                        )
                        AuthPasswordField(password, { password = it; errorText = null }, passwordVisible, { passwordVisible = it })

                        errorText?.let { ErrorBanner(it) }

                        AuthButton("Create Account", Icons.Default.PersonAdd, busy) {
                            when {
                                email.isBlank() || password.isBlank() -> errorText = "Enter an email and password."
                                password.length < 6 -> errorText = "Password must be at least 6 characters."
                                else -> {
                                    busy = true
                                    viewModel.signUp(email, password, displayName) { result ->
                                        busy = false
                                        when (result) {
                                            is AccountManager.AuthResult.Success -> onDismiss()
                                            is AccountManager.AuthResult.Failure -> errorText = result.message
                                        }
                                    }
                                }
                            }
                        }

                        TextButton(onClick = { mode = AuthMode.SIGN_IN; errorText = null }, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                            Text("Already have an account? Sign in", color = NaturalPrimary, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        }
                    }

                    AuthMode.FORGOT -> {
                        InfoBanner("Enter your account email and we'll send a reset link.")
                        AuthTextField(
                            value = email,
                            onValueChange = { email = it; errorText = null },
                            label = "Email address",
                            icon = Icons.Default.Email,
                            keyboardType = KeyboardType.Email
                        )

                        errorText?.let { ErrorBanner(it) }

                        AuthButton("Send Reset Link", Icons.Default.OutgoingMail, busy) {
                            if (email.isBlank()) {
                                errorText = "Enter your email first."
                            } else {
                                busy = true
                                viewModel.sendPasswordReset(email) { result ->
                                    busy = false
                                    when (result) {
                                        is AccountManager.AuthResult.Success -> {
                                            infoText = "Reset link sent — check your inbox."
                                            mode = AuthMode.SIGN_IN
                                        }
                                        is AccountManager.AuthResult.Failure -> errorText = result.message
                                    }
                                }
                            }
                        }

                        TextButton(onClick = { mode = AuthMode.SIGN_IN; errorText = null; infoText = null }, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                            Text("Back to sign in", color = NaturalPrimary, fontSize = 13.sp)
                        }
                    }
                }
            }

            PrivacyNote()
        }
    }
}

@Composable
private fun SignedInContent(viewModel: MainViewModel, email: String, isEmailVerified: Boolean, onDone: () -> Unit) {
    val syncState by viewModel.syncState.collectAsState()
    val syncMessage by viewModel.syncMessage.collectAsState()
    val scope = rememberCoroutineScope()
    var cloudMeta by remember { mutableStateOf<AccountManager.CloudLibraryMeta?>(null) }

    LaunchedEffect(Unit) {
        cloudMeta = viewModel.accountManager.fetchCloudMeta()
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Surface(
            color = NaturalPrimary.copy(alpha = 0.12f),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Icon(Icons.Default.AccountCircle, contentDescription = null, tint = NaturalPrimary, modifier = Modifier.size(30.dp))
                Column {
                    Text(email, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onSurface)
                    Text(
                        text = if (isEmailVerified) "Email verified ✓" else "Verify from the link we emailed you",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isEmailVerified) NaturalSageAccent else NaturalOchreAccent
                    )
                }
            }
        }

        cloudMeta?.let { meta ->
            Surface(color = MaterialTheme.colorScheme.background, shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.History, contentDescription = null, tint = NaturalPrimary, modifier = Modifier.size(16.dp))
                    Text(
                        text = "Cloud copy: ${meta.itemCount} items • from ${meta.deviceName}",
                        style = MaterialTheme.typography.labelSmall,
                        color = NaturalDarkTextMuted
                    )
                }
            }
        }

        val isSyncing = syncState == BackupOperationState.PROCESSING
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            Button(
                onClick = { viewModel.uploadLibraryToCloud() },
                enabled = !isSyncing,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = NaturalPrimary)
            ) {
                Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("Back Up Now", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
            }
            OutlinedButton(
                onClick = { viewModel.downloadLibraryFromCloud() },
                enabled = !isSyncing,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = NaturalPrimary)
            ) {
                Icon(Icons.Default.CloudDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("Restore", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
            }
        }

        if (syncMessage.isNotBlank()) {
            Text(syncMessage, style = MaterialTheme.typography.labelSmall, color = NaturalPrimary)
        }

        OutlinedButton(
            onClick = {
                viewModel.signOutAccount()
                onDone()
            },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
        ) {
            Icon(Icons.Default.Logout, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            Text("Sign Out", fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun AuthTextField(value: String, onValueChange: (String) -> Unit, label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, keyboardType: KeyboardType) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        leadingIcon = { Icon(icon, contentDescription = null, tint = NaturalPrimary, modifier = Modifier.size(18.dp)) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = NaturalPrimary,
            cursorColor = NaturalPrimary,
            focusedLabelColor = NaturalPrimary
        ),
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun AuthPasswordField(value: String, onValueChange: (String) -> Unit, visible: Boolean, onToggle: () -> Unit) {
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
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = NaturalPrimary,
            cursorColor = NaturalPrimary,
            focusedLabelColor = NaturalPrimary
        ),
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun AuthButton(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, busy: Boolean, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        enabled = !busy,
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp),
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(containerColor = NaturalPrimary, contentColor = NaturalOnPrimary)
    ) {
        if (busy) {
            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = NaturalOnPrimary)
        } else {
            Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(label, fontWeight = FontWeight.Bold, fontSize = 15.sp)
        }
    }
}

@Composable
private fun ErrorBanner(message: String) {
    Surface(color = MaterialTheme.colorScheme.error.copy(alpha = 0.12f), shape = RoundedCornerShape(10.dp), modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.padding(10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
            Text(message, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
        }
    }
}

@Composable
private fun InfoBanner(message: String) {
    Surface(color = NaturalPrimary.copy(alpha = 0.12f), shape = RoundedCornerShape(10.dp), modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.padding(10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Info, contentDescription = null, tint = NaturalPrimary, modifier = Modifier.size(16.dp))
            Text(message, style = MaterialTheme.typography.labelSmall, color = NaturalPrimary)
        }
    }
}

@Composable
private fun PrivacyNote() {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Icon(Icons.Default.Shield, contentDescription = null, tint = NaturalDarkTextMuted, modifier = Modifier.size(13.dp))
        Text(
            text = "Your library syncs only to your own private account. No reading data is shared or sold.",
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
            color = NaturalDarkTextMuted
        )
    }
}

private enum class AuthMode { SIGN_IN, SIGN_UP, FORGOT }
