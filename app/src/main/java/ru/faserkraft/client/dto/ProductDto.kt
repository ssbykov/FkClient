package ru.faserkraft.client.dto

import com.google.gson.annotations.SerializedName
import ru.faserkraft.client.R


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
    val status: ProductStatus,
    val steps: List<StepDto>
) : ItemDto()


enum class ProductStatus(
    val titleRes: Int,
    val bgColorRes: Int,
) {
    @SerializedName("normal")
    NORMAL(
        R.string.normal,
        R.color.status_success_bg,
    ),

    @SerializedName("rework")
    REPAIR(
        R.string.repair,
        R.color.status_warning_bg,
    ),
    @SerializedName("scrap")
    SCRAP(
        R.string.scrap,
        R.color.status_warning_icon,
    );
}

fun ProductStatus.toUiProductStatus(): ProductStatus = when (name) {
    "NORMAL" -> ProductStatus.NORMAL
    "REPAIR" -> ProductStatus.REPAIR
    "SCRAP" -> ProductStatus.SCRAP
    else -> ProductStatus.NORMAL
}
