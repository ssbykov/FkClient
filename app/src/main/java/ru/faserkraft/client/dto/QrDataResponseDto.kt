package ru.faserkraft.client.dto

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class QrDataResponseDto(
    val action: String,
    val id: Int,
    val token: String,
)

private val json = Json {
    encodeDefaults = true
    ignoreUnknownKeys = true
}


fun QrDataResponseDto.toQrContent(): String =
    json.encodeToString(this)
