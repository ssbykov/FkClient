package ru.faserkraft.client.presentation.update

import ru.faserkraft.client.domain.model.VersionInfo

sealed interface UpdateUiEvent {
    data class ShowUpdateDialog(val version: VersionInfo) : UpdateUiEvent
    data class ShowError(val message: String) : UpdateUiEvent
}