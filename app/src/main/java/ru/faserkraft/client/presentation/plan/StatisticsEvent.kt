package ru.faserkraft.client.presentation.plan

sealed interface StatisticsEvent {
    data class ShowError(val message: String) : StatisticsEvent
}