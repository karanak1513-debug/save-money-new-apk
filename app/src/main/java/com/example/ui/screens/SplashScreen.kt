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
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.MonospaceSmall
import com.example.ui.theme.SwissBorder
import com.example.ui.theme.SwissCrimson
import com.example.ui.theme.SwissCrimsonLight
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
    initialValue = 0.96f,
    targetValue = 1.04f,
    animationSpec = infiniteRepeatable(
      animation = tween(1000, easing = FastOutSlowInEasing),
      repeatMode = RepeatMode.Reverse
    ),
    label = "pulse_scale"
  )

  LaunchedEffect(Unit) {
    // Elegant minimum splash display duration before routing
    delay(1200)
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
      // Minimalist Geometric Rupee Emblem
      Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
          .size(96.dp)
          .scale(pulseScale)
          .background(MaterialTheme.colorScheme.surface, CircleShape)
          .border(2.dp, SwissBorder, CircleShape)
      ) {
        Canvas(modifier = Modifier.size(54.dp)) {
          val stroke = 5.dp.toPx()
          val crimson = Color(0xFFD32F2F)
          val dark = Color(0xFF111111)

          // Upper Rupee horizontal bar (Crimson accent)
          drawLine(
            color = crimson,
            start = Offset(4f, 8f),
            end = Offset(size.width - 4f, 8f),
            strokeWidth = stroke
          )

          // Second Rupee horizontal bar (Dark)
          drawLine(
            color = dark,
            start = Offset(4f, 24f),
            end = Offset(size.width * 0.75f, 24f),
            strokeWidth = stroke
          )

          // Central curved stem
          drawLine(
            color = dark,
            start = Offset(size.width * 0.28f, 8f),
            end = Offset(size.width * 0.28f, size.height * 0.58f),
            strokeWidth = stroke
          )

          // Diagonal kick
          drawLine(
            color = crimson,
            start = Offset(size.width * 0.25f, size.height * 0.52f),
            end = Offset(size.width * 0.85f, size.height - 4f),
            strokeWidth = stroke
          )
        }
      }

      Spacer(modifier = Modifier.height(24.dp))

      // Stark Swiss Brand Title
      Text(
        text = "SANCHAY",
        style = MaterialTheme.typography.headlineLarge,
        fontWeight = FontWeight.Black,
        color = MaterialTheme.colorScheme.onSurface,
        letterSpacing = 4.sp,
        fontSize = 28.sp
      )

      Spacer(modifier = Modifier.height(6.dp))

      Text(
        text = "PERSONAL MANUAL SAVINGS TRACKER",
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.Bold,
        color = SwissTextTertiary,
        letterSpacing = 1.6.sp,
        fontSize = 10.sp
      )

      Spacer(modifier = Modifier.height(48.dp))

      // Loading Session indicator
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        Box(
          modifier = Modifier
            .size(8.dp)
            .background(SwissCrimson, CircleShape)
        )
        Text(
          text = "Checking session & ledger...",
          style = MonospaceSmall,
          color = SwissTextSecondary,
          fontSize = 11.sp
        )
      }
    }
  }
}
