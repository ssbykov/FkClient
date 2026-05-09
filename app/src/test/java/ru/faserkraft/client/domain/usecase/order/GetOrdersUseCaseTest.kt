package ru.faserkraft.client.domain.usecase.order

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import ru.faserkraft.client.domain.model.Order
import ru.faserkraft.client.domain.repository.OrderRepository

class GetOrdersUseCaseTest {

    // ── фикстуры ──────────────────────────────────────────────────────────────

    private val repository: OrderRepository = mockk()
    private lateinit var useCase: GetOrdersUseCase

    private val orders = listOf(
        Order(
            id = 1,
            contractNumber = "КД-2024-001",
            contractDate = "2024-01-15",
            plannedShipmentDate = "2024-03-01",
            shipmentDate = null,
            shipmentBy = null,
            items = emptyList(),
            packaging = emptyList()
        ),
        Order(
            id = 2,
            contractNumber = "КД-2024-002",
            contractDate = "2024-02-10",
            plannedShipmentDate = "2024-04-01",
            shipmentDate = "2024-04-03",
            shipmentBy = null,
            items = emptyList(),
            packaging = emptyList()
        )
    )

    @Before
    fun setUp() {
        useCase = GetOrdersUseCase(repository)
    }

    // ── invoke() ──────────────────────────────────────────────────────────────

    @Test
    fun `invoke - returns list of orders from repository`() = runTest {
        coEvery { repository.getAllOrders() } returns orders

        val result = useCase()

        assertEquals(orders, result)
    }

    @Test
    fun `invoke - calls repository exactly once`() = runTest {
        coEvery { repository.getAllOrders() } returns orders

        useCase()

        coVerify(exactly = 1) { repository.getAllOrders() }
    }

    @Test
    fun `invoke - returns correct orders count`() = runTest {
        coEvery { repository.getAllOrders() } returns orders

        val result = useCase()

        assertEquals(2, result.size)
    }

    @Test
    fun `invoke - returns orders with correct fields`() = runTest {
        coEvery { repository.getAllOrders() } returns orders

        val result = useCase()

        assertEquals(1, result[0].id)
        assertEquals("КД-2024-001", result[0].contractNumber)
        assertEquals(2, result[1].id)
        assertEquals("КД-2024-002", result[1].contractNumber)
    }

    @Test
    fun `invoke - returns empty list if repository returns empty`() = runTest {
        coEvery { repository.getAllOrders() } returns emptyList()

        val result = useCase()

        assertTrue(result.isEmpty())
    }

    @Test
    fun `invoke - open order has null shipmentDate`() = runTest {
        coEvery { repository.getAllOrders() } returns orders

        val result = useCase()

        assertEquals(null, result[0].shipmentDate)
    }

    @Test
    fun `invoke - closed order has non-null shipmentDate`() = runTest {
        coEvery { repository.getAllOrders() } returns orders

        val result = useCase()

        assertEquals("2024-04-03", result[1].shipmentDate)
    }

    @Test
    fun `invoke - propagates exception from repository`() = runTest {
        coEvery { repository.getAllOrders() } throws RuntimeException("Network error")

        val exception = runCatching { useCase() }.exceptionOrNull()

        assertEquals("Network error", exception?.message)
    }

    @Test
    fun `invoke - returns single order list correctly`() = runTest {
        val single = listOf(orders.first())
        coEvery { repository.getAllOrders() } returns single

        val result = useCase()

        assertEquals(1, result.size)
        assertEquals(1, result.first().id)
    }
}