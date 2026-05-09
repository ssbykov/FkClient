package ru.faserkraft.client.data.mapper

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import ru.faserkraft.client.data.dto.FinishedProcessDto
import ru.faserkraft.client.data.dto.ProcessDto
import ru.faserkraft.client.data.dto.SizeType
import ru.faserkraft.client.data.dto.StepDefinitionDto
import ru.faserkraft.client.data.dto.TemplateDto

class ProcessMapperTest {

    // ── фикстуры ──────────────────────────────────────────────────────────────

    private val templateDto = TemplateDto(
        name = "Сборка",
        nameGenitive = "Сборки"
    )

    private val stepDefinitionDto = StepDefinitionDto(
        id = 1,
        order = 2,
        template = templateDto
    )

    private val sizeType = SizeType(
        id = 3,
        name = "XL",
        packagingCount = 10
    )

    // ── StepDefinitionDto.toDomain() ──────────────────────────────────────────

    @Test
    fun `StepDefinitionDto toDomain - maps id and order`() {
        val result = stepDefinitionDto.toDomain()

        assertEquals(1, result.id)
        assertEquals(2, result.order)
    }

    @Test
    fun `StepDefinitionDto toDomain - maps name from template`() {
        val result = stepDefinitionDto.toDomain()

        assertEquals("Сборка", result.name)
    }

    @Test
    fun `StepDefinitionDto toDomain - maps nameGenitive from template`() {
        val result = stepDefinitionDto.toDomain()

        assertEquals("Сборки", result.nameGenitive)
    }

    // ── ProcessDto.toDomain() ─────────────────────────────────────────────────

    @Test
    fun `ProcessDto toDomain - maps id and name`() {
        val dto = ProcessDto(id = 10, name = "Процесс А", description = "Описание", steps = null)

        val result = dto.toDomain()

        assertEquals(10, result.id)
        assertEquals("Процесс А", result.name)
    }

    @Test
    fun `ProcessDto toDomain - maps description`() {
        val dto = ProcessDto(id = 1, name = "А", description = "Текст", steps = null)

        assertEquals("Текст", dto.toDomain().description)
    }

    @Test
    fun `ProcessDto toDomain - null description returns empty string`() {
        val dto = ProcessDto(id = 1, name = "А", description = null, steps = null)

        assertEquals("", dto.toDomain().description)
    }

    @Test
    fun `ProcessDto toDomain - null steps returns empty list`() {
        val dto = ProcessDto(id = 1, name = "А", description = null, steps = null)

        assertTrue(dto.toDomain().steps.isEmpty())
    }

    @Test
    fun `ProcessDto toDomain - steps list is mapped`() {
        val dto = ProcessDto(
            id = 1,
            name = "А",
            description = null,
            steps = listOf(stepDefinitionDto, stepDefinitionDto.copy(id = 2))
        )

        assertEquals(2, dto.toDomain().steps.size)
    }

    @Test
    fun `ProcessDto toDomain - steps are mapped correctly`() {
        val dto = ProcessDto(
            id = 1,
            name = "А",
            description = null,
            steps = listOf(stepDefinitionDto)
        )

        val step = dto.toDomain().steps.first()
        assertEquals(1, step.id)
        assertEquals("Сборка", step.name)
    }

    // ── FinishedProcessDto.toDomain() ─────────────────────────────────────────

    @Test
    fun `FinishedProcessDto toDomain - maps id and name`() {
        val dto = FinishedProcessDto(id = 7, name = "Финальный", type = sizeType)

        val result = dto.toDomain()

        assertEquals(7, result.id)
        assertEquals("Финальный", result.name)
    }

    @Test
    fun `FinishedProcessDto toDomain - maps sizeTypeId from type`() {
        val dto = FinishedProcessDto(id = 7, name = "Финальный", type = sizeType)

        assertEquals(3, dto.toDomain().sizeTypeId)
    }

    @Test
    fun `FinishedProcessDto toDomain - maps sizeTypeName from type`() {
        val dto = FinishedProcessDto(id = 7, name = "Финальный", type = sizeType)

        assertEquals("XL", dto.toDomain().sizeTypeName)
    }

    @Test
    fun `FinishedProcessDto toDomain - maps packagingCount from type`() {
        val dto = FinishedProcessDto(id = 7, name = "Финальный", type = sizeType)

        assertEquals(10, dto.toDomain().packagingCount)
    }

    @Test
    fun `FinishedProcessDto toDomain - null type returns null sizeTypeId`() {
        val dto = FinishedProcessDto(id = 7, name = "Финальный", type = null)

        assertNull(dto.toDomain().sizeTypeId)
    }

    @Test
    fun `FinishedProcessDto toDomain - null type returns null sizeTypeName`() {
        val dto = FinishedProcessDto(id = 7, name = "Финальный", type = null)

        assertNull(dto.toDomain().sizeTypeName)
    }

    @Test
    fun `FinishedProcessDto toDomain - null type returns null packagingCount`() {
        val dto = FinishedProcessDto(id = 7, name = "Финальный", type = null)

        assertNull(dto.toDomain().packagingCount)
    }
}