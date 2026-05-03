package ru.faserkraft.client.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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

    private val _updateAvailable = MutableStateFlow<VersionInfo?>(null)
    val updateAvailable: StateFlow<VersionInfo?> = _updateAvailable.asStateFlow()

    val status: StateFlow<AppUpdateManager.UpdateStatus> = updateManager.status


    fun startUpdate(version: VersionInfo) {
        updateManager.downloadAndInstall(
            fileName = "${version.versionName}.apk"
        )
    }

    fun checkForUpdates(user: UserData) {
        viewModelScope.launch {
            runCatching {
                val latest = repository.getLatestVersion()
                if (latest.versionName > BuildConfig.VERSION_NAME &&
                    user.role in latest.roles
                ) {
                    _updateAvailable.value = latest
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        updateManager.destroy()
    }
}