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
import ru.faserkraft.client.data.dto.DeviceRequestDto
import ru.faserkraft.client.data.dto.DeviceResponseDto
import ru.faserkraft.client.data.dto.QrDataResponseDto
import ru.faserkraft.client.data.network.Api
import ru.faserkraft.client.data.network.AuthApi
import ru.faserkraft.client.domain.model.DeviceRequest
import ru.faserkraft.client.error.AppError
import ru.faserkraft.client.util.FakeLogger
import java.io.IOException

class DeviceRepositoryImplTest {

    private lateinit var api: Api
    private lateinit var authApi: AuthApi
    private lateinit var fakeLogger: FakeLogger
    private lateinit var repository: DeviceRepositoryImpl

    companion object {
        private val testJson = Json {
            encodeDefaults = true
            ignoreUnknownKeys = true
        }
    }

    // ─── fixtures ─────────────────────────────────────────────────────────────

    private val deviceRequest = DeviceRequest(
        deviceId = "device-uuid-123",
        model = "Pixel 7",
        manufacturer = "Google",
        token = "fcm-token-abc",
        password = "pass1234",
        userId = 42,
    )

    private val deviceResponseDto = DeviceResponseDto(
        userName = "Ivan Petrov",
        userEmail = "ivan@test.com",
        userRole = "worker",
        deviceId = "device-uuid-123",
        model = "Pixel 7",
        manufacturer = "Google",
    )

    private val qrDataDto = QrDataResponseDto(
        action = "scan",
        id = 7,
        token = "qr-token-xyz",
    )

    @Before
    fun setUp() {
        api = mockk()
        authApi = mockk()
        fakeLogger = FakeLogger()
        repository = DeviceRepositoryImpl(api, authApi, fakeLogger)
    }

    // ─── registerDevice ───────────────────────────────────────────────────────

    @Test
    fun `registerDevice - returns UserRegistration on success`() = runTest {
        coEvery { authApi.registerDevice(any()) } returns Response.success(deviceResponseDto)

        val result = repository.registerDevice(deviceRequest)

        assertEquals("ivan@test.com", result.userEmail)
        assertEquals("Ivan Petrov", result.userName)
        assertEquals("worker", result.userRole)
        assertEquals("pass1234", result.password)
        assertTrue(fakeLogger.errors.isEmpty())
    }

    @Test
    fun `registerDevice - passes correct dto to authApi`() = runTest {
        coEvery { authApi.registerDevice(any()) } returns Response.success(deviceResponseDto)

        repository.registerDevice(deviceRequest)

        coVerify(exactly = 1) {
            authApi.registerDevice(
                DeviceRequestDto(
                    deviceId = "device-uuid-123",
                    model = "Pixel 7",
                    manufacturer = "Google",
                    token = "fcm-token-abc",
                    password = "pass1234",
                    userId = 42,
                )
            )
        }
    }

    @Test
    fun `registerDevice - throws IllegalArgumentException when response body is null`() = runTest {
        coEvery { authApi.registerDevice(any()) } returns Response.success(null)

        val ex = catchError<IllegalArgumentException> { repository.registerDevice(deviceRequest) }

        assertNotNull(ex)
    }

    @Test
    fun `registerDevice - throws AppError ApiError on HTTP 401`() = runTest {
        coEvery { authApi.registerDevice(any()) } returns makeErrorResponse(401)

        val ex = catchError<AppError.ApiError> { repository.registerDevice(deviceRequest) }

        assertNotNull(ex)
        assertEquals(401, ex!!.status)
        assertTrue(fakeLogger.errors.any { it.message.contains("401") })
    }

    @Test
    fun `registerDevice - throws AppError ApiError on HTTP 409 conflict`() = runTest {
        coEvery { authApi.registerDevice(any()) } returns makeErrorResponse(
            code = 409,
            body = """{"code":"device_already_exists","detail":"Device already registered"}"""
        )

        val ex = catchError<AppError.ApiError> { repository.registerDevice(deviceRequest) }

        assertNotNull(ex)
        assertEquals(409, ex!!.status)
        assertEquals("device_already_exists", ex.uiCode)
        assertEquals("Device already registered", ex.message)
    }

    @Test
    fun `registerDevice - throws AppError NetworkError on IOException`() = runTest {
        coEvery { authApi.registerDevice(any()) } throws IOException("no network")

        val ex = catchError<AppError.NetworkError> { repository.registerDevice(deviceRequest) }

        assertNotNull(ex)
        assertTrue(ex!!.error is IOException)
        assertTrue(fakeLogger.errors.any { it.throwable is IOException })
    }

    // ─── getQrCode ────────────────────────────────────────────────────────────

    @Test
    fun `getQrCode - returns serialized QR content on success`() = runTest {
        coEvery { api.getQrCode(any()) } returns Response.success(qrDataDto)

        val result = repository.getQrCode(employeeId = 7)

        val expected =testJson.encodeToString(qrDataDto)
        assertEquals(expected, result)
        coVerify(exactly = 1) { api.getQrCode(7) }
        assertTrue(fakeLogger.errors.isEmpty())
    }

    @Test
    fun `getQrCode - throws IllegalArgumentException when response body is null`() = runTest {
        coEvery { api.getQrCode(any()) } returns Response.success(null)

        val ex = catchError<IllegalArgumentException> { repository.getQrCode(7) }

        assertNotNull(ex)
    }

    @Test
    fun `getQrCode - throws AppError ApiError on HTTP 404`() = runTest {
        coEvery { api.getQrCode(any()) } returns makeErrorResponse(404)

        val ex = catchError<AppError.ApiError> { repository.getQrCode(7) }

        assertNotNull(ex)
        assertEquals(404, ex!!.status)
        assertEquals("error_api_404", ex.uiCode)
    }

    @Test
    fun `getQrCode - throws AppError NetworkError on IOException`() = runTest {
        coEvery { api.getQrCode(any()) } throws IOException("timeout")

        val ex = catchError<AppError.NetworkError> { repository.getQrCode(7) }

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