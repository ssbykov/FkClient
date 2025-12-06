package ru.faserkraft.client.repository

import ru.faserkraft.client.dto.DeviceRequestDto
import ru.faserkraft.client.dto.DeviceResponseDto
import ru.faserkraft.client.dto.ProcessDto
import ru.faserkraft.client.dto.ProductDto
import ru.faserkraft.client.dto.StepCloseDto

interface ApiRepository {
    suspend fun getProduct(serialNumber: String): ProductDto
    suspend fun getProcesses(): Sequence<ProcessDto>
    suspend fun postDevice(device: DeviceRequestDto): DeviceResponseDto?
    suspend fun postStep(step: StepCloseDto): ProductDto?
}