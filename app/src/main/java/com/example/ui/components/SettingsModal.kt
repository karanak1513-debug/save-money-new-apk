package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Login
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Upload
import com.google.firebase.auth.FirebaseUser
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsModal(
  currentUserName: String,
  isDarkTheme: Boolean,
  currentUser: FirebaseUser? = null,
  isGuestMode: Boolean = false,
  onUpdateUserName: (String) -> Unit,
  onToggleDarkTheme: (Boolean) -> Unit,
  onExportJson: () -> String,
  onImportJson: (String) -> Boolean,
  onSyncFirestore: ((Boolean, String) -> Unit) -> Unit,
  onSignOut: () -> Unit = {},
  onOpenSignIn: () -> Unit = {},
  onDismiss: () -> Unit
) {
  val context = LocalContext.current
  var nameText by remember { mutableStateOf(currentUserName) }

  var showExportDialog by remember { mutableStateOf(false) }
  var exportedJsonText by remember { mutableStateOf("") }

  var showImportDialog by remember { mutableStateOf(false) }
  var importJsonInput by remember { mutableStateOf("") }
  var importStatusMessage by remember { mutableStateOf<String?>(null) }

  var isSyncingCloud by remember { mutableStateOf(false) }
  var cloudSyncMessage by remember { mutableStateOf<String?>(null) }

  ModalBottomSheet(
    onDismissRequest = onDismiss,
    sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    containerColor = MaterialTheme.colorScheme.surface,
    shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
    dragHandle = null
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .verticalScroll(rememberScrollState())
        .padding(horizontal = 20.dp, vertical = 20.dp)
        .testTag("settings_modal")
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Column {
          Text(
            text = "CONFIGURATION",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = SwissCrimson,
            letterSpacing = 1.2.sp,
            fontSize = 10.sp
          )
          Text(
            text = "App Settings",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
          )
        }
        IconButton(
          onClick = onDismiss,
          modifier = Modifier.testTag("close_settings_button")
        ) {
          Icon(
            imageVector = Icons.Default.Close,
            contentDescription = "Close",
            tint = SwissTextSecondary
          )
        }
      }

      Spacer(modifier = Modifier.height(18.dp))

      // User Profile Name Field
      Text(
        text = "USER NAME",
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.sp,
        color = SwissTextTertiary,
        fontSize = 10.sp
      )
      Spacer(modifier = Modifier.height(4.dp))
      OutlinedTextField(
        value = nameText,
        onValueChange = { nameText = it },
        leadingIcon = {
          Icon(
            imageVector = Icons.Default.Person,
            contentDescription = null,
            tint = SwissTextSecondary
          )
        },
        singleLine = true,
        colors = OutlinedTextFieldDefaults.colors(
          focusedBorderColor = SwissDark,
          unfocusedBorderColor = SwissBorder
        ),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier
          .fillMaxWidth()
          .testTag("settings_username_input")
      )

      Spacer(modifier = Modifier.height(16.dp))

      // Currency Display
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .background(Color(0xFFF9FAFB), RoundedCornerShape(8.dp))
          .border(1.dp, SwissBorder, RoundedCornerShape(8.dp))
          .padding(14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Column {
          Text(
            text = "Primary Currency",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
          )
          Text(
            text = "Indian Rupee (₹ INR) - 100% Offline Local Data",
            style = MaterialTheme.typography.bodySmall,
            color = SwissTextSecondary
          )
        }
        Box(
          modifier = Modifier
            .background(Color(0xFFF3F4F6), RoundedCornerShape(4.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
          Text(
            text = "₹ INR",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = SwissDark
          )
        }
      }

      Spacer(modifier = Modifier.height(14.dp))

      // Theme Toggle Row
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .background(Color(0xFFF9FAFB), RoundedCornerShape(8.dp))
          .border(1.dp, SwissBorder, RoundedCornerShape(8.dp))
          .padding(14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          Icon(
            imageVector = if (isDarkTheme) Icons.Default.DarkMode else Icons.Default.LightMode,
            contentDescription = null,
            tint = if (isDarkTheme) SwissCrimson else SwissDark,
            modifier = Modifier.size(20.dp)
          )
          Column {
            Text(
              text = "Theme Appearance",
              style = MaterialTheme.typography.bodyMedium,
              fontWeight = FontWeight.SemiBold,
              color = MaterialTheme.colorScheme.onSurface
            )
            Text(
              text = if (isDarkTheme) "Dark minimalist" else "Light Swiss typographic (Default)",
              style = MaterialTheme.typography.bodySmall,
              color = SwissTextSecondary
            )
          }
        }

        Switch(
          checked = isDarkTheme,
          onCheckedChange = { onToggleDarkTheme(it) },
          colors = SwitchDefaults.colors(
            checkedThumbColor = Color.White,
            checkedTrackColor = SwissCrimson,
            uncheckedThumbColor = Color.White,
            uncheckedTrackColor = SwissBorder
          ),
          modifier = Modifier.testTag("theme_switch")
        )
      }

      Spacer(modifier = Modifier.height(20.dp))

      // Account & Authentication Section
      Text(
        text = "AUTHENTICATION & ACCOUNT",
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.sp,
        color = SwissTextTertiary,
        fontSize = 10.sp
      )
      Spacer(modifier = Modifier.height(8.dp))

      Box(
        modifier = Modifier
          .fillMaxWidth()
          .background(Color(0xFFF9FAFB), RoundedCornerShape(8.dp))
          .border(1.dp, SwissBorder, RoundedCornerShape(8.dp))
          .padding(14.dp)
      ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
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
                  .size(36.dp)
                  .background(if (currentUser != null) SwissCrimsonLight else Color(0xFFE5E7EB), RoundedCornerShape(6.dp)),
                contentAlignment = Alignment.Center
              ) {
                Icon(
                  imageVector = Icons.Default.Person,
                  contentDescription = null,
                  tint = if (currentUser != null) SwissCrimson else SwissDark,
                  modifier = Modifier.size(20.dp)
                )
              }
              Column {
                Text(
                  text = if (currentUser != null) {
                    currentUser.displayName ?: currentUser.email ?: currentUser.phoneNumber ?: "Authenticated User"
                  } else {
                    "Offline Local Account"
                  },
                  style = MaterialTheme.typography.bodyMedium,
                  fontWeight = FontWeight.Bold,
                  color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                  text = if (currentUser != null) {
                    val method = when {
                      currentUser.providerData.any { it.providerId == "google.com" } -> "Google Account"
                      currentUser.providerData.any { it.providerId == "phone" } -> "Phone OTP"
                      else -> "Email & Password"
                    }
                    "Synced with $method"
                  } else {
                    "Guest Mode • Data stored on device"
                  },
                  style = MaterialTheme.typography.bodySmall,
                  color = SwissTextSecondary,
                  fontSize = 11.sp
                )
              }
            }
          }

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
          ) {
            if (currentUser != null) {
              OutlinedButton(
                onClick = {
                  onSignOut()
                  onDismiss()
                },
                shape = RoundedCornerShape(6.dp),
                modifier = Modifier.testTag("sign_out_button")
              ) {
                Icon(
                  imageVector = Icons.Default.ExitToApp,
                  contentDescription = null,
                  tint = SwissCrimson,
                  modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("Sign Out", color = SwissCrimson, style = MaterialTheme.typography.labelSmall)
              }
            } else {
              Button(
                onClick = {
                  onOpenSignIn()
                  onDismiss()
                },
                colors = ButtonDefaults.buttonColors(
                  containerColor = SwissDark,
                  contentColor = Color.White
                ),
                shape = RoundedCornerShape(6.dp),
                modifier = Modifier.testTag("sign_in_link_button")
              ) {
                Icon(
                  imageVector = Icons.Default.Login,
                  contentDescription = null,
                  tint = Color.White,
                  modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("Sign In / Sync Cloud", style = MaterialTheme.typography.labelSmall)
              }
            }
          }
        }
      }

      Spacer(modifier = Modifier.height(20.dp))

      // Feature 4: 1-Click Backup & Restore Section
      Text(
        text = "DATA BACKUP & CLOUD STORAGE",
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.sp,
        color = SwissTextTertiary,
        fontSize = 10.sp
      )
      Spacer(modifier = Modifier.height(8.dp))

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        // Export Data (JSON)
        Box(
          modifier = Modifier
            .weight(1f)
            .background(Color(0xFFF9FAFB), RoundedCornerShape(8.dp))
            .border(1.dp, SwissBorder, RoundedCornerShape(8.dp))
            .clickable {
              val json = onExportJson()
              exportedJsonText = json
              showExportDialog = true
            }
            .padding(vertical = 12.dp, horizontal = 10.dp)
            .testTag("export_data_json_button"),
          contentAlignment = Alignment.Center
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            Icon(Icons.Default.Download, contentDescription = null, tint = SwissDark, modifier = Modifier.size(16.dp))
            Text(
              text = "Export (JSON)",
              style = MaterialTheme.typography.bodySmall,
              fontWeight = FontWeight.Bold,
              color = SwissDark
            )
          }
        }

        // Import Data (JSON)
        Box(
          modifier = Modifier
            .weight(1f)
            .background(Color(0xFFF9FAFB), RoundedCornerShape(8.dp))
            .border(1.dp, SwissBorder, RoundedCornerShape(8.dp))
            .clickable {
              importJsonInput = ""
              importStatusMessage = null
              showImportDialog = true
            }
            .padding(vertical = 12.dp, horizontal = 10.dp)
            .testTag("import_data_json_button"),
          contentAlignment = Alignment.Center
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            Icon(Icons.Default.Upload, contentDescription = null, tint = SwissCrimson, modifier = Modifier.size(16.dp))
            Text(
              text = "Import (JSON)",
              style = MaterialTheme.typography.bodySmall,
              fontWeight = FontWeight.Bold,
              color = SwissCrimson
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(10.dp))

      // Cloud Sync (Firebase Firestore)
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .background(Color(0xFFF9FAFB), RoundedCornerShape(8.dp))
          .border(1.dp, SwissBorder, RoundedCornerShape(8.dp))
          .clickable {
            if (!isSyncingCloud) {
              isSyncingCloud = true
              cloudSyncMessage = "Syncing with Firebase Firestore..."
              onSyncFirestore { success, msg ->
                isSyncingCloud = false
                cloudSyncMessage = msg
                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
              }
            }
          }
          .padding(14.dp)
          .testTag("firebase_cloud_sync_button")
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
          ) {
            Icon(
              imageVector = Icons.Default.CloudUpload,
              contentDescription = null,
              tint = SwissCrimson,
              modifier = Modifier.size(20.dp)
            )
            Column {
              Text(
                text = "Firebase Firestore Cloud Sync",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
              )
              Text(
                text = cloudSyncMessage ?: "Backup goals and transactions to cloud database",
                style = MaterialTheme.typography.bodySmall,
                color = if (cloudSyncMessage?.contains("error", ignoreCase = true) == true) SwissCrimson else SwissTextSecondary,
                fontSize = 11.sp
              )
            }
          }

          if (isSyncingCloud) {
            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = SwissCrimson)
          } else {
            Text(
              text = "Sync Now",
              style = MaterialTheme.typography.labelSmall,
              fontWeight = FontWeight.Bold,
              color = SwissCrimson
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(24.dp))

      Button(
        onClick = {
          if (nameText.isNotBlank()) {
            onUpdateUserName(nameText.trim())
          }
          onDismiss()
        },
        colors = ButtonDefaults.buttonColors(
          containerColor = SwissDark,
          contentColor = Color.White
        ),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier
          .fillMaxWidth()
          .height(50.dp)
          .testTag("save_settings_button")
      ) {
        Text(
          text = "Save Preferences",
          style = MaterialTheme.typography.labelLarge,
          fontWeight = FontWeight.Bold
        )
      }

      Spacer(modifier = Modifier.height(10.dp))
    }
  }

  // Export Dialog
  if (showExportDialog) {
    AlertDialog(
      onDismissRequest = { showExportDialog = false },
      title = { Text("Export Data (JSON)", fontWeight = FontWeight.Bold) },
      text = {
        Column {
          Text(
            text = "Your complete backup JSON is generated. Copy it to your clipboard or share it to keep your data safe.",
            style = MaterialTheme.typography.bodySmall,
            color = SwissTextSecondary
          )
          Spacer(modifier = Modifier.height(10.dp))
          Box(
            modifier = Modifier
              .fillMaxWidth()
              .height(160.dp)
              .background(Color(0xFFF3F4F6), RoundedCornerShape(6.dp))
              .border(1.dp, SwissBorder, RoundedCornerShape(6.dp))
              .verticalScroll(rememberScrollState())
              .padding(10.dp)
          ) {
            Text(
              text = exportedJsonText,
              style = MonospaceSmall,
              fontSize = 10.sp,
              color = SwissDark
            )
          }
        }
      },
      confirmButton = {
        Button(
          onClick = {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            clipboard.setPrimaryClip(ClipData.newPlainText("Sanchay Backup JSON", exportedJsonText))
            Toast.makeText(context, "Backup JSON copied to clipboard!", Toast.LENGTH_SHORT).show()
            showExportDialog = false
          },
          colors = ButtonDefaults.buttonColors(containerColor = SwissCrimson)
        ) {
          Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
            Text("Copy JSON")
          }
        }
      },
      dismissButton = {
        TextButton(onClick = {
          val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "Sanchay Backup Data")
            putExtra(Intent.EXTRA_TEXT, exportedJsonText)
          }
          context.startActivity(Intent.createChooser(shareIntent, "Share Backup JSON"))
          showExportDialog = false
        }) {
          Text("Share")
        }
      }
    )
  }

  // Import Dialog
  if (showImportDialog) {
    AlertDialog(
      onDismissRequest = { showImportDialog = false },
      title = { Text("Import Data (JSON)", fontWeight = FontWeight.Bold) },
      text = {
        Column {
          Text(
            text = "Paste a valid Sanchay backup JSON string below to restore your goals and ledger.",
            style = MaterialTheme.typography.bodySmall,
            color = SwissTextSecondary
          )
          Spacer(modifier = Modifier.height(10.dp))
          OutlinedTextField(
            value = importJsonInput,
            onValueChange = { importJsonInput = it },
            placeholder = { Text("Paste JSON here...") },
            trailingIcon = {
              IconButton(onClick = {
                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                val clip = clipboard?.primaryClip?.getItemAt(0)?.text?.toString()
                if (!clip.isNullOrBlank()) {
                  importJsonInput = clip
                }
              }) {
                Icon(Icons.Default.ContentPaste, contentDescription = "Paste", tint = SwissDark)
              }
            },
            maxLines = 5,
            modifier = Modifier.fillMaxWidth()
          )

          if (importStatusMessage != null) {
            Spacer(modifier = Modifier.height(6.dp))
            Text(
              text = importStatusMessage!!,
              style = MaterialTheme.typography.bodySmall,
              color = SwissCrimson
            )
          }
        }
      },
      confirmButton = {
        Button(
          onClick = {
            if (importJsonInput.isBlank()) {
              importStatusMessage = "Please paste a backup JSON payload"
              return@Button
            }
            val success = onImportJson(importJsonInput)
            if (success) {
              Toast.makeText(context, "Data successfully restored!", Toast.LENGTH_SHORT).show()
              showImportDialog = false
            } else {
              importStatusMessage = "Invalid backup JSON format. Please verify the content."
            }
          },
          colors = ButtonDefaults.buttonColors(containerColor = SwissCrimson)
        ) {
          Text("Restore Data")
        }
      },
      dismissButton = {
        TextButton(onClick = { showImportDialog = false }) {
          Text("Cancel")
        }
      }
    )
  }
}
