package com.example.ui.viewmodel

import android.app.Activity
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.auth.AuthManager
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Dedicated ViewModel for AuthScreen managing live Google and Email/Password flows.
 * Handles state updates, token exchange with FirebaseAuth, and error reporting.
 */
class AuthViewModel(application: Application) : AndroidViewModel(application) {

  val authUser: StateFlow<FirebaseUser?> = AuthManager.currentUserState

  private val _isLoadingGoogle = MutableStateFlow(false)
  val isLoadingGoogle: StateFlow<Boolean> = _isLoadingGoogle.asStateFlow()

  private val _isLoadingEmail = MutableStateFlow(false)
  val isLoadingEmail: StateFlow<Boolean> = _isLoadingEmail.asStateFlow()

  private val _errorMessage = MutableStateFlow<String?>(null)
  val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

  fun setGoogleLoading(loading: Boolean) {
    _isLoadingGoogle.value = loading
  }

  fun setErrorMessage(msg: String?) {
    _errorMessage.value = msg
  }

  fun clearError() {
    _errorMessage.value = null
  }

  /**
   * Exchanges Google ID Token for Firebase credential and authenticates directly.
   */
  fun handleGoogleIdToken(idToken: String, onSuccess: () -> Unit) {
    _isLoadingGoogle.value = true
    _errorMessage.value = null

    AuthManager.signInWithGoogleToken(idToken, viewModelScope) { success, errorMsg ->
      _isLoadingGoogle.value = false
      if (success) {
        onSuccess()
      } else {
        _errorMessage.value = errorMsg ?: "Google sign-in credential exchange failed."
      }
    }
  }

  /**
   * Executes Google 1-Tap Sign-In with Android Credential Manager using the verified Web Client ID.
   */
  fun signInWithGoogle(activity: Activity, onSuccess: () -> Unit) {
    _isLoadingGoogle.value = true
    _errorMessage.value = null

    AuthManager.signInWithGoogle(activity, viewModelScope) { success, errorMsg ->
      _isLoadingGoogle.value = false
      if (success) {
        onSuccess()
      } else if (errorMsg != null) {
        _errorMessage.value = errorMsg
      }
    }
  }

  /**
   * Real-time Email & Password authentication binding directly to live FirebaseAuth.
   */
  fun signInWithEmail(email: String, pass: String, isSignUp: Boolean, onSuccess: () -> Unit) {
    val cleanEmail = email.trim()
    val cleanPass = pass.trim()

    if (cleanEmail.isBlank() || !cleanEmail.contains("@") || !cleanEmail.contains(".")) {
      _errorMessage.value = "Please provide a valid email address."
      return
    }

    if (cleanPass.length < 6) {
      _errorMessage.value = "Password must be at least 6 characters."
      return
    }

    _isLoadingEmail.value = true
    _errorMessage.value = null

    AuthManager.signInWithEmailPassword(
      email = cleanEmail,
      pass = cleanPass,
      isSignUp = isSignUp,
      scope = viewModelScope
    ) { success, errorMsg ->
      _isLoadingEmail.value = false
      if (success) {
        onSuccess()
      } else if (errorMsg != null) {
        _errorMessage.value = errorMsg
      }
    }
  }
}
