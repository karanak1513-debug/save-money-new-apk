package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.R
import com.example.data.auth.AuthManager
import com.example.data.local.SanchayDatabase
import com.example.data.model.Channel
import com.example.data.model.ChannelBreakdown
import com.example.data.model.CurrencyFormatter
import com.example.data.model.DailyBarData
import com.example.data.model.Goal
import com.example.data.model.PacingInfo
import com.example.data.model.TransactionItem
import com.example.data.repository.BackupManager
import com.example.data.repository.SanchayRepository
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
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
  private val database = SanchayDatabase.getDatabase(application, viewModelScope)
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

  private val _isGuestMode = MutableStateFlow(prefs.getBoolean("is_guest_mode", false))
  val isGuestMode: StateFlow<Boolean> = _isGuestMode

  val authUser: StateFlow<FirebaseUser?> = AuthManager.currentUserState

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
      isConfirmed = true
    )
    addTransaction(item)
  }

  fun addTransaction(item: TransactionItem) {
    viewModelScope.launch {
      repository.addTransaction(item)
    }
  }

  fun deleteTransaction(item: TransactionItem) {
    viewModelScope.launch {
      repository.deleteTransaction(item)
    }
  }

  fun clearAllTransactions() {
    viewModelScope.launch {
      repository.clearAllTransactions()
    }
  }

  fun saveGoal(goal: Goal) {
    viewModelScope.launch {
      if (goal.id == 0L) {
        val count = goals.value.size
        repository.insertGoal(goal.copy(isPrimary = count == 0))
      } else {
        repository.updateGoal(goal)
      }
    }
  }

  fun deleteGoal(goal: Goal) {
    viewModelScope.launch {
      repository.deleteGoal(goal)
    }
  }

  fun setPrimaryGoal(goalId: Long) {
    viewModelScope.launch {
      repository.setPrimaryGoal(goalId)
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
    viewModelScope.launch {
      repository.restoreBackup(payload)
      if (payload.userName.isNotBlank()) {
        updateUserName(payload.userName)
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
    viewModelScope.launch {
      try {
        val app = FirebaseApp.getInstance()
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
      } catch (e: Exception) {
        onResult(false, e.localizedMessage ?: "Firestore initialization error")
      }
    }
  }
}
