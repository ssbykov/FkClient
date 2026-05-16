package ru.faserkraft.client.domain.repository

import ru.faserkraft.client.domain.model.ProductShort
import ru.faserkraft.client.domain.model.Product
import ru.faserkraft.client.domain.model.ProductStatus
import ru.faserkraft.client.domain.model.ProductsInventory

interface ProductRepository {
    suspend fun getProduct(serialNumber: String): Product?
    suspend fun getProductsByStatus(statuses: List<ProductStatus>): List<Product>
    suspend fun createProduct(serialNumber: String, processId: Int): Product
    suspend fun changeStatus(productId: Long, status: ProductStatus): Product
    suspend fun changeProcess(productId: Long, newProcessId: Int): Product
    suspend fun getProductsInventory(): List<ProductsInventory>
    suspend fun getFinishedProducts(): List<ProductShort>
    suspend fun getProductsByLastCompletedStep(
        processId: Int,
        stepDefinitionId: Int,
    ): List<Product>

    suspend fun getProductsByStepEmployeeDay(
        stepDefinitionId: Int,
        day: String,
        employeeId: Int,
    ): List<Product>
}