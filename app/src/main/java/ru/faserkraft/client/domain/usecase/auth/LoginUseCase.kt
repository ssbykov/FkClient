package ru.faserkraft.client.domain.usecase.auth

import ru.faserkraft.client.domain.repository.AuthRepository
import ru.faserkraft.client.dto.LoginData
import ru.faserkraft.client.dto.LoginDto
import javax.inject.Inject

class LoginUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(loginData: LoginData): LoginDto {
        return authRepository.login(loginData)
    }
}