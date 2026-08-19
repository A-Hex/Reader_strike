package com.example.model

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

data class CloudSyncInfo(
    val syncState: SyncState = SyncState.IDLE,
    val lastSyncedAt: Long = System.currentTimeMillis() - 3600_000L * 3,
    val autoSyncEnabled: Boolean = true,
    val syncOnWifiOnly: Boolean = false,
    val pendingChangesCount: Int = 0,
    val cloudAccountName: String = "csec423@ahex.cloud",
    val cloudStorageUsed: String = "42.8 MB / 5.0 GB",
    val connectedDevices: List<SyncDevice> = listOf(
        SyncDevice("dev-1", "Pixel 8 Pro (This Device)", "Android 15", System.currentTimeMillis(), true),
        SyncDevice("dev-2", "Hex Book Reader Tab", "Android Tablet", System.currentTimeMillis() - 86400000L),
        SyncDevice("dev-3", "Hex Web Cloud", "Chrome / Desktop", System.currentTimeMillis() - 172800000L)
    ),
    val syncLogs: List<SyncLogItem> = listOf(
        SyncLogItem("log-1", System.currentTimeMillis() - 1000 * 60 * 45, "Synced 8 bookmarks and reading progress for 'The Art of War'", true, 8),
        SyncLogItem("log-2", System.currentTimeMillis() - 1000 * 60 * 180, "Cloud backup completed: 6 books, 24 highlights", true, 30),
        SyncLogItem("log-3", System.currentTimeMillis() - 1000 * 3600 * 24, "Full library catalog synchronized", true, 12)
    )
)
