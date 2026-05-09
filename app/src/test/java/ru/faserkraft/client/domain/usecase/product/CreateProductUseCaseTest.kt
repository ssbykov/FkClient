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

class CreateProductUseCaseTest {

    // ── фикстуры ──────────────────────────────────────────────────────────────

    private val repository: ProductRepository = mockk()
    private lateinit var useCase: CreateProductUseCase

    private val serialNumber = "SN-PROD-001"
    private val processId = 1

    private val process = Process(
        id = processId,
        name = "Сборка",
        description = "Сборка изделия",
        steps = emptyList()
    )

    private val createdProduct = Product(
        id = 10L,
        serialNumber = serialNumber,
        process = process,
        createdAt = "2024-01-10T09:00:00",
        packagingSerialNumber = null,
        status = ProductStatus.NORMAL,
        steps = emptyList()
    )

    @Before
    fun setUp() {
        useCase = CreateProductUseCase(repository)
    }

    // ── invoke() ──────────────────────────────────────────────────────────────

    @Test
    fun `invoke - returns created product from repository`() = runTest {
        coEvery { repository.createProduct(serialNumber, processId) } returns createdProduct

        val result = useCase(serialNumber, processId)

        assertEquals(createdProduct, result)
    }

    @Test
    fun `invoke - calls repository exactly once`() = runTest {
        coEvery { repository.createProduct(any(), any()) } returns createdProduct

        useCase(serialNumber, processId)

        coVerify(exactly = 1) { repository.createProduct(serialNumber, processId) }
    }

    @Test
    fun `invoke - passes serialNumber unchanged`() = runTest {
        coEvery { repository.createProduct(any(), any()) } returns createdProduct

        useCase(serialNumber, processId)

        coVerify { repository.createProduct(serialNumber, any()) }
    }

    @Test
    fun `invoke - passes processId unchanged`() = runTest {
        coEvery { repository.createProduct(any(), any()) } returns createdProduct

        useCase(serialNumber, processId)

        coVerify { repository.createProduct(any(), processId) }
    }

    @Test
    fun `invoke - returned product has correct serialNumber`() = runTest {
        coEvery { repository.createProduct(any(), any()) } returns createdProduct

        val result = useCase(serialNumber, processId)

        assertEquals(serialNumber, result.serialNumber)
    }

    @Test
    fun `invoke - returned product has correct process id`() = runTest {
        coEvery { repository.createProduct(any(), any()) } returns createdProduct

        val result = useCase(serialNumber, processId)

        assertEquals(processId, result.process.id)
    }

    @Test
    fun `invoke - new product has NORMAL status`() = runTest {
        coEvery { repository.createProduct(any(), any()) } returns createdProduct

        val result = useCase(serialNumber, processId)

        assertEquals(ProductStatus.NORMAL, result.status)
    }

    @Test
    fun `invoke - new product has null packagingSerialNumber`() = runTest {
        coEvery { repository.createProduct(any(), any()) } returns createdProduct

        val result = useCase(serialNumber, processId)

        assertNull(result.packagingSerialNumber)
    }

    @Test
    fun `invoke - new product has empty steps`() = runTest {
        coEvery { repository.createProduct(any(), any()) } returns createdProduct

        val result = useCase(serialNumber, processId)

        assertEquals(0, result.steps.size)
    }

    @Test
    fun `invoke - different serialNumbers produce different products`() = runTest {
        val product2 = createdProduct.copy(id = 20L, serialNumber = "SN-PROD-002")
        coEvery { repository.createProduct(serialNumber, processId) } returns createdProduct
        coEvery { repository.createProduct("SN-PROD-002", processId) } returns product2

        assertEquals(10L, useCase(serialNumber, processId).id)
        assertEquals(20L, useCase("SN-PROD-002", processId).id)
    }

    @Test
    fun `invoke - propagates exception from repository`() = runTest {
        coEvery {
            repository.createProduct(any(), any())
        } throws RuntimeException("Duplicate serial number")

        val exception = runCatching { useCase(serialNumber, processId) }.exceptionOrNull()

        assertEquals("Duplicate serial number", exception?.message)
    }
}