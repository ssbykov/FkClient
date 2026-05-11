package ru.faserkraft.client.data.repository

import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import retrofit2.Response
import ru.faserkraft.client.data.dto.ProcessDto
import ru.faserkraft.client.data.dto.ProductDto
import ru.faserkraft.client.data.dto.ProductStatusDto
import ru.faserkraft.client.data.network.Api
import ru.faserkraft.client.domain.model.ProductStatus
import ru.faserkraft.client.error.AppError
import ru.faserkraft.client.utils.Logger

class StepRepositoryImplTest {

    private val mockApi: Api = mockk()
    private val mockLogger: Logger = mockk(relaxed = true)

    private lateinit var repository: StepRepositoryImpl

    private val productDto = ProductDto(
        id = 42L,
        serialNumber = "SN-001",
        process = ProcessDto(id = 1, name = "Process A"),
        createdAt = "2024-06-15T10:30:00Z",
        packaging = null,
        status = ProductStatusDto.NORMAL,
        steps = emptyList()
    )

    @Before
    fun setUp() {
        repository = StepRepositoryImpl(
            api = mockApi,
            logger = mockLogger,
        )
    }

    // ==========================================
    // closeStep
    // ==========================================

    @Test
    fun `closeStep returns mapped domain product`() = runTest {
        coEvery { mockApi.postStep(10) } returns Response.success(productDto)

        val result = repository.closeStep(stepId = 10)

        assertEquals(42L, result.id)
        assertEquals("SN-001", result.serialNumber)
        assertEquals(ProductStatus.NORMAL, result.status)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `closeStep throws when api returns null body`() = runTest {
        coEvery { mockApi.postStep(any()) } returns Response.success(null)

        repository.closeStep(stepId = 10)
    }

    @Test(expected = AppError.ApiError::class)
    fun `closeStep throws ApiError on http error`() = runTest {
        coEvery { mockApi.postStep(any()) } returns
                Response.error(422, "".toResponseBody())

        repository.closeStep(stepId = 10)
    }

    @Test(expected = AppError.NetworkError::class)
    fun `closeStep throws NetworkError on IOException`() = runTest {
        coEvery { mockApi.postStep(any()) } throws AppError.NetworkError()

        repository.closeStep(stepId = 10)
    }

    // ==========================================
    // changeStepPerformer
    // ==========================================

    @Test
    fun `changeStepPerformer returns mapped domain product`() = runTest {
        coEvery { mockApi.changeStepPerformer(10, 5) } returns Response.success(productDto)

        val result = repository.changeStepPerformer(stepId = 10, newEmployeeId = 5)

        assertEquals(42L, result.id)
        assertEquals("SN-001", result.serialNumber)
    }

    @Test
    fun `changeStepPerformer passes correct stepId and employeeId to api`() = runTest {
        coEvery { mockApi.changeStepPerformer(10, 5) } returns Response.success(productDto)

        repository.changeStepPerformer(stepId = 10, newEmployeeId = 5)

        // если mockk не выбросил исключение — значит вызов с точными аргументами прошёл
        // (strict matching по умолчанию в coEvery)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `changeStepPerformer throws when api returns null body`() = runTest {
        coEvery { mockApi.changeStepPerformer(any(), any()) } returns Response.success(null)

        repository.changeStepPerformer(stepId = 10, newEmployeeId = 5)
    }

    @Test(expected = AppError.ApiError::class)
    fun `changeStepPerformer throws ApiError on http error`() = runTest {
        coEvery { mockApi.changeStepPerformer(any(), any()) } returns
                Response.error(404, "".toResponseBody())

        repository.changeStepPerformer(stepId = 10, newEmployeeId = 5)
    }

    @Test(expected = AppError.NetworkError::class)
    fun `changeStepPerformer throws NetworkError on IOException`() = runTest {
        coEvery { mockApi.changeStepPerformer(any(), any()) } throws AppError.NetworkError()

        repository.changeStepPerformer(stepId = 10, newEmployeeId = 5)
    }
}