package ru.faserkraft.client.data.repository

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Response
import ru.faserkraft.client.data.dto.FinishedProcessDto
import ru.faserkraft.client.data.dto.PeriodStatisticsDto
import ru.faserkraft.client.data.dto.ProcessCountStatDto
import ru.faserkraft.client.data.dto.ProcessDto
import ru.faserkraft.client.data.dto.ProductCreateDto
import ru.faserkraft.client.data.dto.ProductDto
import ru.faserkraft.client.data.dto.ProductShortDto
import ru.faserkraft.client.data.dto.ProductStatusDto
import ru.faserkraft.client.data.dto.ProductsInventoryDto
import ru.faserkraft.client.data.dto.StepCountStatDto
import ru.faserkraft.client.data.network.Api
import ru.faserkraft.client.domain.model.Product
import ru.faserkraft.client.domain.model.ProductStatus
import ru.faserkraft.client.error.AppError
import ru.faserkraft.client.utils.logger.Logger
import ru.faserkraft.client.utils.timeprovider.TimeProvider

class ProductRepositoryImplTest {

    private val mockApi: Api = mockk()
    private val mockLogger: Logger = mockk(relaxed = true)

    private val fakeTimestamp = "2024-06-15T10:30:00Z"
    private val fakeTimeProvider = object : TimeProvider {
        override fun nowIsoUtc(): String = fakeTimestamp
    }

    private lateinit var repository: ProductRepositoryImpl

    // ---------- фикстуры ----------

    private val processDto = ProcessDto(id = 1, name = "Process A")

    private val productDto = ProductDto(
        id = 42L,
        serialNumber = "SN-001",
        process = processDto,
        createdAt = fakeTimestamp,
        packaging = null,
        status = ProductStatusDto.NORMAL,
        steps = emptyList()
    )

    @Before
    fun setUp() {
        repository = ProductRepositoryImpl(
            api = mockApi,
            timeProvider = fakeTimeProvider,
            logger = mockLogger,
        )
    }

    // ==========================================
    // getProduct
    // ==========================================

    @Test
    fun `getProduct returns domain model when api returns dto`() = runTest {
        coEvery { mockApi.getProduct("SN-001") } returns Response.success(productDto)

        val result = repository.getProduct("SN-001")

        assertEquals(42L, result?.id)
        assertEquals("SN-001", result?.serialNumber)
        assertEquals(ProductStatus.NORMAL, result?.status)
        assertNull(result?.packagingSerialNumber)
    }

    @Test
    fun `getProduct returns null when api returns 404`() = runTest {
        coEvery { mockApi.getProduct("UNKNOWN") } returns
                Response.error(404, "".toResponseBody())

        val result = repository.getProduct("UNKNOWN")

        assertNull(result)
    }

    @Test(expected = AppError.ApiError::class)
    fun `getProduct rethrows ApiError when status is not 404`() = runTest {
        coEvery { mockApi.getProduct("SN-ERR") } returns
                Response.error(500, "".toResponseBody())

        repository.getProduct("SN-ERR")
    }

    @Test
    fun `getProduct returns null when api body is null`() = runTest {
        coEvery { mockApi.getProduct("SN-NULL") } returns Response.success(null)

        val result = repository.getProduct("SN-NULL")

        assertNull(result)
    }

    @Test(expected = AppError.NetworkError::class)
    fun `getProduct throws NetworkError on IOException`() = runTest {
        coEvery { mockApi.getProduct(any()) } throws AppError.NetworkError()

        repository.getProduct("SN-001")
    }

    // ==========================================
    // createProduct
    // ==========================================

    @Test
    fun `createProduct sends correct dto with fakeTimestamp`() = runTest {
        coEvery { mockApi.postProduct(any()) } returns Response.success(productDto)

        repository.createProduct("SN-001", processId = 1)

        coVerify {
            mockApi.postProduct(
                ProductCreateDto(
                    processId = 1,
                    serialNumber = "SN-001",
                    createdAt = fakeTimestamp
                )
            )
        }
    }

