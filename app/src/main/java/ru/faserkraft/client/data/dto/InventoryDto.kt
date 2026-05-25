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
) : ItemDto()

data class InventoryItemDto(
    val id: Int,
    @SerializedName("inventory_id")
    val inventoryId: Int,
    @SerializedName("serial_number")
    val serialNumber: String,
    @SerializedName("step_definition_id")
    val stepDefinitionId: Int,
    @SerializedName("step_definition")
    val stepDefinition: StepDefinitionDto,
    @SerializedName("scanned_at")
    val scannedAt: String,
) : ItemDto()

data class InventoryItemCreateDto(
    @SerializedName("serial_number")
    val serialNumber: String,
    @SerializedName("step_definition_id")
    val stepDefinitionId: Int,
) : ItemDto()

data class InventoryCompareResultDto(
    @SerializedName("step_definition_id")
    val stepDefinitionId: Int,
    @SerializedName("step_name")
    val stepName: String?,
    @SerializedName("process_id")
    val processId: Int?,
    @SerializedName("db_count")
    val dbCount: Int,
    @SerializedName("scanned_count")
    val scannedCount: Int,
    val matched: List<String>,
    val missing: List<String>,
    val unexpected: List<String>,
)