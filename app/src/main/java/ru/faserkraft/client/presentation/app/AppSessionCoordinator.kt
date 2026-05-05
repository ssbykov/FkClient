package ru.faserkraft.client.presentation.app

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppSessionCoordinator @Inject constructor() {

    private val _events = MutableSharedFlow<AppSessionEvent>(extraBufferCapacity = 1)
    val events: SharedFlow<AppSessionEvent> = _events.asSharedFlow()

    suspend fun send(event: AppSessionEvent) {
        _events.emit(event)
    }

}

sealed class AppSessionEvent {
    data object Logout : AppSessionEvent()
}