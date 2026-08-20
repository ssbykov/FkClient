package ru.faserkraft.client.presentation.plan

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import ru.faserkraft.client.domain.usecase.statistic.GetProductsStatisticsUseCase
import ru.faserkraft.client.presentation.app.AppSessionCoordinator
import ru.faserkraft.client.presentation.app.AppSessionEvent
import ru.faserkraft.client.presentation.base.toErrorMessage
import ru.faserkraft.client.utils.timeprovider.RealTimeProvider
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class StatisticsViewModel @Inject constructor(
    private val getProductsStatisticsUseCase: GetProductsStatisticsUseCase,
    private val sessionCoordinator: AppSessionCoordinator,
    private val timeProvider: RealTimeProvider,
) : ViewModel() {

    private val _uiState = MutableStateFlow(StatisticsUiState())
    val uiState: StateFlow<StatisticsUiState> = _uiState

    private val _events = Channel<StatisticsEvent>()
    val events = _events.receiveAsFlow()

    private var currentPeriod: StatPeriod = StatPeriod.MONTH
    private var currentOffset: Int = 0

    var currentDateFrom: String = ""
        private set
    var currentDateTo: String = ""
        private set

    init {
        observeSessionEvents()
        loadStatistics()
    }

    private fun observeSessionEvents() {
        viewModelScope.launch {
            sessionCoordinator.events.collect { event ->
                when (event) {
                    AppSessionEvent.Logout -> resetState()
                }
            }
        }
    }

    fun resetState() {
        currentPeriod = StatPeriod.MONTH
        currentOffset = 0
        _uiState.value = StatisticsUiState()
    }

    // ---------- Управление режимом и периодом ----------

    fun setMode(mode: StatMode) {
        _uiState.update { it.copy(mode = mode) }
    }

    fun setPeriod(period: StatPeriod) {
        currentPeriod = period
        currentOffset = 0
        loadStatistics()
    }

    fun shiftPeriod(direction: Int) {
        currentOffset += direction
        loadStatistics()
    }

    fun refresh() {
        loadStatistics()
    }

    fun loadStatistics() {
        val (dateFrom, dateTo) = computeDateRange(currentPeriod, currentOffset)
        currentDateFrom = dateFrom.format(API_DATE_FORMAT)
        currentDateTo = dateTo.format(API_DATE_FORMAT)
        val periodLabel = formatPeriodLabel(dateFrom, currentPeriod)

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, periodLabel = periodLabel) }

            runCatching {
                getProductsStatisticsUseCase(
                    dateFrom = currentDateFrom,
                    dateTo = currentDateTo,
                )
            }
                .onSuccess { statsData ->
                    // 1. Общее количество по видам продукции (процессам)
                    val totalItems = statsData.finishedProducts
                        .map { stat ->
                            ProcessTotalUiItem(
                                processId = stat.processId,
                                processName = stat.processName,
                                completedProducts = stat.count,
                            )
                        }
                        .sortedByDescending { it.completedProducts }

                    // 2. Этапы по видам продукции
                    val stepsItems = statsData.totalSteps
                        .groupBy { it.processId to it.processName }
                        .map { (processKey, employeeSteps) ->
                            val groupedSteps = employeeSteps
                                .groupBy { it.stepDefinitionId }
                                .values
                                .sortedBy { sameList -> sameList.first().order }
                                .map { sameList ->
                                    val firstItem = sameList.first()
                                    StepCountUiItem(
                                        stepDefinitionId = firstItem.stepDefinitionId,
                                        stepName = firstItem.stepName,
                                        count = sameList.sumOf { it.count }
                                    )
                                }

                            ProcessStepsUiItem(
                                processId = processKey.first,
                                processName = processKey.second,
                                steps = groupedSteps
                            )
                        }
                        .sortedByDescending { it.steps.sumOf { step -> step.count } }
                        .filter { it.steps.isNotEmpty() }

                    // 3. Сотрудники -> Типоразмеры -> Этапы
                    val employeeItems = statsData.totalSteps
                        .groupBy { it.employeeId to it.employeeName }
                        .map { (employeeKey, employeeSteps) ->
                            val sizeTypes = employeeSteps
                                .groupBy {
                                    (it.sizeTypeId ?: -1) to (it.sizeTypeName ?: "Без типоразмера")
                                }
                                .map { (sizeTypeKey, sizeTypeSteps) ->
                                    val groupedSteps = sizeTypeSteps
                                        .groupBy { it.order to it.stepName }
                                        .values
                                        .sortedBy { sameList -> sameList.first().order }
                                        .map { sameList ->
                                            val firstItem = sameList.first()
                                            StepCountUiItem(
                                                stepDefinitionId = firstItem.stepDefinitionId,
                                                stepName = firstItem.stepName,
                                                count = sameList.sumOf { it.count }
                                            )
                                        }

                                    EmployeeSizeTypeUiItem(
                                        sizeTypeId = sizeTypeKey.first.takeIf { it != -1 },
                                        sizeTypeName = sizeTypeKey.second,
                                        totalCompleted = groupedSteps.sumOf { it.count },
                                        steps = groupedSteps
                                    )
                                }
                                .sortedByDescending { it.totalCompleted }
                                .filter { it.steps.isNotEmpty() }

                            EmployeeStatsUiItem(
                                employeeId = employeeKey.first,
                                employeeName = employeeKey.second,
                                totalCompleted = sizeTypes.sumOf { it.totalCompleted },
                                sizeTypes = sizeTypes
                            )
                        }
                        .sortedByDescending { it.totalCompleted }
                        .filter { it.sizeTypes.isNotEmpty() }

                    _uiState.update {
                        it.copy(
                            totalByProcess = totalItems,
                            stepsByProcess = stepsItems,
                            employees = employeeItems
                        )
                    }
                }
                .onFailure { emitError(it) }

            _uiState.update { it.copy(isLoading = false) }
        }
    }

    private fun computeDateRange(
        period: StatPeriod,
        offset: Int,
    ): Pair<LocalDate, LocalDate> {
        val today = timeProvider.nowLocalDate()
        return when (period) {
            StatPeriod.MONTH -> {
                val base = today.withDayOfMonth(1).plusMonths(offset.toLong())
                base to base.plusMonths(1).minusDays(1)
            }

            StatPeriod.QUARTER -> {
                val quarterIndex = (today.monthValue - 1) / 3
                val firstMonth = quarterIndex * 3 + 1
                val base = today
                    .withMonth(firstMonth)
                    .withDayOfMonth(1)
                    .plusMonths((offset * 3).toLong())
                base to base.plusMonths(3).minusDays(1)
            }

            StatPeriod.YEAR -> {
                val base = today.withDayOfYear(1).plusYears(offset.toLong())
                base to base.plusYears(1).minusDays(1)
            }
        }
    }

    private fun formatPeriodLabel(from: LocalDate, period: StatPeriod): String {
        val ruLocale = Locale.forLanguageTag("ru")

        return when (period) {
            StatPeriod.MONTH ->
                from.format(DateTimeFormatter.ofPattern("LLLL yyyy", ruLocale))
                    .replaceFirstChar { it.uppercaseChar() }

            StatPeriod.QUARTER ->
                "Кв. ${(from.monthValue - 1) / 3 + 1} ${from.year}"

            StatPeriod.YEAR ->
                from.year.toString()
        }
    }

    private suspend fun emitError(e: Throwable) {
        _events.send(StatisticsEvent.ShowError(e.toErrorMessage()))
    }

    companion object {
        private val API_DATE_FORMAT: DateTimeFormatter =
            DateTimeFormatter.ofPattern("yyyy-MM-dd")
    }
}