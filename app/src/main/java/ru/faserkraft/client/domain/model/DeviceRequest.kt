package ru.faserkraft.client.domain.model

data class DeviceRequest(
    val invitationToken: String,
    val deviceId: String,
    val model: String,
    val manufacturer: String,
)