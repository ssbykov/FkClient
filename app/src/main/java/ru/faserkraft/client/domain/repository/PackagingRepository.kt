package ru.faserkraft.client.domain.repository

import ru.faserkraft.client.domain.model.Packaging

interface PackagingRepository {
    suspend fun getPackaging(serialNumber: String): Packaging?
    suspend fun createPackaging(serialNumber: String, productIds: List<Int>): Packaging
    suspend fun deletePackaging(serialNumber: String)
    suspend fun getPackagingInStorage(): List<Packaging>
}