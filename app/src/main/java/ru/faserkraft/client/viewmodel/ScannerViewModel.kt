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
import ru.faserkraft.client.dto.ProductDto
import ru.faserkraft.client.dto.StepCloseDto
import ru.faserkraft.client.dto.StepDto
import ru.faserkraft.client.dto.emptyStep
import ru.faserkraft.client.error.ApiError
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

    suspend fun getProduct(serialNumber: String) {
        try {
            val product = repository.getProduct(serialNumber)
            _productState.postValue(product)

            // если при первом открытии нужно автоматически выбрать шаг
            val initialStep = product.steps
                .filter { it.status != "done" }
                .minByOrNull { it.stepDefinition.order } ?: emptyStep
            _selectedStep.postValue(initialStep)

            _events.emit(UiEvent.NavigateToProduct)
        } catch (e: Exception) {
            _errorState.emit("Ошибка загрузки товара")
        }
    }

    fun onRegistrationReady(model: RegistrationModel) {
        _registrationState.value = model
        _events.tryEmit(UiEvent.NavigateToRegistration)
    }

    suspend fun closeStep(step: StepDto) {
        val stepId = step.id
        if (stepId == 0) return

        val loginData = appAuth.getLoginData() ?: return
        val stepClose = StepCloseDto(stepId, loginData.username)
        val product = repository.postStep(stepClose)
        if (product != null) {
            _productState.postValue(product)
            val lastStep = product.steps
                .find { it.stepDefinition.order == step.stepDefinition.order }
                ?: emptyStep

            _selectedStep.postValue(lastStep)
        }
    }

    suspend fun decodeQrCode(jsonString: String) {
        if (isHandled) return
        isHandled = true

        if (isUfCode(jsonString)) {
            // ветка товара по серийному номеру
            runCatching {
                getProduct(jsonString)
            }.onFailure {
                _errorState.emit("Ошибка получения товара")
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
                            appAuth.setLoginData(result.userEmail, data.password)
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
                    } catch (e: ApiError) {
                        _errorState.emit("Ошибка сервера: ${e.code}")
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


}