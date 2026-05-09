package ru.faserkraft.client.domain.usecase.product

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import ru.faserkraft.client.domain.model.Process
import ru.faserkraft.client.domain.model.Product
import ru.faserkraft.client.domain.model.ProductStatus
import ru.faserkraft.client.domain.repository.ProductRepository

class ChangeProductProcessUseCaseTest {

    // ── фикстуры ──────────────────────────────────────────────────────────────

    private val repository: ProductRepository = mockk()
    private lateinit var useCase: ChangeProductProcessUseCase

    private val productId = 100L
    private val newProcessId = 3

    private val newProcess = Process(
        id = newProcessId,
        name = "Покраска",
        description = "Покраска изделия",
        steps = emptyList()
    )

    private val updatedProduct = Product(
        id = productId,
        serialNumber = "SN-PROD-001",
        process = newProcess,
        createdAt = "2024-01-10T09:00:00",
        packagingSerialNumber = null,
        status = ProductStatus.NORMAL,
        steps = emptyList()
    )

    @Before
    fun setUp() {
        useCase = ChangeProductProcessUseCase(repository)
    }

    // ── invoke() ──────────────────────────────────────────────────────────────

    @Test
    fun `invoke - returns updated product from repository`() = runTest {
        coEvery { repository.changeProcess(productId, newProcessId) } returns updatedProduct

        val result = useCase(productId, newProcessId)

        assertEquals(updatedProduct, result)
    }

    @Test
    fun `invoke - calls repository exactly once`() = runTest {
        coEvery { repository.changeProcess(any(), any()) } returns updatedProduct

        useCase(productId, newProcessId)

        coVerify(exactly = 1) { repository.changeProcess(productId, newProcessId) }
    }

    @Test
    fun `invoke - passes productId unchanged`() = runTest {
        coEvery { repository.changeProcess(any(), any()) } returns updatedProduct

        useCase(999L, newProcessId)

        coVerify { repository.changeProcess(999L, any()) }
    }

    @Test
    fun `invoke - passes newProcessId unchanged`() = runTest {
        coEvery { repository.changeProcess(any(), any()) } returns updatedProduct

        useCase(productId, newProcessId)

        coVerify { repository.changeProcess(any(), newProcessId) }
    }

    @Test
    fun `invoke - returned product has updated process id`() = runTest {
        coEvery { repository.changeProcess(productId, newProcessId) } returns updatedProduct

        val result = useCase(productId, newProcessId)

        assertEquals(newProcessId, result.process.id)
    }

    @Test
    fun `invoke - returned product has updated process name`() = runTest {
        coEvery { repository.changeProcess(productId, newProcessId) } returns updatedProduct

        val result = useCase(productId, newProcessId)

        assertEquals("Покраска", result.process.name)
    }

    @Test
    fun `invoke - returned product status is unchanged`() = runTest {
        coEvery { repository.changeProcess(productId, newProcessId) } returns updatedProduct

        val result = useCase(productId, newProcessId)

        assertEquals(ProductStatus.NORMAL, result.status)
    }

    @Test
    fun `invoke - different productIds call repository with correct id`() = runTest {
        val product2 = updatedProduct.copy(id = 200L, serialNumber = "SN-PROD-002")
        coEvery { repository.changeProcess(productId, newProcessId) } returns updatedProduct
        coEvery { repository.changeProcess(200L, newProcessId) } returns product2

        assertEquals(productId, useCase(productId, newProcessId).id)
        assertEquals(200L, useCase(200L, newProcessId).id)
    }

    @Test
    fun `invoke - propagates exception from repository`() = runTest {
        coEvery {
            repository.changeProcess(any(), any())
        } throws RuntimeException("Product not found")

        val exception = runCatching { useCase(productId, newProcessId) }.exceptionOrNull()

        assertEquals("Product not found", exception?.message)
    }
}