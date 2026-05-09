package ru.faserkraft.client.domain.usecase.auth

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import ru.faserkraft.client.domain.model.LoginCredentials
import ru.faserkraft.client.domain.repository.AuthRepository

class LoginUseCaseTest {

    // ── фикстуры ──────────────────────────────────────────────────────────────

    private val authRepository: AuthRepository = mockk()
    private lateinit var useCase: LoginUseCase

    private val credentials = LoginCredentials(
        username = "worker@faserkraft.ru",
        password = "secret123"
    )

    @Before
    fun setUp() {
        useCase = LoginUseCase(authRepository)
    }

    // ── invoke() ──────────────────────────────────────────────────────────────

    @Test
    fun `invoke - returns token from repository`() = runTest {
        coEvery { authRepository.login(credentials) } returns "jwt-token-xyz"

        val result = useCase(credentials)

        assertEquals("jwt-token-xyz", result)
    }

    @Test
    fun `invoke - calls repository exactly once`() = runTest {
        coEvery { authRepository.login(credentials) } returns "token"

        useCase(credentials)

        coVerify(exactly = 1) { authRepository.login(credentials) }
    }

    @Test
    fun `invoke - passes credentials to repository unchanged`() = runTest {
        coEvery { authRepository.login(any()) } returns "token"

        useCase(credentials)

        coVerify { authRepository.login(credentials) }
    }

    @Test
    fun `invoke - propagates exception from repository`() = runTest {
        coEvery { authRepository.login(credentials) } throws RuntimeException("Unauthorized")

        val exception = runCatching { useCase(credentials) }.exceptionOrNull()

        assertEquals("Unauthorized", exception?.message)
    }

    @Test
    fun `invoke - returns empty string token if repository returns empty`() = runTest {
        coEvery { authRepository.login(credentials) } returns ""

        val result = useCase(credentials)

        assertEquals("", result)
    }
}