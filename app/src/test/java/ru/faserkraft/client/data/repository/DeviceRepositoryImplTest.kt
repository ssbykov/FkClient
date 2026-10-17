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
import ru.faserkraft.client.data.dto.DeviceResponseDto
import ru.faserkraft.client.data.dto.InvitationRequestDto
import ru.faserkraft.client.data.dto.QrDataResponseDto
import ru.faserkraft.client.data.network.Api
import ru.faserkraft.client.data.network.AuthApi
import ru.faserkraft.client.domain.model.DeviceRequest
import ru.faserkraft.client.error.AppError
import ru.faserkraft.client.util.FakeLogger
import java.io.IOException
import java.net.SocketTimeoutException

class DeviceRepositoryImplTest {

    private lateinit var api: Api
    private lateinit var authApi: AuthApi
    private lateinit var fakeLogger: FakeLogger
    private lateinit var repository: DeviceRepositoryImpl

    private val request = DeviceRequest(
        invitationToken = "invitation-token-xyz",
        deviceId = "device-001",
        model = "Samsung Galaxy A53",
        manufacturer = "Samsung",
    )

    private val responseDto = DeviceResponseDto(
        accessToken = "mobile-session-token",
        expiresAt = "2026-11-16T10:00:00Z",
        userName = "Иван Петров",
        userEmail = "ivan@faserkraft.ru",
        userRole = "worker",
        deviceId = "device-001",
        model = "Samsung Galaxy A53",
        manufacturer = "Samsung",
    )

    @Before
    fun setUp() {
        api = mockk()
        authApi = mockk()
        fakeLogger = FakeLogger()
        repository = DeviceRepositoryImpl(api, authApi, fakeLogger)
    }

    // ─── registerDevice: success ──────────────────────────────────────────────

    @Test
    fun `registerDevice - returns session data on success`() = runTest {
        coEvery { authApi.registerDevice(any()) } returns Response.success(responseDto)

        val result = repository.registerDevice(request)

        assertEquals("mobile-session-token", result.accessToken)
        assertEquals("2026-11-16T10:00:00Z", result.expiresAt)
        assertEquals("ivan@faserkraft.ru", result.userEmail)
        assertEquals("Иван Петров", result.userName)
        assertEquals("worker", result.userRole)
        coVerify(exactly = 1) { authApi.registerDevice(any()) }
        assertTrue(fakeLogger.errors.isEmpty())
    }

    @Test
    fun `registerDevice - sends invitation token and device info to server`() = runTest {
        coEvery { authApi.registerDevice(any()) } returns Response.success(responseDto)

        repository.registerDevice(request)

        coVerify {
            authApi.registerDevice(
                match {
                    it.invitationToken == "invitation-token-xyz" &&
                            it.deviceId == "device-001" &&
                            it.model == "Samsung Galaxy A53" &&
                            it.manufacturer == "Samsung"
                }
            )
        }
    }

    // ─── registerDevice: HTTP errors → AppError.ApiError ─────────────────────

    @Test
    fun `registerDevice - throws ApiError on HTTP 400 invalid invitation`() = runTest {
        coEvery { authApi.registerDevice(any()) } returns makeErrorResponse(
            code = 400,
            body = """{"detail":"INVALID_DEVICE_INVITATION"}"""
        )

        val ex = catchError<AppError.ApiError> { repository.registerDevice(request) }

        assertNotNull(ex)
        assertEquals(400, ex!!.status)
        assertEquals("INVALID_DEVICE_INVITATION", ex.message)
        assertTrue(fakeLogger.errors.any { it.message.contains("400") })
    }

    @Test
    fun `registerDevice - throws ApiError on HTTP 409 device already registered`() = runTest {
        coEvery { authApi.registerDevice(any()) } returns makeErrorResponse(
            code = 409,
            body = """{"detail":"DEVICE_ALREADY_REGISTERED"}"""
        )

        val ex = catchError<AppError.ApiError> { repository.registerDevice(request) }

        assertNotNull(ex)
        assertEquals(409, ex!!.status)
        assertEquals("DEVICE_ALREADY_REGISTERED", ex.message)
    }

    @Test
    fun `registerDevice - throws ApiError on HTTP 403 user not allowed`() = runTest {
        coEvery { authApi.registerDevice(any()) } returns makeErrorResponse(
            code = 403,
            body = """{"detail":"USER_NOT_ALLOWED"}"""
        )

        val ex = catchError<AppError.ApiError> { repository.registerDevice(request) }

        assertNotNull(ex)
        assertEquals(403, ex!!.status)
        assertEquals("USER_NOT_ALLOWED", ex.message)
    }

