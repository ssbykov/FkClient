package ru.faserkraft.client.data.qr

import com.google.gson.JsonParser
import ru.faserkraft.client.data.dto.DeviceRegisterDto
import ru.faserkraft.client.data.dto.deviceRegisterBuilder
import ru.faserkraft.client.data.mapper.toDomain
import ru.faserkraft.client.domain.model.DeviceRequest
import ru.faserkraft.client.domain.qr.QrClassifier
import ru.faserkraft.client.domain.qr.QrParseResult
import ru.faserkraft.client.utils.qrcode.isUfCode
import ru.faserkraft.client.utils.qrcode.isUfPkgCode
import javax.inject.Inject

class QrClassifierImpl @Inject constructor() : QrClassifier {

    override fun classify(raw: String): QrParseResult {
        return when {
            isUfCode(raw) -> QrParseResult.Product(raw)
            isUfPkgCode(raw) -> QrParseResult.Packaging(raw)
            else -> decodeRegistration(raw)?.let { QrParseResult.DeviceRegistration(it) }
                ?: QrParseResult.Unknown
        }
    }

    private fun decodeRegistration(jsonString: String): DeviceRequest? = runCatching {
        val obj = JsonParser.parseString(jsonString).asJsonObject
        val token = obj.get("invitation_token")?.asString?.takeIf { it.isNotBlank() }
            ?: return@runCatching null
        val dataIn = DeviceRegisterDto(obj.get("employee_id").asInt, token)
        deviceRegisterBuilder(dataIn).toDomain()
    }.getOrNull()
}