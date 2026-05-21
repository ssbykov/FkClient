package ru.faserkraft.client.data.mapper

import org.junit.Assert.assertEquals
import org.junit.Test
import ru.faserkraft.client.data.dto.PeriodStatisticsDto
import ru.faserkraft.client.data.dto.ProcessCountStatDto
import ru.faserkraft.client.data.dto.StepCountStatDto

class StatisticsMapperTest {

    @Test
    fun `StepCountStatDto toDomain maps all fields correctly`() {
        // Arrange
        val dto = StepCountStatDto(
            processId = 1,
            processName = "Сборка",
            stepDefinitionId = 10,
            order = 2,
            stepName = "Установка детали",
            employeeId = 100,
            employeeName = "Иван Иванов",
            count = 15
        )

        // Act
        val domain = dto.toDomain()

        // Assert
        assertEquals(dto.processId, domain.processId)
        assertEquals(dto.processName, domain.processName)
        assertEquals(dto.stepDefinitionId, domain.stepDefinitionId)
        assertEquals(dto.order, domain.order)
        assertEquals(dto.stepName, domain.stepName)
        assertEquals(dto.employeeId, domain.employeeId)
        assertEquals(dto.employeeName, domain.employeeName)
        assertEquals(dto.count, domain.count)
    }

    @Test
    fun `ProcessCountStatDto toDomain maps all fields correctly`() {
        // Arrange
        val dto = ProcessCountStatDto(
            processId = 2,
            processName = "Покраска",
            count = 42
        )

        // Act
        val domain = dto.toDomain()

        // Assert
        assertEquals(dto.processId, domain.processId)
        assertEquals(dto.processName, domain.processName)
        assertEquals(dto.count, domain.count)
    }

    @Test
    fun `PeriodStatisticsDto toDomain maps nested lists correctly`() {
        // Arrange
        val processDto = ProcessCountStatDto(
            processId = 1,
            processName = "Процесс 1",
            count = 10
        )
        val stepDto = StepCountStatDto(
            processId = 1,
            processName = "Процесс 1",
            stepDefinitionId = 10,
            order = 1,
            stepName = "Этап 1",
            employeeId = 100,
            employeeName = "Сотрудник 1",
            count = 5
        )

        val dto = PeriodStatisticsDto(
            finishedProducts = listOf(processDto),
            totalSteps = listOf(stepDto)
        )

        // Act
        val domain = dto.toDomain()

        // Assert
        assertEquals(1, domain.finishedProducts.size)
        // Если модели data class, можно сравнивать объекты целиком,
        // но здесь проверяем ключевое поле для надежности
        assertEquals(processDto.processId, domain.finishedProducts.first().processId)
        assertEquals(processDto.count, domain.finishedProducts.first().count)

        assertEquals(1, domain.totalSteps.size)
        assertEquals(stepDto.stepDefinitionId, domain.totalSteps.first().stepDefinitionId)
        assertEquals(stepDto.order, domain.totalSteps.first().order)
    }

    @Test
    fun `PeriodStatisticsDto toDomain handles empty lists correctly`() {
        // Arrange
        val dto = PeriodStatisticsDto(
            finishedProducts = emptyList(),
            totalSteps = emptyList()
        )

        // Act
        val domain = dto.toDomain()

        // Assert
        assertEquals(0, domain.finishedProducts.size)
        assertEquals(0, domain.totalSteps.size)
    }
}