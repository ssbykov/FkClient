package ru.faserkraft.client.data.repository

import ru.faserkraft.client.data.dto.InvitationRequestDto
import ru.faserkraft.client.data.dto.toQrContent
import ru.faserkraft.client.data.mapper.toDomain
import ru.faserkraft.client.data.mapper.toDto
import ru.faserkraft.client.data.network.Api
import ru.faserkraft.client.data.network.AuthApi
import ru.faserkraft.client.domain.model.DeviceRequest
import ru.faserkraft.client.domain.model.UserRegistration
import ru.faserkraft.client.domain.repository.DeviceRepository
import ru.faserkraft.client.utils.logger.Logger
import javax.inject.Inject

class DeviceRepositoryImpl @Inject constructor(
    private val api: Api,
    private val authApi: AuthApi,
    logger: Logger,
) : BaseRepository(logger), DeviceRepository {

    override suspend fun registerDevice(request: DeviceRequest): UserRegistration {
        val response = requireNotNull(callApi { authApi.registerDevice(request.toDto()) })
        return response.toDomain()
    }

    override suspend fun getQrCode(employeeId: Int): String =
        requireNotNull(callApi { api.getQrCode(InvitationRequestDto(employeeId)) })
            .toQrContent()

    override suspend fun logoutDevice() {
        requireNotNull(callApi { api.logoutDevice() })
    }
}