package ru.faserkraft.client.domain.usecase.packaging

import io.mockk.coEvery
import io.mockk.coJustRun
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import ru.faserkraft.client.domain.repository.PackagingRepository

class DeletePackagingUseCaseTest {

    // ── фикстуры ──────────────────────────────────────────────────────────────

    private val repository: PackagingRepository = mockk()
    private lateinit var useCase: DeletePackagingUseCase

    @Before
    fun setUp() {
        useCase = DeletePackagingUseCase(repository)
    }

    // ── invoke() ──────────────────────────────────────────────────────────────

    @Test
    fun `invoke - calls repository with correct serialNumber`() = runTest {
        coJustRun { repository.deletePackaging("SN-2024-001") }

        useCase("SN-2024-001")

        coVerify(exactly = 1) { repository.deletePackaging("SN-2024-001") }
    }

    @Test
    fun `invoke - calls repository exactly once`() = runTest {
        coJustRun { repository.deletePackaging(any()) }

        useCase("SN-2024-001")

        coVerify(exactly = 1) { repository.deletePackaging(any()) }
    }

    @Test
    fun `invoke - passes serialNumber unchanged`() = runTest {
        coJustRun { repository.deletePackaging(any()) }

        useCase("SN-2024-999")

        coVerify { repository.deletePackaging("SN-2024-999") }
    }

    @Test
    fun `invoke - different serialNumbers call repository with correct value`() = runTest {
        coJustRun { repository.deletePackaging(any()) }

        useCase("SN-2024-001")
        useCase("SN-2024-002")

        coVerify { repository.deletePackaging("SN-2024-001") }
        coVerify { repository.deletePackaging("SN-2024-002") }
    }

    @Test
    fun `invoke - propagates exception from repository`() = runTest {
        coEvery {
            repository.deletePackaging(any())
        } throws RuntimeException("Packaging not found")

        val exception = runCatching { useCase("SN-2024-001") }.exceptionOrNull()

        assertEquals("Packaging not found", exception?.message)
    }

    @Test
    fun `invoke - propagates exception when packaging is attached to order`() = runTest {
        coEvery {
            repository.deletePackaging(any())
        } throws RuntimeException("Packaging is attached to order")

        val exception = runCatching { useCase("SN-2024-001") }.exceptionOrNull()

        assertEquals("Packaging is attached to order", exception?.message)
    }
}