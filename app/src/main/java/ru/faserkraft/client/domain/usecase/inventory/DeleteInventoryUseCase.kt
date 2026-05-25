package ru.faserkraft.client.domain.usecase.inventory

import ru.faserkraft.client.domain.repository.InventoryRepository
import javax.inject.Inject

class DeleteInventoryUseCase @Inject constructor(
    private val repository: InventoryRepository,
) {
    suspend operator fun invoke(id: Int) = repository.deleteInventory(id)
}