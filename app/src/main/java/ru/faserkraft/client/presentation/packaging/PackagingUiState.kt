package ru.faserkraft.client.presentation.packaging

import ru.faserkraft.client.domain.model.FinishedProduct
import ru.faserkraft.client.domain.model.Packaging

data class PackagingUiState(
    val isLoading: Boolean = false,
    val isActionInProgress: Boolean = false,
    val packaging: Packaging? = null,
    val packagingInStorage: List<Packaging> = emptyList(),
    val availableProducts: List<FinishedProduct> = emptyList(),
)