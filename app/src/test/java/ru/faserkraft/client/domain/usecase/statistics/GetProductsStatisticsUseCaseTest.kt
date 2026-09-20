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
import java.math.BigDecimal

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
                    templateId = 101, // Добавлен templateId
                    order = 1,
                    stepName = "Step A",
                    employeeId = 100,
                    employeeName = "Emp A",
                    count = 5,
                    totalAmount = BigDecimal("500.00") // Добавлен totalAmount (в доменной модели BigDecimal)
                )
            ),
            employeePlans = emptyList(),
            employeeEarnings = emptyList(), // Добавлен employeeEarnings
            firstHalfEarnings = emptyList(), // Добавлен firstHalfEarnings
            totalWorkingDays = 5, // Добавлен totalWorkingDays
            totalEarnedAll = BigDecimal("500.00") // Добавлен totalEarnedAll
        )

        coEvery {
            repository.getPeriodStatistics(dateFrom, dateTo, any())
        } returns expectedStats

        // Act
        val result = useCase(dateFrom, dateTo)

        // Assert
        assertEquals(expectedStats, result)

        // Проверяем, что в репозиторий ушли правильные параметры
        coVerify(exactly = 1) {
            repository.getPeriodStatistics(dateFrom, dateTo, any())
        }
    }

    @Test(expected = Exception::class)
    fun `invoke should propagate exception from repository`() = runTest {
        // Arrange
        val dateFrom = "2024-05-01"
        val dateTo = "2024-05-31"

        coEvery {
            repository.getPeriodStatistics(dateFrom, dateTo, any())
        } throws Exception("Repository error")

        // Act & Assert
        useCase(dateFrom, dateTo)
    }
}
