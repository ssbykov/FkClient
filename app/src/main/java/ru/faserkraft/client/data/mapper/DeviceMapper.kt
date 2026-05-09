package ru.faserkraft.client.data.mapper

import ru.faserkraft.client.domain.model.DeviceRequest
import ru.faserkraft.client.domain.model.UserRegistration
import ru.faserkraft.client.data.dto.DeviceRequestDto
import ru.faserkraft.client.data.dto.DeviceResponseDto

fun DeviceResponseDto.toDomain(password: String): UserRegistration = UserRegistration(
    userEmail = userEmail,
    userName = userName,
    userRole = userRole,
    password = password,
)

fun DeviceRequest.toDto() = DeviceRequestDto(
    deviceId = deviceId,
    model = model,
    manufacturer = manufacturer,
    token = token,
    password = password,
    userId = userId,
)

fun DeviceRequestDto.toDomain() = DeviceRequest(
    deviceId = deviceId,
    model = model,
    manufacturer = manufacturer,
    token = token,
    password = password,
    userId = userId,
)