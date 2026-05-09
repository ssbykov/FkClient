package ru.faserkraft.client.domain.usecase.order

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import ru.faserkraft.client.domain.model.Order
import ru.faserkraft.client.domain.repository.OrderRepository

class GetOrderUseCaseTest {

    // ── фикстуры ──────────────────────────────────────────────────────────────

    private val repository: OrderRepository = mockk()
    private lateinit var useCase: GetOrderUseCase

    private val order = Order(
        id = 10,
        contractNumber = "КД-2024-001",
        contractDate = "2024-01-15",
        plannedShipmentDate = "2024-03-01",
        shipmentDate = null,
        shipmentBy = null,
        items = emptyList(),
        packaging = emptyList()
    )

    @Before
    fun setUp() {
        useCase = GetOrderUseCase(repository)
    }

    // ── invoke() ──────────────────────────────────────────────────────────────

    @Test
    fun `invoke - returns order from repository`() = runTest {
        coEvery { repository.getOrder(10) } returns order

        val result = useCase(10)

        assertEquals(order, result)
    }

    @Test
    fun `invoke - calls repository exactly once`() = runTest {
        coEvery { repository.getOrder(any()) } returns order

        useCase(10)

        coVerify(exactly = 1) { repository.getOrder(10) }
    }

    @Test
    fun `invoke - passes orderId to repository unchanged`() = runTest {
        coEvery { repository.getOrder(any()) } returns order

        useCase(99)

        coVerify { repository.getOrder(99) }
    }

    @Test
    fun `invoke - returned order id matches requested id`() = runTest {
        coEvery { repository.getOrder(10) } returns order

        val result = useCase(10)

        assertEquals(10, result.id)
    }

    @Test
    fun `invoke - returned order has correct contractNumber`() = runTest {
        coEvery { repository.getOrder(10) } returns order

        val result = useCase(10)

        assertEquals("КД-2024-001", result.contractNumber)
    }

    @Test
    fun `invoke - returned order with null shipmentDate`() = runTest {
        coEvery { repository.getOrder(10) } returns order

        val result = useCase(10)

        assertNull(result.shipmentDate)
    }

    @Test
    fun `invoke - different orderIds return different orders`() = runTest {
        val order2 = order.copy(id = 20, contractNumber = "КД-2024-002")
        coEvery { repository.getOrder(10) } returns order
        coEvery { repository.getOrder(20) } returns order2

        assertEquals(10, useCase(10).id)
        assertEquals(20, useCase(20).id)
    }

    @Test
    fun `invoke - propagates exception from repository`() = runTest {
        coEvery { repository.getOrder(any()) } throws RuntimeException("Order not found")

        val exception = runCatching { useCase(10) }.exceptionOrNull()

        assertEquals("Order not found", exception?.message)
    }
}