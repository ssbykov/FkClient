package ru.faserkraft.client.domain.usecase.order

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import ru.faserkraft.client.domain.model.Order
import ru.faserkraft.client.domain.model.OrderItem
import ru.faserkraft.client.domain.model.Process
import ru.faserkraft.client.domain.repository.OrderRepository

class UpdateOrderItemsUseCaseTest {

    // ── фикстуры ──────────────────────────────────────────────────────────────

    private val repository: OrderRepository = mockk()
    private lateinit var useCase: UpdateOrderItemsUseCase

    private val process = Process(
        id = 1,
        name = "Сборка",
        description = "",
        steps = emptyList()
    )

    private val items = listOf(
        OrderItem(id = 1, quantity = 10, workProcess = process),
        OrderItem(id = 2, quantity = 20, workProcess = process),
    )

    private val updatedOrder = Order(
        id = 5,
        contractNumber = "КД-2024-001",
        contractDate = "2024-01-15",
        plannedShipmentDate = "2024-03-01",
        shipmentDate = null,
        shipmentBy = null,
        items = items,
        packaging = emptyList()
    )

    @Before
    fun setUp() {
        useCase = UpdateOrderItemsUseCase(repository)
    }

    // ── invoke() ──────────────────────────────────────────────────────────────

    @Test
    fun `invoke - returns updated order from repository`() = runTest {
        coEvery { repository.updateOrderItems(5, items) } returns updatedOrder

        val result = useCase(5, items)

        assertEquals(updatedOrder, result)
    }

    @Test
    fun `invoke - calls repository exactly once`() = runTest {
        coEvery { repository.updateOrderItems(any(), any()) } returns updatedOrder

        useCase(5, items)

        coVerify(exactly = 1) { repository.updateOrderItems(5, items) }
    }

    @Test
    fun `invoke - passes orderId unchanged`() = runTest {
        coEvery { repository.updateOrderItems(any(), any()) } returns updatedOrder

        useCase(99, items)

        coVerify { repository.updateOrderItems(99, any()) }
    }

    @Test
    fun `invoke - passes items list unchanged`() = runTest {
        coEvery { repository.updateOrderItems(any(), any()) } returns updatedOrder

        useCase(5, items)

        coVerify { repository.updateOrderItems(any(), items) }
    }

    @Test
    fun `invoke - returned order contains updated items`() = runTest {
        coEvery { repository.updateOrderItems(5, items) } returns updatedOrder

        val result = useCase(5, items)

        assertEquals(2, result.items.size)
        assertEquals(10, result.items[0].quantity)
        assertEquals(20, result.items[1].quantity)
    }

    @Test
    fun `invoke - works with empty items list`() = runTest {
        val emptyOrder = updatedOrder.copy(items = emptyList())
        coEvery { repository.updateOrderItems(5, emptyList()) } returns emptyOrder

        val result = useCase(5, emptyList())

        assertEquals(0, result.items.size)
    }

    @Test
    fun `invoke - works with single item`() = runTest {
        val singleItem = listOf(items.first())
        val singleOrder = updatedOrder.copy(items = singleItem)
        coEvery { repository.updateOrderItems(5, singleItem) } returns singleOrder

        val result = useCase(5, singleItem)

        assertEquals(1, result.items.size)
        assertEquals(10, result.items.first().quantity)
    }

    @Test
    fun `invoke - propagates exception from repository`() = runTest {
        coEvery {
            repository.updateOrderItems(any(), any())
        } throws RuntimeException("Order is closed")

        val exception = runCatching { useCase(5, items) }.exceptionOrNull()

        assertEquals("Order is closed", exception?.message)
    }
}