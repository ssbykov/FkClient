package ru.faserkraft.client.domain.usecase.inventory

import ru.faserkraft.client.domain.model.InventoryCompareResult
import ru.faserkraft.client.domain.repository.InventoryRepository
import javax.inject.Inject

class CompareAndCloseInventoryUseCase @Inject constructor(
    private val repository: InventoryRepository,
) {
    suspend operator fun invoke(inventoryId: Int): List<InventoryCompareResult> =
        repository.compareAndClose(inventoryId)
}