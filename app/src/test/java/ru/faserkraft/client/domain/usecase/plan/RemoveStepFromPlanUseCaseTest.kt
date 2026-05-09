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

class RemoveStepFromPlanUseCaseTest {

    // ── фикстуры ──────────────────────────────────────────────────────────────

    private val repository: DailyPlanRepository = mockk()
    private lateinit var useCase: RemoveStepFromPlanUseCase

    private val employee = Employee(
        id = 1,
        name = "Иван Петров",
        email = "ivan@faserkraft.ru"
    )

    private val updatedPlans = listOf(
        DailyPlan(
            id = 1,
            employeeId = 1,
            date = "2024-06-10",
            employee = employee,
            steps = emptyList()
        ),
        DailyPlan(
            id = 2,
            employeeId = 2,
            date = "2024-06-10",
            employee = employee.copy(id = 2, name = "Анна Смирнова"),
            steps = emptyList()
        )
    )

    @Before
    fun setUp() {
        useCase = RemoveStepFromPlanUseCase(repository)
    }

    // ── invoke() ──────────────────────────────────────────────────────────────

    @Test
    fun `invoke - returns updated plans from repository`() = runTest {
        coEvery { repository.removeStep(10) } returns updatedPlans

        val result = useCase(10)

        assertEquals(updatedPlans, result)
    }

    @Test
    fun `invoke - calls repository exactly once`() = runTest {
        coEvery { repository.removeStep(any()) } returns updatedPlans

        useCase(10)

        coVerify(exactly = 1) { repository.removeStep(10) }
    }

    @Test
    fun `invoke - passes dailyPlanStepId unchanged`() = runTest {
        coEvery { repository.removeStep(any()) } returns updatedPlans

        useCase(99)

        coVerify { repository.removeStep(99) }
    }

    @Test
    fun `invoke - returns correct plans count`() = runTest {
        coEvery { repository.removeStep(any()) } returns updatedPlans

        val result = useCase(10)

        assertEquals(2, result.size)
    }

    @Test
    fun `invoke - returned plans have steps removed`() = runTest {
        coEvery { repository.removeStep(any()) } returns updatedPlans

        val result = useCase(10)

        assertTrue(result[0].steps.isEmpty())
        assertTrue(result[1].steps.isEmpty())
    }

    @Test
    fun `invoke - different stepIds call repository with correct id`() = runTest {
        val otherPlans = listOf(updatedPlans.first())
        coEvery { repository.removeStep(10) } returns updatedPlans
        coEvery { repository.removeStep(20) } returns otherPlans

        assertEquals(2, useCase(10).size)
        assertEquals(1, useCase(20).size)
    }

    @Test
    fun `invoke - returns empty list if no plans remain`() = runTest {
        coEvery { repository.removeStep(any()) } returns emptyList()

        val result = useCase(10)

        assertTrue(result.isEmpty())
    }

    @Test
    fun `invoke - propagates exception from repository`() = runTest {
        coEvery {
            repository.removeStep(any())
        } throws RuntimeException("Step not found")

        val exception = runCatching { useCase(10) }.exceptionOrNull()

        assertEquals("Step not found", exception?.message)
    }
}