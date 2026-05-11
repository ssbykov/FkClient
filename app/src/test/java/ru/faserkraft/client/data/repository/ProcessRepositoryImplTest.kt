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
import ru.faserkraft.client.data.dto.ProcessDto
import ru.faserkraft.client.data.network.Api
import ru.faserkraft.client.error.AppError
import ru.faserkraft.client.util.FakeLogger
import java.io.IOException

class ProcessRepositoryImplTest {

    private lateinit var api: Api
    private lateinit var fakeLogger: FakeLogger
    private lateinit var repository: ProcessRepositoryImpl

    // ─── fixtures ─────────────────────────────────────────────────────────────

    private val processDto1 = ProcessDto(id = 1, name = "Cutting", description = "Cut fabric")
    private val processDto2 = ProcessDto(id = 2, name = "Sewing", description = "Sew parts")

    @Before
    fun setUp() {
        api = mockk()
        fakeLogger = FakeLogger()
        repository = ProcessRepositoryImpl(api, fakeLogger)
    }

    // ─── getProcesses ─────────────────────────────────────────────────────────

    @Test
    fun `getProcesses - returns mapped list on success`() = runTest {
        coEvery { api.getProcesses() } returns Response.success(listOf(processDto1, processDto2))

        val result = repository.getProcesses()

        assertEquals(2, result.size)
        assertEquals(1, result[0].id)
        assertEquals("Cutting", result[0].name)
        assertEquals(2, result[1].id)
        assertEquals("Sewing", result[1].name)
        coVerify(exactly = 1) { api.getProcesses() }
        assertTrue(fakeLogger.errors.isEmpty())
    }

    @Test
    fun `getProcesses - returns empty list when response body is null`() = runTest {
        coEvery { api.getProcesses() } returns Response.success(null)

        val result = repository.getProcesses()

        assertTrue(result.isEmpty())
    }

    @Test
    fun `getProcesses - returns empty list when server returns empty list`() = runTest {
        coEvery { api.getProcesses() } returns Response.success(emptyList())

        val result = repository.getProcesses()

        assertTrue(result.isEmpty())
    }

    @Test
    fun `getProcesses - throws AppError ApiError on HTTP 401`() = runTest {
        coEvery { api.getProcesses() } returns makeErrorResponse(401)

        val ex = catchError<AppError.ApiError> { repository.getProcesses() }

        assertNotNull(ex)
        assertEquals(401, ex!!.status)
        assertTrue(fakeLogger.errors.any { it.message.contains("401") })
    }

    @Test
    fun `getProcesses - throws AppError ApiError on HTTP 500`() = runTest {
        coEvery { api.getProcesses() } returns makeErrorResponse(500)

        val ex = catchError<AppError.ApiError> { repository.getProcesses() }

        assertNotNull(ex)
        assertEquals(500, ex!!.status)
    }

    @Test
    fun `getProcesses - throws AppError NetworkError on IOException`() = runTest {
        coEvery { api.getProcesses() } throws IOException("timeout")

        val ex = catchError<AppError.NetworkError> { repository.getProcesses() }

        assertNotNull(ex)
        assertTrue(ex!!.error is IOException)
        assertTrue(fakeLogger.errors.any { it.throwable is IOException })
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