package ru.faserkraft.client.domain.usecase.process

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import ru.faserkraft.client.domain.model.Process
import ru.faserkraft.client.domain.repository.ProcessRepository

class GetProcessesUseCaseTest {

    // ── фикстуры ──────────────────────────────────────────────────────────────

    private val repository: ProcessRepository = mockk()
    private lateinit var useCase: GetProcessesUseCase

    private val processes = listOf(
        Process(
            id = 1,
            name = "Сборка",
            description = "Сборка изделия",
            steps = emptyList()
        ),
        Process(
            id = 2,
            name = "Покраска",
            description = "Покраска изделия",
            steps = emptyList()
        )
    )

    @Before
    fun setUp() {
        useCase = GetProcessesUseCase(repository)
    }

    // ── invoke() ──────────────────────────────────────────────────────────────

    @Test
    fun `invoke - returns list of processes from repository`() = runTest {
        coEvery { repository.getProcesses() } returns processes

        val result = useCase()

        assertEquals(processes, result)
    }

    @Test
    fun `invoke - calls repository exactly once`() = runTest {
        coEvery { repository.getProcesses() } returns processes

        useCase()

        coVerify(exactly = 1) { repository.getProcesses() }
    }

    @Test
    fun `invoke - returns correct processes count`() = runTest {
        coEvery { repository.getProcesses() } returns processes

        val result = useCase()

        assertEquals(2, result.size)
    }

    @Test
    fun `invoke - returns processes with correct fields`() = runTest {
        coEvery { repository.getProcesses() } returns processes

        val result = useCase()

        assertEquals(1, result[0].id)
        assertEquals("Сборка", result[0].name)
        assertEquals("Сборка изделия", result[0].description)
        assertEquals(2, result[1].id)
        assertEquals("Покраска", result[1].name)
    }

    @Test
    fun `invoke - returns processes with steps list`() = runTest {
        coEvery { repository.getProcesses() } returns processes

        val result = useCase()

        assertTrue(result[0].steps.isEmpty())
        assertTrue(result[1].steps.isEmpty())
    }

    @Test
    fun `invoke - returns empty list if repository returns empty`() = runTest {
        coEvery { repository.getProcesses() } returns emptyList()

        val result = useCase()

        assertTrue(result.isEmpty())
    }

    @Test
    fun `invoke - returns single process correctly`() = runTest {
        val single = listOf(processes.first())
        coEvery { repository.getProcesses() } returns single

        val result = useCase()

        assertEquals(1, result.size)
        assertEquals(1, result.first().id)
        assertEquals("Сборка", result.first().name)
    }

    @Test
    fun `invoke - propagates exception from repository`() = runTest {
        coEvery { repository.getProcesses() } throws RuntimeException("Network error")

        val exception = runCatching { useCase() }.exceptionOrNull()

        assertEquals("Network error", exception?.message)
    }
}