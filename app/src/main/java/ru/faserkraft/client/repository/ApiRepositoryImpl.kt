package ru.faserkraft.client.repository

// Импорты для заказов

import ru.faserkraft.client.api.Api
import ru.faserkraft.client.dto.DailyPlanCopyDto
import ru.faserkraft.client.dto.DailyPlanStepCreateDto
import ru.faserkraft.client.dto.DailyPlanStepUpdateDto
import ru.faserkraft.client.dto.DayPlanDto
import ru.faserkraft.client.dto.DeviceRequestDto
import ru.faserkraft.client.dto.DeviceResponseDto
import ru.faserkraft.client.dto.EmployeeDto
import ru.faserkraft.client.dto.FinishedProductDto
import ru.faserkraft.client.dto.OrderCreateDto
import ru.faserkraft.client.dto.OrderDto
import ru.faserkraft.client.dto.OrderItemCreateDto
import ru.faserkraft.client.dto.OrderUpdateDto
import ru.faserkraft.client.dto.PackagingCreateDto
import ru.faserkraft.client.dto.PackagingDto
import ru.faserkraft.client.dto.ProcessDto
import ru.faserkraft.client.dto.ProductCreateDto
import ru.faserkraft.client.dto.ProductDto
import ru.faserkraft.client.dto.ProductStatusDto
import ru.faserkraft.client.dto.ProductsInventoryDto
import ru.faserkraft.client.dto.QrDataResponseDto
import ru.faserkraft.client.dto.toBackendValue
import javax.inject.Inject

class ApiRepositoryImpl @Inject constructor(
    private val api: Api,
) : ApiRepository {

    // ================== ПРОДУКТЫ И ШАГИ (PRODUCTS & STEPS) ==================

    override suspend fun getProduct(serialNumber: String): ProductDto? =
        callApi { api.getProduct(serialNumber) }

    override suspend fun postProduct(product: ProductCreateDto): ProductDto? =
        callApi { api.postProduct(product) }

    override suspend fun getProductsByLastCompletedStep(
        processId: Int,
        stepDefinitionId: Int
    ): List<ProductDto>? =
        callApi { api.getProductsByLastCompletedStep(processId, stepDefinitionId) }

    override suspend fun getProductsByStepEmployeeDay(
        stepDefinitionId: Int,
        day: String,
        employeeId: Int
    ): List<ProductDto>? = callApi {
        api.getProductsByStepEmployeeDay(stepDefinitionId, day, employeeId)
    }

    override suspend fun getFinishedProduct(): List<FinishedProductDto>? =
        callApi { api.getFinishedProduct() }

    override suspend fun getProductsInventory(): List<ProductsInventoryDto>? =
        callApi { api.getProductsInventory() }

    override suspend fun changeProductProcess(
        productId: Long,
        newProcessId: Int
    ): ProductDto? = callApi { api.changeProductProcess(productId, newProcessId) }

    override suspend fun changeProductStatus(
        productId: Long,
        status: ProductStatusDto,
    ): ProductDto? = callApi { api.changeProductStatus(productId, status.toBackendValue()) }

    override suspend fun postStep(stepId: Int): ProductDto? =
        callApi { api.postStep(stepId) }

    override suspend fun changeStepPerformer(
        stepId: Int,
        newEmployeeId: Int,
    ): ProductDto? = callApi { api.changeStepPerformer(stepId, newEmployeeId) }


    // ================== УПАКОВКА (PACKAGING) ==================

    override suspend fun getPackaging(serialNumber: String): PackagingDto? =
        callApi { api.getPackaging(serialNumber) }

    override suspend fun getPackagingInStorage(): List<PackagingDto>? =
        callApi { api.getPackagingInStorage() }

    override suspend fun addPackagingToOrder(
        orderId: Int,
        packagingIds: List<Int>
    ) = callApi { api.addPackagingToOrder(orderId, packagingIds) }

    override suspend fun detachPackagingFromOrder(packagingIds: List<Int>): Boolean? =
        callApi { api.detachPackagingFromOrder(packagingIds) }


    override suspend fun createPackaging(packaging: PackagingCreateDto): PackagingDto? =
        callApi { api.createPackaging(packaging) }

    override suspend fun deletePackaging(serialNumber: String) =
        callApiNoBody { api.deletePackaging(serialNumber) }


    // ================== ПЛАНЫ НА ДЕНЬ (DAILY PLANS) ==================

    override suspend fun getDayPlans(date: String): List<DayPlanDto>? =
        callApi { api.getDayPlans(date) }

    override suspend fun copyDailyPlan(dayPlanCopy: DailyPlanCopyDto): List<DayPlanDto>? =
        callApi { api.copyDailyPlan(dayPlanCopy) }

    override suspend fun addStepToDailyPlan(body: DailyPlanStepCreateDto): List<DayPlanDto>? =
        callApi { api.addStepToDailyPlan(body) }

    override suspend fun removeStepFromDailyPlan(dailyPlanStepId: Int): List<DayPlanDto>? =
        callApi { api.removeStepFromDailyPlan(dailyPlanStepId) }

    override suspend fun updateStepInDailyPlan(body: DailyPlanStepUpdateDto): List<DayPlanDto>? =
        callApi { api.updateStepInDailyPlan(body) }


    // ================== ПОЛЬЗОВАТЕЛИ / СОТРУДНИКИ (USERS / EMPLOYEES) ==================

    override suspend fun getEmployees(): List<EmployeeDto>? =
        callApi { api.getEmployees() }

    override suspend fun postDevice(device: DeviceRequestDto): DeviceResponseDto? =
        callApi { api.postDevice(device) }

    override suspend fun getQrCode(employeeId: Int): QrDataResponseDto? =
        callApi { api.getQrCode(employeeId) }


    // ================== СПРАВОЧНИКИ (REFERENCE DATA) ==================

    override suspend fun getProcesses(): List<ProcessDto>? =
        callApi { api.getProcesses() }


    // ================== ЗАКАЗЫ (ORDERS) ==================

    override suspend fun getAllOrders(): List<OrderDto>? =
        callApi { api.getAllOrders() }

    override suspend fun getOrder(orderId: Int): OrderDto? =
        callApi { api.getOrder(orderId) }

    override suspend fun createOrder(order: OrderCreateDto): OrderDto? =
        callApi { api.createOrder(order) }

    override suspend fun updateOrder(order: OrderUpdateDto): OrderDto? =
        callApi { api.updateOrder(order) }

    override suspend fun updateOrderItems(
        orderId: Int,
        items: List<OrderItemCreateDto>
    ): OrderDto? = callApi { api.updateOrderItems(orderId, items) }

    override suspend fun closeOrder(
        orderId: Int,
    ): OrderDto? = callApi { api.closeOrder(orderId) }

    override suspend fun deleteOrder(orderId: Int) =
        callApiNoBody { api.deleteOrder(orderId) }

}