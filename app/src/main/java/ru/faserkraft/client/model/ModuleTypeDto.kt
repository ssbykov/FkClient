package ru.faserkraft.client.model

/**
 * UI model for module type information in orders
 */
data class ModuleTypeDto(
    val type: String,
    val requiredCount: Int,
    val packedCount: Int
)
