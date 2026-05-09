package ru.faserkraft.client.data.mapper

import org.junit.Assert.assertEquals
import org.junit.Test
import ru.faserkraft.client.data.dto.EmployeeDto
import ru.faserkraft.client.data.dto.UserDto

class EmployeeMapperTest {

    // ── фикстура ──────────────────────────────────────────────────────────────

    private val dto = EmployeeDto(
        id = 42,
        name = "Иван Петров",
        user = UserDto(id = 1, email = "ivan@faserkraft.ru")
    )

    // ── EmployeeDto.toDomain() ────────────────────────────────────────────────

    @Test
    fun `toDomain - maps id`() {
        assertEquals(42, dto.toDomain().id)
    }

    @Test
    fun `toDomain - maps name`() {
        assertEquals("Иван Петров", dto.toDomain().name)
    }

    @Test
    fun `toDomain - maps email from nested user`() {
        assertEquals("ivan@faserkraft.ru", dto.toDomain().email)
    }

    @Test
    fun `toDomain - email comes from user not from dto root`() {
        val dtoWithDifferentUser = EmployeeDto(
            id = 1,
            name = "Другой",
            user = UserDto(id = 99, email = "other@faserkraft.ru")
        )
        assertEquals("other@faserkraft.ru", dtoWithDifferentUser.toDomain().email)
    }
}