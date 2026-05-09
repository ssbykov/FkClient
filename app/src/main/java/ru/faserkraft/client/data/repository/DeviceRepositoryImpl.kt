package ru.faserkraft.client.data.repository

import ru.faserkraft.client.data.network.Api
import ru.faserkraft.client.data.network.AuthApi
import ru.faserkraft.client.data.callApi
import ru.faserkraft.client.data.mapper.toDto
import ru.faserkraft.client.domain.model.DeviceRequest
import ru.faserkraft.client.domain.model.UserRegistration
import ru.faserkraft.client.domain.repository.DeviceRepository
import ru.faserkraft.client.data.dto.toQrContent
import javax.inject.Inject

class DeviceRepositoryImpl @Inject constructor(
    private val api: Api,
    private val authApi: AuthApi,
) : DeviceRepository {

    override suspend fun registerDevice(request: DeviceRequest): UserRegistration {
        val dto = request.toDto()
        val response = requireNotNull(callApi { authApi.registerDevice(dto) })
        return UserRegistration(
            userEmail = response.userEmail,
            userName = response.userName,
            userRole = response.userRole,
            password = dto.password,
        )
    }

    override suspend fun getQrCode(employeeId: Int): String =
        requireNotNull(callApi { api.getQrCode(employeeId) }).toQrContent()
}