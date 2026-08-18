package ru.faserkraft.client.data.network


import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query
import ru.faserkraft.client.BuildConfig
import ru.faserkraft.client.data.dto.DailyPlanCopyDto
import ru.faserkraft.client.data.dto.DailyPlanStepCreateDto
import ru.faserkraft.client.data.dto.DailyPlanStepUpdateDto
import ru.faserkraft.client.data.dto.DayPlanDto
import ru.faserkraft.client.data.dto.EmployeeDto
import ru.faserkraft.client.data.dto.InventoryDto
import ru.faserkraft.client.data.dto.InventoryItemCreateDto
import ru.faserkraft.client.data.dto.InventoryItemDto
import ru.faserkraft.client.data.dto.OrderCreateDto
import ru.faserkraft.client.data.dto.OrderDto
import ru.faserkraft.client.data.dto.OrderItemCreateDto
import ru.faserkraft.client.data.dto.OrderUpdateDto
import ru.faserkraft.client.data.dto.PackagingCreateDto
import ru.faserkraft.client.data.dto.PackagingDto
import ru.faserkraft.client.data.dto.PeriodStatisticsDto
import ru.faserkraft.client.data.dto.ProcessDto
import ru.faserkraft.client.data.dto.ProductCreateDto
import ru.faserkraft.client.data.dto.ProductDto
import ru.faserkraft.client.data.dto.ProductInventoryCompareItemDto
import ru.faserkraft.client.data.dto.ProductShortDto
import ru.faserkraft.client.data.dto.ProductsOverviewDto
import ru.faserkraft.client.data.dto.QrDataResponseDto

const val BASE_URL = BuildConfig.BASE_URL

interface Api {

    // ================== ПРОДУКТЫ (PRODUCTS) ==================

    @GET(BASE_URL + "products/by-serial/{serial_number}")
    suspend fun getProduct(
        @Path("serial_number") id: String,
    ): Response<ProductDto>

    @GET("products/by-last-completed-step")
    suspend fun getProductsByLastCompletedStep(
        @Query("process_id") processId: Int,
        @Query("step_definition_id") stepDefinitionId: Int,
    ): Response<List<ProductDto>>

    @GET("products/by-step-employee-day")
    suspend fun getProductsByStepEmployeeDay(
        @Query("step_definition_id") stepDefinitionId: Int,
        @Query("day") day: String,
        @Query("employee_id") employeeId: Int? = null,
    ): Response<List<ProductDto>>

    @GET(BASE_URL + "products/finished")
    suspend fun getFinishedProduct(): Response<List<ProductShortDto>>

    @GET("products/statistics/period")
    suspend fun getFinishedProductsByPeriod(
        @Query("date_from") dateFrom: String,
        @Query("date_to") dateTo: String
    ): Response<PeriodStatisticsDto>

    @POST(BASE_URL + "products")
    suspend fun postProduct(
        @Body product: ProductCreateDto
    ): Response<ProductDto>

    @POST(BASE_URL + "products/change_product_process")
    suspend fun changeProductProcess(
        @Query("product_id") productId: Long,
        @Query("new_process_id") newProcessId: Int
    ): Response<ProductDto>

    @POST(BASE_URL + "products/{product_id}/change_status")
    suspend fun changeProductStatus(
        @Path("product_id") productId: Long,
        @Query("status") status: String
    ): Response<ProductDto>

    @GET("products/stats/by-last-done-step")
    suspend fun getProductsOverview(): Response<List<ProductsOverviewDto>>

    @GET(BASE_URL + "products/not-normal")
    suspend fun getProductsNotNormal(): Response<List<ProductDto>>

// ================== ИНВЕНТАРИЗАЦИЯ (INVENTORY) ==================

    @GET(BASE_URL + "inventories/")
    suspend fun getInventories(): Response<List<InventoryDto>>

    @POST(BASE_URL + "inventories/")
    suspend fun createInventory(): Response<InventoryDto>

    @GET(BASE_URL + "inventories/{inventory_id}/items")
    suspend fun getInventoryItems(
        @Path("inventory_id") inventoryId: Int,
    ): Response<List<InventoryItemDto>>

    @POST(BASE_URL + "inventories/{inventory_id}/items")
    suspend fun upsertInventoryItem(
        @Path("inventory_id") inventoryId: Int,
        @Body item: InventoryItemCreateDto,
    ): Response<InventoryItemDto>

    @POST(BASE_URL + "inventories/{inventory_id}/complete")
    suspend fun completeInventory(
        @Path("inventory_id") inventoryId: Int,
    ): Response<InventoryDto>

    @POST(BASE_URL + "inventories/{inventory_id}/compare")
    suspend fun compareInventory(
        @Path("inventory_id") inventoryId: Int,
    ): Response<List<ProductInventoryCompareItemDto>>

