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

class AddPackagingToOrderUseCaseTest {

    // ── фикстуры ──────────────────────────────────────────────────────────────

    private val repository: OrderRepository = mockk()
    private lateinit var useCase: AddPackagingToOrderUseCase

    @Before
    fun setUp() {
        useCase = AddPackagingToOrderUseCase(repository)
    }

    // ── invoke() ──────────────────────────────────────────────────────────────

    @Test
    fun `invoke - calls repository with correct orderId and packagingIds`() = runTest {
        coJustRun { repository.addPackagingToOrder(10, listOf(1, 2, 3)) }

        useCase(10, listOf(1, 2, 3))

        coVerify(exactly = 1) { repository.addPackagingToOrder(10, listOf(1, 2, 3)) }
    }

    @Test
    fun `invoke - calls repository exactly once`() = runTest {
        coJustRun { repository.addPackagingToOrder(any(), any()) }

        useCase(10, listOf(1, 2))

        coVerify(exactly = 1) { repository.addPackagingToOrder(any(), any()) }
    }

    @Test
    fun `invoke - passes orderId unchanged`() = runTest {
        coJustRun { repository.addPackagingToOrder(any(), any()) }

        useCase(99, listOf(1))

        coVerify { repository.addPackagingToOrder(99, any()) }
    }

    @Test
    fun `invoke - passes packagingIds list unchanged`() = runTest {
        val ids = listOf(5, 10, 15)
        coJustRun { repository.addPackagingToOrder(any(), any()) }

        useCase(1, ids)

        coVerify { repository.addPackagingToOrder(any(), ids) }
    }

    @Test
    fun `invoke - works with empty packagingIds list`() = runTest {
        coJustRun { repository.addPackagingToOrder(any(), any()) }

        useCase(1, emptyList())

        coVerify { repository.addPackagingToOrder(1, emptyList()) }
    }

    @Test
    fun `invoke - works with single packagingId`() = runTest {
        coJustRun { repository.addPackagingToOrder(any(), any()) }

        useCase(1, listOf(42))

        coVerify { repository.addPackagingToOrder(1, listOf(42)) }
    }

    @Test
    fun `invoke - propagates exception from repository`() = runTest {
        coEvery {
            repository.addPackagingToOrder(any(), any())
        } throws RuntimeException("Order not found")

        val exception = runCatching { useCase(1, listOf(1)) }.exceptionOrNull()

        assertEquals("Order not found", exception?.message)
    }
}