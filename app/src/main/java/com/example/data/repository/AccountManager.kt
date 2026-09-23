package com.example.data.repository

import android.content.Context
import android.util.Log
import com.example.data.AppDatabase
import com.example.util.BackupManager
import com.example.util.BackupResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.userProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.security.MessageDigest

/**
 * Cloud accounts for A-Hex streak, built on Firebase Authentication + Cloud Firestore.
 *
 * Responsibilities:
 *  1. **Account lifecycle** — email/password sign-up, sign-in, password reset, sign-out.
 *     Sessions survive app restarts (Firebase persists the refresh token on device).
 *  2. **Cloud library sync** — the existing [BackupManager] backup JSON is uploaded to the
 *     signed-in user's private Firestore document and can be downloaded and merged back on
 *     any other device where the same account signs in.
 *
 * Security model:
 *  - Users can only ever read/write `users/{uid}/library/backup` — their own document.
 *  - This is enforced server-side by Firestore security rules (documented in the project
 *    README / dashboard): `allow read, write: if request.auth != null && request.auth.uid == uid`.
 *  - No user content ever passes through our own servers; the app talks to Firebase directly.
 *
 * Sync semantics are a full non-destructive merge (same as the file backup restore):
 * missing records are added, existing records keep the newer reading progress. The payload
 * is size-capped by BackupManager already.
 */
