package com.example.ui.screens

import android.app.DatePickerDialog
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CurrencyFormatter
import com.example.data.model.FrequencyPref
import com.example.data.model.Goal
import com.example.ui.theme.MonospaceBody
import com.example.ui.theme.MonospaceHeadline
import com.example.ui.theme.MonospaceSmall
import com.example.ui.theme.SwissBorder
import com.example.ui.theme.SwissCrimson
import com.example.ui.theme.SwissCrimsonLight
import com.example.ui.theme.SwissDark
import com.example.ui.theme.SwissTextSecondary
import com.example.ui.theme.SwissTextTertiary
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.max

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoalManagerScreen(
  goals: List<Goal>,
  onSaveGoal: (Goal) -> Unit,
  onDeleteGoal: (Goal) -> Unit,
  onSetPrimary: (Long) -> Unit,
  modifier: Modifier = Modifier
) {
  var showGoalSheet by remember { mutableStateOf(false) }
  var editingGoal by remember { mutableStateOf<Goal?>(null) }

  Scaffold(
    modifier = modifier.fillMaxSize(),
    containerColor = MaterialTheme.colorScheme.background,
    floatingActionButton = {
      ExtendedFloatingActionButton(
        onClick = {
          editingGoal = null
          showGoalSheet = true
        },
        containerColor = SwissDark,
        contentColor = Color.White,
        shape = RoundedCornerShape(28.dp),
        modifier = Modifier
          .padding(bottom = 12.dp)
          .testTag("create_goal_fab")
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(6.dp),
          modifier = Modifier.padding(horizontal = 8.dp)
        ) {
          Icon(
            imageVector = Icons.Default.Add,
            contentDescription = "New Goal",
            modifier = Modifier.size(20.dp)
          )
          Text(
            text = "Create Goal",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold
          )
        }
      }
    }
  ) { innerPadding ->
    LazyColumn(
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding)
        .testTag("goal_manager_list"),
      contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
      verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
      // Header Section
      item {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Column {
            Text(
              text = "PORTFOLIO TARGETS",
              style = MaterialTheme.typography.labelSmall,
              fontWeight = FontWeight.Bold,
              color = SwissCrimson,
              letterSpacing = 1.2.sp,
              fontSize = 10.sp
            )
            Text(
              text = "Goal Manager",
              style = MaterialTheme.typography.headlineLarge,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.onSurface,
              letterSpacing = (-0.3).sp
            )
          }

          Text(
            text = "${goals.size} ACTIVE",
            style = MonospaceSmall,
            fontWeight = FontWeight.Bold,
            color = SwissTextTertiary,
            fontSize = 11.sp
          )
        }
      }

      // Active Goals List:
      // Cards showing each active goal with target date countdown ("42 days remaining").
      // Inline action triggers: Edit (Pencil) and Delete (Trash).
      items(goals, key = { it.id }) { goal ->
        val todayEpoch = LocalDate.now().toEpochDay()
        val daysRemaining = max(0L, goal.deadlineEpochDay - todayEpoch)
        val progress = goal.progressFraction
        val animatedProgress by animateFloatAsState(
          targetValue = progress,
          animationSpec = tween(durationMillis = 600),
          label = "goal_item_progress_${goal.id}"
        )

        // Frequency run-rate calculation
        val dailyPace = if (daysRemaining > 0) goal.remainingAmount / daysRemaining else 0.0
        val runRateText = when (goal.frequencyPref) {
          FrequencyPref.DAILY -> "${CurrencyFormatter.formatRupee(dailyPace)} / day"
          FrequencyPref.WEEKLY -> "${CurrencyFormatter.formatRupee(dailyPace * 7)} / week"
          FrequencyPref.MONTHLY -> "${CurrencyFormatter.formatRupee(dailyPace * 30)} / month"
        }

        Column(
          modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(12.dp))
            .border(
              width = if (goal.isPrimary) 1.5.dp else 1.dp,
              color = if (goal.isPrimary) SwissDark else SwissBorder,
              shape = RoundedCornerShape(12.dp)
            )
            .padding(16.dp)
            .testTag("goal_card_${goal.id}")
        ) {
          // Top Row: Goal Title & Action Triggers
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(8.dp),
              modifier = Modifier.weight(1f)
            ) {
              if (goal.isPrimary) {
                Box(
                  modifier = Modifier
                    .background(SwissCrimsonLight, RoundedCornerShape(4.dp))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                  Text(
                    text = "PRIMARY",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = SwissCrimson,
                    fontSize = 9.sp
                  )
                }
              }

              Text(
                text = goal.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
              )
            }

            // Inline Action Triggers: Primary Star, Edit (Pencil), Delete (Trash)
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
              if (!goal.isPrimary) {
                IconButton(
                  onClick = { onSetPrimary(goal.id) },
                  modifier = Modifier
                    .size(34.dp)
                    .testTag("set_primary_goal_${goal.id}")
                ) {
                  Icon(
                    imageVector = Icons.Outlined.StarBorder,
                    contentDescription = "Set Primary",
                    tint = SwissTextSecondary,
                    modifier = Modifier.size(18.dp)
                  )
                }
              }

              IconButton(
                onClick = {
                  editingGoal = goal
                  showGoalSheet = true
                },
                modifier = Modifier
                  .size(34.dp)
                  .testTag("edit_goal_button_${goal.id}")
              ) {
                Icon(
                  imageVector = Icons.Default.Edit,
                  contentDescription = "Edit Goal",
                  tint = SwissTextSecondary,
                  modifier = Modifier.size(17.dp)
                )
              }

              IconButton(
                onClick = { onDeleteGoal(goal) },
                modifier = Modifier
                  .size(34.dp)
                  .testTag("delete_goal_button_${goal.id}")
              ) {
                Icon(
                  imageVector = Icons.Default.DeleteOutline,
                  contentDescription = "Delete Goal",
                  tint = SwissCrimson,
                  modifier = Modifier.size(18.dp)
                )
              }
            }
          }

          Spacer(modifier = Modifier.height(10.dp))

          // Saved vs Target Monospace
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
          ) {
            Text(
              text = "${CurrencyFormatter.formatRupee(goal.savedAmount)} / ${CurrencyFormatter.formatRupee(goal.targetAmount)}",
              style = MonospaceHeadline,
              color = MaterialTheme.colorScheme.onSurface,
              fontSize = 18.sp
            )

            // Target date countdown ("42 days remaining")
            Box(
              modifier = Modifier
                .background(Color(0xFFF3F4F6), RoundedCornerShape(4.dp))
                .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
              Text(
                text = "$daysRemaining days remaining",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = SwissDark
              )
            }
          }

          Spacer(modifier = Modifier.height(10.dp))

          // 4px flat progress line
          Box(
            modifier = Modifier
              .fillMaxWidth()
              .height(4.dp)
              .background(Color(0xFFE5E7EB), RoundedCornerShape(2.dp))
          ) {
            Box(
              modifier = Modifier
                .fillMaxWidth(animatedProgress)
                .height(4.dp)
                .background(if (goal.isPrimary) SwissCrimson else SwissDark, RoundedCornerShape(2.dp))
            )
          }

          Spacer(modifier = Modifier.height(10.dp))

          // Footer: Priority run-rate & percentage
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Text(
              text = "Pace: $runRateText",
              style = MaterialTheme.typography.bodySmall,
              color = SwissTextSecondary,
              fontWeight = FontWeight.Medium
            )

            Text(
              text = "${(progress * 100).toInt()}% completed",
              style = MonospaceSmall,
              fontWeight = FontWeight.Bold,
              color = SwissDark
            )
          }
        }
      }

      item {
        Spacer(modifier = Modifier.height(72.dp))
      }
    }
  }

  // "Create / Edit Goal" Form Sheet:
  if (showGoalSheet) {
    CreateEditGoalModal(
      initialGoal = editingGoal,
      onDismiss = { showGoalSheet = false },
      onSave = { updatedGoal ->
        onSaveGoal(updatedGoal)
        showGoalSheet = false
      }
    )
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateEditGoalModal(
  initialGoal: Goal?,
  onDismiss: () -> Unit,
  onSave: (Goal) -> Unit
) {
  val context = LocalContext.current
  var goalTitle by remember { mutableStateOf(initialGoal?.title ?: "") }
  var targetAmountText by remember {
    mutableStateOf(initialGoal?.targetAmount?.let { if (it % 1 == 0.0) it.toLong().toString() else it.toString() } ?: "")
  }
  var deadlineEpochDay by remember {
    mutableStateOf(initialGoal?.deadlineEpochDay ?: (LocalDate.now().toEpochDay() + 42))
  }
  var frequencyPref by remember { mutableStateOf(initialGoal?.frequencyPref ?: FrequencyPref.DAILY) }
  var errorMessage by remember { mutableStateOf<String?>(null) }

  val deadlineDate = LocalDate.ofEpochDay(deadlineEpochDay)
  val dateFormatter = DateTimeFormatter.ofPattern("dd MMMM yyyy", Locale.ENGLISH)

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
        .padding(20.dp)
        .testTag("create_goal_form_sheet")
    ) {
      // Header
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Column {
          Text(
            text = if (initialGoal != null) "EDIT TARGET" else "NEW TARGET",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = SwissCrimson,
            letterSpacing = 1.2.sp,
            fontSize = 10.sp
          )
          Text(
            text = if (initialGoal != null) "Modify Goal" else "Create Savings Goal",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
          )
        }
        IconButton(
          onClick = onDismiss,
          modifier = Modifier.testTag("close_goal_form_button")
        ) {
          Icon(
            imageVector = Icons.Default.Close,
            contentDescription = "Close",
            tint = SwissTextSecondary
          )
        }
      }

      Spacer(modifier = Modifier.height(18.dp))

      // Input: Goal Name (e.g., "Bike Fund")
      Text(
        text = "GOAL NAME",
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.sp,
        color = SwissTextTertiary,
        fontSize = 10.sp
      )
      Spacer(modifier = Modifier.height(4.dp))
      OutlinedTextField(
        value = goalTitle,
        onValueChange = {
          goalTitle = it
          errorMessage = null
        },
        placeholder = { Text("e.g. Bike Fund, Reserve Target") },
        singleLine = true,
        colors = OutlinedTextFieldDefaults.colors(
          focusedBorderColor = SwissDark,
          unfocusedBorderColor = SwissBorder
        ),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier
          .fillMaxWidth()
          .testTag("goal_name_input")
      )

      Spacer(modifier = Modifier.height(16.dp))

      // Input: Target Amount (₹ with numeric pad)
      Text(
        text = "TARGET AMOUNT (₹)",
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.sp,
        color = SwissTextTertiary,
        fontSize = 10.sp
      )
      Spacer(modifier = Modifier.height(4.dp))
      OutlinedTextField(
        value = targetAmountText,
        onValueChange = { input ->
          if (input.all { it.isDigit() || it == '.' }) {
            targetAmountText = input
            errorMessage = null
          }
        },
        leadingIcon = {
          Text(
            text = "₹",
            style = MonospaceHeadline,
            color = SwissDark,
            modifier = Modifier.padding(start = 12.dp)
          )
        },
        placeholder = { Text("80000", style = MonospaceHeadline, color = SwissTextTertiary) },
        textStyle = MonospaceHeadline,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        singleLine = true,
        colors = OutlinedTextFieldDefaults.colors(
          focusedBorderColor = SwissDark,
          unfocusedBorderColor = SwissBorder
        ),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier
          .fillMaxWidth()
          .testTag("goal_amount_input")
      )

      Spacer(modifier = Modifier.height(16.dp))

      // Date Picker: Target Deadline calendar selector
      Text(
        text = "TARGET DEADLINE",
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.sp,
        color = SwissTextTertiary,
        fontSize = 10.sp
      )
      Spacer(modifier = Modifier.height(4.dp))
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .background(Color(0xFFF9FAFB), RoundedCornerShape(8.dp))
          .border(1.dp, SwissBorder, RoundedCornerShape(8.dp))
          .clickable {
            val picker = DatePickerDialog(
              context,
              { _, year, month, dayOfMonth ->
                val picked = LocalDate.of(year, month + 1, dayOfMonth)
                deadlineEpochDay = picked.toEpochDay()
              },
              deadlineDate.year,
              deadlineDate.monthValue - 1,
              deadlineDate.dayOfMonth
            )
            picker.show()
          }
          .padding(horizontal = 14.dp, vertical = 14.dp)
          .testTag("goal_deadline_picker"),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          Icon(
            imageVector = Icons.Default.CalendarMonth,
            contentDescription = null,
            tint = SwissDark,
            modifier = Modifier.size(20.dp)
          )
          Text(
            text = deadlineDate.format(dateFormatter),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
          )
        }

        val daysLeft = max(0L, deadlineEpochDay - LocalDate.now().toEpochDay())
        Text(
          text = "$daysLeft days left",
          style = MaterialTheme.typography.labelSmall,
          color = SwissTextSecondary,
          fontWeight = FontWeight.Medium
        )
      }

      Spacer(modifier = Modifier.height(16.dp))

      // Frequency Preference: Toggle priority run-rate display (Daily / Weekly / Monthly)
      Text(
        text = "PACING FREQUENCY PREFERENCE",
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.sp,
        color = SwissTextTertiary,
        fontSize = 10.sp
      )
      Spacer(modifier = Modifier.height(6.dp))
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .background(Color(0xFFF3F4F6), RoundedCornerShape(8.dp))
          .padding(4.dp)
      ) {
        FrequencyPref.values().forEach { pref ->
          val isSelected = frequencyPref == pref
          Box(
            modifier = Modifier
              .weight(1f)
              .background(
                color = if (isSelected) SwissDark else Color.Transparent,
                shape = RoundedCornerShape(6.dp)
              )
              .clickable { frequencyPref = pref }
              .padding(vertical = 10.dp),
            contentAlignment = Alignment.Center
          ) {
            Text(
              text = pref.displayName,
              style = MaterialTheme.typography.labelMedium,
              fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
              color = if (isSelected) Color.White else SwissTextSecondary
            )
          }
        }
      }

      if (errorMessage != null) {
        Spacer(modifier = Modifier.height(8.dp))
        Text(
          text = errorMessage!!,
          style = MaterialTheme.typography.bodySmall,
          color = SwissCrimson
        )
      }

      Spacer(modifier = Modifier.height(24.dp))

      // Button: "Save Goal" (Solid Black with white text)
      Button(
        onClick = {
          if (goalTitle.isBlank()) {
            errorMessage = "Please enter a goal title"
            return@Button
          }
          val parsedAmount = targetAmountText.toDoubleOrNull()
          if (parsedAmount == null || parsedAmount <= 0) {
            errorMessage = "Please enter a valid target amount"
            return@Button
          }

          val updatedGoal = initialGoal?.copy(
            title = goalTitle.trim(),
            targetAmount = parsedAmount,
            deadlineEpochDay = deadlineEpochDay,
            frequencyPref = frequencyPref
          ) ?: Goal(
            title = goalTitle.trim(),
            targetAmount = parsedAmount,
            savedAmount = 0.0,
            deadlineEpochDay = deadlineEpochDay,
            frequencyPref = frequencyPref,
            isPrimary = false,
            createdAtEpochDay = LocalDate.now().toEpochDay()
          )

          onSave(updatedGoal)
        },
        colors = ButtonDefaults.buttonColors(
          containerColor = SwissDark,
          contentColor = Color.White
        ),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier
          .fillMaxWidth()
          .height(52.dp)
          .testTag("save_goal_button")
      ) {
        Text(
          text = "Save Goal",
          style = MaterialTheme.typography.labelLarge,
          fontWeight = FontWeight.Bold
        )
      }

      Spacer(modifier = Modifier.height(10.dp))
    }
  }
}
