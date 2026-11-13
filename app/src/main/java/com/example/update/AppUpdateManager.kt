package com.example.update

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit

data class ReleaseUpdateInfo(
    val versionName: String,
    val remoteVersionCode: Int?,
    val releaseTitle: String,
    val releaseNotes: String,
    val apkUrl: String,
    val apkName: String,
    val apkSizeBytes: Long
)

sealed interface UpdateUiState {
    data object Idle : UpdateUiState
    data object Checking : UpdateUiState
    data object UpToDate : UpdateUiState
    data class Available(val info: ReleaseUpdateInfo) : UpdateUiState
    data class Downloading(
        val info: ReleaseUpdateInfo,
        val progress: Float,
        val downloadedBytes: Long,
        val totalBytes: Long
    ) : UpdateUiState
    data class ReadyToInstall(val info: ReleaseUpdateInfo, val apkFile: File) : UpdateUiState
    data class NeedsInstallPermission(val info: ReleaseUpdateInfo, val apkFile: File) : UpdateUiState
    data class Error(
        val messageAr: String,
        val messageEn: String,
        val info: ReleaseUpdateInfo? = null
    ) : UpdateUiState
}

object AppUpdateManager {

    private const val GITHUB_LATEST_RELEASE_API =
        "https://api.github.com/repos/charfeddine23/Fluffy/releases/latest"

