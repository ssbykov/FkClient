package ru.faserkraft.client.domain.usecase.inventory

import ru.faserkraft.client.domain.model.InventoryItem
import ru.faserkraft.client.domain.repository.InventoryRepository
import javax.inject.Inject

class UpsertInventoryItemUseCase @Inject constructor(
    private val repository: InventoryRepository,
) {
    suspend operator fun invoke(
        inventoryId: Int,
        serialNumber: String,
        stepDefinitionId: Int,
    ): InventoryItem = repository.upsertItem(inventoryId, serialNumber, stepDefinitionId)
}