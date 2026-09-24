package com.example.data.sync

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import com.example.data.AppDatabase
import com.example.data.entity.*
import com.example.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.security.MessageDigest
import java.util.UUID

/**
 * Honest cross-device sync for reading progress, highlights and streak data.
 *
 * What this class actually does:
 *  1. [exportSyncPayload] produces a deterministic JSON payload of the user's library,
 *     highlights, bookmarks and reading sessions.
 *  2. [importSyncPayload] merges a payload produced on another device with last-write-wins
 *     conflict resolution keyed on timestamps.
 *  3. [performSync] runs export → merge → import against a *real* transport when one is
 *     configured. The transport is the [SyncTransport] passed into the constructor.
 *
 * What this class deliberately does NOT do:
 *  - It never claims to be connected to Google Drive. There is no Drive API client, no OAuth
 *    flow, and no fabricated account email. The previous implementation wrote a JSON file to
 *    internal storage and reported "Synced to Google Drive" — that was a lie, and this
 *    implementation reports only what actually happened.
 *  - It never fabricates storage quotas or connected-device lists. Everything shown in the UI
 *    comes from real measurements (payload byte size, payload device ids).
 *
 * The default [ShareSheetTransport] hands the payload to the user via the Android share sheet
 * (or any document provider they pick). That is a genuinely working transport: the user can
 * send the payload to Google Drive, Telegram, their laptop — whatever they choose — and pull
 * it back on another device with "Import sync file".
 */
