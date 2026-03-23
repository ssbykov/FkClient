package ru.faserkraft.client.dto

import java.io.Serializable

data class EmployeeDto(
    val id: Int,
    val name: String,
    val user: UserDto
) : Serializable

data class UserDto(
    val id: Int,
    val email: String,
)

