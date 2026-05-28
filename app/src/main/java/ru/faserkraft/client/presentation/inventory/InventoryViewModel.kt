package ru.faserkraft.client.presentation.inventory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import ru.faserkraft.client.domain.model.Inventory
import ru.faserkraft.client.domain.usecase.inventory.CompareInventoryUseCase
import ru.faserkraft.client.domain.usecase.inventory.CompleteInventoryUseCase
import ru.faserkraft.client.domain.usecase.inventory.CreateInventoryUseCase
import ru.faserkraft.client.domain.usecase.inventory.DeleteInventoryUseCase
import ru.faserkraft.client.domain.usecase.inventory.GetInventoriesUseCase
import ru.faserkraft.client.domain.usecase.inventory.GetInventoryItemsUseCase
import ru.faserkraft.client.domain.usecase.inventory.UpsertInventoryItemUseCase
import ru.faserkraft.client.domain.usecase.product.GetProductUseCase
import ru.faserkraft.client.presentation.base.toErrorMessage
import javax.inject.Inject

@HiltViewModel
class InventoryViewModel @Inject constructor(
    private val getInventoriesUseCase: GetInventoriesUseCase,
    private val createInventoryUseCase: CreateInventoryUseCase,
    private val deleteInventoryUseCase: DeleteInventoryUseCase,
    private val getInventoryItemsUseCase: GetInventoryItemsUseCase,
    private val upsertInventoryItemUseCase: UpsertInventoryItemUseCase,
    private val completeInventoryUseCase: CompleteInventoryUseCase,
    private val compareInventoryUseCase: CompareInventoryUseCase,
    private val getProductUseCase: GetProductUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(InventoryUiState())
    val uiState: StateFlow<InventoryUiState> = _uiState

    private val _events = Channel<InventoryEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    private var isScanHandled = false

    fun resetScanHandled() {
        isScanHandled = false
    }

    fun loadInventories() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            runCatching { getInventoriesUseCase() }
                .onSuccess { list ->
                    _uiState.update { it.copy(inventories = list) }
                }
                .onFailure { emitError(it) }
            _uiState.update { it.copy(isLoading = false) }
        }
    }

    fun createInventory() {
        viewModelScope.launch {
            _uiState.update { it.copy(isActionInProgress = true) }
            runCatching { createInventoryUseCase() }
                .onSuccess { inventory ->
                    _uiState.update {
                        it.copy(
                            currentInventory = inventory,
                            currentInventoryItems = emptyList(),
                            pendingProduct = null,
                            availableSteps = emptyList(),
                            preselectedStepId = null,
                            compareResults = emptyList(),
                            isActionInProgress = false,
                        )
                    }
                    _events.send(InventoryEvent.NavigateToScan)
                }
                .onFailure {
                    _uiState.update { it.copy(isActionInProgress = false) }
                    emitError(it)
                }
        }
    }

    fun openInventoryDetail(inventory: Inventory) {
        loadInventoryAndNavigate(inventory, InventoryEvent.NavigateToDetail)
    }

    fun continueInventory() {
        viewModelScope.launch {
            _events.send(InventoryEvent.NavigateToScan)
        }
    }

    private fun loadInventoryAndNavigate(inventory: Inventory, event: InventoryEvent) {
        viewModelScope.launch {
            _uiState.update { it.copy(currentInventory = inventory, isLoading = true) }
            runCatching { getInventoryItemsUseCase(inventory.id) }
                .onSuccess { items ->
                    _uiState.update {
                        it.copy(
                            currentInventoryItems = items,
                            pendingProduct = null,
                            availableSteps = emptyList(),
                            preselectedStepId = null,
                            compareResults = emptyList(),
                            isLoading = false,
                        )
                    }
                    _events.send(event)
                }
                .onFailure {
                    _uiState.update { it.copy(isLoading = false) }
                    emitError(it)
                }
        }
    }

    fun deleteInventory(id: Int) {
        viewModelScope.launch {
            _uiState.update { it.copy(isActionInProgress = true) }
            runCatching { deleteInventoryUseCase(id) }
                .onSuccess {
                    _uiState.update { state ->
                        state.copy(
                            inventories = state.inventories.filterNot { it.id == id },
                            currentInventory = state.currentInventory?.takeIf { it.id != id },
                            currentInventoryItems = if (state.currentInventory?.id == id) emptyList() else state.currentInventoryItems,
                            pendingProduct = null,
                            availableSteps = emptyList(),
                            preselectedStepId = null,
                            compareResults = emptyList(),
                        )
                    }
                }
                .onFailure { emitError(it) }
            _uiState.update { it.copy(isActionInProgress = false) }
        }
    }

    fun onBarcodeScanned(rawSerial: String) {
        if (isScanHandled) return

        _uiState.value.currentInventory ?: return

        isScanHandled = true

        val isDuplicate = _uiState.value.currentInventoryItems.any { it.serialNumber == rawSerial }
        if (isDuplicate) {
            viewModelScope.launch {
                _events.send(InventoryEvent.ShowDuplicateWarning(rawSerial))
            }
            return
        }

        processScannedProduct(rawSerial)
    }

    fun forceAddDuplicate(rawSerial: String) {
        isScanHandled = true
        processScannedProduct(rawSerial)
    }

    private fun processScannedProduct(rawSerial: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                runCatching { getProductUseCase(rawSerial) }
                    .onSuccess { product ->
                        if (product == null) {
                            emitError(Exception("Продукт не найден"))
                            return@onSuccess
                        }
                        val performedSteps = product.steps
                            .filter { it.performedAt != null }
                            .sortedBy { it.definition.order }
                        val lastStep = performedSteps.lastOrNull()
                        _uiState.update { state ->
                            state.copy(
                                pendingProduct = product,
                                availableSteps = performedSteps.map { it.definition },
                                preselectedStepId = lastStep?.definition?.id,
                            )
                        }
                        _events.send(InventoryEvent.ShowConfirmDialog)
                    }
                    .onFailure { emitError(it) }
            } finally {
                _uiState.update { it.copy(isLoading = false) }
                isScanHandled = false
            }
        }
    }

    fun confirmItem(serialNumber: String, stepDefinitionId: Int) {
        val inventoryId = _uiState.value.currentInventory?.id ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isActionInProgress = true) }
            runCatching { upsertInventoryItemUseCase(inventoryId, serialNumber, stepDefinitionId) }
                .onSuccess { newItem ->
                    _uiState.update { state ->
                        val updated = state.currentInventoryItems
                            .filterNot { it.serialNumber == newItem.serialNumber } + newItem
                        state.copy(
                            currentInventoryItems = updated,
                            pendingProduct = null,
                            availableSteps = emptyList(),
                            preselectedStepId = null,
                        )
                    }
                    _events.send(InventoryEvent.ItemUpserted)
                }
                .onFailure { emitError(it) }
            _uiState.update { it.copy(isActionInProgress = false) }
        }
    }

    fun cancelPendingItem() {
        _uiState.update {
            it.copy(
                pendingProduct = null,
                availableSteps = emptyList(),
                preselectedStepId = null,
            )
        }
    }

    fun completeInventory() {
        val inventoryId = _uiState.value.currentInventory?.id ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isActionInProgress = true) }
            runCatching { completeInventoryUseCase(inventoryId) }
                .onSuccess { updatedInventory ->
                    _uiState.update {
                        it.copy(
                            currentInventory = updatedInventory,
                            isActionInProgress = false,
                        )
                    }
                    compareInventory()
                }
                .onFailure { error ->
                    _uiState.update { it.copy(isActionInProgress = false) }
                    emitError(error)
                }
        }
    }

    fun compareInventory() {
        val inventoryId = _uiState.value.currentInventory?.id ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isActionInProgress = true) }
            runCatching { compareInventoryUseCase(inventoryId) }
                .onSuccess { results ->
                    _uiState.update {
                        it.copy(
                            compareResults = results,
                            isActionInProgress = false,
                        )
                    }
                    android.util.Log.d("INV_DEBUG", "send NavigateToResults")
                    _events.send(InventoryEvent.NavigateToResults)
                }
                .onFailure { error ->
                    _uiState.update { it.copy(isActionInProgress = false) }
                    emitError(error)
                }
        }
    }

    fun clearCurrentInventory() {
        _uiState.update {
            it.copy(
                currentInventory = null,
                currentInventoryItems = emptyList(),
                compareResults = emptyList(),
            )
        }
    }

    fun resetState() {
        _uiState.value = InventoryUiState()
    }

    private suspend fun emitError(e: Throwable) {
        _events.send(InventoryEvent.ShowError(e.toErrorMessage()))
    }
}