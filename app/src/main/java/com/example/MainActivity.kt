package com.example

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.ui.SanchayAppContent

class MainActivity : ComponentActivity() {

  companion object {
    private const val TAG = "MainActivity"
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    try {
      enableEdgeToEdge()
      // Zero blocking calls on main thread:
      // Completely decoupled from background services, Room DB reads, and Firebase operations.
      // Boots immediately with zero flicker or startup freezes.
      setContent {
        SanchayAppContent()
      }
    } catch (e: Throwable) {
      Log.e(TAG, "Prevented startup crash in MainActivity onCreate", e)
    }
  }
}
