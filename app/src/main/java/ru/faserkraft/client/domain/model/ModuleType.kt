package ru.faserkraft.client.domain.model

data class ModuleType(
    val type: String,
    val requiredCount: Int,
    val packedCount: Int
)