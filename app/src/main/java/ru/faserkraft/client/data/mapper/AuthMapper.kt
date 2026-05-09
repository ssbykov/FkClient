package ru.faserkraft.client.data.mapper

import ru.faserkraft.client.domain.model.LoginCredentials
import ru.faserkraft.client.data.dto.LoginRequestDto

fun LoginCredentials.toDto() = LoginRequestDto(
    username = username,
    password = password
)