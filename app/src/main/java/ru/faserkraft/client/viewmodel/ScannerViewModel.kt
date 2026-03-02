package ru.faserkraft.client.viewmodel

import android.graphics.Bitmap
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
import ru.faserkraft.client.dto.DailyPlanStepCreateDto
import ru.faserkraft.client.dto.DailyPlanStepUpdateDto
import ru.faserkraft.client.dto.DayPlanDto
import ru.faserkraft.client.dto.DeviceRequestDto
import ru.faserkraft.client.dto.EmployeeDto
import ru.faserkraft.client.dto.ProcessDto
import ru.faserkraft.client.dto.ProductCreateDto
import ru.faserkraft.client.dto.ProductDto
import ru.faserkraft.client.dto.ProductStatus
import ru.faserkraft.client.dto.QrDataResponseDto
import ru.faserkraft.client.dto.StepDto
import ru.faserkraft.client.dto.emptyStep
import ru.faserkraft.client.dto.toQrContent
import ru.faserkraft.client.error.AppError
import ru.faserkraft.client.model.UserData
import ru.faserkraft.client.model.UserRole
import ru.faserkraft.client.repository.ApiRepository
import ru.faserkraft.client.utils.QrCodeGenerator
import ru.faserkraft.client.utils.isUfCode
import ru.faserkraft.client.utils.qrCodeDecode
import java.io.IOException
import javax.inject.Inject

data class ScannerUiState(
    val isLoading: Boolean = false,
    val isActionInProgress: Boolean = false,
)

