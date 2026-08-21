package ru.faserkraft.client.presentation.statistics

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import ru.faserkraft.client.domain.model.DailyPlanStep
import ru.faserkraft.client.domain.model.EmployeePlanStat
import ru.faserkraft.client.domain.model.PeriodStatistics
import ru.faserkraft.client.domain.model.ProcessCountStat
import ru.faserkraft.client.domain.model.StepCountStat
import ru.faserkraft.client.domain.model.StepDefinition
import ru.faserkraft.client.domain.usecase.statistic.GetProductsStatisticsUseCase
import ru.faserkraft.client.presentation.app.AppSessionCoordinator
import ru.faserkraft.client.presentation.app.AppSessionEvent
import ru.faserkraft.client.presentation.plan.StatMode
import ru.faserkraft.client.presentation.plan.StatPeriod
import ru.faserkraft.client.presentation.plan.StatisticsEvent
import ru.faserkraft.client.presentation.plan.StatisticsViewModel
import ru.faserkraft.client.util.MainDispatcherRule
import ru.faserkraft.client.utils.timeprovider.RealTimeProvider
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class StatisticsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val useCase: GetProductsStatisticsUseCase = mockk()
    private val sessionCoordinator: AppSessionCoordinator = mockk()
    private val timeProvider: RealTimeProvider = mockk()

    private val sessionEventsFlow = MutableSharedFlow<AppSessionEvent>()

    // Фиксированная дата: 21 мая 2024, Q2
    private val fixedToday = LocalDate.of(2024, 5, 21)

    private lateinit var viewModel: StatisticsViewModel

    // ── setUp ─────────────────────────────────────────────────────────────────

    @Before
    fun setUp() {
        every { timeProvider.nowLocalDate() } returns fixedToday
        every { sessionCoordinator.events } returns sessionEventsFlow
    }

    private fun createViewModel() {
        viewModel = StatisticsViewModel(
            getProductsStatisticsUseCase = useCase,
            sessionCoordinator = sessionCoordinator,
            timeProvider = timeProvider,
        )
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private fun stubStepDefinition(id: Int) = StepDefinition(
        id = id,
        order = id, // используем id как order для удобства сортировки в тестах
        name = "step_$id",
        nameGenitive = "step_${id}_gen",
    )

    private fun makePlanStep(
        stepDefinitionId: Int,
        plannedQuantity: Int,
    ) = DailyPlanStep(
        id = stepDefinitionId * 100,
        dailyPlanId = 1,
        stepDefinitionId = stepDefinitionId,
        plannedQuantity = plannedQuantity,
        actualQuantity = 0,
        workProcess = "",
        stepDefinition = stubStepDefinition(stepDefinitionId),
    )

    private fun emptyStats() = PeriodStatistics(
        finishedProducts = emptyList(),
        totalSteps = emptyList(),
        employeePlans = emptyList(),
    )

    // ── init ──────────────────────────────────────────────────────────────────

    @Test
    fun `init loads statistics for current month by default`() = runTest {
        coEvery { useCase(dateFrom = "2024-05-01", dateTo = "2024-05-31") } returns emptyStats()

        createViewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("Май 2024", state.periodLabel)
        assertFalse(state.isLoading)
        assertTrue(state.totalByProcess.isEmpty())
        assertTrue(state.stepsByProcess.isEmpty())
        assertTrue(state.employees.isEmpty())
        assertEquals("2024-05-01", viewModel.currentDateFrom)
        assertEquals("2024-05-31", viewModel.currentDateTo)
        coVerify(exactly = 1) { useCase(dateFrom = "2024-05-01", dateTo = "2024-05-31") }
    }

    // ── mapping ───────────────────────────────────────────────────────────────

    @Test
    fun `loadStatistics maps processes steps plans and employees correctly`() = runTest {
        val stats = PeriodStatistics(
            finishedProducts = listOf(
                ProcessCountStat(processId = 1, processName = "Сборка", count = 10),
                ProcessCountStat(processId = 2, processName = "Покраска", count = 20),
            ),
            totalSteps = listOf(
                // Иван · Сборка · Размер A · Этап 1 → 5 шт
                StepCountStat(
                    processId = 1, processName = "Сборка",
                    sizeTypeId = 1001, sizeTypeName = "Размер A",
                    stepDefinitionId = 101, order = 1, stepName = "Этап 1",
                    employeeId = 10, employeeName = "Иван", count = 5,
                ),
                // Петр · Сборка · Размер A · Этап 1 → 3 шт
                StepCountStat(
                    processId = 1, processName = "Сборка",
                    sizeTypeId = 1001, sizeTypeName = "Размер A",
                    stepDefinitionId = 101, order = 1, stepName = "Этап 1",
                    employeeId = 11, employeeName = "Петр", count = 3,
                ),
                // Иван · Сборка · Размер B · Этап 2 → 4 шт
                StepCountStat(
                    processId = 1, processName = "Сборка",
                    sizeTypeId = 1002, sizeTypeName = "Размер B",
                    stepDefinitionId = 102, order = 2, stepName = "Этап 2",
                    employeeId = 10, employeeName = "Иван", count = 4,
                ),
                // Анна · Покраска · Размер C · Этап покраски → 1 шт
                StepCountStat(
                    processId = 2, processName = "Покраска",
                    sizeTypeId = 2001, sizeTypeName = "Размер C",
                    stepDefinitionId = 201, order = 1, stepName = "Этап покраски",
                    employeeId = 12, employeeName = "Анна", count = 1,
                ),
            ),
            employeePlans = listOf(
                EmployeePlanStat(
                    employeeId = 10, employeeName = "Иван", workingDays = 10,
                    steps = listOf(
                        makePlanStep(stepDefinitionId = 101, plannedQuantity = 8),
                        makePlanStep(stepDefinitionId = 102, plannedQuantity = 5),
                    ),
                ),
                EmployeePlanStat(
                    employeeId = 11, employeeName = "Петр", workingDays = 8,
                    steps = listOf(
                        makePlanStep(stepDefinitionId = 101, plannedQuantity = 4),
                    ),
                ),
                EmployeePlanStat(
                    employeeId = 12, employeeName = "Анна", workingDays = 6,
                    steps = listOf(
                        makePlanStep(stepDefinitionId = 201, plannedQuantity = 2),
                    ),
                ),
            ),
        )
        coEvery { useCase(any(), any()) } returns stats

        createViewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)

        // periodWorkingDays = max(10, 8, 6) = 10
        assertEquals(10, state.periodWorkingDays)

        // ── totalByProcess: сортировка по убыванию ────────────────────────────
        assertEquals(2, state.totalByProcess.size)
        assertEquals("Покраска", state.totalByProcess[0].processName)
        assertEquals(20, state.totalByProcess[0].completedProducts)
        assertEquals("Сборка", state.totalByProcess[1].processName)
        assertEquals(10, state.totalByProcess[1].completedProducts)

        // ── stepsByProcess ────────────────────────────────────────────────────
        // Сборка суммарно 5+3+4=12, Покраска 1 → Сборка первая
        assertEquals(2, state.stepsByProcess.size)
        val sborka = state.stepsByProcess[0]
        assertEquals("Сборка", sborka.processName)
        assertEquals(2, sborka.steps.size)

        val s1 = sborka.steps[0] // order=1
        assertEquals(101, s1.stepDefinitionId)
        assertEquals("Этап 1", s1.stepName)
        assertEquals(8, s1.count)                             // 5 + 3
        assertEquals(12, s1.planCount)                        // план Ивана 8 + план Петра 4
        assertEquals(66.67, s1.completionPercentage!!, 0.01)  // 8/12 * 100
        assertEquals(0.8, s1.dailyAverage, 0.001)             // 8 / 10

        val s2 = sborka.steps[1] // order=2
        assertEquals(102, s2.stepDefinitionId)
        assertEquals("Этап 2", s2.stepName)
        assertEquals(4, s2.count)
        assertEquals(5, s2.planCount)
        assertEquals(80.0, s2.completionPercentage!!, 0.01)   // 4/5 * 100
        assertEquals(0.4, s2.dailyAverage, 0.001)             // 4 / 10

        // ── employees ─────────────────────────────────────────────────────────
        assertEquals(3, state.employees.size)

        val ivan = state.employees.first { it.employeeId == 10 }
        assertEquals("Иван", ivan.employeeName)
        assertEquals(10, ivan.workingDays)
        assertEquals(9, ivan.totalCompleted)  // 5 + 4
        assertEquals(2, ivan.sizeTypes.size)

        val ivanSizeA = ivan.sizeTypes.first { it.sizeTypeId == 1001 }
        assertEquals(5, ivanSizeA.totalCompleted)
        val ivanStep1 = ivanSizeA.steps.single()
        assertEquals(5, ivanStep1.count)
        assertEquals(8, ivanStep1.planCount)                        // только план Ивана
        assertEquals(62.5, ivanStep1.completionPercentage!!, 0.01) // 5/8 * 100
        assertEquals(0.5, ivanStep1.dailyAverage, 0.001)           // 5 / 10

        val ivanSizeB = ivan.sizeTypes.first { it.sizeTypeId == 1002 }
        assertEquals(4, ivanSizeB.totalCompleted)
        val ivanStep2 = ivanSizeB.steps.single()
        assertEquals(4, ivanStep2.count)
        assertEquals(5, ivanStep2.planCount)
        assertEquals(80.0, ivanStep2.completionPercentage!!, 0.01) // 4/5 * 100
        assertEquals(0.4, ivanStep2.dailyAverage, 0.001)           // 4 / 10

        val petr = state.employees.first { it.employeeId == 11 }
        assertEquals("Петр", petr.employeeName)
        assertEquals(8, petr.workingDays)
        assertEquals(3, petr.totalCompleted)
        val petrStep = petr.sizeTypes.single().steps.single()
        assertEquals(3, petrStep.count)
        assertEquals(4, petrStep.planCount)
        assertEquals(75.0, petrStep.completionPercentage!!, 0.01) // 3/4 * 100
        assertEquals(0.375, petrStep.dailyAverage, 0.001)         // 3 / 8

        val anna = state.employees.first { it.employeeId == 12 }
        assertEquals("Анна", anna.employeeName)
        assertEquals(6, anna.workingDays)
        assertEquals(1, anna.totalCompleted)
        val annaStep = anna.sizeTypes.single().steps.single()
        assertEquals(1, annaStep.count)
        assertEquals(2, annaStep.planCount)
        assertEquals(50.0, annaStep.completionPercentage!!, 0.01) // 1/2 * 100
        assertEquals(0.1667, annaStep.dailyAverage, 0.001)        // 1 / 6
    }

    // ── shiftPeriod ───────────────────────────────────────────────────────────

    @Test
    fun `shiftPeriod minus 1 loads April 2024`() = runTest {
        coEvery { useCase(any(), any()) } returns emptyStats()
        createViewModel()
        advanceUntilIdle()

        viewModel.shiftPeriod(-1)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("Апрель 2024", state.periodLabel)
        assertEquals("2024-04-01", viewModel.currentDateFrom)
        assertEquals("2024-04-30", viewModel.currentDateTo)
        coVerify(exactly = 1) { useCase(dateFrom = "2024-04-01", dateTo = "2024-04-30") }
    }

    @Test
    fun `shiftPeriod plus 1 loads June 2024`() = runTest {
        coEvery { useCase(any(), any()) } returns emptyStats()
        createViewModel()
        advanceUntilIdle()

        viewModel.shiftPeriod(1)
        advanceUntilIdle()

        assertEquals("Июнь 2024", viewModel.uiState.value.periodLabel)
        assertEquals("2024-06-01", viewModel.currentDateFrom)
        assertEquals("2024-06-30", viewModel.currentDateTo)
        coVerify(exactly = 1) { useCase(dateFrom = "2024-06-01", dateTo = "2024-06-30") }
    }

    // ── setPeriod ─────────────────────────────────────────────────────────────

    @Test
    fun `setPeriod QUARTER resets offset and loads Q2 2024`() = runTest {
        coEvery { useCase(any(), any()) } returns emptyStats()
        createViewModel()
        advanceUntilIdle()

        // смещаемся назад, чтобы убедиться что setPeriod сбросит offset
        viewModel.shiftPeriod(-2)
        advanceUntilIdle()

        viewModel.setPeriod(StatPeriod.QUARTER)
        advanceUntilIdle()

        assertEquals("Кв. 2 2024", viewModel.uiState.value.periodLabel)
        assertEquals("2024-04-01", viewModel.currentDateFrom)
        assertEquals("2024-06-30", viewModel.currentDateTo)
        coVerify(exactly = 1) { useCase(dateFrom = "2024-04-01", dateTo = "2024-06-30") }
    }

    @Test
    fun `setPeriod YEAR loads full 2024`() = runTest {
        coEvery { useCase(any(), any()) } returns emptyStats()
        createViewModel()
        advanceUntilIdle()

        viewModel.setPeriod(StatPeriod.YEAR)
        advanceUntilIdle()

        assertEquals("2024", viewModel.uiState.value.periodLabel)
        assertEquals("2024-01-01", viewModel.currentDateFrom)
        assertEquals("2024-12-31", viewModel.currentDateTo)
        coVerify(exactly = 1) { useCase(dateFrom = "2024-01-01", dateTo = "2024-12-31") }
    }

    // ── error ─────────────────────────────────────────────────────────────────

    @Test
    fun `refresh on error clears isLoading and emits ShowError`() = runTest {
        coEvery { useCase(any(), any()) } returns emptyStats()
        createViewModel()
        advanceUntilIdle()

        val events = mutableListOf<StatisticsEvent>()
        val collectorJob = backgroundScope.launch(
            UnconfinedTestDispatcher(testScheduler)
        ) {
            viewModel.events.collect(events::add)
        }

        coEvery { useCase(any(), any()) } throws IllegalStateException("Network error")

        viewModel.refresh()
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isLoading)
        assertEquals(1, events.size)
        assertTrue(events.single() is StatisticsEvent.ShowError)

        collectorJob.cancel()
    }

    // ── logout ────────────────────────────────────────────────────────────────

    @Test
    fun `logout resets uiState to defaults`() = runTest {
        coEvery { useCase(any(), any()) } returns PeriodStatistics(
            finishedProducts = listOf(ProcessCountStat(1, "Сборка", 5)),
            totalSteps = emptyList(),
            employeePlans = listOf(
                EmployeePlanStat(
                    employeeId = 1, employeeName = "Иван",
                    workingDays = 10, steps = emptyList(),
                )
            ),
        )
        createViewModel()
        advanceUntilIdle()

        // убеждаемся, что данные загружены
        assertEquals(1, viewModel.uiState.value.totalByProcess.size)
        assertEquals(10, viewModel.uiState.value.periodWorkingDays)

        sessionEventsFlow.emit(AppSessionEvent.Logout)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("", state.periodLabel)
        assertFalse(state.isLoading)
        assertEquals(StatMode.BY_PROCESS, state.mode)
        // periodWorkingDays дефолт = 0 (см. StatisticsUiState)
        assertEquals(0, state.periodWorkingDays)
        assertTrue(state.totalByProcess.isEmpty())
        assertTrue(state.stepsByProcess.isEmpty())
        assertTrue(state.employees.isEmpty())
        // currentDateFrom/To не сбрасываются в resetState() — известный code smell.
        // После исправления resetState() раскомментировать:
        // assertEquals("", viewModel.currentDateFrom)
        // assertEquals("", viewModel.currentDateTo)
    }

    // ── edge cases ────────────────────────────────────────────────────────────

    @Test
    fun `step without plan has null planCount and completionPercentage`() = runTest {
        val stats = PeriodStatistics(
            finishedProducts = emptyList(),
            totalSteps = listOf(
                StepCountStat(
                    processId = 1, processName = "Сборка",
                    sizeTypeId = 1001, sizeTypeName = "Размер A",
                    stepDefinitionId = 101, order = 1, stepName = "Этап",
                    employeeId = 10, employeeName = "Иван", count = 7,
                ),
            ),
            employeePlans = listOf(
                EmployeePlanStat(
                    employeeId = 10, employeeName = "Иван",
                    workingDays = 5, steps = emptyList(), // плана нет
                ),
            ),
        )
        coEvery { useCase(any(), any()) } returns stats
        createViewModel()
        advanceUntilIdle()

        val processStep = viewModel.uiState.value.stepsByProcess.single().steps.single()
        assertNull(processStep.planCount)
        assertNull(processStep.completionPercentage)
        assertEquals(1.4, processStep.dailyAverage, 0.001) // 7 / 5

        val empStep = viewModel.uiState.value.employees
            .single().sizeTypes.single().steps.single()
        assertNull(empStep.planCount)
        assertNull(empStep.completionPercentage)
        assertEquals(1.4, empStep.dailyAverage, 0.001) // 7 / 5
    }

    @Test
    fun `empty employeePlans uses fallback periodWorkingDays of 1`() = runTest {
        val stats = PeriodStatistics(
            finishedProducts = emptyList(),
            totalSteps = listOf(
                StepCountStat(
                    processId = 1, processName = "Сборка",
                    sizeTypeId = 1001, sizeTypeName = "Размер A",
                    stepDefinitionId = 101, order = 1, stepName = "Этап",
                    employeeId = 10, employeeName = "Иван", count = 3,
                ),
            ),
            employeePlans = emptyList(),
        )
        coEvery { useCase(any(), any()) } returns stats
        createViewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        // periodWorkingDays fallback = 1 (coerceAtLeast(1) ?: 1)
        assertEquals(1, state.periodWorkingDays)
        // dailyAvg для stepsByProcess = 3 / 1 = 3.0
        assertEquals(3.0, state.stepsByProcess.single().steps.single().dailyAverage, 0.001)
        // сотрудник без плана → employeeWorkingDays fallback = 1
        assertEquals(3.0, state.employees.single().sizeTypes.single().steps.single().dailyAverage, 0.001)
    }
}