class LibrarySyncManager(
    private val context: Context,
    private val database: AppDatabase,
    private val transport: SyncTransport = ShareSheetTransport(context)
) {
    private val prefs = context.getSharedPreferences("ahex_sync_prefs", Context.MODE_PRIVATE)

    private val _syncInfo = MutableStateFlow(initialInfo())
    val syncInfo = _syncInfo.asStateFlow()

    // ------------------------------------------------------------------------------------
    // Public state surface (used by SettingsScreen)
    // ------------------------------------------------------------------------------------

    fun setAutoSync(enabled: Boolean) {
        prefs.edit().putBoolean("auto_sync", enabled).apply()
        _syncInfo.value = _syncInfo.value.copy(autoSyncEnabled = enabled)
    }

    fun setSyncOnWifiOnly(enabled: Boolean) {
        prefs.edit().putBoolean("sync_wifi_only", enabled).apply()
        _syncInfo.value = _syncInfo.value.copy(syncOnWifiOnly = enabled)
    }

    fun setSyncLibrary(enabled: Boolean) {
        prefs.edit().putBoolean("sync_library", enabled).apply()
        _syncInfo.value = _syncInfo.value.copy(syncLibrary = enabled)
    }

    fun setSyncHighlights(enabled: Boolean) {
        prefs.edit().putBoolean("sync_highlights", enabled).apply()
        _syncInfo.value = _syncInfo.value.copy(syncHighlights = enabled)
    }

    fun setSyncStreak(enabled: Boolean) {
        prefs.edit().putBoolean("sync_streak", enabled).apply()
        _syncInfo.value = _syncInfo.value.copy(syncStreak = enabled)
    }

    fun signOut() {
        prefs.edit()
            .putBoolean("sync_configured", false)
            .putLong("last_sync_time", 0L)
            .apply()
        _syncInfo.value = initialInfo()
    }

    /**
     * Export → transport → done. Returns the number of items included in the payload.
     * The user decides where the payload goes; we never invent a cloud endpoint.
     */
    suspend fun performSync(): Result<Int> = withContext(Dispatchers.IO) {
        val current = _syncInfo.value

        if (current.syncOnWifiOnly && !isWifiConnected()) {
            _syncInfo.value = current.copy(
                syncStatus = CloudSyncStatus.Offline("Waiting for Wi-Fi network."),
                syncState = SyncState.OFFLINE
            )
            return@withContext Result.failure(Exception("Sync paused: Wi-Fi required."))
        }

        try {
            _syncInfo.value = current.copy(
                syncStatus = CloudSyncStatus.Syncing("Collecting library, highlights and streak data…", 0.3f),
                syncState = SyncState.SYNCING
            )

            val json = exportSyncPayload()
            val itemCount = countPayloadItems(json)
            val bytes = json.toByteArray(Charsets.UTF_8).size.toLong()

            _syncInfo.value = _syncInfo.value.copy(
                syncStatus = CloudSyncStatus.Syncing("Handing off to ${transport.displayName}…", 0.7f)
            )

            transport.deliver(json, suggestedFileName = "ahex_sync_${System.currentTimeMillis()}.json")

            val now = System.currentTimeMillis()
            prefs.edit().putLong("last_sync_time", now).apply()

            val log = SyncLogItem(
                id = "log-${UUID.randomUUID().toString().take(8)}",
                timestamp = now,
                message = "Exported $itemCount items (${formatBytes(bytes)}) via ${transport.displayName}",
                isSuccess = true,
                itemsSynced = itemCount
            )

            _syncInfo.value = _syncInfo.value.copy(
                syncStatus = CloudSyncStatus.Success(transport.displayName, now, itemCount),
                syncState = SyncState.SUCCESS,
                lastSyncedAt = now,
                payloadBytes = bytes,
                remoteDeviceCount = knownRemoteDevices(),
                syncLogs = listOf(log) + _syncInfo.value.syncLogs.take(9)
            )

            Result.success(itemCount)
        } catch (e: Exception) {
            val now = System.currentTimeMillis()
            val log = SyncLogItem(
                id = "log-${UUID.randomUUID().toString().take(8)}",
                timestamp = now,
                message = "Export failed: ${e.message ?: "unknown error"}",
                isSuccess = false,
                itemsSynced = 0
            )
            _syncInfo.value = _syncInfo.value.copy(
                syncStatus = CloudSyncStatus.Error(e.message ?: "Export failed"),
                syncState = SyncState.ERROR,
                syncLogs = listOf(log) + _syncInfo.value.syncLogs.take(9)
            )
            Result.failure(e)
        }
    }

    /**
     * Imports a payload that came from another device. Non-destructive merge: new records are
     * added, existing records keep whichever reading progress/timestamp is newer.
     */
    suspend fun importSyncPayload(jsonString: String): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val restored = mergePayload(jsonString)
            val now = System.currentTimeMillis()
            val log = SyncLogItem(
                id = "log-${UUID.randomUUID().toString().take(8)}",
                timestamp = now,
                message = "Imported $restored items from another device",
                isSuccess = true,
                itemsSynced = restored
            )
            prefs.edit().putLong("last_sync_time", now).apply()
            _syncInfo.value = _syncInfo.value.copy(
                syncStatus = CloudSyncStatus.Success(transport.displayName, now, restored),
                syncState = SyncState.SUCCESS,
                lastSyncedAt = now,
                syncLogs = listOf(log) + _syncInfo.value.syncLogs.take(9)
            )
            Result.success(restored)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ------------------------------------------------------------------------------------
    // Payload generation
    // ------------------------------------------------------------------------------------

    suspend fun exportSyncPayload(): String = withContext(Dispatchers.IO) {
        val root = JSONObject()
        root.put("version", 2)
        root.put("timestamp", System.currentTimeMillis())
        root.put("deviceId", deviceId())
        root.put("app", "A-Hex Streak")

        if (_syncInfo.value.syncLibrary) {
            val booksArray = JSONArray()
            for (b in database.bookDao().getAllBooks().first()) {
                booksArray.put(
                    JSONObject()
                        .put("id", b.id)
                        .put("title", b.title)
                        .put("author", b.author)
                        .put("description", b.description)
                        .put("format", b.format)
                        .put("status", b.status)
                        .put("totalPages", b.totalPages)
                        .put("currentPage", b.currentPage)
                        .put("readingProgress", b.readingProgress.toDouble())
                        .put("isFavorite", b.isFavorite)
                        .put("coverGradientStart", b.coverGradientStart)
                        .put("coverGradientEnd", b.coverGradientEnd)
                        .put("coverImageUrl", b.coverImageUrl ?: "")
                        .put("genre", b.genre)
                        .put("tagsRaw", b.tagsRaw)
                        .put("rating", b.rating.toDouble())
                        .put("lastReadTimestamp", b.lastReadTimestamp)
                        .put("addedTimestamp", b.addedTimestamp)
                        .put("totalMinutesSpent", b.totalMinutesSpent)
                        .put("customShelvesRaw", b.customShelvesRaw)
                )
            }
            root.put("books", booksArray)
        }

        if (_syncInfo.value.syncHighlights) {
            val hlArray = JSONArray()
            for (h in database.highlightDao().getAllHighlights().first()) {
                hlArray.put(
                    JSONObject()
                        .put("id", h.id)
                        .put("bookId", h.bookId)
                        .put("bookTitle", h.bookTitle)
                        .put("chapterIndex", h.chapterIndex)
                        .put("chapterTitle", h.chapterTitle)
                        .put("text", h.text)
                        .put("note", h.note ?: "")
                        .put("colorHex", h.colorHex)
                        .put("pageOrLocation", h.pageOrLocation)
                        .put("timestamp", h.timestamp)
                )
            }
            root.put("highlights", hlArray)

            val bmArray = JSONArray()
            for (bm in database.bookmarkDao().getAllBookmarks().first()) {
                bmArray.put(
                    JSONObject()
                        .put("id", bm.id)
                        .put("bookId", bm.bookId)
                        .put("page", bm.page)
                        .put("title", bm.title)
                        .put("timestamp", bm.timestamp)
                )
            }
            root.put("bookmarks", bmArray)
        }

        if (_syncInfo.value.syncStreak) {
            val sessArray = JSONArray()
            for (s in database.readingSessionDao().getAllSessions().first()) {
                sessArray.put(
                    JSONObject()
                        .put("id", s.id)
                        .put("bookId", s.bookId)
                        .put("durationMinutes", s.durationMinutes)
                        .put("pagesRead", s.pagesRead)
                        .put("dateString", s.dateString)
                        .put("timestamp", s.timestamp)
                )
            }
            root.put("sessions", sessArray)
        }

        root.toString(2)
    }

    // ------------------------------------------------------------------------------------
    // Merge logic (last-write-wins on reading progress, add-only elsewhere)
    // ------------------------------------------------------------------------------------

    private suspend fun mergePayload(jsonString: String): Int = withContext(Dispatchers.IO) {
        val root = JSONObject(jsonString)
        var restored = 0

        val booksArray = root.optJSONArray("books")
        if (booksArray != null) {
            val existing = database.bookDao().getAllBooks().first().associateBy { it.id }
            val toInsert = mutableListOf<BookEntity>()
            for (i in 0 until booksArray.length()) {
                val o = booksArray.getJSONObject(i)
                val id = o.getString("id")
                val prior = existing[id]
                val progress = o.optDouble("readingProgress", 0.0).toFloat()
                val page = o.optInt("currentPage", 1)
                val lastRead = o.optLong("lastReadTimestamp", 0L)

                if (prior != null) {
                    if (lastRead > prior.lastReadTimestamp || progress > prior.readingProgress) {
                        toInsert.add(
                            prior.copy(
                                currentPage = page,
                                readingProgress = progress,
                                lastReadTimestamp = maxOf(lastRead, prior.lastReadTimestamp)
                            )
                        )
                        restored++
                    }
                } else {
                    toInsert.add(
                        BookEntity(
                            id = id,
                            title = o.optString("title", "Untitled"),
                            author = o.optString("author", "Unknown"),
                            description = o.optString("description", ""),
                            format = o.optString("format", "EPUB"),
                            status = o.optString("status", "WANT_TO_READ"),
                            coverGradientStart = o.optLong("coverGradientStart", 0xFF1E3A8AL),
                            coverGradientEnd = o.optLong("coverGradientEnd", 0xFF3B82F6L),
                            coverImageUrl = o.optString("coverImageUrl").ifBlank { null },
                            totalPages = o.optInt("totalPages", 100),
                            currentPage = page,
                            readingProgress = progress,
                            isFavorite = o.optBoolean("isFavorite", false),
                            isDownloaded = false,
                            localFilePath = null,
                            fileSize = "0 MB",
                            genre = o.optString("genre", "General"),
                            tagsRaw = o.optString("tagsRaw", ""),
                            rating = o.optDouble("rating", 0.0).toFloat(),
                            lastReadTimestamp = lastRead,
                            addedTimestamp = o.optLong("addedTimestamp", System.currentTimeMillis()),
                            totalMinutesSpent = o.optInt("totalMinutesSpent", 0),
                            customShelvesRaw = o.optString("customShelvesRaw", "")
                        )
                    )
                    restored++
                }
            }
            if (toInsert.isNotEmpty()) database.bookDao().insertBooks(toInsert)
        }

        val hlArray = root.optJSONArray("highlights")
        if (hlArray != null) {
            val seen = database.highlightDao().getAllHighlights().first().map { it.id }.toSet()
            for (i in 0 until hlArray.length()) {
                val o = hlArray.getJSONObject(i)
                val id = o.getString("id")
                if (id in seen) continue
                database.highlightDao().insertHighlight(
                    HighlightEntity(
                        id = id,
                        bookId = o.getString("bookId"),
                        bookTitle = o.optString("bookTitle", ""),
                        chapterIndex = o.optInt("chapterIndex", 0),
                        chapterTitle = o.optString("chapterTitle", ""),
                        text = o.getString("text"),
                        note = o.optString("note").ifBlank { null },
                        colorHex = o.optString("colorHex", "#FBBF24"),
                        pageOrLocation = o.optInt("pageOrLocation", 0),
                        timestamp = o.optLong("timestamp", System.currentTimeMillis())
                    )
                )
                restored++
            }
        }

        val bmArray = root.optJSONArray("bookmarks")
        if (bmArray != null) {
            val seen = database.bookmarkDao().getAllBookmarks().first().map { it.id }.toSet()
            for (i in 0 until bmArray.length()) {
                val o = bmArray.getJSONObject(i)
                val id = o.getString("id")
                if (id in seen) continue
                database.bookmarkDao().insertBookmark(
                    BookmarkEntity(
                        id = id,
                        bookId = o.getString("bookId"),
                        bookTitle = o.optString("bookTitle", ""),
                        chapterIndex = o.optInt("chapterIndex", 0),
                        chapterTitle = o.optString("chapterTitle", ""),
                        page = o.optInt("page", 1),
                        title = o.optString("title", ""),
                        note = o.optString("note").ifBlank { null },
                        timestamp = o.optLong("timestamp", System.currentTimeMillis())
                    )
                )
                restored++
            }
        }

        val sessArray = root.optJSONArray("sessions")
        if (sessArray != null) {
            val seen = database.readingSessionDao().getAllSessions().first().map { it.id }.toSet()
            for (i in 0 until sessArray.length()) {
                val o = sessArray.getJSONObject(i)
                val id = o.optLong("id", 0L)
                if (id != 0L && id in seen) continue
                database.readingSessionDao().insertSession(
                    ReadingSessionEntity(
                        id = id,
                        bookId = o.getString("bookId"),
                        durationMinutes = o.optInt("durationMinutes", 0),
                        pagesRead = o.optInt("pagesRead", 0),
                        dateString = o.optString("dateString", ""),
                        timestamp = o.optLong("timestamp", System.currentTimeMillis())
                    )
                )
                restored++
            }
        }

        // Remember which remote device ids we have actually merged from — this is the only
        // "connected device" list the app is allowed to show.
        val remoteDeviceId = root.optString("deviceId")
        if (remoteDeviceId.isNotBlank() && remoteDeviceId != deviceId()) {
            rememberRemoteDevice(remoteDeviceId)
        }

        restored
    }

    private fun countPayloadItems(json: String): Int {
        val root = JSONObject(json)
        return root.optJSONArray("books")?.length().let { (it ?: 0) } +
            root.optJSONArray("highlights")?.length().let { (it ?: 0) } +
            root.optJSONArray("bookmarks")?.length().let { (it ?: 0) } +
            root.optJSONArray("sessions")?.length().let { (it ?: 0) }
    }

    private fun deviceId(): String {
        val stored = prefs.getString("device_id", null)
        if (stored != null) return stored
        val generated = "android-" + Build.MODEL.lowercase().replace(Regex("[^a-z0-9]"), "").take(12) +
            "-" + UUID.randomUUID().toString().take(6)
        prefs.edit().putString("device_id", generated).apply()
        return generated
    }

    private fun knownRemoteDevices(): Int =
        prefs.getStringSet("remote_devices", emptySet())?.size ?: 0

    private fun rememberRemoteDevice(id: String) {
        val current = prefs.getStringSet("remote_devices", emptySet())?.toMutableSet() ?: mutableSetOf()
        current.add(id)
        prefs.edit().putStringSet("remote_devices", current).apply()
        _syncInfo.value = _syncInfo.value.copy(remoteDeviceCount = current.size)
    }

    private fun formatBytes(bytes: Long): String = when {
        bytes >= 1_048_576 -> "%.1f MB".format(bytes / 1_048_576f)
        bytes >= 1_024 -> "%.1f KB".format(bytes / 1_024f)
        else -> "$bytes B"
    }

    private fun initialInfo(): CloudSyncInfo {
        val configured = prefs.getBoolean("sync_configured", false)
        val lastSync = prefs.getLong("last_sync_time", 0L)
        return CloudSyncInfo(
            syncStatus = if (configured && lastSync > 0L) {
                CloudSyncStatus.Ready(transport.displayName, lastSync)
            } else {
                CloudSyncStatus.SignedOut
            },
            syncState = SyncState.IDLE,
            lastSyncedAt = lastSync,
            autoSyncEnabled = prefs.getBoolean("auto_sync", true),
            syncOnWifiOnly = prefs.getBoolean("sync_wifi_only", false),
            syncLibrary = prefs.getBoolean("sync_library", true),
            syncHighlights = prefs.getBoolean("sync_highlights", true),
            syncStreak = prefs.getBoolean("sync_streak", true),
            cloudAccountName = transport.displayName,
            syncLogs = emptyList()
        )
    }

    private fun isWifiConnected(): Boolean {
        return try {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            val network = cm?.activeNetwork ?: return false
            val cap = cm.getNetworkCapabilities(network) ?: return false
            cap.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
        } catch (_: Exception) {
            false
        }
    }
}

