package com.example.ui.screens

import android.app.Activity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp
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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.theme.MonospaceMicro
import com.example.ui.theme.MonospaceSmall
import com.example.ui.theme.SwissBorder
import com.example.ui.theme.SwissCrimson
import com.example.ui.theme.SwissDark
import com.example.ui.theme.SwissHairline
import com.example.ui.theme.SwissTextSecondary
import com.example.ui.theme.SwissTextTertiary
import com.example.ui.viewmodel.AuthViewModel
import kotlinx.coroutines.launch

/**
 * Production-ready Swiss Minimalist Auth UI connected directly to live Firebase.
 * - Primary CTA: "Continue with Google" via Android Credential Manager One-Tap
 * - Secondary: Collapsible Email & Password accordion binding directly to FirebaseAuth
 * - Non-blocking Snackbars on failure/cancel (zero offline mode downgrade)
 */
@Composable
fun AuthScreen(
  onAuthenticated: () -> Unit,
  onContinueOffline: () -> Unit = {},
  authViewModel: AuthViewModel = viewModel(),
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  val activity = context as? Activity
  val scope = rememberCoroutineScope()
  val focusManager = LocalFocusManager.current
  val snackbarHostState = remember { SnackbarHostState() }

  val isLoadingGoogle by authViewModel.isLoadingGoogle.collectAsStateWithLifecycle()
  val isLoadingEmail by authViewModel.isLoadingEmail.collectAsStateWithLifecycle()
  val errorMessage by authViewModel.errorMessage.collectAsStateWithLifecycle()

  // Collapsible Email & Password Section
  var showEmailAuth by remember { mutableStateOf(false) }
  var isCreateAccountMode by remember { mutableStateOf(false) }
  var emailInput by remember { mutableStateOf("") }
  var passwordInput by remember { mutableStateOf("") }
  var isPasswordVisible by remember { mutableStateOf(false) }

  val isAnyLoading = isLoadingGoogle || isLoadingEmail

  // Show clean non-blocking snackbar on error/cancel instead of downgrading the app to offline mode
  LaunchedEffect(errorMessage) {
    errorMessage?.let { msg ->
      snackbarHostState.showSnackbar(msg)
      authViewModel.clearError()
    }
  }

  Scaffold(
    snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
    containerColor = Color(0xFFFFFFFF),
    modifier = modifier.fillMaxSize()
  ) { paddingValues ->
    Box(
      modifier = Modifier
        .fillMaxSize()
        .padding(paddingValues)
        .testTag("auth_screen"),
      contentAlignment = Alignment.Center
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .verticalScroll(rememberScrollState())
          .padding(horizontal = 28.dp, vertical = 36.dp),
        horizontalAlignment = Alignment.CenterHorizontally
      ) {
        // Geometric Emblem Accent
        Box(
          modifier = Modifier
            .size(48.dp)
            .background(Color(0xFFF9FAFB), RoundedCornerShape(8.dp))
            .border(1.dp, SwissBorder, RoundedCornerShape(8.dp)),
          contentAlignment = Alignment.Center
        ) {
          Box(
            modifier = Modifier
              .size(10.dp)
              .background(SwissCrimson, CircleShape)
          )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Header: "SANCHAY" — Minimalist Wealth Management
        Text(
          text = "SANCHAY",
          color = SwissDark,
          fontWeight = FontWeight.Black,
          fontSize = 28.sp,
          letterSpacing = 8.sp,
          modifier = Modifier.testTag("auth_title")
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
          text = "Minimalist Wealth Management",
          style = MaterialTheme.typography.bodyMedium,
          color = SwissTextSecondary,
          fontWeight = FontWeight.Medium,
          fontSize = 13.sp
        )

        Spacer(modifier = Modifier.height(36.dp))

        // PRIMARY CTA: "Continue with Google"
        Button(
          onClick = {
            if (activity != null) {
              authViewModel.signInWithGoogle(activity) {
                onAuthenticated()
              }
            } else {
              scope.launch {
                snackbarHostState.showSnackbar("Android Activity context is unavailable.")
              }
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
            .height(50.dp)
            .testTag("continue_with_google_button")
        ) {
          if (isLoadingGoogle) {
            CircularProgressIndicator(
              modifier = Modifier.size(18.dp),
              color = Color.White,
              strokeWidth = 2.dp
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
              text = "CONNECTING GOOGLE...",
              style = MonospaceSmall,
              fontWeight = FontWeight.Bold,
              color = Color.White,
              fontSize = 12.sp
            )
          } else {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.Center
            ) {
              // Minimalist Google "G" badge
              Box(
                modifier = Modifier
                  .size(18.dp)
                  .background(Color.White, CircleShape),
                contentAlignment = Alignment.Center
              ) {
                Text(
                  text = "G",
                  color = SwissDark,
                  fontWeight = FontWeight.Bold,
                  fontSize = 11.sp
                )
              }
              Spacer(modifier = Modifier.width(10.dp))
              Text(
                text = "Continue with Google",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = Color.White,
                fontSize = 14.sp
              )
            }
          }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Hairline Divider
        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Box(modifier = Modifier.weight(1f).height(1.dp).background(SwissHairline))
          Text(
            text = "OR",
            style = MonospaceMicro,
            color = SwissTextTertiary,
            modifier = Modifier.padding(horizontal = 12.dp),
            fontSize = 10.sp
          )
          Box(modifier = Modifier.weight(1f).height(1.dp).background(SwissHairline))
        }

        Spacer(modifier = Modifier.height(16.dp))

        // SECONDARY: Minimalist Email & Password Accordion
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFF9FAFB), RoundedCornerShape(6.dp))
            .border(1.dp, SwissBorder, RoundedCornerShape(6.dp))
            .clickable { showEmailAuth = !showEmailAuth }
            .padding(horizontal = 14.dp, vertical = 12.dp)
            .testTag("email_auth_collapsible_toggle"),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(
            text = "Sign in with Email & Password",
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium,
            color = SwissDark,
            fontSize = 12.sp
          )
          Icon(
            imageVector = if (showEmailAuth) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
            contentDescription = null,
            tint = SwissTextSecondary
          )
        }

        // Collapsible Form binding directly to live FirebaseAuth
        AnimatedVisibility(
          visible = showEmailAuth,
          enter = fadeIn() + expandVertically(),
          exit = fadeOut() + shrinkVertically()
        ) {
          Column(
            modifier = Modifier
              .fillMaxWidth()
              .padding(top = 14.dp)
          ) {
            // Mode Switch: Sign In vs Create Account
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFFF3F4F6), RoundedCornerShape(6.dp))
                .padding(3.dp)
            ) {
              Box(
                modifier = Modifier
                  .weight(1f)
                  .background(
                    if (!isCreateAccountMode) Color.White else Color.Transparent,
                    RoundedCornerShape(4.dp)
                  )
                  .clickable { isCreateAccountMode = false }
                  .padding(vertical = 7.dp),
                contentAlignment = Alignment.Center
              ) {
                Text(
                  text = "SIGN IN",
                  style = MonospaceMicro,
                  color = if (!isCreateAccountMode) SwissDark else SwissTextSecondary,
                  fontWeight = if (!isCreateAccountMode) FontWeight.Bold else FontWeight.Normal
                )
              }

              Box(
                modifier = Modifier
                  .weight(1f)
                  .background(
                    if (isCreateAccountMode) Color.White else Color.Transparent,
                    RoundedCornerShape(4.dp)
                  )
                  .clickable { isCreateAccountMode = true }
                  .padding(vertical = 7.dp),
                contentAlignment = Alignment.Center
              ) {
                Text(
                  text = "CREATE ACCOUNT",
                  style = MonospaceMicro,
                  color = if (isCreateAccountMode) SwissDark else SwissTextSecondary,
                  fontWeight = if (isCreateAccountMode) FontWeight.Bold else FontWeight.Normal
                )
              }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Email Input
            OutlinedTextField(
              value = emailInput,
              onValueChange = { emailInput = it },
              label = { Text("Email", style = MonospaceMicro) },
              singleLine = true,
              keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Next
              ),
              colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color.White,
                focusedBorderColor = SwissDark,
                unfocusedBorderColor = SwissBorder,
                cursorColor = SwissDark
              ),
              shape = RoundedCornerShape(6.dp),
              modifier = Modifier
                .fillMaxWidth()
                .testTag("auth_email_input")
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Password Input
            OutlinedTextField(
              value = passwordInput,
              onValueChange = { passwordInput = it },
              label = { Text("Password", style = MonospaceMicro) },
              singleLine = true,
              visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
              keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Done
              ),
              keyboardActions = KeyboardActions(
                onDone = { focusManager.clearFocus() }
              ),
              trailingIcon = {
                IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                  Icon(
                    imageVector = if (isPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                    contentDescription = null,
                    tint = SwissTextSecondary,
                    modifier = Modifier.size(18.dp)
                  )
                }
              },
              colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color.White,
                focusedBorderColor = SwissDark,
                unfocusedBorderColor = SwissBorder,
                cursorColor = SwissDark
              ),
              shape = RoundedCornerShape(6.dp),
              modifier = Modifier
                .fillMaxWidth()
                .testTag("auth_password_input")
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Submit Button
            Button(
              onClick = {
                focusManager.clearFocus()
                authViewModel.signInWithEmail(
                  email = emailInput,
                  pass = passwordInput,
                  isSignUp = isCreateAccountMode
                ) {
                  onAuthenticated()
                }
              },
              enabled = !isAnyLoading,
              colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF1F2937),
                contentColor = Color.White
              ),
              shape = RoundedCornerShape(6.dp),
              modifier = Modifier
                .fillMaxWidth()
                .height(46.dp)
                .testTag("email_auth_submit_button")
            ) {
              if (isLoadingEmail) {
                CircularProgressIndicator(
                  modifier = Modifier.size(16.dp),
                  color = Color.White,
                  strokeWidth = 2.dp
                )
              } else {
                Text(
                  text = if (isCreateAccountMode) "Create Account" else "Sign In",
                  style = MaterialTheme.typography.bodyMedium,
                  fontWeight = FontWeight.SemiBold
                )
              }
            }
          }
        }

        Spacer(modifier = Modifier.height(28.dp))

        // Guest / Local Mode Link
        Text(
          text = "Continue without account (Local Mode)",
          style = MonospaceMicro,
          color = SwissTextSecondary,
          fontWeight = FontWeight.Medium,
          fontSize = 11.sp,
          modifier = Modifier
            .clickable { onContinueOffline() }
            .padding(8.dp)
            .testTag("continue_offline_link")
        )
      }
    }
  }
}
