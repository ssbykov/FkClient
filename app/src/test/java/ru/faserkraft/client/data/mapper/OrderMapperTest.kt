package ru.faserkraft.client.data.mapper

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import ru.faserkraft.client.data.dto.EmployeeDto
import ru.faserkraft.client.data.dto.OrderDto
import ru.faserkraft.client.data.dto.OrderItemDto
import ru.faserkraft.client.data.dto.ProcessDto
import ru.faserkraft.client.data.dto.UserDto
import ru.faserkraft.client.domain.model.OrderItem
import ru.faserkraft.client.domain.model.Process

class OrderMapperTest {

    // ── фикстуры ──────────────────────────────────────────────────────────────

    private val processDto = ProcessDto(
        id = 1,
        name = "Сборка",
        description = "Описание",
        steps = emptyList()
    )

    private val employeeDto = EmployeeDto(
        id = 10,
        name = "Анна Смирнова",
        user = UserDto(id = 10, email = "anna@faserkraft.ru")
    )

    private val orderItemDto = OrderItemDto(
        id = 5,
        quantity = 20,
        workProcess = processDto
    )

    private val fullOrderDto = OrderDto(
        id = 100,
        contractNumber = "КД-2024-001",
        contractDate = "2024-01-15",
        plannedShipmentDate = "2024-03-01",
        shipmentDate = "2024-03-05",
        shipmentBy = employeeDto,
        items = listOf(orderItemDto),
        packaging = emptyList()
    )

    private val domainProcess = Process(
        id = 3,
        name = "Упаковка",
        description = "",
        steps = emptyList()
    )

    private val domainOrderItem = OrderItem(
        id = 7,
        quantity = 15,
        workProcess = domainProcess
    )

    // ── OrderItemDto.toDomain() ───────────────────────────────────────────────

    @Test
    fun `OrderItemDto toDomain - maps id`() {
        assertEquals(5, orderItemDto.toDomain().id)
    }

    @Test
    fun `OrderItemDto toDomain - maps quantity`() {
        assertEquals(20, orderItemDto.toDomain().quantity)
    }

    @Test
    fun `OrderItemDto toDomain - maps workProcess id`() {
        assertEquals(1, orderItemDto.toDomain().workProcess.id)
    }

    @Test
    fun `OrderItemDto toDomain - maps workProcess name`() {
        assertEquals("Сборка", orderItemDto.toDomain().workProcess.name)
    }

    // ── OrderDto.toDomain() ───────────────────────────────────────────────────

    @Test
    fun `OrderDto toDomain - maps id`() {
        assertEquals(100, fullOrderDto.toDomain().id)
    }

    @Test
    fun `OrderDto toDomain - maps contractNumber`() {
        assertEquals("КД-2024-001", fullOrderDto.toDomain().contractNumber)
    }

    @Test
    fun `OrderDto toDomain - maps contractDate`() {
        assertEquals("2024-01-15", fullOrderDto.toDomain().contractDate)
    }

    @Test
    fun `OrderDto toDomain - maps plannedShipmentDate`() {
        assertEquals("2024-03-01", fullOrderDto.toDomain().plannedShipmentDate)
    }

    @Test
    fun `OrderDto toDomain - maps shipmentDate`() {
        assertEquals("2024-03-05", fullOrderDto.toDomain().shipmentDate)
    }

    @Test
    fun `OrderDto toDomain - null shipmentDate returns null`() {
        val dto = fullOrderDto.copy(shipmentDate = null)
        assertNull(dto.toDomain().shipmentDate)
    }

    @Test
    fun `OrderDto toDomain - maps shipmentBy name`() {
        assertEquals("Анна Смирнова", fullOrderDto.toDomain().shipmentBy?.name)
    }

    @Test
    fun `OrderDto toDomain - maps shipmentBy email from nested user`() {
        assertEquals("anna@faserkraft.ru", fullOrderDto.toDomain().shipmentBy?.email)
    }

    @Test
    fun `OrderDto toDomain - null shipmentBy returns null`() {
        val dto = fullOrderDto.copy(shipmentBy = null)
        assertNull(dto.toDomain().shipmentBy)
    }

    @Test
    fun `OrderDto toDomain - maps items list`() {
        assertEquals(1, fullOrderDto.toDomain().items.size)
    }

    @Test
    fun `OrderDto toDomain - null items returns empty list`() {
        val dto = fullOrderDto.copy(items = null)
        assertTrue(dto.toDomain().items.isEmpty())
    }

    @Test
    fun `OrderDto toDomain - empty items returns empty list`() {
        val dto = fullOrderDto.copy(items = emptyList())
        assertTrue(dto.toDomain().items.isEmpty())
    }

    @Test
    fun `OrderDto toDomain - null packaging returns empty list`() {
        val dto = fullOrderDto.copy(packaging = null)
        assertTrue(dto.toDomain().packaging.isEmpty())
    }

    @Test
    fun `OrderDto toDomain - empty packaging returns empty list`() {
        assertTrue(fullOrderDto.toDomain().packaging.isEmpty())
    }

    // ── OrderItem.toCreateDto() ───────────────────────────────────────────────

    @Test
    fun `OrderItem toCreateDto - maps processId from workProcess`() {
        assertEquals(3, domainOrderItem.toCreateDto().processId)
    }

    @Test
    fun `OrderItem toCreateDto - maps quantity`() {
        assertEquals(15, domainOrderItem.toCreateDto().quantity)
    }

    @Test
    fun `OrderItem toCreateDto - processId comes from workProcess id not item id`() {
        val item = domainOrderItem.copy(
            id = 999,
            workProcess = domainProcess.copy(id = 42)
        )
        assertEquals(42, item.toCreateDto().processId)
    }
}