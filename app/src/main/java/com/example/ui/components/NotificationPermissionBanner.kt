package com.example.ui.components

import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationManagerCompat
import com.example.ui.theme.SwissBorder
import com.example.ui.theme.SwissCrimson
import com.example.ui.theme.SwissDark
import com.example.ui.theme.SwissTextSecondary
import com.example.ui.theme.SwissTextTertiary

@Composable
fun NotificationPermissionBanner(
  isPermissionGranted: Boolean,
  onOpenSettings: () -> Unit,
  onOpenSimulator: () -> Unit,
  modifier: Modifier = Modifier
) {
  Row(
    modifier = modifier
      .fillMaxWidth()
      .background(Color(0xFFF9FAFB), RoundedCornerShape(10.dp))
      .border(1.dp, SwissBorder, RoundedCornerShape(10.dp))
      .padding(12.dp)
      .testTag("notification_listener_status_banner"),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically
  ) {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(10.dp),
      modifier = Modifier.weight(1f)
    ) {
      Box(
        modifier = Modifier
          .size(8.dp)
          .background(if (isPermissionGranted) Color(0xFF10B981) else SwissCrimson, CircleShape)
      )

      Column {
        Text(
          text = if (isPermissionGranted) "UPI Auto-Reader Active" else "UPI Auto-Reader Inactive",
          style = MaterialTheme.typography.bodyMedium,
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.onSurface
        )
        Text(
          text = if (isPermissionGranted)
            "Listening for Google Pay, PhonePe & Paytm"
          else
            "Grant notification access or use live simulator",
          style = MaterialTheme.typography.bodySmall,
          color = SwissTextSecondary,
          fontSize = 11.sp
        )
      }
    }

    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
      if (!isPermissionGranted) {
        Button(
          onClick = onOpenSettings,
          colors = ButtonDefaults.buttonColors(containerColor = SwissDark),
          shape = RoundedCornerShape(6.dp),
          modifier = Modifier.height(34.dp)
        ) {
          Text("Enable", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
        }
      }

      Button(
        onClick = onOpenSimulator,
        colors = ButtonDefaults.buttonColors(
          containerColor = if (isPermissionGranted) SwissDark else SwissCrimson
        ),
        shape = RoundedCornerShape(6.dp),
        modifier = Modifier.height(34.dp).testTag("simulate_notification_button")
      ) {
        Text("Test Demo", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
      }
    }
  }
}
