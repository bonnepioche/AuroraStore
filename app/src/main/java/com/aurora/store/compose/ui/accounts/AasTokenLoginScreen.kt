/*
 * SPDX-FileCopyrightText: 2026 Aurora OSS
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.aurora.store.compose.ui.accounts

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aurora.gplayapi.session.TokenType
import com.aurora.store.R
import com.aurora.store.compose.navigation.Destination
import com.aurora.store.data.model.AuthState
import com.aurora.store.util.Preferences
import com.aurora.store.viewmodel.auth.AuthViewModel

/**
 * Direct AAS-token sign-in. Never store the input in saved UI state or log it.
 * The existing AuthProvider handles session creation and persistence.
 */
@Composable
fun AasTokenLoginScreen(
    onNavigateTo: (Destination) -> Unit,
    addAccount: Boolean = false,
    viewModel: AuthViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val authState by viewModel.authState.collectAsStateWithLifecycle()
    var email by remember { mutableStateOf("") }
    var aasToken by remember { mutableStateOf("") }
    var submitting by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(addAccount) {
        if (addAccount) {
            viewModel.accountAdded.collect { failure ->
                submitting = false
                aasToken = ""
                if (failure == null) {
                    onNavigateTo(Destination.Accounts)
                } else {
                    error = failure
                }
            }
        }
    }

    LaunchedEffect(authState, addAccount) {
        if (!addAccount && submitting) {
            when (val state = authState) {
                AuthState.SignedIn, AuthState.Valid -> {
                    submitting = false
                    aasToken = ""
                    onNavigateTo(
                        Destination.Main(
                            Preferences.getInteger(
                                context,
                                Preferences.PREFERENCE_DEFAULT_SELECTED_TAB
                            )
                        )
                    )
                }
                is AuthState.Failed -> {
                    submitting = false
                    aasToken = ""
                    error = state.status.ifBlank {
                        context.getString(R.string.aas_login_failed)
                    }
                }
                else -> Unit
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.aas_login_title)) }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = stringResource(R.string.aas_login_helper),
                style = MaterialTheme.typography.bodyMedium
            )
            OutlinedTextField(
                value = email,
                onValueChange = { email = it; error = null },
                label = { Text(stringResource(R.string.aas_login_email)) },
                enabled = !submitting,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = aasToken,
                onValueChange = { aasToken = it; error = null },
                label = { Text(stringResource(R.string.aas_login_token)) },
                enabled = !submitting,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            error?.let {
                Text(text = it, color = MaterialTheme.colorScheme.error)
            }
            Button(
                onClick = {
                    val submittedEmail = email.trim()
                    val submittedToken = aasToken.trim()
                    if (!submittedEmail.contains('@') || submittedToken.isEmpty()) {
                        error = context.getString(R.string.aas_login_invalid)
                    } else {
                        submitting = true
                        error = null
                        // Clear plaintext from the input immediately after submission.
                        aasToken = ""
                        if (addAccount) {
                            viewModel.addGoogleAuthData(submittedEmail, submittedToken)
                        } else {
                            viewModel.buildGoogleAuthData(
                                submittedEmail,
                                submittedToken,
                                TokenType.AAS
                            )
                        }
                    }
                },
                enabled = !submitting,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (submitting) {
                    CircularProgressIndicator()
                } else {
                    Text(stringResource(R.string.aas_login_submit))
                }
            }
        }
    }
}
