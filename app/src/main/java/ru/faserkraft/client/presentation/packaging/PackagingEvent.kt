package ru.faserkraft.client.presentation.packaging

sealed interface PackagingEvent {
    data object NavigateToPackaging : PackagingEvent
    data object NavigateToNewPackaging : PackagingEvent
    data class ShowError(val message: String) : PackagingEvent
}