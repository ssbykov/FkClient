package ru.faserkraft.client.presentation.plan

sealed interface PlanEvent {
    data class ShowError(val message: String) : PlanEvent
}