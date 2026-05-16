package ru.faserkraft.client.presentation.product

import ru.faserkraft.client.domain.model.Product
import ru.faserkraft.client.domain.model.ProductStatus
import ru.faserkraft.client.domain.model.ProductsInventory
import ru.faserkraft.client.domain.model.UserRole

data class ReworkScrapSelection(
    val processName: String,
    val status: ProductStatus,
)

data class ProductsUiState(
    val isLoading: Boolean = false,
    val productsInventory: List<ProductsInventory> = emptyList(),
    val productsInventoryByProcess: List<Product> = emptyList(),
    val selectedInventoryItem: ProductsInventory? = null,
    val reworkScrapProducts: List<Product> = emptyList(),
    val selectedScrapReworkItem: ReworkScrapSelection? = null,
    val userRole: UserRole? = null,
    val errorMessage: String? = null,
)