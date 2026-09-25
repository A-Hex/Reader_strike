package com.example.ai

import android.content.Context
import android.util.Log
import com.google.ai.edge.litertlm.Backend
import com.google.ai.edge.litertlm.Content
import com.google.ai.edge.litertlm.Conversation
import com.google.ai.edge.litertlm.ConversationConfig
import com.google.ai.edge.litertlm.Engine
import com.google.ai.edge.litertlm.EngineConfig
import com.google.ai.edge.litertlm.Message
import com.google.ai.edge.litertlm.SamplerConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.File

/**
 * What happens while the on-device model answers.
 *
 * [Progress] carries a *snapshot* of the answer so far rather than a delta, so a consumer can
 * replace what it renders instead of tracking which chunks it has already appended.
 */
sealed interface LocalLlmEvent {

    /** The answer so far, safe to render immediately. More [Progress] events will follow. */
    data class Progress(val markdown: String) : LocalLlmEvent

    /** Terminal success. [markdown] is the finished answer, trimmed. */
    data class Completed(val markdown: String, val model: String) : LocalLlmEvent

    /** No usable model bundle on the device. */
    data object NotInstalled : LocalLlmEvent

    /** Terminal failure; the message is written to be shown to the user as-is. */
    data class Failed(val message: String) : LocalLlmEvent
}

/**
 * Coalesces the model's output chunks into the answer so far.
 *
 * Chunks arrive far faster than a phone needs to redraw a growing block of text, so partial text
 * is published at most once per [intervalNanos]. The clock is injected, so the behaviour is unit
 * tested without waiting in real time.
 *
 * Chunks are *deltas*: they are appended verbatim, never trimmed individually, because the
 * whitespace at a chunk boundary is real text.
 */
internal class LocalLlmStreamBuffer(private val intervalNanos: Long = DEFAULT_INTERVAL_NANOS) {

    private val builder = StringBuilder()
    private var lastPublishedNanos = NOTHING_PUBLISHED

    /** Everything received so far, trimmed for use as a finished answer. */
    fun completed(): String = builder.toString().trim()

    /** Everything received so far, exactly as it arrived. */
    fun snapshot(): String = builder.toString()

    /**
     * Appends [chunk] and returns the answer so far when it is time to publish it, or null when the
     * publishing interval has not elapsed yet. Empty chunks never publish.
     */
    fun append(chunk: String, nowNanos: Long): String? {
        if (chunk.isEmpty()) return null
        builder.append(chunk)
        if (lastPublishedNanos != NOTHING_PUBLISHED && nowNanos - lastPublishedNanos < intervalNanos) {
            return null
        }
        lastPublishedNanos = nowNanos
        return builder.toString()
    }

    companion object {
        /** 80 ms: fast enough to look live, slow enough to keep recomposition cheap. */
        const val DEFAULT_INTERVAL_NANOS = 80_000_000L
        private const val NOTHING_PUBLISHED = Long.MIN_VALUE
    }
}

/**
 * Runs a real LLM on the device through LiteRT-LM.
 *
 * The model bundle stays memory-resident between calls because loading it is the expensive part
 * (tens of seconds and ~1 GB of RAM for a 1B 4-bit model). Generation is streamed chunk by chunk
 * so the UI can show progress, and every generation runs on [Dispatchers.Default] so the UI thread
 * is never blocked.
 */
object LocalLlmEngine {

    private const val TAG = "LocalLlmEngine"

    /**
     * Total token budget (prompt + answer) handed to the runtime. It also sizes the KV cache, so
     * it stays modest: a bigger window costs memory on a phone for no benefit on a small model.
     */
    private const val MAX_TOKENS = 4_096

    /** Serialises generation: the runtime exposes one conversation at a time. */
    private val lock = Mutex()

    private var engine: Engine? = null
    private var loadedPath: String? = null
    private var loadedBackend: LocalLlmBackend? = null

    fun isLoaded(): Boolean = engine?.isInitialized() == true

