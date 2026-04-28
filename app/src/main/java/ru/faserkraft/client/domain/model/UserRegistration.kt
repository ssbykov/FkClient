package ru.faserkraft.client.domain.model

data class UserRegistration(
    val userEmail: String,
    val userName: String,
    val userRole: String,
    val password: String,
)