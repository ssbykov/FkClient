package ru.faserkraft.client.domain.model

data class VersionInfo(
    val versionName: String,
    val apkFile: String,
    val changelog: String,
    val roles: List<UserRole>,
    val forceUpdate: Boolean
)