package com.example.ui.screens

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

/**
 * Ultra-minimalist Swiss-editorial launch screen:
 * - Pure Stark White background (#FFFFFF)
 * - Centered crisp wordmark: SANCHAY in bold tracking-widest typography
 * - Subtle 3-dot pulsating crimson indicator (#DC2626)
 */
@Composable
fun SplashScreen(
  isAuthEvaluating: Boolean = false,
  onSplashFinished: () -> Unit,
  modifier: Modifier = Modifier
) {
  LaunchedEffect(isAuthEvaluating) {
    // Ensure smooth zero-flicker display: wait at least 800ms, and until auth evaluation has completed
    delay(850)
    while (isAuthEvaluating) {
      delay(50)
    }
    onSplashFinished()
  }

  val infiniteTransition = rememberInfiniteTransition(label = "pulse_dots")

  val dot1Scale by infiniteTransition.animateFloat(
    initialValue = 0.6f,
    targetValue = 1.25f,
    animationSpec = infiniteRepeatable(
      animation = tween(durationMillis = 600, easing = FastOutSlowInEasing),
      repeatMode = RepeatMode.Reverse
    ),
    label = "dot1_scale"
  )

  val dot2Scale by infiniteTransition.animateFloat(
    initialValue = 0.6f,
    targetValue = 1.25f,
    animationSpec = infiniteRepeatable(
      animation = tween(durationMillis = 600, delayMillis = 180, easing = FastOutSlowInEasing),
      repeatMode = RepeatMode.Reverse
    ),
    label = "dot2_scale"
  )

  val dot3Scale by infiniteTransition.animateFloat(
    initialValue = 0.6f,
    targetValue = 1.25f,
    animationSpec = infiniteRepeatable(
      animation = tween(durationMillis = 600, delayMillis = 360, easing = FastOutSlowInEasing),
      repeatMode = RepeatMode.Reverse
    ),
    label = "dot3_scale"
  )

  Box(
    modifier = modifier
      .fillMaxSize()
      .background(Color(0xFFFFFFFF)) // Pure Stark White
      .testTag("splash_screen"),
    contentAlignment = Alignment.Center
  ) {
    Column(
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center
    ) {
      // Centered crisp wordmark: SANCHAY in bold tracking-widest typography
      Text(
        text = "SANCHAY",
        color = Color(0xFF111827), // Stark Black
        fontWeight = FontWeight.Black,
        fontSize = 32.sp,
        letterSpacing = 10.sp,
        modifier = Modifier.testTag("splash_wordmark")
      )

      Spacer(modifier = Modifier.height(28.dp))

      // Subtle 3-dot pulsating crimson indicator (#DC2626)
      Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.testTag("pulsating_crimson_dots")
      ) {
        val crimson = Color(0xFFDC2626)

        Box(
          modifier = Modifier
            .size(7.dp)
            .scale(dot1Scale)
            .alpha(0.35f + (dot1Scale - 0.6f) * 0.9f)
            .background(crimson, CircleShape)
        )
        Box(
          modifier = Modifier
            .size(7.dp)
            .scale(dot2Scale)
            .alpha(0.35f + (dot2Scale - 0.6f) * 0.9f)
            .background(crimson, CircleShape)
        )
        Box(
          modifier = Modifier
            .size(7.dp)
            .scale(dot3Scale)
            .alpha(0.35f + (dot3Scale - 0.6f) * 0.9f)
            .background(crimson, CircleShape)
        )
      }
    }
  }
}
