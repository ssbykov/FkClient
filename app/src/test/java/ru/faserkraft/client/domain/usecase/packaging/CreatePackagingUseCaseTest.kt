package ru.faserkraft.client.domain.usecase.packaging

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import ru.faserkraft.client.domain.model.Packaging
import ru.faserkraft.client.domain.repository.PackagingRepository

class CreatePackagingUseCaseTest {

    // ── фикстуры ──────────────────────────────────────────────────────────────

    private val repository: PackagingRepository = mockk()
    private lateinit var useCase: CreatePackagingUseCase

    private val serialNumber = "SN-2024-001"
    private val productIds = listOf(1, 2, 3)

    private val createdPackaging = Packaging(
        id = 10,
        serialNumber = serialNumber,
        performedBy = null,
        performedAt = null,
        orderId = null,
        products = emptyList()
    )

    @Before
    fun setUp() {
        useCase = CreatePackagingUseCase(repository)
    }

    // ── invoke() ──────────────────────────────────────────────────────────────

    @Test
    fun `invoke - returns created packaging from repository`() = runTest {
        coEvery { repository.createPackaging(serialNumber, productIds) } returns createdPackaging

        val result = useCase(serialNumber, productIds)

        assertEquals(createdPackaging, result)
    }

    @Test
    fun `invoke - calls repository exactly once`() = runTest {
        coEvery { repository.createPackaging(any(), any()) } returns createdPackaging

        useCase(serialNumber, productIds)

        coVerify(exactly = 1) { repository.createPackaging(serialNumber, productIds) }
    }

    @Test
    fun `invoke - passes serialNumber unchanged`() = runTest {
        coEvery { repository.createPackaging(any(), any()) } returns createdPackaging

        useCase(serialNumber, productIds)

        coVerify { repository.createPackaging(serialNumber, any()) }
    }

    @Test
    fun `invoke - passes productIds list unchanged`() = runTest {
        coEvery { repository.createPackaging(any(), any()) } returns createdPackaging

        useCase(serialNumber, productIds)

        coVerify { repository.createPackaging(any(), productIds) }
    }

    @Test
    fun `invoke - returned packaging has correct serialNumber`() = runTest {
        coEvery { repository.createPackaging(any(), any()) } returns createdPackaging

        val result = useCase(serialNumber, productIds)

        assertEquals(serialNumber, result.serialNumber)
    }

    @Test
    fun `invoke - new packaging has null orderId`() = runTest {
        coEvery { repository.createPackaging(any(), any()) } returns createdPackaging

        val result = useCase(serialNumber, productIds)

        assertNull(result.orderId)
    }

    @Test
    fun `invoke - new packaging has null performedAt`() = runTest {
        coEvery { repository.createPackaging(any(), any()) } returns createdPackaging

        val result = useCase(serialNumber, productIds)

        assertNull(result.performedAt)
    }

    @Test
    fun `invoke - works with empty productIds list`() = runTest {
        val emptyPackaging = createdPackaging.copy(products = emptyList())
        coEvery { repository.createPackaging(serialNumber, emptyList()) } returns emptyPackaging

        val result = useCase(serialNumber, emptyList())

        assertEquals(serialNumber, result.serialNumber)
    }

    @Test
    fun `invoke - works with single productId`() = runTest {
        coEvery { repository.createPackaging(any(), listOf(1)) } returns createdPackaging

        useCase(serialNumber, listOf(1))

        coVerify { repository.createPackaging(serialNumber, listOf(1)) }
    }

    @Test
    fun `invoke - different serialNumbers produce different packaging`() = runTest {
        val packaging2 = createdPackaging.copy(id = 20, serialNumber = "SN-2024-002")
        coEvery { repository.createPackaging(serialNumber, productIds) } returns createdPackaging
        coEvery { repository.createPackaging("SN-2024-002", productIds) } returns packaging2

        assertEquals(10, useCase(serialNumber, productIds).id)
        assertEquals(20, useCase("SN-2024-002", productIds).id)
    }

    @Test
    fun `invoke - propagates exception from repository`() = runTest {
        coEvery {
            repository.createPackaging(any(), any())
        } throws RuntimeException("Duplicate serial number")

        val exception = runCatching { useCase(serialNumber, productIds) }.exceptionOrNull()

        assertEquals("Duplicate serial number", exception?.message)
    }
}