@HiltViewModel
class ScannerViewModel @Inject constructor(
    private val repository: ApiRepository,
    private val appAuth: AppAuth
) : ViewModel() {

    // ---------- UI state ----------
    private val _uiState = MutableLiveData(ScannerUiState())
    val uiState: LiveData<ScannerUiState> = _uiState

    private fun updateUiState(reducer: (ScannerUiState) -> ScannerUiState) {
        _uiState.postValue(reducer(_uiState.value ?: ScannerUiState()))
    }

    // ---------- Data state ----------
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

    private val _qrBitmap = MutableLiveData<Bitmap?>()
    val qrBitmap: LiveData<Bitmap?> = _qrBitmap


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

    // ---------- Navigation events ----------
    sealed class UiEvent {
        object NavigateToRegistration : UiEvent()
        object NavigateToProduct : UiEvent()
        object NavigateToNewProduct : UiEvent()
    }

    private val _events = MutableSharedFlow<UiEvent>(extraBufferCapacity = 1)
    val events: SharedFlow<UiEvent> = _events

    // ---------- Error events (for dialogs/snackbar) ----------
    private val _errorState = MutableSharedFlow<String>()
    val errorState: SharedFlow<String> = _errorState

    // ---------- QR handling ----------
    private var isHandled = false
    fun resetIsHandled() {
        isHandled = false
    }

    init {
        loadRegistrationData()
    }

    // ---------- Generic wrappers for loading/action states ----------
    private suspend fun <T> withLoading(block: suspend () -> T): T? {
        updateUiState { it.copy(isLoading = true) }
        return try {
            block()
        } catch (e: AppError) {
            _errorState.emit(appErrorToMessage(e))
            null
        } catch (e: Exception) {
            _errorState.emit(UNKNOWN_ERROR)
            null
        } finally {
            updateUiState { it.copy(isLoading = false) }
        }
    }

    private suspend fun <T> withAction(block: suspend () -> T): T? {
        updateUiState { it.copy(isActionInProgress = true) }
        return try {
            block()
        } catch (e: AppError) {
            _errorState.emit(appErrorToMessage(e))
            null
        } catch (e: Exception) {
            _errorState.emit(UNKNOWN_ERROR)
            null
        } finally {
            updateUiState { it.copy(isActionInProgress = false) }
        }
    }

    // ---------- Day plans ----------
    fun getDayPlans(date: String) {
        viewModelScope.launch {
            withLoading {
                repository.getDayPlans(date)
            }?.let { _dayPlans.postValue(it) }
        }
    }

    // ---------- Product ----------
    suspend fun getProduct(serialNumber: String) {
        updateUiState { it.copy(isLoading = true) }
        try {
            val product = repository.getProduct(serialNumber)
            if (product == null) {
                newProduct(serialNumber)
            } else {
                setProduct(product)
            }
        } catch (e: AppError.ApiError) {
            if (e.status == 404) {
                newProduct(serialNumber)
            } else {
                _errorState.emit(appErrorToMessage(e))
            }
        } catch (e: Exception) {
            _errorState.emit(UNKNOWN_ERROR)
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
                _errorState.emit(PRODUCT_NO_STEPS)
                return
            }

        _selectedStep.postValue(initialStep)
        _events.emit(UiEvent.NavigateToProduct)
    }

    suspend fun setProcesses() {
        withLoading {
            repository.getProcesses()
        }?.let { _processes.postValue(it) }
    }

    suspend fun setEmployees() {
        withLoading {
            repository.getEmployees()
        }?.let { _employees.postValue(it) }
    }

    suspend fun newProduct(serialNumber: String) {
        _newProduct.postValue(ProductCreateDto(serialNumber = serialNumber))
        setProcesses()
        _events.emit(UiEvent.NavigateToNewProduct)
    }

    suspend fun createProduct(newProduct: ProductCreateDto): Result<Unit> =
        withActionAndResult {
            repository.postProduct(newProduct)?.let { setProduct(it) }
        }

    suspend fun changeProductProcess(productId: Long, newProcessId: Int): Result<Unit> =
        withActionAndResult {
            repository.changeProductProcess(productId, newProcessId)?.let { setProduct(it) }
        }

    suspend fun setProductStatus(productId: Long, status: ProductStatus): Result<Unit> =
        withActionAndResult {
            repository.changeProductStatus(productId, status)?.let { setProduct(it) }
        }

    // ---------- Registration / User ----------
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

    // ---------- Step operations ----------
    suspend fun closeStep(step: StepDto) {
        if (step.id == 0) return
        withAction {
            repository.postStep(step.id)?.let { product ->
                _productState.postValue(product)
                val updatedStep = product.steps
                    .find { it.stepDefinition.order == step.stepDefinition.order }
                    ?: emptyStep
                _selectedStep.postValue(updatedStep)
            }
        }
    }

    suspend fun changeStepPerformer(step: StepDto, newEmployeeId: Int): Result<Unit> =
        withActionAndResult {
            repository.changeStepPerformer(step.id, newEmployeeId)
                ?.let { _productState.postValue(it) }
        }

    // ---------- Daily plan steps ----------
    suspend fun addStepToDailyPlan(
        planDate: String,
        employeeId: Int,
        stepId: Int,
        plannedQuantity: Int,
    ): Result<Unit> =
        withActionAndResult {
            val body = DailyPlanStepCreateDto(
                planDate = planDate,
                employeeId = employeeId,
                stepId = stepId,
                plannedQuantity = plannedQuantity,
            )
            repository.addStepToDailyPlan(body)?.let { _dayPlans.postValue(it) }
        }

    suspend fun updateStepInDailyPlan(
        planDate: String,
        employeeId: Int,
        stepId: Int,
        stepDefinitionId: Int,
        plannedQuantity: Int,
    ): Result<Unit> =
        withActionAndResult {
            val body = DailyPlanStepUpdateDto(
                stepId = stepId,
                planDate = planDate,
                stepDefinitionId = stepDefinitionId,
                employeeId = employeeId,
                plannedQuantity = plannedQuantity,
            )
            repository.updateStepInDailyPlan(body)?.let { _dayPlans.postValue(it) }
        }

    suspend fun removeStepFromDailyPlan(dailyPlanStepId: Int): Result<Unit> =
        withActionAndResult {
            repository.removeStepFromDailyPlan(dailyPlanStepId)?.let { _dayPlans.postValue(it) }
        }

    // ---------- QR decoding ----------
    suspend fun decodeQrCode(jsonString: String) {
        if (isHandled) return
        isHandled = true

        when {
            isUfCode(jsonString) -> handleProductQr(jsonString)
            else -> handleDeviceRegistrationQr(jsonString)
        }
    }

    private suspend fun handleProductQr(serialNumber: String) {
        withLoading {
            getProduct(serialNumber)
        }
    }

    private suspend fun handleDeviceRegistrationQr(jsonString: String) {
        withLoading {
            val decodedResult = qrCodeDecode(jsonString)
            val decoded = decodedResult.getOrNull() as? DeviceRequestDto ?: run {
                _errorState.emit(QR_PARSE_ERROR)
                return@withLoading
            }

            if (appAuth.checkRegistration() != null) {
                _errorState.emit(DEVICE_ALREADY_REGISTERED)
                return@withLoading
            }

            val deviceResponse = try {
                repository.postDevice(decoded)
            } catch (e: IOException) {
                _errorState.emit(NETWORK_ERROR)
                return@withLoading
            } catch (e: AppError.ApiError) {
                _errorState.emit("Ошибка сервера: ${e.status}")
                return@withLoading
            }

            if (deviceResponse == null) {
                _errorState.emit(EMPTY_SERVER_RESPONSE)
                return@withLoading
            }

            val userData = UserData(
                email = deviceResponse.userEmail,
                password = decoded.password,
                name = deviceResponse.userName,
                role = UserRole.fromValue(deviceResponse.userRole) ?: UserRole.WORKER
            )
            appAuth.saveUserData(userData)
            onRegistrationReady(userData)
        }
    }

    // ---------- QR generation (server-driven) ----------
    suspend fun loadAndGenerateQr(employeeId: Int): Result<Unit> =
        withActionAndResult {
            // 1. Получаем данные с сервера
            val response = repository.getQrCode(employeeId)
            if (response == null) {
                _errorState.emit(EMPTY_SERVER_RESPONSE)
                return@withActionAndResult
            }

            // 2. Сериализуем в JSON, как на сервере
            val content = response.toQrContent()

            // 3. Генерируем Bitmap QR-кода
            val bitmap = QrCodeGenerator.generate(content)
            _qrBitmap.postValue(bitmap)
        }


    // ---------- Result wrapper for actions that need to return Result<Unit> ----------
    private suspend fun withActionAndResult(block: suspend () -> Unit): Result<Unit> {
        updateUiState { it.copy(isActionInProgress = true) }
        return try {
            block()
            Result.success(Unit)
        } catch (e: AppError) {
            _errorState.emit(appErrorToMessage(e))
            Result.failure(e)
        } catch (e: Exception) {
            _errorState.emit(UNKNOWN_ERROR)
            Result.failure(e)
        } finally {
            updateUiState { it.copy(isActionInProgress = false) }
        }
    }

    // ---------- Error mapping ----------
    private fun appErrorToMessage(e: AppError): String = when (e) {
        is AppError.NetworkError -> NETWORK_ERROR
        is AppError.ApiError -> when (e.status) {
            401 -> "Не авторизован"
            403 -> "Доступ запрещён"
            404 -> e.message ?: "Ресурс не найден"
            else -> "Ошибка сервера: ${e.status}"
        }

        is AppError.DaoError -> "Ошибка локальной базы"
        is AppError.UnknownError -> UNKNOWN_ERROR
    }

    companion object {
        private const val UNKNOWN_ERROR = "Неизвестная ошибка"
        private const val NETWORK_ERROR = "Нет соединения с сервером"
        private const val QR_PARSE_ERROR = "Ошибка разбора QR‑кода"
        private const val DEVICE_ALREADY_REGISTERED = "Устройство уже зарегистрировано"
        private const val EMPTY_SERVER_RESPONSE = "Пустой ответ от сервера"
        private const val PRODUCT_NO_STEPS = "У товара нет шагов"
    }
}