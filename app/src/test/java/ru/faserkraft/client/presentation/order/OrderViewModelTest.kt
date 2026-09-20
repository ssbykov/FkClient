package ru.faserkraft.client.presentation.order

import app.cash.turbine.test
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import ru.faserkraft.client.domain.model.OrderItem
import ru.faserkraft.client.domain.model.ProductStatus
import ru.faserkraft.client.domain.usecase.order.AddPackagingToOrderUseCase
import ru.faserkraft.client.domain.usecase.order.CloseOrderUseCase
import ru.faserkraft.client.domain.usecase.order.CreateOrderUseCase
import ru.faserkraft.client.domain.usecase.order.DeleteOrderUseCase
import ru.faserkraft.client.domain.usecase.order.DetachPackagingFromOrderUseCase
import ru.faserkraft.client.domain.usecase.order.GetOrdersUseCase
import ru.faserkraft.client.domain.usecase.order.UpdateOrderItemsUseCase
import ru.faserkraft.client.domain.usecase.order.UpdateOrderUseCase
import ru.faserkraft.client.domain.usecase.process.GetProcessesUseCase
import ru.faserkraft.client.util.MainDispatcherRule

@OptIn(ExperimentalCoroutinesApi::class)
class OrderViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    // ── Mocks ────────────────────────────────────────────────────────────────

    private val getOrdersUseCase: GetOrdersUseCase = mockk()
    private val createOrderUseCase: CreateOrderUseCase = mockk()
    private val updateOrderUseCase: UpdateOrderUseCase = mockk()
    private val updateOrderItemsUseCase: UpdateOrderItemsUseCase = mockk()
    private val closeOrderUseCase: CloseOrderUseCase = mockk()
    private val deleteOrderUseCase: DeleteOrderUseCase = mockk()
    private val addPackagingToOrderUseCase: AddPackagingToOrderUseCase = mockk()
    private val detachPackagingFromOrderUseCase: DetachPackagingFromOrderUseCase = mockk()
    private val getProcessesUseCase: GetProcessesUseCase = mockk()

    private lateinit var viewModel: OrderViewModel

    // ── Dummies ──────────────────────────────────────────────────────────────

    private val dummyOrder = mockk<ru.faserkraft.client.domain.model.Order>(relaxed = true) {
        every { id } returns 1
    }
    private val dummyOrdersList = listOf(dummyOrder)
    private val dummyProcess = mockk<ru.faserkraft.client.domain.model.Process>(relaxed = true)
    private val dummyProcessesList = listOf(dummyProcess)
    private val dummyOrderItem = mockk<OrderItem>(relaxed = true)

    @Before
    fun setUp() {
        viewModel = OrderViewModel(
            getOrdersUseCase = getOrdersUseCase,
            createOrderUseCase = createOrderUseCase,
            updateOrderUseCase = updateOrderUseCase,
            updateOrderItemsUseCase = updateOrderItemsUseCase,
            closeOrderUseCase = closeOrderUseCase,
            deleteOrderUseCase = deleteOrderUseCase,
            addPackagingToOrderUseCase = addPackagingToOrderUseCase,
            detachPackagingFromOrderUseCase = detachPackagingFromOrderUseCase,
            getProcessesUseCase = getProcessesUseCase,
        )
    }

    // ── loadOrders ───────────────────────────────────────────────────────────

    @Test
    fun `loadOrders - updates state with orders on success`() = runTest {
        coEvery { getOrdersUseCase() } returns dummyOrdersList

        viewModel.loadOrders()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(dummyOrdersList, state.orders)
        assertFalse(state.isLoading)
    }

    @Test
    fun `loadOrders - emits ShowError and resets loading on failure`() = runTest {
        coEvery { getOrdersUseCase() } throws RuntimeException("Network error")

        viewModel.events.test {
            viewModel.loadOrders()
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertFalse(state.isLoading)
            assertEquals(OrderEvent.ShowError("Неизвестная ошибка"), awaitItem())
        }
    }

    // ── selectOrder ──────────────────────────────────────────────────────────

    @Test
    fun `selectOrder - finds order in state and sets currentOrder`() = runTest {
        coEvery { getOrdersUseCase() } returns dummyOrdersList
        viewModel.loadOrders()
        advanceUntilIdle()

        viewModel.selectOrder(1)

        val state = viewModel.uiState.value
        assertEquals(dummyOrder, state.currentOrder)
    }

    // ── loadProcesses ────────────────────────────────────────────────────────

    @Test
    fun `loadProcesses - updates state with processes on success`() = runTest {
        coEvery { getProcessesUseCase() } returns dummyProcessesList

        viewModel.loadProcesses()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(dummyProcessesList, state.processes)
    }

    // ── createOrder ──────────────────────────────────────────────────────────

    @Test
    fun `createOrder - creates order, updates items, reloads and emits OrderCreated`() = runTest {
        coEvery { createOrderUseCase("123", "01.01.2026", "10.01.2026") } returns dummyOrder
        coEvery { updateOrderItemsUseCase(1, listOf(dummyOrderItem)) } returns dummyOrder
        coEvery { getOrdersUseCase() } returns dummyOrdersList

        viewModel.events.test {
            viewModel.createOrder("123", "01.01.2026", "10.01.2026", listOf(dummyOrderItem))
            advanceUntilIdle()

            coVerify(exactly = 1) { createOrderUseCase("123", "01.01.2026", "10.01.2026") }
            coVerify(exactly = 1) { updateOrderItemsUseCase(1, listOf(dummyOrderItem)) }
            coVerify(exactly = 1) { getOrdersUseCase() }
            assertFalse(viewModel.uiState.value.isActionInProgress)

            assertEquals(OrderEvent.OrderCreated, awaitItem())
        }
    }

    @Test
    fun `createOrder - emits ShowError if creation fails`() = runTest {
        coEvery { createOrderUseCase(any(), any(), any()) } throws RuntimeException("Error")

        viewModel.events.test {
            viewModel.createOrder("123", "01.01.2026", "10.01.2026", listOf(dummyOrderItem))
            advanceUntilIdle()

            assertFalse(viewModel.uiState.value.isActionInProgress)
            assertEquals(OrderEvent.ShowError("Неизвестная ошибка"), awaitItem())
        }
    }

    // ── updateOrder ──────────────────────────────────────────────────────────

    @Test
    fun `updateOrder - updates order and items, reloads and emits OrderUpdated`() = runTest {
        coEvery { updateOrderUseCase(1, "123", "01.01.2026", "10.01.2026") } returns dummyOrder
        coEvery { updateOrderItemsUseCase(1, listOf(dummyOrderItem)) } returns dummyOrder
        coEvery { getOrdersUseCase() } returns dummyOrdersList

        viewModel.events.test {
            viewModel.updateOrder(1, "123", "01.01.2026", "10.01.2026", listOf(dummyOrderItem))
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertEquals(dummyOrder, state.currentOrder)
            assertFalse(state.isActionInProgress)

            assertEquals(OrderEvent.OrderUpdated, awaitItem())
        }
    }

    // ── requestCloseOrder ────────────────────────────────────────────────────

    @Test
    fun `requestCloseOrder - emits ShowError if order not found`() = runTest {
        coEvery { getOrdersUseCase() } returns emptyList()
        viewModel.loadOrders()
        advanceUntilIdle()

        viewModel.events.test {
            viewModel.requestCloseOrder(999)
            advanceUntilIdle()

            assertEquals(OrderEvent.ShowError("Не удалось найти заказ"), awaitItem())
        }
    }

    @Test
    fun `requestCloseOrder - emits CloseOrderDenied if packaging contains non-normal products`() =
        runTest {
            val badProduct = mockk<ru.faserkraft.client.domain.model.ProductShort> {
                every { status } returns ProductStatus.SCRAP
            }
            val goodProduct = mockk<ru.faserkraft.client.domain.model.ProductShort> {
                every { status } returns ProductStatus.NORMAL
            }

            val badPackaging = mockk<ru.faserkraft.client.domain.model.Packaging> {
                every { serialNumber } returns "BOX-001"
                every { products } returns listOf(goodProduct, badProduct)
            }

            val goodPackaging = mockk<ru.faserkraft.client.domain.model.Packaging> {
                every { serialNumber } returns "BOX-002"
                every { products } returns listOf(goodProduct)
            }

            val order = mockk<ru.faserkraft.client.domain.model.Order> {
                every { id } returns 1
                every { contractNumber } returns "123"
                every { packaging } returns listOf(goodPackaging, badPackaging)
            }

            coEvery { getOrdersUseCase() } returns listOf(order)
            viewModel.loadOrders()
            advanceUntilIdle()

            viewModel.events.test {
                viewModel.requestCloseOrder(1)
                advanceUntilIdle()

                assertEquals(OrderEvent.CloseOrderDenied(listOf("BOX-001")), awaitItem())
            }
        }

    @Test
    fun `requestCloseOrder - emits ConfirmCloseOrder if all products are normal`() = runTest {
        val goodProduct = mockk<ru.faserkraft.client.domain.model.ProductShort> {
            every { status } returns ProductStatus.NORMAL
        }

        val goodPackaging = mockk<ru.faserkraft.client.domain.model.Packaging> {
            every { serialNumber } returns "BOX-002"
            every { products } returns listOf(goodProduct, goodProduct)
        }

        val order = mockk<ru.faserkraft.client.domain.model.Order> {
            every { id } returns 1
            every { contractNumber } returns "CONTRACT-777"
            every { packaging } returns listOf(goodPackaging)
        }

        coEvery { getOrdersUseCase() } returns listOf(order)
        viewModel.loadOrders()
        advanceUntilIdle()

        viewModel.events.test {
            viewModel.requestCloseOrder(1)
            advanceUntilIdle()

            assertEquals(OrderEvent.ConfirmCloseOrder(1, "CONTRACT-777"), awaitItem())
        }
    }

    // ── closeOrder ───────────────────────────────────────────────────────────

    @Test
    fun `closeOrder - closes order, updates state, reloads and emits OrderClosed`() = runTest {
        coEvery { closeOrderUseCase(1) } returns dummyOrder
        coEvery { getOrdersUseCase() } returns dummyOrdersList

        viewModel.events.test {
            viewModel.closeOrder(1)
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertEquals(dummyOrder, state.currentOrder)
            assertFalse(state.isActionInProgress)

            assertEquals(OrderEvent.OrderClosed, awaitItem())
        }
    }

    // ── deleteOrder ──────────────────────────────────────────────────────────

    @Test
    fun `deleteOrder - deletes order, clears currentOrder if matched, reloads and emits OrderDeleted`() =
        runTest {
            coEvery { getOrdersUseCase() } returns dummyOrdersList
            viewModel.loadOrders()
            advanceUntilIdle()
            viewModel.selectOrder(1)

            coEvery { deleteOrderUseCase(1) } returns Unit
            coEvery { getOrdersUseCase() } returns dummyOrdersList

            viewModel.events.test {
                viewModel.deleteOrder(1)
                advanceUntilIdle()

                val state = viewModel.uiState.value
                assertNull(state.currentOrder)
                assertFalse(state.isActionInProgress)

                assertEquals(OrderEvent.OrderDeleted, awaitItem())
            }
        }

    @Test
    fun `deleteOrder - does not clear currentOrder if id does not match`() = runTest {
        val anotherOrder = mockk<ru.faserkraft.client.domain.model.Order>(relaxed = true) {
            every { id } returns 2
        }
        coEvery { getOrdersUseCase() } returns listOf(anotherOrder)
        viewModel.loadOrders()
        advanceUntilIdle()
        viewModel.selectOrder(2)

        coEvery { deleteOrderUseCase(1) } returns Unit
        coEvery { getOrdersUseCase() } returns listOf(anotherOrder)

        viewModel.deleteOrder(1)
        advanceUntilIdle()

        assertEquals(anotherOrder, viewModel.uiState.value.currentOrder)
    }

    // ── requestAddPackaging ──────────────────────────────────────────────────

    @Test
    fun `requestAddPackaging - emits ShowError if list is empty`() = runTest {
        viewModel.events.test {
            viewModel.requestAddPackaging(1, emptyList())
            advanceUntilIdle()

            assertEquals(
                OrderEvent.ShowError("Вы не выбрали ни одной упаковки для добавления"),
                awaitItem()
            )
        }
    }

    @Test
    fun `requestAddPackaging - emits AddPackagingDenied if items have non-normal products`() =
        runTest {
            val badUiItem = mockk<PackagingShipmentUiItem> {
                every { hasNonNormalProducts } returns true
                every { serialNumber } returns "UI-BOX-99"
            }
            val goodUiItem = mockk<PackagingShipmentUiItem> {
                every { hasNonNormalProducts } returns false
                every { serialNumber } returns "UI-BOX-88"
            }

            viewModel.events.test {
                viewModel.requestAddPackaging(1, listOf(badUiItem, goodUiItem))
                advanceUntilIdle()

                assertEquals(OrderEvent.AddPackagingDenied(listOf("UI-BOX-99")), awaitItem())
            }
        }

    @Test
    fun `requestAddPackaging - emits ConfirmAddPackaging if all items are valid`() = runTest {
        val goodUiItem1 = mockk<PackagingShipmentUiItem> {
            every { id } returns 101
            every { hasNonNormalProducts } returns false
        }
        val goodUiItem2 = mockk<PackagingShipmentUiItem> {
            every { id } returns 102
            every { hasNonNormalProducts } returns false
        }

        viewModel.events.test {
            val list = listOf(goodUiItem1, goodUiItem2)
            viewModel.requestAddPackaging(1, list)
            advanceUntilIdle()

            assertEquals(OrderEvent.ConfirmAddPackaging(1, listOf(101, 102), 2), awaitItem())
        }
    }

    // ── addPackagingToOrder ──────────────────────────────────────────────────

    @Test
    fun `addPackagingToOrder - adds packaging, reloads list, emits PackagingAdded`() =
        runTest {
            val packIds = listOf(10, 11)
            coEvery { addPackagingToOrderUseCase(1, packIds) } returns Unit
            coEvery { getOrdersUseCase() } returns dummyOrdersList

            viewModel.events.test {
                viewModel.addPackagingToOrder(1, packIds)
                advanceUntilIdle()

                coVerify(exactly = 1) { getOrdersUseCase() }
                assertEquals(OrderEvent.PackagingAdded, awaitItem())
            }
        }

    // ── detachPackagingFromOrder ─────────────────────────────────────────────

    @Test
    fun `detachPackagingFromOrder - detaches packaging, reloads list`() = runTest {
        val packIds = listOf(10, 11)
        coEvery { detachPackagingFromOrderUseCase(packIds) } returns Unit
        coEvery { getOrdersUseCase() } returns dummyOrdersList

        viewModel.detachPackagingFromOrder(packIds)
        advanceUntilIdle()

        coVerify(exactly = 1) { detachPackagingFromOrderUseCase(packIds) }
        coVerify(exactly = 1) { getOrdersUseCase() }
        assertFalse(viewModel.uiState.value.isActionInProgress)
    }

    @Test
    fun `detachPackagingFromOrder - emits ShowError and DetachPackagingFailed on failure`() = runTest {
        val packIds = listOf(10)
        coEvery { detachPackagingFromOrderUseCase(packIds) } throws RuntimeException("Error")

        viewModel.events.test {
            viewModel.detachPackagingFromOrder(packIds)
            advanceUntilIdle()

            assertEquals(OrderEvent.ShowError("Неизвестная ошибка"), awaitItem())
            assertEquals(OrderEvent.DetachPackagingFailed(10), awaitItem())
        }
    }
}
