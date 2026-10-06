package com.example

import android.app.Application
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions

/**
 * Custom Application class extending android.app.Application.
 * Hardcodes and initializes live Firebase credentials on startup with zero offline fallback downgrade.
 */
open class SanchayApp : Application() {

  companion object {
    private const val TAG = "SanchayApp"
    lateinit var instance: SanchayApp
      private set

    const val WEB_CLIENT_ID = "41954701104-1eqfhs3s6p6naknnui85ab2hubaki4dc.apps.googleusercontent.com"
    const val FIREBASE_PROJECT_ID = "gen-lang-client-0521710909"
    const val FIREBASE_STORAGE_BUCKET = "gen-lang-client-0521710909.firebasestorage.app"
    const val FIREBASE_API_KEY = "AIzaSyBfdgixiQZgZFDqYaePDJ25dzXHQ7CXVKs"
    const val FIREBASE_APPLICATION_ID = "1:41954701104:android:4575462b4e58e7bc2fe2f5"

    fun ensureFirebaseInitialized(appContext: Application): FirebaseApp {
      if (FirebaseApp.getApps(appContext).isNotEmpty()) {
        return FirebaseApp.getInstance()
      }
      return try {
        val options = FirebaseOptions.Builder()
          .setApplicationId(FIREBASE_APPLICATION_ID)
          .setApiKey(FIREBASE_API_KEY)
          .setProjectId(FIREBASE_PROJECT_ID)
          .setStorageBucket(FIREBASE_STORAGE_BUCKET)
          .build()
        FirebaseApp.initializeApp(appContext, options)
      } catch (e: Throwable) {
        Log.e(TAG, "Error initializing live Firebase: ${e.message}", e)
        if (FirebaseApp.getApps(appContext).isNotEmpty()) {
          FirebaseApp.getInstance()
        } else {
          FirebaseApp.initializeApp(appContext) ?: FirebaseApp.getInstance()
        }
      }
    }
  }

  override fun onCreate() {
    super.onCreate()
    instance = this

    try {
      Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
        Log.e(TAG, "Uncaught exception on thread [${thread.name}]: ${throwable.message}", throwable)
      }
    } catch (e: Throwable) {
      Log.e(TAG, "Failed to register global uncaught exception handler", e)
    }

    // Force live Firebase connection immediately
    val fbApp = ensureFirebaseInitialized(this)
    Log.i(TAG, "Live Firebase connection initialized: ${fbApp.name} (${fbApp.options.projectId})")
  }
}

/**
 * Backward compatibility alias for SanchayApplication
 */
class SanchayApplication : SanchayApp()
