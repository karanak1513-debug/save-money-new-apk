package com.example

import android.app.Application
import android.util.Log
import com.google.firebase.FirebaseApp

/**
 * Custom Application class extending android.app.Application with unhandled exception handler
 * to log errors cleanly to Logcat instead of triggering the OS "App has stopped working" dialog.
 */
open class SanchayApp : Application() {

  companion object {
    private const val TAG = "SanchayApp"
    lateinit var instance: SanchayApp
      private set
  }

  override fun onCreate() {
    super.onCreate()
    instance = this

    // Global Unhandled Exception Handler: logs errors cleanly to Logcat instead of triggering OS "App has stopped working" dialog
    try {
      Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
        Log.e(TAG, "Uncaught exception on thread [${thread.name}]: ${throwable.message}", throwable)
        // Suppress crash propagation to prevent OS fatal crash dialog
      }
    } catch (e: Throwable) {
      Log.e(TAG, "Failed to register global uncaught exception handler", e)
    }

    // Completely safe non-fatal Firebase initialization
    try {
      if (FirebaseApp.getApps(this).isEmpty()) {
        FirebaseApp.initializeApp(this)
      }
      Log.i(TAG, "Firebase initialized safely")
    } catch (e: Throwable) {
      Log.w(TAG, "Firebase initialization fallback: ${e.message}")
    }
  }
}

/**
 * Backward compatibility alias for SanchayApplication
 */
class SanchayApplication : SanchayApp()
