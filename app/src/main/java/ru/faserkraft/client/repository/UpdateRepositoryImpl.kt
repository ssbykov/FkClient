package ru.faserkraft.client.repository

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import ru.faserkraft.client.api.UpdateApi
import ru.faserkraft.client.data.callApi
import ru.faserkraft.client.error.AppError
import ru.faserkraft.client.model.VersionInfo
import java.io.File
import javax.inject.Inject

class UpdateRepositoryImpl @Inject constructor(
    private val api: UpdateApi
) : UpdateRepository {
    override suspend fun getLatestVersion(): VersionInfo =
        callApi { api.getLatestVersion() }   // теперь тип выводится корректно
            ?: throw AppError.UnknownError

    override suspend fun downloadApkToFile(destFile: File, onProgress: (Int) -> Unit) {
        val response = api.downloadApk()

        if (!response.isSuccessful) {
            throw AppError.ApiError(
                status = response.code(),
                uiCode = "error_api_${response.code()}",
                message = response.message()
            )
        }

        val body = response.body() ?: throw AppError.UnknownError
        val totalBytes = body.contentLength()

        withContext(Dispatchers.IO) {
            var downloadedBytes = 0L
            var lastNotifiedPercent = -1

            body.byteStream().use { input ->
                destFile.outputStream().use { output ->
                    val buffer = ByteArray(8 * 1024)
                    var bytes: Int

                    while (input.read(buffer).also { bytes = it } != -1) {
                        output.write(buffer, 0, bytes)
                        downloadedBytes += bytes

                        if (totalBytes > 0) {
                            val percent = (downloadedBytes * 100 / totalBytes).toInt()
                            if (percent != lastNotifiedPercent) {
                                lastNotifiedPercent = percent
                                onProgress(percent)
                            }
                        }
                    }
                }
            }
        }
    }
}