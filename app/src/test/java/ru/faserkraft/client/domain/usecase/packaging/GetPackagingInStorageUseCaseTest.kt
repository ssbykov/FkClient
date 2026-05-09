package ru.faserkraft.client.domain.usecase.packaging

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import ru.faserkraft.client.domain.model.Packaging
import ru.faserkraft.client.domain.repository.PackagingRepository

class GetPackagingInStorageUseCaseTest {

    // ── фикстуры ──────────────────────────────────────────────────────────────

    private val repository: PackagingRepository = mockk()
    private lateinit var useCase: GetPackagingInStorageUseCase

    private val packagingList = listOf(
        Packaging(
            id = 1,
            serialNumber = "SN-2024-001",
            performedBy = null,
            performedAt = "2024-03-01T10:00:00",
            orderId = null,
            products = emptyList()
        ),
        Packaging(
            id = 2,
            serialNumber = "SN-2024-002",
            performedBy = null,
            performedAt = "2024-03-02T12:00:00",
            orderId = null,
            products = emptyList()
        )
    )

    @Before
    fun setUp() {
        useCase = GetPackagingInStorageUseCase(repository)
    }

    // ── invoke() ──────────────────────────────────────────────────────────────

    @Test
    fun `invoke - returns list of packaging from repository`() = runTest {
        coEvery { repository.getPackagingInStorage() } returns packagingList

        val result = useCase()

        assertEquals(packagingList, result)
    }

    @Test
    fun `invoke - calls repository exactly once`() = runTest {
        coEvery { repository.getPackagingInStorage() } returns packagingList

        useCase()

        coVerify(exactly = 1) { repository.getPackagingInStorage() }
    }

    @Test
    fun `invoke - returns correct packaging count`() = runTest {
        coEvery { repository.getPackagingInStorage() } returns packagingList

        val result = useCase()

        assertEquals(2, result.size)
    }

    @Test
    fun `invoke - returns packaging with correct fields`() = runTest {
        coEvery { repository.getPackagingInStorage() } returns packagingList

        val result = useCase()

        assertEquals(1, result[0].id)
        assertEquals("SN-2024-001", result[0].serialNumber)
        assertEquals(2, result[1].id)
        assertEquals("SN-2024-002", result[1].serialNumber)
    }

    @Test
    fun `invoke - storage packaging has null orderId`() = runTest {
        coEvery { repository.getPackagingInStorage() } returns packagingList

        val result = useCase()

        assertNull(result[0].orderId)
        assertNull(result[1].orderId)
    }

    @Test
    fun `invoke - returns empty list if repository returns empty`() = runTest {
        coEvery { repository.getPackagingInStorage() } returns emptyList()

        val result = useCase()

        assertTrue(result.isEmpty())
    }

    @Test
    fun `invoke - returns single packaging list correctly`() = runTest {
        val single = listOf(packagingList.first())
        coEvery { repository.getPackagingInStorage() } returns single

        val result = useCase()

        assertEquals(1, result.size)
        assertEquals("SN-2024-001", result.first().serialNumber)
    }

    @Test
    fun `invoke - propagates exception from repository`() = runTest {
        coEvery { repository.getPackagingInStorage() } throws RuntimeException("Network error")

        val exception = runCatching { useCase() }.exceptionOrNull()

        assertEquals("Network error", exception?.message)
    }
}