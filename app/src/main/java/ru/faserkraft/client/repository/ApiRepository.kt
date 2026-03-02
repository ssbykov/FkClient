package ru.faserkraft.client.repository

import ru.faserkraft.client.dto.DailyPlanStepCreateDto
import ru.faserkraft.client.dto.DailyPlanStepUpdateDto
import ru.faserkraft.client.dto.DayPlanDto
import ru.faserkraft.client.dto.DeviceRequestDto
import ru.faserkraft.client.dto.DeviceResponseDto
import ru.faserkraft.client.dto.EmployeeDto
import ru.faserkraft.client.dto.ProcessDto
import ru.faserkraft.client.dto.ProductCreateDto
import ru.faserkraft.client.dto.ProductDto
import ru.faserkraft.client.dto.ProductStatus
import ru.faserkraft.client.dto.QrDataResponseDto

interface ApiRepository {
    suspend fun getProduct(serialNumber: String): ProductDto?
    suspend fun postProduct(product: ProductCreateDto): ProductDto?
    suspend fun getProcesses(): List<ProcessDto>?
    suspend fun getEmployees(): List<EmployeeDto>?
    suspend fun postDevice(device: DeviceRequestDto): DeviceResponseDto?
    suspend fun getQrCode(employeeId: Int): QrDataResponseDto?
    suspend fun postStep(stepId: Int): ProductDto?
    suspend fun changeProductProcess(productId: Long, newProcessId: Int): ProductDto?
    suspend fun getDayPlans(date: String): List<DayPlanDto>?
    suspend fun changeProductStatus(
        productId: Long,
        status: ProductStatus,
    ): ProductDto?

    suspend fun changeStepPerformer(
        stepId: Int,
        newEmployeeId: Int,
    ): ProductDto?

    suspend fun addStepToDailyPlan(
        body: DailyPlanStepCreateDto
    ): List<DayPlanDto>?

    suspend fun removeStepFromDailyPlan(
        dailyPlanStepId: Int
    ): List<DayPlanDto>?

    suspend fun updateStepInDailyPlan(
        body: DailyPlanStepUpdateDto
    ): List<DayPlanDto>?
}