package com.personal.mymusic.data.network

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import androidx.core.content.FileProvider
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.File
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class UpdateManager(
    private val context: Context,
    private val client: OkHttpClient
) {
    companion object {
        // IMPORTANT: Change these to match your GitHub repository
        const val GITHUB_OWNER = "maheshkumavat" // Please update this to your real GitHub username
        const val GITHUB_REPO = "mymusic"
        
        private var activeDownloadId: Long = -1L
        private var downloadReceiver: BroadcastReceiver? = null
    }

    data class UpdateInfo(
        val isUpdateAvailable: Boolean,
        val latestVersionName: String,
        val downloadUrl: String,
        val releaseNotes: String
    )

    suspend fun checkForUpdates(): UpdateInfo = withContext(Dispatchers.IO) {
        val url = "https://api.github.com/repos/$GITHUB_OWNER/$GITHUB_REPO/releases/latest"
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "mymusic-android-app")
            .build()

        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    throw IOException("GitHub API returned unsuccessful code: ${response.code}")
                }
                
                val body = response.body?.string() ?: throw IOException("Empty response body from GitHub")
                val json = JSONObject(body)
                val latestVersionTag = json.getString("tag_name")
                val releaseNotes = json.optString("body", "")
                
                val assets = json.getJSONArray("assets")
                var apkDownloadUrl = ""
                for (i in 0 until assets.length()) {
                    val asset = assets.getJSONObject(i)
                    val assetName = asset.getString("name")
                    if (assetName.endsWith(".apk")) {
                        apkDownloadUrl = asset.getString("browser_download_url")
                        break
                    }
                }

                if (apkDownloadUrl.isEmpty()) {
                    throw IOException("No APK found in the latest GitHub release assets.")
                }

                val currentVersionName = getCurrentVersionName()
                val updateAvailable = isNewerVersion(currentVersion = currentVersionName, latestVersion = latestVersionTag)

                UpdateInfo(
                    isUpdateAvailable = updateAvailable,
                    latestVersionName = latestVersionTag,
                    downloadUrl = apkDownloadUrl,
                    releaseNotes = releaseNotes
                )
            }
        } catch (e: Exception) {
            android.util.Log.e("UpdateManager", "Error checking for updates", e)
            throw e
        }
    }

    private fun getCurrentVersionName(): String {
        return try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            packageInfo.versionName ?: "1.0"
        } catch (e: Exception) {
            "1.0"
        }
    }

    fun isNewerVersion(currentVersion: String, latestVersion: String): Boolean {
        val currClean = currentVersion.removePrefix("v").trim()
        val lateClean = latestVersion.removePrefix("v").trim()
        
        val currParts = currClean.split(".").mapNotNull { it.toIntOrNull() }
        val lateParts = lateClean.split(".").mapNotNull { it.toIntOrNull() }
        
        val maxLen = maxOf(currParts.size, lateParts.size)
        for (i in 0 until maxLen) {
            val currVal = currParts.getOrElse(i) { 0 }
            val lateVal = lateParts.getOrElse(i) { 0 }
            
            if (lateVal > currVal) return true
            if (currVal > lateVal) return false
        }
        return false
    }

    fun downloadAndInstallUpdate(latestVersion: String, downloadUrl: String, onDownloadStarted: () -> Unit, onError: (String) -> Unit) {
        // Step 1: Check permission to install unknown apps on Oreo (API 26) and above
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (!context.packageManager.canRequestPackageInstalls()) {
                onError("Please enable 'Install Unknown Apps' permission for MyMusic in the next screen, then try again.")
                try {
                    val intent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                        data = Uri.parse("package:${context.packageName}")
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(intent)
                } catch (e: Exception) {
                    onError("Failed to open install permissions settings: ${e.localizedMessage}")
                }
                return
            }
        }

        // Step 2: Set up download file path
        val destinationFile = File(
            context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS),
            "mymusic-update.apk"
        )
        if (destinationFile.exists()) {
            destinationFile.delete()
        }

        // Step 3: Configure DownloadManager Request
        val request = DownloadManager.Request(Uri.parse(downloadUrl)).apply {
            setTitle("MyMusic Update")
            setDescription("Downloading version $latestVersion")
            setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            setDestinationUri(Uri.fromFile(destinationFile))
            setMimeType("application/vnd.android.package-archive")
        }

        val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        
        // Clean up previous receiver if any
        try {
            downloadReceiver?.let { context.unregisterReceiver(it) }
        } catch (e: Exception) {}

        // Step 4: Register receiver for download completion
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(receiverContext: Context, intent: Intent) {
                val id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1L)
                if (id == activeDownloadId) {
                    context.unregisterReceiver(this)
                    downloadReceiver = null
                    activeDownloadId = -1L
                    
                    if (destinationFile.exists() && destinationFile.length() > 0) {
                        installApk(receiverContext, destinationFile)
                    } else {
                        onError("Downloaded APK file is empty or missing.")
                    }
                }
            }
        }
        
        downloadReceiver = receiver
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(receiver, IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE), Context.RECEIVER_EXPORTED)
        } else {
            context.registerReceiver(receiver, IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE))
        }
        
        // Step 5: Enqueue the download
        try {
            activeDownloadId = downloadManager.enqueue(request)
            onDownloadStarted()
        } catch (e: Exception) {
            onError("Failed to start download: ${e.localizedMessage}")
            try {
                context.unregisterReceiver(receiver)
            } catch (ex: Exception) {}
            downloadReceiver = null
            activeDownloadId = -1L
        }
    }

    private fun installApk(context: Context, file: File) {
        try {
            val apkUri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(apkUri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            android.util.Log.e("UpdateManager", "Failed to launch package installer", e)
        }
    }
}
