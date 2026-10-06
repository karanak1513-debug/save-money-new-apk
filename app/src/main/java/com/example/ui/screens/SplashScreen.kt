package com.example.ui.screens

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.MonospaceMicro
import com.example.ui.theme.MonospaceSmall
import com.example.ui.theme.SwissBorder
import com.example.ui.theme.SwissCrimson
import com.example.ui.theme.SwissDark
import com.example.ui.theme.SwissTextSecondary
import com.example.ui.theme.SwissTextTertiary
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(
  onSplashFinished: () -> Unit,
  modifier: Modifier = Modifier
) {
  val infiniteTransition = rememberInfiniteTransition(label = "splash_pulse")
  val pulseScale by infiniteTransition.animateFloat(
    initialValue = 0.98f,
    targetValue = 1.03f,
    animationSpec = infiniteRepeatable(
      animation = tween(900, easing = FastOutSlowInEasing),
      repeatMode = RepeatMode.Reverse
    ),
    label = "pulse_scale"
  )

  LaunchedEffect(Unit) {
    delay(1100)
    onSplashFinished()
  }

  Box(
    modifier = modifier
      .fillMaxSize()
      .background(MaterialTheme.colorScheme.background)
      .testTag("splash_screen"),
    contentAlignment = Alignment.Center
  ) {
    Column(
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center
    ) {
      // High-precision Architectural Emblem
      Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
          .size(88.dp)
          .scale(pulseScale)
          .background(MaterialTheme.colorScheme.surface, CircleShape)
          .border(1.dp, SwissBorder, CircleShape)
      ) {
        Canvas(modifier = Modifier.size(46.dp)) {
          val stroke = 4.5f.dp.toPx()
          val crimson = Color(0xFFE11D48)
          val dark = Color(0xFF090A0C)

          // Top crossbar
          drawLine(
            color = crimson,
            start = Offset(2f, 6f),
            end = Offset(size.width - 2f, 6f),
            strokeWidth = stroke
          )

          // Sub-bar
          drawLine(
            color = dark,
            start = Offset(2f, 20f),
            end = Offset(size.width * 0.72f, 20f),
            strokeWidth = stroke
          )

          // Vertical spine
          drawLine(
            color = dark,
            start = Offset(size.width * 0.28f, 6f),
            end = Offset(size.width * 0.28f, size.height * 0.58f),
            strokeWidth = stroke
          )

          // Diagonal terminal kick
          drawLine(
            color = crimson,
            start = Offset(size.width * 0.25f, size.height * 0.52f),
            end = Offset(size.width * 0.88f, size.height - 2f),
            strokeWidth = stroke
          )
        }
      }

      Spacer(modifier = Modifier.height(26.dp))

      // Stark Swiss Brand Title
      Text(
        text = "SANCHAY",
        style = MaterialTheme.typography.headlineLarge,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurface,
        letterSpacing = 5.sp,
        fontSize = 26.sp
      )

      Spacer(modifier = Modifier.height(6.dp))

      Text(
        text = "EDITORIAL CAPITAL DISCIPLINE // ED. 2026",
        style = MonospaceMicro,
        color = SwissTextTertiary,
        letterSpacing = 1.5.sp
      )

      Spacer(modifier = Modifier.height(44.dp))

      // Loading Session Indicator
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        Box(
          modifier = Modifier
            .size(6.dp)
            .background(SwissCrimson, CircleShape)
        )
        Text(
          text = "SYNCHRONIZING PERSISTENT SESSION...",
          style = MonospaceMicro,
          color = SwissTextSecondary,
          letterSpacing = 1.sp
        )
      }
    }
  }
}
