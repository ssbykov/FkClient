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

class UpdateOrderUseCaseTest {

    // ── фикстуры ──────────────────────────────────────────────────────────────

    private val repository: OrderRepository = mockk()
    private lateinit var useCase: UpdateOrderUseCase

    private val orderId = 10
    private val contractNumber = "КД-2024-001"
    private val contractDate = "2024-01-15"
    private val plannedShipmentDate = "2024-03-01"

    private val updatedOrder = Order(
        id = orderId,
        contractNumber = contractNumber,
        contractDate = contractDate,
        plannedShipmentDate = plannedShipmentDate,
        shipmentDate = null,
        shipmentBy = null,
        items = emptyList(),
        packaging = emptyList()
    )

    @Before
    fun setUp() {
        useCase = UpdateOrderUseCase(repository)
    }

    // ── invoke() ──────────────────────────────────────────────────────────────

    @Test
    fun `invoke - returns updated order from repository`() = runTest {
        coEvery {
            repository.updateOrder(orderId, contractNumber, contractDate, plannedShipmentDate)
        } returns updatedOrder

        val result = useCase(orderId, contractNumber, contractDate, plannedShipmentDate)

        assertEquals(updatedOrder, result)
    }

    @Test
    fun `invoke - calls repository exactly once`() = runTest {
        coEvery { repository.updateOrder(any(), any(), any(), any()) } returns updatedOrder

        useCase(orderId, contractNumber, contractDate, plannedShipmentDate)

        coVerify(exactly = 1) {
            repository.updateOrder(orderId, contractNumber, contractDate, plannedShipmentDate)
        }
    }

    @Test
    fun `invoke - passes orderId unchanged`() = runTest {
        coEvery { repository.updateOrder(any(), any(), any(), any()) } returns updatedOrder

        useCase(99, contractNumber, contractDate, plannedShipmentDate)

        coVerify { repository.updateOrder(99, any(), any(), any()) }
    }

    @Test
    fun `invoke - passes contractNumber unchanged`() = runTest {
        coEvery { repository.updateOrder(any(), any(), any(), any()) } returns updatedOrder

        useCase(orderId, contractNumber, contractDate, plannedShipmentDate)

        coVerify { repository.updateOrder(any(), contractNumber, any(), any()) }
    }

    @Test
    fun `invoke - passes contractDate unchanged`() = runTest {
        coEvery { repository.updateOrder(any(), any(), any(), any()) } returns updatedOrder

        useCase(orderId, contractNumber, contractDate, plannedShipmentDate)

        coVerify { repository.updateOrder(any(), any(), contractDate, any()) }
    }

    @Test
    fun `invoke - passes plannedShipmentDate unchanged`() = runTest {
        coEvery { repository.updateOrder(any(), any(), any(), any()) } returns updatedOrder

        useCase(orderId, contractNumber, contractDate, plannedShipmentDate)

        coVerify { repository.updateOrder(any(), any(), any(), plannedShipmentDate) }
    }

    @Test
    fun `invoke - returned order has correct id`() = runTest {
        coEvery { repository.updateOrder(any(), any(), any(), any()) } returns updatedOrder

        val result = useCase(orderId, contractNumber, contractDate, plannedShipmentDate)

        assertEquals(orderId, result.id)
    }

    @Test
    fun `invoke - returned order has updated contractNumber`() = runTest {
        coEvery { repository.updateOrder(any(), any(), any(), any()) } returns updatedOrder

        val result = useCase(orderId, contractNumber, contractDate, plannedShipmentDate)

        assertEquals(contractNumber, result.contractNumber)
    }

    @Test
    fun `invoke - different parameters produce different orders`() = runTest {
        val order2 = updatedOrder.copy(id = 20, contractNumber = "КД-2024-002")
        coEvery {
            repository.updateOrder(orderId, contractNumber, contractDate, plannedShipmentDate)
        } returns updatedOrder
        coEvery {
            repository.updateOrder(20, "КД-2024-002", contractDate, plannedShipmentDate)
        } returns order2

        assertEquals(10, useCase(orderId, contractNumber, contractDate, plannedShipmentDate).id)
        assertEquals(20, useCase(20, "КД-2024-002", contractDate, plannedShipmentDate).id)
    }

    @Test
    fun `invoke - propagates exception from repository`() = runTest {
        coEvery {
            repository.updateOrder(any(), any(), any(), any())
        } throws RuntimeException("Order is closed")

        val exception = runCatching {
            useCase(orderId, contractNumber, contractDate, plannedShipmentDate)
        }.exceptionOrNull()

        assertEquals("Order is closed", exception?.message)
    }
}