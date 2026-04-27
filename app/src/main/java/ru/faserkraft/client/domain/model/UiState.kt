package ru.faserkraft.client.domain.model

/**
 * Generic UI State для управления состоянием в UI слое
 */
sealed class UiState<out T> {
    object Idle : UiState<Nothing>()
    object Loading : UiState<Nothing>()
    data class Success<T>(val data: T) : UiState<T>()
    data class Error(val exception: Throwable) : UiState<Nothing>()
}

/**
 * Action state для отслеживания выполнения операций
 */
sealed class ActionState {
    object Idle : ActionState()
    object InProgress : ActionState()
    data class Success(val message: String? = null) : ActionState()
    data class Error(val exception: Throwable) : ActionState()
}

/**
 * Navigation Event
 */
sealed class NavigationEvent {
    object NavigateBack : NavigationEvent()
    object NavigateToProduct : NavigationEvent()
    object NavigateToOrder : NavigationEvent()
    object NavigateToPackaging : NavigationEvent()
    object NavigateToRegistration : NavigationEvent()
}

