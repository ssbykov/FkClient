package ru.faserkraft.client.domain.usecase.step

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import ru.faserkraft.client.domain.model.Process
import ru.faserkraft.client.domain.model.Product
import ru.faserkraft.client.domain.model.ProductStatus
import ru.faserkraft.client.domain.repository.StepRepository

class CloseStepUseCaseTest {

    // ── фикстуры ──────────────────────────────────────────────────────────────

    private val repository: StepRepository = mockk()
    private lateinit var useCase: CloseStepUseCase

    private val stepId = 7

    private val process = Process(
        id = 1,
        name = "Сборка",
        description = "",
        steps = emptyList()
    )

    private val product = Product(
        id = 10L,
        serialNumber = "SN-PROD-001",
        process = process,
        createdAt = "2024-01-15T08:00:00",
        packagingSerialNumber = null,
        status = ProductStatus.NORMAL,
        steps = emptyList()
    )

    @Before
    fun setUp() {
        useCase = CloseStepUseCase(repository)
    }

    // ── invoke() ──────────────────────────────────────────────────────────────

    @Test
    fun `invoke - returns updated product from repository`() = runTest {
        coEvery { repository.closeStep(stepId) } returns product

        val result = useCase(stepId)

        assertEquals(product, result)
    }

    @Test
    fun `invoke - calls repository exactly once`() = runTest {
        coEvery { repository.closeStep(any()) } returns product

        useCase(stepId)

        coVerify(exactly = 1) { repository.closeStep(stepId) }
    }

    @Test
    fun `invoke - passes stepId unchanged`() = runTest {
        coEvery { repository.closeStep(any()) } returns product

        useCase(stepId)

        coVerify { repository.closeStep(stepId) }
    }

    @Test
    fun `invoke - returned product has correct id`() = runTest {
        coEvery { repository.closeStep(any()) } returns product

        val result = useCase(stepId)

        assertEquals(10L, result.id)
    }

    @Test
    fun `invoke - returned product has correct serialNumber`() = runTest {
        coEvery { repository.closeStep(any()) } returns product

        val result = useCase(stepId)

        assertEquals("SN-PROD-001", result.serialNumber)
    }

    @Test
    fun `invoke - returned product has NORMAL status after close`() = runTest {
        coEvery { repository.closeStep(any()) } returns product

        val result = useCase(stepId)

        assertEquals(ProductStatus.NORMAL, result.status)
    }

    @Test
    fun `invoke - returned product can have REWORK status after close`() = runTest {
        val reworkProduct = product.copy(status = ProductStatus.REWORK)
        coEvery { repository.closeStep(stepId) } returns reworkProduct

        val result = useCase(stepId)

        assertEquals(ProductStatus.REWORK, result.status)
    }

    @Test
    fun `invoke - returned product has null packagingSerialNumber`() = runTest {
        coEvery { repository.closeStep(any()) } returns product

        val result = useCase(stepId)

        assertNull(result.packagingSerialNumber)
    }

    @Test
    fun `invoke - returned product can have packagingSerialNumber after close`() = runTest {
        val packedProduct = product.copy(packagingSerialNumber = "PKG-001")
        coEvery { repository.closeStep(stepId) } returns packedProduct

        val result = useCase(stepId)

        assertNotNull(result.packagingSerialNumber)
        assertEquals("PKG-001", result.packagingSerialNumber)
    }

    @Test
    fun `invoke - different stepId returns different product`() = runTest {
        val otherProduct = product.copy(id = 20L, serialNumber = "SN-PROD-002")
        coEvery { repository.closeStep(stepId) } returns product
        coEvery { repository.closeStep(99) } returns otherProduct

        assertEquals(10L, useCase(stepId).id)
        assertEquals(20L, useCase(99).id)
    }

    @Test
    fun `invoke - propagates exception when step not found`() = runTest {
        coEvery {
            repository.closeStep(any())
        } throws RuntimeException("Step not found")

        val exception = runCatching { useCase(stepId) }.exceptionOrNull()

        assertEquals("Step not found", exception?.message)
    }

    @Test
    fun `invoke - propagates exception when step already closed`() = runTest {
        coEvery {
            repository.closeStep(any())
        } throws IllegalStateException("Step is already closed")

        val exception = runCatching { useCase(stepId) }.exceptionOrNull()

        assertEquals("Step is already closed", exception?.message)
    }
}