package ru.faserkraft.client.repository


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

interface ApiRepository {

    // ================== ПРОДУКТЫ И ШАГИ (PRODUCTS & STEPS) ==================
    suspend fun getProduct(serialNumber: String): ProductDto?

    suspend fun postProduct(product: ProductCreateDto): ProductDto?

    suspend fun getProductsByLastCompletedStep(
        processId: Int,
        stepDefinitionId: Int,
    ): List<ProductDto>?

    suspend fun getProductsByStepEmployeeDay(
        stepDefinitionId: Int,
        day: String,
        employeeId: Int
    ): List<ProductDto>?

    suspend fun getFinishedProduct(): List<FinishedProductDto>?

    suspend fun getProductsInventory(): List<ProductsInventoryDto>?

    suspend fun changeProductProcess(
        productId: Long,
        newProcessId: Int,
    ): ProductDto?

    suspend fun changeProductStatus(
        productId: Long,
        status: ProductStatusDto,
    ): ProductDto?

    suspend fun postStep(stepId: Int): ProductDto?

    suspend fun changeStepPerformer(
        stepId: Int,
        newEmployeeId: Int,
    ): ProductDto?


    // ================== УПАКОВКА (PACKAGING) ==================
    suspend fun getPackaging(serialNumber: String): PackagingDto?

    suspend fun getPackagingInStorage(): List<PackagingDto>?

    suspend fun addPackagingToOrder(orderId: Int, packagingIds: List<Int>): Boolean?

    suspend fun detachPackagingFromOrder(packagingIds: List<Int>): Boolean?

    suspend fun createPackaging(packaging: PackagingCreateDto): PackagingDto?

    suspend fun deletePackaging(serialNumber: String)


    // ================== ПЛАНЫ НА ДЕНЬ (DAILY PLANS) ==================
    suspend fun getDayPlans(date: String): List<DayPlanDto>?

    suspend fun copyDailyPlan(dayPlanCopy: DailyPlanCopyDto): List<DayPlanDto>?

    suspend fun addStepToDailyPlan(body: DailyPlanStepCreateDto): List<DayPlanDto>?

    suspend fun updateStepInDailyPlan(body: DailyPlanStepUpdateDto): List<DayPlanDto>?

    suspend fun removeStepFromDailyPlan(dailyPlanStepId: Int): List<DayPlanDto>?


    // ================== ПОЛЬЗОВАТЕЛИ / СОТРУДНИКИ (USERS / EMPLOYEES) ==================
    suspend fun getEmployees(): List<EmployeeDto>?

    suspend fun postDevice(device: DeviceRequestDto): DeviceResponseDto?

    suspend fun getQrCode(employeeId: Int): QrDataResponseDto?


    // ================== СПРАВОЧНИКИ (REFERENCE DATA) ==================
    suspend fun getProcesses(): List<ProcessDto>?


    // ================== ЗАКАЗЫ (ORDERS) ==================
    suspend fun getAllOrders(): List<OrderDto>?

    suspend fun getOrder(orderId: Int): OrderDto?

    suspend fun createOrder(order: OrderCreateDto): OrderDto?

    suspend fun updateOrder(order: OrderUpdateDto): OrderDto?

    suspend fun updateOrderItems(
        orderId: Int,
        items: List<OrderItemCreateDto>
    ): OrderDto?

    suspend fun closeOrder(
        orderId: Int,
    ): OrderDto?

    suspend fun deleteOrder(orderId: Int)
}