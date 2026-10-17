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

    private val deviceRepository: DeviceRepository = mockk()
    private lateinit var useCase: RegisterDeviceUseCase

    private val request = DeviceRequest(
        invitationToken = "invitation-token-xyz",
        deviceId = "device-001",
        model = "Samsung Galaxy A53",
        manufacturer = "Samsung",
    )

    private val registration = UserRegistration(
        accessToken = "mobile-session-token",
        expiresAt = "2026-11-16T10:00:00Z",
        userEmail = "ivan@faserkraft.ru",
        userName = "Иван Петров",
        userRole = "worker",
    )

    @Before
    fun setUp() {
        useCase = RegisterDeviceUseCase(deviceRepository)
    }

    @Test
    fun `invoke - returns registration from repository`() = runTest {
        coEvery { deviceRepository.registerDevice(request) } returns registration

        val result = useCase(request)

        assertEquals(registration, result)
    }

    @Test
    fun `invoke - calls repository exactly once with unchanged request`() = runTest {
        coEvery { deviceRepository.registerDevice(request) } returns registration

        useCase(request)

        coVerify(exactly = 1) { deviceRepository.registerDevice(request) }
    }

    @Test
    fun `invoke - propagates exception from repository`() = runTest {
        coEvery { deviceRepository.registerDevice(request) } throws RuntimeException("DEVICE_ALREADY_REGISTERED")

        val exception = runCatching { useCase(request) }.exceptionOrNull()

        assertEquals("DEVICE_ALREADY_REGISTERED", exception?.message)
    }
}