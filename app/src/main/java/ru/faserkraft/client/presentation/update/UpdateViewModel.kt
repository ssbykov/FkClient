package ru.faserkraft.client.presentation.update

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.receiveAsFlow
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

    private val _events = Channel<UpdateUiEvent>(Channel.BUFFERED)
    val events: Flow<UpdateUiEvent> = _events.receiveAsFlow()

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
                    _events.send(UpdateUiEvent.ShowUpdateDialog(latest))
                }
            }.onFailure { error ->
                updateCheckStarted = false
                _events.send(
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
        appUpdateManager.destroy()
        super.onCleared()
    }
}