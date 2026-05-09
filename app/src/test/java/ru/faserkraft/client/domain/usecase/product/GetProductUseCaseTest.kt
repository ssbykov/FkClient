package ru.faserkraft.client.domain.usecase.product

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import ru.faserkraft.client.domain.model.Process
import ru.faserkraft.client.domain.model.Product
import ru.faserkraft.client.domain.model.ProductStatus
import ru.faserkraft.client.domain.repository.ProductRepository

class GetProductUseCaseTest {

    // ── фикстуры ──────────────────────────────────────────────────────────────

    private val repository: ProductRepository = mockk()
    private lateinit var useCase: GetProductUseCase

    private val serialNumber = "SN-PROD-001"

    private val process = Process(
        id = 1,
        name = "Сборка",
        description = "",
        steps = emptyList()
    )

    private val product = Product(
        id = 10L,
        serialNumber = serialNumber,
        process = process,
        createdAt = "2024-01-15T08:00:00",
        packagingSerialNumber = null,
        status = ProductStatus.NORMAL,
        steps = emptyList()
    )

    @Before
    fun setUp() {
        useCase = GetProductUseCase(repository)
    }

    // ── invoke() ──────────────────────────────────────────────────────────────

    @Test
    fun `invoke - returns product when found`() = runTest {
        coEvery { repository.getProduct(serialNumber) } returns product

        val result = useCase(serialNumber)

        assertEquals(product, result)
    }

    @Test
    fun `invoke - returns null when product not found`() = runTest {
        coEvery { repository.getProduct(serialNumber) } returns null

        val result = useCase(serialNumber)

        assertNull(result)
    }

    @Test
    fun `invoke - calls repository exactly once`() = runTest {
        coEvery { repository.getProduct(any()) } returns product

        useCase(serialNumber)

        coVerify(exactly = 1) { repository.getProduct(serialNumber) }
    }

    @Test
    fun `invoke - passes serialNumber unchanged`() = runTest {
        coEvery { repository.getProduct(any()) } returns product

        useCase(serialNumber)

        coVerify { repository.getProduct(serialNumber) }
    }

    @Test
    fun `invoke - returned product has correct serialNumber`() = runTest {
        coEvery { repository.getProduct(any()) } returns product

        val result = useCase(serialNumber)

        assertEquals(serialNumber, result?.serialNumber)
    }

    @Test
    fun `invoke - returned product has correct id`() = runTest {
        coEvery { repository.getProduct(any()) } returns product

        val result = useCase(serialNumber)

        assertEquals(10L, result?.id)
    }

    @Test
    fun `invoke - returned product has correct status`() = runTest {
        coEvery { repository.getProduct(any()) } returns product

        val result = useCase(serialNumber)

        assertEquals(ProductStatus.NORMAL, result?.status)
    }

    @Test
    fun `invoke - returned product with packagingSerialNumber`() = runTest {
        val packedProduct = product.copy(packagingSerialNumber = "PKG-001")
        coEvery { repository.getProduct(serialNumber) } returns packedProduct

        val result = useCase(serialNumber)

        assertEquals("PKG-001", result?.packagingSerialNumber)
    }

    @Test
    fun `invoke - returned product with null packagingSerialNumber`() = runTest {
        coEvery { repository.getProduct(serialNumber) } returns product

        val result = useCase(serialNumber)

        assertNull(result?.packagingSerialNumber)
    }

    @Test
    fun `invoke - returned product with REWORK status`() = runTest {
        val reworkProduct = product.copy(status = ProductStatus.REWORK)
        coEvery { repository.getProduct(serialNumber) } returns reworkProduct

        val result = useCase(serialNumber)

        assertEquals(ProductStatus.REWORK, result?.status)
    }

    @Test
    fun `invoke - returned product with SCRAP status`() = runTest {
        val scrapProduct = product.copy(status = ProductStatus.SCRAP)
        coEvery { repository.getProduct(serialNumber) } returns scrapProduct

        val result = useCase(serialNumber)

        assertEquals(ProductStatus.SCRAP, result?.status)
    }

    @Test
    fun `invoke - different serialNumbers return different products`() = runTest {
        val otherSerial = "SN-PROD-002"
        val otherProduct = product.copy(id = 20L, serialNumber = otherSerial)
        coEvery { repository.getProduct(serialNumber) } returns product
        coEvery { repository.getProduct(otherSerial) } returns otherProduct

        assertEquals(10L, useCase(serialNumber)?.id)
        assertEquals(20L, useCase(otherSerial)?.id)
    }

    @Test
    fun `invoke - propagates exception from repository`() = runTest {
        coEvery {
            repository.getProduct(any())
        } throws RuntimeException("Network error")

        val exception = runCatching { useCase(serialNumber) }.exceptionOrNull()

        assertEquals("Network error", exception?.message)
    }
}