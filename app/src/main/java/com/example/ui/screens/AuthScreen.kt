package com.example.ui.screens

import android.app.Activity
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.draw.clip
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
import com.example.data.auth.AuthManager
import com.example.ui.components.ambientMeshBackground
import com.example.ui.components.frostedGlass
import com.example.ui.theme.PlusJakartaSans
import com.example.ui.theme.SectionHeaderMedium
import com.example.ui.theme.SlateHeader
import com.example.ui.theme.SwissBorder
import com.example.ui.theme.SwissCrimson
import com.example.ui.theme.SwissDark
import com.example.ui.theme.SwissHairline
import com.example.ui.theme.SwissTextSecondary
import com.example.ui.theme.SwissTextTertiary
import com.example.ui.viewmodel.AuthViewModel
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.launch

private const val TAG = "AuthScreen"
private const val GOOGLE_WEB_CLIENT_ID = "41954701104-1eqfhs3s6p6naknnui85ab2hubaki4dc.apps.googleusercontent.com"

/**
 * Luxury Frosted Glassmorphism Auth UI connected directly to live Firebase.
 * - Primary CTA: "Continue with Google" via official GoogleSignInClient with explicit Web Client ID
 * - Robust result handling: clears stale session with signOut() before launching, inspects ApiException codes
 * - Secondary: Collapsible Email & Password accordion binding directly to FirebaseAuth
 * - Immediate state update on sign in and routing directly to DashboardScreen
 */