    private val client: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .followRedirects(true)
            .followSslRedirects(true)
            .build()
    }

    sealed interface CheckResult {
        data class UpdateAvailable(val info: ReleaseUpdateInfo) : CheckResult
        data object AlreadyLatest : CheckResult
        data class Failure(val messageAr: String, val messageEn: String) : CheckResult
    }

    suspend fun checkForUpdate(): CheckResult = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url(GITHUB_LATEST_RELEASE_API)
                .header("Accept", "application/vnd.github+json")
                .header("User-Agent", "Fluffy-Android/${BuildConfig.VERSION_NAME}")
                .build()

            client.newCall(request).execute().use { response ->
                if (response.code == 404) {
                    return@withContext CheckResult.AlreadyLatest
                }
                if (!response.isSuccessful) {
                    return@withContext CheckResult.Failure(
                        messageAr = "تعذر الاتصال بخادم التحديثات (${response.code})",
                        messageEn = "Unable to reach update server (${response.code})"
                    )
                }

                val bodyString = response.body?.string().orEmpty()
                if (bodyString.isBlank()) {
                    return@withContext CheckResult.AlreadyLatest
                }

                val json = JSONObject(bodyString)
                val tagName = json.optString("tag_name", "").trim()
                val releaseName = json.optString("name", tagName).trim()
                val rawNotes = json.optString("body", "").trim()
                val assets = json.optJSONArray("assets") ?: return@withContext CheckResult.AlreadyLatest

                var apkUrl = ""
                var apkName = ""
                var apkSize = 0L

                for (i in 0 until assets.length()) {
                    val asset = assets.getJSONObject(i)
                    val name = asset.optString("name", "")
                    if (name.endsWith(".apk", ignoreCase = true)) {
                        apkUrl = asset.optString("browser_download_url", "")
                        apkName = name
                        apkSize = asset.optLong("size", 0L)
                        break
                    }
                }

                if (apkUrl.isBlank()) {
                    return@withContext CheckResult.AlreadyLatest
                }

                val remoteVersion = tagName.removePrefix("v").removePrefix("V").trim()
                if (remoteVersion.isBlank()) {
                    return@withContext CheckResult.AlreadyLatest
                }

                val remoteCode = extractVersionCode(rawNotes)
                val currentVersion = BuildConfig.VERSION_NAME.removePrefix("v").removePrefix("V").trim()
                val currentCode = BuildConfig.VERSION_CODE

                val isNewer = when {
                    remoteCode != null && remoteCode > currentCode -> true
                    compareVersions(remoteVersion, currentVersion) > 0 -> true
                    else -> false
                }

                if (!isNewer) {
                    return@withContext CheckResult.AlreadyLatest
                }

                val cleanedNotes = cleanReleaseNotes(rawNotes)
                CheckResult.UpdateAvailable(
                    ReleaseUpdateInfo(
                        versionName = remoteVersion,
                        remoteVersionCode = remoteCode,
                        releaseTitle = releaseName.ifBlank { "Fluffy v$remoteVersion" },
                        releaseNotes = cleanedNotes,
                        apkUrl = apkUrl,
                        apkName = apkName,
                        apkSizeBytes = apkSize
                    )
                )
            }
        } catch (e: Exception) {
            CheckResult.Failure(
                messageAr = "تحقق من اتصالك بالإنترنت وحاول مجدداً",
                messageEn = e.localizedMessage ?: "Network error while checking updates"
            )
        }
    }

    suspend fun downloadApk(
        context: Context,
        info: ReleaseUpdateInfo,
        onProgress: (progress: Float, downloadedBytes: Long, totalBytes: Long) -> Unit
    ): File = withContext(Dispatchers.IO) {
        val updatesDir = File(context.cacheDir, "updates").apply {
            if (!exists()) mkdirs()
        }

        // Remove older cached APKs
        updatesDir.listFiles()?.forEach { old ->
            if (old.isFile) old.delete()
        }

        val targetFile = File(updatesDir, info.apkName.ifBlank { "Fluffy-v${info.versionName}.apk" })
        val tempFile = File(updatesDir, "${targetFile.name}.part")

        val request = Request.Builder()
            .url(info.apkUrl)
            .header("User-Agent", "Fluffy-Android/${BuildConfig.VERSION_NAME}")
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IllegalStateException("Download failed with HTTP ${response.code}")
            }

            val body = response.body ?: throw IllegalStateException("Empty download response")
            val totalBytes = body.contentLength().takeIf { it > 0L } ?: info.apkSizeBytes

            body.byteStream().use { input ->
                FileOutputStream(tempFile).use { output ->
                    val buffer = ByteArray(8192)
                    var bytesRead: Int
                    var downloaded = 0L
                    var lastReportTime = 0L

                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        output.write(buffer, 0, bytesRead)
                        downloaded += bytesRead

                        val now = System.currentTimeMillis()
                        if (now - lastReportTime >= 60L || (totalBytes > 0 && downloaded >= totalBytes)) {
                            lastReportTime = now
                            val fraction = if (totalBytes > 0L) {
                                (downloaded.toFloat() / totalBytes.toFloat()).coerceIn(0f, 1f)
                            } else {
                                0f
                            }
                            onProgress(fraction, downloaded, totalBytes)
                        }
                    }
                    output.flush()
                }
            }
        }

        if (targetFile.exists()) targetFile.delete()
        if (!tempFile.renameTo(targetFile)) {
            tempFile.copyTo(targetFile, overwrite = true)
            tempFile.delete()
        }

        onProgress(1f, targetFile.length(), targetFile.length())
        targetFile
    }

    fun canInstallPackages(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.packageManager.canRequestPackageInstalls()
        } else {
            true
        }
    }

    fun createInstallPermissionIntent(context: Context): Intent? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Intent(
                Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                Uri.parse("package:${context.packageName}")
            ).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        } else null
    }

    fun installApk(context: Context, apkFile: File): Boolean {
        if (!apkFile.exists()) return false
        if (!canInstallPackages(context)) return false

        val contentUri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            apkFile
        )

        val installIntent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(contentUri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(installIntent)
        return true
    }

    private fun extractVersionCode(body: String): Int? {
        val regex = Regex("""Version Code[^\d]*(\d+)""", RegexOption.IGNORE_CASE)
        return regex.find(body)?.groupValues?.getOrNull(1)?.toIntOrNull()
    }

    private fun compareVersions(remote: String, current: String): Int {
        val rParts = remote.split(".", "-", "_").mapNotNull { it.filter(Char::isDigit).toIntOrNull() }
        val cParts = current.split(".", "-", "_").mapNotNull { it.filter(Char::isDigit).toIntOrNull() }
        val maxLen = maxOf(rParts.size, cParts.size)
        for (i in 0 until maxLen) {
            val r = rParts.getOrElse(i) { 0 }
            val c = cParts.getOrElse(i) { 0 }
            if (r != c) return r.compareTo(c)
        }
        return 0
    }

    private fun cleanReleaseNotes(raw: String): String {
        if (raw.isBlank()) return ""
        return raw.lines()
            .map { it.trim() }
            .filter { line ->
                line.isNotBlank() &&
                    !line.startsWith("- **Download APK**") &&
                    !line.startsWith("- **Version Code**")
            }
            .joinToString("\n") { line ->
                line.replace(Regex("""^#{1,6}\s*"""), "")
                    .replace("**", "")
                    .replace("`", "")
                    .trim()
            }
            .take(360)
    }
}
