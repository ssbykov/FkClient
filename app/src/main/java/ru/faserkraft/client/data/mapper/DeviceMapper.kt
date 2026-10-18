package ru.faserkraft.client.data.mapper

import ru.faserkraft.client.data.dto.DeviceRequestDto
import ru.faserkraft.client.data.dto.DeviceResponseDto
import ru.faserkraft.client.domain.model.DeviceRequest
import ru.faserkraft.client.domain.model.UserRegistration

fun DeviceResponseDto.toDomain(): UserRegistration = UserRegistration(
    accessToken = accessToken,
    refreshToken = refreshToken,
    expiresAt = expiresAt,
    userEmail = userEmail,
    userName = userName,
    userRole = userRole,
)

fun DeviceRequest.toDto() = DeviceRequestDto(
    invitationToken = invitationToken,
    deviceId = deviceId,
    model = model,
    manufacturer = manufacturer,
)

fun DeviceRequestDto.toDomain() = DeviceRequest(
    invitationToken = invitationToken,
    deviceId = deviceId,
    model = model,
    manufacturer = manufacturer,
)