    @DELETE("inventories/{id}")
    suspend fun deleteInventory(@Path("id") id: Int): Response<Unit>

    // ================== ШАГИ ПРОЦЕССА (PRODUCTS STEPS) ==================

    @POST(BASE_URL + "products_steps/")
    suspend fun postStep(
        @Query("step_id") stepId: Int
    ): Response<ProductDto>

    @POST(BASE_URL + "products_steps/change_performer")
    suspend fun changeStepPerformer(
        @Query("step_id") stepId: Int,
        @Query("new_employee_id") newEmployeeId: Int,
    ): Response<ProductDto>

    @POST(BASE_URL + "products_steps/reset")
    suspend fun resetStep(
        @Query("step_id") stepId: Int,
    ): Response<ProductDto>

    // ================== УПАКОВКА (PACKAGING) ==================

    @GET(BASE_URL + "packaging/by_serial/{serial_number}")
    suspend fun getPackaging(
        @Path("serial_number") serialNumber: String
    ): Response<PackagingDto>

    @GET(BASE_URL + "packaging/get_in_storage")
    suspend fun getPackagingInStorage(): Response<List<PackagingDto>>

    @POST("packaging/attach_to_order/{order_id}")
    suspend fun addPackagingToOrder(
        @Path("order_id") orderId: Int,
        @Body packagingIds: List<Int>
    ): Response<Boolean>

    @POST("packaging/detach_from_order")
    suspend fun detachPackagingFromOrder(
        @Body packagingIds: List<Int>
    ): Response<Boolean>

    @POST(BASE_URL + "packaging")
    suspend fun createPackaging(
        @Body packaging: PackagingCreateDto
    ): Response<PackagingDto>

    @DELETE(BASE_URL + "packaging/{serial_number}")
    suspend fun deletePackaging(
        @Path("serial_number") serialNumber: String
    ): Response<Unit>


    // ================== ЗАКАЗЫ (ORDERS) ==================

    @GET(BASE_URL + "orders/get_all_orders")
    suspend fun getAllOrders(): Response<List<OrderDto>>

    @GET(BASE_URL + "orders/{order_id}")
    suspend fun getOrder(
        @Path("order_id") orderId: Int
    ): Response<OrderDto>

    @POST(BASE_URL + "orders")
    suspend fun createOrder(
        @Body order: OrderCreateDto
    ): Response<OrderDto>

    @PUT(BASE_URL + "orders")
    suspend fun updateOrder(
        @Body order: OrderUpdateDto
    ): Response<OrderDto>

    @PUT(BASE_URL + "orders/{order_id}/items")
    suspend fun updateOrderItems(
        @Path("order_id") orderId: Int,
        @Body items: List<OrderItemCreateDto>
    ): Response<OrderDto>

    @POST(BASE_URL + "orders/{order_id}/close")
    suspend fun closeOrder(
        @Path("order_id") orderId: Int,
    ): Response<OrderDto>

    @DELETE(BASE_URL + "orders/{order_id}")
    suspend fun deleteOrder(
        @Path("order_id") orderId: Int
    ): Response<Unit>


    // ================== ПЛАНЫ НА ДЕНЬ (DAILY PLANS) ==================

    @GET(BASE_URL + "daily-plans")
    suspend fun getDayPlans(
        @Query("plan_date") date: String
    ): Response<List<DayPlanDto>>

    @POST(BASE_URL + "daily-plans/copy")
    suspend fun copyDailyPlan(
        @Body body: DailyPlanCopyDto
    ): Response<List<DayPlanDto>>

    @POST(BASE_URL + "daily-plans/add_step")
    suspend fun addStepToDailyPlan(
        @Body body: DailyPlanStepCreateDto
    ): Response<List<DayPlanDto>>

    @POST(BASE_URL + "daily-plans/update_step")
    suspend fun updateStepInDailyPlan(
        @Body body: DailyPlanStepUpdateDto
    ): Response<List<DayPlanDto>>

    @DELETE(BASE_URL + "daily-plans/steps/{id}")
    suspend fun removeStepFromDailyPlan(
        @Path("id") dailyPlanStepId: Int
    ): Response<List<DayPlanDto>>


    // ================== ПОЛЬЗОВАТЕЛИ / СОТРУДНИКИ (USERS / EMPLOYEES) ==================

    @GET(BASE_URL + "employees/")
    suspend fun getEmployees(): Response<List<EmployeeDto>>

    @POST("users/get-qr-code")
    suspend fun getQrCode(
        @Query("employee_id") employeeId: Int
    ): Response<QrDataResponseDto>


    // ================== СПРАВОЧНИКИ (REFERENCE DATA) ==================

    @GET(BASE_URL + "processes/")
    suspend fun getProcesses(): Response<List<ProcessDto>>

}