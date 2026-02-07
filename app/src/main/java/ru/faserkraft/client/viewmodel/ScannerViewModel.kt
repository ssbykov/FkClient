package ru.faserkraft.client.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import ru.faserkraft.client.auth.AppAuth
import ru.faserkraft.client.dto.DeviceRequestDto
import ru.faserkraft.client.dto.ProcessDto
import ru.faserkraft.client.dto.ProductCreateDto
import ru.faserkraft.client.dto.ProductDto
import ru.faserkraft.client.dto.StepCloseDto
import ru.faserkraft.client.dto.StepDto
import ru.faserkraft.client.dto.emptyStep
import ru.faserkraft.client.error.AppError
import ru.faserkraft.client.model.RegistrationModel
import ru.faserkraft.client.repository.ApiRepository
import ru.faserkraft.client.utils.isUfCode
import ru.faserkraft.client.utils.qrCodeDecode
import java.io.IOException
import javax.inject.Inject


@HiltViewModel
class ScannerViewModel @Inject constructor(
    private val repository: ApiRepository,
    private val appAuth: AppAuth
) : ViewModel() {


    private val _productState = MutableLiveData<ProductDto?>()
    val productState: LiveData<ProductDto?> = _productState

    private val _newProduct = MutableLiveData<ProductCreateDto>()
    val newProduct: LiveData<ProductCreateDto> = _newProduct

    private val _processes = MutableLiveData<List<ProcessDto>?>()
    val processes: LiveData<List<ProcessDto>?> = _processes

    private val _selectedStep = MutableLiveData<StepDto?>()
    val selectedStep: LiveData<StepDto?> = _selectedStep

    private val _registrationState = MutableLiveData<RegistrationModel?>()
    val registrationState: LiveData<RegistrationModel?> = _registrationState

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


    sealed class UiEvent {
        object NavigateToRegistration : UiEvent()
        object NavigateToProduct : UiEvent()
        object NavigateToNewProduct : UiEvent()
    }

    private val _events = MutableSharedFlow<UiEvent>(
        extraBufferCapacity = 1
    )
    val events: SharedFlow<UiEvent> = _events

    private val _errorState = MutableSharedFlow<String>()
    val errorState: SharedFlow<String> = _errorState

    private var isHandled = false
    fun resetIsHandled() {
        isHandled = false
    }

    init {
        loadRegistrationData()
    }


    suspend fun getProduct(serialNumber: String) {
        try {
            val product = repository.getProduct(serialNumber)
            if (product == null) {
                newProduct(serialNumber)
            } else {
                setProduct(product)
            }
        } catch (e: AppError) {
            throw e
        }
    }

    suspend fun setProduct(product: ProductDto) {
        _productState.postValue(product)

        val initialStep = product.steps
            .firstOrNull { it.status != "done" }
            ?: product.steps.lastOrNull()
            ?: run {
                _errorState.emit("У товара нет шагов")
                return
            }

        _selectedStep.postValue(initialStep)
        _events.emit(UiEvent.NavigateToProduct)
    }


    suspend fun newProduct(serialNumber: String) {
        val newProductDto = ProductCreateDto(
            serialNumber = serialNumber,
        )
        _newProduct.postValue(newProductDto)

        val processes = repository.getProcesses()
        _processes.postValue(processes)
        _events.emit(UiEvent.NavigateToNewProduct)
    }

    suspend fun createProduct(newProduct: ProductCreateDto) {
        repository.postProduct(newProduct)?.let { product ->
            setProduct(product)
        }
    }

    fun onRegistrationReady(model: RegistrationModel) {
        _registrationState.value = model
        _events.tryEmit(UiEvent.NavigateToRegistration)
    }

    fun loadRegistrationData() {
        _registrationState.value = appAuth.getRegistrationData()
    }

    fun resetRegistrationData() {
        appAuth.resetRegistration()
        loadRegistrationData()
    }

    suspend fun closeStep(step: StepDto) {
        val stepId = step.id
        if (stepId == 0) return

        val loginData = appAuth.getLoginData() ?: return
        val stepClose = StepCloseDto(stepId, loginData.username)

        try {
            val product = repository.postStep(stepClose)
            if (product != null) {
                _productState.postValue(product)

                val lastStep = product.steps
                    .find { it.stepDefinition.order == step.stepDefinition.order }
                    ?: emptyStep

                _selectedStep.postValue(lastStep)
            }
        } catch (e: AppError) {
            _errorState.emit(appErrorToMessage(e))
        } catch (e: Exception) {
            _errorState.emit("Неизвестная ошибка")
        }
    }



    suspend fun decodeQrCode(jsonString: String) {
        if (isHandled) return
        isHandled = true

        if (isUfCode(jsonString)) {
            runCatching {
                getProduct(jsonString)
            }.onFailure { e ->
                val message = throwableToMessage(e)
                _errorState.emit("Ошибка получения товара: $message")
            }
            return
        }

        qrCodeDecode(jsonString)
            .onSuccess { data ->
                if (data is DeviceRequestDto) {
                    if (appAuth.checkRegistration() != null) {
                        _errorState.emit("Устройство уже зарегистрировано")
                        return
                    }

                    try {
                        val result = repository.postDevice(data)
                        result?.let {
                            appAuth.setLoginData(
                                result.userEmail,
                                data.password,
                                result.userName
                            )
                            val registration = RegistrationModel(
                                result.userName,
                                result.userEmail,
                            )
                            onRegistrationReady(registration)
                        } ?: run {
                            _errorState.emit("Пустой ответ от сервера")
                        }
                    } catch (e: IOException) {
                        _errorState.emit("Проблема с сетью, попробуйте ещё раз")
                    } catch (e: AppError.ApiError) {
                        _errorState.emit("Ошибка сервера: ${e.status}")
                    } catch (e: Exception) {
                        _errorState.emit("Неизвестная ошибка $e")
                    }
                } else {
                    _errorState.emit("Некорректный QR‑код")
                }
            }
            .onFailure {
                _errorState.emit("Ошибка разбора QR‑кода")
            }
    }

    private fun appErrorToMessage(e: AppError): String =
        when (e) {
            is AppError.NetworkError -> "Нет соединения с сервером"
            is AppError.ApiError -> when (e.status) {
                401 -> "Не авторизован"
                403 -> "Доступ запрещён"
                404 -> "Шаг не найден"
                else -> "Ошибка сервера: ${e.status}"
            }
            is AppError.DaoError     -> "Ошибка локальной базы"
            is AppError.UnknownError -> "Неизвестная ошибка"
        }

    private fun throwableToMessage(e: Throwable): String =
        when (e) {
            is AppError -> appErrorToMessage(e)
            else        -> "Неизвестная ошибка"
        }



}