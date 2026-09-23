package com.example.model

sealed class CloudSyncStatus {
    object SignedOut : CloudSyncStatus()
    object SigningIn : CloudSyncStatus()
    data class Ready(val accountEmail: String, val lastSyncTime: Long) : CloudSyncStatus()
    data class Syncing(val currentStep: String, val progress: Float) : CloudSyncStatus()
    data class Success(val accountEmail: String, val lastSyncTime: Long, val itemsSynced: Int) : CloudSyncStatus()
    data class Offline(val reason: String) : CloudSyncStatus()
    data class PermissionDenied(val message: String) : CloudSyncStatus()
    data class TokenExpired(val message: String) : CloudSyncStatus()
    data class Conflict(val remoteTimestamp: Long, val localTimestamp: Long) : CloudSyncStatus()
    data class Error(val errorMessage: String) : CloudSyncStatus()
}

enum class SyncState {
    IDLE,
    SYNCING,
    SUCCESS,
    ERROR,
    OFFLINE
}

data class SyncDevice(
    val deviceId: String,
    val deviceName: String,
    val platform: String,
    val lastActiveTimestamp: Long,
    val isCurrentDevice: Boolean = false
)

data class SyncLogItem(
    val id: String,
    val timestamp: Long,
    val message: String,
    val isSuccess: Boolean,
    val itemsSynced: Int
)

data class SyncBackupManifest(
    val version: Int = 1,
    val timestamp: Long = System.currentTimeMillis(),
    val deviceId: String,
    val accountEmail: String,
    val booksCount: Int,
    val highlightsCount: Int,
    val bookmarksCount: Int,
    val sessionsCount: Int,
    val checksum: String = ""
)

/**
 * Sync state shown in Settings. Every field is a real measurement:
 *  - [payloadBytes] is the byte size of the last exported payload (0 when none).
 *  - [remoteDeviceCount] counts distinct device ids actually merged from a payload.
 * The previous version of this model shipped hardcoded "12.4 MB / 15.0 GB" quotas and three
 * fictional devices; those fields no longer exist.
 */
data class CloudSyncInfo(
    val syncStatus: CloudSyncStatus = CloudSyncStatus.SignedOut,
    val syncState: SyncState = SyncState.IDLE,
    val lastSyncedAt: Long = 0L,
    val autoSyncEnabled: Boolean = true,
    val syncOnWifiOnly: Boolean = false,
    val syncLibrary: Boolean = true,
    val syncHighlights: Boolean = true,
    val syncStreak: Boolean = true,
    val pendingChangesCount: Int = 0,
    val cloudAccountName: String = "",
    val payloadBytes: Long = 0L,
    val remoteDeviceCount: Int = 0,
    val syncLogs: List<SyncLogItem> = emptyList()
)
