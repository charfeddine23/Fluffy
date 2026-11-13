package com.example.drive

import android.content.Context
import android.content.Intent
import android.content.IntentSender
import com.google.android.gms.auth.api.identity.AuthorizationRequest
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.common.api.Scope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder
import java.util.UUID
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume

sealed interface DriveAuthOutcome {
    data class Authorized(val accessToken: String) : DriveAuthOutcome
    data class NeedsConsent(val intentSender: IntentSender) : DriveAuthOutcome
    data class Failed(val message: String) : DriveAuthOutcome
}

sealed interface DriveSyncStatus {
    data object Idle : DriveSyncStatus
    data object BackingUp : DriveSyncStatus
    data object Restoring : DriveSyncStatus
    data class Success(val messageAr: String, val messageEn: String) : DriveSyncStatus
    data class Error(val messageAr: String, val messageEn: String) : DriveSyncStatus
}

object GoogleDriveBackupManager {

    private const val BACKUP_FILE_NAME = "Fluffy_Notes_Backup.json"
    private const val DRIVE_FILES_API = "https://www.googleapis.com/drive/v3/files"
    private const val DRIVE_UPLOAD_API = "https://www.googleapis.com/upload/drive/v3/files"

    private val requestedScopes = listOf(
        Scope("https://www.googleapis.com/auth/drive.file"),
        Scope("https://www.googleapis.com/auth/drive.appdata")
    )

    private val client: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build()
    }

    suspend fun authorize(context: Context): DriveAuthOutcome {
        val request = AuthorizationRequest.builder()
            .setRequestedScopes(requestedScopes)
            .build()

        return suspendCancellableCoroutine { cont ->
            Identity.getAuthorizationClient(context)
                .authorize(request)
                .addOnSuccessListener { result ->
                    val token = result.accessToken
                    val pending = result.pendingIntent
                    if (result.hasResolution() && pending != null) {
                        cont.resume(DriveAuthOutcome.NeedsConsent(pending.intentSender))
                    } else if (!token.isNullOrBlank()) {
                        cont.resume(DriveAuthOutcome.Authorized(token))
                    } else {
                        cont.resume(DriveAuthOutcome.Failed("No access token returned by Google Identity"))
                    }
                }
                .addOnFailureListener { e ->
                    cont.resume(
                        DriveAuthOutcome.Failed(
                            e.localizedMessage ?: "Google Drive authorization unavailable on this device"
                        )
                    )
                }
        }
    }

    fun extractTokenFromConsentResult(context: Context, data: Intent?): String? {
        return try {
            val result = Identity.getAuthorizationClient(context)
                .getAuthorizationResultFromIntent(data)
            result.accessToken?.takeIf { it.isNotBlank() }
        } catch (_: Exception) {
            null
        }
    }

    suspend fun uploadBackupJson(accessToken: String, jsonPayload: String): Boolean =
        withContext(Dispatchers.IO) {
            val driveUploaded = upsertFileInSpace(
                accessToken = accessToken,
                space = "drive",
                jsonPayload = jsonPayload
            )
            val appDataUploaded = upsertFileInSpace(
                accessToken = accessToken,
                space = "appDataFolder",
                jsonPayload = jsonPayload
            )
            if (!driveUploaded && !appDataUploaded) {
                throw IllegalStateException("Google Drive rejected the backup upload")
            }
            true
        }

    suspend fun downloadLatestBackupJson(accessToken: String): String? =
        withContext(Dispatchers.IO) {
            val driveFileId = findExistingFileId(accessToken, "drive")
            val targetId = driveFileId ?: findExistingFileId(accessToken, "appDataFolder")
            if (targetId.isNullOrBlank()) return@withContext null

            val request = Request.Builder()
                .url("$DRIVE_FILES_API/$targetId?alt=media")
                .header("Authorization", "Bearer $accessToken")
                .get()
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    throw IllegalStateException("Drive download failed (HTTP ${response.code})")
                }
                response.body?.string()
            }
        }

    private fun findExistingFileId(accessToken: String, space: String): String? {
        val query = URLEncoder.encode(
            "name = '$BACKUP_FILE_NAME' and trashed = false",
            "UTF-8"
        )
        val url = "$DRIVE_FILES_API?spaces=$space&q=$query&orderBy=modifiedTime%20desc&pageSize=1&fields=files(id,name,modifiedTime)"
        val request = Request.Builder()
            .url(url)
            .header("Authorization", "Bearer $accessToken")
            .get()
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return null
            val body = response.body?.string().orEmpty()
            val files = JSONObject(body).optJSONArray("files") ?: return null
            if (files.length() == 0) return null
            return files.getJSONObject(0).optString("id").ifBlank { null }
        }
    }

    private fun upsertFileInSpace(
        accessToken: String,
        space: String,
        jsonPayload: String
    ): Boolean {
        return try {
            val existingId = findExistingFileId(accessToken, space)
            val boundary = "fluffy_boundary_${UUID.randomUUID()}"

            val metadataJson = JSONObject().apply {
                put("name", BACKUP_FILE_NAME)
                put("mimeType", "application/json")
                if (existingId == null && space == "appDataFolder") {
                    put("parents", JSONArray().put("appDataFolder"))
                }
            }.toString()

            val multipartBody = buildString {
                append("--").append(boundary).append("\r\n")
                append("Content-Type: application/json; charset=UTF-8\r\n\r\n")
                append(metadataJson).append("\r\n")
                append("--").append(boundary).append("\r\n")
                append("Content-Type: application/json; charset=UTF-8\r\n\r\n")
                append(jsonPayload).append("\r\n")
                append("--").append(boundary).append("--\r\n")
            }

            val mediaType = "multipart/related; boundary=$boundary".toMediaType()
            val requestBody = multipartBody.toRequestBody(mediaType)

            val builder = Request.Builder()
                .header("Authorization", "Bearer $accessToken")

            val request = if (existingId != null) {
                builder
                    .url("$DRIVE_UPLOAD_API/$existingId?uploadType=multipart")
                    .patch(requestBody)
                    .build()
            } else {
                builder
                    .url("$DRIVE_UPLOAD_API?uploadType=multipart")
                    .post(requestBody)
                    .build()
            }

            client.newCall(request).execute().use { response ->
                response.isSuccessful
            }
        } catch (_: Exception) {
            false
        }
    }
}
