package ru.faserkraft.client.data.mapper

import org.junit.Assert.assertEquals
import org.junit.Test
import ru.faserkraft.client.data.dto.DeviceRequestDto
import ru.faserkraft.client.data.dto.DeviceResponseDto
import ru.faserkraft.client.domain.model.DeviceRequest

class DeviceMapperTest {

    // ── фикстуры ──────────────────────────────────────────────────────────────

    private val responseDto = DeviceResponseDto(
        accessToken = "mobile-session-token",
        expiresAt = "2026-11-16T10:00:00Z",
        userName = "Иван Петров",
        userEmail = "ivan@faserkraft.ru",
        userRole = "worker",
        deviceId = "device-001",
        model = "Samsung Galaxy A53",
        manufacturer = "Samsung"
    )

    private val requestDto = DeviceRequestDto(
        invitationToken = "invitation-token-xyz",
        deviceId = "device-001",
        model = "Samsung Galaxy A53",
        manufacturer = "Samsung"
    )

    private val domainRequest = DeviceRequest(
        invitationToken = "invitation-token-xyz",
        deviceId = "device-001",
        model = "Samsung Galaxy A53",
        manufacturer = "Samsung"
    )

    // ── DeviceResponseDto.toDomain() ──────────────────────────────────────────

    @Test
    fun `DeviceResponseDto toDomain - maps accessToken`() {
        assertEquals("mobile-session-token", responseDto.toDomain().accessToken)
    }

    @Test
    fun `DeviceResponseDto toDomain - maps expiresAt`() {
        assertEquals("2026-11-16T10:00:00Z", responseDto.toDomain().expiresAt)
    }

    @Test
    fun `DeviceResponseDto toDomain - maps userEmail`() {
        assertEquals("ivan@faserkraft.ru", responseDto.toDomain().userEmail)
    }

    @Test
    fun `DeviceResponseDto toDomain - maps userName`() {
        assertEquals("Иван Петров", responseDto.toDomain().userName)
    }

    @Test
    fun `DeviceResponseDto toDomain - maps userRole`() {
        assertEquals("worker", responseDto.toDomain().userRole)
    }

    // ── DeviceRequest.toDto() ─────────────────────────────────────────────────

    @Test
    fun `DeviceRequest toDto - maps invitationToken`() {
        assertEquals("invitation-token-xyz", domainRequest.toDto().invitationToken)
    }

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

    // ── DeviceRequestDto.toDomain() ───────────────────────────────────────────

    @Test
    fun `DeviceRequestDto toDomain - maps invitationToken`() {
        assertEquals("invitation-token-xyz", requestDto.toDomain().invitationToken)
    }

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

    // ── roundtrip ─────────────────────────────────────────────────────────────

    @Test
    fun `DeviceRequest toDto and back toDomain preserves all fields`() {
        val result = domainRequest.toDto().toDomain()
        assertEquals(domainRequest, result)
    }
}