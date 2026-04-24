package ru.faserkraft.client.model

import com.google.gson.annotations.SerializedName

data class UserData(
    val email: String,
    val password: String,
    val name: String,
    val role: UserRole?,
)

enum class UserRole(val value: String) {
    @SerializedName("admin")
    ADMIN("admin"),
    @SerializedName("master")
    MASTER("master"),
    @SerializedName("worker")
    WORKER("worker");

    companion object {
        fun fromValue(value: String): UserRole? =
            entries.firstOrNull { it.value == value }
    }
}


