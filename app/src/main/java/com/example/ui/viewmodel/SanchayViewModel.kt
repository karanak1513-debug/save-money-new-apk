package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.R
import com.example.data.auth.AuthManager
import com.example.data.local.SanchayDatabase
import com.example.data.model.Channel
import com.example.data.model.ChannelBreakdown
import com.example.data.model.CurrencyFormatter
import com.example.data.model.DailyBarData
import com.example.data.model.ExpenseCategory
import com.example.data.model.Goal
import com.example.data.model.GoalFeasibility
import com.example.data.model.MicroLeakAlert
import com.example.data.model.PacingInfo
import com.example.data.model.TransactionItem
import com.example.data.model.WeeklyAuditSummary
import com.example.data.repository.BackupManager
import com.example.data.repository.SanchayRepository
import com.example.service.ExpenseClassifier
import com.example.service.FinancialAnalyticsEngine
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

enum class NavigationTab(val title: String) {
  DASHBOARD("Dashboard"),
  GOALS("Goals"),
  LEDGER("Ledger")
}

class SanchayViewModel(application: Application) : AndroidViewModel(application) {

  private val prefs = application.getSharedPreferences("sanchay_prefs", Context.MODE_PRIVATE)

  val globalExceptionHandler = CoroutineExceptionHandler { _, throwable ->
    Log.e("SanchayViewModel", "Global coroutine exception caught: ${throwable.message}", throwable)
  }

  private val database = SanchayDatabase.getDatabase(application)
  private val repository = SanchayRepository(database.sanchayDao())

  val goals: StateFlow<List<Goal>> = repository.allGoals.stateIn(
    scope = viewModelScope,
    started = SharingStarted.WhileSubscribed(5000),
    initialValue = emptyList()
  )

  val primaryGoal: StateFlow<Goal?> = repository.primaryGoal.stateIn(
    scope = viewModelScope,
    started = SharingStarted.WhileSubscribed(5000),
    initialValue = null
  )

  val transactions: StateFlow<List<TransactionItem>> = repository.allTransactions.stateIn(
    scope = viewModelScope,
    started = SharingStarted.WhileSubscribed(5000),
    initialValue = emptyList()
  )

  val dailyStreak: StateFlow<Int> = transactions.map { txs ->
    repository.calculateStreak(txs)
  }.stateIn(
    scope = viewModelScope,
    started = SharingStarted.WhileSubscribed(5000),
    initialValue = 0
  )

  val pacingInfo: StateFlow<PacingInfo> = primaryGoal.combine(goals) { primary, allGoals ->
    val goalToCalculate = primary ?: allGoals.firstOrNull()
    repository.calculatePacing(goalToCalculate)
  }.stateIn(
    scope = viewModelScope,
    started = SharingStarted.WhileSubscribed(5000),
    initialValue = PacingInfo(
      dailyPace = 0.0,
      weeklyPace = 0.0,
      monthlyPace = 0.0,
      daysRemaining = 0,
      isExpired = false,
      isCompleted = false
    )
  )

  // 1. Dynamic Predictive Goal Feasibility (Run-Rate AI)
  val goalFeasibility: StateFlow<GoalFeasibility> = combine(primaryGoal, transactions, pacingInfo) { primary, txs, pacing ->
    FinancialAnalyticsEngine.calculateGoalFeasibility(primary, txs, pacing)
  }.stateIn(
    scope = viewModelScope,
    started = SharingStarted.WhileSubscribed(5000),
    initialValue = GoalFeasibility(
      scorePercent = 0,
      isOnTrack = false,
      statusBadgeText = "⚪ Analyzing Trailing Velocity...",
      trailing14dVelocity = 0.0,
      shortfallPerDay = 0.0,
      probabilityPercent = 0
    )
  )

  // 2. Micro-Leak / Anomaly Detection
  val microLeakAlert: StateFlow<MicroLeakAlert> = combine(transactions, pacingInfo) { txs, pacing ->
    FinancialAnalyticsEngine.detectMicroLeaks(txs, pacing)
  }.stateIn(
    scope = viewModelScope,
    started = SharingStarted.WhileSubscribed(5000),
    initialValue = MicroLeakAlert(
      isDetected = false,
      microPercent = 0,
      microTotal = 0.0,
      delayedDays = 0,
      transactionCount = 0,
      badgeText = "",
      weeklyOutflow = 0.0
    )
  )

  // 3. Weekly Audit Brief (Executive Summary)
  val weeklyAuditSummary: StateFlow<WeeklyAuditSummary> = combine(transactions, primaryGoal, pacingInfo) { txs, primary, pacing ->
    FinancialAnalyticsEngine.generateWeeklyAudit(txs, primary, pacing)
  }.stateIn(
    scope = viewModelScope,
    started = SharingStarted.WhileSubscribed(5000),
    initialValue = WeeklyAuditSummary(
      totalInflow = 0.0,
      totalOutflow = 0.0,
      netSavings = 0.0,
      biggestCategory = null,
      biggestCategoryAmount = 0.0,
      recommendedPaceAdjustment = 0.0,
      recommendationText = "Auditing 7-day capital cadence...",
      dateRangeLabel = "CURRENT 7-DAY CYCLE",
      totalTransactionCount = 0
    )
  )

