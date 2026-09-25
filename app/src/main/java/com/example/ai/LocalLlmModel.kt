package com.example.ai

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Locale

/** Which LiteRT-LM backend the on-device model should run on. */
enum class LocalLlmBackend {
    CPU,
    GPU;

    val label: String
        get() = when (this) {
            CPU -> "CPU"
            GPU -> "GPU"
        }
}

/**
 * Locates the on-device LLM bundle.
 *
 * A `.litertlm` file is 0.5-2 GB, so it is never packaged in the APK and never downloaded behind
 * the user's back. The app loads a model the user actually brought onto the device:
 *
 *  1. a file the user imported from device storage (copied into the app's private `models/`),
 *  2. anything already in the app-specific external `models/` directory,
 *  3. `adb push model.litertlm /data/local/tmp/llm/` during development.
 *
 * Only paths 1-3 are ever read, so the assistant stays offline and the model stays under the
 * user's control.
 */
object LocalLlmModel {

    private const val TAG = "LocalLlmModel"

    const val MODEL_EXTENSION = ".litertlm"
    const val PREFS_NAME = "ahex_ai_settings"
    const val PREF_MODEL_PATH = "local_llm_model_path"
    const val PREF_BACKEND = "local_llm_backend"
    const val PREF_PREFER_LOCAL = "local_llm_preferred"

    const val MODELS_DIR = "models"
    const val DEV_DIR = "/data/local/tmp/llm"

    /** A model bundle must be a real, non-empty `.litertlm` file. */
    fun isSupportedFileName(name: String): Boolean =
        name.isNotBlank() && name.lowercase(Locale.ROOT).endsWith(MODEL_EXTENSION)

    fun listModels(dir: File): List<File> {
        val entries = dir.listFiles() ?: return emptyList()
        return entries
            .filter { it.isFile && it.length() > 0L && isSupportedFileName(it.name) }
            .sortedByDescending { it.lastModified() }
    }

    /**
     * Pure resolution order (explicit import first, then managed directories, then the adb
     * directory) so it can be unit tested without a device.
     */
    fun resolve(configured: File?, searchDirs: List<File>, devDir: File?): File? {
        if (configured != null && configured.isFile && configured.length() > 0L) return configured
        searchDirs.forEach { dir ->
            listModels(dir).firstOrNull()?.let { return it }
        }
        if (devDir != null) listModels(devDir).firstOrNull()?.let { return it }
        return null
    }

    fun managedModelDirs(context: Context): List<File> = listOfNotNull(
        File(context.filesDir, MODELS_DIR),
        context.getExternalFilesDir(MODELS_DIR)
    )

    fun modelFile(context: Context): File? =
        resolve(configuredFile(context), managedModelDirs(context), File(DEV_DIR))

    fun isInstalled(context: Context): Boolean = modelFile(context) != null

    /** Model name for status text; blank when nothing is installed. */
    fun displayName(context: Context): String = modelFile(context)?.name.orEmpty()

    fun sizeLabel(context: Context): String = modelFile(context)?.let { formatSize(it.length()) }.orEmpty()

    fun formatSize(bytes: Long): String = when {
        bytes >= 1_073_741_824L -> String.format(Locale.US, "%.2f GB", bytes / 1_073_741_824.0)
        bytes >= 1_048_576L -> String.format(Locale.US, "%.0f MB", bytes / 1_048_576.0)
        bytes >= 1_024L -> String.format(Locale.US, "%.0f KB", bytes / 1_024.0)
        else -> "$bytes B"
    }

    fun configuredPath(context: Context): String? =
        prefs(context).getString(PREF_MODEL_PATH, null)?.takeIf { it.isNotBlank() }

    fun backend(context: Context): LocalLlmBackend {
        val stored = prefs(context).getString(PREF_BACKEND, null) ?: return LocalLlmBackend.CPU
        return runCatching { LocalLlmBackend.valueOf(stored) }.getOrDefault(LocalLlmBackend.CPU)
    }

    fun setBackend(context: Context, backend: LocalLlmBackend) {
        prefs(context).edit().putString(PREF_BACKEND, backend.name).apply()
    }

    /** When true (the default) an installed model is preferred over the cloud key. */
    fun preferLocal(context: Context): Boolean = prefs(context).getBoolean(PREF_PREFER_LOCAL, true)

