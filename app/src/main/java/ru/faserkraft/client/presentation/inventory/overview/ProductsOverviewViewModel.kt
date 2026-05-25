package ru.faserkraft.client.presentation.inventory.overview

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import ru.faserkraft.client.auth.AppAuth
import ru.faserkraft.client.domain.model.ProductStatus
import ru.faserkraft.client.domain.model.ProductsOverview
import ru.faserkraft.client.domain.model.UserRole
import ru.faserkraft.client.domain.usecase.product.GetProductsByLastStepUseCase
import ru.faserkraft.client.domain.usecase.product.GetProductsByStatusUseCase
import ru.faserkraft.client.domain.usecase.product.GetProductsOverviewUseCase
import ru.faserkraft.client.presentation.base.toErrorMessage
import javax.inject.Inject

@HiltViewModel
class ProductsOverviewViewModel @Inject constructor(
    private val getProductsOverviewUseCase: GetProductsOverviewUseCase,
    private val getProductsByLastStepUseCase: GetProductsByLastStepUseCase,
    private val getProductsByStatusUseCase: GetProductsByStatusUseCase,
    private val appAuth: AppAuth,
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        ProductsOverviewUiState(userRole = appAuth.getRegistrationData()?.role)
    )
    val uiState: StateFlow<ProductsOverviewUiState> = _uiState

    private fun currentRole(): UserRole? =
        _uiState.value.userRole ?: appAuth.getRegistrationData()?.role

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    // ---------- Inventory ----------

    fun loadProductsInventory() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            runCatching { getProductsOverviewUseCase() }
                .onSuccess { list ->
                    _uiState.update {
                        it.copy(productsOverview = list, userRole = currentRole())
                    }
                }
                .onFailure { e ->
                    _uiState.update { it.copy(errorMessage = e.toErrorMessage()) }
                }
            _uiState.update { it.copy(isLoading = false) }
        }
    }

    fun loadProductsByLastStep(processId: Int, stepDefinitionId: Int) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, productsOverviewByProcess = emptyList()) }
            runCatching { getProductsByLastStepUseCase(processId, stepDefinitionId) }
                .onSuccess { list ->
                    _uiState.update {
                        it.copy(productsOverviewByProcess = list, userRole = currentRole())
                    }
                }
                .onFailure { e ->
                    _uiState.update { it.copy(errorMessage = e.toErrorMessage()) }
                }
            _uiState.update { it.copy(isLoading = false) }
        }
    }

    fun selectInventoryItem(item: ProductsOverview) {
        _uiState.update { it.copy(selectedOverviewItem = item) }
    }

    // ---------- Rework / Scrap ----------

    fun loadReworkScrapProducts() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            runCatching {
                getProductsByStatusUseCase(listOf(ProductStatus.REWORK, ProductStatus.SCRAP))
            }
                .onSuccess { list ->
                    _uiState.update {
                        it.copy(reworkScrapProducts = list, userRole = currentRole())
                    }
                }
                .onFailure { e ->
                    _uiState.update { it.copy(errorMessage = e.toErrorMessage()) }
                }
            _uiState.update { it.copy(isLoading = false) }
        }
    }

    fun selectReworkScrapProduct(processName: String, status: ProductStatus) {
        _uiState.update {
            it.copy(selectedScrapReworkItem = ReworkScrapSelection(processName, status))
        }
    }
}