package ru.faserkraft.client.dto

import com.google.gson.annotations.SerializedName
import ru.faserkraft.client.R
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
    @SerializedName("packaging_id")
    val packagingId: Int?,
    val status: ProductStatus,
    val steps: List<StepDto>
) : ItemDto()

data class FinishedProductDto(
    val id: Int = 0,
    @SerializedName("serial_number")
    val serialNumber: String,
    @SerializedName("work_process")
    val process: FinishedProcessDto,
) : ItemDto()


enum class ProductStatus(
    val titleRes: Int,
    val bgColorRes: Int,
    val textColorRes: Int,
) {
    @SerializedName("normal")
    NORMAL(
        R.string.normal,
        R.color.status_success_bg,
        R.color.status_success_text,
    ),

    @SerializedName("rework")
    REWORK(
        R.string.rework,
        R.color.status_rework_bg,
        R.color.status_rework_text,
    ),

    @SerializedName("scrap")
    SCRAP(
        R.string.scrap,
        R.color.status_scrap_bg,
        R.color.status_scrap_text,
    );
}


fun ProductStatus.toUiProductStatus(): ProductStatus = when (name) {
    "NORMAL" -> ProductStatus.NORMAL
    "REWORK" -> ProductStatus.REWORK
    "SCRAP" -> ProductStatus.SCRAP
    else -> ProductStatus.NORMAL
}

fun ProductStatus.toBackendValue(): String = when (this) {
    ProductStatus.NORMAL -> "normal"
    ProductStatus.REWORK -> "rework"
    ProductStatus.SCRAP -> "scrap"
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