@Composable
fun AuthScreen(
  onAuthenticated: () -> Unit,
  onContinueOffline: () -> Unit = {},
  authViewModel: AuthViewModel = viewModel(),
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  val focusManager = LocalFocusManager.current
  val snackbarHostState = remember { SnackbarHostState() }

  val isLoadingGoogle by authViewModel.isLoadingGoogle.collectAsStateWithLifecycle()
  val isLoadingEmail by authViewModel.isLoadingEmail.collectAsStateWithLifecycle()
  val errorMessage by authViewModel.errorMessage.collectAsStateWithLifecycle()

  // Google Sign-In Client configuration with official Web Client ID
  val gso = remember {
    GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
      .requestIdToken(GOOGLE_WEB_CLIENT_ID)
      .requestEmail()
      .build()
  }
  val googleSignInClient = remember(context) { GoogleSignIn.getClient(context, gso) }

  // Activity Result Launcher for Google Sign-In with precise exception handling
  val googleSignInLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.StartActivityForResult()
  ) { result ->
    val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
    try {
      val account = task.getResult(ApiException::class.java)
      val idToken = account?.idToken
      if (!idToken.isNullOrEmpty()) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        FirebaseAuth.getInstance().signInWithCredential(credential)
          .addOnSuccessListener { authResult ->
            val user = authResult.user
            if (user != null) {
              AuthManager.onFirebaseUserAuthenticated(user)
            }
            authViewModel.setGoogleLoading(false)
            // Direct transition to Dashboard
            onAuthenticated()
          }
          .addOnFailureListener { e ->
            authViewModel.setGoogleLoading(false)
            Log.e(TAG, "Firebase credential sign-in error: ${e.message}", e)
            authViewModel.setErrorMessage(e.localizedMessage ?: "Firebase authentication failed.")
          }
      } else {
        authViewModel.setGoogleLoading(false)
        authViewModel.setErrorMessage("Failed to obtain Google ID token. Please try again.")
      }
    } catch (e: ApiException) {
      authViewModel.setGoogleLoading(false)
      Log.w(TAG, "Google Sign-In ApiException: code=${e.statusCode}, message=${e.message}")
      // Do NOT treat ApiException (like 10 or 12500) as a simple user cancellation.
      // Display or log the exact error: "Google Sign-In Error Code: ${e.statusCode}"
      if (e.statusCode != 12501 && e.statusCode != 16) {
        authViewModel.setErrorMessage("Google Sign-In Error Code: ${e.statusCode}")
      } else {
        // User explicitly tapped outside or dismissed account picker
        authViewModel.setErrorMessage("Sign-in was cancelled.")
      }
    } catch (e: Throwable) {
      authViewModel.setGoogleLoading(false)
      Log.e(TAG, "Unexpected Google Sign-In error: ${e.message}", e)
      authViewModel.setErrorMessage(e.localizedMessage ?: "Google Sign-In failed.")
    }
  }

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
    containerColor = Color.Transparent,
    modifier = modifier.fillMaxSize()
  ) { paddingValues ->
    Box(
      modifier = Modifier
        .fillMaxSize()
        .ambientMeshBackground()
        .padding(paddingValues)
        .testTag("auth_screen"),
      contentAlignment = Alignment.Center
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 22.dp)
          .frostedGlass(shape = RoundedCornerShape(24.dp), elevation = 6.dp)
          .verticalScroll(rememberScrollState())
          .padding(horizontal = 24.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
      ) {
        // Geometric Emblem Accent
        Box(
          modifier = Modifier
            .size(48.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFFF1F5F9))
            .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(14.dp)),
          contentAlignment = Alignment.Center
        ) {
          Box(
            modifier = Modifier
              .size(10.dp)
              .background(SwissCrimson, CircleShape)
          )
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Header: "SANCHAY" — Minimalist Wealth Management
        Text(
          text = "SANCHAY",
          fontFamily = PlusJakartaSans,
          color = SwissDark,
          fontWeight = FontWeight.Black,
          fontSize = 28.sp,
          letterSpacing = 6.sp,
          modifier = Modifier.testTag("auth_title")
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
          text = "Minimalist Wealth Management",
          style = SectionHeaderMedium,
          color = SlateHeader,
          fontWeight = FontWeight.Medium,
          fontSize = 13.sp
        )

        Spacer(modifier = Modifier.height(32.dp))

        // PRIMARY CTA: "Continue with Google"
        Button(
          onClick = {
            authViewModel.setGoogleLoading(true)
            authViewModel.clearError()
            // Clear any stuck or stale account sessions so the account picker always opens cleanly
            googleSignInClient.signOut().addOnCompleteListener {
              val signInIntent = googleSignInClient.signInIntent
              googleSignInLauncher.launch(signInIntent)
            }
          },
          enabled = !isAnyLoading,
          colors = ButtonDefaults.buttonColors(
            containerColor = SwissDark,
            contentColor = Color.White
          ),
          shape = RoundedCornerShape(14.dp),
          modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
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
              text = "Connecting Google...",
              fontFamily = PlusJakartaSans,
              fontWeight = FontWeight.SemiBold,
              color = Color.White,
              fontSize = 13.sp
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
                fontFamily = PlusJakartaSans,
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
            fontFamily = PlusJakartaSans,
            fontWeight = FontWeight.Medium,
            color = SwissTextTertiary,
            modifier = Modifier.padding(horizontal = 12.dp),
            fontSize = 11.sp
          )
          Box(modifier = Modifier.weight(1f).height(1.dp).background(SwissHairline))
        }

        Spacer(modifier = Modifier.height(16.dp))

        // SECONDARY: Minimalist Email & Password Accordion
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFFF1F5F9))
            .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(14.dp))
            .clickable { showEmailAuth = !showEmailAuth }
            .padding(horizontal = 16.dp, vertical = 14.dp)
            .testTag("email_auth_collapsible_toggle"),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(
            text = "Sign in with Email & Password",
            fontFamily = PlusJakartaSans,
            fontWeight = FontWeight.Medium,
            color = SwissDark,
            fontSize = 13.sp
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
                .background(Color(0xFFF3F4F6), RoundedCornerShape(8.dp))
                .padding(3.dp)
            ) {
              Box(
                modifier = Modifier
                  .weight(1f)
                  .background(
                    if (!isCreateAccountMode) Color.White else Color.Transparent,
                    RoundedCornerShape(6.dp)
                  )
                  .clickable { isCreateAccountMode = false }
                  .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center
              ) {
                Text(
                  text = "Sign In",
                  fontFamily = PlusJakartaSans,
                  color = if (!isCreateAccountMode) SwissDark else SwissTextSecondary,
                  fontWeight = if (!isCreateAccountMode) FontWeight.Bold else FontWeight.Medium,
                  fontSize = 12.sp
                )
              }

              Box(
                modifier = Modifier
                  .weight(1f)
                  .background(
                    if (isCreateAccountMode) Color.White else Color.Transparent,
                    RoundedCornerShape(6.dp)
                  )
                  .clickable { isCreateAccountMode = true }
                  .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center
              ) {
                Text(
                  text = "Create Account",
                  fontFamily = PlusJakartaSans,
                  color = if (isCreateAccountMode) SwissDark else SwissTextSecondary,
                  fontWeight = if (isCreateAccountMode) FontWeight.Bold else FontWeight.Medium,
                  fontSize = 12.sp
                )
              }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Email Input
            OutlinedTextField(
              value = emailInput,
              onValueChange = { emailInput = it },
              label = { Text("Email", fontFamily = PlusJakartaSans, fontSize = 12.sp) },
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
              shape = RoundedCornerShape(12.dp),
              modifier = Modifier
                .fillMaxWidth()
                .testTag("auth_email_input")
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Password Input
            OutlinedTextField(
              value = passwordInput,
              onValueChange = { passwordInput = it },
              label = { Text("Password", fontFamily = PlusJakartaSans, fontSize = 12.sp) },
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
              shape = RoundedCornerShape(12.dp),
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
              shape = RoundedCornerShape(12.dp),
              modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
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
                  fontFamily = PlusJakartaSans,
                  fontWeight = FontWeight.SemiBold,
                  fontSize = 14.sp
                )
              }
            }
          }
        }

        Spacer(modifier = Modifier.height(28.dp))

        // Guest / Local Mode Link
        Text(
          text = "Continue without account (Local Mode)",
          fontFamily = PlusJakartaSans,
          color = SwissTextSecondary,
          fontWeight = FontWeight.Medium,
          fontSize = 12.sp,
          modifier = Modifier
            .clickable { onContinueOffline() }
            .padding(8.dp)
            .testTag("continue_offline_link")
        )
      }
    }
  }
}
