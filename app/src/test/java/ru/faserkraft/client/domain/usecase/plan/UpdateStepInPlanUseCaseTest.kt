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

class UpdateStepInPlanUseCaseTest {

    // ── фикстуры ──────────────────────────────────────────────────────────────

    private val repository: DailyPlanRepository = mockk()
    private lateinit var useCase: UpdateStepInPlanUseCase

    private val stepId = 10
    private val planDate = "2024-06-10"
    private val stepDefinitionId = 3
    private val employeeId = 1
    private val plannedQuantity = 50

    private val employee = Employee(
        id = employeeId,
        name = "Иван Петров",
        email = "ivan@faserkraft.ru"
    )

    private val updatedPlans = listOf(
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
        useCase = UpdateStepInPlanUseCase(repository)
    }

    // ── invoke() ──────────────────────────────────────────────────────────────

    @Test
    fun `invoke - returns updated plans from repository`() = runTest {
        coEvery {
            repository.updateStep(stepId, planDate, stepDefinitionId, employeeId, plannedQuantity)
        } returns updatedPlans

        val result = useCase(stepId, planDate, stepDefinitionId, employeeId, plannedQuantity)

        assertEquals(updatedPlans, result)
    }

    @Test
    fun `invoke - calls repository exactly once`() = runTest {
        coEvery { repository.updateStep(any(), any(), any(), any(), any()) } returns updatedPlans

        useCase(stepId, planDate, stepDefinitionId, employeeId, plannedQuantity)

        coVerify(exactly = 1) {
            repository.updateStep(stepId, planDate, stepDefinitionId, employeeId, plannedQuantity)
        }
    }

    @Test
    fun `invoke - passes stepId unchanged`() = runTest {
        coEvery { repository.updateStep(any(), any(), any(), any(), any()) } returns updatedPlans

        useCase(stepId, planDate, stepDefinitionId, employeeId, plannedQuantity)

        coVerify { repository.updateStep(stepId, any(), any(), any(), any()) }
    }

    @Test
    fun `invoke - passes planDate unchanged`() = runTest {
        coEvery { repository.updateStep(any(), any(), any(), any(), any()) } returns updatedPlans

        useCase(stepId, planDate, stepDefinitionId, employeeId, plannedQuantity)

        coVerify { repository.updateStep(any(), planDate, any(), any(), any()) }
    }

    @Test
    fun `invoke - passes stepDefinitionId unchanged`() = runTest {
        coEvery { repository.updateStep(any(), any(), any(), any(), any()) } returns updatedPlans

        useCase(stepId, planDate, stepDefinitionId, employeeId, plannedQuantity)

        coVerify { repository.updateStep(any(), any(), stepDefinitionId, any(), any()) }
    }

    @Test
    fun `invoke - passes employeeId unchanged`() = runTest {
        coEvery { repository.updateStep(any(), any(), any(), any(), any()) } returns updatedPlans

        useCase(stepId, planDate, stepDefinitionId, employeeId, plannedQuantity)

        coVerify { repository.updateStep(any(), any(), any(), employeeId, any()) }
    }

    @Test
    fun `invoke - passes plannedQuantity unchanged`() = runTest {
        coEvery { repository.updateStep(any(), any(), any(), any(), any()) } returns updatedPlans

        useCase(stepId, planDate, stepDefinitionId, employeeId, plannedQuantity)

        coVerify { repository.updateStep(any(), any(), any(), any(), plannedQuantity) }
    }

    @Test
    fun `invoke - returns correct plans count`() = runTest {
        coEvery { repository.updateStep(any(), any(), any(), any(), any()) } returns updatedPlans

        val result = useCase(stepId, planDate, stepDefinitionId, employeeId, plannedQuantity)

        assertEquals(2, result.size)
    }

    @Test
    fun `invoke - returns empty list if no plans exist`() = runTest {
        coEvery { repository.updateStep(any(), any(), any(), any(), any()) } returns emptyList()

        val result = useCase(stepId, planDate, stepDefinitionId, employeeId, plannedQuantity)

        assertTrue(result.isEmpty())
    }

    @Test
    fun `invoke - propagates exception from repository`() = runTest {
        coEvery {
            repository.updateStep(any(), any(), any(), any(), any())
        } throws RuntimeException("Step not found")

        val exception = runCatching {
            useCase(stepId, planDate, stepDefinitionId, employeeId, plannedQuantity)
        }.exceptionOrNull()

        assertEquals("Step not found", exception?.message)
    }
}