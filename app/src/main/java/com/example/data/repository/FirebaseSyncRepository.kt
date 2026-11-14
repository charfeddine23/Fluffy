package com.example.data.repository

import android.content.Context
import android.util.Base64
import android.util.Log
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.example.data.model.NoteEntity
import com.example.data.model.VaultItemEntity
import com.example.data.storage.FluffySettingsSnapshot
import com.google.firebase.Firebase
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File

data class FirebaseUserInfo(
    val uid: String,
    val email: String?,
    val displayName: String?,
    val photoUrl: String?
)

sealed interface CloudSyncStatus {
    data object Disconnected : CloudSyncStatus
    data object Syncing : CloudSyncStatus
    data class Synced(val lastSyncAt: Long, val syncedCount: Int) : CloudSyncStatus
    data class Error(val messageAr: String, val messageEn: String) : CloudSyncStatus
}

data class RemoteNotesSnapshot(
    val upserted: List<NoteEntity>,
    val removedSyncIds: List<String>,
    val hasPendingWrites: Boolean
)

data class RemoteVaultItemPayload(
    val item: VaultItemEntity,
    val rawFileBase64: String?
)

data class RemoteSettingsPayload(
    val snapshot: FluffySettingsSnapshot,
    val updatedAt: Long
)

object FirebaseSyncRepository {

    private const val TAG = "FirebaseSyncRepository"
    private const val COLLECTION_USERS = "users"
    private const val COLLECTION_NOTES = "notes"
    private const val COLLECTION_SETTINGS = "settings"
    private const val DOC_PREFERENCES = "preferences"
    private const val COLLECTION_VAULT = "vault"

    // Keep inline file base64 strictly below Firestore's 1 MiB document size limit
    private const val MAX_FIRESTORE_INLINE_BYTES = 520 * 1024L

    fun isFirebaseAvailable(context: Context): Boolean {
        return try {
            FirebaseApp.getApps(context.applicationContext).isNotEmpty()
        } catch (_: Exception) {
            false
        }
    }

    fun getWebClientId(context: Context): String {
        val resId = context.applicationContext.resources.getIdentifier(
            "default_web_client_id",
            "string",
            context.applicationContext.packageName
        )
        return if (resId != 0) context.applicationContext.getString(resId) else ""
    }

    private fun getAuthOrNull(context: Context): FirebaseAuth? {
        if (!isFirebaseAvailable(context)) return null
        return runCatching { Firebase.auth }.getOrNull()
    }

    private fun getFirestoreOrNull(context: Context): FirebaseFirestore? {
        if (!isFirebaseAvailable(context)) return null
        return runCatching {
            val res = context.applicationContext.resources
            val dbIdRes = res.getIdentifier(
                "firestore_database_id",
                "string",
                context.applicationContext.packageName
            )
            val dbId = if (dbIdRes != 0) res.getString(dbIdRes).trim() else ""
            if (dbId.isNotEmpty() && dbId != "(default)") {
                FirebaseFirestore.getInstance(FirebaseApp.getInstance(), dbId)
            } else {
                Firebase.firestore
            }
        }.getOrNull()
    }

    fun FirebaseUser.toUserInfo(): FirebaseUserInfo = FirebaseUserInfo(
        uid = uid,
        email = email,
        displayName = displayName,
        photoUrl = photoUrl?.toString()
    )

