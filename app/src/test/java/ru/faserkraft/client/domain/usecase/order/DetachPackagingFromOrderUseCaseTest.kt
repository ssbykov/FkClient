package ru.faserkraft.client.domain.usecase.order

import io.mockk.coEvery
import io.mockk.coJustRun
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import ru.faserkraft.client.domain.repository.OrderRepository

class DetachPackagingFromOrderUseCaseTest {

    // ── фикстуры ──────────────────────────────────────────────────────────────

    private val repository: OrderRepository = mockk()
    private lateinit var useCase: DetachPackagingFromOrderUseCase

    @Before
    fun setUp() {
        useCase = DetachPackagingFromOrderUseCase(repository)
    }

    // ── invoke() ──────────────────────────────────────────────────────────────

    @Test
    fun `invoke - calls repository with correct packagingIds`() = runTest {
        coJustRun { repository.detachPackagingFromOrder(listOf(1, 2, 3)) }

        useCase(listOf(1, 2, 3))

        coVerify(exactly = 1) { repository.detachPackagingFromOrder(listOf(1, 2, 3)) }
    }

    @Test
    fun `invoke - calls repository exactly once`() = runTest {
        coJustRun { repository.detachPackagingFromOrder(any()) }

        useCase(listOf(1, 2))

        coVerify(exactly = 1) { repository.detachPackagingFromOrder(any()) }
    }

    @Test
    fun `invoke - passes packagingIds unchanged`() = runTest {
        val ids = listOf(5, 10, 15)
        coJustRun { repository.detachPackagingFromOrder(any()) }

        useCase(ids)

        coVerify { repository.detachPackagingFromOrder(ids) }
    }

    @Test
    fun `invoke - works with single packagingId`() = runTest {
        coJustRun { repository.detachPackagingFromOrder(any()) }

        useCase(listOf(42))

        coVerify { repository.detachPackagingFromOrder(listOf(42)) }
    }

    @Test
    fun `invoke - works with empty packagingIds list`() = runTest {
        coJustRun { repository.detachPackagingFromOrder(any()) }

        useCase(emptyList())

        coVerify { repository.detachPackagingFromOrder(emptyList()) }
    }

    @Test
    fun `invoke - propagates exception from repository`() = runTest {
        coEvery {
            repository.detachPackagingFromOrder(any())
        } throws RuntimeException("Packaging not found")

        val exception = runCatching { useCase(listOf(1)) }.exceptionOrNull()

        assertEquals("Packaging not found", exception?.message)
    }
}