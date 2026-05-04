package ru.faserkraft.client.data.mapper

import ru.faserkraft.client.domain.model.UserRegistration
import ru.faserkraft.client.dto.DeviceResponseDto

fun DeviceResponseDto.toDomain(password: String): UserRegistration = UserRegistration(
    userEmail = userEmail,
    userName = userName,
    userRole = userRole,
    password = password,
)