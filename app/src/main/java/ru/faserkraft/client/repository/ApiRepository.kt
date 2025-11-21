package ru.faserkraft.client.repository

import ru.faserkraft.client.dto.ProductDto

interface ApiRepository {
    suspend fun getProduct(serialNumber: String): ProductDto
}