package ru.faserkraft.client.domain.usecase.step

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
import ru.faserkraft.client.domain.repository.StepRepository

class ChangeStepPerformerUseCaseTest {

    // ── фикстуры ──────────────────────────────────────────────────────────────

    private val repository: StepRepository = mockk()
    private lateinit var useCase: ChangeStepPerformerUseCase

    private val stepId = 5
    private val newEmployeeId = 42

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
        useCase = ChangeStepPerformerUseCase(repository)
    }

    // ── invoke() ──────────────────────────────────────────────────────────────

    @Test
    fun `invoke - returns updated product from repository`() = runTest {
        coEvery { repository.changeStepPerformer(stepId, newEmployeeId) } returns product

        val result = useCase(stepId, newEmployeeId)

        assertEquals(product, result)
    }

    @Test
    fun `invoke - calls repository exactly once`() = runTest {
        coEvery { repository.changeStepPerformer(any(), any()) } returns product

        useCase(stepId, newEmployeeId)

        coVerify(exactly = 1) { repository.changeStepPerformer(stepId, newEmployeeId) }
    }

    @Test
    fun `invoke - passes stepId unchanged`() = runTest {
        coEvery { repository.changeStepPerformer(any(), any()) } returns product

        useCase(stepId, newEmployeeId)

        coVerify { repository.changeStepPerformer(stepId, any()) }
    }

    @Test
    fun `invoke - passes newEmployeeId unchanged`() = runTest {
        coEvery { repository.changeStepPerformer(any(), any()) } returns product

        useCase(stepId, newEmployeeId)

        coVerify { repository.changeStepPerformer(any(), newEmployeeId) }
    }

    @Test
    fun `invoke - returned product has correct id`() = runTest {
        coEvery { repository.changeStepPerformer(any(), any()) } returns product

        val result = useCase(stepId, newEmployeeId)

        assertEquals(10L, result.id)
    }

    @Test
    fun `invoke - returned product has correct serialNumber`() = runTest {
        coEvery { repository.changeStepPerformer(any(), any()) } returns product

        val result = useCase(stepId, newEmployeeId)

        assertEquals("SN-PROD-001", result.serialNumber)
    }

    @Test
    fun `invoke - different stepId returns different product`() = runTest {
        val otherProduct = product.copy(id = 20L, serialNumber = "SN-PROD-002")
        coEvery { repository.changeStepPerformer(stepId, newEmployeeId) } returns product
        coEvery { repository.changeStepPerformer(99, newEmployeeId) } returns otherProduct

        assertEquals(10L, useCase(stepId, newEmployeeId).id)
        assertEquals(20L, useCase(99, newEmployeeId).id)
    }

    @Test
    fun `invoke - different newEmployeeId returns different product`() = runTest {
        val otherProduct = product.copy(id = 30L, serialNumber = "SN-PROD-003")
        coEvery { repository.changeStepPerformer(stepId, newEmployeeId) } returns product
        coEvery { repository.changeStepPerformer(stepId, 99) } returns otherProduct

        assertEquals(10L, useCase(stepId, newEmployeeId).id)
        assertEquals(30L, useCase(stepId, 99).id)
    }

    @Test
    fun `invoke - propagates exception when step not found`() = runTest {
        coEvery {
            repository.changeStepPerformer(any(), any())
        } throws RuntimeException("Step not found")

        val exception = runCatching { useCase(stepId, newEmployeeId) }.exceptionOrNull()

        assertEquals("Step not found", exception?.message)
    }

    @Test
    fun `invoke - propagates exception when employee not found`() = runTest {
        coEvery {
            repository.changeStepPerformer(any(), any())
        } throws RuntimeException("Employee not found")

        val exception = runCatching { useCase(stepId, newEmployeeId) }.exceptionOrNull()

        assertEquals("Employee not found", exception?.message)
    }
}