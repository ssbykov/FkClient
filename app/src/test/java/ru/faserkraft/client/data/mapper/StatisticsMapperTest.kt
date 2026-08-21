package ru.faserkraft.client.data.mapper

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import ru.faserkraft.client.data.dto.DayPlanStepDto
import ru.faserkraft.client.data.dto.EmployeePlanStatDto
import ru.faserkraft.client.data.dto.PeriodStatisticsDto
import ru.faserkraft.client.data.dto.ProcessCountStatDto
import ru.faserkraft.client.data.dto.StepCountStatDto
import ru.faserkraft.client.data.dto.StepDefinitionDto
import ru.faserkraft.client.data.dto.TemplateDto

class StatisticsMapperTest {

    @Test
    fun `StepCountStatDto toDomain maps all fields correctly`() {
        val dto = StepCountStatDto(
            processId = 1,
            processName = "Сборка",
            sizeTypeId = 15,
            sizeTypeName = "100x200",
            stepDefinitionId = 10,
            order = 2,
            stepName = "Установка детали",
            employeeId = 100,
            employeeName = "Иван Иванов",
            count = 15,
        )

        val domain = dto.toDomain()

        assertEquals(1, domain.processId)
        assertEquals("Сборка", domain.processName)
        assertEquals(15, domain.sizeTypeId)
        assertEquals("100x200", domain.sizeTypeName)
        assertEquals(10, domain.stepDefinitionId)
        assertEquals(2, domain.order)
        assertEquals("Установка детали", domain.stepName)
        assertEquals(100, domain.employeeId)
        assertEquals("Иван Иванов", domain.employeeName)
        assertEquals(15, domain.count)
    }

    @Test
    fun `ProcessCountStatDto toDomain maps all fields correctly`() {
        val dto = ProcessCountStatDto(
            processId = 2,
            processName = "Покраска",
            count = 42,
        )

        val domain = dto.toDomain()

        assertEquals(2, domain.processId)
        assertEquals("Покраска", domain.processName)
        assertEquals(42, domain.count)
    }

    @Test
    fun `EmployeePlanStatDto toDomain maps fields and nested steps correctly`() {
        val planStep = createPlanStepDto(
            id = 501,
            dailyPlanId = 50,
            stepDefinitionId = 10,
            plannedQuantity = 20,
            actualQuantity = 7,
            workProcess = "Сборка",
        )

        val dto = EmployeePlanStatDto(
            employeeId = 100,
            employeeName = "Иван Иванов",
            workingDays = 5,
            steps = listOf(planStep),
        )

        val domain = dto.toDomain()

        assertEquals(100, domain.employeeId)
        assertEquals("Иван Иванов", domain.employeeName)
        assertEquals(5, domain.workingDays)

        assertEquals(1, domain.steps.size)
        val domainStep = domain.steps.single()
        assertEquals(501, domainStep.id)
        assertEquals(50, domainStep.dailyPlanId)
        assertEquals(10, domainStep.stepDefinitionId)
        assertEquals(20, domainStep.plannedQuantity)
        assertEquals(7, domainStep.actualQuantity)
        assertEquals("Сборка", domainStep.workProcess)
        assertEquals(10, domainStep.stepDefinition.id)
    }

    @Test
    fun `PeriodStatisticsDto toDomain maps all nested lists correctly`() {
        val processDto = ProcessCountStatDto(
            processId = 1,
            processName = "Сборка",
            count = 10,
        )
        val stepDto = StepCountStatDto(
            processId = 1,
            processName = "Сборка",
            sizeTypeId = 15,
            sizeTypeName = "100x200",
            stepDefinitionId = 10,
            order = 1,
            stepName = "Этап 1",
            employeeId = 100,
            employeeName = "Иван Иванов",
            count = 5,
        )
        val employeePlanDto = EmployeePlanStatDto(
            employeeId = 100,
            employeeName = "Иван Иванов",
            workingDays = 5,
            steps = listOf(
                createPlanStepDto(
                    id = 501,
                    dailyPlanId = 50,
                    stepDefinitionId = 10,
                    plannedQuantity = 8,
                    actualQuantity = 5,
                    workProcess = "Сборка",
                ),
            ),
        )
        val dto = PeriodStatisticsDto(
            finishedProducts = listOf(processDto),
            totalSteps = listOf(stepDto),
            employeePlans = listOf(employeePlanDto),
        )

        val domain = dto.toDomain()

        assertEquals(1, domain.finishedProducts.size)
        assertEquals(1, domain.finishedProducts.single().processId)
        assertEquals("Сборка", domain.finishedProducts.single().processName)
        assertEquals(10, domain.finishedProducts.single().count)

        assertEquals(1, domain.totalSteps.size)
        assertEquals(15, domain.totalSteps.single().sizeTypeId)
        assertEquals("100x200", domain.totalSteps.single().sizeTypeName)
        assertEquals(10, domain.totalSteps.single().stepDefinitionId)
        assertEquals(5, domain.totalSteps.single().count)

        assertEquals(1, domain.employeePlans.size)
        assertEquals(100, domain.employeePlans.single().employeeId)
        assertEquals("Иван Иванов", domain.employeePlans.single().employeeName)
        assertEquals(5, domain.employeePlans.single().workingDays)
        assertEquals(1, domain.employeePlans.single().steps.size)
        assertEquals(8, domain.employeePlans.single().steps.single().plannedQuantity)
    }

    @Test
    fun `EmployeePlanStatDto toDomain maps empty steps`() {
        val dto = EmployeePlanStatDto(
            employeeId = 100,
            employeeName = "Иван Иванов",
            workingDays = 0,
            steps = emptyList(),
        )

        val domain = dto.toDomain()

        assertEquals(100, domain.employeeId)
        assertEquals(0, domain.workingDays)
        assertTrue(domain.steps.isEmpty())
    }

    @Test
    fun `PeriodStatisticsDto toDomain maps empty lists`() {
        val dto = PeriodStatisticsDto(
            finishedProducts = emptyList(),
            totalSteps = emptyList(),
            employeePlans = emptyList(),
        )

        val domain = dto.toDomain()

        assertTrue(domain.finishedProducts.isEmpty())
        assertTrue(domain.totalSteps.isEmpty())
        assertTrue(domain.employeePlans.isEmpty())
    }

    private fun createPlanStepDto(
        id: Int,
        dailyPlanId: Int,
        stepDefinitionId: Int,
        plannedQuantity: Int,
        actualQuantity: Int,
        workProcess: String,
    ): DayPlanStepDto =
        DayPlanStepDto(
            id = id,
            dailyPlanId = dailyPlanId,
            stepDefinitionId = stepDefinitionId,
            plannedQuantity = plannedQuantity,
            actualQuantity = actualQuantity,
            workProcess = workProcess,
            stepDefinition = StepDefinitionDto(
                id = stepDefinitionId,
                order = 1,
                template = TemplateDto(
                    name = "Этап $stepDefinitionId",
                    nameGenitive = "Этапа $stepDefinitionId",
                ),
            ),
        )
}