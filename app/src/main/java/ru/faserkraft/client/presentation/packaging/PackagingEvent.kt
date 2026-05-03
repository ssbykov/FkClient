package ru.faserkraft.client.presentation.packaging

sealed interface PackagingEvent {
    data class ShowError(val message: String) : PackagingEvent
    data object NavigateToPackaging : PackagingEvent
    data object NavigateToNewPackaging : PackagingEvent
    data object NavigateToEdit : PackagingEvent
    data object PackagingDeleted : PackagingEvent
}