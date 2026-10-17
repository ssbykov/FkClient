package ru.faserkraft.client.data.dto

import android.os.Build
import com.google.gson.annotations.SerializedName
import java.util.UUID

data class DeviceRegisterDto(
    @SerializedName("employee_id")
    val employeeId: Int,
    @SerializedName("invitation_token")
    val invitationToken: String,
)

data class DeviceResponseDto(
    @SerializedName("access_token")
    val accessToken: String,
    @SerializedName("expires_at")
    val expiresAt: String,
    @SerializedName("employee_name")
    val userName: String,
    @SerializedName("user_email")
    val userEmail: String,
    @SerializedName("employee_role")
    val userRole: String,
    @SerializedName("device_id")
    val deviceId: String,
    val model: String,
    val manufacturer: String,
) : ItemDto()

data class DeviceRequestDto(
    @SerializedName("invitation_token")
    val invitationToken: String,
    @SerializedName("device_id")
    val deviceId: String,
    val model: String,
    val manufacturer: String,
) : ItemDto()

fun deviceRegisterBuilder(dataIn: DeviceRegisterDto): DeviceRequestDto =
    DeviceRequestDto(
        invitationToken = dataIn.invitationToken,
        deviceId = UUID.randomUUID().toString(),
        model = Build.MODEL.orEmpty().ifBlank { "unknown" },
        manufacturer = Build.MANUFACTURER.orEmpty().ifBlank { "unknown" },
    )