    fun setPreferLocal(context: Context, value: Boolean) {
        prefs(context).edit().putBoolean(PREF_PREFER_LOCAL, value).apply()
    }

    /**
     * Copies a user-picked model into private storage. Streaming the copy is the only way to make
     * the model usable by the runtime, which needs a real filesystem path rather than a
     * content:// URI.
     */
    suspend fun importModel(context: Context, uri: Uri): Result<File> = withContext(Dispatchers.IO) {
        try {
            // The runtime keeps the old bundle open; drop it before its file is replaced.
            LocalLlmEngine.release()

            val name = queryDisplayName(context, uri)
                ?: uri.lastPathSegment?.substringAfterLast('/')?.takeIf { it.isNotBlank() }
                ?: "model$MODEL_EXTENSION"

            if (!isSupportedFileName(name)) {
                return@withContext Result.failure(
                    IllegalArgumentException("Pick a $MODEL_EXTENSION model bundle (got \"$name\").")
                )
            }

            val targetDir = File(context.filesDir, MODELS_DIR)
            if (!targetDir.exists() && !targetDir.mkdirs()) {
                return@withContext Result.failure(
                    IllegalStateException("Could not create the model directory.")
                )
            }

            val declaredSize = querySize(context, uri)
            val freeBytes = targetDir.usableSpace
            if (declaredSize > 0L && declaredSize + 32L * 1024L * 1024L > freeBytes) {
                return@withContext Result.failure(
                    IllegalStateException(
                        "Not enough free space: the model needs ${formatSize(declaredSize)} " +
                            "and only ${formatSize(freeBytes)} is available."
                    )
                )
            }

            val target = File(targetDir, name)
            // Replace any previous copy of the same bundle instead of filling storage with clones.
            if (target.exists()) target.delete()

            context.contentResolver.openInputStream(uri)?.use { input ->
                target.outputStream().use { output -> input.copyTo(output, DEFAULT_BUFFER_SIZE) }
            } ?: return@withContext Result.failure(
                IllegalStateException("That file could not be opened.")
            )

            if (!target.isFile || target.length() == 0L) {
                target.delete()
                return@withContext Result.failure(
                    IllegalStateException("The copy finished but the model file is empty.")
                )
            }

            // Free the previous model so two bundles never sit in private storage at once. This
            // runs only after the new copy is known good, so a failed import never costs the user
            // the model they already had.
            removeManagedModels(context, keep = target)
            setConfiguredPath(context, target.absolutePath)

            Result.success(target)
        } catch (t: Throwable) {
            if (t is kotlin.coroutines.cancellation.CancellationException) throw t
            Log.e(TAG, "Model import failed", t)
            Result.failure(t)
        }
    }

    /** Clears the imported model and everything in the managed directories. */
    fun deleteInstalledModels(context: Context) {
        LocalLlmEngine.release()
        removeManagedModels(context, keep = null)
        setConfiguredPath(context, null)
    }

    fun setConfiguredPath(context: Context, path: String?) {
        val clean = path?.trim()?.takeIf { it.isNotBlank() }
        prefs(context).edit().apply {
            if (clean == null) remove(PREF_MODEL_PATH) else putString(PREF_MODEL_PATH, clean)
        }.apply()
    }

    private fun removeManagedModels(context: Context, keep: File?) {
        managedModelDirs(context).forEach { dir ->
            dir.listFiles()?.forEach { file ->
                if (file.isFile && file.absolutePath != keep?.absolutePath) file.delete()
            }
        }
    }

    private fun configuredFile(context: Context): File? =
        configuredPath(context)?.let { File(it) }

    private fun queryDisplayName(context: Context, uri: Uri): String? = runCatching {
        context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
            ?.use { cursor ->
                val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (index >= 0 && cursor.moveToFirst()) cursor.getString(index) else null
            }
    }.getOrNull()

    private fun querySize(context: Context, uri: Uri): Long = runCatching {
        context.contentResolver.query(uri, arrayOf(OpenableColumns.SIZE), null, null, null)
            ?.use { cursor ->
                val index = cursor.getColumnIndex(OpenableColumns.SIZE)
                if (index >= 0 && cursor.moveToFirst()) cursor.getLong(index) else 0L
            } ?: 0L
    }.getOrDefault(0L)

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
}
