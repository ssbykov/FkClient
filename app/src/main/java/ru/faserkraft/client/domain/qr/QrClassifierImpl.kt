package ru.faserkraft.client.data.qr

import com.google.gson.Gson
import com.google.gson.JsonParser
import ru.faserkraft.client.domain.qr.QrClassifier
import ru.faserkraft.client.domain.qr.QrParseResult
import ru.faserkraft.client.dto.DeviceRegisterDto
import ru.faserkraft.client.dto.DeviceRequestDto
import ru.faserkraft.client.dto.deviceRegisterBuilder
import ru.faserkraft.client.utils.isUfCode
import ru.faserkraft.client.utils.isUfPkgCode
import javax.inject.Inject

class QrClassifierImpl @Inject constructor(
    private val gson: Gson,
) : QrClassifier {

    override fun classify(raw: String): QrParseResult {
        return when {
            isUfCode(raw) -> QrParseResult.Product(raw)
            isUfPkgCode(raw) -> QrParseResult.Packaging(raw)
            else -> decodeRegistration(raw)?.let { QrParseResult.DeviceRegistration(it) }
                ?: QrParseResult.Unknown
        }
    }

    private fun decodeRegistration(jsonString: String): DeviceRequestDto? {
        return runCatching {
            val obj = JsonParser.parseString(jsonString).asJsonObject
            val dataIn: DeviceRegisterDto =
                gson.fromJson(obj, DeviceRegisterDto::class.java)
            deviceRegisterBuilder(dataIn)
        }.getOrNull()
    }
}