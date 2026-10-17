package ru.faserkraft.client.data.dto

import com.google.gson.annotations.SerializedName

data class InvitationRequestDto(
    @SerializedName("employee_id")
    val employeeId: Int,
)

data class QrDataResponseDto(
    @SerializedName("qr_payload")
    val qrPayload: String,
    @SerializedName("expires_at")
    val expiresAt: String,
)

fun QrDataResponseDto.toQrContent(): String = qrPayload