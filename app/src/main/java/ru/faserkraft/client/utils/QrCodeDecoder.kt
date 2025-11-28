package ru.faserkraft.client.utils

import com.google.gson.Gson
import com.google.gson.JsonParser
import ru.faserkraft.client.dto.DeviceRegisterDto
import ru.faserkraft.client.dto.deviceRegisterBuilder

private val gson = Gson()

fun qrCodeDecode(jsonString: String): Result<Any?> =
    runCatching {
        val obj = JsonParser().parse(jsonString).asJsonObject
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