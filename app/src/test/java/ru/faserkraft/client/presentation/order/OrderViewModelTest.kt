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
import ru.faserkraft.client.domain.usecase.order.GetOrderUseCase
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
    private val getOrderUseCase: GetOrderUseCase = mockk()
    private val createOrderUseCase: CreateOrderUseCase = mockk()
    private val updateOrderUseCase: UpdateOrderUseCase = mockk()
    private val updateOrderItemsUseCase: UpdateOrderItemsUseCase = mockk()
    private val closeOrderUseCase: CloseOrderUseCase = mockk()
    private val deleteOrderUseCase: DeleteOrderUseCase = mockk()
    private val addPackagingToOrderUseCase: AddPackagingToOrderUseCase = mockk()
    private val detachPackagingFromOrderUseCase: DetachPackagingFromOrderUseCase = mockk()
    private val getProcessesUseCase: GetProcessesUseCase = mockk()

    private lateinit var viewModel: OrderViewModel

    // ── Dummies (используем relaxed mockk, так как структура классов неизвестна) ──

    // Предполагается, что эти классы есть в domain.model
    // Если структура известна, лучше заменить mockk(relaxed = true) на реальные вызовы конструкторов
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
            getOrderUseCase = getOrderUseCase,
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
            // Проверяем результат маппера toErrorMessage()
            assertEquals(OrderEvent.ShowError("Неизвестная ошибка"), awaitItem())
        }
    }

    // ── loadOrder ────────────────────────────────────────────────────────────

    @Test
    fun `loadOrder - updates state with currentOrder on success`() = runTest {
        coEvery { getOrderUseCase(1) } returns dummyOrder

        viewModel.loadOrder(1)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(dummyOrder, state.currentOrder)
        assertFalse(state.isLoading)
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
            // Создаем продукт с браком
            val badProduct = mockk<ru.faserkraft.client.domain.model.ProductShort> {
                every { status } returns ProductStatus.SCRAP
            }
            val goodProduct = mockk<ru.faserkraft.client.domain.model.ProductShort> {
                every { status } returns ProductStatus.NORMAL
            }

            // Упаковка с браком
            val badPackaging = mockk<ru.faserkraft.client.domain.model.Packaging> {
                every { serialNumber } returns "BOX-001"
                every { products } returns listOf(goodProduct, badProduct)
            }

            // Упаковка без брака
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

                // Ожидаем отказ с указанием серийника плохой коробки
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
            // Подготавливаем state так, будто удаляемый заказ сейчас открыт
            coEvery { getOrderUseCase(1) } returns dummyOrder
            viewModel.loadOrder(1)
            advanceUntilIdle()

            coEvery { deleteOrderUseCase(1) } returns Unit
            coEvery { getOrdersUseCase() } returns dummyOrdersList

            viewModel.events.test {
                viewModel.deleteOrder(1)
                advanceUntilIdle()

                val state = viewModel.uiState.value
                assertNull(state.currentOrder) // Текущий заказ должен сброситься в null
                assertFalse(state.isActionInProgress)

                assertEquals(OrderEvent.OrderDeleted, awaitItem())
            }
        }

    @Test
    fun `deleteOrder - does not clear currentOrder if id does not match`() = runTest {
        val anotherOrder = mockk<ru.faserkraft.client.domain.model.Order>(relaxed = true) {
            every { id } returns 2
        }
        coEvery { getOrderUseCase(2) } returns anotherOrder
        viewModel.loadOrder(2)
        advanceUntilIdle()

        coEvery { deleteOrderUseCase(1) } returns Unit
        coEvery { getOrdersUseCase() } returns dummyOrdersList

        viewModel.deleteOrder(1)
        advanceUntilIdle()

        // Удаляли заказ 1, а открыт был заказ 2. Он должен остаться в state.
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
    fun `addPackagingToOrder - adds packaging, reloads list and order, emits PackagingAdded`() =
        runTest {
            val packIds = listOf(10, 11)
            coEvery { addPackagingToOrderUseCase(1, packIds) } returns Unit
            coEvery { getOrdersUseCase() } returns dummyOrdersList
            coEvery { getOrderUseCase(1) } returns dummyOrder

            viewModel.events.test {
                viewModel.addPackagingToOrder(1, packIds)
                advanceUntilIdle()

                coVerify(exactly = 1) { getOrdersUseCase() }
                coVerify(exactly = 1) { getOrderUseCase(1) }
                assertEquals(OrderEvent.PackagingAdded, awaitItem())
            }
        }

    // ── detachPackagingFromOrder ─────────────────────────────────────────────

    @Test
    fun `detachPackagingFromOrder - detaches packaging, reloads list and order`() = runTest {
        val packIds = listOf(10, 11)
        coEvery { detachPackagingFromOrderUseCase(packIds) } returns Unit
        coEvery { getOrdersUseCase() } returns dummyOrdersList
        coEvery { getOrderUseCase(1) } returns dummyOrder

        // Здесь нет проверки event.test { }, так как ViewModel
        // не отправляет никаких OrderEvent при успешном выполнении этого метода.
        viewModel.detachPackagingFromOrder(1, packIds)
        advanceUntilIdle()

        coVerify(exactly = 1) { detachPackagingFromOrderUseCase(packIds) }
        coVerify(exactly = 1) { getOrdersUseCase() }
        coVerify(exactly = 1) { getOrderUseCase(1) }
        assertFalse(viewModel.uiState.value.isActionInProgress)
    }

    @Test
    fun `detachPackagingFromOrder - emits ShowError on failure`() = runTest {
        val packIds = listOf(10, 11)
        coEvery { detachPackagingFromOrderUseCase(packIds) } throws RuntimeException("Error")

        viewModel.events.test {
            viewModel.detachPackagingFromOrder(1, packIds)
            advanceUntilIdle()

            assertEquals(OrderEvent.ShowError("Неизвестная ошибка"), awaitItem())
        }
    }
}