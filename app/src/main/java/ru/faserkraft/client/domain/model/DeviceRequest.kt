package ru.faserkraft.client.domain.model

data class DeviceRequest(
    val deviceId: String,
    val model: String,
    val manufacturer: String,
    val token: String,
    val password: String,
    val userId: Int,
)