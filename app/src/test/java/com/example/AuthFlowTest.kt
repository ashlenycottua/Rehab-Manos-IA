package com.example

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import com.example.ui.screens.WelcomeScreen
import com.example.ui.theme.RehabManosTheme
import com.example.ui.viewmodel.AuthUiState
import com.example.ui.viewmodel.AuthViewModel
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class AuthFlowTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        if (FirebaseApp.getApps(context).isEmpty()) {
            FirebaseApp.initializeApp(context)
        }
    }

    @Test
    fun `AuthViewModel toggles guest mode bypass cleanly`() {
        val auth = FirebaseAuth.getInstance()
        auth.signOut()
        val viewModel = AuthViewModel(auth = auth)

        assertEquals(AuthUiState.Unauthenticated, viewModel.authState.value)
        assertFalse(viewModel.guestModeBypassed.value)

        viewModel.enterGuestMode()
        assertTrue(viewModel.guestModeBypassed.value)

        viewModel.resetGuestModeBypass()
        assertFalse(viewModel.guestModeBypassed.value)
    }

    @Test
    fun `WelcomeScreen displays Google sign-in button and guest exploration button`() {
        var guestClicked = false
        composeTestRule.setContent {
            RehabManosTheme {
                WelcomeScreen(
                    onAuthSuccess = {},
                    onContinueAsGuest = { guestClicked = true },
                    enableAutoSignInCheck = false
                )
            }
        }

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("welcome_screen_root").assertIsDisplayed()
        composeTestRule.onNodeWithTag("welcome_google_signin_button").assertIsDisplayed()
        composeTestRule.onNodeWithTag("welcome_continue_guest_button").assertIsDisplayed()

        composeTestRule.onNodeWithTag("welcome_continue_guest_button").performClick()
        composeTestRule.waitForIdle()
        assertTrue(guestClicked)
    }
}
