package com.example.ui

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.ListAlt
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.components.AddEntryBottomSheet
import com.example.ui.components.QuickPasteUpiSheet
import com.example.ui.components.SettingsModal
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.GoalManagerScreen
import com.example.ui.screens.HistoryLedgerScreen
import com.example.ui.screens.SignInScreen
import com.example.ui.screens.SplashScreen
import com.example.ui.theme.MonospaceMicro
import com.example.ui.theme.SanchayTheme
import com.example.ui.theme.SwissBorder
import com.example.ui.theme.SwissCrimson
import com.example.ui.theme.SwissDark
import com.example.ui.theme.SwissTextSecondary
import com.example.ui.theme.SwissTextTertiary
import com.example.ui.viewmodel.NavigationTab
import com.example.ui.viewmodel.SanchayViewModel

enum class AppScreen {
  SPLASH,
  SIGN_IN,
  MAIN
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SanchayApp(
  viewModel: SanchayViewModel = viewModel()
) {
  val goals by viewModel.goals.collectAsStateWithLifecycle()
  val primaryGoal by viewModel.primaryGoal.collectAsStateWithLifecycle()
  val pacingInfo by viewModel.pacingInfo.collectAsStateWithLifecycle()
  val channelBreakdown by viewModel.channelBreakdown.collectAsStateWithLifecycle()
  val weeklyBars by viewModel.weeklyBars.collectAsStateWithLifecycle()
  val transactions by viewModel.transactions.collectAsStateWithLifecycle()
  val dailyStreak by viewModel.dailyStreak.collectAsStateWithLifecycle()
  val userName by viewModel.userName.collectAsStateWithLifecycle()
  val isDarkTheme by viewModel.isDarkTheme.collectAsStateWithLifecycle()
  val currentTab by viewModel.currentTab.collectAsStateWithLifecycle()
  val filterChannel by viewModel.filterChannelForLedger.collectAsStateWithLifecycle()
  val authUser by viewModel.authUser.collectAsStateWithLifecycle()
  val isGuestMode by viewModel.isGuestMode.collectAsStateWithLifecycle()

  var appScreen by remember { mutableStateOf(AppScreen.SPLASH) }
  var showAddEntrySheet by remember { mutableStateOf(false) }
  var showQuickPasteSheet by remember { mutableStateOf(false) }
  var showSettingsModal by remember { mutableStateOf(false) }

  SanchayTheme(darkTheme = isDarkTheme) {
    when (appScreen) {
      AppScreen.SPLASH -> {
        SplashScreen(
          onSplashFinished = {
            if (authUser != null || isGuestMode) {
              appScreen = AppScreen.MAIN
            } else {
              appScreen = AppScreen.SIGN_IN
            }
          }
        )
      }

      AppScreen.SIGN_IN -> {
        if (isGuestMode) {
          BackHandler {
            appScreen = AppScreen.MAIN
          }
        }

        SignInScreen(
          onAuthenticated = {
            viewModel.onUserAuthenticated()
            appScreen = AppScreen.MAIN
          },
          onContinueAsGuest = {
            viewModel.setGuestMode(true)
            appScreen = AppScreen.MAIN
          }
        )
      }

      AppScreen.MAIN -> {
        // Back handling: If on secondary tab, return to Dashboard first
        if (currentTab != NavigationTab.DASHBOARD) {
          BackHandler {
            viewModel.setTab(NavigationTab.DASHBOARD)
          }
        }

        Scaffold(
          modifier = Modifier.fillMaxSize(),
          containerColor = MaterialTheme.colorScheme.background,
          bottomBar = {
            // Strict Swiss minimalist bottom navigation bar
            Column(
              modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
                .border(1.dp, SwissBorder)
                .navigationBarsPadding()
            ) {
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .height(58.dp)
                  .padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
              ) {
                // Tab 1: Dashboard
                SwissNavItem(
                  title = "Dashboard",
                  isSelected = currentTab == NavigationTab.DASHBOARD,
                  icon = Icons.Default.Home,
                  onClick = { viewModel.setTab(NavigationTab.DASHBOARD) },
                  testTag = "nav_dashboard"
                )

                // Tab 2: Goals
                SwissNavItem(
                  title = "Goals",
                  isSelected = currentTab == NavigationTab.GOALS,
                  icon = Icons.Default.Flag,
                  onClick = { viewModel.setTab(NavigationTab.GOALS) },
                  testTag = "nav_goals"
                )

                // Tab 3: Ledger
                SwissNavItem(
                  title = "Ledger",
                  isSelected = currentTab == NavigationTab.LEDGER,
                  icon = Icons.Default.ListAlt,
                  onClick = { viewModel.setTab(NavigationTab.LEDGER) },
                  testTag = "nav_ledger"
                )
              }
            }
          }
        ) { paddingValues ->
          Box(
            modifier = Modifier
              .fillMaxSize()
              .padding(paddingValues)
          ) {
            when (currentTab) {
              NavigationTab.DASHBOARD -> {
                DashboardScreen(
                  userName = userName,
                  dailyStreak = dailyStreak,
                  primaryGoal = primaryGoal,
                  pacingInfo = pacingInfo,
                  channelBreakdown = channelBreakdown,
                  weeklyBars = weeklyBars,
                  hasTransactions = transactions.isNotEmpty(),
                  onQuickAddPreset = { amount ->
                    viewModel.quickAddPreset(amount)
                  },
                  onOpenQuickPaste = { showQuickPasteSheet = true },
                  onOpenSettings = { showSettingsModal = true },
                  onOpenAddEntry = { showAddEntrySheet = true },
                  onSwitchGoal = { viewModel.setTab(NavigationTab.GOALS) },
                  onChannelFilterClick = { ch ->
                    viewModel.filterLedgerByChannel(ch)
                  }
                )
              }

              NavigationTab.GOALS -> {
                GoalManagerScreen(
                  goals = goals,
                  onSaveGoal = { g -> viewModel.saveGoal(g) },
                  onDeleteGoal = { g -> viewModel.deleteGoal(g) },
                  onSetPrimary = { id -> viewModel.setPrimaryGoal(id) }
                )
              }

              NavigationTab.LEDGER -> {
                HistoryLedgerScreen(
                  transactions = transactions,
                  onUpdateTransaction = { tx -> viewModel.addTransaction(tx) },
                  onDeleteTransaction = { tx -> viewModel.deleteTransaction(tx) },
                  onClearLedger = { viewModel.clearAllTransactions() },
                  initialChannelFilter = filterChannel
                )
              }
            }

            // Quick Add Bottom Sheet
            if (showAddEntrySheet) {
              AddEntryBottomSheet(
                goals = goals,
                selectedGoalId = primaryGoal?.id,
                onDismiss = { showAddEntrySheet = false },
                onSaveEntry = { item ->
                  viewModel.addTransaction(item)
                }
              )
            }

            // Quick Paste UPI / SMS Smart Sheet
            if (showQuickPasteSheet) {
              QuickPasteUpiSheet(
                goals = goals,
                selectedGoalId = primaryGoal?.id,
                onDismiss = { showQuickPasteSheet = false },
                onSaveTransaction = { item ->
                  viewModel.addTransaction(item)
                }
              )
            }

            // Settings Modal
            if (showSettingsModal) {
              SettingsModal(
                currentUserName = userName,
                isDarkTheme = isDarkTheme,
                currentUser = authUser,
                isGuestMode = isGuestMode,
                onUpdateUserName = { newName -> viewModel.updateUserName(newName) },
                onToggleDarkTheme = { dark -> viewModel.toggleDarkTheme(dark) },
                onExportJson = { viewModel.exportBackupJson() },
                onImportJson = { jsonStr -> viewModel.importBackupJson(jsonStr) },
                onSyncFirestore = { callback -> viewModel.syncToFirestore(callback) },
                onSignOut = {
                  viewModel.signOut()
                  appScreen = AppScreen.SIGN_IN
                },
                onOpenSignIn = {
                  appScreen = AppScreen.SIGN_IN
                },
                onDismiss = { showSettingsModal = false }
              )
            }
          }
        }
      }
    }
  }
}

@Composable
private fun SwissNavItem(
  title: String,
  isSelected: Boolean,
  icon: androidx.compose.ui.graphics.vector.ImageVector,
  onClick: () -> Unit,
  testTag: String
) {
  Column(
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.Center,
    modifier = Modifier
      .clickable { onClick() }
      .padding(horizontal = 18.dp, vertical = 6.dp)
      .testTag(testTag)
  ) {
    Icon(
      imageVector = icon,
      contentDescription = title,
      tint = if (isSelected) SwissDark else SwissTextTertiary,
      modifier = Modifier.size(19.dp)
    )
    Spacer(modifier = Modifier.height(3.dp))
    Text(
      text = title.uppercase(),
      style = MonospaceMicro,
      color = if (isSelected) SwissDark else SwissTextTertiary,
      fontSize = 9.sp,
      letterSpacing = 1.sp
    )
    if (isSelected) {
      Box(
        modifier = Modifier
          .padding(top = 3.dp)
          .size(width = 16.dp, height = 2.dp)
          .background(SwissCrimson)
      )
    } else {
      Spacer(modifier = Modifier.height(5.dp))
    }
  }
}
