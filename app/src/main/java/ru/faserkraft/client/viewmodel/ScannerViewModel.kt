package ru.faserkraft.client.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import ru.faserkraft.client.auth.AppAuth
import ru.faserkraft.client.dto.DeviceRequestDto
import ru.faserkraft.client.dto.ProductDto
import ru.faserkraft.client.error.ApiError
import ru.faserkraft.client.model.RegistrationModel
import ru.faserkraft.client.repository.ApiRepository
import ru.faserkraft.client.utils.qrCodeDecode
import java.io.IOException
import javax.inject.Inject


@HiltViewModel
class ScannerViewModel @Inject constructor(
    private val repository: ApiRepository,
    private val appAuth: AppAuth
) : ViewModel() {


    private val _productState = MutableLiveData<ProductDto>()
    val productState: LiveData<ProductDto>
        get() = _productState

    private val _errorState = MutableSharedFlow<String>()
    val errorState: SharedFlow<String> = _errorState

    private val _registrationState = MutableLiveData<RegistrationModel>()
    val registrationState: LiveData<RegistrationModel>
        get() = _registrationState

    fun onNavigationDone() {
        val currentModel = _registrationState.value
        if (currentModel != null) {
            _registrationState.value = currentModel.copy(isUpdated = false)
        }
    }

    suspend fun getProduct(serialNumber: String) {
        val product = repository.getProduct(serialNumber)
        _productState.value = product
    }

    private var isHandled = false
    fun resetIsHandled(){
        isHandled = false
    }


    suspend fun decodeQrCodeJson(jsonString: String) {
        if (isHandled) return
        isHandled = true
        qrCodeDecode(jsonString)
            .onSuccess { data ->
                if (data is DeviceRequestDto) {
                    try {
                        val result = repository.postDevice(data)
                        result?.let {
                            appAuth.setLoginData(result.userEmail, data.password)
                            _registrationState.value = RegistrationModel(
                                result.userName,
                                result.userEmail,
                                true
                            )
                        } ?: run {
                            _errorState.emit("Пустой ответ от сервера")
                        }
                    } catch (e: IOException) {
                        _errorState.emit("Проблема с сетью, попробуйте ещё раз")
                    } catch (e: ApiError) {
                        _errorState.emit("Ошибка сервера: ${e.code}")
                    } catch (e: Exception) {
                        _errorState.emit("Неизвестная ошибка")
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