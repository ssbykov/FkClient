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

class DeleteOrderUseCaseTest {

    // ── фикстуры ──────────────────────────────────────────────────────────────

    private val repository: OrderRepository = mockk()
    private lateinit var useCase: DeleteOrderUseCase

    @Before
    fun setUp() {
        useCase = DeleteOrderUseCase(repository)
    }

    // ── invoke() ──────────────────────────────────────────────────────────────

    @Test
    fun `invoke - calls repository with correct orderId`() = runTest {
        coJustRun { repository.deleteOrder(10) }

        useCase(10)

        coVerify(exactly = 1) { repository.deleteOrder(10) }
    }

    @Test
    fun `invoke - calls repository exactly once`() = runTest {
        coJustRun { repository.deleteOrder(any()) }

        useCase(10)

        coVerify(exactly = 1) { repository.deleteOrder(any()) }
    }

    @Test
    fun `invoke - passes orderId unchanged`() = runTest {
        coJustRun { repository.deleteOrder(any()) }

        useCase(99)

        coVerify { repository.deleteOrder(99) }
    }

    @Test
    fun `invoke - different orderIds call repository with correct id`() = runTest {
        coJustRun { repository.deleteOrder(any()) }

        useCase(1)
        useCase(2)

        coVerify { repository.deleteOrder(1) }
        coVerify { repository.deleteOrder(2) }
    }

    @Test
    fun `invoke - propagates exception from repository`() = runTest {
        coEvery { repository.deleteOrder(any()) } throws RuntimeException("Order not found")

        val exception = runCatching { useCase(10) }.exceptionOrNull()

        assertEquals("Order not found", exception?.message)
    }
}