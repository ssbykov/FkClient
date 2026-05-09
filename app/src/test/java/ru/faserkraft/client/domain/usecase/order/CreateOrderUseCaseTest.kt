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

class CreateOrderUseCaseTest {

    // ── фикстуры ──────────────────────────────────────────────────────────────

    private val repository: OrderRepository = mockk()
    private lateinit var useCase: CreateOrderUseCase

    private val contractNumber = "КД-2024-001"
    private val contractDate = "2024-01-15"
    private val plannedShipmentDate = "2024-03-01"

    private val createdOrder = Order(
        id = 1,
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
        useCase = CreateOrderUseCase(repository)
    }

    // ── invoke() ──────────────────────────────────────────────────────────────

    @Test
    fun `invoke - returns created order from repository`() = runTest {
        coEvery {
            repository.createOrder(contractNumber, contractDate, plannedShipmentDate)
        } returns createdOrder

        val result = useCase(contractNumber, contractDate, plannedShipmentDate)

        assertEquals(createdOrder, result)
    }

    @Test
    fun `invoke - calls repository exactly once`() = runTest {
        coEvery { repository.createOrder(any(), any(), any()) } returns createdOrder

        useCase(contractNumber, contractDate, plannedShipmentDate)

        coVerify(exactly = 1) {
            repository.createOrder(contractNumber, contractDate, plannedShipmentDate)
        }
    }

    @Test
    fun `invoke - passes contractNumber unchanged`() = runTest {
        coEvery { repository.createOrder(any(), any(), any()) } returns createdOrder

        useCase(contractNumber, contractDate, plannedShipmentDate)

        coVerify { repository.createOrder(contractNumber, any(), any()) }
    }

    @Test
    fun `invoke - passes contractDate unchanged`() = runTest {
        coEvery { repository.createOrder(any(), any(), any()) } returns createdOrder

        useCase(contractNumber, contractDate, plannedShipmentDate)

        coVerify { repository.createOrder(any(), contractDate, any()) }
    }

    @Test
    fun `invoke - passes plannedShipmentDate unchanged`() = runTest {
        coEvery { repository.createOrder(any(), any(), any()) } returns createdOrder

        useCase(contractNumber, contractDate, plannedShipmentDate)

        coVerify { repository.createOrder(any(), any(), plannedShipmentDate) }
    }

    @Test
    fun `invoke - returned order has correct contractNumber`() = runTest {
        coEvery { repository.createOrder(any(), any(), any()) } returns createdOrder

        val result = useCase(contractNumber, contractDate, plannedShipmentDate)

        assertEquals(contractNumber, result.contractNumber)
    }

    @Test
    fun `invoke - returned order has null shipmentDate`() = runTest {
        coEvery { repository.createOrder(any(), any(), any()) } returns createdOrder

        val result = useCase(contractNumber, contractDate, plannedShipmentDate)

        assertEquals(null, result.shipmentDate)
    }

    @Test
    fun `invoke - propagates exception from repository`() = runTest {
        coEvery {
            repository.createOrder(any(), any(), any())
        } throws RuntimeException("Duplicate contract number")

        val exception = runCatching {
            useCase(contractNumber, contractDate, plannedShipmentDate)
        }.exceptionOrNull()

        assertEquals("Duplicate contract number", exception?.message)
    }

    @Test
    fun `invoke - different parameters produce different orders`() = runTest {
        val order2 = createdOrder.copy(id = 2, contractNumber = "КД-2024-002")
        coEvery {
            repository.createOrder(contractNumber, contractDate, plannedShipmentDate)
        } returns createdOrder
        coEvery {
            repository.createOrder("КД-2024-002", contractDate, plannedShipmentDate)
        } returns order2

        assertEquals(1, useCase(contractNumber, contractDate, plannedShipmentDate).id)
        assertEquals(2, useCase("КД-2024-002", contractDate, plannedShipmentDate).id)
    }
}