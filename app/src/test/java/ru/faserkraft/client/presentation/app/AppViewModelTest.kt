package ru.faserkraft.client.presentation.app

import app.cash.turbine.test
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import ru.faserkraft.client.auth.AppAuth
import ru.faserkraft.client.domain.model.DeviceRequest
import ru.faserkraft.client.domain.model.UserData
import ru.faserkraft.client.domain.model.UserRegistration
import ru.faserkraft.client.domain.model.UserRole
import ru.faserkraft.client.domain.usecase.device.RegisterDeviceUseCase
import ru.faserkraft.client.util.FakeLogger
import ru.faserkraft.client.util.MainDispatcherRule

@OptIn(ExperimentalCoroutinesApi::class)
class AppViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val appAuth: AppAuth = mockk(relaxed = true)
    private val registerDeviceUseCase: RegisterDeviceUseCase = mockk()
    private val sessionCoordinator: AppSessionCoordinator = mockk(relaxed = true)
    private val logger = FakeLogger()

    private lateinit var viewModel: AppViewModel

    private val request = DeviceRequest(
        invitationToken = "invitation-token-xyz",
        deviceId = "device-123",
        model = "Pixel 8",
        manufacturer = "Google",
    )

    private val registration = UserRegistration(
        accessToken = "mobile-session-token",
        expiresAt = "2026-11-16T10:00:00Z",
        userEmail = "ivan@test.com",
        userName = "Иван",
        userRole = "worker",
    )

    private val mappedUserData = UserData(
        email = "ivan@test.com",
        name = "Иван",
        role = UserRole.WORKER,
    )

    @Before
    fun setUp() {
        every { appAuth.getRegistrationData() } returns null
        every { appAuth.checkRegistration() } returns null

        viewModel = AppViewModel(
            appAuth = appAuth,
            registerDeviceUseCase = registerDeviceUseCase,
            sessionCoordinator = sessionCoordinator,
            logger = logger,
        )
    }

    // ── Init ─────────────────────────────────────────────────────────────────

    @Test
    fun `init - loads userData from appAuth`() {
        val storedUser = UserData(
            email = "stored@test.com",
            name = "Stored User",
            role = UserRole.WORKER,
        )
        every { appAuth.getRegistrationData() } returns storedUser

        val vm = AppViewModel(
            appAuth = appAuth,
            registerDeviceUseCase = registerDeviceUseCase,
            sessionCoordinator = sessionCoordinator,
            logger = logger,
        )

        assertEquals(storedUser, vm.userData.value)
    }

    // ── Success path ─────────────────────────────────────────────────────────

    @Test
    fun `registerDevice - emits RegistrationCompleted on success`() = runTest {
        coEvery { registerDeviceUseCase(request) } returns registration

        viewModel.events.test {
            viewModel.registerDevice(request)
            advanceUntilIdle()

            assertEquals(AppEvent.RegistrationCompleted, awaitItem())
        }
    }

    @Test
    fun `registerDevice - calls registerDeviceUseCase with correct request`() = runTest {
        coEvery { registerDeviceUseCase(request) } returns registration

        viewModel.registerDevice(request)
        advanceUntilIdle()

        coVerify(exactly = 1) { registerDeviceUseCase(request) }
    }

    @Test
    fun `registerDevice - saves session with token expiry and mapped userData`() = runTest {
        coEvery { registerDeviceUseCase(request) } returns registration

        viewModel.registerDevice(request)
        advanceUntilIdle()

        coVerify(exactly = 1) {
            appAuth.saveSession(
                token = "mobile-session-token",
                expiresAt = "2026-11-16T10:00:00Z",
                userData = mappedUserData,
            )
        }
    }

    @Test
    fun `registerDevice - falls back to WORKER role for unknown role`() = runTest {
        coEvery { registerDeviceUseCase(request) } returns
                registration.copy(userRole = "unknown_role")

        viewModel.registerDevice(request)
        advanceUntilIdle()

        coVerify(exactly = 1) {
            appAuth.saveSession(
                token = "mobile-session-token",
                expiresAt = "2026-11-16T10:00:00Z",
                userData = mappedUserData,
            )
        }
    }

    @Test
    fun `registerDevice - updates userData state after success`() = runTest {
        coEvery { registerDeviceUseCase(request) } returns registration
        every { appAuth.getRegistrationData() } returns mappedUserData

        viewModel.registerDevice(request)
        advanceUntilIdle()

        assertEquals(mappedUserData, viewModel.userData.value)
    }

    @Test
    fun `registerDevice - logs info on success`() = runTest {
        coEvery { registerDeviceUseCase(request) } returns registration
        every { appAuth.getRegistrationData() } returns mappedUserData

        viewModel.registerDevice(request)
        advanceUntilIdle()

        assertEquals("Ожидалось, что ошибок не будет", 0, logger.errors.size)

        assertEquals(1, logger.infos.size)
        assertEquals("AppViewModel", logger.infos.first().tag)
        assertEquals("Device registration completed", logger.infos.first().message)
    }

    // ── Failure path ─────────────────────────────────────────────────────────

    @Test
    fun `registerDevice - emits already registered error when device exists`() = runTest {
        every { appAuth.checkRegistration() } returns "already-registered"

        viewModel.errorState.test {
            viewModel.registerDevice(request)
            advanceUntilIdle()

            assertEquals("Устройство уже зарегистрировано", awaitItem())
        }
    }

    @Test
    fun `registerDevice - does not call use case when device already registered`() = runTest {
        every { appAuth.checkRegistration() } returns "already-registered"

        viewModel.registerDevice(request)
        advanceUntilIdle()

        coVerify(exactly = 0) { registerDeviceUseCase(any()) }
    }

    @Test
    fun `registerDevice - emits mapped error when registerDeviceUseCase throws`() = runTest {
        coEvery { registerDeviceUseCase(request) } throws RuntimeException("Network error")

        viewModel.errorState.test {
            viewModel.registerDevice(request)
            advanceUntilIdle()

            assertEquals("Неизвестная ошибка", awaitItem())
        }
    }

    @Test
    fun `registerDevice - emits mapped error when saving session fails`() = runTest {
        coEvery { registerDeviceUseCase(request) } returns registration
        every { appAuth.saveSession(any(), any(), any()) } throws RuntimeException("disk full")

        viewModel.errorState.test {
            viewModel.registerDevice(request)
            advanceUntilIdle()

            assertEquals("Неизвестная ошибка", awaitItem())
        }
    }

    @Test
    fun `registerDevice - does not emit RegistrationCompleted on failure`() = runTest {
        coEvery { registerDeviceUseCase(request) } throws RuntimeException("boom")

        viewModel.events.test {
            viewModel.registerDevice(request)
            advanceUntilIdle()

            expectNoEvents()
        }
    }

    @Test
    fun `registerDevice - does not save session on failure`() = runTest {
        coEvery { registerDeviceUseCase(request) } throws RuntimeException("boom")

        viewModel.registerDevice(request)
        advanceUntilIdle()

        coVerify(exactly = 0) { appAuth.saveSession(any(), any(), any()) }
    }

    @Test
    fun `registerDevice - logs error when registration fails`() = runTest {
        coEvery { registerDeviceUseCase(request) } throws RuntimeException("boom")

        viewModel.registerDevice(request)
        advanceUntilIdle()

        assertEquals(1, logger.errors.size)
        assertEquals("AppViewModel", logger.errors.first().tag)
        assertEquals("registerDevice failed", logger.errors.first().message)
        assertEquals("boom", logger.errors.first().throwable?.message)
    }

    // ── Logout ───────────────────────────────────────────────────────────────

    @Test
    fun `logout - clears appAuth`() = runTest {
        viewModel.logout()
        advanceUntilIdle()

        coVerify(exactly = 1) { appAuth.clear() }
    }

    @Test
    fun `logout - resets userData to null`() = runTest {
        viewModel.logout()
        advanceUntilIdle()

        assertNull(viewModel.userData.value)
    }

    @Test
    fun `logout - notifies sessionCoordinator`() = runTest {
        viewModel.logout()
        advanceUntilIdle()

        coVerify(exactly = 1) { sessionCoordinator.send(AppSessionEvent.Logout) }
    }

    @Test
    fun `logout - emits LogoutCompleted`() = runTest {
        viewModel.events.test {
            viewModel.logout()
            advanceUntilIdle()

            assertEquals(AppEvent.LogoutCompleted, awaitItem())
        }
    }
}