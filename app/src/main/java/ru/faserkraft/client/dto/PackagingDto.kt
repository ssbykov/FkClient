package ru.faserkraft.client.dto

import com.google.gson.annotations.SerializedName


data class PackagingDto(
    val id: Int,
    @SerializedName("serial_number")
    val serialNumber: String,
    @SerializedName("performed_by")
    val performedBy: EmployeeDto? = null,
    @SerializedName("performed_at")
    val performedAt: String? = null,
    @SerializedName("shipment_at")
    val shipmentAt: String? = null,
    val products: List<FinishedProductDto>
) : ItemDto()

data class PackagingCreateDto(
    @SerializedName("serial_number")
    val serialNumber: String,
    val products: List<Int>
) : ItemDto()

data class PackagingShipmentResponse(
    @SerializedName("updated_ids")
    val updatedIds: List<Int>
)
