package ru.faserkraft.client.domain.usecase.inventory

import ru.faserkraft.client.domain.model.InventoryCompareResult
import ru.faserkraft.client.domain.repository.InventoryRepository
import javax.inject.Inject

class CompareInventoryUseCase @Inject constructor(
    private val repository: InventoryRepository,
) {
    suspend operator fun invoke(inventoryId: Int): List<InventoryCompareResult> =
        repository.compareInventory(inventoryId)
}