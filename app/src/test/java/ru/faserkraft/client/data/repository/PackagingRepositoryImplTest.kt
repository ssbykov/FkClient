package ru.faserkraft.client.data.repository

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Response
import ru.faserkraft.client.data.dto.PackagingCreateDto
import ru.faserkraft.client.data.dto.PackagingDto
import ru.faserkraft.client.data.network.Api
import ru.faserkraft.client.error.AppError
import ru.faserkraft.client.util.FakeLogger
import java.io.IOException

class PackagingRepositoryImplTest {

    private lateinit var api: Api
    private lateinit var fakeLogger: FakeLogger
    private lateinit var repository: PackagingRepositoryImpl

    // ─── fixtures ─────────────────────────────────────────────────────────────

    private val packagingDto = PackagingDto(
        id = 1,
        serialNumber = "SN-001",
        performedBy = null,
        performedAt = null,
        orderId = null,
        products = emptyList(),
    )

    @Before
    fun setUp() {
        api = mockk()
        fakeLogger = FakeLogger()
        repository = PackagingRepositoryImpl(api, fakeLogger)
    }

    // ─── getPackaging ─────────────────────────────────────────────────────────

    @Test
    fun `getPackaging - returns mapped packaging on success`() = runTest {
        coEvery { api.getPackaging(any()) } returns Response.success(packagingDto)

        val result = repository.getPackaging("SN-001")

        assertNotNull(result)
        assertEquals(1, result!!.id)
        assertEquals("SN-001", result.serialNumber)
        coVerify(exactly = 1) { api.getPackaging("SN-001") }
        assertTrue(fakeLogger.errors.isEmpty())
    }

    @Test
    fun `getPackaging - returns null when response body is null`() = runTest {
        coEvery { api.getPackaging(any()) } returns Response.success(null)

        val result = repository.getPackaging("SN-001")

        assertNull(result)
    }

    @Test
    fun `getPackaging - returns null on HTTP 404`() = runTest {
        coEvery { api.getPackaging(any()) } returns makeErrorResponse(404)

        // 404 перехватывается внутри репозитория и возвращает null, не бросает исключение
        val result = repository.getPackaging("SN-UNKNOWN")

        assertNull(result)
        coVerify(exactly = 1) { api.getPackaging("SN-UNKNOWN") }
    }

    @Test
    fun `getPackaging - rethrows AppError ApiError on non-404 HTTP error`() = runTest {
        coEvery { api.getPackaging(any()) } returns makeErrorResponse(500)

        val ex = catchError<AppError.ApiError> { repository.getPackaging("SN-001") }

        assertNotNull(ex)
        assertEquals(500, ex!!.status)
        assertTrue(fakeLogger.errors.any { it.message.contains("500") })
    }

    @Test
    fun `getPackaging - rethrows AppError ApiError on HTTP 403`() = runTest {
        coEvery { api.getPackaging(any()) } returns makeErrorResponse(403)

        val ex = catchError<AppError.ApiError> { repository.getPackaging("SN-001") }

        assertNotNull(ex)
        assertEquals(403, ex!!.status)
    }

    @Test
    fun `getPackaging - throws AppError NetworkError on IOException`() = runTest {
        coEvery { api.getPackaging(any()) } throws IOException("timeout")

        val ex = catchError<AppError.NetworkError> { repository.getPackaging("SN-001") }

        assertNotNull(ex)
        assertTrue(ex!!.error is IOException)
    }

    // ─── createPackaging ──────────────────────────────────────────────────────

    @Test
    fun `createPackaging - returns created packaging on success`() = runTest {
        coEvery { api.createPackaging(any()) } returns Response.success(packagingDto)

        val result = repository.createPackaging(
            serialNumber = "SN-001",
            productIds = listOf(10, 20),
        )

        assertEquals(1, result.id)
        assertEquals("SN-001", result.serialNumber)
        coVerify(exactly = 1) {
            api.createPackaging(
                PackagingCreateDto(serialNumber = "SN-001", products = listOf(10, 20))
            )
        }
    }

    @Test
    fun `createPackaging - passes correct dto to api`() = runTest {
        coEvery { api.createPackaging(any()) } returns Response.success(packagingDto)

        repository.createPackaging(serialNumber = "SN-002", productIds = listOf(1, 2, 3))

        coVerify(exactly = 1) {
            api.createPackaging(
                PackagingCreateDto(serialNumber = "SN-002", products = listOf(1, 2, 3))
            )
        }
    }

