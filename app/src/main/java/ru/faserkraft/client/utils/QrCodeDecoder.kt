package ru.faserkraft.client.utils

import com.google.gson.Gson
import com.google.gson.JsonParser
import ru.faserkraft.client.dto.DeviceRegisterDto
import ru.faserkraft.client.dto.DeviceRequestDto
import ru.faserkraft.client.dto.deviceRegisterBuilder

private val gson = Gson()

fun qrCodeDecode(jsonString: String): Result<Any?> =
    runCatching {
        val obj = JsonParser.parseString(jsonString).getAsJsonObject()
        val action = obj.get("action")?.asString

        when (action) {
            "register" -> {
                val dataIn: DeviceRegisterDto =
                    gson.fromJson(obj, DeviceRegisterDto::class.java)
                deviceRegisterBuilder(dataIn)
            }
            else -> null
        }
    }

fun isUfCode(str: String): Boolean {
    val pattern = Regex("^uf-\\d{7,9}$")
    return pattern.matches(str)
}

fun isUfPkgCode(str: String): Boolean {
    val pattern = Regex("^PKG-\\d{7}$")
    return pattern.matches(str)
}

fun isDeviceRegistrationQr(str: String): Boolean {
    return qrCodeDecode(str)
        .getOrNull()
        ?.let { it is DeviceRequestDto }
        ?: false
}