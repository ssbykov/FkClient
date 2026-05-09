package ru.faserkraft.client.domain.usecase.device

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import ru.faserkraft.client.domain.repository.DeviceRepository

class GetQrCodeUseCaseTest {

    // ── фикстуры ──────────────────────────────────────────────────────────────

    private val repository: DeviceRepository = mockk()
    private lateinit var useCase: GetQrCodeUseCase

    @Before
    fun setUp() {
        useCase = GetQrCodeUseCase(repository)
    }

    // ── invoke() ──────────────────────────────────────────────────────────────

    @Test
    fun `invoke - returns qr code string from repository`() = runTest {
        coEvery { repository.getQrCode(42) } returns "https://qr.faserkraft.ru/employee/42"

        val result = useCase(42)

        assertEquals("https://qr.faserkraft.ru/employee/42", result)
    }

    @Test
    fun `invoke - calls repository exactly once`() = runTest {
        coEvery { repository.getQrCode(any()) } returns "qr-data"

        useCase(42)

        coVerify(exactly = 1) { repository.getQrCode(42) }
    }

    @Test
    fun `invoke - passes employeeId to repository unchanged`() = runTest {
        coEvery { repository.getQrCode(any()) } returns "qr-data"

        useCase(99)

        coVerify { repository.getQrCode(99) }
    }

    @Test
    fun `invoke - different employeeIds return different results`() = runTest {
        coEvery { repository.getQrCode(1) } returns "qr-for-1"
        coEvery { repository.getQrCode(2) } returns "qr-for-2"

        assertEquals("qr-for-1", useCase(1))
        assertEquals("qr-for-2", useCase(2))
    }

    @Test
    fun `invoke - propagates exception from repository`() = runTest {
        coEvery { repository.getQrCode(any()) } throws RuntimeException("Employee not found")

        val exception = runCatching { useCase(42) }.exceptionOrNull()

        assertEquals("Employee not found", exception?.message)
    }

    @Test
    fun `invoke - returns empty string if repository returns empty`() = runTest {
        coEvery { repository.getQrCode(any()) } returns ""

        val result = useCase(1)

        assertEquals("", result)
    }
}