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

data class CloudSyncInfo(
    val syncStatus: CloudSyncStatus = CloudSyncStatus.Ready("reader@ahex.cloud", System.currentTimeMillis() - 3600_000L),
    val syncState: SyncState = SyncState.IDLE,
    val lastSyncedAt: Long = System.currentTimeMillis() - 3600_000L,
    val autoSyncEnabled: Boolean = true,
    val syncOnWifiOnly: Boolean = false,
    val syncLibrary: Boolean = true,
    val syncHighlights: Boolean = true,
    val syncStreak: Boolean = true,
    val pendingChangesCount: Int = 0,
    val cloudAccountName: String = "reader@ahex.cloud",
    val cloudStorageUsed: String = "12.4 MB / 15.0 GB (Google Drive)",
    val connectedDevices: List<SyncDevice> = listOf(
        SyncDevice("dev-current", "Android Device (Current)", "Android 15 / M3", System.currentTimeMillis(), true),
        SyncDevice("dev-tablet", "A-Hex E-Reader Tab", "Android 14 Tablet", System.currentTimeMillis() - 86400000L),
        SyncDevice("dev-web", "A-Hex Cloud Sync", "Web Client", System.currentTimeMillis() - 172800000L)
    ),
    val syncLogs: List<SyncLogItem> = listOf(
        SyncLogItem("log-1", System.currentTimeMillis() - 1000 * 60 * 25, "Synchronized library reading progress & 12 highlights with Google Drive", true, 14),
        SyncLogItem("log-2", System.currentTimeMillis() - 1000 * 3600 * 4, "Backup manifest verified: 6 books, 24 highlights, streaks up to date", true, 30)
    )
)
