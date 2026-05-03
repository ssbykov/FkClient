package ru.faserkraft.client.presentation

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
import javax.inject.Inject

@HiltViewModel
class AppViewModel @Inject constructor(
    private val appAuth: AppAuth,
) : ViewModel() {

    private val _userData = MutableStateFlow<UserData?>(null)
    val userData: StateFlow<UserData?> = _userData

    private val _errorState = MutableSharedFlow<String>()
    val errorState: SharedFlow<String> = _errorState

    init {
        _userData.value = appAuth.getRegistrationData()
    }

    fun reloadUser() {
        _userData.value = appAuth.getRegistrationData()
    }

    fun resetRegistrationData() {
        appAuth.resetRegistration()
        reloadUser()
    }

    fun emitError(message: String) {
        viewModelScope.launch { _errorState.emit(message) }
    }
}