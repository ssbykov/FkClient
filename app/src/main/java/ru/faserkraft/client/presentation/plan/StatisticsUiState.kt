package ru.faserkraft.client.presentation.plan

enum class StatMode {
    BY_PROCESS,
    BY_EMPLOYEE
}

enum class StatPeriod {
    MONTH,
    QUARTER,
    YEAR,
}

data class StatisticsUiState(
    val isLoading: Boolean = false,
    val mode: StatMode = StatMode.BY_PROCESS,
    val periodLabel: String = "",
    val periodWorkingDays: Int = 0,
    val totalByProcess: List<ProcessTotalUiItem> = emptyList(),
    val stepsByProcess: List<ProcessStepsUiItem> = emptyList(),
    val employees: List<EmployeeStatsUiItem> = emptyList(),
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

data class EmployeeSizeTypeUiItem(
    val sizeTypeId: Int?,
    val sizeTypeName: String,
    val totalCompleted: Int,
    val steps: List<StepCountUiItem>
)

data class EmployeeStatsUiItem(
    val employeeId: Int,
    val employeeName: String,
    val workingDays: Int,
    val totalCompleted: Int,
    val sizeTypes: List<EmployeeSizeTypeUiItem>
)

data class StepCountUiItem(
    val stepDefinitionId: Int,
    val stepName: String,
    val count: Int,
    val planCount: Int? = null,
    val completionPercentage: Double? = null,
    val dailyAverage: Double = 0.0
)
