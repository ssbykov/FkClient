package ru.faserkraft.client.data.repository

import ru.faserkraft.client.data.dto.InventoryItemCreateDto
import ru.faserkraft.client.data.mapper.toDomain
import ru.faserkraft.client.data.network.Api
import ru.faserkraft.client.domain.model.Inventory
import ru.faserkraft.client.domain.model.InventoryCompareResult
import ru.faserkraft.client.domain.model.InventoryItem
import ru.faserkraft.client.domain.repository.InventoryRepository
import ru.faserkraft.client.utils.logger.Logger
import javax.inject.Inject

class InventoryRepositoryImpl @Inject constructor(
    private val api: Api,
    logger: Logger,
) : BaseRepository(logger), InventoryRepository {

    override suspend fun getInventories(): List<Inventory> =
        callApi { api.getInventories() }.orEmpty().map { it.toDomain() }

    override suspend fun createInventory(): Inventory =
        requireNotNull(callApi { api.createInventory() }).toDomain()

    override suspend fun deleteInventory(id: Int) {
        callApiUnit { api.deleteInventory(id) }
    }

    override suspend fun getInventoryItems(inventoryId: Int): List<InventoryItem> =
        callApi { api.getInventoryItems(inventoryId) }.orEmpty().map { it.toDomain() }

    override suspend fun upsertItem(
        inventoryId: Int,
        serialNumber: String,
        stepDefinitionId: Int,
    ): InventoryItem =
        requireNotNull(
            callApi {
                api.upsertInventoryItem(
                    inventoryId = inventoryId,
                    item = InventoryItemCreateDto(
                        serialNumber = serialNumber,
                        stepDefinitionId = stepDefinitionId,
                    )
                )
            }
        ).toDomain()

    override suspend fun compareAndClose(inventoryId: Int): List<InventoryCompareResult> =
        callApi { api.compareAndClose(inventoryId) }.orEmpty().map { it.toDomain() }
}