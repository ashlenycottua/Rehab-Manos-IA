package com.example.data.repository

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.provider.Settings
import android.util.Log
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.GetCredentialProviderConfigurationException
import androidx.credentials.exceptions.GetCredentialUnsupportedException
import androidx.credentials.exceptions.NoCredentialException
import com.example.R
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.Companion.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.auth
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

private const val AUTH_TAG = "Auth"

enum class GoogleSignInFailureKind {
    NO_ACCOUNT_ON_DEVICE,
    PLAY_SERVICES_OR_CONFIG,
    GENERAL
}

internal fun classifyCredentialException(error: Throwable): Pair<String, GoogleSignInFailureKind> = when (error) {
    is NoCredentialException -> Pair(
        "No encontramos una cuenta de Google configurada en este dispositivo. Agrega tu cuenta en Ajustes y vuelve a intentarlo.",
        GoogleSignInFailureKind.NO_ACCOUNT_ON_DEVICE
    )
    is GetCredentialProviderConfigurationException -> Pair(
        "Actualiza o activa los Servicios de Google Play para entrar con Google en un toque.",
        GoogleSignInFailureKind.PLAY_SERVICES_OR_CONFIG
    )
    is GetCredentialUnsupportedException -> Pair(
        "Este dispositivo no tiene habilitado el selector rápido de Google.",
        GoogleSignInFailureKind.PLAY_SERVICES_OR_CONFIG
    )
    is GetCredentialException -> Pair(
        error.localizedMessage ?: "No se pudo completar el acceso con Google. Verifica tu conexión.",
        GoogleSignInFailureKind.GENERAL
    )
    else -> Pair(
        error.localizedMessage ?: "No se pudo iniciar sesión con Google.",
        GoogleSignInFailureKind.GENERAL
    )
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

internal fun FirebaseAuth.authStateFlow(): Flow<FirebaseUser?> = callbackFlow {
    val listener = FirebaseAuth.AuthStateListener { auth ->
        trySend(auth.currentUser)
    }
    addAuthStateListener(listener)
    awaitClose { removeAuthStateListener(listener) }
}

fun openAddGoogleAccountSettings(context: Context): Boolean {
    val addGoogleIntent = Intent(Settings.ACTION_ADD_ACCOUNT).apply {
        putExtra(Settings.EXTRA_ACCOUNT_TYPES, arrayOf("com.google"))
        if (context.findActivity() == null) {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }
    val syncIntent = Intent(Settings.ACTION_SYNC_SETTINGS).apply {
        if (context.findActivity() == null) {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }
    val generalIntent = Intent(Settings.ACTION_SETTINGS).apply {
        if (context.findActivity() == null) {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }
    for (candidate in listOf(addGoogleIntent, syncIntent, generalIntent)) {
        try {
            context.startActivity(candidate)
            return true
        } catch (e: Exception) {
            Log.w(AUTH_TAG, "Unable to open settings intent ${candidate.action}: ${e.message}")
        }
    }
    return false
}

fun attemptAutoSignIn(
    context: Context,
    credentialManager: CredentialManager,
    onAuthSuccess: () -> Unit,
    onUnauthenticated: () -> Unit,
    scope: CoroutineScope
): Job? {
    if (Firebase.auth.currentUser != null) {
        onAuthSuccess()
        return null
    }
    val clientId = try {
        context.getString(R.string.default_web_client_id)
    } catch (_: Exception) {
        onUnauthenticated()
        return null
    }
    val googleIdOption = GetGoogleIdOption.Builder()
        .setFilterByAuthorizedAccounts(true)
        .setServerClientId(clientId)
        .setAutoSelectEnabled(true)
        .build()
    val request = GetCredentialRequest.Builder()
        .addCredentialOption(googleIdOption)
        .build()

    return scope.launch {
        try {
            val hostContext = context.findActivity() ?: context
            val result = credentialManager.getCredential(hostContext, request)
            val cred = result.credential
            if (cred is CustomCredential && cred.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                val token = GoogleIdTokenCredential.createFrom(cred.data).idToken
                Firebase.auth.signInWithCredential(
                    GoogleAuthProvider.getCredential(token, null)
                ).await()
                onAuthSuccess()
            } else {
                onUnauthenticated()
            }
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            onUnauthenticated()
        }
    }
}

fun onGoogleSignInClicked(
    context: Context,
    credentialManager: CredentialManager,
    onAuthSuccess: () -> Unit,
    onAuthError: (String) -> Unit,
    scope: CoroutineScope,
    onAuthCancelled: () -> Unit = {},
    onAuthErrorDetailed: ((String, GoogleSignInFailureKind) -> Unit)? = null
): Job? {
    fun reportError(message: String, kind: GoogleSignInFailureKind) {
        onAuthErrorDetailed?.invoke(message, kind) ?: onAuthError(message)
    }

    val clientId = try {
        context.getString(R.string.default_web_client_id)
    } catch (_: Exception) {
        reportError(
            "Falta la configuración de Google Sign-In en esta compilación.",
            GoogleSignInFailureKind.PLAY_SERVICES_OR_CONFIG
        )
        return null
    }
    val signInOption = GetSignInWithGoogleOption.Builder(serverClientId = clientId).build()
    val request = GetCredentialRequest.Builder()
        .addCredentialOption(signInOption)
        .build()

    return scope.launch {
        try {
            val hostContext = context.findActivity() ?: context
            val result = credentialManager.getCredential(hostContext, request)
            val cred = result.credential
            if (cred is CustomCredential && cred.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                val token = GoogleIdTokenCredential.createFrom(cred.data).idToken
                Firebase.auth.signInWithCredential(
                    GoogleAuthProvider.getCredential(token, null)
                ).await()
                onAuthSuccess()
            } else {
                reportError(
                    "No se recibió una credencial válida de Google.",
                    GoogleSignInFailureKind.GENERAL
                )
            }
        } catch (e: GetCredentialCancellationException) {
            Log.w(AUTH_TAG, "Google Sign-In cancelled or dismissed: ${e.message}", e)
            onAuthCancelled()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            val (message, kind) = classifyCredentialException(e)
            if (kind == GoogleSignInFailureKind.NO_ACCOUNT_ON_DEVICE) {
                Log.w(AUTH_TAG, "No Google account available on device: ${e.message}", e)
            } else {
                Log.e(AUTH_TAG, "Google Sign-In failed", e)
            }
            reportError(message, kind)
        }
    }
}

fun signOutUser(
    credentialManager: CredentialManager,
    scope: CoroutineScope,
    onSignOutComplete: () -> Unit = {}
) {
    scope.launch {
        withContext(NonCancellable) {
            try {
                credentialManager.clearCredentialState(ClearCredentialStateRequest())
            } catch (e: Exception) {
                Log.w(AUTH_TAG, "Failed to clear credential state: ${e.message}")
            } finally {
                Firebase.auth.signOut()
                onSignOutComplete()
            }
        }
    }
}
