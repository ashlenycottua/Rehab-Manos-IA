package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.repository.authStateFlow
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

sealed interface AuthUiState {
    data object Unauthenticated : AuthUiState
    data class Authenticated(
        val uid: String,
        val displayName: String,
        val email: String?
    ) : AuthUiState
}

internal fun FirebaseUser?.toAuthUiState(): AuthUiState = if (this == null) {
    AuthUiState.Unauthenticated
} else {
    AuthUiState.Authenticated(
        uid = uid,
        displayName = displayName.orEmpty(),
        email = email
    )
}

class AuthViewModel(
    auth: FirebaseAuth = Firebase.auth
) : ViewModel() {

    val authState: StateFlow<AuthUiState> = auth.authStateFlow()
        .map { it.toAuthUiState() }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = auth.currentUser.toAuthUiState()
        )

    private val _guestModeBypassed = MutableStateFlow(false)
    val guestModeBypassed: StateFlow<Boolean> = _guestModeBypassed.asStateFlow()

    fun enterGuestMode() {
        _guestModeBypassed.value = true
    }

    fun resetGuestModeBypass() {
        _guestModeBypassed.value = false
    }
}
