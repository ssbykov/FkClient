package ru.faserkraft.client.data.mapper

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import ru.faserkraft.client.data.dto.EmployeeDto
import ru.faserkraft.client.data.dto.FinishedProcessDto
import ru.faserkraft.client.data.dto.PackagingShortDto
import ru.faserkraft.client.data.dto.ProcessDto
import ru.faserkraft.client.data.dto.ProductDto
import ru.faserkraft.client.data.dto.ProductStatusDto
import ru.faserkraft.client.data.dto.ProductShortDto
import ru.faserkraft.client.data.dto.ProductsOverviewDto
import ru.faserkraft.client.data.dto.StepDefinitionDto
import ru.faserkraft.client.data.dto.StepDto
import ru.faserkraft.client.data.dto.TemplateDto
import ru.faserkraft.client.data.dto.UserDto
import ru.faserkraft.client.domain.model.ProductStatus
import ru.faserkraft.client.domain.model.StepStatus

class ProductMapperTest {

    // ── вспомогательные фикстуры ──────────────────────────────────────────────

    private val userDto = UserDto(id = 1, email = "ivan@faserkraft.ru")

    private val employeeDto = EmployeeDto(id = 1, name = "Иван", user = userDto)

    private val stepDefinitionDto = StepDefinitionDto(
        id = 2,
        order = 1,
        template = TemplateDto(name = "Сборка", nameGenitive = "Сборки")
    )

    private val processDto = ProcessDto(
        id = 10,
        name = "Процесс А",
        description = null,
        steps = null
    )

    private fun makeStepDto(status: String, performedBy: EmployeeDto? = null) = StepDto(
        id = 1,
        productId = 100,
        stepDefinition = stepDefinitionDto,
        status = status,
        performedById = performedBy?.id,
        performedBy = performedBy,
        performedAt = "2024-01-01T10:00:00"
    )

    private fun makeProductDto(
        packaging: PackagingShortDto? = PackagingShortDto(serialNumber = "PKG-01"),
        status: ProductStatusDto = ProductStatusDto.NORMAL,
        steps: List<StepDto> = listOf(makeStepDto("done"))
    ) = ProductDto(
        id = 100L,
        serialNumber = "SN-001",
        process = processDto,
        createdAt = "2024-01-01",
        packaging = packaging,
        status = status,
        steps = steps
    )

    // ── StepDto.toDomain() ────────────────────────────────────────────────────

    @Test
    fun `StepDto toDomain - status 'done' maps to DONE`() {
        val result = makeStepDto("done").toDomain()
        assertEquals(StepStatus.DONE, result.status)
    }

    @Test
    fun `StepDto toDomain - status 'DONE' uppercase maps to DONE`() {
        val result = makeStepDto("DONE").toDomain()
        assertEquals(StepStatus.DONE, result.status)
    }

    @Test
    fun `StepDto toDomain - unknown status maps to PENDING`() {
        val result = makeStepDto("in_progress").toDomain()
        assertEquals(StepStatus.PENDING, result.status)
    }

    @Test
    fun `StepDto toDomain - empty status maps to PENDING`() {
        val result = makeStepDto("").toDomain()
        assertEquals(StepStatus.PENDING, result.status)
    }

    @Test
    fun `StepDto toDomain - null performedBy maps to null`() {
        val result = makeStepDto("done", performedBy = null).toDomain()
        assertNull(result.performedBy)
    }

    @Test
    fun `StepDto toDomain - performedBy maps employee correctly`() {
        val result = makeStepDto("done", performedBy = employeeDto).toDomain()
        assertEquals("Иван", result.performedBy?.name)
        assertEquals("ivan@faserkraft.ru", result.performedBy?.email)
    }

    @Test
    fun `StepDto toDomain - maps id, productId and performedAt`() {
        val result = makeStepDto("done").toDomain()
        assertEquals(1, result.id)
        assertEquals(100, result.productId)
        assertEquals("2024-01-01T10:00:00", result.performedAt)
    }

    @Test
    fun `StepDto toDomain - stepDefinition maps name correctly`() {
        val result = makeStepDto("done").toDomain()
        assertEquals("Сборка", result.definition.name)
        assertEquals("Сборки", result.definition.nameGenitive)
    }

    // ── ProductDto.toDomain() ─────────────────────────────────────────────────

    @Test
    fun `ProductDto toDomain - maps id and serialNumber`() {
        val result = makeProductDto().toDomain()
        assertEquals(100L, result.id)
        assertEquals("SN-001", result.serialNumber)
    }

