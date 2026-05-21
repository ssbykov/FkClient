package ru.faserkraft.client.presentation.statistics


import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import ru.faserkraft.client.domain.model.PeriodStatistics
import ru.faserkraft.client.domain.model.ProcessCountStat
import ru.faserkraft.client.domain.model.StepCountStat
import ru.faserkraft.client.domain.usecase.statistic.GetProductsStatisticsUseCase
import ru.faserkraft.client.presentation.app.AppSessionCoordinator
import ru.faserkraft.client.presentation.app.AppSessionEvent
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
    private val fixedToday = LocalDate.of(2024, 5, 21) // Фиксируем дату для предсказуемости

    private lateinit var viewModel: StatisticsViewModel

    @Before
    fun setUp() {
        // Настраиваем TimeProvider на возврат фиксированной даты
        every { timeProvider.nowLocalDate() } returns fixedToday
        every { sessionCoordinator.events } returns sessionEventsFlow
    }

    private fun createViewModel() {
        viewModel = StatisticsViewModel(
            getProductsStatisticsUseCase = useCase,
            sessionCoordinator = sessionCoordinator,
            timeProvider = timeProvider
        )
    }

    @Test
    fun `init loads statistics for current month by default`() = runTest {
        val emptyStats = PeriodStatistics(emptyList(), emptyList())
        coEvery { useCase("2024-05-01", "2024-05-31") } returns emptyStats

        createViewModel()
        advanceUntilIdle() // Ждем, пока отработают корутины в init

        val state = viewModel.uiState.value
        assertEquals("Май 2024", state.periodLabel)
        assertFalse(state.isLoading)
        coVerify(exactly = 1) { useCase("2024-05-01", "2024-05-31") }
    }

    @Test
    fun `loadStatistics maps and groups data correctly`() = runTest {
        val mockStats = PeriodStatistics(
            finishedProducts = listOf(
                ProcessCountStat(1, "Сборка", 10),
                ProcessCountStat(2, "Покраска", 20)
            ),
            totalSteps = listOf(
                StepCountStat(1, "Сборка", 101, 1, "Этап 1", 10, "Иван", 5),
                StepCountStat(1, "Сборка", 101, 1, "Этап 1", 11, "Петр", 3),
                StepCountStat(1, "Сборка", 102, 2, "Этап 2", 10, "Иван", 4),
                StepCountStat(2, "Покраска", 201, 1, "Этап Покраски", 12, "Анна", 1)
            )
        )
        coEvery { useCase(any(), any()) } returns mockStats

        createViewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value

        // Проверяем верхнюю карточку
        assertEquals(2, state.totalByProcess.size)
        assertEquals("Покраска", state.totalByProcess[0].processName) // 20 > 10, поэтому первая
        assertEquals(20, state.totalByProcess[0].completedProducts)
        assertEquals("Сборка", state.totalByProcess[1].processName)

        // Проверяем нижнюю карточку
        assertEquals(2, state.stepsByProcess.size)

        val sborkaSteps = state.stepsByProcess[0] // 12 шагов суммарно
        assertEquals("Сборка", sborkaSteps.processName)
        assertEquals(2, sborkaSteps.steps.size)

        // Проверяем суммирование и порядок этапов (по order)
        assertEquals("Этап 1", sborkaSteps.steps[0].stepName)
        assertEquals(8, sborkaSteps.steps[0].count) // 5 + 3

        assertEquals("Этап 2", sborkaSteps.steps[1].stepName)
        assertEquals(4, sborkaSteps.steps[1].count)
    }

    @Test
    fun `shiftPeriod updates offset and loads correct dates`() = runTest {
        coEvery { useCase(any(), any()) } returns PeriodStatistics(emptyList(), emptyList())
        createViewModel()
        advanceUntilIdle()

        viewModel.shiftPeriod(-1)
        advanceUntilIdle()

        assertEquals("Апрель 2024", viewModel.uiState.value.periodLabel)
        coVerify(exactly = 1) { useCase("2024-04-01", "2024-04-30") }
    }

    @Test
    fun `setPeriod to QUARTER calculates dates correctly`() = runTest {
        coEvery { useCase(any(), any()) } returns PeriodStatistics(emptyList(), emptyList())
        createViewModel()
        advanceUntilIdle()

        viewModel.setPeriod(StatPeriod.QUARTER)
        advanceUntilIdle()

        assertEquals("Кв. 2 2024", viewModel.uiState.value.periodLabel)
        coVerify(exactly = 1) { useCase("2024-04-01", "2024-06-30") }
    }

    @Test
    fun `loadStatistics handles error and emits event`() = runTest {
        // Arrange
        val exception = Exception("Network error")
        coEvery { useCase(any(), any()) } throws exception

        val emittedEvents = mutableListOf<StatisticsEvent>()

        // Act
        createViewModel()

        // Запускаем сбор событий привязывая launch к скоупу runTest (this)
        // Используем UnconfinedTestDispatcher(testScheduler) чтобы сборщик начал работать сразу
        val job = launch(kotlinx.coroutines.test.UnconfinedTestDispatcher(testScheduler)) {
            viewModel.events.collect { event ->
                emittedEvents.add(event)
            }
        }

        // Выполняем все корутины внутри ViewModel до конца
        advanceUntilIdle()

        // Assert
        val state = viewModel.uiState.value

        assertFalse("isLoading should be false after error", state.isLoading)

        assertEquals("Should emit exactly 1 error event", 1, emittedEvents.size)
        assertTrue("Event should be ShowError", emittedEvents.first() is StatisticsEvent.ShowError)

        // Отменяем фоновую подписку (важно, иначе тест зависнет ожидая новые события в collect)
        job.cancel()
    }

    @Test
    fun `session Logout event resets state`() = runTest {
        coEvery { useCase(any(), any()) } returns PeriodStatistics(emptyList(), emptyList())
        createViewModel()
        advanceUntilIdle()

        viewModel.shiftPeriod(5)
        advanceUntilIdle()

        // Имитируем выход пользователя
        sessionEventsFlow.emit(AppSessionEvent.Logout)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("", state.periodLabel)
        assertEquals(emptyList<Any>(), state.totalByProcess)
    }
}