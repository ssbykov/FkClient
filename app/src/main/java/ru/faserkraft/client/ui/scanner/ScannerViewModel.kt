package ru.faserkraft.client.ui.scanner

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import ru.faserkraft.client.domain.model.NavigationEvent
import ru.faserkraft.client.domain.usecase.dailyplan.GetDayPlansUseCase
import ru.faserkraft.client.domain.usecase.order.GetOrdersUseCase
import ru.faserkraft.client.domain.usecase.packaging.GetPackagingUseCase
import ru.faserkraft.client.domain.usecase.product.GetProductUseCase
import javax.inject.Inject

/**
 * ViewModel для QR сканера
 * Координирует получение данных в зависимости от типа сканированного кода
 */
@HiltViewModel
class ScannerViewModel @Inject constructor(
    private val getProductUseCase: GetProductUseCase,
    private val getPackagingUseCase: GetPackagingUseCase,
    private val getOrdersUseCase: GetOrdersUseCase,
    private val getDayPlansUseCase: GetDayPlansUseCase
) : ViewModel() {

    private val _loadingState = MutableStateFlow(false)
    val loadingState: StateFlow<Boolean> = _loadingState

    private val _navigationEvents = MutableSharedFlow<NavigationEvent>(extraBufferCapacity = 1)
    val navigationEvents: SharedFlow<NavigationEvent> = _navigationEvents

    private val _errorMessages = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val errorMessages: SharedFlow<String> = _errorMessages

    /**
     * Обработать сканированный UUID товара
     */
    fun handleProductQrCode(serialNumber: String) {
        viewModelScope.launch {
            _loadingState.value = true
            getProductUseCase(serialNumber)
                .onSuccess {
                    _navigationEvents.emit(NavigationEvent.NavigateToProduct)
                }
                .onFailure { error ->
                    _errorMessages.emit(error.message ?: "Ошибка загрузки товара")
                }
            _loadingState.value = false
        }
    }

    /**
     * Обработать сканированный код упаковки
     */
    fun handlePackagingQrCode(serialNumber: String) {
        viewModelScope.launch {
            _loadingState.value = true
            getPackagingUseCase(serialNumber)
                .onSuccess {
                    _navigationEvents.emit(NavigationEvent.NavigateToPackaging)
                }
                .onFailure { error ->
                    _errorMessages.emit(error.message ?: "Ошибка загрузки упаковки")
                }
            _loadingState.value = false
        }
    }

    /**
     * Получить все заказы
     */
    fun loadOrders() {
        viewModelScope.launch {
            _loadingState.value = true
            getOrdersUseCase()
                .onSuccess {
                    _navigationEvents.emit(NavigationEvent.NavigateToOrder)
                }
                .onFailure { error ->
                    _errorMessages.emit(error.message ?: "Ошибка загрузки заказов")
                }
            _loadingState.value = false
        }
    }
}