class AccountManager(
    private val context: Context,
    private val database: AppDatabase
) {
    companion object {
        private const val TAG = "AccountManager"

        /** Firestore path for the user's library backup document. */
        private const val BACKUP_COLLECTION = "users"
        private const val BACKUP_DOC_FIELD = "backupJson"
        private const val BACKUP_META_UPDATED = "updatedAt"
        private const val BACKUP_META_CHECKSUM = "checksum"
        private const val BACKUP_META_DEVICE = "deviceName"
        private const val BACKUP_META_ITEM_COUNT = "itemCount"

        /** Firestore documents have a 1 MB doc limit; keep a safety margin. */
        private const val MAX_SYNC_PAYLOAD_CHARS = 900_000
    }

    // ------------------------------------------------------------------
    // Auth state
    // ------------------------------------------------------------------

    data class AccountState(
        val isSignedIn: Boolean = false,
        val userId: String? = null,
        val email: String? = null,
        val displayName: String? = null,
        val isEmailVerified: Boolean = false,
        /** True while the Firebase SDK restores a saved session at app start. */
        val isRestoring: Boolean = false
    )

    private val _accountState = MutableStateFlow(AccountState(isRestoring = true))
    val accountState: StateFlow<AccountState> = _accountState.asStateFlow()

    /**
     * Firebase only initializes after a `google-services.json` is added to the app module. Until
     * then [FirebaseAuth.getInstance] / [FirebaseFirestore.getInstance] throw, so resolve them
     * defensively and surface a clear "not configured" state instead of crashing at launch.
     */
    private val auth: FirebaseAuth? = runCatching { FirebaseAuth.getInstance() }.getOrNull()
    private val firestore: FirebaseFirestore? = runCatching { FirebaseFirestore.getInstance() }.getOrNull()

    /** False until the one-time Firebase setup (google-services.json) is in place. */
    val isConfigured: Boolean get() = auth != null && firestore != null

    private val NOT_CONFIGURED =
        "Cloud accounts aren't set up yet. Add google-services.json to the app (see CLOUD_ACCOUNT_SETUP.md)."

    /** Observable auth state, emitted as [AccountState]. */
    val authStateFlow: Flow<AccountState> = callbackFlow {
        val current = auth
        if (current == null) {
            trySend(AccountState(isRestoring = false))
            awaitClose { }
            return@callbackFlow
        }
        val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            trySend(firebaseAuth.toAccountState(isRestoring = false))
        }
        current.addAuthStateListener(listener)
        // Emit the current snapshot immediately so collectors always have a fresh value.
        trySend(current.toAccountState(isRestoring = false))
        awaitClose { current.removeAuthStateListener(listener) }
    }

    private fun FirebaseUser.toAccountState(isRestoring: Boolean) = AccountState(
        isSignedIn = true,
        userId = uid,
        email = email,
        displayName = displayName,
        isEmailVerified = isEmailVerified,
        isRestoring = isRestoring
    )

    private fun FirebaseAuth.toAccountState(isRestoring: Boolean) =
        currentUser?.toAccountState(isRestoring) ?: AccountState(isSignedIn = false, isRestoring = isRestoring)

    init {
        val current = auth
        if (current != null) {
            // Mirror the auth listener into the StateFlow for UI that just reads state.
            current.addAuthStateListener { firebaseAuth ->
                _accountState.value = firebaseAuth.toAccountState(isRestoring = false)
            }
            // Seed immediately (listener fires async).
            _accountState.value = current.toAccountState(isRestoring = false)
        } else {
            // Firebase not configured: present a plain signed-out state rather than crashing.
            _accountState.value = AccountState(isRestoring = false)
        }
    }

    // ------------------------------------------------------------------
    // Sign-up / sign-in / reset / sign-out
    // ------------------------------------------------------------------

    sealed interface AuthResult {
        data object Success : AuthResult
        data class Failure(val message: String) : AuthResult
    }

    /**
     * Creates a new account. Firebase sends a verification email automatically when the
     * project has "Email link (passwordless sign-in) → verification" enabled; otherwise the
     * account is usable immediately and verification can be triggered later.
     */    suspend fun signUp(email: String, password: String, displayName: String?): AuthResult {
        val current = auth ?: return AuthResult.Failure(NOT_CONFIGURED)
        return try {
            val result = current.createUserWithEmailAndPassword(email.trim(), password).await()
            val user = result.user
                ?: return AuthResult.Failure("Account created but no user was returned. Try signing in.")

            if (!displayName.isNullOrBlank()) {
                user.updateProfile(userProfileChangeRequest { this.displayName = displayName.trim() }).await()
            }
            // Best-effort verification email; failure here does not block the account.
            runCatching { user.sendEmailVerification().await() }

            AuthResult.Success
        } catch (e: Exception) {
            AuthResult.Failure(friendlyAuthError(e))
        }
    }

    suspend fun signIn(email: String, password: String): AuthResult {
        val current = auth ?: return AuthResult.Failure(NOT_CONFIGURED)
        return try {
            current.signInWithEmailAndPassword(email.trim(), password).await()
            AuthResult.Success
        } catch (e: Exception) {
            AuthResult.Failure(friendlyAuthError(e))
        }
    }

    suspend fun sendPasswordReset(email: String): AuthResult {
        val current = auth ?: return AuthResult.Failure(NOT_CONFIGURED)
        return try {
            current.sendPasswordResetEmail(email.trim()).await()
            AuthResult.Success
        } catch (e: Exception) {
            AuthResult.Failure(friendlyAuthError(e))
        }
    }

    fun signOut() {
        val current = auth ?: return
        current.signOut()
        _accountState.value = current.toAccountState(isRestoring = false)
    }

    /** True when the signed-in user's email is still unverified (nudge banner). */
    fun needsEmailVerification(): Boolean {
        val user = auth?.currentUser ?: return false
        return !user.isEmailVerified
    }

    suspend fun resendVerificationEmail(): AuthResult {
        val current = auth ?: return AuthResult.Failure(NOT_CONFIGURED)
        val user = current.currentUser ?: return AuthResult.Failure("Not signed in.")
        return try {
            user.sendEmailVerification().await()
            AuthResult.Success
        } catch (e: Exception) {
            AuthResult.Failure(friendlyAuthError(e))
        }
    }

    // ------------------------------------------------------------------
    // Cloud library sync (Firestore, per-user document)
    // ------------------------------------------------------------------

    data class SyncCounts(
        val books: Int,
        val highlights: Int,
        val bookmarks: Int,
        val sessions: Int
    ) {
        val total: Int get() = books + highlights + bookmarks + sessions
    }

    private val _syncState = MutableStateFlow(BackupOperationState.IDLE)
    val syncState: StateFlow<BackupOperationState> = _syncState.asStateFlow()

    private val _syncMessage = MutableStateFlow("")
    val syncMessage: StateFlow<String> = _syncMessage.asStateFlow()

    private fun requireSignedIn(): String {
        val current = auth ?: throw IllegalStateException(NOT_CONFIGURED)
        return current.currentUser?.uid ?: throw IllegalStateException("Sign in to sync your library.")
    }

    /** Serialises the local Room database using the same validated backup format. */
    private suspend fun buildBackupJson(): String {
        val json = BackupManager.createBackupJson(database)
        require(json.length <= MAX_SYNC_PAYLOAD_CHARS) {
            "Library backup is too large for one document (${json.length} chars). Export a file backup instead."
        }
        return json
    }

    private fun sha256(text: String): String =
        MessageDigest.getInstance("SHA-256").digest(text.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }

    /** Uploads the current library snapshot to the user's private cloud document. */
    suspend fun uploadLibrary(): Result<SyncCounts> = withContext(Dispatchers.IO) {
        _syncState.value = BackupOperationState.PROCESSING
        _syncMessage.value = "Preparing library snapshot…"

        try {
            val store = firestore ?: error(NOT_CONFIGURED)
            val uid = requireSignedIn()
            val json = buildBackupJson()
            val payload = BackupManager.parseAndValidateBackup(json).getOrThrow()
            val counts = SyncCounts(
                books = payload.books.size,
                highlights = payload.highlights.size,
                bookmarks = payload.bookmarks.size,
                sessions = payload.sessions.size
            )

            _syncMessage.value = "Uploading to your account…"
            val doc = mapOf(
                BACKUP_DOC_FIELD to json,
                BACKUP_META_UPDATED to System.currentTimeMillis(),
                BACKUP_META_CHECKSUM to sha256(json),
                BACKUP_META_DEVICE to android.os.Build.MODEL,
                BACKUP_META_ITEM_COUNT to counts.total
            )
            store.collection(BACKUP_COLLECTION).document(uid)
                .set(doc, SetOptions.merge())
                .await()

            _syncState.value = BackupOperationState.SUCCESS
            _syncMessage.value = "Uploaded ${counts.total} items to your account."
            Result.success(counts)
        } catch (e: Exception) {
            Log.e(TAG, "uploadLibrary failed", e)
            _syncState.value = BackupOperationState.ERROR
            _syncMessage.value = friendlySyncError(e)
            Result.failure(e)
        }
    }

    /** Downloads the cloud snapshot and merges it into the local library (non-destructive). */
    suspend fun downloadLibrary(): Result<SyncCounts> = withContext(Dispatchers.IO) {
        _syncState.value = BackupOperationState.PROCESSING
        _syncMessage.value = "Fetching your cloud library…"

        try {
            val store = firestore ?: error(NOT_CONFIGURED)
            val uid = requireSignedIn()
            val snapshot = store.collection(BACKUP_COLLECTION).document(uid).get().await()
            val json = snapshot.getString(BACKUP_DOC_FIELD)
                ?: return@withContext Result.failure(
                    IllegalStateException("No cloud library yet. Upload from your other device first.")
                )

            _syncMessage.value = "Merging into this device…"
            val result = BackupManager.restoreBackupFromJson(context, json, database)

            when (result) {
                is BackupResult.Success -> {
                    val counts = SyncCounts(
                        books = result.booksCount,
                        highlights = result.highlightsCount,
                        bookmarks = result.bookmarksCount,
                        sessions = result.sessionsCount
                    )
                    _syncState.value = BackupOperationState.SUCCESS
                    _syncMessage.value = "Restored ${counts.total} items from your account."
                    Result.success(counts)
                }
                is BackupResult.Error -> {
                    _syncState.value = BackupOperationState.ERROR
                    _syncMessage.value = result.message
                    Result.failure(IllegalStateException(result.message))
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "downloadLibrary failed", e)
            _syncState.value = BackupOperationState.ERROR
            _syncMessage.value = friendlySyncError(e)
            Result.failure(e)
        }
    }

    /** Metadata for the cloud copy, shown in Settings ("Last synced from Pixel 7, 2 h ago"). */
    data class CloudLibraryMeta(
        val updatedAt: Long,
        val itemCount: Int,
        val deviceName: String
    )

    suspend fun fetchCloudMeta(): CloudLibraryMeta? = withContext(Dispatchers.IO) {
        val store = firestore ?: return@withContext null
        val uid = try { requireSignedIn() } catch (_: IllegalStateException) { return@withContext null }
        try {
            val snapshot = store.collection(BACKUP_COLLECTION).document(uid).get().await()
            if (!snapshot.exists()) return@withContext null
            CloudLibraryMeta(
                updatedAt = snapshot.getLong(BACKUP_META_UPDATED) ?: 0L,
                itemCount = (snapshot.getLong(BACKUP_META_ITEM_COUNT) ?: 0L).toInt(),
                deviceName = snapshot.getString(BACKUP_META_DEVICE) ?: "Unknown device"
            )
        } catch (e: Exception) {
            Log.w(TAG, "fetchCloudMeta failed", e)
            null
        }
    }

    // ------------------------------------------------------------------
    // Error mapping (Firebase codes → human sentences)
    // ------------------------------------------------------------------

    private fun friendlyAuthError(e: Exception): String {
        val message = e.message ?: return "Authentication failed. Please try again."
        return when {
            message.contains("email address is already in use", ignoreCase = true) ||
                message.contains("already in use by another account", ignoreCase = true) ->
                "That email already has an account. Try signing in instead."
            message.contains("password is invalid", ignoreCase = true) ||
                message.contains("credential is invalid", ignoreCase = true) ||
                message.contains("INVALID_LOGIN_CREDENTIALS", ignoreCase = true) ||
                message.contains("The password is invalid", ignoreCase = true) ->
                "Incorrect email or password."
            message.contains("no user record", ignoreCase = true) ->
                "No account found with that email."
            message.contains("malformed or has expired", ignoreCase = true) ->
                "Your session expired. Please sign in again."
            message.contains("too many requests", ignoreCase = true) ||
                message.contains("blocked all requests", ignoreCase = true) ->
                "Too many attempts. Please wait a minute and try again."
            message.contains("weak password", ignoreCase = true) ->
                "Password is too weak — use at least 6 characters."
            message.contains("badly formatted", ignoreCase = true) ->
                "That email address doesn't look right."
            message.contains("network error", ignoreCase = true) ->
                "Network error — check your connection and try again."
            else -> message.substringBefore("\n").take(160)
        }
    }

    private fun friendlySyncError(e: Exception): String = when {
        e.message?.contains("permission", ignoreCase = true) == true ->
            "Cloud sync was denied by security rules. Make sure Firestore rules allow the signed-in user to write their own document."
        e.message?.contains("No cloud library yet", ignoreCase = true) == true -> e.message ?: "Nothing to restore."
        else -> e.message?.take(160) ?: "Cloud sync failed. Check your connection and try again."
    }
}
