package ru.faserkraft.client.data.dto

import com.google.gson.annotations.SerializedName

/**
 * DTO для создания товара
 */
data class ProductCreateDto(
    @SerializedName("serial_number") val serialNumber: String,
    @SerializedName("process_id") val processId: Int
)

/**
 * DTO для создания заказа
 */
data class OrderCreateDto(
    @SerializedName("number") val number: String
)

/**
 * DTO для обновления заказа
 */
data class OrderUpdateDto(
    @SerializedName("id") val id: Int,
    @SerializedName("number") val number: String
)

/**
 * DTO для создания упаковки
 */
data class PackagingCreateDto(
    @SerializedName("serial_number") val serialNumber: String,
    @SerializedName("product_ids") val productIds: List<Long>
)
