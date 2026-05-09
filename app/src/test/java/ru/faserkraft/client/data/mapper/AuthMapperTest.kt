package ru.faserkraft.client.data.mapper

import org.junit.Assert.assertEquals
import org.junit.Test
import ru.faserkraft.client.data.dto.LoginRequestDto
import ru.faserkraft.client.domain.model.LoginCredentials

class AuthMapperTest {

    @Test
    fun `LoginCredentials toDto maps username and password`() {
        val credentials = LoginCredentials(
            username = "worker@faserkraft.ru",
            password = "secret123"
        )

        val dto: LoginRequestDto = credentials.toDto()

        assertEquals("worker@faserkraft.ru", dto.username)
        assertEquals("secret123", dto.password)
    }
}