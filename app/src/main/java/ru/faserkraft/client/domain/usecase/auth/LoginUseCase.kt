package ru.faserkraft.client.domain.usecase.auth

import ru.faserkraft.client.domain.model.LoginCredentials
import ru.faserkraft.client.domain.repository.AuthRepository
import javax.inject.Inject

class LoginUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(credentials: LoginCredentials): String =
        authRepository.login(credentials)
}