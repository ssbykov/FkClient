package ru.faserkraft.client.domain.usecase.product

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import ru.faserkraft.client.domain.model.ProductsOverview
import ru.faserkraft.client.domain.repository.ProductRepository

class GetProductsInventoryUseCaseTest {

    // ── фикстуры ──────────────────────────────────────────────────────────────

    private val repository: ProductRepository = mockk()
    private lateinit var useCase: GetProductsOverviewUseCase

    private val inventory = listOf(
        ProductsOverview(
            processId = 1,
            processName = "Сборка",
            stepDefinitionId = 10,
            stepName = "Контроль качества",
            stepNameGenitive = "Контроля качества",
            count = 120
        ),
        ProductsOverview(
            processId = 2,
            processName = "Покраска",
            stepDefinitionId = 11,
            stepName = "Финишная покраска",
            stepNameGenitive = "Финишной покраски",
            count = 50
        )
    )

    @Before
    fun setUp() {
        useCase = GetProductsOverviewUseCase(repository)
    }

    // ── invoke() ──────────────────────────────────────────────────────────────

    @Test
    fun `invoke - returns inventory list from repository`() = runTest {
        coEvery { repository.getProductsOverview() } returns inventory

        val result = useCase()

        assertEquals(inventory, result)
    }

    @Test
    fun `invoke - calls repository exactly once`() = runTest {
        coEvery { repository.getProductsOverview() } returns inventory

        useCase()

        coVerify(exactly = 1) { repository.getProductsOverview() }
    }

    @Test
    fun `invoke - returns correct item count`() = runTest {
        coEvery { repository.getProductsOverview() } returns inventory

        val result = useCase()

        assertEquals(2, result.size)
    }

    @Test
    fun `invoke - returns items with correct processId and processName`() = runTest {
        coEvery { repository.getProductsOverview() } returns inventory

        val result = useCase()

        assertEquals(1, result[0].processId)
        assertEquals("Сборка", result[0].processName)
        assertEquals(2, result[1].processId)
        assertEquals("Покраска", result[1].processName)
    }

    @Test
    fun `invoke - returns items with correct stepDefinitionId and stepName`() = runTest {
        coEvery { repository.getProductsOverview() } returns inventory

        val result = useCase()

        assertEquals(10, result[0].stepDefinitionId)
        assertEquals("Контроль качества", result[0].stepName)
    }

    @Test
    fun `invoke - returns items with correct stepNameGenitive`() = runTest {
        coEvery { repository.getProductsOverview() } returns inventory

        val result = useCase()

        assertEquals("Контроля качества", result[0].stepNameGenitive)
        assertEquals("Финишной покраски", result[1].stepNameGenitive)
    }

    @Test
    fun `invoke - returns items with correct count`() = runTest {
        coEvery { repository.getProductsOverview() } returns inventory

        val result = useCase()

        assertEquals(120, result[0].count)
        assertEquals(50, result[1].count)
    }

    @Test
    fun `invoke - returns empty list if repository returns empty`() = runTest {
        coEvery { repository.getProductsOverview() } returns emptyList()

        val result = useCase()

        assertTrue(result.isEmpty())
    }

    @Test
    fun `invoke - returns single item correctly`() = runTest {
        val single = listOf(inventory.first())
        coEvery { repository.getProductsOverview() } returns single

        val result = useCase()

        assertEquals(1, result.size)
        assertEquals(1, result.first().processId)
    }

    @Test
    fun `invoke - handles zero count`() = runTest {
        val zeroItem = ProductsOverview(
            processId = 3,
            processName = "Новый процесс",
            stepDefinitionId = 99,
            stepName = "Начальный шаг",
            stepNameGenitive = "Начального шага",
            count = 0
        )
        coEvery { repository.getProductsOverview() } returns listOf(zeroItem)

        val result = useCase()

        assertEquals(0, result.first().count)
    }

    @Test
    fun `invoke - propagates exception from repository`() = runTest {
        coEvery { repository.getProductsOverview() } throws RuntimeException("DB connection failed")

        val exception = runCatching { useCase() }.exceptionOrNull()

        assertEquals("DB connection failed", exception?.message)
    }
}