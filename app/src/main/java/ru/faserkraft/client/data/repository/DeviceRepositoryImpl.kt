package ru.faserkraft.client.data.repository

import ru.faserkraft.client.api.Api
import ru.faserkraft.client.api.AuthApi
import ru.faserkraft.client.data.callApi
import ru.faserkraft.client.domain.model.UserRegistration
import ru.faserkraft.client.domain.repository.DeviceRepository
import ru.faserkraft.client.dto.DeviceRequestDto
import ru.faserkraft.client.dto.toQrContent
import javax.inject.Inject

class DeviceRepositoryImpl @Inject constructor(
    private val api: Api,
    private val authApi: AuthApi,
) : DeviceRepository {

    override suspend fun registerDevice(request: DeviceRequestDto): UserRegistration {
        val response = requireNotNull(callApi { authApi.registerDevice(request) })
        return UserRegistration(
            userEmail = response.userEmail,
            userName = response.userName,
            userRole = response.userRole,
            password = request.password,
        )
    }

    override suspend fun getQrCode(employeeId: Int): String {
        return requireNotNull(callApi { api.getQrCode(employeeId) }).toQrContent()
    }
}