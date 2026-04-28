package ru.faserkraft.client.domain.repository

import ru.faserkraft.client.domain.model.UserRegistration

interface DeviceRepository {
    suspend fun registerDevice(
        serverUrl: String,
        password: String,
    ): UserRegistration
    suspend fun getQrCode(employeeId: Int): String  // возвращает JSON-строку для QR
}