package ru.faserkraft.client.api


import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query
import ru.faserkraft.client.BuildConfig
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
import ru.faserkraft.client.dto.ProductsInventoryDto
import ru.faserkraft.client.dto.QrDataResponseDto

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
    suspend fun getFinishedProduct(): Response<List<FinishedProductDto>>

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
    suspend fun getProductsInventory(): Response<List<ProductsInventoryDto>>


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

    @POST(BASE_URL + "users/new-device")
    suspend fun postDevice(
        @Body device: DeviceRequestDto
    ): Response<DeviceResponseDto>

    @POST("users/get-qr-code")
    suspend fun getQrCode(
        @Query("employee_id") employeeId: Int
    ): Response<QrDataResponseDto>


    // ================== СПРАВОЧНИКИ (REFERENCE DATA) ==================

    @GET(BASE_URL + "processes/")
    suspend fun getProcesses(): Response<List<ProcessDto>>

}