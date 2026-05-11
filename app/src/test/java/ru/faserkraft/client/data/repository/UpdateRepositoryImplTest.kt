package ru.faserkraft.client.data.repository

import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import retrofit2.Response
import ru.faserkraft.client.data.dto.VersionInfoDto
import ru.faserkraft.client.data.network.UpdateApi
import ru.faserkraft.client.domain.model.UserRole
import ru.faserkraft.client.error.AppError
import ru.faserkraft.client.utils.Logger

class UpdateRepositoryImplTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private val mockApi: UpdateApi = mockk()
    private val mockLogger: Logger = mockk(relaxed = true)

    private lateinit var repository: UpdateRepositoryImpl

    private val versionInfoDto = VersionInfoDto(
        versionName = "1.2.3",
        apkFile = "https://example.com/app.apk",
        changelog = "Bug fixes",
        roles = listOf(UserRole.WORKER),
        forceUpdate = false
    )

    @Before
    fun setUp() {
        repository = UpdateRepositoryImpl(
            api = mockApi,
            logger = mockLogger,
        )
    }

    // ==========================================
    // getLatestVersion
    // ==========================================

    @Test
    fun `getLatestVersion returns mapped domain model`() = runTest {
        coEvery { mockApi.getLatestVersion() } returns Response.success(versionInfoDto)

        val result = repository.getLatestVersion()

        assertEquals("1.2.3", result.versionName)
        assertEquals("https://example.com/app.apk", result.apkFile)
        assertEquals("Bug fixes", result.changelog)
        assertEquals(listOf(UserRole.WORKER), result.roles)
        assertEquals(false, result.forceUpdate)
    }

    @Test(expected = AppError.UnknownError::class)
    fun `getLatestVersion throws UnknownError when api body is null`() = runTest {
        coEvery { mockApi.getLatestVersion() } returns Response.success(null)

        repository.getLatestVersion()
    }

    @Test(expected = AppError.ApiError::class)
    fun `getLatestVersion throws ApiError on http error`() = runTest {
        coEvery { mockApi.getLatestVersion() } returns
                Response.error(500, "".toResponseBody())

        repository.getLatestVersion()
    }

    @Test(expected = AppError.NetworkError::class)
    fun `getLatestVersion throws NetworkError on IOException`() = runTest {
        coEvery { mockApi.getLatestVersion() } throws AppError.NetworkError()

        repository.getLatestVersion()
    }

    // ==========================================
    // downloadApkToFile
    // ==========================================

    @Test
    fun `downloadApkToFile writes content to file`() = runTest {
        val content = "fake apk content".toByteArray()
        val responseBody = content.toResponseBody("application/octet-stream".toMediaType())
        coEvery { mockApi.downloadApk() } returns Response.success(responseBody)

        val destFile = tempFolder.newFile("app.apk")
        repository.downloadApkToFile(destFile) {}

        assertTrue(destFile.exists())
        assertEquals("fake apk content", destFile.readText())
    }

    @Test
    fun `downloadApkToFile reports progress 100 when content length known`() = runTest {
        val content = ByteArray(1024) { it.toByte() }
        val responseBody = content.toResponseBody("application/octet-stream".toMediaType())
        coEvery { mockApi.downloadApk() } returns Response.success(responseBody)

        val progressValues = mutableListOf<Int>()
        val destFile = tempFolder.newFile("app.apk")
        repository.downloadApkToFile(destFile) { progressValues.add(it) }

        assertTrue(progressValues.isNotEmpty())
        assertEquals(100, progressValues.last())
    }

    @Test
    fun `downloadApkToFile does not report progress when content length is unknown`() = runTest {
        val content = "data".toByteArray()
        // contentLength() == -1 когда размер неизвестен
        val responseBody = object : okhttp3.ResponseBody() {
            override fun contentType() = "application/octet-stream".toMediaType()
            override fun contentLength() = -1L
            override fun source() = okio.Buffer().apply { write(content) }
        }
        coEvery { mockApi.downloadApk() } returns Response.success(responseBody)

        val progressValues = mutableListOf<Int>()
        val destFile = tempFolder.newFile("app.apk")
        repository.downloadApkToFile(destFile) { progressValues.add(it) }

        assertTrue(progressValues.isEmpty())
    }

    @Test(expected = AppError.ApiError::class)
    fun `downloadApkToFile throws ApiError when response is not successful`() = runTest {
        coEvery { mockApi.downloadApk() } returns
                Response.error(403, "".toResponseBody())

        val destFile = tempFolder.newFile("app.apk")
        repository.downloadApkToFile(destFile) {}
    }

    @Test(expected = AppError.UnknownError::class)
    fun `downloadApkToFile throws UnknownError when body is null`() = runTest {
        // Response.success(null) для ResponseBody даёт body() == null
        coEvery { mockApi.downloadApk() } returns Response.success(null)

        val destFile = tempFolder.newFile("app.apk")
        repository.downloadApkToFile(destFile) {}
    }

    @Test
    fun `downloadApkToFile ApiError contains correct status code`() = runTest {
        coEvery { mockApi.downloadApk() } returns
                Response.error(404, "".toResponseBody())

        val destFile = tempFolder.newFile("app.apk")
        val error = runCatching {
            repository.downloadApkToFile(destFile) {}
        }.exceptionOrNull() as? AppError.ApiError

        assertEquals(404, error?.status)
    }
}