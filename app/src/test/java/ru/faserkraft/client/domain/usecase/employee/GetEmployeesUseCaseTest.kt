package ru.faserkraft.client.domain.usecase.employee

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import ru.faserkraft.client.domain.model.Employee
import ru.faserkraft.client.domain.repository.EmployeeRepository

class GetEmployeesUseCaseTest {

    // ── фикстуры ──────────────────────────────────────────────────────────────

    private val repository: EmployeeRepository = mockk()
    private lateinit var useCase: GetEmployeesUseCase

    private val employees = listOf(
        Employee(id = 1, name = "Иван Петров", email = "ivan@faserkraft.ru"),
        Employee(id = 2, name = "Анна Смирнова", email = "anna@faserkraft.ru"),
    )

    @Before
    fun setUp() {
        useCase = GetEmployeesUseCase(repository)
    }

    // ── invoke() ──────────────────────────────────────────────────────────────

    @Test
    fun `invoke - returns list of employees from repository`() = runTest {
        coEvery { repository.getEmployees() } returns employees

        val result = useCase()

        assertEquals(employees, result)
    }

    @Test
    fun `invoke - calls repository exactly once`() = runTest {
        coEvery { repository.getEmployees() } returns employees

        useCase()

        coVerify(exactly = 1) { repository.getEmployees() }
    }

    @Test
    fun `invoke - returns correct employees count`() = runTest {
        coEvery { repository.getEmployees() } returns employees

        val result = useCase()

        assertEquals(2, result.size)
    }

    @Test
    fun `invoke - returns employees with correct fields`() = runTest {
        coEvery { repository.getEmployees() } returns employees

        val result = useCase()

        assertEquals(1, result[0].id)
        assertEquals("Иван Петров", result[0].name)
        assertEquals("ivan@faserkraft.ru", result[0].email)
    }

    @Test
    fun `invoke - returns empty list if repository returns empty`() = runTest {
        coEvery { repository.getEmployees() } returns emptyList()

        val result = useCase()

        assertTrue(result.isEmpty())
    }

    @Test
    fun `invoke - propagates exception from repository`() = runTest {
        coEvery { repository.getEmployees() } throws RuntimeException("Network error")

        val exception = runCatching { useCase() }.exceptionOrNull()

        assertEquals("Network error", exception?.message)
    }

    @Test
    fun `invoke - returns single employee list correctly`() = runTest {
        val single = listOf(Employee(id = 5, name = "Один", email = "one@faserkraft.ru"))
        coEvery { repository.getEmployees() } returns single

        val result = useCase()

        assertEquals(1, result.size)
        assertEquals(5, result.first().id)
    }
}