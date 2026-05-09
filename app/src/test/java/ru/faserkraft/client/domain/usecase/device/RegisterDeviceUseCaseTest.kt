package ru.faserkraft.client.domain.usecase.device

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import ru.faserkraft.client.domain.model.DeviceRequest
import ru.faserkraft.client.domain.model.UserRegistration
import ru.faserkraft.client.domain.repository.DeviceRepository

class RegisterDeviceUseCaseTest {

    // ── фикстуры ──────────────────────────────────────────────────────────────

    private val repository: DeviceRepository = mockk()
    private lateinit var useCase: RegisterDeviceUseCase

    private val request = DeviceRequest(
        deviceId = "device-001",
        model = "Samsung Galaxy A53",
        manufacturer = "Samsung",
        token = "fcm-token-xyz",
        password = "pass1234",
        userId = 5
    )

    private val registration = UserRegistration(
        userEmail = "ivan@faserkraft.ru",
        userName = "Иван Петров",
        userRole = "worker",
        password = "pass1234"
    )

    @Before
    fun setUp() {
        useCase = RegisterDeviceUseCase(repository)
    }

    // ── invoke() ──────────────────────────────────────────────────────────────

    @Test
    fun `invoke - returns UserRegistration from repository`() = runTest {
        coEvery { repository.registerDevice(request) } returns registration

        val result = useCase(request)

        assertEquals(registration, result)
    }

    @Test
    fun `invoke - calls repository exactly once`() = runTest {
        coEvery { repository.registerDevice(any()) } returns registration

        useCase(request)

        coVerify(exactly = 1) { repository.registerDevice(request) }
    }

    @Test
    fun `invoke - passes request to repository unchanged`() = runTest {
        coEvery { repository.registerDevice(any()) } returns registration

        useCase(request)

        coVerify { repository.registerDevice(request) }
    }

    @Test
    fun `invoke - returned userEmail matches repository response`() = runTest {
        coEvery { repository.registerDevice(request) } returns registration

        val result = useCase(request)

        assertEquals("ivan@faserkraft.ru", result.userEmail)
    }

    @Test
    fun `invoke - returned userRole matches repository response`() = runTest {
        coEvery { repository.registerDevice(request) } returns registration

        val result = useCase(request)

        assertEquals("worker", result.userRole)
    }

    @Test
    fun `invoke - propagates exception from repository`() = runTest {
        coEvery { repository.registerDevice(any()) } throws RuntimeException("Device already registered")

        val exception = runCatching { useCase(request) }.exceptionOrNull()

        assertEquals("Device already registered", exception?.message)
    }

    @Test
    fun `invoke - different requests produce different registrations`() = runTest {
        val request2 = request.copy(deviceId = "device-002", userId = 6)
        val registration2 = registration.copy(userEmail = "anna@faserkraft.ru")

        coEvery { repository.registerDevice(request) } returns registration
        coEvery { repository.registerDevice(request2) } returns registration2

        assertEquals("ivan@faserkraft.ru", useCase(request).userEmail)
        assertEquals("anna@faserkraft.ru", useCase(request2).userEmail)
    }
}