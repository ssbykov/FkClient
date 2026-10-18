package ru.faserkraft.client.presentation.app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import ru.faserkraft.client.auth.AppAuth
import ru.faserkraft.client.domain.model.DeviceRequest
import ru.faserkraft.client.domain.model.UserData
import ru.faserkraft.client.domain.model.UserRole
import ru.faserkraft.client.domain.usecase.device.LogoutDeviceUseCase
import ru.faserkraft.client.domain.usecase.device.RegisterDeviceUseCase
import ru.faserkraft.client.presentation.base.toErrorMessage
import ru.faserkraft.client.utils.logger.Logger
import javax.inject.Inject

private const val TAG = "AppViewModel"
private const val DEVICE_ALREADY_REGISTERED = "Устройство уже зарегистрировано"

@HiltViewModel
class AppViewModel @Inject constructor(
    private val appAuth: AppAuth,
    private val logoutDeviceUseCase: LogoutDeviceUseCase,
    private val registerDeviceUseCase: RegisterDeviceUseCase,
    private val sessionCoordinator: AppSessionCoordinator,
    private val logger: Logger,
) : ViewModel() {

    private val _userData = MutableStateFlow<UserData?>(null)
    val userData: StateFlow<UserData?> = _userData.asStateFlow()

    private val _events = Channel<AppEvent>(Channel.BUFFERED)
    val events: Flow<AppEvent> = _events.receiveAsFlow()

    private val _errorState = Channel<String>(Channel.BUFFERED)
    val errorState: Flow<String> = _errorState.receiveAsFlow()

    private var isLoggingOut = false

    init {
        _userData.value = appAuth.getRegistrationData()
        observeSessionEvents()
    }

    private fun observeSessionEvents() {
        viewModelScope.launch {
            sessionCoordinator.events.collect { event ->
                when (event) {
                    AppSessionEvent.Logout -> {
                        _userData.value = null
                        _events.send(AppEvent.LogoutCompleted)
                    }
                }
            }
        }
    }

    fun logout() {
        if (isLoggingOut) return
        isLoggingOut = true

        viewModelScope.launch {
            try {
                runCatching { logoutDeviceUseCase() }
                    .onFailure { error ->
                        // Сессия уже недействительна: выход выполнил TokenAuthenticator.
                        if (appAuth.getToken().isNullOrBlank()) return@launch

                        logger.e(TAG, "logout failed", error)
                        _errorState.send(error.toErrorMessage())
                        return@launch
                    }

                appAuth.clear()
                sessionCoordinator.send(AppSessionEvent.Logout)
            } finally {
                isLoggingOut = false
            }
        }
    }

    fun registerDevice(request: DeviceRequest) {
        viewModelScope.launch {
            if (appAuth.checkRegistration() != null) {
                _errorState.send(DEVICE_ALREADY_REGISTERED)
                return@launch
            }

            runCatching {
                val registration = registerDeviceUseCase(request)

                appAuth.saveSession(
                    token = registration.accessToken,
                    refreshToken = registration.refreshToken,
                    expiresAt = registration.expiresAt,
                    userData = UserData(
                        email = registration.userEmail,
                        name = registration.userName,
                        role = UserRole.fromValue(registration.userRole) ?: UserRole.WORKER,
                    ),
                )
                _userData.value = appAuth.getRegistrationData()

                logger.i(TAG, "Device registration completed")
                _events.send(AppEvent.RegistrationCompleted)
            }.onFailure { error ->
                logger.e(TAG, "registerDevice failed", error)
                _errorState.send(error.toErrorMessage())
            }
        }
    }
}

sealed class AppEvent {
    data object RegistrationCompleted : AppEvent()
    data object LogoutCompleted : AppEvent()
}