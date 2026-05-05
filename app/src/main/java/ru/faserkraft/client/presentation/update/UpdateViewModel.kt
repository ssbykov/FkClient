package ru.faserkraft.client.presentation.update

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import ru.faserkraft.client.BuildConfig
import ru.faserkraft.client.data.update.AppUpdateManager
import ru.faserkraft.client.data.update.UpdateStatus
import ru.faserkraft.client.domain.model.UserData
import ru.faserkraft.client.domain.model.VersionInfo
import ru.faserkraft.client.domain.repository.UpdateRepository
import javax.inject.Inject

@HiltViewModel
class UpdateViewModel @Inject constructor(
    private val repository: UpdateRepository,
    private val appUpdateManager: AppUpdateManager
) : ViewModel() {

    private val _events = MutableSharedFlow<UpdateUiEvent>(extraBufferCapacity = 1)
    val events: SharedFlow<UpdateUiEvent> = _events.asSharedFlow()

    val status: StateFlow<UpdateStatus> = appUpdateManager.status

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
                    _events.emit(UpdateUiEvent.ShowUpdateDialog(latest))
                }
            }.onFailure { error ->
                _events.tryEmit(
                    UpdateUiEvent.ShowError(
                        error.message ?: "Ошибка проверки обновлений"
                    )
                )
            }
        }
    }

    fun startUpdate(version: VersionInfo) {
        appUpdateManager.downloadAndInstall(
            apkFileName = "${version.versionName}.apk"
        )
    }

    override fun onCleared() {
        super.onCleared()
        appUpdateManager.destroy()
    }
}