package ru.faserkraft.client.dto

import com.google.gson.annotations.SerializedName

data class SizeType(
    val id: Int,
    val name: String,
    @SerializedName("packaging_count")
    val packagingCount: Int,
)
