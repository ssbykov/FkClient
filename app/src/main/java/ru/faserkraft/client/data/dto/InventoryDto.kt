package ru.faserkraft.client.data.dto


import com.google.gson.annotations.SerializedName

data class InventoryDto(
    val id: Int,
    @SerializedName("created_at")
    val createdAt: String,
    @SerializedName("completed_at")
    val completedAt: String?,
    @SerializedName("created_by_id")
    val createdById: Int,
    @SerializedName("item_count")
    val itemCount: Int,
) : ItemDto()


data class InventoryItemDto(
    val id: Int,
    @SerializedName("inventory_id")
    val inventoryId: Int,
    @SerializedName("serial_number")
    val serialNumber: String,
    @SerializedName("step_definition")
    val stepDefinition: StepDefinitionWithProcessDto,
    @SerializedName("scanned_at")
    val scannedAt: String,
) : ItemDto()

data class InventoryItemCreateDto(
    @SerializedName("serial_number")
    val serialNumber: String,
    @SerializedName("step_definition_id")
    val stepDefinitionId: Int,
) : ItemDto()

data class ProductInventoryItemDto(
    @SerializedName("serial_number")
    val serialNumber: String,
    @SerializedName("step_definition")
    val stepDefinition: StepDefinitionWithProcessDto,
)

data class InventoryCompareResultDto(
    @SerializedName("db_count")
    val dbCount: Int,
    @SerializedName("scanned_count")
    val scannedCount: Int,
    val matched: List<ProductInventoryItemDto>,
    val missing: List<ProductInventoryItemDto>,
    val unexpected: List<ProductInventoryItemDto>,
)