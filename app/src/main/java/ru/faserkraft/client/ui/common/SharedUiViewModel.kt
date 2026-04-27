package ru.faserkraft.client.ui.common

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import ru.faserkraft.client.domain.model.ActionState
import ru.faserkraft.client.domain.model.NavigationEvent
import javax.inject.Inject

/**
 * Shared ViewModel для общих состояний UI
 */
@HiltViewModel
class SharedUiViewModel @Inject constructor() : ViewModel() {

    private val _actionState = MutableStateFlow<ActionState>(ActionState.Idle)
    val actionState: StateFlow<ActionState> = _actionState

    private val _navigationEvents = MutableSharedFlow<NavigationEvent>(extraBufferCapacity = 1)
    val navigationEvents: SharedFlow<NavigationEvent> = _navigationEvents

    private val _errorMessages = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val errorMessages: SharedFlow<String> = _errorMessages

    /**
     * Установить состояние действия
     */
    fun setActionState(state: ActionState) {
        _actionState.value = state
    }

    /**
     * Отправить событие навигации
     */
    fun navigate(event: NavigationEvent) {
        _navigationEvents.tryEmit(event)
    }

    /**
     * Показать ошибку
     */
    fun showError(message: String) {
        _errorMessages.tryEmit(message)
    }

    /**
     * Сбросить состояние действия
     */
    fun resetActionState() {
        _actionState.value = ActionState.Idle
    }
}

