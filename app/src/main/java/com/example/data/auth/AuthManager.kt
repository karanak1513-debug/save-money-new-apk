package com.example.data.auth

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import com.example.R
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.FirebaseException
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.concurrent.TimeUnit

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
   * Google Sign-In using Android Jetpack CredentialManager and GetSignInWithGoogleOption.
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
   * Email/Password sign-in or account creation.
   */
  fun signInWithEmailPassword(
    email: String,
    pass: String,
    isSignUp: Boolean,
    scope: CoroutineScope,
    onResult: (Boolean, String?) -> Unit
  ) {
    if (email.isBlank() || pass.length < 6) {
      onResult(false, "Please provide a valid email and at least 6-character password.")
      return
    }

    scope.launch(Dispatchers.IO) {
      try {
        val task = if (isSignUp) {
          auth.createUserWithEmailAndPassword(email.trim(), pass)
        } else {
          auth.signInWithEmailAndPassword(email.trim(), pass)
        }
        val result = task.await()
        if (result.user != null) {
          onResult(true, null)
        } else {
          onResult(false, "Unable to complete authentication.")
        }
      } catch (e: Exception) {
        Log.e(TAG, "Email/Password auth error", e)
        onResult(false, e.localizedMessage ?: "Authentication failed")
      }
    }
  }

  /**
   * Initiate Phone Number OTP verification.
   */
  fun sendPhoneOtp(
    activity: Activity,
    phoneNumber: String,
    onCodeSent: (String) -> Unit,
    onError: (String) -> Unit
  ) {
    val formattedNumber = if (!phoneNumber.startsWith("+")) "+91$phoneNumber" else phoneNumber

    val callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
      override fun onVerificationCompleted(credential: PhoneAuthCredential) {
        // Auto-retrieval
        auth.signInWithCredential(credential)
      }

      override fun onVerificationFailed(e: FirebaseException) {
        Log.e(TAG, "Phone verification failed", e)
        onError(e.localizedMessage ?: "Phone OTP verification failed.")
      }

      override fun onCodeSent(
        verificationId: String,
        token: PhoneAuthProvider.ForceResendingToken
      ) {
        Log.i(TAG, "OTP code sent to $formattedNumber")
        onCodeSent(verificationId)
      }
    }

    val options = PhoneAuthOptions.newBuilder(auth)
      .setPhoneNumber(formattedNumber)
      .setTimeout(60L, TimeUnit.SECONDS)
      .setActivity(activity)
      .setCallbacks(callbacks)
      .build()

    PhoneAuthProvider.verifyPhoneNumber(options)
  }

  /**
   * Verify Phone OTP and sign in.
   */
  fun verifyPhoneOtp(
    verificationId: String,
    code: String,
    scope: CoroutineScope,
    onResult: (Boolean, String?) -> Unit
  ) {
    if (verificationId.isBlank() || code.length < 6) {
      onResult(false, "Please enter a valid 6-digit OTP code.")
      return
    }

    scope.launch(Dispatchers.IO) {
      try {
        val credential = PhoneAuthProvider.getCredential(verificationId, code.trim())
        val result = auth.signInWithCredential(credential).await()
        if (result.user != null) {
          onResult(true, null)
        } else {
          onResult(false, "Invalid verification code.")
        }
      } catch (e: Exception) {
        Log.e(TAG, "OTP verification error", e)
        onResult(false, e.localizedMessage ?: "OTP verification failed")
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
