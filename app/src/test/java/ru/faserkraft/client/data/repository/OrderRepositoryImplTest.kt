package ru.faserkraft.client.data.repository

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Response
import ru.faserkraft.client.data.dto.OrderCreateDto
import ru.faserkraft.client.data.dto.OrderDto
import ru.faserkraft.client.data.dto.OrderItemCreateDto
import ru.faserkraft.client.data.dto.OrderItemDto
import ru.faserkraft.client.data.dto.OrderUpdateDto
import ru.faserkraft.client.data.dto.ProcessDto
import ru.faserkraft.client.data.network.Api
import ru.faserkraft.client.domain.model.OrderItem
import ru.faserkraft.client.domain.model.Process
import ru.faserkraft.client.error.AppError
import ru.faserkraft.client.util.FakeLogger
import java.io.IOException

class OrderRepositoryImplTest {

    private lateinit var api: Api
    private lateinit var fakeLogger: FakeLogger
    private lateinit var repository: OrderRepositoryImpl

    // ─── fixtures ─────────────────────────────────────────────────────────────

    private val processDto = ProcessDto(id = 1, name = "Cutting")

    private val orderItemDto = OrderItemDto(
        id = 10,
        quantity = 5,
        workProcess = processDto,
    )

    private val orderDto = OrderDto(
        id = 100,
        contractNumber = "CNT-001",
        contractDate = "2024-01-01",
        plannedShipmentDate = "2024-02-01",
        shipmentDate = null,
        shipmentBy = null,
        items = listOf(orderItemDto),
        packaging = emptyList(),
    )

    private val orderItem = OrderItem(
        id = 10,
        quantity = 5,
        workProcess = Process(id = 1, name = "Cutting", description = "", steps = emptyList()),
    )

    @Before
    fun setUp() {
        api = mockk()
        fakeLogger = FakeLogger()
        repository = OrderRepositoryImpl(api, fakeLogger)
    }

    // ─── getAllOrders ──────────────────────────────────────────────────────────

    @Test
    fun `getAllOrders - returns mapped list on success`() = runTest {
        coEvery { api.getAllOrders() } returns Response.success(listOf(orderDto))

        val result = repository.getAllOrders()

        assertEquals(1, result.size)
        assertEquals(100, result[0].id)
        assertEquals("CNT-001", result[0].contractNumber)
        coVerify(exactly = 1) { api.getAllOrders() }
        assertTrue(fakeLogger.errors.isEmpty())
    }

    @Test
    fun `getAllOrders - returns empty list when response body is null`() = runTest {
        coEvery { api.getAllOrders() } returns Response.success(null)

        val result = repository.getAllOrders()

        assertTrue(result.isEmpty())
    }

    @Test
    fun `getAllOrders - throws AppError ApiError on HTTP 500`() = runTest {
        coEvery { api.getAllOrders() } returns makeErrorResponse(500)

        val ex = catchError<AppError.ApiError> { repository.getAllOrders() }

        assertNotNull(ex)
        assertEquals(500, ex!!.status)
        assertTrue(fakeLogger.errors.any { it.message.contains("500") })
    }

    @Test
    fun `getAllOrders - throws AppError NetworkError on IOException`() = runTest {
        coEvery { api.getAllOrders() } throws IOException("timeout")

        val ex = catchError<AppError.NetworkError> { repository.getAllOrders() }

        assertNotNull(ex)
        assertTrue(ex!!.error is IOException)
    }

    // ─── getOrder ─────────────────────────────────────────────────────────────

    @Test
    fun `getOrder - returns mapped order on success`() = runTest {
        coEvery { api.getOrder(any()) } returns Response.success(orderDto)

        val result = repository.getOrder(orderId = 100)

        assertEquals(100, result.id)
        assertEquals("CNT-001", result.contractNumber)
        coVerify(exactly = 1) { api.getOrder(100) }
    }

    @Test
    fun `getOrder - throws IllegalArgumentException when response body is null`() = runTest {
        coEvery { api.getOrder(any()) } returns Response.success(null)

        val ex = catchError<IllegalArgumentException> { repository.getOrder(100) }

        assertNotNull(ex)
    }

