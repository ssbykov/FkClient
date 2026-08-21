package ru.faserkraft.client.domain.usecase.statistics

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import ru.faserkraft.client.domain.model.PeriodStatistics
import ru.faserkraft.client.domain.model.ProcessCountStat
import ru.faserkraft.client.domain.model.StepCountStat
import ru.faserkraft.client.domain.repository.ProductRepository
import ru.faserkraft.client.domain.usecase.statistic.GetProductsStatisticsUseCase

class GetProductsStatisticsUseCaseTest {

    private val repository: ProductRepository = mockk()
    private lateinit var useCase: GetProductsStatisticsUseCase

    @Before
    fun setUp() {
        useCase = GetProductsStatisticsUseCase(repository)
    }

    @Test
    fun `invoke should return statistics from repository`() = runTest {
        // Arrange
        val dateFrom = "2024-05-01"
        val dateTo = "2024-05-31"

        val expectedStats = PeriodStatistics(
            finishedProducts = listOf(
                ProcessCountStat(processId = 1, processName = "Process A", count = 10)
            ),
            totalSteps = listOf(
                StepCountStat(
                    processId = 1,
                    processName = "Process A",
                    sizeTypeId = 15,
                    sizeTypeName = "100x200",
                    stepDefinitionId = 10,
                    order = 1,
                    stepName = "Step A",
                    employeeId = 100,
                    employeeName = "Emp A",
                    count = 5
                )
            ),
            employeePlans = emptyList() // ← добавлено поле
        )

        coEvery {
            repository.getFinishedProductsByPeriod(dateFrom, dateTo)
        } returns expectedStats

        // Act
        val result = useCase(dateFrom, dateTo)

        // Assert
        assertEquals(expectedStats, result)

        // Проверяем, что в репозиторий ушли правильные параметры
        coVerify(exactly = 1) {
            repository.getFinishedProductsByPeriod(dateFrom, dateTo)
        }
    }

    @Test(expected = Exception::class)
    fun `invoke should propagate exception from repository`() = runTest {
        // Arrange
        val dateFrom = "2024-05-01"
        val dateTo = "2024-05-31"

        coEvery {
            repository.getFinishedProductsByPeriod(dateFrom, dateTo)
        } throws Exception("Repository error")

        // Act & Assert
        useCase(dateFrom, dateTo)
    }
}