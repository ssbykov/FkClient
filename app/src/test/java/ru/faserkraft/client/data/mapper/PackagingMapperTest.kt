package ru.faserkraft.client.data.mapper

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import ru.faserkraft.client.data.dto.EmployeeDto
import ru.faserkraft.client.data.dto.FinishedProcessDto
import ru.faserkraft.client.data.dto.FinishedProductDto
import ru.faserkraft.client.data.dto.PackagingDto
import ru.faserkraft.client.data.dto.UserDto

class PackagingMapperTest {

    // ── фикстуры ──────────────────────────────────────────────────────────────

    private val employeeDto = EmployeeDto(
        id = 1,
        name = "Иван Петров",
        user = UserDto(id = 1, email = "ivan@faserkraft.ru")
    )

    private val finishedProductDto = FinishedProductDto(
        id = 10,
        serialNumber = "FIN-001",
        process = FinishedProcessDto(id = 3, name = "Финальный", type = null)
    )

    private val fullDto = PackagingDto(
        id = 5,
        serialNumber = "PKG-100",
        performedBy = employeeDto,
        performedAt = "2024-06-01T12:00:00",
        orderId = 7,
        products = listOf(finishedProductDto)
    )

    // ── id, serialNumber ──────────────────────────────────────────────────────

    @Test
    fun `toDomain - maps id`() {
        assertEquals(5, fullDto.toDomain().id)
    }

    @Test
    fun `toDomain - maps serialNumber`() {
        assertEquals("PKG-100", fullDto.toDomain().serialNumber)
    }

    // ── performedBy ───────────────────────────────────────────────────────────

    @Test
    fun `toDomain - maps performedBy name`() {
        assertEquals("Иван Петров", fullDto.toDomain().performedBy?.name)
    }

    @Test
    fun `toDomain - maps performedBy email from nested user`() {
        assertEquals("ivan@faserkraft.ru", fullDto.toDomain().performedBy?.email)
    }

    @Test
    fun `toDomain - null performedBy returns null`() {
        val dto = fullDto.copy(performedBy = null)
        assertNull(dto.toDomain().performedBy)
    }

    // ── performedAt ───────────────────────────────────────────────────────────

    @Test
    fun `toDomain - maps performedAt`() {
        assertEquals("2024-06-01T12:00:00", fullDto.toDomain().performedAt)
    }

    @Test
    fun `toDomain - null performedAt returns null`() {
        val dto = fullDto.copy(performedAt = null)
        assertNull(dto.toDomain().performedAt)
    }

    // ── orderId ───────────────────────────────────────────────────────────────

    @Test
    fun `toDomain - maps orderId`() {
        assertEquals(7, fullDto.toDomain().orderId)
    }

    @Test
    fun `toDomain - null orderId returns null`() {
        val dto = fullDto.copy(orderId = null)
        assertNull(dto.toDomain().orderId)
    }

    // ── products ──────────────────────────────────────────────────────────────

    @Test
    fun `toDomain - maps products list size`() {
        assertEquals(1, fullDto.toDomain().products.size)
    }

    @Test
    fun `toDomain - maps product serialNumber`() {
        assertEquals("FIN-001", fullDto.toDomain().products.first().serialNumber)
    }

    @Test
    fun `toDomain - null products returns empty list`() {
        val dto = fullDto.copy(products = null)
        assertTrue(dto.toDomain().products.isEmpty())
    }

    @Test
    fun `toDomain - empty products returns empty list`() {
        val dto = fullDto.copy(products = emptyList())
        assertTrue(dto.toDomain().products.isEmpty())
    }

    @Test
    fun `toDomain - multiple products are all mapped`() {
        val dto = fullDto.copy(
            products = listOf(
                finishedProductDto,
                finishedProductDto.copy(id = 11, serialNumber = "FIN-002")
            )
        )
        assertEquals(2, dto.toDomain().products.size)
        assertEquals("FIN-002", dto.toDomain().products[1].serialNumber)
    }
}