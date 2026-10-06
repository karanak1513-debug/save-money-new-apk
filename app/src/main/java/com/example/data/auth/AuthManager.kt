package com.example.data.auth

import android.app.Activity
import android.util.Log
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import com.example.R
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.UserProfileChangeRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

sealed interface AuthState {
  data object Initial : AuthState
  data object Loading : AuthState
  data class Authenticated(val user: FirebaseUser) : AuthState
  data object Unauthenticated : AuthState
  data class Error(val message: String) : AuthState
}

object AuthManager {

  private const val TAG = "AuthManager"
  private val auth: FirebaseAuth = FirebaseAuth.getInstance()

  private val _currentUserState = MutableStateFlow<FirebaseUser?>(auth.currentUser)
  val currentUserState: StateFlow<FirebaseUser?> = _currentUserState.asStateFlow()

  init {
    auth.addAuthStateListener { firebaseAuth ->
      _currentUserState.value = firebaseAuth.currentUser
    }
  }

  val currentUser: FirebaseUser?
    get() = auth.currentUser

  val isAuthenticated: Boolean
    get() = auth.currentUser != null

  /**
   * Google One-Tap Sign-In using Android Jetpack CredentialManager and GetSignInWithGoogleOption.
   */
  fun signInWithGoogle(
    activity: Activity,
    scope: CoroutineScope,
    onResult: (Boolean, String?) -> Unit
  ) {
    scope.launch(Dispatchers.Main) {
      try {
        val serverClientId = activity.getString(R.string.default_web_client_id)
        val credentialManager = CredentialManager.create(activity)

        val googleIdOption = GetSignInWithGoogleOption.Builder(serverClientId)
          .build()

        val request = GetCredentialRequest.Builder()
          .addCredentialOption(googleIdOption)
          .build()

        val result = credentialManager.getCredential(
          request = request,
          context = activity
        )

        val credential = result.credential
        if (credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
          val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
          val idToken = googleIdTokenCredential.idToken

          val authCredential = GoogleAuthProvider.getCredential(idToken, null)
          val authResult = auth.signInWithCredential(authCredential).await()

          if (authResult.user != null) {
            onResult(true, null)
          } else {
            onResult(false, "Authentication failed. No user returned.")
          }
        } else {
          onResult(false, "Unexpected credential type received.")
        }
      } catch (e: GetCredentialCancellationException) {
        Log.w(TAG, "Google Sign-In was cancelled by user.")
        onResult(false, "Sign-in was cancelled.")
      } catch (e: Exception) {
        Log.e(TAG, "Google Sign-In failed", e)
        onResult(false, e.localizedMessage ?: "Google Sign-In error")
      }
    }
  }

  /**
   * Email/Password sign-in or account creation with optional display name.
   */
  fun signInWithEmailPassword(
    email: String,
    pass: String,
    isSignUp: Boolean,
    displayName: String? = null,
    scope: CoroutineScope,
    onResult: (Boolean, String?) -> Unit
  ) {
    val cleanEmail = email.trim()
    val cleanPass = pass.trim()

    if (cleanEmail.isBlank() || !cleanEmail.contains("@") || !cleanEmail.contains(".")) {
      onResult(false, "Please enter a valid email address (e.g., name@example.com).")
      return
    }

    if (cleanPass.length < 6) {
      onResult(false, "Password must be at least 6 characters long.")
      return
    }

    scope.launch(Dispatchers.IO) {
      try {
        val task = if (isSignUp) {
          auth.createUserWithEmailAndPassword(cleanEmail, cleanPass)
        } else {
          auth.signInWithEmailAndPassword(cleanEmail, cleanPass)
        }
        val result = task.await()
        val user = result.user

        if (user != null) {
          if (isSignUp && !displayName.isNullOrBlank()) {
            try {
              val profileUpdates = UserProfileChangeRequest.Builder()
                .setDisplayName(displayName.trim())
                .build()
              user.updateProfile(profileUpdates).await()
            } catch (profileEx: Exception) {
              Log.w(TAG, "Failed to update user profile display name", profileEx)
            }
          }
          onResult(true, null)
        } else {
          onResult(false, "Unable to complete authentication.")
        }
      } catch (e: Exception) {
        Log.e(TAG, "Email/Password auth error", e)
        val readableMessage = when {
          e.message?.contains("The email address is badly formatted", ignoreCase = true) == true ->
            "The email address is incorrectly formatted."
          e.message?.contains("The password is invalid", ignoreCase = true) == true ||
          e.message?.contains("wrong password", ignoreCase = true) == true ||
          e.message?.contains("invalid-credential", ignoreCase = true) == true ->
            "Incorrect password or email combination. Please check your credentials."
          e.message?.contains("email address is already in use", ignoreCase = true) == true ->
            "An account already exists with this email address. Please sign in instead."
          e.message?.contains("no user record corresponding", ignoreCase = true) == true ||
          e.message?.contains("user-not-found", ignoreCase = true) == true ->
            "No account found with this email. Switch to 'Create Account' to register."
          else -> e.localizedMessage ?: "Authentication failed. Please verify your details."
        }
        onResult(false, readableMessage)
      }
    }
  }

  /**
   * Sign out current user.
   */
  fun signOut() {
    auth.signOut()
    _currentUserState.value = null
  }
}
