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
import ru.faserkraft.client.data.dto.LoginDto
import ru.faserkraft.client.data.network.AuthApi
import ru.faserkraft.client.domain.model.LoginCredentials
import ru.faserkraft.client.error.AppError
import ru.faserkraft.client.util.FakeLogger
import java.io.IOException
import java.net.SocketTimeoutException

class AuthRepositoryImplTest {

    private lateinit var authApi: AuthApi
    private lateinit var fakeLogger: FakeLogger
    private lateinit var repository: AuthRepositoryImpl

    private val credentials = LoginCredentials(
        username = "testuser",
        password = "testpass"
    )

    @Before
    fun setUp() {
        authApi = mockk()
        fakeLogger = FakeLogger()
        repository = AuthRepositoryImpl(authApi, fakeLogger)
    }

    // ─── success ───────────────────────────────────────────────────────────────

    @Test
    fun `login - returns access token on success`() = runTest {
        coEvery { authApi.login(any()) } returns Response.success(
            LoginDto(accessToken = "test_token_123", tokenType = "bearer")
        )

        val result = repository.login(credentials)

        assertEquals("test_token_123", result)
        coVerify(exactly = 1) { authApi.login(any()) }
        assertTrue(fakeLogger.errors.isEmpty())
    }

    // ─── HTTP errors → AppError.ApiError ──────────────────────────────────────

    @Test
    fun `login - throws AppError ApiError on HTTP 401`() = runTest {
        coEvery { authApi.login(any()) } returns makeErrorResponse(
            code = 401,
            body = """{"code":"unauthorized","detail":"Invalid credentials"}"""
        )

        val ex = catchError<AppError.ApiError> { repository.login(credentials) }

        assertNotNull(ex)
        assertEquals(401, ex!!.status)
        assertEquals("unauthorized", ex.uiCode)
        assertEquals("Invalid credentials", ex.message)
        assertTrue(fakeLogger.errors.any { it.message.contains("401") })
    }

    @Test
    fun `login - throws AppError ApiError on HTTP 422 with array detail`() = runTest {
        coEvery { authApi.login(any()) } returns makeErrorResponse(
            code = 422,
            body = """{"detail":[{"msg":"field required"}]}"""
        )

        val ex = catchError<AppError.ApiError> { repository.login(credentials) }

        assertNotNull(ex)
        assertEquals(422, ex!!.status)
        assertEquals("error_api_422", ex.uiCode)  // code отсутствует → fallback
        assertEquals("field required", ex.message)
    }

    @Test
    fun `login - throws AppError ApiError on HTTP 500`() = runTest {
        coEvery { authApi.login(any()) } returns makeErrorResponse(
            code = 500,
            body = """{"detail":"Internal Server Error"}"""
        )

        val ex = catchError<AppError.ApiError> { repository.login(credentials) }

        assertNotNull(ex)
        assertEquals(500, ex!!.status)
        assertEquals("error_api_500", ex.uiCode)
    }

    @Test
    fun `login - uses fallback uiCode when error body is empty`() = runTest {
        coEvery { authApi.login(any()) } returns makeErrorResponse(code = 403, body = "")

        val ex = catchError<AppError.ApiError> { repository.login(credentials) }

        assertNotNull(ex)
        assertEquals(403, ex!!.status)
        assertEquals("error_api_403", ex.uiCode)
    }

    // ─── network failures → AppError.NetworkError ─────────────────────────────

    @Test
    fun `login - throws AppError NetworkError on IOException`() = runTest {
        coEvery { authApi.login(any()) } throws IOException("Connection refused")

        val ex = catchError<AppError.NetworkError> { repository.login(credentials) }

        assertNotNull(ex)
        assertTrue(ex!!.error is IOException)
        assertEquals("Connection refused", ex.error?.message)
        assertTrue(fakeLogger.errors.any { it.throwable is IOException })
    }

    @Test
    fun `login - throws AppError NetworkError on SocketTimeoutException`() = runTest {
        coEvery { authApi.login(any()) } throws SocketTimeoutException("timeout")

        val ex = catchError<AppError.NetworkError> { repository.login(credentials) }

        assertNotNull(ex)
        assertTrue(ex!!.error is SocketTimeoutException)
    }

    // ─── unexpected exception → AppError.UnknownError ─────────────────────────

    @Test
    fun `login - throws AppError UnknownError on unexpected exception`() = runTest {
        coEvery { authApi.login(any()) } throws RuntimeException("Unexpected crash")

        val ex = catchError<AppError.UnknownError> { repository.login(credentials) }

        assertNotNull(ex)
        assertTrue(ex!!.error is RuntimeException)
        assertEquals("Unexpected crash", ex.error?.message)
        assertTrue(fakeLogger.errors.any { it.throwable is RuntimeException })
    }

    // ─── null body on 200 ─────────────────────────────────────────────────────

    @Test
    fun `login - throws IllegalArgumentException when response body is null`() = runTest {
        coEvery { authApi.login(any()) } returns Response.success(null)

        val ex = catchError<IllegalArgumentException> { repository.login(credentials) }

        assertNotNull(ex)
    }

    // ─── helpers ───────────────────────────────────────────────────────────────

    private fun makeErrorResponse(
        code: Int,
        body: String,
        contentType: String = "application/json"
    ): Response<LoginDto> =
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