    @Test
    fun `ProductDto toDomain - packaging serialNumber mapped`() {
        val result = makeProductDto(packaging = PackagingShortDto("PKG-01")).toDomain()
        assertEquals("PKG-01", result.packagingSerialNumber)
    }

    @Test
    fun `ProductDto toDomain - null packaging returns null serialNumber`() {
        val result = makeProductDto(packaging = null).toDomain()
        assertNull(result.packagingSerialNumber)
    }

    @Test
    fun `ProductDto toDomain - steps list is mapped`() {
        val result = makeProductDto(steps = listOf(makeStepDto("done"), makeStepDto("pending"))).toDomain()
        assertEquals(2, result.steps.size)
    }

    @Test
    fun `ProductDto toDomain - empty steps returns empty list`() {
        val result = makeProductDto(steps = emptyList()).toDomain()
        assertTrue(result.steps.isEmpty())
    }

    @Test
    fun `ProductDto toDomain - process is mapped`() {
        val result = makeProductDto().toDomain()
        assertEquals(10, result.process.id)
        assertEquals("Процесс А", result.process.name)
    }

    // ── ProductStatusDto.toDomain() ───────────────────────────────────────────

    @Test
    fun `ProductStatusDto NORMAL maps to ProductStatus NORMAL`() {
        assertEquals(ProductStatus.NORMAL, ProductStatusDto.NORMAL.toDomain())
    }

    @Test
    fun `ProductStatusDto REWORK maps to ProductStatus REWORK`() {
        assertEquals(ProductStatus.REWORK, ProductStatusDto.REWORK.toDomain())
    }

    @Test
    fun `ProductStatusDto SCRAP maps to ProductStatus SCRAP`() {
        assertEquals(ProductStatus.SCRAP, ProductStatusDto.SCRAP.toDomain())
    }

    // ── ProductStatus.toDto() ─────────────────────────────────────────────────

    @Test
    fun `ProductStatus NORMAL toDto returns 'normal'`() {
        assertEquals("normal", ProductStatus.NORMAL.toDto())
    }

    @Test
    fun `ProductStatus REWORK toDto returns 'rework'`() {
        assertEquals("rework", ProductStatus.REWORK.toDto())
    }

    @Test
    fun `ProductStatus SCRAP toDto returns 'scrap'`() {
        assertEquals("scrap", ProductStatus.SCRAP.toDto())
    }

    // ── ProductShortDto.toDomain() ────────────────────────────────────────────

    @Test
    fun `ProductShortDto toDomain - maps id and serialNumber`() {
        val dto = ProductShortDto(
            id = 5,
            serialNumber = "FIN-001",
            process = FinishedProcessDto(id = 3, name = "Финальный", type = null),
            status = ProductStatusDto.NORMAL
        )

        val result = dto.toDomain()

        assertEquals(5, result.id)
        assertEquals("FIN-001", result.serialNumber)
    }

    @Test
    fun `ProductShortDto toDomain - process is mapped`() {
        val dto = ProductShortDto(
            id = 5,
            serialNumber = "FIN-001",
            process = FinishedProcessDto(id = 3, name = "Финальный", type = null),
            status = ProductStatusDto.NORMAL
        )

        assertEquals(3, dto.toDomain().process.id)
        assertEquals("Финальный", dto.toDomain().process.name)
    }

    @Test
    fun `ProductShortDto toDomain - status is mapped`() {
        val dto = ProductShortDto(
            id = 5,
            serialNumber = "FIN-001",
            process = FinishedProcessDto(id = 3, name = "Финальный", type = null),
            status = ProductStatusDto.REWORK
        )

        assertEquals(ProductStatus.REWORK, dto.toDomain().status)
    }

    // ── ProductsOverviewDto.toDomain() ───────────────────────────────────────

    @Test
    fun `ProductsOverviewDto toDomain - maps all fields`() {
        val dto = ProductsOverviewDto(
            processId = 1,
            processName = "Процесс",
            stepDefinitionId = 2,
            stepName = "Сборка",
            stepNameGenitive = "Сборки",
            count = 15
        )

        val result = dto.toDomain()

        assertEquals(1, result.processId)
        assertEquals("Процесс", result.processName)
        assertEquals(2, result.stepDefinitionId)
        assertEquals("Сборка", result.stepName)
        assertEquals("Сборки", result.stepNameGenitive)
        assertEquals(15, result.count)
    }

    @Test
    fun `ProductsOverviewDto toDomain - zero count is preserved`() {
        val dto = ProductsOverviewDto(
            processId = 1, processName = "П", stepDefinitionId = 1,
            stepName = "С", stepNameGenitive = "С", count = 0
        )

        assertEquals(0, dto.toDomain().count)
    }
}