    @Test
    fun `getOrder - throws AppError ApiError on HTTP 404`() = runTest {
        coEvery { api.getOrder(any()) } returns makeErrorResponse(
            code = 404,
            body = """{"code":"order_not_found","detail":"Order not found"}"""
        )

        val ex = catchError<AppError.ApiError> { repository.getOrder(100) }

        assertNotNull(ex)
        assertEquals(404, ex!!.status)
        assertEquals("order_not_found", ex.uiCode)
    }

    // ─── createOrder ──────────────────────────────────────────────────────────

    @Test
    fun `createOrder - returns created order on success`() = runTest {
        coEvery { api.createOrder(any()) } returns Response.success(orderDto)

        val result = repository.createOrder(
            contractNumber = "CNT-001",
            contractDate = "2024-01-01",
            plannedShipmentDate = "2024-02-01",
        )

        assertEquals(100, result.id)
        coVerify(exactly = 1) {
            api.createOrder(
                OrderCreateDto(
                    contractNumber = "CNT-001",
                    contractDate = "2024-01-01",
                    plannedShipmentDate = "2024-02-01",
                )
            )
        }
    }

    @Test
    fun `createOrder - throws IllegalArgumentException when response body is null`() = runTest {
        coEvery { api.createOrder(any()) } returns Response.success(null)

        val ex = catchError<IllegalArgumentException> {
            repository.createOrder("CNT-001", "2024-01-01", "2024-02-01")
        }

        assertNotNull(ex)
    }

    @Test
    fun `createOrder - throws AppError ApiError on HTTP 422`() = runTest {
        coEvery { api.createOrder(any()) } returns makeErrorResponse(422)

        val ex = catchError<AppError.ApiError> {
            repository.createOrder("CNT-001", "2024-01-01", "2024-02-01")
        }

        assertNotNull(ex)
        assertEquals(422, ex!!.status)
    }

    // ─── updateOrder ──────────────────────────────────────────────────────────

    @Test
    fun `updateOrder - returns updated order on success`() = runTest {
        coEvery { api.updateOrder(any()) } returns Response.success(orderDto)

        val result = repository.updateOrder(
            orderId = 100,
            contractNumber = "CNT-002",
            contractDate = "2024-01-15",
            plannedShipmentDate = "2024-02-15",
        )

        assertEquals(100, result.id)
        coVerify(exactly = 1) {
            api.updateOrder(
                OrderUpdateDto(
                    id = 100,
                    contractNumber = "CNT-002",
                    contractDate = "2024-01-15",
                    plannedShipmentDate = "2024-02-15",
                )
            )
        }
    }

    @Test
    fun `updateOrder - throws IllegalArgumentException when response body is null`() = runTest {
        coEvery { api.updateOrder(any()) } returns Response.success(null)

        val ex = catchError<IllegalArgumentException> {
            repository.updateOrder(100, "CNT-002", "2024-01-15", "2024-02-15")
        }

        assertNotNull(ex)
    }

    // ─── updateOrderItems ─────────────────────────────────────────────────────

    @Test
    fun `updateOrderItems - returns updated order on success`() = runTest {
        coEvery { api.updateOrderItems(any(), any()) } returns Response.success(orderDto)

        val result = repository.updateOrderItems(
            orderId = 100,
            items = listOf(orderItem),
        )

        assertEquals(100, result.id)
        coVerify(exactly = 1) {
            api.updateOrderItems(
                100,
                listOf(OrderItemCreateDto(processId = 1, quantity = 5))
            )
        }
    }

    @Test
    fun `updateOrderItems - throws IllegalArgumentException when response body is null`() =
        runTest {
            coEvery { api.updateOrderItems(any(), any()) } returns Response.success(null)

            val ex = catchError<IllegalArgumentException> {
                repository.updateOrderItems(100, listOf(orderItem))
            }

            assertNotNull(ex)
        }

    // ─── closeOrder ───────────────────────────────────────────────────────────

