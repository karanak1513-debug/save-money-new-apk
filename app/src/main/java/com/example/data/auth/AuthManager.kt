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

  private fun getFirebaseAuth(): FirebaseAuth? {
    return try {
      FirebaseAuth.getInstance()
    } catch (e: Throwable) {
      Log.w(TAG, "FirebaseAuth not ready or offline: ${e.message}")
      null
    }
  }

  private val _currentUserState = MutableStateFlow<FirebaseUser?>(null)
  val currentUserState: StateFlow<FirebaseUser?> = _currentUserState.asStateFlow()

  private val _isAuthReady = MutableStateFlow(false)
  val isAuthReady: StateFlow<Boolean> = _isAuthReady.asStateFlow()

  init {
    // Check initial auth state strictly on Dispatchers.IO to never block the Android Main UI Thread
    CoroutineScope(Dispatchers.IO).launch {
      try {
        val fbAuth = getFirebaseAuth()
        if (fbAuth != null) {
          _currentUserState.value = fbAuth.currentUser
          _isAuthReady.value = true
          fbAuth.addAuthStateListener { firebaseAuth ->
            _currentUserState.value = firebaseAuth.currentUser
            _isAuthReady.value = true
          }
        } else {
          _isAuthReady.value = true
        }
      } catch (e: Throwable) {
        Log.w(TAG, "Error registering auth listener: ${e.message}")
        _isAuthReady.value = true
      }
    }
  }

  val currentUser: FirebaseUser?
    get() = runCatching { getFirebaseAuth()?.currentUser }.getOrNull()

  val isAuthenticated: Boolean
    get() = currentUser != null

  /**
   * Google 1-Tap Sign-In using Android Jetpack CredentialManager and GetSignInWithGoogleOption.
   * Completely wrapped in non-fatal fallbacks.
   */
  fun signInWithGoogle(
    activity: Activity,
    scope: CoroutineScope,
    onResult: (Boolean, String?) -> Unit
  ) {
    scope.launch(Dispatchers.Main) {
      val fbAuth = getFirebaseAuth()
      if (fbAuth == null) {
        onResult(false, "Authentication service is operating in offline mode.")
        return@launch
      }

      try {
        val serverClientId = try {
          activity.getString(R.string.default_web_client_id)
        } catch (t: Throwable) {
          "41954701104-1eqfhs3s6p6naknnui85ab2hubaki4dc.apps.googleusercontent.com"
        }

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
          val authResult = fbAuth.signInWithCredential(authCredential).await()

          if (authResult.user != null) {
            _currentUserState.value = authResult.user
            onResult(true, null)
          } else {
            onResult(false, "Authentication failed. No user profile returned.")
          }
        } else {
          onResult(false, "Unexpected credential type received.")
        }
      } catch (e: GetCredentialCancellationException) {
        Log.w(TAG, "Google Sign-In was cancelled by user.")
        onResult(false, "Sign-in was cancelled.")
      } catch (e: Throwable) {
        Log.e(TAG, "Google Sign-In failed safely", e)
        val msg = e.localizedMessage ?: "Google Sign-In network timeout or configuration issue."
        onResult(false, msg)
      }
    }
  }

  /**
   * Minimalist Email/Password sign-in or account creation.
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
      onResult(false, "Please enter a valid email address.")
      return
    }

    if (cleanPass.length < 6) {
      onResult(false, "Password must be at least 6 characters.")
      return
    }

    scope.launch(Dispatchers.IO) {
      val fbAuth = getFirebaseAuth()
      if (fbAuth == null) {
        onResult(false, "Authentication service is unavailable offline.")
        return@launch
      }

      try {
        val task = if (isSignUp) {
          fbAuth.createUserWithEmailAndPassword(cleanEmail, cleanPass)
        } else {
          fbAuth.signInWithEmailAndPassword(cleanEmail, cleanPass)
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
            } catch (profileEx: Throwable) {
              Log.w(TAG, "Failed to update user profile display name", profileEx)
            }
          }
          _currentUserState.value = user
          onResult(true, null)
        } else {
          onResult(false, "Unable to complete authentication.")
        }
      } catch (e: Throwable) {
        Log.e(TAG, "Email/Password auth error", e)
        val readableMessage = when {
          e.message?.contains("The email address is badly formatted", ignoreCase = true) == true ->
            "The email address is incorrectly formatted."
          e.message?.contains("The password is invalid", ignoreCase = true) == true ||
          e.message?.contains("wrong password", ignoreCase = true) == true ||
          e.message?.contains("invalid-credential", ignoreCase = true) == true ->
            "Incorrect password or email combination."
          e.message?.contains("email address is already in use", ignoreCase = true) == true ->
            "An account already exists with this email. Please sign in instead."
          e.message?.contains("no user record corresponding", ignoreCase = true) == true ||
          e.message?.contains("user-not-found", ignoreCase = true) == true ->
            "No account found with this email. Please switch to Create Account."
          else -> e.localizedMessage ?: "Authentication failed."
        }
        onResult(false, readableMessage)
      }
    }
  }

  fun signOut() {
    try {
      getFirebaseAuth()?.signOut()
    } catch (e: Throwable) {
      Log.w(TAG, "Sign out error ignored: ${e.message}")
    }
    _currentUserState.value = null
  }
}
