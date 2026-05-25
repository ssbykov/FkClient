package ru.faserkraft.client.presentation.inventory.overview

import ru.faserkraft.client.domain.model.Product
import ru.faserkraft.client.domain.model.ProductStatus
import ru.faserkraft.client.domain.model.ProductsOverview
import ru.faserkraft.client.domain.model.UserRole

data class ReworkScrapSelection(
    val processName: String,
    val status: ProductStatus,
)

data class ProductsOverviewUiState(
    val isLoading: Boolean = false,
    val productsOverview: List<ProductsOverview> = emptyList(),
    val productsOverviewByProcess: List<Product> = emptyList(),
    val selectedOverviewItem: ProductsOverview? = null,
    val reworkScrapProducts: List<Product> = emptyList(),
    val selectedScrapReworkItem: ReworkScrapSelection? = null,
    val userRole: UserRole? = null,
    val errorMessage: String? = null,
)