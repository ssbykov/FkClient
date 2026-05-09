package ru.faserkraft.client.data.mapper

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import ru.faserkraft.client.data.dto.DayPlanDto
import ru.faserkraft.client.data.dto.DayPlanStepDto
import ru.faserkraft.client.data.dto.EmployeeDto
import ru.faserkraft.client.data.dto.StepDefinitionDto
import ru.faserkraft.client.data.dto.TemplateDto
import ru.faserkraft.client.data.dto.UserDto

class DailyPlanMapperTest {

    // ── фикстуры ──────────────────────────────────────────────────────────────

    private val employeeDto = EmployeeDto(
        id = 1,
        name = "Анна Смирнова",
        user = UserDto(id = 1, email = "anna@faserkraft.ru")
    )

    private val stepDefinitionDto = StepDefinitionDto(
        id = 2,
        order = 1,
        template = TemplateDto(name = "Сборка", nameGenitive = "Сборки")
    )

    private val stepDto = DayPlanStepDto(
        id = 10,
        dailyPlanId = 5,
        stepDefinitionId = 2,
        plannedQuantity = 50,
        actualQuantity = 45,
        workProcess = "Сборка",
        stepDefinition = stepDefinitionDto
    )

    private val planDto = DayPlanDto(
        id = 5,
        employeeId = 1,
        date = "2024-06-10",
        employee = employeeDto,
        steps = listOf(stepDto)
    )

    // ── DayPlanStepDto.toDomain() ─────────────────────────────────────────────

    @Test
    fun `DayPlanStepDto toDomain - maps id`() {
        assertEquals(10, stepDto.toDomain().id)
    }

    @Test
    fun `DayPlanStepDto toDomain - maps dailyPlanId`() {
        assertEquals(5, stepDto.toDomain().dailyPlanId)
    }

    @Test
    fun `DayPlanStepDto toDomain - maps stepDefinitionId`() {
        assertEquals(2, stepDto.toDomain().stepDefinitionId)
    }

    @Test
    fun `DayPlanStepDto toDomain - maps plannedQuantity`() {
        assertEquals(50, stepDto.toDomain().plannedQuantity)
    }

    @Test
    fun `DayPlanStepDto toDomain - maps actualQuantity`() {
        assertEquals(45, stepDto.toDomain().actualQuantity)
    }

    @Test
    fun `DayPlanStepDto toDomain - maps workProcess as string`() {
        assertEquals("Сборка", stepDto.toDomain().workProcess)
    }

    @Test
    fun `DayPlanStepDto toDomain - maps stepDefinition name`() {
        assertEquals("Сборка", stepDto.toDomain().stepDefinition.name)
    }

    @Test
    fun `DayPlanStepDto toDomain - maps stepDefinition nameGenitive`() {
        assertEquals("Сборки", stepDto.toDomain().stepDefinition.nameGenitive)
    }

    @Test
    fun `DayPlanStepDto toDomain - maps stepDefinition order`() {
        assertEquals(1, stepDto.toDomain().stepDefinition.order)
    }

    @Test
    fun `DayPlanStepDto toDomain - zero actualQuantity is preserved`() {
        val dto = stepDto.copy(actualQuantity = 0)
        assertEquals(0, dto.toDomain().actualQuantity)
    }

    @Test
    fun `DayPlanStepDto toDomain - zero plannedQuantity is preserved`() {
        val dto = stepDto.copy(plannedQuantity = 0)
        assertEquals(0, dto.toDomain().plannedQuantity)
    }

    // ── DayPlanDto.toDomain() ─────────────────────────────────────────────────

    @Test
    fun `DayPlanDto toDomain - maps id`() {
        assertEquals(5, planDto.toDomain().id)
    }

    @Test
    fun `DayPlanDto toDomain - maps employeeId`() {
        assertEquals(1, planDto.toDomain().employeeId)
    }

    @Test
    fun `DayPlanDto toDomain - maps date`() {
        assertEquals("2024-06-10", planDto.toDomain().date)
    }

    @Test
    fun `DayPlanDto toDomain - maps employee name`() {
        assertEquals("Анна Смирнова", planDto.toDomain().employee.name)
    }

    @Test
    fun `DayPlanDto toDomain - maps employee email from nested user`() {
        assertEquals("anna@faserkraft.ru", planDto.toDomain().employee.email)
    }

    @Test
    fun `DayPlanDto toDomain - maps steps list size`() {
        assertEquals(1, planDto.toDomain().steps.size)
    }

    @Test
    fun `DayPlanDto toDomain - empty steps returns empty list`() {
        val dto = planDto.copy(steps = emptyList())
        assertTrue(dto.toDomain().steps.isEmpty())
    }

    @Test
    fun `DayPlanDto toDomain - multiple steps are all mapped`() {
        val dto = planDto.copy(
            steps = listOf(
                stepDto,
                stepDto.copy(id = 11, plannedQuantity = 30)
            )
        )
        val result = dto.toDomain()
        assertEquals(2, result.steps.size)
        assertEquals(30, result.steps[1].plannedQuantity)
    }

    @Test
    fun `DayPlanDto toDomain - step workProcess string is preserved`() {
        assertEquals("Сборка", planDto.toDomain().steps.first().workProcess)
    }
}