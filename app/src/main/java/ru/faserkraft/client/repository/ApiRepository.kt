package ru.faserkraft.client.repository

import ru.faserkraft.client.dto.DayPlanDto
import ru.faserkraft.client.dto.DeviceRequestDto
import ru.faserkraft.client.dto.DeviceResponseDto
import ru.faserkraft.client.dto.ProcessDto
import ru.faserkraft.client.dto.ProductCreateDto
import ru.faserkraft.client.dto.ProductDto

interface ApiRepository {
    suspend fun getProduct(serialNumber: String): ProductDto?
    suspend fun postProduct(product: ProductCreateDto): ProductDto?
    suspend fun getProcesses(): List<ProcessDto>?
    suspend fun postDevice(device: DeviceRequestDto): DeviceResponseDto?
    suspend fun postStep(stepId: Int): ProductDto?
    suspend fun changeProductProcess(productId: Long, newProcessId: Int): ProductDto?
    suspend fun getDayPlans(date: String): List<DayPlanDto>?
}