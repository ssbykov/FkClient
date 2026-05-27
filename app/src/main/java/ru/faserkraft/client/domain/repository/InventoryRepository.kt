package ru.faserkraft.client.domain.repository


import ru.faserkraft.client.domain.model.Inventory
import ru.faserkraft.client.domain.model.InventoryCompareResult
import ru.faserkraft.client.domain.model.InventoryItem

interface InventoryRepository {
    suspend fun getInventories(): List<Inventory>
    suspend fun createInventory(): Inventory
    suspend fun deleteInventory(id: Int)
    suspend fun getInventoryItems(inventoryId: Int): List<InventoryItem>
    suspend fun upsertItem(
        inventoryId: Int,
        serialNumber: String,
        stepDefinitionId: Int,
    ): InventoryItem

    suspend fun completeInventory(inventoryId: Int): Inventory
    suspend fun compareInventory(inventoryId: Int): List<InventoryCompareResult>
}