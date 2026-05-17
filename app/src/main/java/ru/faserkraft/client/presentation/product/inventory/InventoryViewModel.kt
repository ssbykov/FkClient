package ru.faserkraft.client.presentation.product.inventory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import ru.faserkraft.client.auth.AppAuth
import ru.faserkraft.client.domain.model.ProductStatus
import ru.faserkraft.client.domain.model.ProductsInventory
import ru.faserkraft.client.domain.model.UserRole
import ru.faserkraft.client.domain.usecase.product.GetProductsByLastStepUseCase
import ru.faserkraft.client.domain.usecase.product.GetProductsByStatusUseCase
import ru.faserkraft.client.domain.usecase.product.GetProductsInventoryUseCase
import ru.faserkraft.client.presentation.base.toErrorMessage
import javax.inject.Inject

@HiltViewModel
class InventoryViewModel @Inject constructor(
    private val getProductsInventoryUseCase: GetProductsInventoryUseCase,
    private val getProductsByLastStepUseCase: GetProductsByLastStepUseCase,
    private val getProductsByStatusUseCase: GetProductsByStatusUseCase,
    private val appAuth: AppAuth,
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        InventoryUiState(userRole = appAuth.getRegistrationData()?.role)
    )
    val uiState: StateFlow<InventoryUiState> = _uiState

    private fun currentRole(): UserRole? =
        _uiState.value.userRole ?: appAuth.getRegistrationData()?.role

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    // ---------- Inventory ----------

    fun loadProductsInventory() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            runCatching { getProductsInventoryUseCase() }
                .onSuccess { list ->
                    _uiState.update {
                        it.copy(productsInventory = list, userRole = currentRole())
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
            _uiState.update { it.copy(isLoading = true, productsInventoryByProcess = emptyList()) }
            runCatching { getProductsByLastStepUseCase(processId, stepDefinitionId) }
                .onSuccess { list ->
                    _uiState.update {
                        it.copy(productsInventoryByProcess = list, userRole = currentRole())
                    }
                }
                .onFailure { e ->
                    _uiState.update { it.copy(errorMessage = e.toErrorMessage()) }
                }
            _uiState.update { it.copy(isLoading = false) }
        }
    }

    fun selectInventoryItem(item: ProductsInventory) {
        _uiState.update { it.copy(selectedInventoryItem = item) }
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