package ru.faserkraft.client.model

data class UserData(
    val email: String,
    val password: String,
    val name: String,
    val role: UserRole?,
)

enum class UserRole(val value: String) {
    ADMIN("admin"),
    MASTER("master"),
    WORKER("worker");

    companion object {
        fun fromValue(value: String): UserRole? =
            entries.firstOrNull { it.value == value }
    }
}