    fun observeAuthState(context: Context): Flow<FirebaseUserInfo?> {
        val auth = getAuthOrNull(context) ?: return flowOf(null)
        return callbackFlow {
            val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
                trySend(firebaseAuth.currentUser?.toUserInfo())
            }
            auth.addAuthStateListener(listener)
            trySend(auth.currentUser?.toUserInfo())
            awaitClose { auth.removeAuthStateListener(listener) }
        }
    }

    fun currentUser(context: Context): FirebaseUserInfo? {
        return getAuthOrNull(context)?.currentUser?.toUserInfo()
    }

    suspend fun signInWithGoogleIdToken(context: Context, idToken: String): Result<FirebaseUserInfo> =
        withContext(Dispatchers.IO) {
            runCatching {
                val auth = getAuthOrNull(context)
                    ?: throw IllegalStateException("Firebase is not initialized. Check app/google-services.json")
                val credential = GoogleAuthProvider.getCredential(idToken, null)
                val result = auth.signInWithCredential(credential).await()
                val user = result.user ?: throw IllegalStateException("No authenticated user returned")
                user.toUserInfo()
            }
        }

    suspend fun signOut(context: Context) {
        runCatching {
            getAuthOrNull(context)?.signOut()
            val webClientId = getWebClientId(context)
            val gsoBuilder = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
            if (webClientId.isNotBlank()) {
                gsoBuilder.requestIdToken(webClientId)
            }
            GoogleSignIn.getClient(context, gsoBuilder.build()).signOut().await()
            CredentialManager.create(context).clearCredentialState(ClearCredentialStateRequest())
        }
    }

    // --- Cloud Firestore Notes Sync (users/{userId}/notes) ---

    suspend fun upsertNote(context: Context, userId: String, note: NoteEntity) {
        val db = getFirestoreOrNull(context) ?: return
        val syncKey = note.syncId.ifBlank { return }
        val path = "$COLLECTION_USERS/$userId/$COLLECTION_NOTES/$syncKey"
        val data = hashMapOf(
            "syncId" to syncKey,
            "userId" to userId,
            "title" to note.title,
            "content" to note.content,
            "category" to note.category,
            "colorPreset" to note.colorPreset,
            "isPinned" to note.isPinned,
            "isFavorite" to note.isFavorite,
            "isArchived" to note.isArchived,
            "checklistJson" to note.checklistJson,
            "audioDurationSec" to note.audioDurationSec,
            "createdAt" to note.createdAt,
            "updatedAt" to note.updatedAt,
            "sortOrder" to note.sortOrder,
            "serverUpdatedAt" to FieldValue.serverTimestamp()
        )
        try {
            db.collection(COLLECTION_USERS)
                .document(userId)
                .collection(COLLECTION_NOTES)
                .document(syncKey)
                .set(data, SetOptions.merge())
                .await()
        } catch (e: Exception) {
            handleFirestoreError(e, "WRITE", path, userId)
            throw e
        }
    }

    suspend fun deleteNote(context: Context, userId: String, syncId: String) {
        val db = getFirestoreOrNull(context) ?: return
        if (syncId.isBlank()) return
        val path = "$COLLECTION_USERS/$userId/$COLLECTION_NOTES/$syncId"
        try {
            db.collection(COLLECTION_USERS)
                .document(userId)
                .collection(COLLECTION_NOTES)
                .document(syncId)
                .delete()
                .await()
        } catch (e: Exception) {
            handleFirestoreError(e, "DELETE", path, userId)
            throw e
        }
    }

    suspend fun fetchNotesOnce(context: Context, userId: String): List<NoteEntity> {
        val db = getFirestoreOrNull(context) ?: return emptyList()
        val path = "$COLLECTION_USERS/$userId/$COLLECTION_NOTES"
        return try {
            val snap = db.collection(COLLECTION_USERS)
                .document(userId)
                .collection(COLLECTION_NOTES)
                .get()
                .await()
            snap.documents.mapNotNull { it.toNoteEntityOrNull() }
        } catch (e: Exception) {
            handleFirestoreError(e, "LIST", path, userId)
            throw e
        }
    }

    fun observeNotes(context: Context, userId: String): Flow<RemoteNotesSnapshot> {
        val db = getFirestoreOrNull(context) ?: return flowOf()
        val path = "$COLLECTION_USERS/$userId/$COLLECTION_NOTES"
        return callbackFlow {
            val reg = db.collection(COLLECTION_USERS)
                .document(userId)
                .collection(COLLECTION_NOTES)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        handleFirestoreError(error, "LISTEN", path, userId)
                        close(error)
                        return@addSnapshotListener
                    }
                    if (snapshot == null) return@addSnapshotListener

                    val upserted = mutableListOf<NoteEntity>()
                    val removed = mutableListOf<String>()
                    snapshot.documentChanges.forEach { change ->
                        when (change.type) {
                            DocumentChange.Type.ADDED,
                            DocumentChange.Type.MODIFIED -> {
                                change.document.toNoteEntityOrNull()?.let { upserted.add(it) }
                            }
                            DocumentChange.Type.REMOVED -> {
                                val sid = change.document.getString("syncId")
                                    ?.takeIf { it.isNotBlank() }
                                    ?: change.document.id
                                removed.add(sid)
                            }
                        }
                    }

                    trySend(
                        RemoteNotesSnapshot(
                            upserted = upserted,
                            removedSyncIds = removed,
                            hasPendingWrites = snapshot.metadata.hasPendingWrites()
                        )
                    )
                }
            awaitClose { reg.remove() }
        }
    }

    // --- Cloud Firestore Settings Sync (users/{userId}/settings/preferences) ---

    suspend fun upsertSettings(
        context: Context,
        userId: String,
        settings: FluffySettingsSnapshot,
        updatedAt: Long = System.currentTimeMillis()
    ) {
        val db = getFirestoreOrNull(context) ?: return
        val path = "$COLLECTION_USERS/$userId/$COLLECTION_SETTINGS/$DOC_PREFERENCES"
        val data = hashMapOf(
            "userId" to userId,
            "isDarkMode" to settings.isDarkMode,
            "isArabic" to settings.isArabic,
            "isGridView" to settings.isGridView,
            "isBiometricEnabled" to settings.isBiometricEnabled,
            "selectedCategory" to settings.selectedCategory,
            "cloudSyncEnabled" to settings.cloudSyncEnabled,
            "updatedAt" to updatedAt,
            "serverUpdatedAt" to FieldValue.serverTimestamp()
        )
        try {
            db.collection(COLLECTION_USERS)
                .document(userId)
                .collection(COLLECTION_SETTINGS)
                .document(DOC_PREFERENCES)
                .set(data, SetOptions.merge())
                .await()
        } catch (e: Exception) {
            handleFirestoreError(e, "WRITE", path, userId)
            throw e
        }
    }

    suspend fun fetchSettingsOnce(context: Context, userId: String): RemoteSettingsPayload? {
        val db = getFirestoreOrNull(context) ?: return null
        val path = "$COLLECTION_USERS/$userId/$COLLECTION_SETTINGS/$DOC_PREFERENCES"
        return try {
            val doc = db.collection(COLLECTION_USERS)
                .document(userId)
                .collection(COLLECTION_SETTINGS)
                .document(DOC_PREFERENCES)
                .get()
                .await()
            if (!doc.exists()) null else doc.toSettingsPayloadOrNull()
        } catch (e: Exception) {
            handleFirestoreError(e, "GET", path, userId)
            null
        }
    }

    fun observeSettings(context: Context, userId: String): Flow<RemoteSettingsPayload?> {
        val db = getFirestoreOrNull(context) ?: return flowOf()
        val path = "$COLLECTION_USERS/$userId/$COLLECTION_SETTINGS/$DOC_PREFERENCES"
        return callbackFlow {
            val reg = db.collection(COLLECTION_USERS)
                .document(userId)
                .collection(COLLECTION_SETTINGS)
                .document(DOC_PREFERENCES)
                .addSnapshotListener { doc, error ->
                    if (error != null) {
                        handleFirestoreError(error, "LISTEN", path, userId)
                        return@addSnapshotListener
                    }
                    if (doc != null && doc.exists() && !doc.metadata.hasPendingWrites()) {
                        trySend(doc.toSettingsPayloadOrNull())
                    }
                }
            awaitClose { reg.remove() }
        }
    }

    // --- Cloud Firestore Immutable Vault Sync (users/{userId}/vault/{vaultId}) ---

    suspend fun upsertVaultItemIfAbsent(context: Context, userId: String, item: VaultItemEntity) {
        val db = getFirestoreOrNull(context) ?: return
        val path = "$COLLECTION_USERS/$userId/$COLLECTION_VAULT/${item.id}"
        try {
            val ref = db.collection(COLLECTION_USERS)
                .document(userId)
                .collection(COLLECTION_VAULT)
                .document(item.id)

            val existing = ref.get().await()
            if (existing.exists()) return // Write-once immutable guarantee!

            var inlineFileB64: String? = null
            if (item.fileName.isNotEmpty() && item.localFilePath.isNotBlank()) {
                val file = File(item.localFilePath)
                if (file.exists() && file.length() <= MAX_FIRESTORE_INLINE_BYTES) {
                    inlineFileB64 = Base64.encodeToString(file.readBytes(), Base64.NO_WRAP)
                }
            }

            val data = hashMapOf(
                "id" to item.id,
                "userId" to userId,
                "title" to item.title,
                "literalContent" to item.literalContent,
                "fileName" to item.fileName,
                "mimeType" to item.mimeType,
                "byteSize" to item.byteSize,
                "sha256" to item.sha256,
                "createdAt" to item.createdAt,
                "isImmutable" to true,
                "rawFileBase64" to (inlineFileB64 ?: ""),
                "serverCreatedAt" to FieldValue.serverTimestamp()
            )
            ref.set(data).await()
        } catch (e: Exception) {
            handleFirestoreError(e, "WRITE", path, userId)
        }
    }

    suspend fun fetchVaultItemsOnce(context: Context, userId: String): List<RemoteVaultItemPayload> {
        val db = getFirestoreOrNull(context) ?: return emptyList()
        val path = "$COLLECTION_USERS/$userId/$COLLECTION_VAULT"
        return try {
            val snap = db.collection(COLLECTION_USERS)
                .document(userId)
                .collection(COLLECTION_VAULT)
                .get()
                .await()
            snap.documents.mapNotNull { it.toVaultItemPayloadOrNull() }
        } catch (e: Exception) {
            handleFirestoreError(e, "LIST", path, userId)
            emptyList()
        }
    }

    fun observeVaultItems(context: Context, userId: String): Flow<List<RemoteVaultItemPayload>> {
        val db = getFirestoreOrNull(context) ?: return flowOf()
        val path = "$COLLECTION_USERS/$userId/$COLLECTION_VAULT"
        return callbackFlow {
            val reg = db.collection(COLLECTION_USERS)
                .document(userId)
                .collection(COLLECTION_VAULT)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        handleFirestoreError(error, "LISTEN", path, userId)
                        return@addSnapshotListener
                    }
                    if (snapshot == null) return@addSnapshotListener
                    val added = snapshot.documentChanges
                        .filter { it.type == DocumentChange.Type.ADDED }
                        .mapNotNull { it.document.toVaultItemPayloadOrNull() }
                    if (added.isNotEmpty()) {
                        trySend(added)
                    }
                }
            awaitClose { reg.remove() }
        }
    }

    private fun DocumentSnapshot.toNoteEntityOrNull(): NoteEntity? {
        if (!exists()) return null
        val sid = getString("syncId")?.takeIf { it.isNotBlank() } ?: id
        return NoteEntity(
            id = 0L,
            title = getString("title") ?: "",
            content = getString("content") ?: "",
            category = getString("category") ?: "Ideas",
            colorPreset = getString("colorPreset") ?: "frost",
            isPinned = getBoolean("isPinned") ?: false,
            isFavorite = getBoolean("isFavorite") ?: false,
            isArchived = getBoolean("isArchived") ?: false,
            checklistJson = getString("checklistJson") ?: "",
            audioDurationSec = (getLong("audioDurationSec") ?: 0L).toInt(),
            createdAt = getLong("createdAt") ?: System.currentTimeMillis(),
            updatedAt = getLong("updatedAt") ?: System.currentTimeMillis(),
            syncId = sid,
            sortOrder = getLong("sortOrder") ?: 0L
        )
    }

    private fun DocumentSnapshot.toSettingsPayloadOrNull(): RemoteSettingsPayload? {
        if (!exists()) return null
        val snapshot = FluffySettingsSnapshot(
            isDarkMode = getBoolean("isDarkMode") ?: true,
            isArabic = getBoolean("isArabic") ?: true,
            isGridView = getBoolean("isGridView") ?: true,
            isBiometricEnabled = getBoolean("isBiometricEnabled") ?: false,
            selectedCategory = getString("selectedCategory") ?: "ALL",
            cloudSyncEnabled = getBoolean("cloudSyncEnabled") ?: true
        )
        return RemoteSettingsPayload(
            snapshot = snapshot,
            updatedAt = getLong("updatedAt") ?: 0L
        )
    }

    private fun DocumentSnapshot.toVaultItemPayloadOrNull(): RemoteVaultItemPayload? {
        if (!exists()) return null
        val itemId = getString("id")?.takeIf { it.isNotBlank() } ?: id
        val b64 = getString("rawFileBase64")?.takeIf { it.isNotBlank() }
        val item = VaultItemEntity(
            id = itemId,
            title = getString("title") ?: "",
            literalContent = getString("literalContent") ?: "",
            fileName = getString("fileName") ?: "",
            mimeType = getString("mimeType") ?: "",
            localFilePath = "",
            byteSize = getLong("byteSize") ?: 0L,
            sha256 = getString("sha256") ?: "",
            createdAt = getLong("createdAt") ?: System.currentTimeMillis(),
            isImmutable = getBoolean("isImmutable") ?: true
        )
        return RemoteVaultItemPayload(item = item, rawFileBase64 = b64)
    }

    private fun handleFirestoreError(
        error: Throwable,
        operationType: String,
        path: String,
        userId: String
    ) {
        val code = (error as? FirebaseFirestoreException)?.code?.name ?: "UNKNOWN"
        val errJson = JSONObject().apply {
            put("error", error.message ?: "Firestore error")
            put("code", code)
            put("operationType", operationType)
            put("path", path)
            put("userId", userId)
        }
        Log.e(TAG, "Firestore failure: $errJson", error)
    }
}
