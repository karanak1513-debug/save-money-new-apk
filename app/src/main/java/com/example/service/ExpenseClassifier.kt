package com.example.service

import android.util.Log
import com.example.BuildConfig
import com.example.data.model.ExpenseCategory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * Intelligent Merchant Entity & Financial Category Classifier.
 * Uses high-speed regex heuristics with fallback to lightweight Gemini API structured prompts.
 */
object ExpenseClassifier {

  private const val TAG = "ExpenseClassifier"

  // 1. Food & Essentials Heuristics
  private val foodRegex = Regex(
    """(?i)\b(swiggy|zomato|blinkit|zepto|instamart|grocery|supermarket|bigbasket|nature's basket|dunzo|eatclub|mcdonald|starbucks|subway|dominos|pizza|burger|chai|coffee|bakery|sweets|restaurant|cafe|dhaba|fresh|baker|kfc|haldiram|food)\b"""
  )

  // 2. Commute & Fuel Heuristics
  private val commuteRegex = Regex(
    """(?i)\b(uber|ola|rapido|dmrc|metro|petrol|fuel|indianoil|iocl|hpcl|bpcl|shell|bharat petroleum|cng|fastag|toll|parking|cab|taxi|auto|rickshaw|irctc|railway|train|flight|indigo|air india|commute)\b"""
  )

  // 3. Bills & Utilities Heuristics
  private val billsRegex = Regex(
    """(?i)\b(jio|airtel|vi|vodafone|bsnl|electricity|bescom|tneb|tata power|uppcl|broadband|wifi|act fibernet|water|gas|igl|adani gas|cylinder|dth|tata sky|dish tv|recharge|bill|credit card|emi|insurance|lic|rent|maintenance|utility)\b"""
  )

  // 4. Discretionary & Leisure Heuristics
  private val discretionaryRegex = Regex(
    """(?i)\b(movies|bookmyshow|pvr|inox|shopping|zara|amazon|flipkart|myntra|h&m|uniqlo|ajio|nykaa|game|steam|playstation|xbox|netflix|spotify|prime video|youtube|pub|bar|club|resort|hotel|salon|spa|theatre|cinema)\b"""
  )

  /**
   * Fast, zero-latency deterministic heuristic classification.
   * Runs locally on device without network requirements.
   */
  fun classify(merchantOrSender: String?, note: String?): ExpenseCategory {
    val combined = buildString {
      if (!merchantOrSender.isNullOrBlank()) append(merchantOrSender).append(" ")
      if (!note.isNullOrBlank()) append(note)
    }.trim()

    if (combined.isBlank()) return ExpenseCategory.GENERAL

    return when {
      foodRegex.containsMatchIn(combined) -> ExpenseCategory.FOOD_ESSENTIALS
      commuteRegex.containsMatchIn(combined) -> ExpenseCategory.COMMUTE_FUEL
      billsRegex.containsMatchIn(combined) -> ExpenseCategory.BILLS_UTILITIES
      discretionaryRegex.containsMatchIn(combined) -> ExpenseCategory.DISCRETIONARY
      else -> ExpenseCategory.GENERAL
    }
  }

  private val okHttpClient by lazy {
    OkHttpClient.Builder()
      .connectTimeout(5, TimeUnit.SECONDS)
      .readTimeout(10, TimeUnit.SECONDS)
      .build()
  }

  /**
   * Lightweight Gemini API prompt for enriching unclassified or novel merchant strings.
   * Model: gemini-3.5-flash
   */
  suspend fun classifyWithGeminiAsync(text: String): ExpenseCategory = withContext(Dispatchers.IO) {
    val localCategory = classify(text, "")
    if (localCategory != ExpenseCategory.GENERAL) {
      return@withContext localCategory
    }

    val apiKey = runCatching { BuildConfig.GEMINI_API_KEY }.getOrDefault("")
    if (apiKey.isBlank() || apiKey == "DEFAULT_GEMINI_API_KEY") {
      return@withContext localCategory
    }

    try {
      val prompt = """
        Classify the following transaction merchant or memo into exactly one of these 4 categories:
        - FOOD_ESSENTIALS
        - COMMUTE_FUEL
        - BILLS_UTILITIES
        - DISCRETIONARY
        If unsure, output GENERAL.
        Respond with ONLY the category identifier.
        Input: "$text"
      """.trimIndent()

      val requestBodyJson = JSONObject().apply {
        put("contents", JSONArray().apply {
          put(JSONObject().apply {
            put("parts", JSONArray().apply {
              put(JSONObject().put("text", prompt))
            })
          })
        })
      }

      val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=$apiKey"
      val request = Request.Builder()
        .url(url)
        .post(requestBodyJson.toString().toRequestBody("application/json".toMediaType()))
        .build()

      val response = okHttpClient.newCall(request).execute()
      if (response.isSuccessful) {
        val bodyStr = response.body?.string() ?: return@withContext localCategory
        val root = JSONObject(bodyStr)
        val candidateText = root.optJSONArray("candidates")
          ?.optJSONObject(0)
          ?.optJSONObject("content")
          ?.optJSONArray("parts")
          ?.optJSONObject(0)
          ?.optString("text", "")
          ?.trim() ?: ""

        val cleaned = candidateText.uppercase().replace(Regex("[^A-Z_]"), "")
        return@withContext runCatching { ExpenseCategory.valueOf(cleaned) }.getOrDefault(localCategory)
      }
    } catch (e: Throwable) {
      Log.w(TAG, "Gemini classification skipped: ${e.message}")
    }

    return@withContext localCategory
  }
}
