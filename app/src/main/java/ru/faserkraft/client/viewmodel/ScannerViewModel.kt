package ru.faserkraft.client.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import ru.faserkraft.client.auth.AppAuth
import ru.faserkraft.client.dto.DeviceRequestDto
import ru.faserkraft.client.dto.ProductDto
import ru.faserkraft.client.repository.ApiRepository
import ru.faserkraft.client.utils.qrCodeDecode
import javax.inject.Inject


@HiltViewModel
class ScannerViewModel @Inject constructor(
    private val repository: ApiRepository,
    private val appAuth: AppAuth
) : ViewModel() {


    private val _productState = MutableLiveData<ProductDto>()
    val productState: LiveData<ProductDto>
        get() = _productState

    suspend fun getProduct(serialNumber: String) {
        val product = repository.getProduct(serialNumber)
        _productState.value = product
    }

    suspend fun decodeQrCodeJson(jsonString: String) {
        qrCodeDecode(jsonString)
            .onSuccess { data ->
                if (data is DeviceRequestDto) {
                    val result = repository.postDevice(data)
                    result?.let {
                        appAuth.setLoginData(result.userEmail, data.password)
                    }
                    println(result)
                } else {
                    // неизвестный action / null
                }
            }
            .onFailure { e ->
                println(e)
                // лог, тост, snackbar и т.п.
            }
    }
}