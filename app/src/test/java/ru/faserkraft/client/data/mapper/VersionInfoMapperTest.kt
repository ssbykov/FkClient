package ru.faserkraft.client.data.mapper

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import ru.faserkraft.client.data.dto.VersionInfoDto
import ru.faserkraft.client.domain.model.UserRole

class VersionInfoMapperTest {

    // ── фикстура ──────────────────────────────────────────────────────────────

    private val dto = VersionInfoDto(
        versionName = "1.5.0",
        apkFile = "release-1.5.0.apk",
        changelog = "Добавлен сканер QR",
        roles = listOf(UserRole.ADMIN, UserRole.WORKER),
        forceUpdate = true
    )

    // ── VersionInfoDto.toDomain() ─────────────────────────────────────────────

    @Test
    fun `toDomain - maps versionName`() {
        assertEquals("1.5.0", dto.toDomain().versionName)
    }

    @Test
    fun `toDomain - maps apkFile`() {
        assertEquals("release-1.5.0.apk", dto.toDomain().apkFile)
    }

    @Test
    fun `toDomain - maps changelog`() {
        assertEquals("Добавлен сканер QR", dto.toDomain().changelog)
    }

    @Test
    fun `toDomain - maps roles list`() {
        val result = dto.toDomain()
        assertEquals(2, result.roles.size)
        assertEquals(UserRole.ADMIN, result.roles[0])
        assertEquals(UserRole.WORKER, result.roles[1])
    }

    @Test
    fun `toDomain - maps forceUpdate true`() {
        assertTrue(dto.toDomain().forceUpdate)
    }

    @Test
    fun `toDomain - maps forceUpdate false`() {
        val dtoFalse = dto.copy(forceUpdate = false)
        assertFalse(dtoFalse.toDomain().forceUpdate)
    }

    @Test
    fun `toDomain - empty roles returns empty list`() {
        val dtoEmpty = dto.copy(roles = emptyList())
        assertTrue(dtoEmpty.toDomain().roles.isEmpty())
    }

    @Test
    fun `toDomain - all three roles are preserved`() {
        val dtoAllRoles = dto.copy(
            roles = listOf(UserRole.ADMIN, UserRole.MASTER, UserRole.WORKER)
        )
        val result = dtoAllRoles.toDomain()
        assertEquals(3, result.roles.size)
        assertTrue(result.roles.containsAll(listOf(UserRole.ADMIN, UserRole.MASTER, UserRole.WORKER)))
    }

    @Test
    fun `toDomain - empty changelog is preserved`() {
        val dtoEmptyLog = dto.copy(changelog = "")
        assertEquals("", dtoEmptyLog.toDomain().changelog)
    }
}