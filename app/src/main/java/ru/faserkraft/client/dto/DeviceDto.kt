package ru.faserkraft.client.dto

import android.os.Build
import com.google.gson.annotations.SerializedName
import kotlinx.serialization.Serializable
import ru.faserkraft.client.utils.generatePassword
import java.util.UUID

@Serializable
data class DeviceRegisterDto(val id: Int, val token: String)

data class DeviceResponseDto(
    @SerializedName("user_id")
    val userId: Int,
    @SerializedName("device_id")
    val deviceId: String,
    val model: String,
    val manufacturer: String,
): ItemDto()


data class DeviceRequestDto(
    @SerializedName("device_id")
    val deviceId: String,
    val model: String,
    val manufacturer: String,
    val token: String,
    val password: String,
    @SerializedName("user_id")
    val userId: Int,
): ItemDto()



fun deviceRegisterBuilder(dataIn: DeviceRegisterDto): DeviceRequestDto {
    val deviceId = UUID.randomUUID().toString()
    val model = Build.MODEL ?: ""
    val manufacturer = Build.MANUFACTURER ?: ""
    val password = generatePassword(length = 8)
    return DeviceRequestDto(
        userId = dataIn.id,
        token = dataIn.token,
        deviceId = deviceId,
        model = model,
        manufacturer = manufacturer,
        password = password
    )
}