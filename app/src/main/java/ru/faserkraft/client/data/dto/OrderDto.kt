package ru.faserkraft.client.data.dto

import com.google.gson.annotations.SerializedName

/**
 * DTO для заказа от сервера
 */
data class OrderDto(
    @SerializedName("id") val id: Int,
    @SerializedName("contract_number") val contractNumber: String,
    @SerializedName("contract_date") val contractDate: String,
    @SerializedName("planned_shipment_date") val plannedShipmentDate: String,
    @SerializedName("shipment_date") val shipmentDate: String?,
    @SerializedName("status") val status: String,
    @SerializedName("items") val items: List<OrderItemDto>,
    @SerializedName("packaging") val packaging: List<PackagingDto>,
    @SerializedName("created_date") val createdDate: String?,
    @SerializedName("last_modified_date") val lastModifiedDate: String?
)

data class OrderItemDto(
    @SerializedName("id") val id: Int,
    @SerializedName("product_id") val productId: Long,
    @SerializedName("quantity") val quantity: Int,
    @SerializedName("status") val status: String,
    @SerializedName("work_process") val workProcess: ProcessDto
)