    @Test
    fun `createProduct returns mapped domain product`() = runTest {
        coEvery { mockApi.postProduct(any()) } returns Response.success(productDto)

        val result = repository.createProduct("SN-001", processId = 1)

        assertEquals(42L, result.id)
        assertEquals("SN-001", result.serialNumber)
        assertEquals(fakeTimestamp, result.createdAt)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `createProduct throws when api returns null body`() = runTest {
        coEvery { mockApi.postProduct(any()) } returns Response.success(null)

        repository.createProduct("SN-001", processId = 1)
    }

    @Test(expected = AppError.NetworkError::class)
    fun `createProduct throws NetworkError on IOException`() = runTest {
        coEvery { mockApi.postProduct(any()) } throws AppError.NetworkError()

        repository.createProduct("SN-001", processId = 1)
    }

    // ==========================================
// getProductsByStatus
// ==========================================

    @Test
    fun `getProductsByStatus - calls getProductsNotNormal, not a status-specific endpoint`() = runTest {
        coEvery { mockApi.getProductsNotNormal() } returns Response.success(emptyList())

        repository.getProductsByStatus(listOf(ProductStatus.REWORK))

        coVerify(exactly = 1) { mockApi.getProductsNotNormal() }
    }

    @Test
    fun `getProductsByStatus - returns only products matching requested statuses`() = runTest {
        val reworkDto = productDto.copy(id = 2L, status = ProductStatusDto.REWORK)
        val scrapDto  = productDto.copy(id = 3L, status = ProductStatusDto.SCRAP)
        coEvery { mockApi.getProductsNotNormal() } returns
                Response.success(listOf(reworkDto, scrapDto))

        val result = repository.getProductsByStatus(listOf(ProductStatus.REWORK))

        assertEquals(1, result.size)
        assertEquals(2L, result[0].id)
        assertEquals(ProductStatus.REWORK, result[0].status)
    }

    @Test
    fun `getProductsByStatus - returns products for multiple requested statuses`() = runTest {
        val reworkDto = productDto.copy(id = 2L, status = ProductStatusDto.REWORK)
        val scrapDto  = productDto.copy(id = 3L, status = ProductStatusDto.SCRAP)
        coEvery { mockApi.getProductsNotNormal() } returns
                Response.success(listOf(reworkDto, scrapDto))

        val result = repository.getProductsByStatus(
            listOf(ProductStatus.REWORK, ProductStatus.SCRAP)
        )

        assertEquals(2, result.size)
        assertTrue(result.any { it.status == ProductStatus.REWORK })
        assertTrue(result.any { it.status == ProductStatus.SCRAP })
    }

    @Test
    fun `getProductsByStatus - excludes products whose status is not in requested list`() = runTest {
        val reworkDto = productDto.copy(id = 2L, status = ProductStatusDto.REWORK)
        val scrapDto  = productDto.copy(id = 3L, status = ProductStatusDto.SCRAP)
        coEvery { mockApi.getProductsNotNormal() } returns
                Response.success(listOf(reworkDto, scrapDto))

        val result = repository.getProductsByStatus(listOf(ProductStatus.SCRAP))

        assertEquals(1, result.size)
        assertEquals(ProductStatus.SCRAP, result[0].status)
        assertTrue(result.none { it.status == ProductStatus.REWORK })
    }

    @Test
    fun `getProductsByStatus - returns empty list when api body is null`() = runTest {
        coEvery { mockApi.getProductsNotNormal() } returns Response.success(null)

        val result = repository.getProductsByStatus(listOf(ProductStatus.REWORK))

        assertEquals(emptyList<Product>(), result)
    }

    @Test
    fun `getProductsByStatus - returns empty list when api returns empty list`() = runTest {
        coEvery { mockApi.getProductsNotNormal() } returns Response.success(emptyList())

        val result = repository.getProductsByStatus(listOf(ProductStatus.REWORK))

        assertEquals(emptyList<Product>(), result)
    }

    @Test
    fun `getProductsByStatus - returns empty list when no products match requested statuses`() = runTest {
        val reworkDto = productDto.copy(id = 2L, status = ProductStatusDto.REWORK)
        coEvery { mockApi.getProductsNotNormal() } returns Response.success(listOf(reworkDto))

        val result = repository.getProductsByStatus(listOf(ProductStatus.SCRAP))

        assertEquals(emptyList<Product>(), result)
    }

    @Test
    fun `getProductsByStatus - returns empty list for empty statuses filter`() = runTest {
        val reworkDto = productDto.copy(id = 2L, status = ProductStatusDto.REWORK)
        coEvery { mockApi.getProductsNotNormal() } returns Response.success(listOf(reworkDto))

        val result = repository.getProductsByStatus(emptyList())

        assertEquals(emptyList<Product>(), result)
    }

    @Test(expected = AppError.ApiError::class)
    fun `getProductsByStatus - throws ApiError on http error`() = runTest {
        coEvery { mockApi.getProductsNotNormal() } returns
                Response.error(500, "".toResponseBody())

        repository.getProductsByStatus(listOf(ProductStatus.REWORK))
    }

    @Test(expected = AppError.NetworkError::class)
    fun `getProductsByStatus - throws NetworkError on IOException`() = runTest {
        coEvery { mockApi.getProductsNotNormal() } throws AppError.NetworkError()

        repository.getProductsByStatus(listOf(ProductStatus.REWORK))
    }

    // ==========================================
    // changeStatus
    // ==========================================

    @Test
    fun `changeStatus returns product with updated status`() = runTest {
        val reworkDto = productDto.copy(status = ProductStatusDto.REWORK)
        coEvery { mockApi.changeProductStatus(42L, any()) } returns Response.success(reworkDto)

        val result = repository.changeStatus(42L, ProductStatus.REWORK)

        assertEquals(ProductStatus.REWORK, result.status)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `changeStatus throws when api returns null body`() = runTest {
        coEvery { mockApi.changeProductStatus(any(), any()) } returns Response.success(null)

        repository.changeStatus(42L, ProductStatus.SCRAP)
    }

    @Test(expected = AppError.ApiError::class)
    fun `changeStatus throws ApiError on http error`() = runTest {
        coEvery { mockApi.changeProductStatus(any(), any()) } returns
                Response.error(422, "".toResponseBody())

        repository.changeStatus(42L, ProductStatus.SCRAP)
    }

    // ==========================================
    // changeProcess
    // ==========================================

    @Test
    fun `changeProcess returns product with new process`() = runTest {
        val newProcessDto = ProcessDto(id = 2, name = "Process B")
        val updatedDto = productDto.copy(process = newProcessDto)
        coEvery { mockApi.changeProductProcess(42L, 2) } returns Response.success(updatedDto)

        val result = repository.changeProcess(42L, newProcessId = 2)

        assertEquals(2, result.process.id)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `changeProcess throws when api returns null body`() = runTest {
        coEvery { mockApi.changeProductProcess(any(), any()) } returns Response.success(null)

        repository.changeProcess(42L, newProcessId = 2)
    }

    // ==========================================
    // getProductsInventory
    // ==========================================

    @Test
    fun `getProductsInventory returns mapped list`() = runTest {
        val inventoryDto = ProductsInventoryDto(
            processId = 1,
            processName = "Process A",
            stepDefinitionId = 10,
            stepName = "Cutting",
            stepNameGenitive = "Cutting gen",
            count = 5
        )
        coEvery { mockApi.getProductsInventory() } returns Response.success(listOf(inventoryDto))

        val result = repository.getProductsInventory()

        assertEquals(1, result.size)
        assertEquals(5, result[0].count)
        assertEquals("Cutting", result[0].stepName)
    }

    @Test
    fun `getProductsInventory returns empty list when api body is null`() = runTest {
        coEvery { mockApi.getProductsInventory() } returns Response.success(null)

        val result = repository.getProductsInventory()

        assertEquals(emptyList<Any>(), result)
    }

    @Test
    fun `getProductsInventory returns empty list when api returns empty list`() = runTest {
        coEvery { mockApi.getProductsInventory() } returns Response.success(emptyList())

        val result = repository.getProductsInventory()

        assertEquals(emptyList<Any>(), result)
    }

    // ==========================================
    // getFinishedProducts
    // ==========================================

    @Test
    fun `getFinishedProducts returns mapped list`() = runTest {
        val productShortDto = ProductShortDto(   // ← FinishedProductDto → ProductShortDto
            id = 7,
            serialNumber = "SN-FIN-001",
            process = FinishedProcessDto(
                id = 1,
                name = "Process A",
                type = null
            ),
            status = ProductStatusDto.NORMAL     // ← добавлен обязательный статус
        )
        coEvery { mockApi.getFinishedProduct() } returns Response.success(listOf(productShortDto))

        val result = repository.getFinishedProducts()

        assertEquals(1, result.size)
        assertEquals(7, result[0].id)
        assertEquals("SN-FIN-001", result[0].serialNumber)
    }

    @Test
    fun `getFinishedProducts returns empty list when api body is null`() = runTest {
        coEvery { mockApi.getFinishedProduct() } returns Response.success(null)

        val result = repository.getFinishedProducts()

        assertEquals(emptyList<Any>(), result)
    }

    // ==========================================
    // getProductsByLastCompletedStep
    // ==========================================

    @Test
    fun `getProductsByLastCompletedStep returns mapped list`() = runTest {
        coEvery {
            mockApi.getProductsByLastCompletedStep(processId = 1, stepDefinitionId = 10)
        } returns Response.success(listOf(productDto))

        val result = repository.getProductsByLastCompletedStep(
            processId = 1,
            stepDefinitionId = 10
        )

        assertEquals(1, result.size)
        assertEquals("SN-001", result[0].serialNumber)
    }

    @Test
    fun `getProductsByLastCompletedStep returns empty list when api body is null`() = runTest {
        coEvery {
            mockApi.getProductsByLastCompletedStep(any(), any())
        } returns Response.success(null)

        val result = repository.getProductsByLastCompletedStep(1, 10)

        assertEquals(emptyList<Product>(), result)
    }

    // ==========================================
    // getProductsByStepEmployeeDay
    // ==========================================

    @Test
    fun `getProductsByStepEmployeeDay returns mapped list`() = runTest {
        coEvery {
            mockApi.getProductsByStepEmployeeDay(
                stepDefinitionId = 10,
                day = "2024-06-15",
                employeeId = 3
            )
        } returns Response.success(listOf(productDto))

        val result = repository.getProductsByStepEmployeeDay(
            stepDefinitionId = 10,
            day = "2024-06-15",
            employeeId = 3
        )

        assertEquals(1, result.size)
        assertEquals(42L, result[0].id)
    }

    @Test
    fun `getProductsByStepEmployeeDay returns empty list when api body is null`() = runTest {
        coEvery {
            mockApi.getProductsByStepEmployeeDay(any(), any(), any())
        } returns Response.success(null)

        val result = repository.getProductsByStepEmployeeDay(10, "2024-06-15", 3)

        assertEquals(emptyList<Product>(), result)
    }

    // ==========================================
    // getFinishedProductsByPeriod
    // ==========================================

    @Test
    fun `getFinishedProductsByPeriod returns mapped domain model on success`() = runTest {
        // Arrange
        val dto = PeriodStatisticsDto(
            finishedProducts = listOf(
                ProcessCountStatDto(processId = 1, processName = "Process A", count = 10)
            ),
            totalSteps = listOf(
                StepCountStatDto(
                    processId = 1,
                    processName = "Process A",
                    stepDefinitionId = 10,
                    order = 1,
                    stepName = "Step A",
                    employeeId = 100,
                    employeeName = "Emp A",
                    count = 5
                )
            )
        )
        coEvery {
            mockApi.getFinishedProductsByPeriod("2024-01-01", "2024-01-31")
        } returns Response.success(dto)

        // Act
        val result = repository.getFinishedProductsByPeriod("2024-01-01", "2024-01-31")

        // Assert
        assertEquals(1, result.finishedProducts.size)
        assertEquals(1, result.finishedProducts.first().processId)
        assertEquals(10, result.finishedProducts.first().count)

        assertEquals(1, result.totalSteps.size)
        assertEquals(10, result.totalSteps.first().stepDefinitionId)
        assertEquals(5, result.totalSteps.first().count)

        coVerify(exactly = 1) {
            mockApi.getFinishedProductsByPeriod("2024-01-01", "2024-01-31")
        }
    }

    @Test(expected = IllegalArgumentException::class)
    fun `getFinishedProductsByPeriod throws exception when api returns null body`() = runTest {
        // Arrange
        coEvery {
            mockApi.getFinishedProductsByPeriod(any(), any())
        } returns Response.success(null)

        // Act
        repository.getFinishedProductsByPeriod("2024-01-01", "2024-01-31")
    }

    @Test(expected = AppError.ApiError::class)
    fun `getFinishedProductsByPeriod throws ApiError on http error`() = runTest {
        // Arrange
        coEvery {
            mockApi.getFinishedProductsByPeriod(any(), any())
        } returns Response.error(500, "".toResponseBody())

        // Act
        repository.getFinishedProductsByPeriod("2024-01-01", "2024-01-31")
    }

    @Test(expected = AppError.NetworkError::class)
    fun `getFinishedProductsByPeriod throws NetworkError on network failure`() = runTest {
        // Arrange
        coEvery {
            mockApi.getFinishedProductsByPeriod(any(), any())
        } throws AppError.NetworkError()

        // Act
        repository.getFinishedProductsByPeriod("2024-01-01", "2024-01-31")
    }
}