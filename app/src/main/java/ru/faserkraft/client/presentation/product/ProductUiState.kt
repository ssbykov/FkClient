package ru.faserkraft.client.presentation.product

import ru.faserkraft.client.domain.model.Employee
import ru.faserkraft.client.domain.model.FinishedProduct
import ru.faserkraft.client.domain.model.Process
import ru.faserkraft.client.domain.model.Product
import ru.faserkraft.client.domain.model.ProductsInventory
import ru.faserkraft.client.domain.model.Step

data class ProductUiState(
    val isLoading: Boolean = false,
    val isActionInProgress: Boolean = false,
    val product: Product? = null,
    val selectedStep: Step? = null,
    val processes: List<Process> = emptyList(),
    val employees: List<Employee> = emptyList(),
    val productsInventory: List<ProductsInventory> = emptyList(),
    val productsInventoryByProcess: List<Product> = emptyList(),
    val availableProductsForPackaging: List<FinishedProduct> = emptyList(),
)