    @Test
    fun `closeOrder - returns closed order on success`() = runTest {
        coEvery { api.closeOrder(any()) } returns Response.success(orderDto)

        val result = repository.closeOrder(orderId = 100)

        assertEquals(100, result.id)
        coVerify(exactly = 1) { api.closeOrder(100) }
    }

    @Test
    fun `closeOrder - throws IllegalArgumentException when response body is null`() = runTest {
        coEvery { api.closeOrder(any()) } returns Response.success(null)

        val ex = catchError<IllegalArgumentException> { repository.closeOrder(100) }

        assertNotNull(ex)
    }

    @Test
    fun `closeOrder - throws AppError ApiError on HTTP 409`() = runTest {
        coEvery { api.closeOrder(any()) } returns makeErrorResponse(
            code = 409,
            body = """{"code":"order_already_closed","detail":"Order is already closed"}"""
        )

        val ex = catchError<AppError.ApiError> { repository.closeOrder(100) }

        assertNotNull(ex)
        assertEquals(409, ex!!.status)
        assertEquals("order_already_closed", ex.uiCode)
    }

    // ─── deleteOrder ──────────────────────────────────────────────────────────

    @Test
    fun `deleteOrder - completes without error on success`() = runTest {
        coEvery { api.deleteOrder(any()) } returns Response.success(Unit)

        repository.deleteOrder(orderId = 100)

        coVerify(exactly = 1) { api.deleteOrder(100) }
        assertTrue(fakeLogger.errors.isEmpty())
    }

    @Test
    fun `deleteOrder - throws AppError ApiError on HTTP 404`() = runTest {
        coEvery { api.deleteOrder(any()) } returns makeErrorResponse(404)

        val ex = catchError<AppError.ApiError> { repository.deleteOrder(100) }

        assertNotNull(ex)
        assertEquals(404, ex!!.status)
    }

    // ─── addPackagingToOrder ──────────────────────────────────────────────────

    @Test
    fun `addPackagingToOrder - completes without error on success`() = runTest {
        coEvery { api.addPackagingToOrder(any(), any()) } returns Response.success(true)

        repository.addPackagingToOrder(orderId = 100, packagingIds = listOf(1, 2, 3))

        coVerify(exactly = 1) { api.addPackagingToOrder(100, listOf(1, 2, 3)) }
        assertTrue(fakeLogger.errors.isEmpty())
    }

    @Test
    fun `addPackagingToOrder - throws AppError ApiError on HTTP 404`() = runTest {
        coEvery { api.addPackagingToOrder(any(), any()) } returns makeErrorResponse(404)

        val ex = catchError<AppError.ApiError> {
            repository.addPackagingToOrder(100, listOf(1, 2))
        }

        assertNotNull(ex)
        assertEquals(404, ex!!.status)
    }

    // ─── detachPackagingFromOrder ─────────────────────────────────────────────

    @Test
    fun `detachPackagingFromOrder - completes without error on success`() = runTest {
        coEvery { api.detachPackagingFromOrder(any()) } returns Response.success(true)

        repository.detachPackagingFromOrder(packagingIds = listOf(1, 2))

        coVerify(exactly = 1) { api.detachPackagingFromOrder(listOf(1, 2)) }
        assertTrue(fakeLogger.errors.isEmpty())
    }

    @Test
    fun `detachPackagingFromOrder - throws AppError NetworkError on IOException`() = runTest {
        coEvery { api.detachPackagingFromOrder(any()) } throws IOException("network error")

        val ex = catchError<AppError.NetworkError> {
            repository.detachPackagingFromOrder(listOf(1, 2))
        }

        assertNotNull(ex)
        assertTrue(ex!!.error is IOException)
    }

    // ─── helpers ──────────────────────────────────────────────────────────────

    private inline fun <reified T> makeErrorResponse(
        code: Int,
        body: String = "",
    ): Response<T> =
        Response.error(code, body.toResponseBody("application/json".toMediaType()))

    private suspend inline fun <reified T : Throwable> catchError(
        crossinline block: suspend () -> Unit
    ): T? = try {
        block()
        null
    } catch (e: Throwable) {
        e as? T
    }
}