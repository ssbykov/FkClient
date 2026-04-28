package ru.faserkraft.client.domain.usecase.device

import ru.faserkraft.client.domain.repository.DeviceRepository
import javax.inject.Inject

class GetQrCodeUseCase @Inject constructor(
    private val repository: DeviceRepository
) {
    suspend operator fun invoke(employeeId: Int): String =
        repository.getQrCode(employeeId)
}