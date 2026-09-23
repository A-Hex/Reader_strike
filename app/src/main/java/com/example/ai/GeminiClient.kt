package com.example.ai

import android.util.Log
import com.squareup.moshi.Moshi
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path
import java.io.IOException
import java.util.concurrent.TimeUnit

/** Typed outcome of a Gemini call so callers can render a real error state instead of faking content. */
sealed interface AiCallResult {
    data class Success(
        val text: String,
        val model: String,
        val promptTokens: Int?,
        val outputTokens: Int?
    ) : AiCallResult

    data class MissingApiKey(val message: String) : AiCallResult
    data class NetworkError(val message: String, val detail: String?) : AiCallResult
    data class RateLimited(val message: String) : AiCallResult
    data class SafetyBlocked(val message: String) : AiCallResult
    data class EmptyResponse(val message: String) : AiCallResult

    /** Non-recoverable HTTP error. [statusCode] is the raw HTTP status. */
    data class ApiError(val statusCode: Int, val message: String) : AiCallResult
}

private interface GeminiApi {

    @POST("v1beta/models/{model}:generateContent")
    suspend fun generateContent(
        @Path("model") model: String,
        @Header("x-goog-api-key") apiKey: String,
        @Body request: GeminiRequest
    ): Response<GeminiResponse>
}

object GeminiClient {

    private const val TAG = "GeminiClient"

    /** Free-tier flash model. */
    const val DEFAULT_MODEL = "gemini-2.5-flash"

    /** Used only when the configured model is reported as unavailable (HTTP 404). */
    private const val ALIAS_MODEL = "gemini-flash-latest"

    private const val MAX_RETRIES = 2
    private const val MAX_RETRY_AFTER_SECONDS = 10L

    private val moshi: Moshi = Moshi.Builder().build()

