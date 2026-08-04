package ru.faserkraft.client.presentation.inventory

import ru.faserkraft.client.domain.model.InventoryCompareResult
import ru.faserkraft.client.domain.model.InventoryItem

fun InventoryCompareResult.toConflictItems(
    scannedItems: List<InventoryItem>
): List<ConflictItem> {
    val scannedBySerial = scannedItems.associateBy { it.serialNumber }

    val missingItems = missing.map { dbItem ->
        ConflictItem(
            item = dbItem,
            scannedStep = null,
            scannedAt = null,
            conflictType = ConflictType.MISSING,
        )
    }

    val mismatchItems = unexpected.map { dbItem ->
        val scanned = scannedBySerial[dbItem.serialNumber]
        val dbAhead = scanned != null &&
                dbItem.stepDefinition.order > scanned.stepDefinition.order

        ConflictItem(
            item = dbItem,
            scannedStep = scanned?.stepDefinition,
            scannedAt = scanned?.scannedAt,
            conflictType = ConflictType.STEP_MISMATCH,
            dbAheadOfScan = dbAhead,
        )
    }

    return missingItems + mismatchItems
}