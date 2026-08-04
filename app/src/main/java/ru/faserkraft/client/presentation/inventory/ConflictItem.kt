package ru.faserkraft.client.presentation.inventory

import ru.faserkraft.client.domain.model.ProductInventoryItem
import ru.faserkraft.client.domain.model.StepDefinitionWithProcess

data class ConflictItem(
    val item: ProductInventoryItem,                      // данные из учёта (всегда есть)
    val scannedStep: StepDefinitionWithProcess?,         // null = MISSING
    val scannedAt: String?,                              // null = MISSING
    val conflictType: ConflictType,
    val dbAheadOfScan: Boolean = false,                  // только для STEP_MISMATCH
)

enum class ConflictType {
    /** Есть в учёте, не попал в инвентаризацию */
    MISSING,
    /** Есть в обоих, этапы расходятся */
    STEP_MISMATCH,
}