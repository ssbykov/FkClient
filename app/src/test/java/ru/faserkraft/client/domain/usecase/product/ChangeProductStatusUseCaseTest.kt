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

class ChangeProductStatusUseCaseTest {

    // ── фикстуры ──────────────────────────────────────────────────────────────

    private val repository: ProductRepository = mockk()
    private lateinit var useCase: ChangeProductStatusUseCase

    private val productId = 100L

    private val process = Process(
        id = 1,
        name = "Сборка",
        description = "",
        steps = emptyList()
    )

    private fun productWithStatus(status: ProductStatus) = Product(
        id = productId,
        serialNumber = "SN-PROD-001",
        process = process,
        createdAt = "2024-01-10T09:00:00",
        packagingSerialNumber = null,
        status = status,
        steps = emptyList()
    )

    @Before
    fun setUp() {
        useCase = ChangeProductStatusUseCase(repository)
    }

    // ── invoke() ──────────────────────────────────────────────────────────────

    @Test
    fun `invoke - returns updated product from repository`() = runTest {
        val product = productWithStatus(ProductStatus.REWORK)
        coEvery { repository.changeStatus(productId, ProductStatus.REWORK) } returns product

        val result = useCase(productId, ProductStatus.REWORK)

        assertEquals(product, result)
    }

    @Test
    fun `invoke - calls repository exactly once`() = runTest {
        coEvery { repository.changeStatus(any(), any()) } returns productWithStatus(ProductStatus.NORMAL)

        useCase(productId, ProductStatus.NORMAL)

        coVerify(exactly = 1) { repository.changeStatus(productId, ProductStatus.NORMAL) }
    }

    @Test
    fun `invoke - passes productId unchanged`() = runTest {
        coEvery { repository.changeStatus(any(), any()) } returns productWithStatus(ProductStatus.NORMAL)

        useCase(999L, ProductStatus.NORMAL)

        coVerify { repository.changeStatus(999L, any()) }
    }

    @Test
    fun `invoke - passes status NORMAL unchanged`() = runTest {
        coEvery { repository.changeStatus(any(), any()) } returns productWithStatus(ProductStatus.NORMAL)

        useCase(productId, ProductStatus.NORMAL)

        coVerify { repository.changeStatus(any(), ProductStatus.NORMAL) }
    }

    @Test
    fun `invoke - passes status REWORK unchanged`() = runTest {
        coEvery { repository.changeStatus(any(), any()) } returns productWithStatus(ProductStatus.REWORK)

        useCase(productId, ProductStatus.REWORK)

        coVerify { repository.changeStatus(any(), ProductStatus.REWORK) }
    }

    @Test
    fun `invoke - passes status SCRAP unchanged`() = runTest {
        coEvery { repository.changeStatus(any(), any()) } returns productWithStatus(ProductStatus.SCRAP)

        useCase(productId, ProductStatus.SCRAP)

        coVerify { repository.changeStatus(any(), ProductStatus.SCRAP) }
    }

    @Test
    fun `invoke - returned product has correct status NORMAL`() = runTest {
        coEvery { repository.changeStatus(productId, ProductStatus.NORMAL) } returns productWithStatus(ProductStatus.NORMAL)

        val result = useCase(productId, ProductStatus.NORMAL)

        assertEquals(ProductStatus.NORMAL, result.status)
    }

    @Test
    fun `invoke - returned product has correct status REWORK`() = runTest {
        coEvery { repository.changeStatus(productId, ProductStatus.REWORK) } returns productWithStatus(ProductStatus.REWORK)

        val result = useCase(productId, ProductStatus.REWORK)

        assertEquals(ProductStatus.REWORK, result.status)
    }

    @Test
    fun `invoke - returned product has correct status SCRAP`() = runTest {
        coEvery { repository.changeStatus(productId, ProductStatus.SCRAP) } returns productWithStatus(ProductStatus.SCRAP)

        val result = useCase(productId, ProductStatus.SCRAP)

        assertEquals(ProductStatus.SCRAP, result.status)
    }

    @Test
    fun `invoke - different productIds call repository with correct id`() = runTest {
        val product2 = productWithStatus(ProductStatus.REWORK).copy(id = 200L)
        coEvery { repository.changeStatus(productId, ProductStatus.REWORK) } returns productWithStatus(ProductStatus.REWORK)
        coEvery { repository.changeStatus(200L, ProductStatus.REWORK) } returns product2

        assertEquals(productId, useCase(productId, ProductStatus.REWORK).id)
        assertEquals(200L, useCase(200L, ProductStatus.REWORK).id)
    }

    @Test
    fun `invoke - propagates exception from repository`() = runTest {
        coEvery {
            repository.changeStatus(any(), any())
        } throws RuntimeException("Product not found")

        val exception = runCatching { useCase(productId, ProductStatus.NORMAL) }.exceptionOrNull()

        assertEquals("Product not found", exception?.message)
    }
}