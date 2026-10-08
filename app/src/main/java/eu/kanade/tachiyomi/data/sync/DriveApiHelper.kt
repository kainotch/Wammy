package eu.kanade.tachiyomi.data.sync

import android.content.Context
import com.google.android.gms.auth.api.identity.AuthorizationClient
import com.google.android.gms.auth.api.identity.AuthorizationRequest
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.common.api.Scope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import logcat.LogPriority
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okio.IOException
import okio.source
import tachiyomi.core.common.util.system.logcat

/**
 * Handles all Google Drive REST API interactions for the hidden appdata folder.
 *
 * Uses OkHttp (already in the project) instead of the heavy Google API Client library.
 * The appdata folder is invisible to the user and other apps — only Wammy can access it.
 */
class DriveApiHelper(
    private val context: Context,
    baseClient: OkHttpClient,
) {
    // Dedicated client with generous timeouts for large backup uploads/downloads
    private val client: OkHttpClient = baseClient.newBuilder()
        .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(90, java.util.concurrent.TimeUnit.SECONDS)
        .writeTimeout(120, java.util.concurrent.TimeUnit.SECONDS)
        .build()

    private val authClient: AuthorizationClient = Identity.getAuthorizationClient(context)

    companion object {
        private const val DRIVE_SCOPE = "https://www.googleapis.com/auth/drive.appdata"
        private const val DRIVE_FILES_URL = "https://www.googleapis.com/drive/v3/files"
        private const val DRIVE_UPLOAD_URL = "https://www.googleapis.com/upload/drive/v3/files"
        private const val SYNC_FILE_NAME = "wammy_backup.tachibk"
        private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
        private val GZIP_MEDIA_TYPE = "application/gzip".toMediaType()
    }

    /**
     * Request the drive.appdata scope from the user.
     * Returns the access token on success, or null if the user needs to resolve a consent screen.
     *
     * @return Pair<accessToken, pendingIntent?> — if pendingIntent is non-null, the caller
     *         must launch it for the user to grant permission.
     */
    suspend fun requestDriveAuthorization(): AuthorizationResult = withContext(Dispatchers.IO) {
        try {
            val request = AuthorizationRequest.builder()
                .setRequestedScopes(listOf(Scope(DRIVE_SCOPE)))
                .build()

            val result = authClient.authorize(request).await()

            if (result.hasResolution()) {
                // User needs to grant permission — caller must launch the PendingIntent
                AuthorizationResult.NeedsConsent(result.pendingIntent!!)
            } else {
                val token = result.accessToken
                if (token != null) {
                    AuthorizationResult.Success(token)
                } else {
                    AuthorizationResult.Error("No access token returned")
                }
            }
        } catch (e: Exception) {
            logcat(LogPriority.ERROR, e) { "Drive authorization failed" }
            AuthorizationResult.Error(e.message ?: "Unknown error")
        }
    }

    /**
     * Get a fresh access token. Call this before every Drive API request.
     */
    suspend fun getAccessToken(): String? = withContext(Dispatchers.IO) {
        try {
            val request = AuthorizationRequest.builder()
                .setRequestedScopes(listOf(Scope(DRIVE_SCOPE)))
                .build()
            val result = authClient.authorize(request).await()
            result.accessToken
        } catch (e: Exception) {
            logcat(LogPriority.ERROR, e) { "Failed to get Drive access token" }
            null
        }
    }

    /**
     * Gets the last modified time of the sync file in the cloud (ISO 8601 string)
     */
    suspend fun getSyncFileModifiedTime(accessToken: String): String? = withContext(Dispatchers.IO) {
        val query = java.net.URLEncoder.encode("name='wammy_backup.tachibk' or name='wammy_sync.json.gz'", "UTF-8")
        val url = "$DRIVE_FILES_URL?spaces=appDataFolder&q=$query&fields=files(id,modifiedTime)"
        val request = Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer $accessToken")
            .get()
            .build()

        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext null
                val body = response.body?.string() ?: return@withContext null
                val files = org.json.JSONObject(body).optJSONArray("files")
                if (files != null && files.length() > 0) {
                    return@withContext files.getJSONObject(0).optString("modifiedTime")
                }
            }
        } catch (e: Exception) {
            logcat(LogPriority.ERROR, e) { "Failed to fetch file modified time" }
        }
        null
    }

    /**
     * Find the sync file in the appdata folder.
     * @return The file ID, or null if the file doesn't exist yet.
     */
    suspend fun findSyncFile(accessToken: String): String? = withContext(Dispatchers.IO) {
        // Look for either the new or old filename so we don't lose user data from the previous build
        val query = java.net.URLEncoder.encode("name='wammy_backup.tachibk' or name='wammy_sync.json.gz'", "UTF-8")
        val url = "$DRIVE_FILES_URL?spaces=appDataFolder&q=$query&fields=files(id,name,modifiedTime)"
        val request = Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer $accessToken")
            .get()
            .build()

        val body = client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                logcat(LogPriority.ERROR) { "Drive findSyncFile failed: ${response.code}" }
                return@withContext null
            }
            response.body?.string() ?: return@withContext null
        }

        // Parse all files and prefer the .tachibk file over the old .json.gz
        try {
            val files = org.json.JSONObject(body).optJSONArray("files")
            if (files == null || files.length() == 0) return@withContext null

            // Prefer .tachibk over .json.gz
            var tachibkId: String? = null
            var fallbackId: String? = null
            for (i in 0 until files.length()) {
                val file = files.getJSONObject(i)
                val name = file.optString("name")
                val id = file.optString("id")
                if (name == SYNC_FILE_NAME) {
                    tachibkId = id
                } else {
                    fallbackId = id
                }
            }
            tachibkId ?: fallbackId
        } catch (e: Exception) {
            // Fallback to regex parsing
            val idRegex = """"id"\s*:\s*"([^"]+)"""".toRegex()
            idRegex.find(body)?.groupValues?.get(1)
        }
    }

    /**
     * Find ONLY the legacy sync file (wammy_sync.json.gz) for cleanup purposes.
     * @return The file ID of the legacy file, or null if it doesn't exist.
     */
    suspend fun findLegacySyncFile(accessToken: String): String? = withContext(Dispatchers.IO) {
        val query = java.net.URLEncoder.encode("name='wammy_sync.json.gz'", "UTF-8")
        val url = "$DRIVE_FILES_URL?spaces=appDataFolder&q=$query&fields=files(id)"
        val request = Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer $accessToken")
            .get()
            .build()

        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext null
                val body = response.body?.string() ?: return@withContext null
                val files = org.json.JSONObject(body).optJSONArray("files")
                if (files != null && files.length() > 0) {
                    return@withContext files.getJSONObject(0).optString("id")
                }
            }
        } catch (e: Exception) {
            logcat(LogPriority.WARN, e) { "Failed to find legacy sync file" }
        }
        null
    }

    /**
     * Download the sync file from Drive directly to a temp file (streaming, avoids OOM).
     * @return The temp file containing the backup, or null on failure.
     */
    suspend fun downloadSyncFileToFile(accessToken: String, fileId: String): java.io.File? = withContext(Dispatchers.IO) {
        val url = "$DRIVE_FILES_URL/$fileId?alt=media"
        val request = Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer $accessToken")
            .get()
            .build()

        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    logcat(LogPriority.ERROR) { "Drive download failed: ${response.code}" }
                    return@withContext null
                }
                val tempFile = java.io.File(context.cacheDir, "wammy_drive_download.tachibk")
                response.body?.byteStream()?.use { inputStream ->
                    tempFile.outputStream().use { outputStream ->
                        inputStream.copyTo(outputStream)
                    }
                } ?: return@withContext null
                tempFile
            }
        } catch (e: IOException) {
            logcat(LogPriority.ERROR, e) { "Drive download IOException" }
            null
        }
    }

    /**
     * Upload (create or update) the sync file in the appdata folder using streaming.
     * @param file The backup file to upload.
     * @param existingFileId If non-null, updates the existing file. Otherwise creates a new one.
     * @return The file ID of the created/updated file, or null on failure.
     */
    suspend fun uploadSyncFile(
        accessToken: String,
        file: java.io.File,
        existingFileId: String? = null,
    ): String? = withContext(Dispatchers.IO) {
        try {
            if (existingFileId != null) {
                // Update existing file (simple upload)
                updateFile(accessToken, existingFileId, file)
            } else {
                // Create new file (multipart upload with metadata)
                createFile(accessToken, file)
            }
        } catch (e: Exception) {
            logcat(LogPriority.ERROR, e) { "Drive upload failed" }
            null
        }
    }

    private fun createFile(accessToken: String, file: java.io.File): String? {
        // Multipart upload: metadata + content (streams file directly)
        val boundary = "wammy_sync_boundary"
        val metadata = """{"name":"$SYNC_FILE_NAME","parents":["appDataFolder"]}"""

        val metadataPart = buildString {
            append("--$boundary\r\n")
            append("Content-Type: application/json; charset=UTF-8\r\n\r\n")
            append(metadata)
            append("\r\n--$boundary\r\n")
            append("Content-Type: application/gzip\r\n")
            append("Content-Transfer-Encoding: binary\r\n\r\n")
        }.toByteArray()
        val endBoundary = "\r\n--$boundary--\r\n".toByteArray()

        val multipartBody = object : RequestBody() {
            override fun contentType() = "multipart/related; boundary=$boundary".toMediaType()
            override fun contentLength() = metadataPart.size.toLong() + file.length() + endBoundary.size.toLong()
            override fun writeTo(sink: okio.BufferedSink) {
                sink.write(metadataPart)
                file.source().use { source ->
                    sink.writeAll(source)
                }
                sink.write(endBoundary)
            }
        }

        val request = Request.Builder()
            .url("$DRIVE_UPLOAD_URL?uploadType=multipart")
            .addHeader("Authorization", "Bearer $accessToken")
            .post(multipartBody)
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                logcat(LogPriority.ERROR) { "Drive create failed: ${response.code} ${response.body?.string()}" }
                return null
            }

            val body = response.body?.string() ?: return null
            val idRegex = """"id"\s*:\s*"([^"]+)"""".toRegex()
            return idRegex.find(body)?.groupValues?.get(1)
        }
    }

    private fun updateFile(accessToken: String, fileId: String, file: java.io.File): String? {
        val streamBody = object : RequestBody() {
            override fun contentType() = GZIP_MEDIA_TYPE
            override fun contentLength() = file.length()
            override fun writeTo(sink: okio.BufferedSink) {
                file.source().use { source ->
                    sink.writeAll(source)
                }
            }
        }

        val request = Request.Builder()
            .url("$DRIVE_UPLOAD_URL/$fileId?uploadType=media")
            .addHeader("Authorization", "Bearer $accessToken")
            .patch(streamBody)
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                logcat(LogPriority.ERROR) { "Drive update failed: ${response.code} ${response.body?.string()}" }
                return null
            }
            return fileId
        }
    }

    /**
     * Delete the sync file from Drive. Used for cleanup/reset.
     */
    suspend fun deleteSyncFile(accessToken: String, fileId: String): Boolean = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url("$DRIVE_FILES_URL/$fileId")
            .addHeader("Authorization", "Bearer $accessToken")
            .delete()
            .build()

        client.newCall(request).execute().use { response ->
            response.isSuccessful
        }
    }
}

/**
 * Result of requesting Drive authorization.
 */
sealed class AuthorizationResult {
    data class Success(val accessToken: String) : AuthorizationResult()
    data class NeedsConsent(val pendingIntent: android.app.PendingIntent) : AuthorizationResult()
    data class Error(val message: String) : AuthorizationResult()
}
