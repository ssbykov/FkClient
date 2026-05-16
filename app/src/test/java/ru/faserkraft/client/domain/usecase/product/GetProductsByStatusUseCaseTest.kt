package ru.faserkraft.client.domain.usecase.product

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import ru.faserkraft.client.domain.model.Process
import ru.faserkraft.client.domain.model.Product
import ru.faserkraft.client.domain.model.ProductStatus
import ru.faserkraft.client.domain.repository.ProductRepository

class GetProductsByStatusUseCaseTest {

    // ── фикстуры ──────────────────────────────────────────────────────────────

    private val repository: ProductRepository = mockk()
    private lateinit var useCase: GetProductsByStatusUseCase

    private val process = Process(
        id = 1,
        name = "Сборка",
        description = "",
        steps = emptyList()
    )

    private val normalProduct = Product(
        id = 1L,
        serialNumber = "SN-001",
        process = process,
        createdAt = "2024-01-15T08:00:00",
        packagingSerialNumber = null,
        status = ProductStatus.NORMAL,
        steps = emptyList()
    )

    private val reworkProduct = Product(
        id = 2L,
        serialNumber = "SN-002",
        process = process,
        createdAt = "2024-01-15T09:00:00",
        packagingSerialNumber = null,
        status = ProductStatus.REWORK,
        steps = emptyList()
    )

    private val scrapProduct = Product(
        id = 3L,
        serialNumber = "SN-003",
        process = process,
        createdAt = "2024-01-15T10:00:00",
        packagingSerialNumber = null,
        status = ProductStatus.SCRAP,
        steps = emptyList()
    )

    @Before
    fun setUp() {
        useCase = GetProductsByStatusUseCase(repository)
    }

    // ── invoke() — базовые сценарии ───────────────────────────────────────────

    @Test
    fun `invoke - returns list of products from repository`() = runTest {
        val statuses = listOf(ProductStatus.NORMAL)
        coEvery { repository.getProductsByStatus(statuses) } returns listOf(normalProduct)

        val result = useCase(statuses)

        assertEquals(listOf(normalProduct), result)
    }

    @Test
    fun `invoke - calls repository exactly once`() = runTest {
        val statuses = listOf(ProductStatus.NORMAL)
        coEvery { repository.getProductsByStatus(any()) } returns listOf(normalProduct)

        useCase(statuses)

        coVerify(exactly = 1) { repository.getProductsByStatus(statuses) }
    }

    @Test
    fun `invoke - passes statuses list unchanged to repository`() = runTest {
        val statuses = listOf(ProductStatus.REWORK, ProductStatus.SCRAP)
        coEvery { repository.getProductsByStatus(statuses) } returns listOf(reworkProduct, scrapProduct)

        useCase(statuses)

        coVerify { repository.getProductsByStatus(statuses) }
    }

    // ── invoke() — одиночный статус ───────────────────────────────────────────

    @Test
    fun `invoke - single status NORMAL returns only normal products`() = runTest {
        val statuses = listOf(ProductStatus.NORMAL)
        coEvery { repository.getProductsByStatus(statuses) } returns listOf(normalProduct)

        val result = useCase(statuses)

        assertEquals(1, result.size)
        assertEquals(ProductStatus.NORMAL, result.first().status)
    }

    @Test
    fun `invoke - single status REWORK returns only rework products`() = runTest {
        val statuses = listOf(ProductStatus.REWORK)
        coEvery { repository.getProductsByStatus(statuses) } returns listOf(reworkProduct)

        val result = useCase(statuses)

        assertEquals(1, result.size)
        assertEquals(ProductStatus.REWORK, result.first().status)
    }

    @Test
    fun `invoke - single status SCRAP returns only scrap products`() = runTest {
        val statuses = listOf(ProductStatus.SCRAP)
        coEvery { repository.getProductsByStatus(statuses) } returns listOf(scrapProduct)

        val result = useCase(statuses)

        assertEquals(1, result.size)
        assertEquals(ProductStatus.SCRAP, result.first().status)
    }

    // ── invoke() — несколько статусов ─────────────────────────────────────────

    @Test
    fun `invoke - multiple statuses returns products with all given statuses`() = runTest {
        val statuses = listOf(ProductStatus.REWORK, ProductStatus.SCRAP)
        val expected = listOf(reworkProduct, scrapProduct)
        coEvery { repository.getProductsByStatus(statuses) } returns expected

        val result = useCase(statuses)

        assertEquals(2, result.size)
        assertTrue(result.any { it.status == ProductStatus.REWORK })
        assertTrue(result.any { it.status == ProductStatus.SCRAP })
    }

    @Test
    fun `invoke - all three statuses returns all products`() = runTest {
        val statuses = listOf(ProductStatus.NORMAL, ProductStatus.REWORK, ProductStatus.SCRAP)
        val expected = listOf(normalProduct, reworkProduct, scrapProduct)
        coEvery { repository.getProductsByStatus(statuses) } returns expected

        val result = useCase(statuses)

        assertEquals(3, result.size)
    }

    // ── invoke() — граничные случаи ───────────────────────────────────────────

    @Test
    fun `invoke - returns empty list when no products found`() = runTest {
        val statuses = listOf(ProductStatus.SCRAP)
        coEvery { repository.getProductsByStatus(statuses) } returns emptyList()

        val result = useCase(statuses)

        assertTrue(result.isEmpty())
    }

    @Test
    fun `invoke - empty statuses list returns empty list`() = runTest {
        val statuses = emptyList<ProductStatus>()
        coEvery { repository.getProductsByStatus(statuses) } returns emptyList()

        val result = useCase(statuses)

        assertTrue(result.isEmpty())
    }

    @Test
    fun `invoke - returns correct product fields`() = runTest {
        val statuses = listOf(ProductStatus.NORMAL)
        coEvery { repository.getProductsByStatus(statuses) } returns listOf(normalProduct)

        val result = useCase(statuses)

        assertEquals(1L, result.first().id)
        assertEquals("SN-001", result.first().serialNumber)
        assertEquals(ProductStatus.NORMAL, result.first().status)
    }

    @Test
    fun `invoke - different statuses return different results`() = runTest {
        val normalStatuses = listOf(ProductStatus.NORMAL)
        val scrapStatuses = listOf(ProductStatus.SCRAP)
        coEvery { repository.getProductsByStatus(normalStatuses) } returns listOf(normalProduct)
        coEvery { repository.getProductsByStatus(scrapStatuses) } returns listOf(scrapProduct)

        val normalResult = useCase(normalStatuses)
        val scrapResult = useCase(scrapStatuses)

        assertEquals(ProductStatus.NORMAL, normalResult.first().status)
        assertEquals(ProductStatus.SCRAP, scrapResult.first().status)
    }

    // ── invoke() — исключения ─────────────────────────────────────────────────

    @Test
    fun `invoke - propagates exception from repository`() = runTest {
        val statuses = listOf(ProductStatus.NORMAL)
        coEvery {
            repository.getProductsByStatus(statuses)
        } throws RuntimeException("Network error")

        val exception = runCatching { useCase(statuses) }.exceptionOrNull()

        assertEquals("Network error", exception?.message)
    }
}