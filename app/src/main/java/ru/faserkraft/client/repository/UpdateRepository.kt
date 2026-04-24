package ru.faserkraft.client.repository

import ru.faserkraft.client.model.VersionInfo
import java.io.File


interface UpdateRepository {
    suspend fun getLatestVersion(): VersionInfo
    suspend fun downloadApkToFile(destFile: File, onProgress: (Int) -> Unit)
}