    private val httpClient: OkHttpClient by lazy {
        val logging = HttpLoggingInterceptor { line -> Log.d(TAG, line) }.apply {
            level = HttpLoggingInterceptor.Level.BASIC
            // Never let a credential reach logcat.
            redactHeader("x-goog-api-key")
        }

        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .callTimeout(90, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .addInterceptor(logging)
            .build()
    }

    private val api: GeminiApi by lazy {
        Retrofit.Builder()
            .baseUrl("https://generativelanguage.googleapis.com/")
            .client(httpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(GeminiApi::class.java)
    }

    /**
     * Executes a grounded generation request.
     *
     * Retries transient failures (transport errors, HTTP 429/500/502/503/504) with bounded
     * exponential backoff and honours `Retry-After` when the server sends it.
     */
    suspend fun generateContent(
        apiKey: String,
        request: GeminiRequest,
        model: String = DEFAULT_MODEL
    ): AiCallResult {
        if (apiKey.isBlank()) {
            return AiCallResult.MissingApiKey(
                "No Gemini API key configured. Add GEMINI_API_KEY to .env or the Keys panel to enable AI features."
            )
        }

        var activeModel = model
        var aliasAttempted = false
        var lastResult: AiCallResult? = null

        var attempt = 0
        while (attempt <= MAX_RETRIES) {
            val result = runCatching { api.generateContent(activeModel, apiKey, request) }

            result.onFailure { error ->
                if (error is IOException) {
                    lastResult = AiCallResult.NetworkError(
                        message = "Could not reach Gemini. Check your connection and try again.",
                        detail = error.message
                    )
                } else {
                    lastResult = AiCallResult.ApiError(
                        statusCode = -1,
                        message = error.message ?: "Unexpected failure calling Gemini."
                    )
                }
            }

            val response = result.getOrNull()
            if (response == null) {
                attempt++
                if (attempt > MAX_RETRIES) break
                sleepBackoff(attempt)
                continue
            }

            if (response.isSuccessful) {
                return parseSuccess(response.body(), activeModel)
            }

            val status = response.code()
            val serverMessage = readErrorMessage(response)

            when {
                // The requested model does not exist for this key: retry once on the rolling alias.
                status == 404 && !aliasAttempted -> {
                    aliasAttempted = true
                    activeModel = ALIAS_MODEL
                    lastResult = AiCallResult.ApiError(status, serverMessage)
                    continue
                }

                status == 429 -> {
                    lastResult = AiCallResult.RateLimited(
                        "Gemini rate limit reached. Wait a moment and retry, or switch to on-device analysis."
                    )
                    if (attempt == MAX_RETRIES) return lastResult
                    sleepRetryAfter(response, attempt)
                    attempt++
                    continue
                }

                status == 400 && serverMessage.contains("API key", ignoreCase = true) -> {
                    return AiCallResult.ApiError(
                        status,
                        "Gemini rejected the API key. Update the key and try again."
                    )
                }

                status == 400 && serverMessage.contains("safety", ignoreCase = true) -> {
                    return AiCallResult.SafetyBlocked(
                        "Gemini declined this request due to safety filters. Try rephrasing your question."
                    )
                }

                status == 403 -> {
                    return AiCallResult.ApiError(
                        status,
                        "Gemini denied access (403). Confirm the Generative Language API is enabled for this key."
                    )
                }

                status in RETRYABLE_STATUS -> {
                    lastResult = AiCallResult.ApiError(status, serverMessage)
                    if (attempt == MAX_RETRIES) return lastResult
                    sleepRetryAfter(response, attempt)
                    attempt++
                    continue
                }

                else -> return AiCallResult.ApiError(status, serverMessage)
            }
        }

        return lastResult ?: AiCallResult.NetworkError(
            "Gemini request failed after ${MAX_RETRIES + 1} attempts.",
            null
        )
    }

    private fun parseSuccess(body: GeminiResponse?, model: String): AiCallResult {
        if (body == null) {
            return AiCallResult.EmptyResponse("Gemini returned an empty response body.")
        }

        val text = body.candidates
            ?.firstOrNull()
            ?.content
            ?.parts
            ?.mapNotNull { it.text }
            ?.joinToString("\n")
            ?.trim()
            .orEmpty()

        if (text.isBlank()) {
            val blockReason = body.promptFeedback?.blockReason
            if (!blockReason.isNullOrBlank()) {
                return AiCallResult.SafetyBlocked(
                    "Gemini blocked this request ($blockReason). Try a different passage or question."
                )
            }
            val finish = body.candidates?.firstOrNull()?.finishReason
            return AiCallResult.EmptyResponse(
                if (finish == "MAX_TOKENS") {
                    "Gemini hit the output limit before producing text. Try a shorter passage."
                } else {
                    "Gemini returned no text."
                }
            )
        }

        return AiCallResult.Success(
            text = text,
            model = model,
            promptTokens = body.usageMetadata?.promptTokenCount,
            outputTokens = body.usageMetadata?.candidatesTokenCount
        )
    }

    private fun readErrorMessage(response: Response<*>): String {
        val raw = runCatching { response.errorBody()?.string() }.getOrNull().orEmpty()
        if (raw.isBlank()) return "Gemini returned HTTP ${response.code()}."

        val parsed = runCatching {
            moshi.adapter(GeminiErrorEnvelope::class.java).fromJson(raw)?.error?.message
        }.getOrNull()

        return parsed?.takeIf { it.isNotBlank() } ?: raw.take(300)
    }

    private suspend fun sleepRetryAfter(response: Response<*>, attempt: Int) {
        val headerSeconds = response.headers()["Retry-After"]
            ?.trim()
            ?.toLongOrNull()
            ?.coerceIn(1L, MAX_RETRY_AFTER_SECONDS)
        if (headerSeconds != null) {
            kotlinx.coroutines.delay(headerSeconds * 1_000L)
        } else {
            sleepBackoff(attempt)
        }
    }

    private suspend fun sleepBackoff(attempt: Int) {
        val millis = 700L * (1L shl (attempt - 1).coerceAtLeast(0))
        kotlinx.coroutines.delay(millis.coerceAtMost(3_000L))
    }

    private val RETRYABLE_STATUS = setOf(408, 500, 502, 503, 504)
}
