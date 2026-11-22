package com.example.ui.screens

import androidx.compose.runtime.Composable

@Composable
fun AuthScreen(
    onAuthSuccess: () -> Unit,
    onContinueAsGuest: (() -> Unit)? = null,
    enableAutoSignInCheck: Boolean = true
) {
    WelcomeScreen(
        onAuthSuccess = onAuthSuccess,
        onContinueAsGuest = onContinueAsGuest,
        enableAutoSignInCheck = enableAutoSignInCheck,
        googleButtonTestTag = "google_sign_in_button"
    )
}
