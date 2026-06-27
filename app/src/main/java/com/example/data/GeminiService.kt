package com.example.data

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

@JsonClass(generateAdapter = true)
data class GeminiRequest(
    val contents: List<GeminiContent>,
    val generationConfig: GeminiGenerationConfig? = null,
    val systemInstruction: GeminiContent? = null
)

@JsonClass(generateAdapter = true)
data class GeminiContent(
    val parts: List<GeminiPart>
)

@JsonClass(generateAdapter = true)
data class GeminiPart(
    val text: String
)

@JsonClass(generateAdapter = true)
data class GeminiGenerationConfig(
    val temperature: Float? = 0.5f,
    val topP: Float? = 0.95f
)

@JsonClass(generateAdapter = true)
data class GeminiResponse(
    val candidates: List<GeminiCandidate>?
)

@JsonClass(generateAdapter = true)
data class GeminiCandidate(
    val content: GeminiContent?
)

interface GeminiApiService {
    @POST("v1beta/models/gemini-3.5-flash:generateContent")
    suspend fun generateContent(
        @Query("key") apiKey: String,
        @Body request: GeminiRequest
    ): GeminiResponse
}

object RetrofitGeminiClient {
    private const val BASE_URL = "https://generativelanguage.googleapis.com/"

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    val service: GeminiApiService by lazy {
        val retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create())
            .build()
        retrofit.create(GeminiApiService::class.java)
    }
}

class GeminiService {
    suspend fun getSpendingInsights(transactions: List<DbTransaction>, budget: Double): String = withContext(Dispatchers.IO) {
        val apiKey = try {
            BuildConfig.GEMINI_API_KEY
        } catch (e: Exception) {
            ""
        }

        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext getOfflineInsights(transactions, budget)
        }

        val txSummary = if (transactions.isEmpty()) {
            "No transactions recorded yet."
        } else {
            transactions.joinToString("\n") { tx ->
                "- ${tx.category}: ₹${tx.amount} to ${tx.recipientName} (${tx.status})"
            }
        }

        val prompt = """
            You are "MitraAI", the smart financial co-pilot of PayMitra UPI.
            Analyze these transactions and budget:
            Total Budget: ₹$budget
            Transactions:
            $txSummary
            
            Provide:
            1. A 2-sentence summary of spending behavior.
            2. Two actionable, highly specific advice for saving.
            Keep it under 150 words total, friendly, clean, and inspiring. Use Indian Rupee (₹).
        """.trimIndent()

        val request = GeminiRequest(
            contents = listOf(
                GeminiContent(parts = listOf(GeminiPart(text = prompt)))
            ),
            systemInstruction = GeminiContent(parts = listOf(GeminiPart(text = "You are a friendly Indian UPI financial co-pilot called MitraAI. Keep replies concise and professional.")))
        )

        try {
            val response = RetrofitGeminiClient.service.generateContent(apiKey, request)
            response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text 
                ?: "Unable to process insights. Let's start saving today!"
        } catch (e: Exception) {
            getOfflineInsights(transactions, budget) + "\n\n(Offline analysis active)"
        }
    }

    private fun getOfflineInsights(transactions: List<DbTransaction>, budget: Double): String {
        if (transactions.isEmpty()) {
            return "Good start! You haven't made any transactions yet. Use PayMitra for instant transfers, mobile recharges, and bill payments to track your spending effortlessly."
        }
        val totalSpent = transactions.filter { it.status == "SUCCESS" }.sumOf { it.amount }
        val categoryBreakdown = transactions.filter { it.status == "SUCCESS" }
            .groupBy { it.category }
            .mapValues { entry -> entry.value.sumOf { it.amount } }

        val mainCategory = categoryBreakdown.maxByOrNull { it.value }

        val baseInsights = StringBuilder()
        baseInsights.append("MitraAI Offline Analysis:\n")
        baseInsights.append("You have spent ₹$totalSpent out of ₹$budget budget. ")

        if (totalSpent > budget) {
            baseInsights.append("⚠️ You have exceeded your budget by ₹${totalSpent - budget}! Consider reducing secondary expenses.\n\n")
        } else {
            baseInsights.append("✅ Great job! You are within your budget with ₹${budget - totalSpent} remaining.\n\n")
        }

        if (mainCategory != null) {
            baseInsights.append("• High Spending Alert: Your largest expense category is ${mainCategory.key} totaling ₹${mainCategory.value}.\n")
        }
        baseInsights.append("• Action Plan: Setup auto-reminders for your upcoming recharges and utility bills to pay them on time and unlock PayMitra cashback scratchcards.")

        return baseInsights.toString()
    }
}
