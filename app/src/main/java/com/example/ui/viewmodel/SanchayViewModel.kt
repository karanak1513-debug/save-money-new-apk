package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.SanchayDatabase
import com.example.data.model.Channel
import com.example.data.model.ChannelBreakdown
import com.example.data.model.DailyBarData
import com.example.data.model.Goal
import com.example.data.model.PacingInfo
import com.example.data.model.TransactionItem
import com.example.data.repository.SanchayRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

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
}
