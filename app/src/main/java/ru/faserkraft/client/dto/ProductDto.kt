package ru.faserkraft.client.dto

import com.google.gson.annotations.SerializedName


data class ProductCreateDto(
    @SerializedName("process_id")
    val processId: Int = 0,
    @SerializedName("serial_number")
    val serialNumber: String,
    @SerializedName("created_at")
    val createdAt: String = "",
) : ItemDto()

data class ProductDto(
    val id: Long = 0,
    @SerializedName("serial_number")
    val serialNumber: String,
    @SerializedName("work_process")
    val process: ProcessDto,
    @SerializedName("created_at")
    val createdAt: String,
    val steps: List<StepDto>
) : ItemDto()
