package ru.faserkraft.client.presentation.plan

data class StatisticsUiState(
    val isLoading: Boolean = false,
    val periodLabel: String = "",
    val totalByProcess: List<ProcessTotalUiItem> = emptyList(),
    val stepsByProcess: List<ProcessStepsUiItem> = emptyList(),
)

data class ProcessTotalUiItem(
    val processId: Int,
    val processName: String,
    val completedProducts: Int
)

data class ProcessStepsUiItem(
    val processId: Int,
    val processName: String,
    val steps: List<StepCountUiItem>
)

data class StepCountUiItem(
    val stepDefinitionId: Int,
    val stepName: String,
    val count: Int
)

enum class StatPeriod {
    MONTH,
    QUARTER,
    YEAR,
}