package com.example.data.repository

import android.content.Context
import android.net.Uri
import com.example.data.AppDatabase
import com.example.util.BackupManager
import com.example.util.BackupResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.util.UUID

enum class BackupOperationState {
    IDLE,
    PROCESSING,
    SUCCESS,
    ERROR
}

data class BackupLogItem(
    val id: String,
    val timestamp: Long,
    val action: String,
    val details: String,
    val isSuccess: Boolean
)

data class LocalBackupInfo(
    val state: BackupOperationState = BackupOperationState.IDLE,
    val lastBackupTimestamp: Long = 0L,
    val lastRestoreTimestamp: Long = 0L,
    val statusMessage: String = "",
    val backupLogs: List<BackupLogItem> = emptyList()
)

class LocalBackupRepository(private val database: AppDatabase) {

    private val _backupInfo = MutableStateFlow(LocalBackupInfo())
    val backupInfo: StateFlow<LocalBackupInfo> = _backupInfo.asStateFlow()

    suspend fun exportBackup(context: Context, destinationUri: Uri): BackupResult = withContext(Dispatchers.IO) {
        _backupInfo.value = _backupInfo.value.copy(
            state = BackupOperationState.PROCESSING,
            statusMessage = "Exporting library database..."
        )

        val result = BackupManager.exportBackupToUri(context, destinationUri, database)
        when (result) {
            is BackupResult.Success -> {
                val log = BackupLogItem(
                    id = UUID.randomUUID().toString().take(8),
                    timestamp = System.currentTimeMillis(),
                    action = "Export Backup",
                    details = "Exported ${result.booksCount} books, ${result.highlightsCount} highlights, ${result.bookmarksCount} bookmarks",
                    isSuccess = true
                )
                _backupInfo.value = _backupInfo.value.copy(
                    state = BackupOperationState.SUCCESS,
                    lastBackupTimestamp = System.currentTimeMillis(),
                    statusMessage = "Backup successfully exported.",
                    backupLogs = listOf(log) + _backupInfo.value.backupLogs.take(9)
                )
            }
            is BackupResult.Error -> {
                val log = BackupLogItem(
                    id = UUID.randomUUID().toString().take(8),
                    timestamp = System.currentTimeMillis(),
                    action = "Export Backup",
                    details = result.message,
                    isSuccess = false
                )
                _backupInfo.value = _backupInfo.value.copy(
                    state = BackupOperationState.ERROR,
                    statusMessage = result.message,
                    backupLogs = listOf(log) + _backupInfo.value.backupLogs.take(9)
                )
            }
        }
        result
    }

    suspend fun restoreBackup(context: Context, sourceUri: Uri): BackupResult = withContext(Dispatchers.IO) {
        _backupInfo.value = _backupInfo.value.copy(
            state = BackupOperationState.PROCESSING,
            statusMessage = "Validating and restoring backup..."
        )

        val result = BackupManager.restoreBackupFromUri(context, sourceUri, database)
        when (result) {
            is BackupResult.Success -> {
                val log = BackupLogItem(
                    id = UUID.randomUUID().toString().take(8),
                    timestamp = System.currentTimeMillis(),
                    action = "Restore Backup",
                    details = "Restored ${result.booksCount} books, ${result.highlightsCount} highlights, ${result.bookmarksCount} bookmarks",
                    isSuccess = true
                )
                _backupInfo.value = _backupInfo.value.copy(
                    state = BackupOperationState.SUCCESS,
                    lastRestoreTimestamp = System.currentTimeMillis(),
                    statusMessage = "Backup restored successfully.",
                    backupLogs = listOf(log) + _backupInfo.value.backupLogs.take(9)
                )
            }
            is BackupResult.Error -> {
                val log = BackupLogItem(
                    id = UUID.randomUUID().toString().take(8),
                    timestamp = System.currentTimeMillis(),
                    action = "Restore Backup",
                    details = result.message,
                    isSuccess = false
                )
                _backupInfo.value = _backupInfo.value.copy(
                    state = BackupOperationState.ERROR,
                    statusMessage = result.message,
                    backupLogs = listOf(log) + _backupInfo.value.backupLogs.take(9)
                )
            }
        }
        result
    }
}
