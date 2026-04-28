package ru.faserkraft.client.domain.usecase.device

import ru.faserkraft.client.domain.model.UserRegistration
import ru.faserkraft.client.domain.repository.DeviceRepository
import javax.inject.Inject

class RegisterDeviceUseCase @Inject constructor(
    private val repository: DeviceRepository
) {
    suspend operator fun invoke(serverUrl: String, password: String): UserRegistration =
        repository.registerDevice(serverUrl, password)
}