package ru.faserkraft.client.presentation.plan

import app.cash.turbine.test
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import ru.faserkraft.client.auth.AppAuth
import ru.faserkraft.client.domain.model.DailyPlan
import ru.faserkraft.client.domain.model.DailyPlanStep
import ru.faserkraft.client.domain.model.Employee
import ru.faserkraft.client.domain.model.Process
import ru.faserkraft.client.domain.model.Product
import ru.faserkraft.client.domain.model.StepDefinition
import ru.faserkraft.client.domain.model.UserData
import ru.faserkraft.client.domain.model.UserRole
import ru.faserkraft.client.domain.usecase.employee.GetEmployeesUseCase
import ru.faserkraft.client.domain.usecase.plan.AddStepToPlanUseCase
import ru.faserkraft.client.domain.usecase.plan.CopyDayPlanUseCase
import ru.faserkraft.client.domain.usecase.plan.GetDayPlansUseCase
import ru.faserkraft.client.domain.usecase.plan.RemoveStepFromPlanUseCase
import ru.faserkraft.client.domain.usecase.plan.UpdateStepInPlanUseCase
import ru.faserkraft.client.domain.usecase.process.GetProcessesUseCase
import ru.faserkraft.client.domain.usecase.product.GetProductsByStepEmployeeDayUseCase
import ru.faserkraft.client.presentation.app.AppSessionCoordinator
import ru.faserkraft.client.presentation.app.AppSessionEvent
import ru.faserkraft.client.util.MainDispatcherRule

