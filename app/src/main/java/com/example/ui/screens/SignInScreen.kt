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
import com.example.ui.theme.MonospaceMicro
import com.example.ui.theme.MonospaceSmall
import com.example.ui.theme.SwissBorder
import com.example.ui.theme.SwissCrimson
import com.example.ui.theme.SwissCrimsonLight
import com.example.ui.theme.SwissDark
import com.example.ui.theme.SwissHairline
import com.example.ui.theme.SwissTextSecondary
import com.example.ui.theme.SwissTextTertiary

enum class AuthTab(val title: String) {
  GOOGLE("GOOGLE"),
  EMAIL("EMAIL"),
  PHONE("PHONE OTP")
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
      // Geometric Emblem
      Box(
        modifier = Modifier
          .size(54.dp)
          .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(8.dp))
          .border(0.75.dp, SwissBorder, RoundedCornerShape(8.dp)),
        contentAlignment = Alignment.Center
      ) {
        Text(
          text = "₹",
          fontSize = 26.sp,
          fontWeight = FontWeight.Bold,
          color = SwissCrimson
        )
      }

      Spacer(modifier = Modifier.height(18.dp))

      Text(
        text = "SANCHAY",
        style = MaterialTheme.typography.headlineLarge,
        fontWeight = FontWeight.Bold,
        letterSpacing = 4.sp,
        color = MaterialTheme.colorScheme.onSurface
      )

      Text(
        text = "ARCHITECTURAL FINANCIAL LEDGER // AUTH",
        style = MonospaceMicro,
        color = SwissTextTertiary,
        letterSpacing = 1.2.sp
      )

      Spacer(modifier = Modifier.height(24.dp))

      // Segmented Tab Switcher
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .background(Color(0xFFF3F3F1), RoundedCornerShape(6.dp))
          .padding(3.dp)
      ) {
        AuthTab.values().forEach { tab ->
          val isSelected = selectedTab == tab
          Box(
            modifier = Modifier
              .weight(1f)
              .background(
                if (isSelected) SwissDark else Color.Transparent,
                RoundedCornerShape(4.dp)
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
              style = MonospaceMicro,
              color = if (isSelected) Color.White else SwissDark
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
              .background(SwissCrimsonLight, RoundedCornerShape(6.dp))
              .border(0.75.dp, SwissCrimson, RoundedCornerShape(6.dp))
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

      // Tab Content
      when (selectedTab) {
        AuthTab.GOOGLE -> {
          Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
          ) {
            Text(
              text = "Authenticate securely using Android Jetpack Credential Manager for verified cloud persistence.",
              style = MaterialTheme.typography.bodySmall,
              color = SwissTextSecondary,
              textAlign = androidx.compose.ui.text.style.TextAlign.Center,
              lineHeight = 17.sp
            )

            Spacer(modifier = Modifier.height(20.dp))

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
              shape = RoundedCornerShape(6.dp),
              modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .testTag("google_sign_in_button")
            ) {
              if (isLoading) {
                CircularProgressIndicator(
                  color = Color.White,
                  modifier = Modifier.size(18.dp),
                  strokeWidth = 2.dp
                )
              } else {
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                  Box(
                    modifier = Modifier
                      .size(20.dp)
                      .background(Color.White, CircleShape),
                    contentAlignment = Alignment.Center
                  ) {
                    Text(
                      text = "G",
                      fontWeight = FontWeight.Black,
                      color = Color(0xFF4285F4),
                      fontSize = 12.sp
                    )
                  }
                  Text(
                    text = "CONTINUE WITH GOOGLE",
                    style = MonospaceMicro,
                    letterSpacing = 1.sp
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
              leadingIcon = { Icon(Icons.Default.Email, contentDescription = null, tint = SwissTextSecondary, modifier = Modifier.size(18.dp)) },
              keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
              singleLine = true,
              colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = SwissDark,
                unfocusedBorderColor = SwissBorder
              ),
              shape = RoundedCornerShape(6.dp),
              modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedTextField(
              value = passwordText,
              onValueChange = { passwordText = it },
              label = { Text("Password (min 6 chars)") },
              leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = SwissTextSecondary, modifier = Modifier.size(18.dp)) },
              visualTransformation = PasswordVisualTransformation(),
              keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
              singleLine = true,
              colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = SwissDark,
                unfocusedBorderColor = SwissBorder
              ),
              shape = RoundedCornerShape(6.dp),
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
                containerColor = SwissDark,
                contentColor = Color.White
              ),
              shape = RoundedCornerShape(6.dp),
              modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .testTag("email_auth_button")
            ) {
              if (isLoading) {
                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
              } else {
                Text(
                  text = if (isSignUpMode) "REGISTER ACCOUNT" else "SIGN IN WITH EMAIL",
                  style = MonospaceMicro,
                  letterSpacing = 1.sp
                )
              }
            }

            Spacer(modifier = Modifier.height(6.dp))

            TextButton(
              onClick = { isSignUpMode = !isSignUpMode },
              modifier = Modifier.align(Alignment.CenterHorizontally)
            ) {
              Text(
                text = if (isSignUpMode) "Already have an account? Sign In" else "Create a new account instead",
                style = MaterialTheme.typography.bodySmall,
                color = SwissDark,
                fontWeight = FontWeight.Medium
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
              leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null, tint = SwissTextSecondary, modifier = Modifier.size(18.dp)) },
              keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
              singleLine = true,
              enabled = !isOtpSent,
              colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = SwissDark,
                unfocusedBorderColor = SwissBorder
              ),
              shape = RoundedCornerShape(6.dp),
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
                shape = RoundedCornerShape(6.dp),
                modifier = Modifier
                  .fillMaxWidth()
                  .height(50.dp)
                  .testTag("send_otp_button")
              ) {
                if (isLoading) {
                  CircularProgressIndicator(color = Color.White, modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                } else {
                  Text("TRANSMIT OTP", style = MonospaceMicro, letterSpacing = 1.sp)
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
                shape = RoundedCornerShape(6.dp),
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
                shape = RoundedCornerShape(6.dp),
                modifier = Modifier
                  .fillMaxWidth()
                  .height(50.dp)
                  .testTag("verify_otp_button")
              ) {
                if (isLoading) {
                  CircularProgressIndicator(color = Color.White, modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                } else {
                  Text("VERIFY & AUTHENTICATE", style = MonospaceMicro, letterSpacing = 1.sp)
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

      Spacer(modifier = Modifier.height(24.dp))

      // Offline Guest Divider
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
      ) {
        Box(modifier = Modifier.weight(1f).height(0.75.dp).background(SwissHairline))
        Text(
          text = "OR",
          style = MonospaceMicro,
          color = SwissTextTertiary,
          modifier = Modifier.padding(horizontal = 12.dp)
        )
        Box(modifier = Modifier.weight(1f).height(0.75.dp).background(SwissHairline))
      }

      Spacer(modifier = Modifier.height(16.dp))

      // Continue as Guest Button (100% Offline Local Ledger)
      Button(
        onClick = onContinueAsGuest,
        colors = ButtonDefaults.buttonColors(
          containerColor = MaterialTheme.colorScheme.surface,
          contentColor = SwissDark
        ),
        border = androidx.compose.foundation.BorderStroke(0.75.dp, SwissBorder),
        shape = RoundedCornerShape(6.dp),
        modifier = Modifier
          .fillMaxWidth()
          .height(48.dp)
          .testTag("continue_as_guest_button")
      ) {
        Text(
          text = "PROCEED IN LOCAL OFFLINE MODE",
          style = MonospaceMicro,
          color = SwissDark,
          letterSpacing = 1.sp
        )
      }
    }
  }
}
