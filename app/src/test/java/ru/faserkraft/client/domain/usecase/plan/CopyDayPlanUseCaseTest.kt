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

class CopyDayPlanUseCaseTest {

    // ── фикстуры ──────────────────────────────────────────────────────────────

    private val repository: DailyPlanRepository = mockk()
    private lateinit var useCase: CopyDayPlanUseCase

    private val fromDate = "2024-06-10"

    private val employee = Employee(
        id = 1,
        name = "Иван Петров",
        email = "ivan@faserkraft.ru"
    )

    private val copiedPlans = listOf(
        DailyPlan(
            id = 10,
            employeeId = 1,
            date = "2024-06-11",
            employee = employee,
            steps = emptyList()
        ),
        DailyPlan(
            id = 11,
            employeeId = 2,
            date = "2024-06-11",
            employee = employee.copy(id = 2, name = "Анна Смирнова"),
            steps = emptyList()
        )
    )

    @Before
    fun setUp() {
        useCase = CopyDayPlanUseCase(repository)
    }

    // ── invoke() ──────────────────────────────────────────────────────────────

    @Test
    fun `invoke - returns copied plans from repository`() = runTest {
        coEvery { repository.copyDayPlan(fromDate) } returns copiedPlans

        val result = useCase(fromDate)

        assertEquals(copiedPlans, result)
    }

    @Test
    fun `invoke - calls repository exactly once`() = runTest {
        coEvery { repository.copyDayPlan(any()) } returns copiedPlans

        useCase(fromDate)

        coVerify(exactly = 1) { repository.copyDayPlan(fromDate) }
    }

    @Test
    fun `invoke - passes fromDate unchanged`() = runTest {
        coEvery { repository.copyDayPlan(any()) } returns copiedPlans

        useCase("2024-05-01")

        coVerify { repository.copyDayPlan("2024-05-01") }
    }

    @Test
    fun `invoke - returns correct plans count`() = runTest {
        coEvery { repository.copyDayPlan(any()) } returns copiedPlans

        val result = useCase(fromDate)

        assertEquals(2, result.size)
    }

    @Test
    fun `invoke - returned plans have correct employeeIds`() = runTest {
        coEvery { repository.copyDayPlan(any()) } returns copiedPlans

        val result = useCase(fromDate)

        assertEquals(1, result[0].employeeId)
        assertEquals(2, result[1].employeeId)
    }

    @Test
    fun `invoke - different fromDates call repository with correct value`() = runTest {
        val otherPlans = listOf(copiedPlans.first().copy(id = 20))
        coEvery { repository.copyDayPlan(fromDate) } returns copiedPlans
        coEvery { repository.copyDayPlan("2024-05-01") } returns otherPlans

        assertEquals(2, useCase(fromDate).size)
        assertEquals(1, useCase("2024-05-01").size)
    }

    @Test
    fun `invoke - returns empty list if source date has no plans`() = runTest {
        coEvery { repository.copyDayPlan(any()) } returns emptyList()

        val result = useCase(fromDate)

        assertTrue(result.isEmpty())
    }

    @Test
    fun `invoke - propagates exception from repository`() = runTest {
        coEvery {
            repository.copyDayPlan(any())
        } throws RuntimeException("Source date not found")

        val exception = runCatching { useCase(fromDate) }.exceptionOrNull()

        assertEquals("Source date not found", exception?.message)
    }
}