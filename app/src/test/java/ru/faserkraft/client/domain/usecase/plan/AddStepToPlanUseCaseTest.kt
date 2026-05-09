package ru.faserkraft.client.domain.usecase.plan

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import ru.faserkraft.client.domain.model.DailyPlan
import ru.faserkraft.client.domain.model.Employee
import ru.faserkraft.client.domain.repository.DailyPlanRepository

class AddStepToPlanUseCaseTest {

    // ── фикстуры ──────────────────────────────────────────────────────────────

    private val repository: DailyPlanRepository = mockk()
    private lateinit var useCase: AddStepToPlanUseCase

    private val planDate = "2024-06-10"
    private val employeeId = 1
    private val stepId = 5
    private val plannedQuantity = 50

    private val employee = Employee(
        id = employeeId,
        name = "Иван Петров",
        email = "ivan@faserkraft.ru"
    )

    private val plans = listOf(
        DailyPlan(
            id = 1,
            employeeId = employeeId,
            date = planDate,
            employee = employee,
            steps = emptyList()
        ),
        DailyPlan(
            id = 2,
            employeeId = 2,
            date = planDate,
            employee = employee.copy(id = 2, name = "Анна Смирнова"),
            steps = emptyList()
        )
    )

    @Before
    fun setUp() {
        useCase = AddStepToPlanUseCase(repository)
    }

    // ── invoke() ──────────────────────────────────────────────────────────────

    @Test
    fun `invoke - returns updated plans from repository`() = runTest {
        coEvery {
            repository.addStep(planDate, employeeId, stepId, plannedQuantity)
        } returns plans

        val result = useCase(planDate, employeeId, stepId, plannedQuantity)

        assertEquals(plans, result)
    }

    @Test
    fun `invoke - calls repository exactly once`() = runTest {
        coEvery { repository.addStep(any(), any(), any(), any()) } returns plans

        useCase(planDate, employeeId, stepId, plannedQuantity)

        coVerify(exactly = 1) {
            repository.addStep(planDate, employeeId, stepId, plannedQuantity)
        }
    }

    @Test
    fun `invoke - passes planDate unchanged`() = runTest {
        coEvery { repository.addStep(any(), any(), any(), any()) } returns plans

        useCase(planDate, employeeId, stepId, plannedQuantity)

        coVerify { repository.addStep(planDate, any(), any(), any()) }
    }

    @Test
    fun `invoke - passes employeeId unchanged`() = runTest {
        coEvery { repository.addStep(any(), any(), any(), any()) } returns plans

        useCase(planDate, employeeId, stepId, plannedQuantity)

        coVerify { repository.addStep(any(), employeeId, any(), any()) }
    }

    @Test
    fun `invoke - passes stepId unchanged`() = runTest {
        coEvery { repository.addStep(any(), any(), any(), any()) } returns plans

        useCase(planDate, employeeId, stepId, plannedQuantity)

        coVerify { repository.addStep(any(), any(), stepId, any()) }
    }

    @Test
    fun `invoke - passes plannedQuantity unchanged`() = runTest {
        coEvery { repository.addStep(any(), any(), any(), any()) } returns plans

        useCase(planDate, employeeId, stepId, plannedQuantity)

        coVerify { repository.addStep(any(), any(), any(), plannedQuantity) }
    }

    @Test
    fun `invoke - returns correct plans count`() = runTest {
        coEvery { repository.addStep(any(), any(), any(), any()) } returns plans

        val result = useCase(planDate, employeeId, stepId, plannedQuantity)

        assertEquals(2, result.size)
    }

    @Test
    fun `invoke - returns empty list if no plans exist`() = runTest {
        coEvery { repository.addStep(any(), any(), any(), any()) } returns emptyList()

        val result = useCase(planDate, employeeId, stepId, plannedQuantity)

        assertTrue(result.isEmpty())
    }

    @Test
    fun `invoke - propagates exception from repository`() = runTest {
        coEvery {
            repository.addStep(any(), any(), any(), any())
        } throws RuntimeException("Step not found")

        val exception = runCatching {
            useCase(planDate, employeeId, stepId, plannedQuantity)
        }.exceptionOrNull()

        assertEquals("Step not found", exception?.message)
    }
}