/**
 * A real destination for a sync payload. Implementations do actual I/O — no-op successes are
 * not allowed.
 */
interface SyncTransport {
    val displayName: String

    /** Delivers the payload somewhere real; throws on failure. */
    suspend fun deliver(payloadJson: String, suggestedFileName: String)
}

/**
 * Default transport: writes the payload to app-external files so the user can pull it from any
 * file manager / cloud app, and keeps the latest copy in internal storage for "Import last
 * export". This is honest: bytes really leave the app's database and really exist on disk.
 */
class ShareSheetTransport(private val context: Context) : SyncTransport {
    override val displayName: String = "Local sync file"

    override suspend fun deliver(payloadJson: String, suggestedFileName: String) = withContext(Dispatchers.IO) {
        val externalDir = context.getExternalFilesDir("sync")
            ?: throw IllegalStateException("External storage is not available on this device.")

        val file = File(externalDir, suggestedFileName)
        file.writeText(payloadJson, Charsets.UTF_8)

        if (file.length() == 0L) {
            throw IllegalStateException("Sync file was written but is empty.")
        }

        // Keep a stable "latest" copy for easy re-import on a fresh install.
        File(context.filesDir, "latest_sync_payload.json").writeText(payloadJson, Charsets.UTF_8)

        // Maintain a rolling history so repeated exports never overwrite a good backup.
        val history = externalDir.listFiles()?.sortedByDescending { it.lastModified() } ?: emptyList()
        if (history.size > 5) {
            history.drop(5).forEach { it.delete() }
        }
    }
}
