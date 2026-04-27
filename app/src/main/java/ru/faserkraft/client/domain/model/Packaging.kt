package ru.faserkraft.client.domain.model

/**
 * Domain model для Упаковки
 */
data class Packaging(
    val id: Int,
    val serialNumber: String,
    val products: List<FinishedProduct>,
    val createdDate: String?,
    val lastModifiedDate: String?
)

data class FinishedProduct(
    val id: Long,
    val serialNumber: String,
    val processId: Int,
    val status: String
)
