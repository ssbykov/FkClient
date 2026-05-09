package ru.faserkraft.client.domain.usecase.packaging

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import ru.faserkraft.client.domain.model.Packaging
import ru.faserkraft.client.domain.repository.PackagingRepository

class GetPackagingUseCaseTest {

    // ── фикстуры ──────────────────────────────────────────────────────────────

    private val repository: PackagingRepository = mockk()
    private lateinit var useCase: GetPackagingUseCase

    private val serialNumber = "SN-2024-001"

    private val packaging = Packaging(
        id = 10,
        serialNumber = serialNumber,
        performedBy = null,
        performedAt = "2024-03-01T10:00:00",
        orderId = null,
        products = emptyList()
    )

    @Before
    fun setUp() {
        useCase = GetPackagingUseCase(repository)
    }

    // ── invoke() ──────────────────────────────────────────────────────────────

    @Test
    fun `invoke - returns packaging if found`() = runTest {
        coEvery { repository.getPackaging(serialNumber) } returns packaging

        val result = useCase(serialNumber)

        assertEquals(packaging, result)
    }

    @Test
    fun `invoke - returns null if packaging not found`() = runTest {
        coEvery { repository.getPackaging(any()) } returns null

        val result = useCase(serialNumber)

        assertNull(result)
    }

    @Test
    fun `invoke - calls repository exactly once`() = runTest {
        coEvery { repository.getPackaging(any()) } returns packaging

        useCase(serialNumber)

        coVerify(exactly = 1) { repository.getPackaging(serialNumber) }
    }

    @Test
    fun `invoke - passes serialNumber unchanged`() = runTest {
        coEvery { repository.getPackaging(any()) } returns packaging

        useCase("SN-2024-999")

        coVerify { repository.getPackaging("SN-2024-999") }
    }

    @Test
    fun `invoke - returned packaging has correct serialNumber`() = runTest {
        coEvery { repository.getPackaging(serialNumber) } returns packaging

        val result = useCase(serialNumber)

        assertEquals(serialNumber, result?.serialNumber)
    }

    @Test
    fun `invoke - returned packaging has null orderId when in storage`() = runTest {
        coEvery { repository.getPackaging(serialNumber) } returns packaging

        val result = useCase(serialNumber)

        assertNull(result?.orderId)
    }

    @Test
    fun `invoke - returned packaging has non-null orderId when attached`() = runTest {
        val attachedPackaging = packaging.copy(orderId = 5)
        coEvery { repository.getPackaging(serialNumber) } returns attachedPackaging

        val result = useCase(serialNumber)

        assertNotNull(result?.orderId)
        assertEquals(5, result?.orderId)
    }

    @Test
    fun `invoke - different serialNumbers return different packaging`() = runTest {
        val packaging2 = packaging.copy(id = 20, serialNumber = "SN-2024-002")
        coEvery { repository.getPackaging(serialNumber) } returns packaging
        coEvery { repository.getPackaging("SN-2024-002") } returns packaging2

        assertEquals(10, useCase(serialNumber)?.id)
        assertEquals(20, useCase("SN-2024-002")?.id)
    }

    @Test
    fun `invoke - propagates exception from repository`() = runTest {
        coEvery {
            repository.getPackaging(any())
        } throws RuntimeException("Network error")

        val exception = runCatching { useCase(serialNumber) }.exceptionOrNull()

        assertEquals("Network error", exception?.message)
    }
}