  val channelBreakdown: StateFlow<List<ChannelBreakdown>> = transactions.combine(goals) { txs, _ ->
    repository.calculateChannelBreakdown(txs)
  }.stateIn(
    scope = viewModelScope,
    started = SharingStarted.WhileSubscribed(5000),
    initialValue = listOf(
      ChannelBreakdown(Channel.UPI, 0.0, 0f),
      ChannelBreakdown(Channel.CASH, 0.0, 0f),
      ChannelBreakdown(Channel.OTHER, 0.0, 0f)
    )
  )

  val weeklyBars: StateFlow<List<DailyBarData>> = transactions.combine(goals) { txs, _ ->
    repository.calculateWeeklyBars(txs)
  }.stateIn(
    scope = viewModelScope,
    started = SharingStarted.WhileSubscribed(5000),
    initialValue = emptyList()
  )

  private val _userName = MutableStateFlow(prefs.getString("user_name", "Karan") ?: "Karan")
  val userName: StateFlow<String> = _userName

  private val _isDarkTheme = MutableStateFlow(prefs.getBoolean("is_dark_theme", false))
  val isDarkTheme: StateFlow<Boolean> = _isDarkTheme

  private val _isPrivacyMode = MutableStateFlow(prefs.getBoolean("is_privacy_mode", false))
  val isPrivacyMode: StateFlow<Boolean> = _isPrivacyMode

  fun togglePrivacyMode() {
    val next = !_isPrivacyMode.value
    _isPrivacyMode.value = next
    prefs.edit().putBoolean("is_privacy_mode", next).apply()
  }

  // Default is false so unauthenticated users land on AuthScreen, but once authenticated or chosen guest mode, persistent
  private val _isGuestMode = MutableStateFlow(prefs.getBoolean("is_guest_mode", false))
  val isGuestMode: StateFlow<Boolean> = _isGuestMode

  val authUser: StateFlow<FirebaseUser?> = AuthManager.currentUserState
  val isAuthReady: StateFlow<Boolean> = AuthManager.isAuthReady

  init {
    // Completely non-blocking background initialization on Dispatchers.IO
    viewModelScope.launch(Dispatchers.IO + globalExceptionHandler) {
      try {
        repository.ensureDefaultGoalExists()
      } catch (e: Throwable) {
        Log.e("SanchayViewModel", "Failed to ensure default goal exists", e)
      }
    }
  }

  fun setGuestMode(enabled: Boolean) {
    _isGuestMode.value = enabled
    prefs.edit().putBoolean("is_guest_mode", enabled).apply()
  }

  fun onUserAuthenticated() {
    setGuestMode(false)
    val user = AuthManager.currentUser
    val name = user?.displayName
      ?: user?.email?.substringBefore("@")
    if (!name.isNullOrBlank()) {
      updateUserName(name)
    }
  }

  fun signOut() {
    AuthManager.signOut()
    setGuestMode(false)
  }

  private val _currentTab = MutableStateFlow(NavigationTab.DASHBOARD)
  val currentTab: StateFlow<NavigationTab> = _currentTab

  private val _filterChannelForLedger = MutableStateFlow<Channel?>(null)
  val filterChannelForLedger: StateFlow<Channel?> = _filterChannelForLedger

  fun setTab(tab: NavigationTab) {
    _currentTab.value = tab
  }

  fun filterLedgerByChannel(channel: Channel) {
    _filterChannelForLedger.value = channel
    _currentTab.value = NavigationTab.LEDGER
  }

  fun updateUserName(name: String) {
    _userName.value = name
    prefs.edit().putString("user_name", name).apply()
  }

  fun toggleDarkTheme(isDark: Boolean) {
    _isDarkTheme.value = isDark
    prefs.edit().putBoolean("is_dark_theme", isDark).apply()
  }

  /**
   * Feature 1: Quick-Add Preset Chips
   * Immediately logs amount into selected active goal with today's timestamp.
   */
  fun quickAddPreset(amount: Double) {
    val today = LocalDate.now()
    val targetGoalId = primaryGoal.value?.id ?: goals.value.firstOrNull()?.id
    val item = TransactionItem(
      goalId = targetGoalId,
      amount = amount,
      channel = Channel.CASH,
      note = "Quick Save (+${CurrencyFormatter.formatRupee(amount)})",
      timestamp = System.currentTimeMillis(),
      dateEpochDay = today.toEpochDay(),
      isConfirmed = true,
      category = ExpenseCategory.GENERAL.name
    )
    addTransaction(item)
  }

  fun addTransaction(item: TransactionItem) {
    viewModelScope.launch(Dispatchers.IO + globalExceptionHandler) {
      try {
        val classifiedCategory = if (item.category == ExpenseCategory.GENERAL.name || item.category.isBlank()) {
          ExpenseClassifier.classify(item.merchantOrSender, item.note).name
        } else {
          item.category
        }
        val enrichedItem = item.copy(category = classifiedCategory)
        repository.addTransaction(enrichedItem)
      } catch (e: Throwable) {
        Log.e("SanchayViewModel", "Failed to add transaction", e)
      }
    }
  }

