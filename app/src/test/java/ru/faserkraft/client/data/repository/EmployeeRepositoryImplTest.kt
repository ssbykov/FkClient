package ru.faserkraft.client.data.repository

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Response
import ru.faserkraft.client.data.dto.EmployeeDto
import ru.faserkraft.client.data.dto.QrDataResponseDto
import ru.faserkraft.client.data.dto.UserDto
import ru.faserkraft.client.data.network.Api
import ru.faserkraft.client.error.AppError
import ru.faserkraft.client.util.FakeLogger
import java.io.IOException

class EmployeeRepositoryImplTest {

    private lateinit var api: Api
    private lateinit var fakeLogger: FakeLogger
    private lateinit var repository: EmployeeRepositoryImpl

    // ─── fixtures ─────────────────────────────────────────────────────────────

    private val employeeDto = EmployeeDto(
        id = 1,
        name = "Ivan Petrov",
        user = UserDto(id = 10, email = "ivan@test.com"),
    )

    private val qrDataDto = QrDataResponseDto(
        action = "scan",
        id = 1,
        token = "qr-token-xyz",
    )

    companion object {
        private val testJson = Json {
            encodeDefaults = true
            ignoreUnknownKeys = true
        }
    }

    @Before
    fun setUp() {
        api = mockk()
        fakeLogger = FakeLogger()
        repository = EmployeeRepositoryImpl(api, fakeLogger)
    }

    // ─── getEmployees ─────────────────────────────────────────────────────────

    @Test
    fun `getEmployees - returns mapped list on success`() = runTest {
        coEvery { api.getEmployees() } returns Response.success(listOf(employeeDto))

        val result = repository.getEmployees()

        assertEquals(1, result.size)
        assertEquals(1, result[0].id)
        assertEquals("Ivan Petrov", result[0].name)
        coVerify(exactly = 1) { api.getEmployees() }
        assertTrue(fakeLogger.errors.isEmpty())
    }

    @Test
    fun `getEmployees - returns empty list when response body is null`() = runTest {
        coEvery { api.getEmployees() } returns Response.success(null)

        val result = repository.getEmployees()

        assertTrue(result.isEmpty())
    }

    @Test
    fun `getEmployees - returns empty list when server returns empty list`() = runTest {
        coEvery { api.getEmployees() } returns Response.success(emptyList())

        val result = repository.getEmployees()

        assertTrue(result.isEmpty())
    }

    @Test
    fun `getEmployees - throws AppError ApiError on HTTP 500`() = runTest {
        coEvery { api.getEmployees() } returns makeErrorResponse(500)

        val ex = catchError<AppError.ApiError> { repository.getEmployees() }

        assertNotNull(ex)
        assertEquals(500, ex!!.status)
        assertTrue(fakeLogger.errors.any { it.message.contains("500") })
    }

    @Test
    fun `getEmployees - throws AppError NetworkError on IOException`() = runTest {
        coEvery { api.getEmployees() } throws IOException("timeout")

        val ex = catchError<AppError.NetworkError> { repository.getEmployees() }

        assertNotNull(ex)
        assertTrue(ex!!.error is IOException)
        assertTrue(fakeLogger.errors.any { it.throwable is IOException })
    }

    // ─── getEmployeeQrContent ─────────────────────────────────────────────────

    @Test
    fun `getEmployeeQrContent - returns serialized QR content on success`() = runTest {
        coEvery { api.getQrCode(any()) } returns Response.success(qrDataDto)

        val result = repository.getEmployeeQrContent(employeeId = 1)

        val expected = testJson.encodeToString(qrDataDto)
        assertEquals(expected, result)
        coVerify(exactly = 1) { api.getQrCode(1) }
    }

    @Test
    fun `getEmployeeQrContent - throws IllegalStateException when response body is null`() = runTest {
        coEvery { api.getQrCode(any()) } returns Response.success(null)

        val ex = catchError<IllegalStateException> { repository.getEmployeeQrContent(1) }

        assertNotNull(ex)
        assertEquals("Пустой ответ от сервера", ex!!.message)
    }

    @Test
    fun `getEmployeeQrContent - throws AppError ApiError on HTTP 404`() = runTest {
        coEvery { api.getQrCode(any()) } returns makeErrorResponse(404)

        val ex = catchError<AppError.ApiError> { repository.getEmployeeQrContent(1) }

        assertNotNull(ex)
        assertEquals(404, ex!!.status)
        assertEquals("error_api_404", ex.uiCode)
    }

    @Test
    fun `getEmployeeQrContent - throws AppError NetworkError on IOException`() = runTest {
        coEvery { api.getQrCode(any()) } throws IOException("no network")

        val ex = catchError<AppError.NetworkError> { repository.getEmployeeQrContent(1) }

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