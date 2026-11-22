package com.vnpt.pingtool.data.update

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.vnpt.pingtool.BuildConfig
import com.vnpt.pingtool.data.model.UpdateReleaseInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.File
import java.util.concurrent.TimeUnit

class GitHubUpdateManager(context: Context) {

    private val context: Context = context.applicationContext

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    private val repoOwner = "khuongnv"
    private val repoName = "ping"

    private var activeReceiver: BroadcastReceiver? = null

    val currentVersionName: String by lazy {
        try {
            val pInfo = this.context.packageManager.getPackageInfo(this.context.packageName, 0)
            pInfo.versionName ?: BuildConfig.VERSION_NAME
        } catch (_: Exception) {
            BuildConfig.VERSION_NAME
        }
    }

    /** @return thông tin release mới nhất, hoặc null nếu không truy vấn được GitHub. */
    suspend fun checkForUpdates(): UpdateReleaseInfo? = withContext(Dispatchers.IO) {
        val apiUrl = "https://api.github.com/repos/$repoOwner/$repoName/releases/latest"
        try {
            val request = Request.Builder()
                .url(apiUrl)
                .header("Accept", "application/vnd.github+json")
                .header("User-Agent", "PingApp-Updater")
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext null
                val json = JSONObject(response.body?.string().orEmpty())
                val tagName = json.optString("tag_name", "")
                if (tagName.isBlank()) return@withContext null

                var downloadUrl = ""
                val assets = json.optJSONArray("assets")
                if (assets != null) {
                    for (i in 0 until assets.length()) {
                        val asset = assets.getJSONObject(i)
                        if (asset.optString("name", "").endsWith(".apk", ignoreCase = true)) {
                            downloadUrl = asset.optString("browser_download_url", "")
                            break
                        }
                    }
                }

                val cleanTag = tagName.removePrefix("v").removePrefix("V")
                val isNew = isVersionNewer(cleanTag, currentVersionName)

                UpdateReleaseInfo(
                    tagName = tagName,
                    name = json.optString("name", "").ifBlank { tagName },
                    body = json.optString("body", "").trim(),
                    apkDownloadUrl = downloadUrl,
                    publishedAt = json.optString("published_at", ""),
                    hasNewVersion = isNew && downloadUrl.isNotBlank()
                )
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun isVersionNewer(latest: String, current: String): Boolean {
        val latestParts = latest.substringBefore('-').split(".").map { it.toIntOrNull() ?: 0 }
        val currentParts = current.substringBefore('-').split(".").map { it.toIntOrNull() ?: 0 }
        val length = maxOf(latestParts.size, currentParts.size)
        for (i in 0 until length) {
            val l = latestParts.getOrElse(i) { 0 }
            val c = currentParts.getOrElse(i) { 0 }
            if (l > c) return true
            if (l < c) return false
        }
        return false
    }

    fun startDownloadAndInstall(apkUrl: String, tagName: String) {
        try {
            // Android 8+: cần cho phép "Cài ứng dụng không rõ nguồn gốc" cho app này
            if (!context.packageManager.canRequestPackageInstalls()) {
                Toast.makeText(context, "Vui lòng cho phép cài đặt ứng dụng từ nguồn này rồi thử lại", Toast.LENGTH_LONG).show()
                val settingsIntent = Intent(
                    Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                    Uri.parse("package:${context.packageName}")
                ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(settingsIntent)
                return
            }

            val safeTag = tagName.replace(Regex("[^A-Za-z0-9._-]"), "")
            val fileName = "Ping-$safeTag.apk"
            val downloadDir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
                ?: run {
                    Toast.makeText(context, "Không truy cập được bộ nhớ để tải bản cập nhật", Toast.LENGTH_LONG).show()
                    return
                }
            // Xóa file cũ trùng tên để DownloadManager không đổi tên (tránh cài nhầm bản cũ)
            File(downloadDir, fileName).takeIf { it.exists() }?.delete()

            val request = DownloadManager.Request(Uri.parse(apkUrl)).apply {
                setTitle("Đang tải bản cập nhật Ping $tagName")
                setDescription("Tải APK từ GitHub Releases")
                setMimeType("application/vnd.android.package-archive")
                setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                setDestinationInExternalFilesDir(context, Environment.DIRECTORY_DOWNLOADS, fileName)
            }

            val manager = context.getSystemService(Context.DOWNLOAD_SERVICE) as? DownloadManager ?: return
            val downloadId = manager.enqueue(request)
            Toast.makeText(context, "Đang tải bản cập nhật $tagName...", Toast.LENGTH_SHORT).show()

            activeReceiver?.let { runCatching { context.unregisterReceiver(it) } }
            val onComplete = object : BroadcastReceiver() {
                override fun onReceive(ctxt: Context?, intent: Intent?) {
                    val id = intent?.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1L)
                    if (id != downloadId) return
                    runCatching { context.unregisterReceiver(this) }
                    activeReceiver = null
                    handleDownloadCompleted(manager, downloadId, File(downloadDir, fileName))
                }
            }
            activeReceiver = onComplete
            ContextCompat.registerReceiver(
                context,
                onComplete,
                IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE),
                ContextCompat.RECEIVER_EXPORTED
            )
        } catch (e: Exception) {
            Toast.makeText(context, "Không thể tải bản cập nhật: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
        }
    }

    private fun handleDownloadCompleted(manager: DownloadManager, downloadId: Long, file: File) {
        val success = manager.query(DownloadManager.Query().setFilterById(downloadId))?.use { cursor ->
            if (!cursor.moveToFirst()) return@use false
            val statusIdx = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS)
            statusIdx >= 0 && cursor.getInt(statusIdx) == DownloadManager.STATUS_SUCCESSFUL
        } ?: false

        if (!success || !file.exists()) {
            Toast.makeText(context, "Tải bản cập nhật thất bại, vui lòng thử lại", Toast.LENGTH_LONG).show()
            return
        }
        installApk(file)
    }

    private fun installApk(file: File) {
        try {
            val apkUri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            val installIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(apkUri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(installIntent)
        } catch (e: Exception) {
            Toast.makeText(context, "Không thể mở trình cài đặt: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
        }
    }

    fun release() {
        activeReceiver?.let { runCatching { context.unregisterReceiver(it) } }
        activeReceiver = null
    }
}
