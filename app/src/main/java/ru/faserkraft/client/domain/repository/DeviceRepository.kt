package ru.faserkraft.client.domain.repository

import ru.faserkraft.client.domain.model.UserRegistration
import ru.faserkraft.client.dto.DeviceRequestDto

interface DeviceRepository {
    suspend fun registerDevice(request: DeviceRequestDto): UserRegistration
    suspend fun getQrCode(employeeId: Int): String
}