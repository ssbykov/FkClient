package ru.faserkraft.client.domain.model

data class UserRegistration(
    val accessToken: String,
    val refreshToken: String,
    val expiresAt: String,
    val userEmail: String,
    val userName: String,
    val userRole: String,
)