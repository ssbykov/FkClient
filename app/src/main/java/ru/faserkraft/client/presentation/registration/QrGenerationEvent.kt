package ru.faserkraft.client.presentation.registration

sealed class QrGenerationEvent {
    data class ShowError(val message: String) : QrGenerationEvent()
}