package ru.faserkraft.client.domain.usecase.inventory

import ru.faserkraft.client.domain.model.Inventory
import ru.faserkraft.client.domain.repository.InventoryRepository
import javax.inject.Inject

class CompleteInventoryUseCase @Inject constructor(
    private val repository: InventoryRepository,
) {
    suspend operator fun invoke(inventoryId: Int): Inventory =
        repository.completeInventory(inventoryId)
}