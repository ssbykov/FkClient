package ru.faserkraft.client.api

import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Streaming
import ru.faserkraft.client.model.VersionInfo


interface UpdateApi {
    @GET("app-update/version")
    suspend fun getLatestVersion(): Response<VersionInfo>

    @GET("app-update/download")
    @Streaming
    suspend fun downloadApk(): Response<ResponseBody>
}