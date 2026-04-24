package ru.faserkraft.client.update

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Environment
import android.provider.Settings
import androidx.core.app.NotificationCompat
import androidx.core.net.toUri
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import ru.faserkraft.client.repository.UpdateRepository
import java.io.File

class AppUpdateManager(
    private val context: Context,
    private val repository: UpdateRepository
) {

    private val managerScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val _status = MutableStateFlow<UpdateStatus>(UpdateStatus.Idle)
    val status: StateFlow<UpdateStatus> = _status.asStateFlow()

    sealed class UpdateStatus {
        object Idle : UpdateStatus()
        object Pending : UpdateStatus()
        data class Downloading(val percent: Int) : UpdateStatus()
        object Installing : UpdateStatus()
        data class Error(val message: String) : UpdateStatus()
    }

    private val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    companion object {
        private const val CHANNEL_ID = "app_update"
        private const val NOTIFICATION_ID = 1001
    }

    init {
        createNotificationChannel()
    }

    fun downloadAndInstall(fileName: String) {
        if (_status.value is UpdateStatus.Downloading ||
            _status.value is UpdateStatus.Pending
        ) return

        val localFile = prepareFile(fileName)

        managerScope.launch {
            try {
                _status.value = UpdateStatus.Pending
                showProgressNotification(0)

                android.util.Log.d(
                    "AppUpdateManager",
                    "Starting download to: ${localFile.absolutePath}"
                )

                repository.downloadApkToFile(
                    destFile = localFile,
                    onProgress = { percent ->
                        _status.value = UpdateStatus.Downloading(percent)
                        showProgressNotification(percent)
                    }
                )

                android.util.Log.d(
                    "AppUpdateManager",
                    "Download complete, file size: ${localFile.length()}"
                )

                _status.value = UpdateStatus.Installing
                showDoneNotification()

                installApk(localFile)

            } catch (e: Exception) {
                // 👈 раньше здесь ничего не логировалось — исключение терялось
                android.util.Log.e("AppUpdateManager", "Error during update", e)
                _status.value = UpdateStatus.Error("Ошибка: ${e.message}")
                cancelNotification()
            }
        }
    }

    // --- Уведомления ---

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Обновление приложения",
                NotificationManager.IMPORTANCE_LOW  // LOW — без звука
            )
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun showProgressNotification(percent: Int) {
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentTitle("Обновление приложения")
            .setContentText("Загрузка $percent%")
            .setProgress(100, percent, percent == 0)  // indeterminate пока 0%
            .setOngoing(true)   // нельзя смахнуть во время загрузки
            .setOnlyAlertOnce(true)
            .build()

        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    private fun showDoneNotification() {
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_download_done)
            .setContentTitle("Обновление загружено")
            .setContentText("Нажмите для установки")
            .setOngoing(false)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    private fun cancelNotification() {
        notificationManager.cancel(NOTIFICATION_ID)
    }

    fun destroy() {
        managerScope.cancel()
        cancelNotification()
    }

    private fun prepareFile(fileName: String): File {
        val updateDir = File(
            context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS),
            "updates"
        ).also { it.mkdirs() }
        return File(updateDir, fileName).also { if (it.exists()) it.delete() }
    }

    private fun installApk(apkFile: File) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (!context.packageManager.canRequestPackageInstalls()) {
                val settingsIntent = Intent(
                    Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                    "package:${context.packageName}".toUri()
                ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(settingsIntent)
                _status.value = UpdateStatus.Error(
                    "Разрешите установку из неизвестных источников и повторите"
                )
                return
            }
        }

        val uri = androidx.core.content.FileProvider.getUriForFile(
            context,
            "${context.packageName}.provider",
            apkFile
        )

        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        context.startActivity(intent)
    }
}
