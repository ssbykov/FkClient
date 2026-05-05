package ru.faserkraft.client.domain.repository

import ru.faserkraft.client.domain.model.VersionInfo
import java.io.File

interface UpdateRepository {
    suspend fun getLatestVersion(): VersionInfo
    suspend fun downloadApkToFile(destFile: File, onProgress: (Int) -> Unit)
}