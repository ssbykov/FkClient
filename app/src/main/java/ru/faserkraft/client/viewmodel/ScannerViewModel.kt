package ru.faserkraft.client.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch
import ru.faserkraft.client.auth.AppAuth
import ru.faserkraft.client.dto.DayPlanDto
import ru.faserkraft.client.dto.DeviceRequestDto
import ru.faserkraft.client.dto.EmployeeDto
import ru.faserkraft.client.dto.ProcessDto
import ru.faserkraft.client.dto.ProductCreateDto
import ru.faserkraft.client.dto.ProductDto
import ru.faserkraft.client.dto.ProductStatus
import ru.faserkraft.client.dto.StepDto
import ru.faserkraft.client.dto.emptyStep
import ru.faserkraft.client.error.AppError
import ru.faserkraft.client.model.UserData
import ru.faserkraft.client.model.UserRole
import ru.faserkraft.client.repository.ApiRepository
import ru.faserkraft.client.utils.isUfCode
import ru.faserkraft.client.utils.qrCodeDecode
import java.io.IOException
import javax.inject.Inject

// UI‑состояние для крутилок/ошибок
data class ScannerUiState(
    val isLoading: Boolean = false,
    val isActionInProgress: Boolean = false,
)

@HiltViewModel
class ScannerViewModel @Inject constructor(
    private val repository: ApiRepository,
    private val appAuth: AppAuth
) : ViewModel() {

    // ---- UI state ----
    private val _uiState = MutableLiveData(ScannerUiState())
    val uiState: LiveData<ScannerUiState> = _uiState

    private fun updateUiState(reducer: (ScannerUiState) -> ScannerUiState) {
        _uiState.value = reducer(_uiState.value ?: ScannerUiState())
    }

    // ---- Данные ----
    private val _productState = MutableLiveData<ProductDto?>()
    val productState: LiveData<ProductDto?> = _productState

    private val _newProduct = MutableLiveData<ProductCreateDto>()
    val newProduct: LiveData<ProductCreateDto> = _newProduct

    private val _processes = MutableLiveData<List<ProcessDto>?>()
    val processes: LiveData<List<ProcessDto>?> = _processes

    private val _employees = MutableLiveData<List<EmployeeDto>?>()
    val employees: LiveData<List<EmployeeDto>?> = _employees

    private val _dayPlans = MutableLiveData<List<DayPlanDto>?>()
    val dayPlans: LiveData<List<DayPlanDto>?> = _dayPlans

    private val _selectedStep = MutableLiveData<StepDto?>()
    val selectedStep: LiveData<StepDto?> = _selectedStep

    private val _userData = MutableLiveData<UserData?>()
    val userData: LiveData<UserData?> = _userData

    val lastStep: LiveData<StepDto> = MediatorLiveData<StepDto>().apply {
        fun recalc() {
            val product = productState.value
            val selected = selectedStep.value
            value = when {
                product == null -> emptyStep
                selected != null -> product.steps
                    .find { it.stepDefinition.order == selected.stepDefinition.order }
                    ?: emptyStep

                else -> product.steps.minByOrNull { it.stepDefinition.order } ?: emptyStep
            }
        }

        addSource(productState) { recalc() }
        addSource(selectedStep) { recalc() }
    }

    // ---- Навигационные события ----
    sealed class UiEvent {
        object NavigateToRegistration : UiEvent()
        object NavigateToProduct : UiEvent()
        object NavigateToNewProduct : UiEvent()
    }

    private val _events = MutableSharedFlow<UiEvent>(extraBufferCapacity = 1)
    val events: SharedFlow<UiEvent> = _events

    // ---- Ошибки (для диалогов/snackbar) ----
    private val _errorState = MutableSharedFlow<String>()
    val errorState: SharedFlow<String> = _errorState

    private var isHandled = false
    fun resetIsHandled() {
        isHandled = false
    }

    init {
        loadRegistrationData()
    }

    // ---- План на день ----
    fun getDayPlans(date: String) {
        viewModelScope.launch {
            updateUiState { it.copy(isLoading = true) }
            try {
                val dayPlans = repository.getDayPlans(date)
                _dayPlans.value = dayPlans
            } catch (e: AppError) {
                val msg = appErrorToMessage(e)
                _errorState.emit(msg)
                _dayPlans.value = emptyList()
            } catch (e: Exception) {
                val msg = "Неизвестная ошибка"
                _errorState.emit(msg)
            } finally {
                updateUiState { it.copy(isLoading = false) }
            }
        }
    }

    // ---- Продукт ----
    suspend fun getProduct(serialNumber: String) {
        updateUiState { it.copy(isLoading = true) }
        try {
            val product = repository.getProduct(serialNumber)
            if (product == null) {
                newProduct(serialNumber)
            } else {
                setProduct(product)
            }
        } catch (e: AppError) {
            if (e is AppError.ApiError && e.status == 404) {
                newProduct(serialNumber)
            } else {
                val msg = appErrorToMessage(e)
                _errorState.emit(msg)
            }
        } catch (e: Exception) {
            val msg = "Неизвестная ошибка"
            _errorState.emit(msg)
        } finally {
            updateUiState { it.copy(isLoading = false) }
        }
    }

    suspend fun setProduct(product: ProductDto) {
        _productState.postValue(product)

        val initialStep = product.steps
            .firstOrNull { it.status != "done" }
            ?: product.steps.lastOrNull()
            ?: run {
                val msg = "У товара нет шагов"
                _errorState.emit(msg)
                return
            }

        _selectedStep.postValue(initialStep)
        _events.emit(UiEvent.NavigateToProduct)
    }

    suspend fun setProcesses() {
        updateUiState { it.copy(isLoading = true) }
        try {
            val processes = repository.getProcesses()
            _processes.postValue(processes)
        } catch (e: AppError) {
            val msg = appErrorToMessage(e)
            _errorState.emit(msg)
        } catch (e: Exception) {
            val msg = "Неизвестная ошибка"
            _errorState.emit(msg)
        } finally {
            updateUiState { it.copy(isLoading = false) }
        }
    }

    suspend fun setEmployees() {
        updateUiState { it.copy(isLoading = true) }
        try {
            val employees = repository.getEmployees()
            _employees.postValue(employees)
        } catch (e: AppError) {
            val msg = appErrorToMessage(e)
            _errorState.emit(msg)
        } catch (e: Exception) {
            val msg = "Неизвестная ошибка"
            _errorState.emit(msg)
        } finally {
            updateUiState { it.copy(isLoading = false) }
        }
    }

    suspend fun newProduct(serialNumber: String) {
        val newProductDto = ProductCreateDto(serialNumber = serialNumber)
        _newProduct.postValue(newProductDto)

        setProcesses()
        _events.emit(UiEvent.NavigateToNewProduct)
    }

    suspend fun createProduct(newProduct: ProductCreateDto): Result<Unit> {
        updateUiState { it.copy(isActionInProgress = true) }
        return try {
            repository.postProduct(newProduct)?.let { product ->
                setProduct(product)
            }
            Result.success(Unit)
        } catch (e: AppError) {
            val msg = appErrorToMessage(e)
            _errorState.emit(msg)
            Result.failure(e)
        } catch (e: Exception) {
            val msg = "Неизвестная ошибка"
            _errorState.emit(msg)
            Result.failure(e)
        } finally {
            updateUiState { it.copy(isActionInProgress = false) }
        }
    }

    suspend fun changeProductProcess(productId: Long, newProcessId: Int): Result<Unit> {
        updateUiState { it.copy(isActionInProgress = true) }
        return try {
            repository.changeProductProcess(productId, newProcessId)?.let { product ->
                setProduct(product)
            }
            Result.success(Unit)
        } catch (e: AppError) {
            val msg = appErrorToMessage(e)
            _errorState.emit(msg)
            Result.failure(e)
        } catch (e: Exception) {
            val msg = "Неизвестная ошибка"
            _errorState.emit(msg)
            Result.failure(e)
        } finally {
            updateUiState { it.copy(isActionInProgress = false) }
        }
    }

    suspend fun setProductStatus(
        productId: Long,
        status: ProductStatus,
    ): Result<Unit> {
        updateUiState { it.copy(isActionInProgress = true) }
        return try {
            repository.changeProductStatus(productId, status)?.let { product ->
                setProduct(product)
            }
            Result.success(Unit)
        } catch (e: AppError) {
            val msg = appErrorToMessage(e)
            _errorState.emit(msg)
            Result.failure(e)
        } catch (e: Exception) {
            val msg = "Неизвестная ошибка"
            _errorState.emit(msg)
            Result.failure(e)
        } finally {
            updateUiState { it.copy(isActionInProgress = false) }
        }
    }


    // ---- Регистрация / пользователь ----
    fun onRegistrationReady(model: UserData) {
        _userData.value = model
        _events.tryEmit(UiEvent.NavigateToRegistration)
    }

    fun loadRegistrationData() {
        _userData.value = appAuth.getRegistrationData()
    }

    fun resetRegistrationData() {
        appAuth.resetRegistration()
        loadRegistrationData()
    }

    // ---- Закрытие этапа ----
    suspend fun closeStep(step: StepDto) {
        val stepId = step.id
        if (stepId == 0) return

        updateUiState { it.copy(isActionInProgress = true) }
        try {
            val product = repository.postStep(stepId)
            if (product != null) {
                _productState.postValue(product)

                val lastStep = product.steps
                    .find { it.stepDefinition.order == step.stepDefinition.order }
                    ?: emptyStep

                _selectedStep.postValue(lastStep)
            }
        } catch (e: AppError) {
            val msg = appErrorToMessage(e)
            _errorState.emit(msg)
        } catch (e: Exception) {
            val msg = "Неизвестная ошибка"
            _errorState.emit(msg)
        } finally {
            updateUiState { it.copy(isActionInProgress = false) }
        }
    }

    suspend fun changeStepPerformer(
        step: StepDto,
        newEmployeeId: Int,
    ): Result<Unit> {
        val stepId = step.id

        updateUiState { it.copy(isActionInProgress = true) }
        return try {
            val product = repository.changeStepPerformer(stepId, newEmployeeId)
            if (product != null) {
                _productState.postValue(product)

            }
            Result.success(Unit)
        } catch (e: AppError) {
            val msg = appErrorToMessage(e)
            _errorState.emit(msg)
            Result.failure(e)
        } catch (e: Exception) {
            val msg = "Неизвестная ошибка"
            _errorState.emit(msg)
            Result.failure(e)
        } finally {
            updateUiState { it.copy(isActionInProgress = false) }
        }
    }


    // ---- Работа с QR ----
    suspend fun decodeQrCode(jsonString: String) {
        if (isHandled) return
        isHandled = true

        if (isUfCode(jsonString)) {
            updateUiState { it.copy(isLoading = true) }
            runCatching {
                getProduct(jsonString)
            }.onFailure { e ->
                val message = throwableToMessage(e)
                val msg = "Ошибка получения товара: $message"
                _errorState.emit(msg)
            }.also {
                updateUiState { it.copy(isLoading = false) }
            }
            return
        }

        // регистрация устройства
        updateUiState { it.copy(isLoading = true) }
        qrCodeDecode(jsonString)
            .onSuccess { data ->
                if (data is DeviceRequestDto) {
                    if (appAuth.checkRegistration() != null) {
                        val msg = "Устройство уже зарегистрировано"
                        _errorState.emit(msg)
                        return@onSuccess
                    }

                    try {
                        val result = repository.postDevice(data)

                        result?.let {
                            val userData = UserData(
                                email = result.userEmail,
                                password = data.password,
                                name = result.userName,
                                role = UserRole.fromValue(result.userRole) ?: UserRole.WORKER
                            )
                            appAuth.saveUserData(userData)
                            onRegistrationReady(userData)
                        } ?: run {
                            val msg = "Пустой ответ от сервера"
                            _errorState.emit(msg)
                        }
                    } catch (e: IOException) {
                        val msg = "Проблема с сетью, попробуйте ещё раз"
                        _errorState.emit(msg)
                    } catch (e: AppError.ApiError) {
                        val msg = "Ошибка сервера: ${e.status}"
                        _errorState.emit(msg)
                    } catch (e: Exception) {
                        val msg = "Неизвестная ошибка $e"
                        _errorState.emit(msg)
                    }
                } else {
                    val msg = "Некорректный QR‑код"
                    _errorState.emit(msg)
                }
            }
            .onFailure {
                val msg = "Ошибка разбора QR‑кода"
                _errorState.emit(msg)
            }
        updateUiState { it.copy(isLoading = false) }
    }

    // ---- Маппинг ошибок ----
    private fun appErrorToMessage(e: AppError): String =
        when (e) {
            is AppError.NetworkError -> "Нет соединения с сервером"
            is AppError.ApiError -> when (e.status) {
                401 -> "Не авторизован"
                403 -> "Доступ запрещён"
                404 -> "${e.message}"
                else -> "Ошибка сервера: ${e.status}"
            }

            is AppError.DaoError -> "Ошибка локальной базы"
            is AppError.UnknownError -> "Неизвестная ошибка"
        }

    private fun throwableToMessage(e: Throwable): String =
        when (e) {
            is AppError -> appErrorToMessage(e)
            else -> "Неизвестная ошибка"
        }
}
