package com.example

import androidx.credentials.exceptions.GetCredentialInterruptedException
import androidx.credentials.exceptions.GetCredentialProviderConfigurationException
import androidx.credentials.exceptions.GetCredentialUnsupportedException
import androidx.credentials.exceptions.NoCredentialException
import com.example.data.repository.GoogleSignInFailureKind
import com.example.data.repository.classifyCredentialException
import com.example.ui.viewmodel.AuthUiState
import com.example.ui.viewmodel.toAuthUiState
import com.google.firebase.auth.FirebaseUser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AuthFlowUnitTest {

    @Test
    fun `classifyCredentialException maps NoCredentialException to NO_ACCOUNT_ON_DEVICE`() {
        val (message, kind) = classifyCredentialException(NoCredentialException("no accounts"))
        assertEquals(GoogleSignInFailureKind.NO_ACCOUNT_ON_DEVICE, kind)
        assertTrue(message.contains("cuenta de Google", ignoreCase = true))
    }

    @Test
    fun `classifyCredentialException maps provider configuration and unsupported errors`() {
        val (_, configKind) = classifyCredentialException(
            GetCredentialProviderConfigurationException("missing play services")
        )
        val (_, unsupportedKind) = classifyCredentialException(
            GetCredentialUnsupportedException("unsupported")
        )
        assertEquals(GoogleSignInFailureKind.PLAY_SERVICES_OR_CONFIG, configKind)
        assertEquals(GoogleSignInFailureKind.PLAY_SERVICES_OR_CONFIG, unsupportedKind)
    }

    @Test
    fun `classifyCredentialException maps generic credential errors cleanly`() {
        val (_, kind) = classifyCredentialException(GetCredentialInterruptedException("interrupted"))
        assertEquals(GoogleSignInFailureKind.GENERAL, kind)
    }

    @Test
    fun `null FirebaseUser maps to Unauthenticated AuthUiState`() {
        val user: FirebaseUser? = null
        assertEquals(AuthUiState.Unauthenticated, user.toAuthUiState())
    }
}
