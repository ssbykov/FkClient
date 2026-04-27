package ru.faserkraft.client.data.dto

import com.google.gson.annotations.SerializedName
import ru.faserkraft.client.domain.model.FinishedProduct
import ru.faserkraft.client.domain.model.Packaging

/**
 * DTO для упаковки от сервера
 */
data class PackagingDto(
    @SerializedName("id") val id: Int,
    @SerializedName("serial_number") val serialNumber: String,
    @SerializedName("performed_at") val performedAt: String? = null,
    @SerializedName("order_id") val orderId: Int? = null,
    @SerializedName("products") val products: List<FinishedProductDto>
)

/**
 * DTO для готовых товаров
 */
data class FinishedProductDto(
    @SerializedName("id") val id: Long,
    @SerializedName("serial_number") val serialNumber: String,
    @SerializedName("process_id") val processId: Int,
    @SerializedName("status") val status: String
)
