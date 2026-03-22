package ru.faserkraft.client.dto

data class EmployeeDto(
    val id: Int,
    val name: String,
    val user: UserDto
)

data class UserDto(
    val id: Int,
    val email: String,
)

