package ru.faserkraft.client.domain.repository

import ru.faserkraft.client.domain.model.DeviceRequest
import ru.faserkraft.client.domain.model.UserRegistration

interface DeviceRepository {
    suspend fun registerDevice(request: DeviceRequest): UserRegistration
    suspend fun getQrCode(employeeId: Int): String
}