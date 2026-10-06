package com.example.ui.screens

import android.app.Activity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.auth.AuthManager
import com.example.ui.theme.MonospaceSmall
import com.example.ui.theme.SwissBorder
import com.example.ui.theme.SwissCrimson
import com.example.ui.theme.SwissCrimsonLight
import com.example.ui.theme.SwissDark
import com.example.ui.theme.SwissTextSecondary
import com.example.ui.theme.SwissTextTertiary

enum class AuthTab(val title: String) {
  GOOGLE("Google"),
  EMAIL("Email"),
  PHONE("Phone OTP")
}

@Composable
fun SignInScreen(
  onAuthenticated: () -> Unit,
  onContinueAsGuest: () -> Unit,
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  val activity = context as? Activity
  val scope = rememberCoroutineScope()

  var selectedTab by remember { mutableStateOf(AuthTab.GOOGLE) }
  var isLoading by remember { mutableStateOf(false) }
  var errorMessage by remember { mutableStateOf<String?>(null) }

  // Email form state
  var emailText by remember { mutableStateOf("") }
  var passwordText by remember { mutableStateOf("") }
  var isSignUpMode by remember { mutableStateOf(false) }

  // Phone form state
  var phoneText by remember { mutableStateOf("") }
  var otpText by remember { mutableStateOf("") }
  var verificationId by remember { mutableStateOf<String?>(null) }
  var isOtpSent by remember { mutableStateOf(false) }

  Box(
    modifier = modifier
      .fillMaxSize()
      .background(MaterialTheme.colorScheme.background)
      .testTag("sign_in_screen"),
    contentAlignment = Alignment.Center
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .verticalScroll(rememberScrollState())
        .padding(horizontal = 24.dp, vertical = 32.dp),
      horizontalAlignment = Alignment.CenterHorizontally
    ) {
      // Sanchay Rupee Badge
      Box(
        modifier = Modifier
          .size(56.dp)
          .background(SwissCrimsonLight, CircleShape)
          .border(1.5.dp, SwissCrimson, CircleShape),
        contentAlignment = Alignment.Center
      ) {
        Text(
          text = "₹",
          fontSize = 28.sp,
          fontWeight = FontWeight.Bold,
          color = SwissCrimson
        )
      }

      Spacer(modifier = Modifier.height(16.dp))

      Text(
        text = "SANCHAY",
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Black,
        letterSpacing = 2.sp,
        color = MaterialTheme.colorScheme.onSurface
      )

      Text(
        text = "Persistent Cloud Sync & Auth",
        style = MaterialTheme.typography.bodySmall,
        color = SwissTextSecondary,
        fontSize = 12.sp
      )

      Spacer(modifier = Modifier.height(24.dp))

      // Tab switcher
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .background(Color(0xFFF3F4F6), RoundedCornerShape(8.dp))
          .padding(4.dp)
      ) {
        AuthTab.values().forEach { tab ->
          val isSelected = selectedTab == tab
          Box(
            modifier = Modifier
              .weight(1f)
              .background(
                if (isSelected) SwissDark else Color.Transparent,
                RoundedCornerShape(6.dp)
              )
              .clickable {
                selectedTab = tab
                errorMessage = null
              }
              .padding(vertical = 8.dp),
            contentAlignment = Alignment.Center
          ) {
            Text(
              text = tab.title,
              style = MaterialTheme.typography.labelSmall,
              fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
              color = if (isSelected) Color.White else SwissDark,
              fontSize = 11.sp
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(20.dp))

      // Error Banner
      AnimatedVisibility(visible = errorMessage != null) {
        if (errorMessage != null) {
          Box(
            modifier = Modifier
              .fillMaxWidth()
              .background(SwissCrimsonLight, RoundedCornerShape(8.dp))
              .border(1.dp, SwissCrimson, RoundedCornerShape(8.dp))
              .padding(12.dp)
          ) {
            Text(
              text = errorMessage!!,
              style = MaterialTheme.typography.bodySmall,
              color = SwissCrimson,
              fontWeight = FontWeight.Medium
            )
          }
          Spacer(modifier = Modifier.height(16.dp))
        }
      }

      // Content based on Selected Tab
      when (selectedTab) {
        AuthTab.GOOGLE -> {
          Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
          ) {
            Text(
              text = "1-Tap Google Sign-In is provisioned and authenticated via Android CredentialManager.",
              style = MaterialTheme.typography.bodySmall,
              color = SwissTextSecondary,
              textAlign = androidx.compose.ui.text.style.TextAlign.Center,
              lineHeight = 16.sp
            )

            Spacer(modifier = Modifier.height(18.dp))

            // Primary Google Button
            Button(
              onClick = {
                if (activity != null) {
                  isLoading = true
                  errorMessage = null
                  AuthManager.signInWithGoogle(activity, scope) { success, err ->
                    isLoading = false
                    if (success) {
                      onAuthenticated()
                    } else if (err != null) {
                      errorMessage = err
                    }
                  }
                } else {
                  errorMessage = "Activity context is unavailable."
                }
              },
              enabled = !isLoading,
              colors = ButtonDefaults.buttonColors(
                containerColor = SwissDark,
                contentColor = Color.White
              ),
              shape = RoundedCornerShape(8.dp),
              modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .testTag("google_sign_in_button")
            ) {
              if (isLoading) {
                CircularProgressIndicator(
                  color = Color.White,
                  modifier = Modifier.size(20.dp),
                  strokeWidth = 2.dp
                )
              } else {
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                  // Clean G emblem
                  Box(
                    modifier = Modifier
                      .size(22.dp)
                      .background(Color.White, CircleShape),
                    contentAlignment = Alignment.Center
                  ) {
                    Text(
                      text = "G",
                      fontWeight = FontWeight.Black,
                      color = Color(0xFF4285F4),
                      fontSize = 14.sp
                    )
                  }
                  Text(
                    text = "Sign in with Google",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                  )
                }
              }
            }
          }
        }

        AuthTab.EMAIL -> {
          Column(modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(
              value = emailText,
              onValueChange = { emailText = it },
              label = { Text("Email Address") },
              leadingIcon = { Icon(Icons.Default.Email, contentDescription = null, tint = SwissTextSecondary) },
              keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
              singleLine = true,
              colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = SwissDark,
                unfocusedBorderColor = SwissBorder
              ),
              shape = RoundedCornerShape(8.dp),
              modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedTextField(
              value = passwordText,
              onValueChange = { passwordText = it },
              label = { Text("Password (min 6 chars)") },
              leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = SwissTextSecondary) },
              visualTransformation = PasswordVisualTransformation(),
              keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
              singleLine = true,
              colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = SwissDark,
                unfocusedBorderColor = SwissBorder
              ),
              shape = RoundedCornerShape(8.dp),
              modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(
              onClick = {
                isLoading = true
                errorMessage = null
                AuthManager.signInWithEmailPassword(
                  emailText,
                  passwordText,
                  isSignUpMode,
                  scope
                ) { success, err ->
                  isLoading = false
                  if (success) {
                    onAuthenticated()
                  } else if (err != null) {
                    errorMessage = err
                  }
                }
              },
              enabled = !isLoading,
              colors = ButtonDefaults.buttonColors(
                containerColor = SwissCrimson,
                contentColor = Color.White
              ),
              shape = RoundedCornerShape(8.dp),
              modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .testTag("email_auth_button")
            ) {
              if (isLoading) {
                CircularProgressIndicator(
                  color = Color.White,
                  modifier = Modifier.size(20.dp),
                  strokeWidth = 2.dp
                )
              } else {
                Text(
                  text = if (isSignUpMode) "Create Account" else "Sign In with Email",
                  style = MaterialTheme.typography.labelLarge,
                  fontWeight = FontWeight.Bold
                )
              }
            }

            Spacer(modifier = Modifier.height(6.dp))

            TextButton(
              onClick = { isSignUpMode = !isSignUpMode },
              modifier = Modifier.align(Alignment.CenterHorizontally)
            ) {
              Text(
                text = if (isSignUpMode) "Already have an account? Sign In" else "Need an account? Sign Up",
                style = MaterialTheme.typography.bodySmall,
                color = SwissDark,
                fontWeight = FontWeight.SemiBold
              )
            }
          }
        }

        AuthTab.PHONE -> {
          Column(modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(
              value = phoneText,
              onValueChange = { phoneText = it },
              label = { Text("Mobile Number (10 digits)") },
              prefix = { Text("+91 ") },
              leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null, tint = SwissTextSecondary) },
              keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
              singleLine = true,
              enabled = !isOtpSent,
              colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = SwissDark,
                unfocusedBorderColor = SwissBorder
              ),
              shape = RoundedCornerShape(8.dp),
              modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(10.dp))

            if (!isOtpSent) {
              Button(
                onClick = {
                  if (activity != null && phoneText.length >= 10) {
                    isLoading = true
                    errorMessage = null
                    AuthManager.sendPhoneOtp(
                      activity = activity,
                      phoneNumber = phoneText.trim(),
                      onCodeSent = { vId ->
                        isLoading = false
                        verificationId = vId
                        isOtpSent = true
                      },
                      onError = { err ->
                        isLoading = false
                        errorMessage = err
                      }
                    )
                  } else {
                    errorMessage = "Please enter a valid 10-digit mobile number."
                  }
                },
                enabled = !isLoading,
                colors = ButtonDefaults.buttonColors(
                  containerColor = SwissDark,
                  contentColor = Color.White
                ),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                  .fillMaxWidth()
                  .height(50.dp)
                  .testTag("send_otp_button")
              ) {
                if (isLoading) {
                  CircularProgressIndicator(color = Color.White, modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                } else {
                  Text("Send OTP", fontWeight = FontWeight.Bold)
                }
              }
            } else {
              OutlinedTextField(
                value = otpText,
                onValueChange = { otpText = it },
                label = { Text("6-Digit OTP Code") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                  focusedBorderColor = SwissDark,
                  unfocusedBorderColor = SwissBorder
                ),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
              )

              Spacer(modifier = Modifier.height(10.dp))

              Button(
                onClick = {
                  val vId = verificationId
                  if (vId != null && otpText.length >= 6) {
                    isLoading = true
                    errorMessage = null
                    AuthManager.verifyPhoneOtp(vId, otpText.trim(), scope) { success, err ->
                      isLoading = false
                      if (success) {
                        onAuthenticated()
                      } else if (err != null) {
                        errorMessage = err
                      }
                    }
                  } else {
                    errorMessage = "Please enter the 6-digit verification code."
                  }
                },
                enabled = !isLoading,
                colors = ButtonDefaults.buttonColors(
                  containerColor = SwissCrimson,
                  contentColor = Color.White
                ),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                  .fillMaxWidth()
                  .height(50.dp)
                  .testTag("verify_otp_button")
              ) {
                if (isLoading) {
                  CircularProgressIndicator(color = Color.White, modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                } else {
                  Text("Verify & Sign In", fontWeight = FontWeight.Bold)
                }
              }

              TextButton(
                onClick = {
                  isOtpSent = false
                  otpText = ""
                },
                modifier = Modifier.align(Alignment.CenterHorizontally)
              ) {
                Text("Change Mobile Number", color = SwissTextSecondary, style = MaterialTheme.typography.bodySmall)
              }
            }
          }
        }
      }

      Spacer(modifier = Modifier.height(28.dp))

      // Offline Guest mode divider
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
      ) {
        Box(modifier = Modifier.weight(1f).height(1.dp).background(SwissBorder))
        Text(
          text = "OR",
          style = MonospaceSmall,
          color = SwissTextTertiary,
          fontSize = 10.sp,
          modifier = Modifier.padding(horizontal = 12.dp)
        )
        Box(modifier = Modifier.weight(1f).height(1.dp).background(SwissBorder))
      }

      Spacer(modifier = Modifier.height(16.dp))

      // Continue as Guest Button (100% Offline Local Mode)
      Button(
        onClick = onContinueAsGuest,
        colors = ButtonDefaults.buttonColors(
          containerColor = Color(0xFFF9FAFB),
          contentColor = SwissDark
        ),
        border = androidx.compose.foundation.BorderStroke(1.dp, SwissBorder),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier
          .fillMaxWidth()
          .height(48.dp)
          .testTag("continue_as_guest_button")
      ) {
        Text(
          text = "Continue Offline (Local Ledger)",
          style = MaterialTheme.typography.labelMedium,
          fontWeight = FontWeight.SemiBold,
          color = SwissDark
        )
      }
    }
  }
}
