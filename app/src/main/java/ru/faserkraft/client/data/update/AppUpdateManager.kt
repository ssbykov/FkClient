package ru.faserkraft.client.data.update

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.os.Environment
import androidx.core.app.NotificationCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import ru.faserkraft.client.domain.repository.UpdateRepository
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppUpdateManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repository: UpdateRepository,
    private val apkInstaller: ApkInstaller
) {

    private val managerScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val _status = MutableStateFlow<UpdateStatus>(UpdateStatus.Idle)
    val status: StateFlow<UpdateStatus> = _status.asStateFlow()

    private val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    companion object {
        private const val CHANNEL_ID = "app_update"
        private const val NOTIFICATION_ID = 1001
    }

    init {
        createNotificationChannel()
    }

    fun downloadAndInstall(apkFileName: String) {
        if (_status.value is UpdateStatus.Downloading ||
            _status.value is UpdateStatus.Pending
        ) return

        val localFile = prepareFile(apkFileName)

        managerScope.launch {
            try {
                _status.value = UpdateStatus.Pending
                showProgressNotification(0)

                repository.downloadApkToFile(
                    destFile = localFile,
                    onProgress = { percent ->
                        _status.value = UpdateStatus.Downloading(percent)
                        showProgressNotification(percent)
                    }
                )

                _status.value = UpdateStatus.Installing
                showDoneNotification()

                when (apkInstaller.install(localFile)) {
                    InstallResult.Started -> Unit
                    InstallResult.RequireUnknownSourcesPermission -> {
                        _status.value = UpdateStatus.Error(
                            "Разрешите установку из неизвестных источников и повторите"
                        )
                    }
                }
            } catch (e: Exception) {
                _status.value = UpdateStatus.Error(
                    e.message ?: "Ошибка во время обновления"
                )
                cancelNotification()
            }
        }
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

        return File(updateDir, fileName).also {
            if (it.exists()) it.delete()
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Обновление приложения",
                NotificationManager.IMPORTANCE_LOW
            )
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun showProgressNotification(percent: Int) {
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentTitle("Обновление приложения")
            .setContentText("Загрузка $percent%")
            .setProgress(100, percent, percent == 0)
            .setOngoing(true)
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
}