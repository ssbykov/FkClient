package ru.faserkraft.client.data.mapper

import org.junit.Assert.assertEquals
import org.junit.Test
import ru.faserkraft.client.data.dto.DeviceRequestDto
import ru.faserkraft.client.data.dto.DeviceResponseDto
import ru.faserkraft.client.domain.model.DeviceRequest

class DeviceMapperTest {

    // ── фикстуры ──────────────────────────────────────────────────────────────

    private val responseDto = DeviceResponseDto(
        userName = "Иван Петров",
        userEmail = "ivan@faserkraft.ru",
        userRole = "worker",
        deviceId = "device-001",
        model = "Samsung Galaxy A53",
        manufacturer = "Samsung"
    )

    private val requestDto = DeviceRequestDto(
        deviceId = "device-001",
        model = "Samsung Galaxy A53",
        manufacturer = "Samsung",
        token = "fcm-token-xyz",
        password = "pass1234",
        userId = 5
    )

    private val domainRequest = DeviceRequest(
        deviceId = "device-001",
        model = "Samsung Galaxy A53",
        manufacturer = "Samsung",
        token = "fcm-token-xyz",
        password = "pass1234",
        userId = 5
    )

    // ── DeviceResponseDto.toDomain(password) ──────────────────────────────────

    @Test
    fun `DeviceResponseDto toDomain - maps userEmail`() {
        val result = responseDto.toDomain(password = "generated")
        assertEquals("ivan@faserkraft.ru", result.userEmail)
    }

    @Test
    fun `DeviceResponseDto toDomain - maps userName`() {
        val result = responseDto.toDomain(password = "generated")
        assertEquals("Иван Петров", result.userName)
    }

    @Test
    fun `DeviceResponseDto toDomain - maps userRole`() {
        val result = responseDto.toDomain(password = "generated")
        assertEquals("worker", result.userRole)
    }

    @Test
    fun `DeviceResponseDto toDomain - password comes from parameter not dto`() {
        val result = responseDto.toDomain(password = "generated_pass")
        assertEquals("generated_pass", result.password)
    }

    @Test
    fun `DeviceResponseDto toDomain - different passwords produce different results`() {
        val result1 = responseDto.toDomain(password = "pass_A")
        val result2 = responseDto.toDomain(password = "pass_B")
        assertEquals("pass_A", result1.password)
        assertEquals("pass_B", result2.password)
    }

    // ── DeviceRequest.toDto() ─────────────────────────────────────────────────

    @Test
    fun `DeviceRequest toDto - maps deviceId`() {
        assertEquals("device-001", domainRequest.toDto().deviceId)
    }

    @Test
    fun `DeviceRequest toDto - maps model`() {
        assertEquals("Samsung Galaxy A53", domainRequest.toDto().model)
    }

    @Test
    fun `DeviceRequest toDto - maps manufacturer`() {
        assertEquals("Samsung", domainRequest.toDto().manufacturer)
    }

    @Test
    fun `DeviceRequest toDto - maps token`() {
        assertEquals("fcm-token-xyz", domainRequest.toDto().token)
    }

    @Test
    fun `DeviceRequest toDto - maps password`() {
        assertEquals("pass1234", domainRequest.toDto().password)
    }

    @Test
    fun `DeviceRequest toDto - maps userId`() {
        assertEquals(5, domainRequest.toDto().userId)
    }

    // ── DeviceRequestDto.toDomain() ───────────────────────────────────────────

    @Test
    fun `DeviceRequestDto toDomain - maps deviceId`() {
        assertEquals("device-001", requestDto.toDomain().deviceId)
    }

    @Test
    fun `DeviceRequestDto toDomain - maps model`() {
        assertEquals("Samsung Galaxy A53", requestDto.toDomain().model)
    }

    @Test
    fun `DeviceRequestDto toDomain - maps manufacturer`() {
        assertEquals("Samsung", requestDto.toDomain().manufacturer)
    }

    @Test
    fun `DeviceRequestDto toDomain - maps token`() {
        assertEquals("fcm-token-xyz", requestDto.toDomain().token)
    }

    @Test
    fun `DeviceRequestDto toDomain - maps password`() {
        assertEquals("pass1234", requestDto.toDomain().password)
    }

    @Test
    fun `DeviceRequestDto toDomain - maps userId`() {
        assertEquals(5, requestDto.toDomain().userId)
    }

    // ── roundtrip ─────────────────────────────────────────────────────────────

    @Test
    fun `DeviceRequest toDto and back toDomain preserves all fields`() {
        val result = domainRequest.toDto().toDomain()
        assertEquals(domainRequest, result)
    }
}