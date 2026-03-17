package ru.faserkraft.client.dto

import com.google.gson.annotations.SerializedName


data class PackagingDto(
    val id: Int,
    @SerializedName("serial_number")
    val serialNumber: String,
    val products: List<FinishedProductDto>
) : ItemDto()

data class PackagingCreateDto(
    @SerializedName("serial_number")
    val serialNumber: String,
    val products: List<Int>
) : ItemDto()

val emptyPackaging = PackagingDto(
    id = 0,
    serialNumber = "",
    products = listOf(),
)