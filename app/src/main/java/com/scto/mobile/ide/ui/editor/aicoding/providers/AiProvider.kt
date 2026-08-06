package com.scto.mobile.ide.ui.editor.aicoding.providers

import android.content.Context
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

sealed class AiResponseChunk {
    data class Content(val text: String) : AiResponseChunk()
    data class Error(val message: String) : AiResponseChunk()
}

interface AiProvider {
    val id: String
    val displayName: String
    fun isConfigured(context: Context): Boolean
    fun isAvailable(context: Context): Boolean
    fun sendPrompt(context: Context, prompt: String, history: List<Pair<String, String>>): Flow<AiResponseChunk>
}

class AiderProvider : AiProvider {
    override val id: String = "aider"
    override val displayName: String = "Aider (CLI)"

    override fun isConfigured(context: Context): Boolean = true
    override fun isAvailable(context: Context): Boolean = true

    override fun sendPrompt(context: Context, prompt: String, history: List<Pair<String, String>>): Flow<AiResponseChunk> = flow {
        emit(AiResponseChunk.Content("Aider CLI Execution: $prompt"))
    }
}

class OpenAiCompatibleProvider : AiProvider {
    override val id: String = "openai"
    override val displayName: String = "OpenAI / Custom API"

    override fun isConfigured(context: Context): Boolean {
        val prefs = context.getSharedPreferences("ai_coding_settings", Context.MODE_PRIVATE)
        return prefs.getString("api_key", "")?.isNotBlank() == true
    }

    override fun isAvailable(context: Context): Boolean = isConfigured(context)

    override fun sendPrompt(context: Context, prompt: String, history: List<Pair<String, String>>): Flow<AiResponseChunk> = flow {
        val prefs = context.getSharedPreferences("ai_coding_settings", Context.MODE_PRIVATE)
        val apiKey = prefs.getString("api_key", "") ?: ""
        val baseUrl = prefs.getString("base_url", "https://api.openai.com/v1") ?: "https://api.openai.com/v1"
        val model = prefs.getString("model", "gpt-3.5-turbo") ?: "gpt-3.5-turbo"

        if (apiKey.isBlank()) {
            emit(AiResponseChunk.Error("API Key is missing in AI settings"))
            return@flow
        }

        try {
            val client = OkHttpClient.Builder()
                .connectTimeout(60, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .build()

            val messagesArray = JSONArray()
            history.forEach { (role, text) ->
                messagesArray.put(JSONObject().put("role", role).put("content", text))
            }
            messagesArray.put(JSONObject().put("role", "user").put("content", prompt))

            val jsonBody = JSONObject().apply {
                put("model", model)
                put("messages", messagesArray)
            }

            val request = Request.Builder()
                .url(if (baseUrl.endsWith("/")) "${baseUrl}chat/completions" else "$baseUrl/chat/completions")
                .addHeader("Authorization", "Bearer $apiKey")
                .post(jsonBody.toString().toRequestBody("application/json".toMediaType()))
                .build()

            val response = client.newCall(request).execute()
            val bodyString = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                emit(AiResponseChunk.Error("API error (${response.code}): $bodyString"))
                return@flow
            }

            val responseJson = JSONObject(bodyString)
            val choices = responseJson.optJSONArray("choices")
            if (choices != null && choices.length() > 0) {
                val content = choices.getJSONObject(0).optJSONObject("message")?.optString("content") ?: ""
                emit(AiResponseChunk.Content(content))
            } else {
                emit(AiResponseChunk.Error("No response content from provider"))
            }
        } catch (e: Exception) {
            emit(AiResponseChunk.Error("Network error: ${e.message}"))
        }
    }
}

object AiProviderRegistry {
    val providers: List<AiProvider> = listOf(
        OpenAiCompatibleProvider(),
        AiderProvider()
    )

    fun getProvider(id: String): AiProvider {
        return providers.find { it.id == id } ?: providers.first()
    }
}
