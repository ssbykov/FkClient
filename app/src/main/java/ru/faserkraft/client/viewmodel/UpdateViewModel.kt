package ru.faserkraft.client.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import ru.faserkraft.client.BuildConfig
import ru.faserkraft.client.domain.model.UserData
import ru.faserkraft.client.model.VersionInfo
import ru.faserkraft.client.repository.UpdateRepository
import ru.faserkraft.client.update.AppUpdateManager
import javax.inject.Inject

@HiltViewModel
class UpdateViewModel @Inject constructor(
    private val repository: UpdateRepository,
    application: Application
) : AndroidViewModel(application) {

    private val updateManager = AppUpdateManager(application, repository)

    private val _updateAvailable = MutableSharedFlow<VersionInfo>(extraBufferCapacity = 1)
    val updateAvailable: SharedFlow<VersionInfo> = _updateAvailable.asSharedFlow()

    val status: StateFlow<AppUpdateManager.UpdateStatus> = updateManager.status

    private var updateCheckStarted = false
    private var updateDialogShown = false

    fun checkForUpdates(user: UserData) {
        if (updateCheckStarted) return
        updateCheckStarted = true

        viewModelScope.launch {
            runCatching {
                val latest = repository.getLatestVersion()
                if (
                    !updateDialogShown &&
                    latest.versionName > BuildConfig.VERSION_NAME &&
                    user.role in latest.roles
                ) {
                    updateDialogShown = true
                    _updateAvailable.emit(latest)
                }
            }.onFailure {
                // ошибка проверки обновлений — не критична, молча игнорируем
            }
        }
    }

    fun startUpdate(version: VersionInfo) {
        updateManager.downloadAndInstall(
            fileName = "${version.versionName}.apk"
        )
    }

    override fun onCleared() {
        super.onCleared()
        updateManager.destroy()
    }
}