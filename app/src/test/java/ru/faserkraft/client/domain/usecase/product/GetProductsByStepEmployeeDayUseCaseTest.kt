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

class GetProductsByStepEmployeeDayUseCaseTest {

    // ── фикстуры ──────────────────────────────────────────────────────────────

    private val repository: ProductRepository = mockk()
    private lateinit var useCase: GetProductsByStepEmployeeDayUseCase

    private val stepDefinitionId = 3
    private val day = "2024-01-15"
    private val employeeId = 7

    private val process = Process(
        id = 1,
        name = "Сборка",
        description = "",
        steps = emptyList()
    )

    private val products = listOf(
        Product(
            id = 10L,
            serialNumber = "SN-PROD-001",
            process = process,
            createdAt = "2024-01-15T08:00:00",
            packagingSerialNumber = null,
            status = ProductStatus.NORMAL,
            steps = emptyList()
        ),
        Product(
            id = 20L,
            serialNumber = "SN-PROD-002",
            process = process,
            createdAt = "2024-01-15T09:00:00",
            packagingSerialNumber = null,
            status = ProductStatus.NORMAL,
            steps = emptyList()
        )
    )

    @Before
    fun setUp() {
        useCase = GetProductsByStepEmployeeDayUseCase(repository)
    }

    // ── invoke() ──────────────────────────────────────────────────────────────

    @Test
    fun `invoke - returns list of products from repository`() = runTest {
        coEvery {
            repository.getProductsByStepEmployeeDay(stepDefinitionId, day, employeeId)
        } returns products

        val result = useCase(stepDefinitionId, day, employeeId)

        assertEquals(products, result)
    }

    @Test
    fun `invoke - calls repository exactly once`() = runTest {
        coEvery { repository.getProductsByStepEmployeeDay(any(), any(), any()) } returns products

        useCase(stepDefinitionId, day, employeeId)

        coVerify(exactly = 1) {
            repository.getProductsByStepEmployeeDay(stepDefinitionId, day, employeeId)
        }
    }

    @Test
    fun `invoke - passes stepDefinitionId unchanged`() = runTest {
        coEvery { repository.getProductsByStepEmployeeDay(any(), any(), any()) } returns products

        useCase(stepDefinitionId, day, employeeId)

        coVerify { repository.getProductsByStepEmployeeDay(stepDefinitionId, any(), any()) }
    }

    @Test
    fun `invoke - passes day unchanged`() = runTest {
        coEvery { repository.getProductsByStepEmployeeDay(any(), any(), any()) } returns products

        useCase(stepDefinitionId, day, employeeId)

        coVerify { repository.getProductsByStepEmployeeDay(any(), day, any()) }
    }

    @Test
    fun `invoke - passes employeeId unchanged`() = runTest {
        coEvery { repository.getProductsByStepEmployeeDay(any(), any(), any()) } returns products

        useCase(stepDefinitionId, day, employeeId)

        coVerify { repository.getProductsByStepEmployeeDay(any(), any(), employeeId) }
    }

    @Test
    fun `invoke - returns correct products count`() = runTest {
        coEvery { repository.getProductsByStepEmployeeDay(any(), any(), any()) } returns products

        val result = useCase(stepDefinitionId, day, employeeId)

        assertEquals(2, result.size)
    }

    @Test
    fun `invoke - returns products with correct fields`() = runTest {
        coEvery { repository.getProductsByStepEmployeeDay(any(), any(), any()) } returns products

        val result = useCase(stepDefinitionId, day, employeeId)

        assertEquals(10L, result[0].id)
        assertEquals("SN-PROD-001", result[0].serialNumber)
        assertEquals(20L, result[1].id)
        assertEquals("SN-PROD-002", result[1].serialNumber)
    }

    @Test
    fun `invoke - different day returns different products`() = runTest {
        val otherDay = "2024-01-16"
        val otherProducts = listOf(products.first())
        coEvery {
            repository.getProductsByStepEmployeeDay(stepDefinitionId, day, employeeId)
        } returns products
        coEvery {
            repository.getProductsByStepEmployeeDay(stepDefinitionId, otherDay, employeeId)
        } returns otherProducts

        assertEquals(2, useCase(stepDefinitionId, day, employeeId).size)
        assertEquals(1, useCase(stepDefinitionId, otherDay, employeeId).size)
    }

    @Test
    fun `invoke - different employeeId returns different products`() = runTest {
        val otherEmployeeId = 99
        val otherProducts = listOf(products.last())
        coEvery {
            repository.getProductsByStepEmployeeDay(stepDefinitionId, day, employeeId)
        } returns products
        coEvery {
            repository.getProductsByStepEmployeeDay(stepDefinitionId, day, otherEmployeeId)
        } returns otherProducts

        assertEquals(2, useCase(stepDefinitionId, day, employeeId).size)
        assertEquals(1, useCase(stepDefinitionId, day, otherEmployeeId).size)
    }

    @Test
    fun `invoke - returns empty list if no products found`() = runTest {
        coEvery {
            repository.getProductsByStepEmployeeDay(any(), any(), any())
        } returns emptyList()

        val result = useCase(stepDefinitionId, day, employeeId)

        assertTrue(result.isEmpty())
    }

    @Test
    fun `invoke - returns single product correctly`() = runTest {
        val single = listOf(products.first())
        coEvery { repository.getProductsByStepEmployeeDay(any(), any(), any()) } returns single

        val result = useCase(stepDefinitionId, day, employeeId)

        assertEquals(1, result.size)
        assertEquals(10L, result.first().id)
    }

    @Test
    fun `invoke - propagates exception from repository`() = runTest {
        coEvery {
            repository.getProductsByStepEmployeeDay(any(), any(), any())
        } throws RuntimeException("Employee not found")

        val exception = runCatching {
            useCase(stepDefinitionId, day, employeeId)
        }.exceptionOrNull()

        assertEquals("Employee not found", exception?.message)
    }
}