package ru.faserkraft.client.dto

import com.google.gson.annotations.SerializedName

data class TemplateDto(
    val name: String,
    @SerializedName("name_genitive")
    val nameGenitive: String
)
