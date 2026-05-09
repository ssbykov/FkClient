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

class GetProductsByLastStepUseCaseTest {

    // ── фикстуры ──────────────────────────────────────────────────────────────

    private val repository: ProductRepository = mockk()
    private lateinit var useCase: GetProductsByLastStepUseCase

    private val processId = 1
    private val stepDefinitionId = 3

    private val process = Process(
        id = processId,
        name = "Сборка",
        description = "",
        steps = emptyList()
    )

    private val products = listOf(
        Product(
            id = 10L,
            serialNumber = "SN-PROD-001",
            process = process,
            createdAt = "2024-01-10T09:00:00",
            packagingSerialNumber = null,
            status = ProductStatus.NORMAL,
            steps = emptyList()
        ),
        Product(
            id = 20L,
            serialNumber = "SN-PROD-002",
            process = process,
            createdAt = "2024-01-11T09:00:00",
            packagingSerialNumber = null,
            status = ProductStatus.NORMAL,
            steps = emptyList()
        )
    )

    @Before
    fun setUp() {
        useCase = GetProductsByLastStepUseCase(repository)
    }

    // ── invoke() ──────────────────────────────────────────────────────────────

    @Test
    fun `invoke - returns list of products from repository`() = runTest {
        coEvery {
            repository.getProductsByLastCompletedStep(processId, stepDefinitionId)
        } returns products

        val result = useCase(processId, stepDefinitionId)

        assertEquals(products, result)
    }

    @Test
    fun `invoke - calls repository exactly once`() = runTest {
        coEvery { repository.getProductsByLastCompletedStep(any(), any()) } returns products

        useCase(processId, stepDefinitionId)

        coVerify(exactly = 1) {
            repository.getProductsByLastCompletedStep(processId, stepDefinitionId)
        }
    }

    @Test
    fun `invoke - passes processId unchanged`() = runTest {
        coEvery { repository.getProductsByLastCompletedStep(any(), any()) } returns products

        useCase(processId, stepDefinitionId)

        coVerify { repository.getProductsByLastCompletedStep(processId, any()) }
    }

    @Test
    fun `invoke - passes stepDefinitionId unchanged`() = runTest {
        coEvery { repository.getProductsByLastCompletedStep(any(), any()) } returns products

        useCase(processId, stepDefinitionId)

        coVerify { repository.getProductsByLastCompletedStep(any(), stepDefinitionId) }
    }

    @Test
    fun `invoke - returns correct products count`() = runTest {
        coEvery { repository.getProductsByLastCompletedStep(any(), any()) } returns products

        val result = useCase(processId, stepDefinitionId)

        assertEquals(2, result.size)
    }

    @Test
    fun `invoke - returns products with correct fields`() = runTest {
        coEvery { repository.getProductsByLastCompletedStep(any(), any()) } returns products

        val result = useCase(processId, stepDefinitionId)

        assertEquals(10L, result[0].id)
        assertEquals("SN-PROD-001", result[0].serialNumber)
        assertEquals(20L, result[1].id)
        assertEquals("SN-PROD-002", result[1].serialNumber)
    }

    @Test
    fun `invoke - different params return different products`() = runTest {
        val otherProducts = listOf(products.first())
        coEvery {
            repository.getProductsByLastCompletedStep(processId, stepDefinitionId)
        } returns products
        coEvery {
            repository.getProductsByLastCompletedStep(2, stepDefinitionId)
        } returns otherProducts

        assertEquals(2, useCase(processId, stepDefinitionId).size)
        assertEquals(1, useCase(2, stepDefinitionId).size)
    }

    @Test
    fun `invoke - returns empty list if no products at step`() = runTest {
        coEvery {
            repository.getProductsByLastCompletedStep(any(), any())
        } returns emptyList()

        val result = useCase(processId, stepDefinitionId)

        assertTrue(result.isEmpty())
    }

    @Test
    fun `invoke - returns single product correctly`() = runTest {
        val single = listOf(products.first())
        coEvery { repository.getProductsByLastCompletedStep(any(), any()) } returns single

        val result = useCase(processId, stepDefinitionId)

        assertEquals(1, result.size)
        assertEquals(10L, result.first().id)
    }

    @Test
    fun `invoke - propagates exception from repository`() = runTest {
        coEvery {
            repository.getProductsByLastCompletedStep(any(), any())
        } throws RuntimeException("Process not found")

        val exception = runCatching { useCase(processId, stepDefinitionId) }.exceptionOrNull()

        assertEquals("Process not found", exception?.message)
    }
}