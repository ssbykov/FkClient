package ru.faserkraft.client.presentation.app

import android.util.Log
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
import ru.faserkraft.client.domain.model.LoginCredentials
import ru.faserkraft.client.domain.model.UserData
import ru.faserkraft.client.domain.model.UserRole
import ru.faserkraft.client.domain.usecase.auth.LoginUseCase
import ru.faserkraft.client.domain.usecase.device.RegisterDeviceUseCase
import ru.faserkraft.client.presentation.base.toErrorMessage
import javax.inject.Inject

private const val TAG = "AppViewModel"
private const val DEVICE_ALREADY_REGISTERED = "Устройство уже зарегистрировано"

@HiltViewModel
class AppViewModel @Inject constructor(
    private val appAuth: AppAuth,
    private val loginUseCase: LoginUseCase,
    private val registerDeviceUseCase: RegisterDeviceUseCase,
    private val sessionCoordinator: AppSessionCoordinator,
) : ViewModel() {

    private val _userData = MutableStateFlow<UserData?>(null)
    val userData: StateFlow<UserData?> = _userData.asStateFlow()

    private val _events = Channel<AppEvent>(Channel.BUFFERED)
    val events: Flow<AppEvent> = _events.receiveAsFlow()

    private val _errorState = Channel<String>(Channel.BUFFERED)
    val errorState: Flow<String> = _errorState.receiveAsFlow()

    init {
        _userData.value = appAuth.getRegistrationData()
    }

    fun registerDevice(request: DeviceRequest) {
        viewModelScope.launch {
            if (appAuth.checkRegistration() != null) {
                _errorState.send(DEVICE_ALREADY_REGISTERED)
                return@launch
            }

            runCatching {
                val registration = registerDeviceUseCase(request)

                val userData = UserData(
                    email = registration.userEmail,
                    password = registration.password,
                    name = registration.userName,
                    role = UserRole.fromValue(registration.userRole) ?: UserRole.WORKER
                )

                appAuth.saveUserData(userData)

                val token = loginUseCase(
                    LoginCredentials(
                        username = registration.userEmail,
                        password = registration.password
                    )
                )

                appAuth.saveToken(token)
                _userData.value = appAuth.getRegistrationData()

                Log.i(TAG, "Device registration and auto-login completed")
                _events.send(AppEvent.RegistrationCompleted)
            }.onFailure { error ->
                Log.e(TAG, "registerDevice failed", error)
                _errorState.send(error.toErrorMessage())
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            appAuth.clear()
            _userData.value = null
            sessionCoordinator.send(AppSessionEvent.Logout)
            _events.send(AppEvent.LogoutCompleted)
        }
    }
}

sealed class AppEvent {
    data object RegistrationCompleted : AppEvent()
    data object LogoutCompleted : AppEvent()
}