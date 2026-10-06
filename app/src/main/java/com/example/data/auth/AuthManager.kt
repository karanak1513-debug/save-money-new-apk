package com.example.data.auth

import android.app.Activity
import android.util.Log
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import com.example.SanchayApp
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.FirebaseApp
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

object AuthManager {

  private const val TAG = "AuthManager"

  const val WEB_CLIENT_ID = SanchayApp.WEB_CLIENT_ID

  /**
   * Retrieves the live, connected FirebaseAuth instance.
   */
  fun getFirebaseAuth(): FirebaseAuth {
    val app = try {
      if (FirebaseApp.getApps(SanchayApp.instance).isNotEmpty()) {
        FirebaseApp.getInstance()
      } else {
        SanchayApp.ensureFirebaseInitialized(SanchayApp.instance)
      }
    } catch (e: Throwable) {
      SanchayApp.ensureFirebaseInitialized(SanchayApp.instance)
    }
    return FirebaseAuth.getInstance(app)
  }

  private val _currentUserState = MutableStateFlow<FirebaseUser?>(null)
  val currentUserState: StateFlow<FirebaseUser?> = _currentUserState.asStateFlow()

  private val _isAuthReady = MutableStateFlow(false)
  val isAuthReady: StateFlow<Boolean> = _isAuthReady.asStateFlow()

  init {
    CoroutineScope(Dispatchers.IO).launch {
      try {
        val fbAuth = getFirebaseAuth()
        _currentUserState.value = fbAuth.currentUser
        _isAuthReady.value = true
        fbAuth.addAuthStateListener { firebaseAuth ->
          _currentUserState.value = firebaseAuth.currentUser
          _isAuthReady.value = true
        }
      } catch (e: Throwable) {
        Log.e(TAG, "Live FirebaseAuth state listener init: ${e.message}", e)
        _isAuthReady.value = true
      }
    }
  }

  val currentUser: FirebaseUser?
    get() = try { getFirebaseAuth().currentUser } catch (e: Throwable) { null }

  val isAuthenticated: Boolean
    get() = currentUser != null

  /**
   * Live Google 1-Tap Sign-In using Android Credential Manager.
   * Authenticates directly via Firebase GoogleAuthProvider and saves user document in Firestore under users/{uid}.
   */
  fun signInWithGoogle(
    activity: Activity,
    scope: CoroutineScope,
    onResult: (Boolean, String?) -> Unit
  ) {
    scope.launch(Dispatchers.Main) {
      try {
        val fbAuth = getFirebaseAuth()
        val credentialManager = CredentialManager.create(activity)

        val googleIdOption = GetSignInWithGoogleOption.Builder(WEB_CLIENT_ID)
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
          val user = authResult.user

          if (user != null) {
            _currentUserState.value = user
            persistUserProfile(user)
            onResult(true, null)
          } else {
            onResult(false, "Authentication completed, but no user profile was returned.")
          }
        } else {
          onResult(false, "Unexpected credential type received.")
        }
      } catch (e: GetCredentialCancellationException) {
        Log.i(TAG, "Google Sign-In prompt was cancelled by user.")
        onResult(false, "Sign-in was cancelled.")
      } catch (e: Throwable) {
        Log.e(TAG, "Google Sign-In error: ${e.message}", e)
        val msg = e.localizedMessage ?: "Google Sign-In network timeout or configuration error."
        onResult(false, msg)
      }
    }
  }

  /**
   * Persists authenticated user profile directly to Firestore under users/{uid}.
   */
  fun persistUserProfile(user: FirebaseUser) {
    CoroutineScope(Dispatchers.IO).launch {
      try {
        val firestore = FirebaseFirestore.getInstance()
        val userDoc = firestore.collection("users").document(user.uid)
        val data = mapOf(
          "uid" to user.uid,
          "email" to (user.email ?: ""),
          "displayName" to (user.displayName ?: ""),
          "photoUrl" to (user.photoUrl?.toString() ?: ""),
          "lastLoginAt" to Timestamp.now()
        )
        userDoc.set(data)
        Log.i(TAG, "User profile synced to Firestore under users/${user.uid}")
      } catch (e: Throwable) {
        Log.w(TAG, "Failed syncing user profile to Firestore: ${e.message}")
      }
    }
  }

  /**
   * Live Email/Password Sign-In & Sign-Up directly using FirebaseAuth.
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
      try {
        val fbAuth = getFirebaseAuth()
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
              Log.w(TAG, "Failed to update user profile display name: ${profileEx.message}")
            }
          }
          _currentUserState.value = user
          persistUserProfile(user)
          onResult(true, null)
        } else {
          onResult(false, "Unable to complete authentication.")
        }
      } catch (e: Throwable) {
        Log.e(TAG, "Email/Password live auth error", e)
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
            "No account found with this email. Switch to Create Account to register."
          else -> e.localizedMessage ?: "Authentication failed."
        }
        onResult(false, readableMessage)
      }
    }
  }

  fun signOut() {
    try {
      getFirebaseAuth().signOut()
    } catch (e: Throwable) {
      Log.w(TAG, "Sign out error: ${e.message}")
    }
    _currentUserState.value = null
  }
}