    @Test
    fun `createPackaging - throws IllegalArgumentException when response body is null`() = runTest {
        coEvery { api.createPackaging(any()) } returns Response.success(null)

        val ex = catchError<IllegalArgumentException> {
            repository.createPackaging("SN-001", listOf(10))
        }

        assertNotNull(ex)
    }

    @Test
    fun `createPackaging - throws AppError ApiError on HTTP 409 duplicate serial`() = runTest {
        coEvery { api.createPackaging(any()) } returns makeErrorResponse(
            code = 409,
            body = """{"code":"serial_number_exists","detail":"Serial number already exists"}"""
        )

        val ex = catchError<AppError.ApiError> {
            repository.createPackaging("SN-DUPLICATE", listOf(10))
        }

        assertNotNull(ex)
        assertEquals(409, ex!!.status)
        assertEquals("serial_number_exists", ex.uiCode)
    }

    @Test
    fun `createPackaging - throws AppError NetworkError on IOException`() = runTest {
        coEvery { api.createPackaging(any()) } throws IOException("no network")

        val ex = catchError<AppError.NetworkError> {
            repository.createPackaging("SN-001", listOf(10))
        }

        assertNotNull(ex)
        assertTrue(ex!!.error is IOException)
    }

    // ─── deletePackaging ──────────────────────────────────────────────────────

    @Test
    fun `deletePackaging - completes without error on success`() = runTest {
        coEvery { api.deletePackaging(any()) } returns Response.success(Unit)

        repository.deletePackaging("SN-001")

        coVerify(exactly = 1) { api.deletePackaging("SN-001") }
        assertTrue(fakeLogger.errors.isEmpty())
    }

    @Test
    fun `deletePackaging - throws AppError ApiError on HTTP 404`() = runTest {
        coEvery { api.deletePackaging(any()) } returns makeErrorResponse(404)

        val ex = catchError<AppError.ApiError> { repository.deletePackaging("SN-UNKNOWN") }

        assertNotNull(ex)
        assertEquals(404, ex!!.status)
    }

    @Test
    fun `deletePackaging - throws AppError NetworkError on IOException`() = runTest {
        coEvery { api.deletePackaging(any()) } throws IOException("timeout")

        val ex = catchError<AppError.NetworkError> { repository.deletePackaging("SN-001") }

        assertNotNull(ex)
        assertTrue(ex!!.error is IOException)
    }

    // ─── getPackagingInStorage ────────────────────────────────────────────────

    @Test
    fun `getPackagingInStorage - returns mapped list on success`() = runTest {
        coEvery { api.getPackagingInStorage() } returns Response.success(listOf(packagingDto))

        val result = repository.getPackagingInStorage()

        assertEquals(1, result.size)
        assertEquals("SN-001", result[0].serialNumber)
        coVerify(exactly = 1) { api.getPackagingInStorage() }
        assertTrue(fakeLogger.errors.isEmpty())
    }

    @Test
    fun `getPackagingInStorage - returns empty list when response body is null`() = runTest {
        coEvery { api.getPackagingInStorage() } returns Response.success(null)

        val result = repository.getPackagingInStorage()

        assertTrue(result.isEmpty())
    }

    @Test
    fun `getPackagingInStorage - returns empty list when server returns empty list`() = runTest {
        coEvery { api.getPackagingInStorage() } returns Response.success(emptyList())

        val result = repository.getPackagingInStorage()

        assertTrue(result.isEmpty())
    }

    @Test
    fun `getPackagingInStorage - throws AppError ApiError on HTTP 500`() = runTest {
        coEvery { api.getPackagingInStorage() } returns makeErrorResponse(500)

        val ex = catchError<AppError.ApiError> { repository.getPackagingInStorage() }

        assertNotNull(ex)
        assertEquals(500, ex!!.status)
    }

    @Test
    fun `getPackagingInStorage - throws AppError NetworkError on IOException`() = runTest {
        coEvery { api.getPackagingInStorage() } throws IOException("timeout")

        val ex = catchError<AppError.NetworkError> { repository.getPackagingInStorage() }

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