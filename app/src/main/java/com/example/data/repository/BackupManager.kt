package com.example.data.repository

import com.example.data.model.Channel
import com.example.data.model.FrequencyPref
import com.example.data.model.Goal
import com.example.data.model.TransactionItem
import org.json.JSONArray
import org.json.JSONObject

data class BackupPayload(
  val userName: String,
  val exportedAt: Long,
  val goals: List<Goal>,
  val transactions: List<TransactionItem>
)

object BackupManager {

  fun exportToJson(
    userName: String,
    goals: List<Goal>,
    transactions: List<TransactionItem>
  ): String {
    val root = JSONObject()
    root.put("version", 1)
    root.put("appName", "Sanchay")
    root.put("userName", userName)
    root.put("exportedAt", System.currentTimeMillis())

    val goalsArray = JSONArray()
    for (goal in goals) {
      val gObj = JSONObject().apply {
        put("id", goal.id)
        put("title", goal.title)
        put("targetAmount", goal.targetAmount)
        put("savedAmount", goal.savedAmount)
        put("deadlineEpochDay", goal.deadlineEpochDay)
        put("frequencyPref", goal.frequencyPref.name)
        put("isPrimary", goal.isPrimary)
        put("createdAtEpochDay", goal.createdAtEpochDay)
      }
      goalsArray.put(gObj)
    }
    root.put("goals", goalsArray)

    val txArray = JSONArray()
    for (tx in transactions) {
      val tObj = JSONObject().apply {
        put("id", tx.id)
        if (tx.goalId != null) put("goalId", tx.goalId) else put("goalId", JSONObject.NULL)
        put("amount", tx.amount)
        put("channel", tx.channel.name)
        put("note", tx.note)
        put("timestamp", tx.timestamp)
        put("dateEpochDay", tx.dateEpochDay)
        put("isAutoCaptured", tx.isAutoCaptured)
        put("upiAppName", tx.upiAppName ?: JSONObject.NULL)
        put("merchantOrSender", tx.merchantOrSender ?: JSONObject.NULL)
        put("upiRefId", tx.upiRefId ?: JSONObject.NULL)
        put("category", tx.category)
      }
      txArray.put(tObj)
    }
    root.put("transactions", txArray)

    return root.toString(2)
  }

  fun importFromJson(jsonStr: String): BackupPayload? {
    return try {
      val root = JSONObject(jsonStr)
      val userName = root.optString("userName", "Karan")
      val exportedAt = root.optLong("exportedAt", System.currentTimeMillis())

      val goalsList = mutableListOf<Goal>()
      val goalsArray = root.optJSONArray("goals")
      if (goalsArray != null) {
        for (i in 0 until goalsArray.length()) {
          val gObj = goalsArray.getJSONObject(i)
          val goal = Goal(
            id = gObj.optLong("id", 0L),
            title = gObj.optString("title", "Goal"),
            targetAmount = gObj.optDouble("targetAmount", 0.0),
            savedAmount = gObj.optDouble("savedAmount", 0.0),
            deadlineEpochDay = gObj.optLong("deadlineEpochDay", 0L),
            frequencyPref = try {
              FrequencyPref.valueOf(gObj.optString("frequencyPref", FrequencyPref.DAILY.name))
            } catch (e: Exception) {
              FrequencyPref.DAILY
            },
            isPrimary = gObj.optBoolean("isPrimary", false),
            createdAtEpochDay = gObj.optLong("createdAtEpochDay", 0L)
          )
          goalsList.add(goal)
        }
      }

      val txList = mutableListOf<TransactionItem>()
      val txArray = root.optJSONArray("transactions")
      if (txArray != null) {
        for (i in 0 until txArray.length()) {
          val tObj = txArray.getJSONObject(i)
          val gId = if (tObj.isNull("goalId")) null else tObj.optLong("goalId")
          val tx = TransactionItem(
            id = tObj.optLong("id", 0L),
            goalId = gId,
            amount = tObj.optDouble("amount", 0.0),
            channel = try {
              Channel.valueOf(tObj.optString("channel", Channel.UPI.name))
            } catch (e: Exception) {
              Channel.UPI
            },
            note = tObj.optString("note", "Savings"),
            timestamp = tObj.optLong("timestamp", System.currentTimeMillis()),
            dateEpochDay = tObj.optLong("dateEpochDay", 0L),
            isAutoCaptured = tObj.optBoolean("isAutoCaptured", false),
            upiAppName = if (tObj.isNull("upiAppName")) null else tObj.optString("upiAppName"),
            merchantOrSender = if (tObj.isNull("merchantOrSender")) null else tObj.optString("merchantOrSender"),
            upiRefId = if (tObj.isNull("upiRefId")) null else tObj.optString("upiRefId"),
            isConfirmed = true,
            category = tObj.optString("category", "GENERAL")
          )
          txList.add(tx)
        }
      }

      BackupPayload(
        userName = userName,
        exportedAt = exportedAt,
        goals = goalsList,
        transactions = txList
      )
    } catch (e: Exception) {
      null
    }
  }
}
