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

    // ---------- Управление периодом ----------

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
        val periodLabel = formatPeriodLabel(dateFrom, currentPeriod)

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, periodLabel = periodLabel) }

            runCatching {
                getProductsStatisticsUseCase(
                    dateFrom = dateFrom.format(API_DATE_FORMAT),
                    dateTo = dateTo.format(API_DATE_FORMAT),
                )
            }
                .onSuccess { statsData ->
                    // 1. Верхняя карточка: группируем готовые продукты по процессам
                    val totalItems = statsData.finishedProducts
                        .map { stat ->
                            ProcessTotalUiItem(
                                processId = stat.processId,
                                processName = stat.processName,
                                completedProducts = stat.count,
                            )
                        }
                        .sortedByDescending { it.completedProducts }

                    // 2. Нижняя карточка: группируем этапы по процессам, а внутри процесса — суммируем этапы
                    val stepsItems = statsData.totalSteps
                        .groupBy { it.processId to it.processName }
                        .map { (processKey, employeeSteps) ->

                            // Группируем, сортируем по order и мапим в UI-модель
                            val groupedSteps = employeeSteps
                                .groupBy { it.stepDefinitionId }
                                .values // Получаем списки одинаковых этапов
                                .sortedBy { sameStepsList -> sameStepsList.first().order }
                                .map { sameStepsList ->
                                    val firstItem = sameStepsList.first()
                                    StepCountUiItem(
                                        stepDefinitionId = firstItem.stepDefinitionId,
                                        stepName = firstItem.stepName,
                                        count = sameStepsList.sumOf { it.count }
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

                    _uiState.update {
                        it.copy(
                            totalByProcess = totalItems,
                            stepsByProcess = stepsItems
                        )
                    }
                }
                .onFailure { emitError(it) }

            _uiState.update { it.copy(isLoading = false) }
        }
    }

    // ---------- Вычисление дат ----------

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

    // ---------- Ошибки ----------

    private suspend fun emitError(e: Throwable) {
        _events.send(StatisticsEvent.ShowError(e.toErrorMessage()))
    }

    companion object {
        private val API_DATE_FORMAT: DateTimeFormatter =
            DateTimeFormatter.ofPattern("yyyy-MM-dd")
    }
}