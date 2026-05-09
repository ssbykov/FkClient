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

class GetDayPlansUseCaseTest {

    // ── фикстуры ──────────────────────────────────────────────────────────────

    private val repository: DailyPlanRepository = mockk()
    private lateinit var useCase: GetDayPlansUseCase

    private val date = "2024-06-10"

    private val employee = Employee(
        id = 1,
        name = "Иван Петров",
        email = "ivan@faserkraft.ru"
    )

    private val plans = listOf(
        DailyPlan(
            id = 1,
            employeeId = 1,
            date = date,
            employee = employee,
            steps = emptyList()
        ),
        DailyPlan(
            id = 2,
            employeeId = 2,
            date = date,
            employee = employee.copy(id = 2, name = "Анна Смирнова"),
            steps = emptyList()
        )
    )

    @Before
    fun setUp() {
        useCase = GetDayPlansUseCase(repository)
    }

    // ── invoke() ──────────────────────────────────────────────────────────────

    @Test
    fun `invoke - returns list of plans from repository`() = runTest {
        coEvery { repository.getDayPlans(date) } returns plans

        val result = useCase(date)

        assertEquals(plans, result)
    }

    @Test
    fun `invoke - calls repository exactly once`() = runTest {
        coEvery { repository.getDayPlans(any()) } returns plans

        useCase(date)

        coVerify(exactly = 1) { repository.getDayPlans(date) }
    }

    @Test
    fun `invoke - passes date unchanged`() = runTest {
        coEvery { repository.getDayPlans(any()) } returns plans

        useCase("2024-05-01")

        coVerify { repository.getDayPlans("2024-05-01") }
    }

    @Test
    fun `invoke - returns correct plans count`() = runTest {
        coEvery { repository.getDayPlans(any()) } returns plans

        val result = useCase(date)

        assertEquals(2, result.size)
    }

    @Test
    fun `invoke - returns plans with correct fields`() = runTest {
        coEvery { repository.getDayPlans(any()) } returns plans

        val result = useCase(date)

        assertEquals(1, result[0].id)
        assertEquals(1, result[0].employeeId)
        assertEquals(date, result[0].date)
        assertEquals(2, result[1].id)
        assertEquals(2, result[1].employeeId)
    }

    @Test
    fun `invoke - returns plans with correct employee name`() = runTest {
        coEvery { repository.getDayPlans(any()) } returns plans

        val result = useCase(date)

        assertEquals("Иван Петров", result[0].employee.name)
        assertEquals("Анна Смирнова", result[1].employee.name)
    }

    @Test
    fun `invoke - different dates return different plans`() = runTest {
        val otherPlans = listOf(plans.first().copy(id = 10, date = "2024-06-11"))
        coEvery { repository.getDayPlans(date) } returns plans
        coEvery { repository.getDayPlans("2024-06-11") } returns otherPlans

        assertEquals(2, useCase(date).size)
        assertEquals(1, useCase("2024-06-11").size)
    }

    @Test
    fun `invoke - returns empty list if no plans for date`() = runTest {
        coEvery { repository.getDayPlans(any()) } returns emptyList()

        val result = useCase(date)

        assertTrue(result.isEmpty())
    }

    @Test
    fun `invoke - returns single plan correctly`() = runTest {
        val single = listOf(plans.first())
        coEvery { repository.getDayPlans(any()) } returns single

        val result = useCase(date)

        assertEquals(1, result.size)
        assertEquals(1, result.first().id)
    }

    @Test
    fun `invoke - propagates exception from repository`() = runTest {
        coEvery { repository.getDayPlans(any()) } throws RuntimeException("Network error")

        val exception = runCatching { useCase(date) }.exceptionOrNull()

        assertEquals("Network error", exception?.message)
    }
}