@OptIn(ExperimentalCoroutinesApi::class)
class PlanViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    // ── Mocks ────────────────────────────────────────────────────────────────

    private val getDayPlansUseCase: GetDayPlansUseCase = mockk()
    private val addStepToPlanUseCase: AddStepToPlanUseCase = mockk()
    private val updateStepInPlanUseCase: UpdateStepInPlanUseCase = mockk()
    private val removeStepFromPlanUseCase: RemoveStepFromPlanUseCase = mockk()
    private val copyDayPlanUseCase: CopyDayPlanUseCase = mockk()
    private val getEmployeesUseCase: GetEmployeesUseCase = mockk()
    private val getProcessesUseCase: GetProcessesUseCase = mockk()
    private val getProductsByStepEmployeeDayUseCase: GetProductsByStepEmployeeDayUseCase = mockk()
    private val appAuth: AppAuth = mockk(relaxed = true)
    private val sessionCoordinator: AppSessionCoordinator = mockk(relaxed = true)

    private lateinit var viewModel: PlanViewModel
    private val sessionEventsFlow = MutableSharedFlow<AppSessionEvent>()

    // ── Dummies ──────────────────────────────────────────────────────────────

    private val masterUserData = mockk<UserData>(relaxed = true) {
        every { role } returns UserRole.MASTER
    }

    private val dummyEmployee = Employee(id = 1, name = "Иван", email = "ivan@test.com")
    private val dummyEmployeeList = listOf(dummyEmployee)

    private val dummyStepDefinition = StepDefinition(
        id = 1,
        order = 1,
        name = "Шаг 1",
        nameGenitive = "Шага 1",
    )

    private val dummyStep = DailyPlanStep(
        id = 1,
        dailyPlanId = 1,
        stepDefinitionId = 1,
        plannedQuantity = 100,
        actualQuantity = 50,
        workProcess = "Сборка",
        stepDefinition = dummyStepDefinition,
    )

    private val dummyPlan = DailyPlan(
        id = 1,
        employeeId = 1,
        date = "2026-05-10",
        employee = dummyEmployee,
        steps = listOf(dummyStep),
    )
    private val dummyPlanList = listOf(dummyPlan)

    private val dummyProcess = Process(
        id = 1,
        name = "Процесс 1",
        description = "Описание",
        steps = listOf(dummyStepDefinition),
    )
    private val dummyProcessList = listOf(dummyProcess)

    // Product — замени на реальный конструктор когда покажешь структуру класса
    private val dummyProduct = mockk<Product>(relaxed = true)
    private val dummyProductList = listOf(dummyProduct)

    private val testDate = "2026-05-10"

    @Before
    fun setUp() {
        every { appAuth.getRegistrationData() } returns masterUserData
        every { sessionCoordinator.events } returns sessionEventsFlow

        viewModel = PlanViewModel(
            getDayPlansUseCase = getDayPlansUseCase,
            addStepToPlanUseCase = addStepToPlanUseCase,
            updateStepInPlanUseCase = updateStepInPlanUseCase,
            removeStepFromPlanUseCase = removeStepFromPlanUseCase,
            copyDayPlanUseCase = copyDayPlanUseCase,
            getEmployeesUseCase = getEmployeesUseCase,
            getProcessesUseCase = getProcessesUseCase,
            getProductsByStepEmployeeDayUseCase = getProductsByStepEmployeeDayUseCase,
            appAuth = appAuth,
            sessionCoordinator = sessionCoordinator,
        )
    }

    // ── Init & Session ───────────────────────────────────────────────────────

    @Test
    fun `init - loads user role as MASTER and sets canEdit true`() = runTest {
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(UserRole.MASTER, state.userRole)
        assertTrue(state.canEdit)
    }

    @Test
    fun `init - sets canEdit false for non-master role`() = runTest {
        val workerUser = mockk<UserData>(relaxed = true) {
            every { role } returns UserRole.WORKER
        }
        every { appAuth.getRegistrationData() } returns workerUser

        val vm = PlanViewModel(
            getDayPlansUseCase = getDayPlansUseCase,
            addStepToPlanUseCase = addStepToPlanUseCase,
            updateStepInPlanUseCase = updateStepInPlanUseCase,
            removeStepFromPlanUseCase = removeStepFromPlanUseCase,
            copyDayPlanUseCase = copyDayPlanUseCase,
            getEmployeesUseCase = getEmployeesUseCase,
            getProcessesUseCase = getProcessesUseCase,
            getProductsByStepEmployeeDayUseCase = getProductsByStepEmployeeDayUseCase,
            appAuth = appAuth,
            sessionCoordinator = sessionCoordinator,
        )
        advanceUntilIdle()

        assertFalse(vm.uiState.value.canEdit)
    }

    @Test
    fun `observeSessionEvents - resets state on Logout event`() = runTest {
        viewModel.selectPlanStep(dummyPlan, dummyStep)
        advanceUntilIdle()

        sessionEventsFlow.emit(AppSessionEvent.Logout)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertNull(state.selectedPlan)
        assertNull(state.selectedStep)
    }

    // ── loadPlans ────────────────────────────────────────────────────────────

    @Test
    fun `loadPlans - updates state with plans and date on success`() = runTest {
        coEvery { getDayPlansUseCase(testDate) } returns dummyPlanList

        viewModel.loadPlans(testDate)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(dummyPlanList, state.plans)
        assertEquals(testDate, state.date)
        assertFalse(state.isLoading)
    }

    @Test
    fun `loadPlans - resets loading flag even on failure`() = runTest {
        coEvery { getDayPlansUseCase(testDate) } throws RuntimeException("Error")

        viewModel.events.test {
            viewModel.loadPlans(testDate)
            advanceUntilIdle()

            assertFalse(viewModel.uiState.value.isLoading)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `loadPlans - emits ShowError on failure`() = runTest {
        coEvery { getDayPlansUseCase(testDate) } throws RuntimeException("Error")

        viewModel.events.test {
            viewModel.loadPlans(testDate)
            advanceUntilIdle()

            assertEquals(PlanEvent.ShowError("Неизвестная ошибка"), awaitItem())
        }
    }

    // ── addStepToPlan ────────────────────────────────────────────────────────

    @Test
    fun `addStepToPlan - updates plans and resets flag on success`() = runTest {
        coEvery { addStepToPlanUseCase(testDate, 1, 2, 100) } returns dummyPlanList

        viewModel.addStepToPlan(testDate, 1, 2, 100)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(dummyPlanList, state.plans)
        assertFalse(state.isActionInProgress)
    }

    @Test
    fun `addStepToPlan - emits ShowError on failure`() = runTest {
        coEvery { addStepToPlanUseCase(any(), any(), any(), any()) } throws RuntimeException()

        viewModel.events.test {
            viewModel.addStepToPlan(testDate, 1, 2, 100)
            advanceUntilIdle()

            assertEquals(PlanEvent.ShowError("Неизвестная ошибка"), awaitItem())
            assertFalse(viewModel.uiState.value.isActionInProgress)
        }
    }

    // ── updateStepInPlan ─────────────────────────────────────────────────────

    @Test
    fun `updateStepInPlan - updates plans on success`() = runTest {
        coEvery { updateStepInPlanUseCase(1, testDate, 2, 3, 50) } returns dummyPlanList

        viewModel.updateStepInPlan(1, testDate, 2, 3, 50)
        advanceUntilIdle()

        assertEquals(dummyPlanList, viewModel.uiState.value.plans)
        assertFalse(viewModel.uiState.value.isActionInProgress)
    }

    // ── removeStepFromPlan ───────────────────────────────────────────────────

    @Test
    fun `removeStepFromPlan - updates plans on success`() = runTest {
        coEvery { removeStepFromPlanUseCase(1) } returns dummyPlanList

        viewModel.removeStepFromPlan(1)
        advanceUntilIdle()

        assertEquals(dummyPlanList, viewModel.uiState.value.plans)
        assertFalse(viewModel.uiState.value.isActionInProgress)
    }

    // ── copyDayPlan ──────────────────────────────────────────────────────────

    @Test
    fun `copyDayPlan - updates plans and resets flag on success`() = runTest {
        coEvery { copyDayPlanUseCase("2026-05-09") } returns dummyPlanList

        viewModel.copyDayPlan("2026-05-09")
        advanceUntilIdle()

        assertEquals(dummyPlanList, viewModel.uiState.value.plans)
        assertFalse(viewModel.uiState.value.isActionInProgress)
    }

    @Test
    fun `copyDayPlan - emits ShowError on failure`() = runTest {
        coEvery { copyDayPlanUseCase(any()) } throws RuntimeException()

        viewModel.events.test {
            viewModel.copyDayPlan("2026-05-09")
            advanceUntilIdle()

            assertEquals(PlanEvent.ShowError("Неизвестная ошибка"), awaitItem())
        }
    }

    // ── loadEmployees & loadProcesses ────────────────────────────────────────

    @Test
    fun `loadEmployees - updates state with employees on success`() = runTest {
        coEvery { getEmployeesUseCase() } returns dummyEmployeeList

        viewModel.loadEmployees()
        advanceUntilIdle()

        assertEquals(dummyEmployeeList, viewModel.uiState.value.employees)
    }

    @Test
    fun `loadProcesses - updates state with processes on success`() = runTest {
        coEvery { getProcessesUseCase() } returns dummyProcessList

        viewModel.loadProcesses()
        advanceUntilIdle()

        assertEquals(dummyProcessList, viewModel.uiState.value.processes)
    }

    // ── loadProductsByStepEmployeeDay ────────────────────────────────────────

    @Test
    fun `loadProductsByStepEmployeeDay - updates filteredProducts on success`() = runTest {
        coEvery { getProductsByStepEmployeeDayUseCase(1, testDate, 2) } returns dummyProductList

        viewModel.loadProductsByStepEmployeeDay(1, testDate, 2)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(dummyProductList, state.filteredProducts)
        assertFalse(state.isLoading)
    }

    @Test
    fun `loadProductsByStepEmployeeDay - resets filteredProducts to empty before loading`() = runTest {
        coEvery { getProductsByStepEmployeeDayUseCase(1, testDate, 2) } returns dummyProductList

        // Проверяем, что filteredProducts очищается перед запросом
        viewModel.uiState.test {
            viewModel.loadProductsByStepEmployeeDay(1, testDate, 2)
            advanceUntilIdle()

            // Пропускаем промежуточные стейты, проверяем финальный
            val final = cancelAndConsumeRemainingEvents()
                .filterIsInstance<app.cash.turbine.Event.Item<PlanUiState>>()
                .last().value

            assertTrue(final.filteredProducts.isNotEmpty())
        }
    }

    // ── shiftDate ────────────────────────────────────────────────────────────

    @Test
    fun `shiftDate - shifts by positive days and calls loadPlans`() = runTest {
        // Устанавливаем фиксированную дату в стейт через loadPlans
        coEvery { getDayPlansUseCase(testDate) } returns dummyPlanList
        viewModel.loadPlans(testDate)
        advanceUntilIdle()

        val nextDate = "2026-05-11"
        coEvery { getDayPlansUseCase(nextDate) } returns dummyPlanList

        viewModel.shiftDate(1)
        advanceUntilIdle()

        coVerify(exactly = 1) { getDayPlansUseCase(nextDate) }
        assertEquals(nextDate, viewModel.uiState.value.date)
    }

    @Test
    fun `shiftDate - shifts by negative days correctly`() = runTest {
        coEvery { getDayPlansUseCase(testDate) } returns dummyPlanList
        viewModel.loadPlans(testDate)
        advanceUntilIdle()

        val prevDate = "2026-05-09"
        coEvery { getDayPlansUseCase(prevDate) } returns dummyPlanList

        viewModel.shiftDate(-1)
        advanceUntilIdle()

        coVerify(exactly = 1) { getDayPlansUseCase(prevDate) }
        assertEquals(prevDate, viewModel.uiState.value.date)
    }

    // ── selectPlanStep & clearSelectedPlanStep ───────────────────────────────

    @Test
    fun `selectPlanStep - updates selectedPlan and selectedStep in state`() {
        viewModel.selectPlanStep(dummyPlan, dummyStep)

        val state = viewModel.uiState.value
        assertEquals(dummyPlan, state.selectedPlan)
        assertEquals(dummyStep, state.selectedStep)
    }

    @Test
    fun `clearSelectedPlanStep - nullifies selectedPlan and selectedStep`() {
        viewModel.selectPlanStep(dummyPlan, dummyStep)
        viewModel.clearSelectedPlanStep()

        val state = viewModel.uiState.value
        assertNull(state.selectedPlan)
        assertNull(state.selectedStep)
    }
}