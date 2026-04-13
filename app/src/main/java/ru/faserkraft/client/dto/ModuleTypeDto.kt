package ru.faserkraft.client.dto

data class ModuleTypeDto(
    val type: String,
    val requiredCount: Int,
    val packedCount: Int
)