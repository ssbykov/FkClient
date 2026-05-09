package ru.faserkraft.client.domain.usecase.order

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import ru.faserkraft.client.domain.model.Order
import ru.faserkraft.client.domain.repository.OrderRepository

class CloseOrderUseCaseTest {

    // ── фикстуры ──────────────────────────────────────────────────────────────

    private val repository: OrderRepository = mockk()
    private lateinit var useCase: CloseOrderUseCase

    private val closedOrder = Order(
        id = 10,
        contractNumber = "КД-2024-001",
        contractDate = "2024-01-15",
        plannedShipmentDate = "2024-03-01",
        shipmentDate = "2024-03-05",
        shipmentBy = null,
        items = emptyList(),
        packaging = emptyList()
    )

    @Before
    fun setUp() {
        useCase = CloseOrderUseCase(repository)
    }

    // ── invoke() ──────────────────────────────────────────────────────────────

    @Test
    fun `invoke - returns closed order from repository`() = runTest {
        coEvery { repository.closeOrder(10) } returns closedOrder

        val result = useCase(10)

        assertEquals(closedOrder, result)
    }

    @Test
    fun `invoke - calls repository exactly once`() = runTest {
        coEvery { repository.closeOrder(any()) } returns closedOrder

        useCase(10)

        coVerify(exactly = 1) { repository.closeOrder(10) }
    }

    @Test
    fun `invoke - passes orderId to repository unchanged`() = runTest {
        coEvery { repository.closeOrder(any()) } returns closedOrder

        useCase(99)

        coVerify { repository.closeOrder(99) }
    }

    @Test
    fun `invoke - returned order id matches`() = runTest {
        coEvery { repository.closeOrder(10) } returns closedOrder

        val result = useCase(10)

        assertEquals(10, result.id)
    }

    @Test
    fun `invoke - returned order has shipmentDate set`() = runTest {
        coEvery { repository.closeOrder(10) } returns closedOrder

        val result = useCase(10)

        assertEquals("2024-03-05", result.shipmentDate)
    }

    @Test
    fun `invoke - propagates exception from repository`() = runTest {
        coEvery { repository.closeOrder(any()) } throws RuntimeException("Order already closed")

        val exception = runCatching { useCase(10) }.exceptionOrNull()

        assertEquals("Order already closed", exception?.message)
    }

    @Test
    fun `invoke - different orderIds call repository with correct id`() = runTest {
        val order2 = closedOrder.copy(id = 20, contractNumber = "КД-2024-002")
        coEvery { repository.closeOrder(10) } returns closedOrder
        coEvery { repository.closeOrder(20) } returns order2

        assertEquals(10, useCase(10).id)
        assertEquals(20, useCase(20).id)
    }
}