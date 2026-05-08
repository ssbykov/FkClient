package ru.faserkraft.client.data.mapper

import ru.faserkraft.client.domain.model.LoginCredentials
import ru.faserkraft.client.dto.LoginRequestDto

fun LoginCredentials.toDto() = LoginRequestDto(
    username = username,
    password = password
)