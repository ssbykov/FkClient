package ru.faserkraft.client.domain.usecase.statistic

import ru.faserkraft.client.domain.model.PeriodStatistics
import ru.faserkraft.client.domain.repository.ProductRepository
import javax.inject.Inject

class GetProductsStatisticsUseCase @Inject constructor(
    private val repository: ProductRepository,
) {
    suspend operator fun invoke(
        dateFrom: String,
        dateTo: String,
        includeFirstHalf: Boolean = false,
    ): PeriodStatistics =
        repository.getPeriodStatistics(
            dateFrom = dateFrom,
            dateTo = dateTo,
            includeFirstHalf = includeFirstHalf,
        )
}
