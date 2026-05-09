package ru.faserkraft.client.data.mapper

import ru.faserkraft.client.domain.model.VersionInfo
import ru.faserkraft.client.data.dto.VersionInfoDto

fun VersionInfoDto.toDomain(): VersionInfo =
    VersionInfo(
        versionName = versionName,
        apkFile = apkFile,
        changelog = changelog,
        roles = roles,
        forceUpdate = forceUpdate
    )