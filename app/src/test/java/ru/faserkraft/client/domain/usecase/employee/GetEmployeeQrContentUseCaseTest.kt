package ru.faserkraft.client.domain.usecase.employee

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import ru.faserkraft.client.domain.repository.EmployeeRepository

class GetEmployeeQrContentUseCaseTest {

    // ── фикстуры ──────────────────────────────────────────────────────────────

    private val employeeRepository: EmployeeRepository = mockk()
    private lateinit var useCase: GetEmployeeQrContentUseCase

    @Before
    fun setUp() {
        useCase = GetEmployeeQrContentUseCase(employeeRepository)
    }

    // ── invoke() ──────────────────────────────────────────────────────────────

    @Test
    fun `invoke - returns qr content string from repository`() = runTest {
        coEvery { employeeRepository.getEmployeeQrContent(7) } returns "employee:7"

        val result = useCase(7)

        assertEquals("employee:7", result)
    }

    @Test
    fun `invoke - calls repository exactly once`() = runTest {
        coEvery { employeeRepository.getEmployeeQrContent(any()) } returns "qr"

        useCase(7)

        coVerify(exactly = 1) { employeeRepository.getEmployeeQrContent(7) }
    }

    @Test
    fun `invoke - passes employeeId to repository unchanged`() = runTest {
        coEvery { employeeRepository.getEmployeeQrContent(any()) } returns "qr"

        useCase(42)

        coVerify { employeeRepository.getEmployeeQrContent(42) }
    }

    @Test
    fun `invoke - different employeeIds return different content`() = runTest {
        coEvery { employeeRepository.getEmployeeQrContent(1) } returns "employee:1"
        coEvery { employeeRepository.getEmployeeQrContent(2) } returns "employee:2"

        assertEquals("employee:1", useCase(1))
        assertEquals("employee:2", useCase(2))
    }

    @Test
    fun `invoke - propagates exception from repository`() = runTest {
        coEvery { employeeRepository.getEmployeeQrContent(any()) } throws RuntimeException("Employee not found")

        val exception = runCatching { useCase(99) }.exceptionOrNull()

        assertEquals("Employee not found", exception?.message)
    }

    @Test
    fun `invoke - returns empty string if repository returns empty`() = runTest {
        coEvery { employeeRepository.getEmployeeQrContent(any()) } returns ""

        val result = useCase(1)

        assertEquals("", result)
    }
}