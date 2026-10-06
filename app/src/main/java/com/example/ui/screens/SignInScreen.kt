package com.example.ui.screens

import android.app.Activity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
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
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
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

enum class EmailAuthMode {
  SIGN_IN,
  CREATE_ACCOUNT
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
  val focusManager = LocalFocusManager.current

  var authMode by remember { mutableStateOf(EmailAuthMode.SIGN_IN) }
  var isLoadingGoogle by remember { mutableStateOf(false) }
  var isLoadingEmail by remember { mutableStateOf(false) }
  var errorMessage by remember { mutableStateOf<String?>(null) }

  // Form Fields
  var nameText by remember { mutableStateOf("") }
  var emailText by remember { mutableStateOf("") }
  var passwordText by remember { mutableStateOf("") }
  var isPasswordVisible by remember { mutableStateOf(false) }

  val isAnyLoading = isLoadingGoogle || isLoadingEmail

  // Form Submission Handler
  val submitEmailAuth: () -> Unit = {
    focusManager.clearFocus()
    errorMessage = null
    val cleanEmail = emailText.trim()
    val cleanPassword = passwordText.trim()

    if (cleanEmail.isEmpty()) {
      errorMessage = "Please enter your email address."
    } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(cleanEmail).matches()) {
      errorMessage = "Invalid email format. Expected format: name@example.com"
    } else if (cleanPassword.isEmpty()) {
      errorMessage = "Please enter your password."
    } else if (cleanPassword.length < 6) {
      errorMessage = "Password must be at least 6 characters."
    } else {
      isLoadingEmail = true
      AuthManager.signInWithEmailPassword(
        email = cleanEmail,
        pass = cleanPassword,
        isSignUp = (authMode == EmailAuthMode.CREATE_ACCOUNT),
        displayName = if (authMode == EmailAuthMode.CREATE_ACCOUNT) nameText.trim().ifEmpty { null } else null,
        scope = scope
      ) { success, err ->
        isLoadingEmail = false
        if (success) {
          onAuthenticated()
        } else if (err != null) {
          errorMessage = err
        }
      }
    }
  }

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
          .size(52.dp)
          .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(8.dp))
          .border(0.75.dp, SwissBorder, RoundedCornerShape(8.dp)),
        contentAlignment = Alignment.Center
      ) {
        Text(
          text = "₹",
          fontSize = 24.sp,
          fontWeight = FontWeight.Bold,
          color = SwissCrimson
        )
      }

      Spacer(modifier = Modifier.height(16.dp))

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

      Spacer(modifier = Modifier.height(28.dp))

      // Error Banner (Inline Red Error Indicator)
      AnimatedVisibility(
        visible = errorMessage != null,
        enter = fadeIn(),
        exit = fadeOut()
      ) {
        if (errorMessage != null) {
          Box(
            modifier = Modifier
              .fillMaxWidth()
              .background(SwissCrimsonLight, RoundedCornerShape(6.dp))
              .border(0.75.dp, SwissCrimson, RoundedCornerShape(6.dp))
              .padding(horizontal = 14.dp, vertical = 10.dp)
              .testTag("auth_error_banner")
          ) {
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
                text = errorMessage!!,
                style = MaterialTheme.typography.bodySmall,
                color = SwissCrimson,
                fontWeight = FontWeight.SemiBold,
                lineHeight = 16.sp
              )
            }
          }
          Spacer(modifier = Modifier.height(16.dp))
        }
      }

      // 1. Google One-Tap Sign-In (Primary Action)
      Button(
        onClick = {
          if (activity != null) {
            isLoadingGoogle = true
            errorMessage = null
            AuthManager.signInWithGoogle(activity, scope) { success, err ->
              isLoadingGoogle = false
              if (success) {
                onAuthenticated()
              } else if (err != null) {
                errorMessage = err
              }
            }
          } else {
            errorMessage = "Android Activity context is unavailable for Google Sign-In."
          }
        },
        enabled = !isAnyLoading,
        colors = ButtonDefaults.buttonColors(
          containerColor = SwissDark,
          contentColor = Color.White
        ),
        shape = RoundedCornerShape(6.dp),
        modifier = Modifier
          .fillMaxWidth()
          .height(52.dp)
          .testTag("google_sign_in_button")
      ) {
        if (isLoadingGoogle) {
          CircularProgressIndicator(
            color = Color.White,
            modifier = Modifier.size(18.dp),
            strokeWidth = 2.dp
          )
        } else {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
          ) {
            // Google G Brand Icon
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
                fontSize = 13.sp
              )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Text(
              text = "Continue with Google",
              style = MaterialTheme.typography.bodyMedium,
              fontWeight = FontWeight.SemiBold,
              letterSpacing = 0.2.sp
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(20.dp))

      // 2. Subtle 1px Divider
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
      ) {
        Box(
          modifier = Modifier
            .weight(1f)
            .height(1.dp)
            .background(SwissHairline)
        )
        Text(
          text = "or continue with email",
          style = MaterialTheme.typography.bodySmall,
          color = SwissTextSecondary,
          modifier = Modifier.padding(horizontal = 12.dp)
        )
        Box(
          modifier = Modifier
            .weight(1f)
            .height(1.dp)
            .background(SwissHairline)
        )
      }

      Spacer(modifier = Modifier.height(20.dp))

      // 3. Email & Password Flow: Segmented Toggle [ Sign In ] | [ Create Account ]
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .background(Color(0xFFF3F3F1), RoundedCornerShape(6.dp))
          .padding(3.dp)
          .testTag("auth_mode_toggle")
      ) {
        val isSignInSelected = authMode == EmailAuthMode.SIGN_IN
        Box(
          modifier = Modifier
            .weight(1f)
            .background(
              if (isSignInSelected) SwissDark else Color.Transparent,
              RoundedCornerShape(4.dp)
            )
            .clickable {
              authMode = EmailAuthMode.SIGN_IN
              errorMessage = null
            }
            .padding(vertical = 9.dp),
          contentAlignment = Alignment.Center
        ) {
          Text(
            text = "Sign In",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (isSignInSelected) FontWeight.Bold else FontWeight.Medium,
            color = if (isSignInSelected) Color.White else SwissDark
          )
        }

        val isCreateSelected = authMode == EmailAuthMode.CREATE_ACCOUNT
        Box(
          modifier = Modifier
            .weight(1f)
            .background(
              if (isCreateSelected) SwissDark else Color.Transparent,
              RoundedCornerShape(4.dp)
            )
            .clickable {
              authMode = EmailAuthMode.CREATE_ACCOUNT
              errorMessage = null
            }
            .padding(vertical = 9.dp),
          contentAlignment = Alignment.Center
        ) {
          Text(
            text = "Create Account",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (isCreateSelected) FontWeight.Bold else FontWeight.Medium,
            color = if (isCreateSelected) Color.White else SwissDark
          )
        }
      }

      Spacer(modifier = Modifier.height(18.dp))

      // Optional "Your Name" Field for Create Account
      AnimatedVisibility(visible = authMode == EmailAuthMode.CREATE_ACCOUNT) {
        Column(modifier = Modifier.fillMaxWidth()) {
          OutlinedTextField(
            value = nameText,
            onValueChange = { nameText = it },
            label = { Text("Your Name (Optional)") },
            placeholder = { Text("e.g. Karan") },
            leadingIcon = {
              Icon(
                Icons.Default.Person,
                contentDescription = null,
                tint = SwissTextSecondary,
                modifier = Modifier.size(18.dp)
              )
            },
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
              focusedBorderColor = SwissDark,
              unfocusedBorderColor = SwissBorder,
              focusedLabelColor = SwissDark
            ),
            shape = RoundedCornerShape(6.dp),
            modifier = Modifier
              .fillMaxWidth()
              .testTag("auth_name_input")
          )
          Spacer(modifier = Modifier.height(12.dp))
        }
      }

      // Email Address Field
      OutlinedTextField(
        value = emailText,
        onValueChange = {
          emailText = it
          if (errorMessage != null) errorMessage = null
        },
        label = { Text("Email Address") },
        placeholder = { Text("name@example.com") },
        leadingIcon = {
          Icon(
            Icons.Default.Email,
            contentDescription = null,
            tint = SwissTextSecondary,
            modifier = Modifier.size(18.dp)
          )
        },
        keyboardOptions = KeyboardOptions(
          keyboardType = KeyboardType.Email,
          imeAction = ImeAction.Next
        ),
        singleLine = true,
        colors = OutlinedTextFieldDefaults.colors(
          focusedBorderColor = SwissDark,
          unfocusedBorderColor = SwissBorder,
          focusedLabelColor = SwissDark
        ),
        shape = RoundedCornerShape(6.dp),
        modifier = Modifier
          .fillMaxWidth()
          .testTag("auth_email_input")
      )

      Spacer(modifier = Modifier.height(12.dp))

      // Password Field with toggle visibility
      OutlinedTextField(
        value = passwordText,
        onValueChange = {
          passwordText = it
          if (errorMessage != null) errorMessage = null
        },
        label = { Text("Password (min 6 characters)") },
        leadingIcon = {
          Icon(
            Icons.Default.Lock,
            contentDescription = null,
            tint = SwissTextSecondary,
            modifier = Modifier.size(18.dp)
          )
        },
        trailingIcon = {
          IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
            Icon(
              imageVector = if (isPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
              contentDescription = if (isPasswordVisible) "Hide password" else "Show password",
              tint = SwissTextSecondary,
              modifier = Modifier.size(18.dp)
            )
          }
        },
        visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(
          keyboardType = KeyboardType.Password,
          imeAction = ImeAction.Done
        ),
        keyboardActions = KeyboardActions(
          onDone = { submitEmailAuth() }
        ),
        singleLine = true,
        colors = OutlinedTextFieldDefaults.colors(
          focusedBorderColor = SwissDark,
          unfocusedBorderColor = SwissBorder,
          focusedLabelColor = SwissDark
        ),
        shape = RoundedCornerShape(6.dp),
        modifier = Modifier
          .fillMaxWidth()
          .testTag("auth_password_input")
      )

      Spacer(modifier = Modifier.height(18.dp))

      // Primary Crimson Red Button: Sign In or Create Account
      Button(
        onClick = submitEmailAuth,
        enabled = !isAnyLoading,
        colors = ButtonDefaults.buttonColors(
          containerColor = SwissCrimson,
          contentColor = Color.White
        ),
        shape = RoundedCornerShape(6.dp),
        modifier = Modifier
          .fillMaxWidth()
          .height(50.dp)
          .testTag("email_auth_submit_button")
      ) {
        if (isLoadingEmail) {
          CircularProgressIndicator(
            color = Color.White,
            modifier = Modifier.size(18.dp),
            strokeWidth = 2.dp
          )
        } else {
          Text(
            text = if (authMode == EmailAuthMode.CREATE_ACCOUNT) "Create Account" else "Sign In",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.5.sp
          )
        }
      }

      Spacer(modifier = Modifier.height(24.dp))

      // Offline Guest Divider
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
      ) {
        Box(
          modifier = Modifier
            .weight(1f)
            .height(1.dp)
            .background(SwissHairline)
        )
        Text(
          text = "OR",
          style = MonospaceMicro,
          color = SwissTextTertiary,
          modifier = Modifier.padding(horizontal = 12.dp)
        )
        Box(
          modifier = Modifier
            .weight(1f)
            .height(1.dp)
            .background(SwissHairline)
        )
      }

      Spacer(modifier = Modifier.height(16.dp))

      // Continue as Guest Button (100% Offline Local Ledger)
      Button(
        onClick = onContinueAsGuest,
        enabled = !isAnyLoading,
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
