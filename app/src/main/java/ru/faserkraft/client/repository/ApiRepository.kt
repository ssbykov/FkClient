package ru.faserkraft.client.repository

import ru.faserkraft.client.dto.DeviceResponseDto
import ru.faserkraft.client.dto.ProductDto
import ru.faserkraft.client.dto.DeviceRequestDto

interface ApiRepository {
    suspend fun getProduct(serialNumber: String): ProductDto
    suspend fun postDevice(device: DeviceRequestDto): DeviceResponseDto?
}