    /**
     * Streams an answer generated entirely on the device.
     *
     * Emits [LocalLlmEvent.Progress] as text arrives, then exactly one terminal event
     * ([LocalLlmEvent.Completed], [LocalLlmEvent.Failed] or [LocalLlmEvent.NotInstalled]).
     *
     * Cancelling the collection stops the native inference instead of letting it run to the end.
     */
    fun generateStreaming(
        context: Context,
        systemInstruction: String,
        prompt: String,
        temperature: Double
    ): Flow<LocalLlmEvent> = flow {
        val modelFile = LocalLlmModel.modelFile(context)
        if (modelFile == null) {
            emit(LocalLlmEvent.NotInstalled)
            return@flow
        }

        val backend = LocalLlmModel.backend(context)
        val appContext = context.applicationContext

        lock.withLock {
            var conversation: Conversation? = null
            try {
                val active = ensureEngine(appContext, modelFile, backend)
                val created = active.createConversation(conversationConfig(systemInstruction, temperature))
                conversation = created

                val buffer = LocalLlmStreamBuffer()
                created.sendMessageAsync(Message.of(prompt)).collect { message ->
                    val text = rawTextOf(message)
                    val answerSoFar = buffer.append(text, System.nanoTime())
                    if (answerSoFar != null) {
                        emit(LocalLlmEvent.Progress(answerSoFar))
                    }
                }

                val answer = buffer.completed()
                emit(
                    if (answer.isBlank()) {
                        LocalLlmEvent.Failed("The on-device model returned no text. Try again with a shorter passage.")
                    } else {
                        LocalLlmEvent.Completed(answer, modelFile.name)
                    }
                )
            } catch (t: Throwable) {
                if (t is kotlinx.coroutines.CancellationException) {
                    // The caller stopped reading: tell the runtime to stop generating too.
                    runCatching { conversation?.cancelProcess() }
                    throw t
                }
                Log.e(TAG, "On-device generation failed", t)
                // A runtime that threw mid-conversation must not be reused for the next request.
                release()
                emit(LocalLlmEvent.Failed(describe(t)))
            } finally {
                conversation?.let { runCatching { it.close() } }
            }
        }
    }.flowOn(Dispatchers.Default)

    /** Drops the loaded model and frees its memory. */
    fun release() {
        val current = engine
        engine = null
        loadedPath = null
        loadedBackend = null
        runCatching { current?.close() }
    }

    private fun conversationConfig(systemInstruction: String, temperature: Double): ConversationConfig =
        ConversationConfig(
            systemMessage = Message.of(systemInstruction),
            samplerConfig = SamplerConfig(
                topK = 40,
                topP = 0.95,
                temperature = temperature,
                seed = 0
            )
        )

    private fun ensureEngine(context: Context, modelFile: File, backend: LocalLlmBackend): Engine {
        val current = engine
        if (current != null &&
            loadedPath == modelFile.absolutePath &&
            loadedBackend == backend &&
            current.isInitialized()
        ) {
            return current
        }

        release()

        val config = EngineConfig(
            modelPath = modelFile.absolutePath,
            backend = backend.toRuntimeBackend(),
            maxNumTokens = MAX_TOKENS,
            cacheDir = context.cacheDir.absolutePath
        )

        Log.i(TAG, "Loading ${modelFile.name} on ${backend.label}")
        val created = Engine(config)
        created.initialize()
        engine = created
        loadedPath = modelFile.absolutePath
        loadedBackend = backend
        return created
    }

    /**
     * Text of a single streamed chunk.
     *
     * Streamed chunks are deltas, so they are concatenated with no separator and never trimmed —
     * a chunk boundary frequently falls in the middle of a word.
     */
    private fun rawTextOf(message: Message): String =
        message.contents
            .filterIsInstance<Content.Text>()
            .joinToString(separator = "") { it.text }

    private fun LocalLlmBackend.toRuntimeBackend(): Backend = when (this) {
        LocalLlmBackend.CPU -> Backend.CPU
        LocalLlmBackend.GPU -> Backend.GPU
    }

    private fun describe(t: Throwable): String = when (t) {
        is UnsatisfiedLinkError ->
            "This device's CPU architecture is not supported by the on-device runtime (arm64-v8a or x86_64 is required)."

        is OutOfMemoryError ->
            "Not enough free memory to load the model. Close other apps or use a smaller bundle."

        else -> t.message?.takeIf { it.isNotBlank() }
            ?: "The on-device model could not produce a response."
    }
}