  fun deleteTransaction(item: TransactionItem) {
    viewModelScope.launch(Dispatchers.IO + globalExceptionHandler) {
      try {
        repository.deleteTransaction(item)
      } catch (e: Throwable) {
        Log.e("SanchayViewModel", "Failed to delete transaction", e)
      }
    }
  }

  fun clearAllTransactions() {
    viewModelScope.launch(Dispatchers.IO + globalExceptionHandler) {
      try {
        repository.clearAllTransactions()
      } catch (e: Throwable) {
        Log.e("SanchayViewModel", "Failed to clear transactions", e)
      }
    }
  }

  fun saveGoal(goal: Goal) {
    viewModelScope.launch(Dispatchers.IO + globalExceptionHandler) {
      try {
        if (goal.id == 0L) {
          val count = goals.value.size
          repository.insertGoal(goal.copy(isPrimary = count == 0))
        } else {
          repository.updateGoal(goal)
        }
      } catch (e: Throwable) {
        Log.e("SanchayViewModel", "Failed to save goal", e)
      }
    }
  }

  fun deleteGoal(goal: Goal) {
    viewModelScope.launch(Dispatchers.IO + globalExceptionHandler) {
      try {
        repository.deleteGoal(goal)
      } catch (e: Throwable) {
        Log.e("SanchayViewModel", "Failed to delete goal", e)
      }
    }
  }

  fun setPrimaryGoal(goalId: Long) {
    viewModelScope.launch(Dispatchers.IO + globalExceptionHandler) {
      try {
        repository.setPrimaryGoal(goalId)
      } catch (e: Throwable) {
        Log.e("SanchayViewModel", "Failed to set primary goal", e)
      }
    }
  }

  /**
   * Feature 4: 1-Click Backup & Restore (JSON)
   */
  fun exportBackupJson(): String {
    return BackupManager.exportToJson(
      userName = userName.value,
      goals = goals.value,
      transactions = transactions.value
    )
  }

  fun importBackupJson(jsonStr: String): Boolean {
    val payload = BackupManager.importFromJson(jsonStr) ?: return false
    viewModelScope.launch(Dispatchers.IO + globalExceptionHandler) {
      try {
        repository.restoreBackup(payload)
        if (payload.userName.isNotBlank()) {
          updateUserName(payload.userName)
        }
      } catch (e: Throwable) {
        Log.e("SanchayViewModel", "Failed to restore backup", e)
      }
    }
    return true
  }

  /**
   * Cloud Sync with provisioned Firebase Firestore database
   */
  fun syncToFirestore(onResult: (Boolean, String) -> Unit) {
    val currentUser = AuthManager.currentUser
    if (currentUser == null) {
      onResult(false, "Please sign in to your Google, Email, or Phone account to sync with Firebase Cloud.")
      return
    }
    viewModelScope.launch(Dispatchers.IO + globalExceptionHandler) {
      try {
        val app = runCatching { FirebaseApp.getInstance() }.getOrNull()
        if (app == null) {
          onResult(false, "Firebase service is not initialized.")
          return@launch
        }
        val dbId = getApplication<Application>().getString(R.string.firestore_database_id)
        val firestore = FirebaseFirestore.getInstance(app, dbId)

        val batch = firestore.batch()
        val currentGoals = goals.value
        val currentTxs = transactions.value
        val uid = currentUser.uid

        for (goal in currentGoals) {
          val docRef = firestore.collection("users").document(uid).collection("goals").document(goal.id.toString())
          val data = mapOf(
            "userId" to uid,
            "title" to goal.title,
            "targetAmount" to goal.targetAmount,
            "savedAmount" to goal.savedAmount,
            "deadlineEpochDay" to goal.deadlineEpochDay,
            "frequencyPref" to goal.frequencyPref.name,
            "isPrimary" to goal.isPrimary,
            "createdAt" to com.google.firebase.Timestamp.now()
          )
          batch.set(docRef, data)
        }

        for (tx in currentTxs) {
          val docRef = firestore.collection("users").document(uid).collection("transactions").document(tx.id.toString())
          val data = mapOf(
            "userId" to uid,
            "goalId" to (tx.goalId?.toString() ?: ""),
            "amount" to tx.amount,
            "channel" to tx.channel.name,
            "note" to tx.note,
            "category" to tx.category,
            "dateEpochDay" to tx.dateEpochDay,
            "createdAt" to com.google.firebase.Timestamp.now()
          )
          batch.set(docRef, data)
        }

        batch.commit()
          .addOnSuccessListener {
            onResult(true, "Cloud sync completed successfully (${currentGoals.size} goals, ${currentTxs.size} entries)")
          }
          .addOnFailureListener { e ->
            onResult(false, e.localizedMessage ?: "Sync error")
          }
      } catch (e: Throwable) {
        onResult(false, e.localizedMessage ?: "Firestore initialization error")
      }
    }
  }
}
