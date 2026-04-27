package ru.faserkraft.client.data.dto

import com.google.gson.annotations.SerializedName

/**
 * DTO для сотрудника
 */
data class EmployeeDto(
    @SerializedName("id") val id: Int,
    @SerializedName("name") val name: String,
    @SerializedName("user") val user: UserDto? = null
)

data class UserDto(
    @SerializedName("id") val id: Int,
    @SerializedName("email") val email: String
)
