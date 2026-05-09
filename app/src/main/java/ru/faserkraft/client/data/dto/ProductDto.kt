package ru.faserkraft.client.data.dto

import com.google.gson.annotations.SerializedName
import java.io.Serializable


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
    @SerializedName("packaging")
    val packaging: PackagingShortDto?,
    val status: ProductStatusDto,
    val steps: List<StepDto>
) : ItemDto()

data class FinishedProductDto(
    val id: Int = 0,
    @SerializedName("serial_number")
    val serialNumber: String,
    @SerializedName("work_process")
    val process: FinishedProcessDto,
) : ItemDto()


enum class ProductStatusDto {
    @SerializedName("normal")
    NORMAL,

    @SerializedName("rework")
    REWORK,

    @SerializedName("scrap")
    SCRAP;
}


data class ProductsInventoryDto(
    @SerializedName("process_id")
    val processId: Int,
    @SerializedName("process_name")
    val processName: String,
    @SerializedName("step_definition_id")
    val stepDefinitionId: Int,
    @SerializedName("step_name")
    val stepName: String,
    @SerializedName("step_name_genitive")
    val stepNameGenitive: String,
    @SerializedName("count")
    val count: Int,
) : Serializable