    @Test
    fun `registerDevice - throws ApiError on HTTP 422 with array detail`() = runTest {
        coEvery { authApi.registerDevice(any()) } returns makeErrorResponse(
            code = 422,
            body = """{"detail":[{"msg":"field required"}]}"""
        )

        val ex = catchError<AppError.ApiError> { repository.registerDevice(request) }

        assertNotNull(ex)
        assertEquals(422, ex!!.status)
        assertEquals("error_api_422", ex.uiCode)
        assertEquals("field required", ex.message)
    }

    @Test
    fun `registerDevice - throws ApiError on HTTP 500`() = runTest {
        coEvery { authApi.registerDevice(any()) } returns makeErrorResponse(
            code = 500,
            body = """{"detail":"Internal Server Error"}"""
        )

        val ex = catchError<AppError.ApiError> { repository.registerDevice(request) }

        assertNotNull(ex)
        assertEquals(500, ex!!.status)
        assertEquals("error_api_500", ex.uiCode)
    }

    @Test
    fun `registerDevice - uses fallback uiCode when error body is empty`() = runTest {
        coEvery { authApi.registerDevice(any()) } returns makeErrorResponse(code = 403, body = "")

        val ex = catchError<AppError.ApiError> { repository.registerDevice(request) }

        assertNotNull(ex)
        assertEquals(403, ex!!.status)
        assertEquals("error_api_403", ex.uiCode)
    }

    // ─── registerDevice: network / unknown ────────────────────────────────────

    @Test
    fun `registerDevice - throws NetworkError on IOException`() = runTest {
        coEvery { authApi.registerDevice(any()) } throws IOException("Connection refused")

        val ex = catchError<AppError.NetworkError> { repository.registerDevice(request) }

        assertNotNull(ex)
        assertTrue(ex!!.error is IOException)
        assertEquals("Connection refused", ex.error?.message)
        assertTrue(fakeLogger.errors.any { it.throwable is IOException })
    }

    @Test
    fun `registerDevice - throws NetworkError on SocketTimeoutException`() = runTest {
        coEvery { authApi.registerDevice(any()) } throws SocketTimeoutException("timeout")

        val ex = catchError<AppError.NetworkError> { repository.registerDevice(request) }

        assertNotNull(ex)
        assertTrue(ex!!.error is SocketTimeoutException)
    }

    @Test
    fun `registerDevice - throws UnknownError on unexpected exception`() = runTest {
        coEvery { authApi.registerDevice(any()) } throws RuntimeException("Unexpected crash")

        val ex = catchError<AppError.UnknownError> { repository.registerDevice(request) }

        assertNotNull(ex)
        assertTrue(ex!!.error is RuntimeException)
        assertEquals("Unexpected crash", ex.error?.message)
    }

    @Test
    fun `registerDevice - throws IllegalArgumentException when response body is null`() = runTest {
        coEvery { authApi.registerDevice(any()) } returns Response.success(null)

        val ex = catchError<IllegalArgumentException> { repository.registerDevice(request) }

        assertNotNull(ex)
    }

    // ─── getQrCode ────────────────────────────────────────────────────────────

    @Test
    fun `getQrCode - returns qr payload from invitation response`() = runTest {
        coEvery { api.getQrCode(any()) } returns Response.success(
            QrDataResponseDto(
                qrPayload = """{"employee_id":5,"invitation_token":"abc"}""",
                expiresAt = "2026-10-17T10:10:00Z",
            )
        )

        val result = repository.getQrCode(5)

        assertEquals("""{"employee_id":5,"invitation_token":"abc"}""", result)
        coVerify(exactly = 1) { api.getQrCode(InvitationRequestDto(5)) }
    }

    // ─── helpers ──────────────────────────────────────────────────────────────

    private fun makeErrorResponse(
        code: Int,
        body: String,
        contentType: String = "application/json"
    ): Response<DeviceResponseDto> =
        Response.error(code, body.toResponseBody(contentType.toMediaType()))

    private suspend inline fun <reified T : Throwable> catchError(
        crossinline block: suspend () -> Unit
    ): T? = try {
        block()
        null
    } catch (e: Throwable) {
        e as? T
    }
}