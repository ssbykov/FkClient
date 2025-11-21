package ru.faserkraft.client.dto

import com.google.gson.annotations.SerializedName

data class ProductDto(
    val id: Long = 0,
    @SerializedName("serial_number")
    val serialNumber: String,
    @SerializedName("process_id")
    val processId: Int,
    @SerializedName("created_at")
    val createdAt: String,
    val steps: List<StepDto>
): ItemDto()
