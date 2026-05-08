package ru.faserkraft.client.dto

import com.google.gson.annotations.SerializedName


data class LoginDto(
    @SerializedName("access_token")
    val accessToken: String,
    @SerializedName("token_type")
    val tokenType: String
) : ItemDto()

data class LoginRequestDto(
    val username: String,
    val password: String
) : ItemDto()
