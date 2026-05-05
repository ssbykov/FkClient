package ru.faserkraft.client.presentation.app

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import ru.faserkraft.client.auth.AppAuth
import ru.faserkraft.client.domain.model.UserData
import ru.faserkraft.client.domain.model.UserRole
import ru.faserkraft.client.domain.usecase.auth.LoginUseCase
import ru.faserkraft.client.domain.usecase.device.RegisterDeviceUseCase
import ru.faserkraft.client.dto.DeviceRequestDto
import ru.faserkraft.client.dto.LoginData
import ru.faserkraft.client.presentation.base.toErrorMessage
import javax.inject.Inject

private const val TAG = "AppViewModel"
private const val DEVICE_ALREADY_REGISTERED = "Устройство уже зарегистрировано"

@HiltViewModel
class AppViewModel @Inject constructor(
    private val appAuth: AppAuth,
    private val loginUseCase: LoginUseCase,
    private val registerDeviceUseCase: RegisterDeviceUseCase,
) : ViewModel() {

    private val _userData = MutableStateFlow<UserData?>(null)
    val userData: StateFlow<UserData?> = _userData.asStateFlow()

    private val _events = MutableSharedFlow<AppEvent>(extraBufferCapacity = 1)
    val events: SharedFlow<AppEvent> = _events.asSharedFlow()

    private val _errorState = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val errorState: SharedFlow<String> = _errorState.asSharedFlow()

    init {
        _userData.value = appAuth.getRegistrationData()
    }

    fun registerDevice(request: DeviceRequestDto) {
        viewModelScope.launch {
            if (appAuth.checkRegistration() != null) {
                _errorState.emit(DEVICE_ALREADY_REGISTERED)
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

                val loginDto = loginUseCase(
                    LoginData(
                        username = registration.userEmail,
                        password = registration.password
                    )
                )

                appAuth.saveToken(loginDto.accessToken)
                _userData.value = appAuth.getRegistrationData()

                Log.i(TAG, "Device registration and auto-login completed")
                _events.emit(AppEvent.RegistrationCompleted)
            }.onFailure { error ->
                Log.e(TAG, "registerDevice failed", error)
                _errorState.emit(error.toErrorMessage())
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            appAuth.clear()
            _userData.value = null
            _events.emit(AppEvent.LogoutCompleted)
        }
    }

}

sealed class AppEvent {
    data object RegistrationCompleted : AppEvent()
    data object LogoutCompleted : AppEvent()
}