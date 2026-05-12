package ru.faserkraft.client.domain.usecase.product

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import ru.faserkraft.client.domain.model.FinishedProcess
import ru.faserkraft.client.domain.model.ProductShort
import ru.faserkraft.client.domain.model.ProductStatus
import ru.faserkraft.client.domain.repository.ProductRepository

class GetFinishedProductsUseCaseTest {

    // ── фикстуры ──────────────────────────────────────────────────────────────

    private val repository: ProductRepository = mockk()
    private lateinit var useCase: GetFinishedProductsUseCase

    private val finishedProcess = FinishedProcess(
        id = 1,
        name = "Сборка",
        sizeTypeId = 2,
        sizeTypeName = "Стандарт",
        packagingCount = 10
    )

    private val products = listOf(
        ProductShort(
            id = 1,
            serialNumber = "SN-PROD-001",
            process = finishedProcess,
            status = ProductStatus.NORMAL
        ),
        ProductShort(
            id = 2,
            serialNumber = "SN-PROD-002",
            process = finishedProcess.copy(id = 2, name = "Покраска"),
            status = ProductStatus.NORMAL
        )
    )

    @Before
    fun setUp() {
        useCase = GetFinishedProductsUseCase(repository)
    }

    // ── invoke() ──────────────────────────────────────────────────────────────

    @Test
    fun `invoke - returns list of products from repository`() = runTest {
        coEvery { repository.getFinishedProducts() } returns products

        val result = useCase()

        assertEquals(products, result)
    }

    @Test
    fun `invoke - calls repository exactly once`() = runTest {
        coEvery { repository.getFinishedProducts() } returns products

        useCase()

        coVerify(exactly = 1) { repository.getFinishedProducts() }
    }

    @Test
    fun `invoke - returns correct products count`() = runTest {
        coEvery { repository.getFinishedProducts() } returns products

        val result = useCase()

        assertEquals(2, result.size)
    }

    @Test
    fun `invoke - returns products with correct serialNumbers`() = runTest {
        coEvery { repository.getFinishedProducts() } returns products

        val result = useCase()

        assertEquals("SN-PROD-001", result[0].serialNumber)
        assertEquals("SN-PROD-002", result[1].serialNumber)
    }

    @Test
    fun `invoke - returns products with correct process fields`() = runTest {
        coEvery { repository.getFinishedProducts() } returns products

        val result = useCase()

        assertEquals(1, result[0].process.id)
        assertEquals("Сборка", result[0].process.name)
        assertEquals("Стандарт", result[0].process.sizeTypeName)
        assertEquals(10, result[0].process.packagingCount)
    }

    @Test
    fun `invoke - returns products with nullable process fields`() = runTest {
        val productWithNulls = products.first().copy(
            process = finishedProcess.copy(sizeTypeId = null, sizeTypeName = null, packagingCount = null)
        )
        coEvery { repository.getFinishedProducts() } returns listOf(productWithNulls)

        val result = useCase()

        assertEquals(null, result[0].process.sizeTypeId)
        assertEquals(null, result[0].process.sizeTypeName)
        assertEquals(null, result[0].process.packagingCount)
    }

    @Test
    fun `invoke - returns products with correct status`() = runTest {
        val productsWithStatuses = listOf(
            products[0].copy(status = ProductStatus.NORMAL),
            products[1].copy(status = ProductStatus.REWORK)
        )
        coEvery { repository.getFinishedProducts() } returns productsWithStatuses

        val result = useCase()

        assertEquals(ProductStatus.NORMAL, result[0].status)
        assertEquals(ProductStatus.REWORK, result[1].status)
    }

    @Test
    fun `invoke - returns empty list if repository returns empty`() = runTest {
        coEvery { repository.getFinishedProducts() } returns emptyList()

        val result = useCase()

        assertTrue(result.isEmpty())
    }

    @Test
    fun `invoke - returns single product correctly`() = runTest {
        val single = listOf(products.first())
        coEvery { repository.getFinishedProducts() } returns single

        val result = useCase()

        assertEquals(1, result.size)
        assertEquals("SN-PROD-001", result.first().serialNumber)
    }

    @Test
    fun `invoke - propagates exception from repository`() = runTest {
        coEvery { repository.getFinishedProducts() } throws RuntimeException("Network error")

        val exception = runCatching { useCase() }.exceptionOrNull()

        assertEquals("Network error", exception?.message)
    }
}