package ru.faserkraft.client.presentation.app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import ru.faserkraft.client.auth.AppAuth
import ru.faserkraft.client.domain.model.UserData
import ru.faserkraft.client.domain.model.UserRole
import ru.faserkraft.client.domain.usecase.device.RegisterDeviceUseCase
import ru.faserkraft.client.dto.DeviceRequestDto
import ru.faserkraft.client.presentation.base.toErrorMessage
import javax.inject.Inject

@HiltViewModel
class AppViewModel @Inject constructor(
    private val appAuth: AppAuth,
    private val registerDeviceUseCase: RegisterDeviceUseCase,
) : ViewModel() {

    companion object {
        const val DEVICE_ALREADY_REGISTERED = "Устройство уже зарегистрировано"
    }

    private val _errorState = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val errorState: SharedFlow<String> = _errorState

    private val _userData = MutableStateFlow<UserData?>(null)
    val userData: StateFlow<UserData?> = _userData

    init {
        loadUserData()
    }

    private fun loadUserData() {
        _userData.value =
            if (appAuth.checkRegistration() != null) {
                appAuth.getRegistrationData()
            } else {
                null
            }
    }

    fun registerDevice(request: DeviceRequestDto) {
        viewModelScope.launch {
            if (appAuth.checkRegistration() != null) {
                _errorState.emit(DEVICE_ALREADY_REGISTERED)
                return@launch
            }

            runCatching {
                registerDeviceUseCase(request)
            }.onSuccess { registration ->
                val userData = UserData(
                    email = registration.userEmail,
                    password = registration.password,
                    name = registration.userName,
                    role = UserRole.fromValue(registration.userRole) ?: UserRole.WORKER
                )
                appAuth.saveUserData(userData)
                _userData.value = userData
            }.onFailure { error ->
                _errorState.emit(error.toErrorMessage())
            }
        }
    }

    fun resetRegistrationData() {
        appAuth.resetRegistration()
        _userData.value = null
    }
}