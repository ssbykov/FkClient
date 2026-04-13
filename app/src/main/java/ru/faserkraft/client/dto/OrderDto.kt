package ru.faserkraft.client.dto

import com.google.gson.annotations.SerializedName


// === Модели для чтения данных (ответов) ===

data class OrderDto(
    val id: Int = 0,
    @SerializedName("contract_number")
    val contractNumber: String,
    @SerializedName("contract_date")
    val contractDate: String,
    @SerializedName("planned_shipment_date")
    val plannedShipmentDate: String,
    @SerializedName("shipment_date")
    val shipmentDate: String?,
    @SerializedName("shipment_by")
    val shipmentBy: EmployeeDto?,
    val items: List<OrderItemDto>,
    val packaging: List<PackagingDto>
) : ItemDto()

data class OrderItemDto(
    val id: Int = 0,
    val quantity: Int,
    @SerializedName("work_process")
    val workProcess: ProcessDto
) : ItemDto()


// === Модели для отправки данных (запросов) ===

data class OrderCreateDto(
    @SerializedName("contract_number")
    val contractNumber: String,
    @SerializedName("contract_date")
    val contractDate: String,
    @SerializedName("planned_shipment_date")
    val plannedShipmentDate: String
)

data class OrderUpdateDto(
    val id: Int,
    @SerializedName("contract_number")
    val contractNumber: String,
    @SerializedName("contract_date")
    val contractDate: String,
    @SerializedName("planned_shipment_date")
    val plannedShipmentDate: String
)

data class OrderItemCreateDto(
    @SerializedName("process_id")
    val processId: Int,
    val quantity: Int
)

data class OrderCloseDto(
    @SerializedName("shipment_date")
    val shipmentDate: String,
    @SerializedName("shipment_by_id")
    val shipmentById: Int
)